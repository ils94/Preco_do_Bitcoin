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

    private static final String PERSISTENT_CHANNEL_ID = "canal_servico_preco_do_bitcoin";
    private static final String PRICE_CHANNEL_ID = "canal_preco_do_bitcoin_atualizacao";
    private static final String PERSISTENT_CHANNEL_NAME = "Serviço do Preço do Bitcoin";
    private static final String PRICE_CHANNEL_NAME = "Atualização do Preço do Bitcoin";
    private static final int PRICE_NOTIFICATION_ID = 2;

    @NonNull
    public static Notification createPersistentNotification(Context context) {
        createNotificationChannels(context);

        return new NotificationCompat.Builder(context, PERSISTENT_CHANNEL_ID)
                .setContentTitle("Serviço de Monitoramento do Preço do Bitcoin")
                .setContentText("Monitorando o Preço do Bitcoin...")
                .setSmallIcon(R.drawable.ic_bitcoin_notification)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .build();
    }

    public static void sendPriceNotification(Context context, double brl, double usd) {
        // Format prices with correct locale
        NumberFormat brlFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        NumberFormat usdFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
        String formattedBrl = brlFormatter.format(brl);
        String formattedUsd = usdFormatter.format(usd);

        String notificationText = String.format("Preço do Bitcoin: %s / %s", formattedBrl, formattedUsd);

        Notification notification = new NotificationCompat.Builder(context, PRICE_CHANNEL_ID)
                .setContentTitle("Atualização do Preço do Bitcoin")
                .setContentText(notificationText)
                .setSmallIcon(R.drawable.ic_bitcoin_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(PRICE_NOTIFICATION_ID, notification);
    }

    private static void createNotificationChannels(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel persistentChannel = new NotificationChannel(
                    PERSISTENT_CHANNEL_ID,
                    PERSISTENT_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_MIN
            );
            persistentChannel.setDescription("Canal para o Serviço de Notificação Persistente do Bitcoin");
            persistentChannel.setShowBadge(false);

            NotificationChannel priceChannel = new NotificationChannel(
                    PRICE_CHANNEL_ID,
                    PRICE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            priceChannel.setDescription("Canal para Atualizações Sobre o Preço do Bitcoin");

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(persistentChannel);
            notificationManager.createNotificationChannel(priceChannel);
        }
    }
}