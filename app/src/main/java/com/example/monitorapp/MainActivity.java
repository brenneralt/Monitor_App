package com.example.monitorapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "TestChannel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Verificar e solicitar permissão de notificações no Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        // Criar o canal de notificação (para Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Test Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Log.d("MainActivity", "Canal de notificação criado");
        }

        // Configurar o botão para testar a notificação
        Button testButton = findViewById(R.id.test_button);
        if (testButton == null) {
            Log.e("MainActivity", "Botão test_button não encontrado no layout");
            return;
        }

        testButton.setOnClickListener(v -> {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            Log.d("MainActivity", "Botão clicado, enviando notificação");

            Notification.Builder builder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder = new Notification.Builder(this, CHANNEL_ID);
            } else {
                builder = new Notification.Builder(this);
            }

            Notification notification = builder
                    .setContentTitle("Teste de Notificação")
                    .setContentText("Isso é um teste!")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build();

            notificationManager.notify(1, notification);
            Log.d("MainActivity", "Notificação enviada");
        });
    }
}
