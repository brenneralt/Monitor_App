package com.example.monitorapp;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotificationListener";

    @Override
    public void onListenerConnected() {
        Log.d(TAG, "Notification Listener conectado");
    }

    @Override
    public void onListenerDisconnected() {
        Log.d(TAG, "Notification Listener desconectado");
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(TAG, "Notificação capturada de: " + sbn.getPackageName());

        // Formatar o timestamp
        long timestampMillis = sbn.getPostTime();
        String formattedTimestamp = formatTimestamp(timestampMillis);

        // Informações básicas
        Log.d(TAG, "ID da notificação: " + sbn.getId());
        Log.d(TAG, "Timestamp: " + formattedTimestamp); // Usar o timestamp formatado

        // Variáveis para título e texto (com valores padrão)
        String title = "Sem título";
        String text = "Sem texto";

        Notification notification = sbn.getNotification();
        if (notification != null) {
            Log.d(TAG, "Ticker Text: " + (notification.tickerText != null ? notification.tickerText.toString() : "null"));

            // Verifica se os extras existem
            Bundle extras = notification.extras;
            if (extras != null) {
                // Obtém título e texto da notificação
                CharSequence titleCharSeq = extras.getCharSequence("android.title");
                CharSequence textCharSeq = extras.getCharSequence("android.text");

                title = titleCharSeq != null ? titleCharSeq.toString() : title;
                text = textCharSeq != null ? textCharSeq.toString() : text;

                Log.d(TAG, "Título: " + title);
                Log.d(TAG, "Texto: " + text);
            } else {
                Log.w(TAG, "Extras da notificação são nulos.");
            }
        } else {
            Log.w(TAG, "Objeto Notification é nulo");
        }

        // Enviar para o servidor com o timestamp formatado
        sendNotificationToServer(sbn.getPackageName(), title, text, formattedTimestamp);
    }

    private String formatTimestamp(long timestampMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestampMillis));
    }

    private void sendNotificationToServer(String app, String title, String text, String timestamp) {
        new Thread(() -> {
            try {
                URL url = new URL("http://192.168.2.2:3000/api/notifications"); // Certifique-se de que este IP é o correto
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                // Montando o JSON com os dados da notificação
                String json = "{\"app\":\"" + app + "\",\"title\":\"" + title + "\",\"text\":\"" + text + "\",\"timestamp\":\"" + timestamp + "\"}";
                conn.getOutputStream().write(json.getBytes());

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "Código de resposta do servidor: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    Log.d(TAG, "Resposta do servidor: " + response.toString());
                }

                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Erro ao enviar notificação: " + e.getMessage());
            }
        }).start();
    }
}