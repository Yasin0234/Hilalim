package com.korkutsoftware.hilalim;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;

public class AboutActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about);
        applySystemBarInsets(findViewById(android.R.id.content));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        String versionText = getString(R.string.app_version_format, getAppVersionName());
        TextView txtHeroVersion = findViewById(R.id.txt_about_hero_version);
        if (txtHeroVersion != null) {
            txtHeroVersion.setText(versionText);
        }
        TextView txtFooterVersion = findViewById(R.id.txt_about_footer_version);
        if (txtFooterVersion != null) {
            txtFooterVersion.setText(versionText);
        }
    }

    private String getAppVersionName() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (pInfo != null && pInfo.versionName != null) {
                return pInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return BuildConfig.VERSION_NAME;
    }
}
