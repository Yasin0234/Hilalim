package com.korkutsoftware.hilalim;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends BaseActivity {

    private final Handler splashHandler = new Handler(Looper.getMainLooper());
    private final Runnable splashRunnable = () -> {
        if (isFinishing() || isDestroyed()) return;
        SharedPreferences prefs = getSharedPreferences("hilalim_prefs", MODE_PRIVATE);
        boolean onboardingCompleted = prefs.getBoolean("onboarding_completed", false);

        Intent intent;
        if (onboardingCompleted) {
            intent = new Intent(MainActivity.this, Dashboard.class);
        } else {
            intent = new Intent(MainActivity.this, WelcomeActivity.class);
        }
        startActivity(intent);
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Splash delay and redirection
        splashHandler.postDelayed(splashRunnable, 2500); // 2.5 seconds splash
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        splashHandler.removeCallbacks(splashRunnable);
    }
}