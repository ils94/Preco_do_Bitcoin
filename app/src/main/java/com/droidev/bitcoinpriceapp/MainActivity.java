package com.droidev.bitcoinpriceapp;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_CODE = 101;
    private Spinner intervalSpinner;
    private Spinner currencySpinner;
    private EditText targetPriceEditText;
    private Button serviceToggleButton;
    private boolean isServiceRunning = false;
    private static final String[] INTERVALS = {"5 minutos", "10 minutos", "15 minutos", "30 minutos", "1 hora"};
    private static final int[] INTERVAL_VALUES = {5 * 60 * 1000, 10 * 60 * 1000, 15 * 60 * 1000, 30 * 60 * 1000, 60 * 60 * 1000};
    private static final String[] CURRENCIES = {"USD", "BRL"};
    private int selectedInterval = INTERVAL_VALUES[0];
    private String selectedCurrency = CURRENCIES[0];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intervalSpinner = findViewById(R.id.intervalSpinner);
        currencySpinner = findViewById(R.id.currencySpinner);
        targetPriceEditText = findViewById(R.id.targetPriceEditText);
        serviceToggleButton = findViewById(R.id.serviceToggleButton);

        isServiceRunning = isServiceRunning();
        updateButtonText();

        setupIntervalSpinner();
        setupCurrencySpinner();
        requestNotificationPermission();

        serviceToggleButton.setOnClickListener(v -> toggleService());
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BitcoinService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void updateButtonText() {
        serviceToggleButton.setText(isServiceRunning ? "Parar Serviço" : "Iniciar Serviço");
    }

    private void setupIntervalSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, INTERVALS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        intervalSpinner.setAdapter(adapter);

        intervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedInterval = INTERVAL_VALUES[position];
                if (isServiceRunning) {
                    stopService();
                    // Get target price from EditText or use default
                    String targetPriceStr = targetPriceEditText.getText().toString();
                    double targetPrice = -1.0;
                    if (!targetPriceStr.isEmpty()) {
                        try {
                            targetPrice = Double.parseDouble(targetPriceStr);
                            if (targetPrice <= 0) {
                                Toast.makeText(MainActivity.this, "Preço alvo inválido, usando padrão.", Toast.LENGTH_SHORT).show();
                                targetPrice = -1.0;
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(MainActivity.this, "Preço alvo inválido, usando padrão.", Toast.LENGTH_SHORT).show();
                        }
                    }
                    startService(targetPrice);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupCurrencySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CURRENCIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(adapter);

        currencySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCurrency = CURRENCIES[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void toggleService() {
        if (isServiceRunning) {
            stopService();
            isServiceRunning = false;
        } else {
            String targetPriceStr = targetPriceEditText.getText().toString();
            double targetPrice = -1.0;
            if (!targetPriceStr.isEmpty()) {
                try {
                    targetPrice = Double.parseDouble(targetPriceStr);
                    if (targetPrice <= 0) {
                        Toast.makeText(this, "Por favor, insira um preço válido maior que 0.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Por favor, insira um preço numérico válido.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            startService(targetPrice);
            isServiceRunning = true;
        }
        updateButtonText();
    }

    private void startService(double targetPrice) {
        Intent serviceIntent = new Intent(this, BitcoinService.class);
        serviceIntent.putExtra("interval", selectedInterval);
        serviceIntent.putExtra("targetPrice", targetPrice);
        serviceIntent.putExtra("currency", selectedCurrency);
        startService(serviceIntent);
    }

    private void stopService() {
        Intent serviceIntent = new Intent(this, BitcoinService.class);
        stopService(serviceIntent);
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}