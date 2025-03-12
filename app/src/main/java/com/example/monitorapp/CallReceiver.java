package com.example.monitorapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.media.MediaRecorder;
import android.util.Log; // Adicione esta linha
import java.io.IOException;

public class CallReceiver extends BroadcastReceiver {
    private MediaRecorder recorder;
    private boolean isRecording = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
        String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

        if (state != null) {
            if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                Log.d("CallReceiver", "Chamada iniciada, iniciando gravação...");
                startRecording(context); // Passa o context aqui
            } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                Log.d("CallReceiver", "Chamada encerrada, parando gravação...");
                stopRecording();
            }
        }
    }

    private void startRecording(Context context) { // Adiciona o parâmetro context
        if (!isRecording) {
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setOutputFile(context.getExternalFilesDir(null) + "/call_record.3gp");
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

            try {
                recorder.prepare();
                recorder.start();
                isRecording = true;
                Log.d("CallReceiver", "Gravação iniciada com sucesso");
            } catch (IOException | IllegalStateException e) {
                Log.e("CallReceiver", "Erro ao iniciar gravação: " + e.getMessage());
            }
        }
    }

    private void stopRecording() {
        if (isRecording && recorder != null) {
            try {
                recorder.stop();
                Log.d("CallReceiver", "Gravação parada com sucesso");
            } catch (RuntimeException e) {
                Log.e("CallReceiver", "Erro ao parar gravação: " + e.getMessage());
            } finally {
                recorder.release();
                recorder = null;
                isRecording = false;
            }
        }
    }
}
