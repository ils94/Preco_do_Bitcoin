package com.droidev.bitcoinpriceapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.text.NumberFormat;
import java.util.Locale;

public class NotificationUtils {

    private static final String CHANNEL_ID = "bitcoin_price_channel";
    private static final String CHANNEL_NAME = "Bitcoin Price Updates";
    private static final int PRICE_NOTIFICATION_ID = 2;

    // Cria a notificação persistente para o serviço em primeiro plano
    @NonNull
    public static Notification createPersistentNotification(Context context) {
        createNotificationChannel(context);

        return new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Serviço de Monitoramento do Preço do Bitcoin")
                .setContentText("Monitorando o preço do Bitcoin...")
                .setSmallIcon(R.drawable.ic_bitcoin_notification) // Ícone monocromático
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    // Envia a notificação com o preço do Bitcoin
    public static void sendPriceNotification(Context context, double brl, double usd) {
        // Formatar preços com os padrões corretos
        NumberFormat brlFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        NumberFormat usdFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
        String formattedBrl = brlFormatter.format(brl);
        String formattedUsd = usdFormatter.format(usd);

        String notificationText = String.format("Bitcoin Price: %s / %s", formattedBrl, formattedUsd);

        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Atualização do Preço do Bitcoin")
                .setContentText(notificationText)
                .setSmallIcon(R.drawable.ic_bitcoin_notification) // Ícone monocromático para a barra
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(PRICE_NOTIFICATION_ID, notification);
    }

    // Cria o canal de notificação (necessário para Android 8.0+)
    private static void createNotificationChannel(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Canal para notificações de preço do Bitcoin");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}