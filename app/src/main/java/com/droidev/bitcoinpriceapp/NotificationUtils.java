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

    private static final String PERSISTENT_CHANNEL_ID = "bitcoin_price_service_channel";
    private static final String PRICE_CHANNEL_ID = "bitcoin_price_update_channel";
    private static final int PRICE_NOTIFICATION_ID = 2;

    @NonNull
    public static Notification createPersistentNotification(Context context) {
        createNotificationChannels(context);

        return new NotificationCompat.Builder(context, PERSISTENT_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.persistent_notification_title))
                .setContentText(context.getString(R.string.persistent_notification_text))
                .setSmallIcon(R.drawable.ic_bitcoin_notification)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .setGroup("persistent_notification_group")
                .build();
    }

    public static void sendPriceNotification(Context context, double brl, double usd) {
        NumberFormat brlFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        NumberFormat usdFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
        String formattedBrl = brlFormatter.format(brl);
        String formattedUsd = usdFormatter.format(usd);

        String notificationText = String.format("%s | %s", formattedBrl, formattedUsd);

        Notification notification = new NotificationCompat.Builder(context, PRICE_CHANNEL_ID)
                .setContentTitle(context.getString(R.string.price_notification_title))
                .setContentText(notificationText)
                .setSmallIcon(R.drawable.ic_bitcoin_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup("price_notification_group")
                .build();

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(PRICE_NOTIFICATION_ID, notification);
    }

    private static void createNotificationChannels(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel persistentChannel = new NotificationChannel(
                    PERSISTENT_CHANNEL_ID,
                    context.getString(R.string.persistent_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );

            persistentChannel.setDescription(context.getString(R.string.persistent_channel_description));
            persistentChannel.setShowBadge(false);

            NotificationChannel priceChannel = new NotificationChannel(
                    PRICE_CHANNEL_ID,
                    context.getString(R.string.price_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
            );

            priceChannel.setDescription(context.getString(R.string.price_channel_description));

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(persistentChannel);
            notificationManager.createNotificationChannel(priceChannel);
        }
    }
}