package com.droidev.bitcoinpriceapp;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Locale;

public class CalculatorActivity extends AppCompatActivity {

    private EditText amountEditText;
    private Spinner currencySpinner;
    private CheckBox useSatsCheckBox;
    private Spinner transactionTypeSpinner;
    private EditText dealerFeeEditText;
    private Button calculateButton;
    private TextView resultTextView;
    private RequestQueue queue;

    private double btcToUsd = -1.0;
    private double btcToBrl = -1.0;
    private double networkFeeSatPerVByte = -1.0; // Network fee in sat/vByte
    private long lastFetchTime = 0;
    private static final long CACHE_DURATION = 60 * 1000; // 1 minute in milliseconds
    private static final String[] TRANSACTION_TYPES = {"Buy", "Sell"}; // Internal values, not localized
    private static final double SATS_PER_BTC = 100_000_000.0;
    private static final double TX_SIZE_VBYTES = 141.0; // Typical 1-in/2-out native SegWit transaction

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        amountEditText = findViewById(R.id.amountEditText);
        currencySpinner = findViewById(R.id.currencySpinner);
        useSatsCheckBox = findViewById(R.id.useSatsCheckBox);
        transactionTypeSpinner = findViewById(R.id.transactionTypeSpinner);
        dealerFeeEditText = findViewById(R.id.dealerFeeEditText);
        calculateButton = findViewById(R.id.calculateButton);
        resultTextView = findViewById(R.id.resultTextView);

        queue = Volley.newRequestQueue(this);

        setupCurrencySpinner();
        setupTransactionTypeSpinner();

