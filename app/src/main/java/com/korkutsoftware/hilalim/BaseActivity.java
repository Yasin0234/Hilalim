package com.korkutsoftware.hilalim;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import com.korkutsoftware.hilalim.util.LocaleHelper;

public class BaseActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
}