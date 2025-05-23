package com.droidev.bitcoinpriceapp;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class BitcoinService extends Service {

    private final Handler handler = new Handler();
    private int INTERVAL = 5 * 60 * 1000; // 5 minutos como padrão
    private Runnable task;
    private double lastBrlPrice = -1.0; // Valor inicial inválido para garantir primeira notificação
    private double lastUsdPrice = -1.0; // Valor inicial inválido para garantir primeira notificação

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, NotificationUtils.createPersistentNotification(this));
        setupTask();
        // Atrasar a primeira execução pelo intervalo definido
        handler.postDelayed(task, INTERVAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("interval")) {
            INTERVAL = intent.getIntExtra("interval", INTERVAL);
            // Reiniciar a tarefa com o novo intervalo
            handler.removeCallbacks(task);
            setupTask();
            handler.postDelayed(task, INTERVAL);
        }
        return START_STICKY;
    }

    private void setupTask() {
        task = new Runnable() {
            @Override
            public void run() {
                fetchBitcoinPrice();
                handler.postDelayed(this, INTERVAL);
            }
        };
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(task);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void fetchBitcoinPrice() {
        String url = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=brl,usd";
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        JSONObject btc = json.getJSONObject("bitcoin");
                        double brl = btc.getDouble("brl");
                        double usd = btc.getDouble("usd");

                        // Enviar notificação apenas se o preço mudou
                        if (brl != lastBrlPrice || usd != lastUsdPrice) {
                            NotificationUtils.sendPriceNotification(getApplicationContext(), brl, usd);
                            lastBrlPrice = brl;
                            lastUsdPrice = usd;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                Throwable::printStackTrace);

        queue.add(request);
    }
}