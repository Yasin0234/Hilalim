package com.korkutsoftware.hilalim;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;

import com.korkutsoftware.hilalim.util.AppCalculators;

import java.util.Locale;

public class ZekatCalculatorActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zekat_calculator);
        applySystemBarInsets(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        EditText etCash = findViewById(R.id.et_cash);
        EditText etGold = findViewById(R.id.et_gold);
        EditText etGoldPrice = findViewById(R.id.et_gold_price);
        TextView txtResult = findViewById(R.id.txt_result);

        findViewById(R.id.btn_calculate).setOnClickListener(v -> {
            Double cash = parseDoubleSafe(etCash.getText() != null ? etCash.getText().toString() : "", 0.0);
            Double gold = parseDoubleSafe(etGold.getText() != null ? etGold.getText().toString() : "", 0.0);
            Double goldPrice = parseDoubleSafe(etGoldPrice.getText() != null ? etGoldPrice.getText().toString() : "", 3000.0);

            if (cash == null || gold == null || goldPrice == null || cash < 0 || gold < 0 || goldPrice <= 0) {
                Toast.makeText(this, R.string.zekat_invalid_input, Toast.LENGTH_SHORT).show();
                return;
            }

            AppCalculators.ZekatResult result = AppCalculators.calculateZekat(cash, gold, goldPrice);

            if (result.isEligible) {
                txtResult.setText(String.format(Locale.getDefault(), getString(R.string.zekat_result_amount), result.zekatAmount));
            } else {
                txtResult.setText(String.format(Locale.getDefault(), getString(R.string.zekat_result_below_nisap), result.nisapThreshold));
            }
        });
    }

    private Double parseDoubleSafe(String raw, double defaultValue) {
        String trimmed = raw.trim().replace(',', '.');
        if (trimmed.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
