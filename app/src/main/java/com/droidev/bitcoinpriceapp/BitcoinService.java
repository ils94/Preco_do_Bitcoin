package com.droidev.bitcoinpriceapp;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class BitcoinService extends Service {

    private final Handler handler = new Handler();
    private long INTERVAL = 5 * 60 * 1000; // Default: 5 minutes (in milliseconds)
    private double targetPrice = -1.0;
    private String currency = "USD";
    private Runnable task;
    private long lastExecutionTime = 0;

    private static final String PREF_NAME = "bitcoin_prices";
    private static final String KEY_BRL = "brl";
    private static final String KEY_USD = "usd";
    private static final String KEY_INTERVAL = "interval";
    private static final String KEY_TARGET_PRICE = "target_price";
    private static final String KEY_CURRENCY = "currency";
    private static final String KEY_LAST_EXECUTION = "last_execution";

    private double lastBrlPrice = -1.0;
    private double lastUsdPrice = -1.0;

    @Override
    public void onCreate() {
        super.onCreate();
        // Load persisted values
        loadPreferences();
        startForeground(1, NotificationUtils.createPersistentNotification(this));
        scheduleNextRun();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra("interval")) {
                INTERVAL = intent.getIntExtra("interval", (int) INTERVAL);
                saveInterval(INTERVAL);
            }
            if (intent.hasExtra("targetPrice")) {
                targetPrice = intent.getDoubleExtra("targetPrice", targetPrice);
                saveTargetPrice(targetPrice);
            }
            if (intent.hasExtra("currency")) {
                currency = intent.getStringExtra("currency");
                saveCurrency(currency);
            }
            // Reschedule task with updated values
            handler.removeCallbacks(task);
            scheduleNextRun();
        } else {
            // If intent is null, reschedule with persisted values
            handler.removeCallbacks(task);
            scheduleNextRun();
        }
        return START_STICKY;
    }

    private void scheduleNextRun() {
        setupTask();
        long currentTime = System.currentTimeMillis();
        long timeSinceLastExecution = currentTime - lastExecutionTime;

        // If enough time has passed, run immediately; otherwise, wait for remaining time
        long delay = timeSinceLastExecution >= INTERVAL ? 0 : INTERVAL - timeSinceLastExecution;
        Log.d("BitcoinService", "Scheduling next run in " + delay + "ms, INTERVAL: " + INTERVAL);
        handler.postDelayed(task, delay);
    }

    private void setupTask() {
        task = new Runnable() {
            @Override
            public void run() {
                lastExecutionTime = System.currentTimeMillis();
                saveLastExecutionTime(lastExecutionTime);
                Log.d("BitcoinService", "Task executed at: " + lastExecutionTime);
                fetchBitcoinPrice();
                // Schedule next run
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

                        boolean shouldNotify = false;

                        if (targetPrice > 0) {
                            // Notify only if current price is at or below target
                            if (currency.equals("USD") && usd <= targetPrice) {
                                shouldNotify = true;
                            } else if (currency.equals("BRL") && brl <= targetPrice) {
                                shouldNotify = true;
                            }
                        } else {
                            // No target price set, notify if price changed
                            if (brl != lastBrlPrice || usd != lastUsdPrice) {
                                shouldNotify = true;
                            }
                        }

                        if (shouldNotify) {
                            NotificationUtils.sendPriceNotification(getApplicationContext(), brl, usd);
                            lastBrlPrice = brl;
                            lastUsdPrice = usd;
                            saveLastPrices(brl, usd);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                Throwable::printStackTrace);

        queue.add(request);
    }

    private void saveLastPrices(double brl, double usd) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit()
                .putLong(KEY_BRL, Double.doubleToLongBits(brl))
                .putLong(KEY_USD, Double.doubleToLongBits(usd))
                .apply();
    }

    private void saveLastExecutionTime(long time) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit()
                .putLong(KEY_LAST_EXECUTION, time)
                .apply();
    }

    private void saveInterval(long interval) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit()
                .putLong(KEY_INTERVAL, interval)
                .apply();
    }

    private void saveTargetPrice(double price) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit()
                .putLong(KEY_TARGET_PRICE, Double.doubleToLongBits(price))
                .apply();
    }

    private void saveCurrency(String currency) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_CURRENCY, currency)
                .apply();
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        INTERVAL = prefs.getLong(KEY_INTERVAL, 5 * 60 * 1000); // Default to 5 minutes
        targetPrice = Double.longBitsToDouble(prefs.getLong(KEY_TARGET_PRICE, Double.doubleToLongBits(-1.0)));
        currency = prefs.getString(KEY_CURRENCY, "USD");
        lastExecutionTime = prefs.getLong(KEY_LAST_EXECUTION, 0);
        lastBrlPrice = Double.longBitsToDouble(prefs.getLong(KEY_BRL, Double.doubleToLongBits(-1.0)));
        lastUsdPrice = Double.longBitsToDouble(prefs.getLong(KEY_USD, Double.doubleToLongBits(-1.0)));
    }
}