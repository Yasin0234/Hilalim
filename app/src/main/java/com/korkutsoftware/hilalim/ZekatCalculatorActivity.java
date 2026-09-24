package com.korkutsoftware.hilalim;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;

import java.util.Locale;

public class ZekatCalculatorActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zekat_calculator);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        EditText etCash = findViewById(R.id.et_cash);
        EditText etGold = findViewById(R.id.et_gold);
        EditText etGoldPrice = findViewById(R.id.et_gold_price);
        TextView txtResult = findViewById(R.id.txt_result);

        findViewById(R.id.btn_calculate).setOnClickListener(v -> {
            String cashStr = etCash.getText().toString();
            String goldStr = etGold.getText().toString();
            String goldPriceStr = etGoldPrice.getText().toString();

            double cash = cashStr.isEmpty() ? 0 : Double.parseDouble(cashStr);
            double gold = goldStr.isEmpty() ? 0 : Double.parseDouble(goldStr);
            double goldPrice = goldPriceStr.isEmpty() ? 3000 : Double.parseDouble(goldPriceStr);

            // Nisap calculation: 80.18 gram gold
            double nisapThreshold = 80.18 * goldPrice;
            double totalWealth = cash + (gold * goldPrice);

            if (totalWealth >= nisapThreshold) {
                double zekat = totalWealth * 0.025; // 1/40 (2.5%)
                txtResult.setText(String.format(Locale.getDefault(), getString(R.string.zekat_result_amount), zekat));
            } else {
                txtResult.setText(String.format(Locale.getDefault(), getString(R.string.zekat_result_below_nisap), nisapThreshold));
            }
        });
    }
}
