
package com.droidev.bitcoinpriceapp;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
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
    private Spinner priceConditionSpinner;
    private EditText targetPriceEditText;
    private Button serviceToggleButton;
    private boolean isServiceRunning = false;
    private static final String[] CURRENCIES = {"USD", "BRL"};
    private static final String[] PRICE_CONDITION_VALUES = {"LESS_THAN_OR_EQUAL", "GREATER_THAN_OR_EQUAL"};
    private int selectedInterval;
    private String selectedCurrency = CURRENCIES[0];
    private String selectedPriceCondition = PRICE_CONDITION_VALUES[0];
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intervalSpinner = findViewById(R.id.intervalSpinner);
        currencySpinner = findViewById(R.id.currencySpinner);
        priceConditionSpinner = findViewById(R.id.priceConditionSpinner);
        targetPriceEditText = findViewById(R.id.targetPriceEditText);
        serviceToggleButton = findViewById(R.id.serviceToggleButton);

        prefs = getSharedPreferences("BitcoinPriceAppPrefs", MODE_PRIVATE);

        String savedTargetPrice = prefs.getString("targetPrice", "");
        if (!savedTargetPrice.isEmpty()) {
            targetPriceEditText.setText(savedTargetPrice);
        }

        int savedIntervalPosition = prefs.getInt("intervalPosition", 0);
        int savedCurrencyPosition = prefs.getInt("currencyPosition", 0);
        int savedPriceConditionPosition = prefs.getInt("priceConditionPosition", 0);

        isServiceRunning = isServiceRunning();
        updateButtonText();

        setupIntervalSpinner();
        setupCurrencySpinner();
        setupPriceConditionSpinner();

        intervalSpinner.setSelection(savedIntervalPosition);
        currencySpinner.setSelection(savedCurrencyPosition);
        priceConditionSpinner.setSelection(savedPriceConditionPosition);

        selectedInterval = getResources().getIntArray(R.array.interval_values)[savedIntervalPosition];
        selectedCurrency = CURRENCIES[savedCurrencyPosition];
        selectedPriceCondition = PRICE_CONDITION_VALUES[savedPriceConditionPosition];

        requestNotificationPermission();

        serviceToggleButton.setOnClickListener(v -> toggleService());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_calculator) {
            Intent intent = new Intent(this, CalculatorActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        serviceToggleButton.setText(isServiceRunning ? R.string.stop_service : R.string.start_service);
    }

    private void setupIntervalSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.intervals, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        intervalSpinner.setAdapter(adapter);

        intervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedInterval = getResources().getIntArray(R.array.interval_values)[position];

                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("intervalPosition", position);
                editor.apply();
                if (isServiceRunning) {
                    stopService();

                    String targetPriceStr = targetPriceEditText.getText().toString();
                    double targetPrice = -1.0;

                    if (!targetPriceStr.isEmpty()) {
                        try {
                            targetPrice = Double.parseDouble(targetPriceStr);
                            if (targetPrice <= 0) {
                                Toast.makeText(MainActivity.this, R.string.invalid_target_price_default, Toast.LENGTH_SHORT).show();
                                targetPrice = -1.0;
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(MainActivity.this, R.string.invalid_target_price_format, Toast.LENGTH_SHORT).show();
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

                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("currencyPosition", position);
                editor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupPriceConditionSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.price_conditions, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priceConditionSpinner.setAdapter(adapter);

        priceConditionSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedPriceCondition = PRICE_CONDITION_VALUES[position];

                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("priceConditionPosition", position);
                editor.apply();
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
                        Toast.makeText(this, R.string.invalid_target_price, Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, R.string.invalid_target_price_format, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("targetPrice", targetPriceStr);
            editor.apply();

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
        serviceIntent.putExtra("priceCondition", selectedPriceCondition);
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