        calculateButton.setOnClickListener(v -> performCalculation());
    }

    private void setupCurrencySpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.calculator_currencies, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        currencySpinner.setAdapter(adapter);
    }

    private void setupTransactionTypeSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.transaction_types, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        transactionTypeSpinner.setAdapter(adapter);
    }

    private void performCalculation() {
        String amountStr = amountEditText.getText().toString();
        String dealerFeeStr = dealerFeeEditText.getText().toString();
        String selectedCurrency = currencySpinner.getSelectedItem().toString();
        String transactionType = TRANSACTION_TYPES[transactionTypeSpinner.getSelectedItemPosition()];
        boolean useSats = useSatsCheckBox.isChecked();

        try {
            double amount = amountStr.isEmpty() ? 0.0 : Double.parseDouble(amountStr);
            if (amount < 0) {
                Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
                return;
            }

            double dealerFeePercent = dealerFeeStr.isEmpty() ? 0.0 : Double.parseDouble(dealerFeeStr);
            if (dealerFeePercent < 0) {
                Toast.makeText(this, R.string.invalid_fee, Toast.LENGTH_SHORT).show();
                return;
            }

            if (System.currentTimeMillis() - lastFetchTime > CACHE_DURATION || btcToUsd == -1.0 || networkFeeSatPerVByte == -1.0) {
                fetchData(amount, dealerFeePercent, selectedCurrency, transactionType, useSats);
            } else {
                calculateResult(amount, dealerFeePercent, selectedCurrency, transactionType, useSats);
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.invalid_amount, Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchData(double amount, double dealerFeePercent, String selectedCurrency, String transactionType, boolean useSats) {
        // Fetch Bitcoin price from CoinGecko
        String priceUrl = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=brl,usd";
        StringRequest priceRequest = new StringRequest(Request.Method.GET, priceUrl,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        JSONObject btc = json.getJSONObject("bitcoin");
                        btcToBrl = btc.getDouble("brl");
                        btcToUsd = btc.getDouble("usd");
                        fetchNetworkFee(amount, dealerFeePercent, selectedCurrency, transactionType, useSats);
                    } catch (Exception e) {
                        Toast.makeText(this, R.string.error_fetching_price, Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> {
                    Toast.makeText(this, R.string.error_fetching_price, Toast.LENGTH_SHORT).show();
                    error.printStackTrace();
                });

        // Add price request to queue
        queue.add(priceRequest);
    }

    private void fetchNetworkFee(double amount, double dealerFeePercent, String selectedCurrency, String transactionType, boolean useSats) {
        // Fetch network fee from Mempool API
        String feeUrl = "https://mempool.space/api/v1/fees/recommended";
        StringRequest feeRequest = new StringRequest(Request.Method.GET, feeUrl,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        networkFeeSatPerVByte = json.getDouble("hourFee"); // Use fee for ~1 hour confirmation
                        lastFetchTime = System.currentTimeMillis();
                        calculateResult(amount, dealerFeePercent, selectedCurrency, transactionType, useSats);
                    } catch (Exception e) {
                        Toast.makeText(this, "Error fetching network fee", Toast.LENGTH_SHORT).show();
                        networkFeeSatPerVByte = 1.0; // Fallback to 1 sat/vByte
                        calculateResult(amount, dealerFeePercent, selectedCurrency, transactionType, useSats);
                    }
                },
                error -> {
                    Toast.makeText(this, "Error fetching network fee", Toast.LENGTH_SHORT).show();
                    networkFeeSatPerVByte = 1.0; // Fallback to 1 sat/vByte
                    calculateResult(amount, dealerFeePercent, selectedCurrency, transactionType, useSats);
                });

        // Add fee request to queue
        queue.add(feeRequest);
    }

    @SuppressLint("DefaultLocale")
    private void calculateResult(double amount, double dealerFeePercent, String selectedCurrency, String transactionType, boolean useSats) {
        NumberFormat usdFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
        NumberFormat brlFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        StringBuilder result = new StringBuilder();

        // Get localized transaction type names
        String[] transactionTypeNames = getResources().getStringArray(R.array.transaction_types);
        String localizedTransactionType = transactionType.equals("Buy") ? transactionTypeNames[0] : transactionTypeNames[1];

        // Calculate network fee in SATs and convert to BTC and USD/BRL
        double networkFeeSats = networkFeeSatPerVByte * TX_SIZE_VBYTES;
        double networkFeeBtc = networkFeeSats / SATS_PER_BTC;
        double networkFeeUsd = networkFeeBtc * btcToUsd;
        double networkFeeBrl = networkFeeBtc * btcToBrl;

        // Convert SATs to BTC if selected
        double effectiveAmount = useSats && selectedCurrency.equals("BTC") ? amount / SATS_PER_BTC : amount;

        if (selectedCurrency.equals("BTC")) {
            // BTC (or SATs) to USD/BRL
            double usdValue = effectiveAmount * btcToUsd;
            double brlValue = effectiveAmount * btcToBrl;
            String cryptoUnit = useSats ? "SATs" : "BTC";
            String formattedCryptoAmount = useSats ? formatSats(amount) : formatAmount(effectiveAmount);
            result.append(String.format("%s %s = %s%n", formattedCryptoAmount, cryptoUnit, usdFormatter.format(usdValue)));
            result.append(String.format("%s %s = %s%n", formattedCryptoAmount, cryptoUnit, brlFormatter.format(brlValue)));

            // BTC (or SATs) with dealer fee and network fee to USD/BRL
            double feeMultiplier = transactionType.equals("Buy") ? (1 + (dealerFeePercent / 100)) : (1 - (dealerFeePercent / 100));
            double usdValueWithDealerFee = usdValue * feeMultiplier;
            double brlValueWithDealerFee = brlValue * feeMultiplier;
            double usdValueFinal = usdValueWithDealerFee + (transactionType.equals("Buy") ? networkFeeUsd : -networkFeeUsd);
            double brlValueFinal = brlValueWithDealerFee + (transactionType.equals("Buy") ? networkFeeBrl : -networkFeeBrl);
            result.append(String.format("Network fee: %s SATs (~%s | %s)%n", formatSats(networkFeeSats),
                    usdFormatter.format(networkFeeUsd), brlFormatter.format(networkFeeBrl)));
            result.append(String.format("%s %s (%s, %.2f%% dealer fee, network fee %s SATs) = %s%n",
                    formattedCryptoAmount, cryptoUnit, localizedTransactionType,
                    dealerFeePercent, formatSats(networkFeeSats), usdFormatter.format(usdValueFinal)));
            result.append(String.format("%s %s (%s, %.2f%% dealer fee, network fee %s SATs) = %s%n",
                    formattedCryptoAmount, cryptoUnit, localizedTransactionType,
                    dealerFeePercent, formatSats(networkFeeSats), brlFormatter.format(brlValueFinal)));
        } else {
            // USD or BRL to BTC (or SATs)
            double btcValue;
            if (selectedCurrency.equals("USD")) {
                btcValue = effectiveAmount / btcToUsd;
            } else { // BRL
                btcValue = effectiveAmount / btcToBrl;
            }
            String cryptoUnit = useSats ? "SATs" : "BTC";
            double displayCryptoValue = useSats ? btcValue * SATS_PER_BTC : btcValue;
            String formattedCryptoValue = useSats ? formatSats(displayCryptoValue) : formatAmount(btcValue);
            result.append(String.format("%s %s = %s %s%n",
                    selectedCurrency.equals("USD") ? usdFormatter.format(effectiveAmount) : brlFormatter.format(effectiveAmount),
                    selectedCurrency, formattedCryptoValue, cryptoUnit));

            // USD or BRL with dealer fee and network fee to BTC (or SATs)
            double dealerFeeBtc = btcValue * (dealerFeePercent / 100);
            double btcValueFinal = btcValue - dealerFeeBtc - networkFeeBtc;
            double displayCryptoValueFinal = useSats ? btcValueFinal * SATS_PER_BTC : btcValueFinal;
            String formattedCryptoValueFinal = useSats ? formatSats(displayCryptoValueFinal) : formatAmount(btcValueFinal);
            result.append(String.format("Network fee: %s SATs (~%s | %s)%n", formatSats(networkFeeSats),
                    usdFormatter.format(networkFeeUsd), brlFormatter.format(networkFeeBrl)));
            result.append(String.format("%s %s (%s, %.2f%% dealer fee, network fee %s SATs) = %s %s%n",
                    selectedCurrency.equals("USD") ? usdFormatter.format(effectiveAmount) : brlFormatter.format(effectiveAmount),
                    selectedCurrency, localizedTransactionType,
                    dealerFeePercent, formatSats(networkFeeSats), formattedCryptoValueFinal, cryptoUnit));
        }

        resultTextView.setText(result.toString());
        resultTextView.setVisibility(View.VISIBLE);
    }

    private String formatAmount(double amount) {
        return String.format(Locale.US, "%.8f", amount).replaceAll("\\.?0+$", "");
    }

    private String formatSats(double amount) {
        return String.format(Locale.US, "%.0f", amount);
    }
}