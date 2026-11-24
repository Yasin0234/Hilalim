package com.korkutsoftware.hilalim;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.gms.ads.AdView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.korkutsoftware.hilalim.util.AdManager;
import com.korkutsoftware.hilalim.util.LocaleHelper;

public class Dashboard extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private BottomNavigationView bottomNav;
    private NavigationView navigationView;
    private String currentLang;
    private AdView bannerAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentLang = LocaleHelper.getLanguage(this);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.drawer_open, R.string.drawer_close);
        toggle.setDrawerIndicatorEnabled(false);
        toggle.setHomeAsUpIndicator(R.drawable.ic_menu);
        toggle.setToolbarNavigationClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        toolbar.setNavigationIcon(R.drawable.ic_menu);

        TextView txtDrawerVersion = findViewById(R.id.txt_drawer_version);
        if (txtDrawerVersion != null) {
            txtDrawerVersion.setText(getString(R.string.app_version_format, getAppVersionName()));
        }

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_quran) {
                selectedFragment = new QuranFragment();
            } else if (itemId == R.id.nav_qibla) {
                selectedFragment = new KibleFragment();
            } else if (itemId == R.id.nav_tools) {
                selectedFragment = new AraclarFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                navigationView.setCheckedItem(itemId);
            }
            return true;
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            bottomNav.setSelectedItemId(R.id.nav_home);
            navigationView.setCheckedItem(R.id.nav_home);
        }

        FrameLayout adContainer = findViewById(R.id.ad_banner_container);
        if (adContainer != null) {
            bannerAdView = AdManager.loadBanner(this, adContainer);
        }

        View mainContent = findViewById(R.id.main_content);
        View drawerFooter = findViewById(R.id.drawer_footer);
        View navHeader = navigationView.getHeaderCount() > 0 ? navigationView.getHeaderView(0) : null;
        int navHeaderTopBase = navHeader != null ? navHeader.getPaddingTop() : 0;
        int footerBottomBase = drawerFooter != null ? drawerFooter.getPaddingBottom() : 0;

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.drawer_layout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (mainContent != null) {
                mainContent.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            }
            if (navHeader != null) {
                navHeader.setPadding(
                        navHeader.getPaddingLeft(),
                        navHeaderTopBase + systemBars.top,
                        navHeader.getPaddingRight(),
                        navHeader.getPaddingBottom());
            }
            if (drawerFooter != null) {
                drawerFooter.setPadding(
                        drawerFooter.getPaddingLeft(),
                        drawerFooter.getPaddingTop(),
                        drawerFooter.getPaddingRight(),
                        footerBottomBase + systemBars.bottom);
            }
            return insets;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        boolean checkableDestination = true;

        if (id == R.id.nav_home) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        } else if (id == R.id.nav_quran) {
            bottomNav.setSelectedItemId(R.id.nav_quran);
        } else if (id == R.id.nav_qibla) {
            bottomNav.setSelectedItemId(R.id.nav_qibla);
        } else if (id == R.id.nav_tools) {
            bottomNav.setSelectedItemId(R.id.nav_tools);
        } else if (id == R.id.nav_notifications) {
            checkableDestination = false;
            startActivity(new Intent(this, NotificationSettingsActivity.class));
        } else if (id == R.id.nav_location) {
            checkableDestination = false;
            startActivity(new Intent(this, LocationActivity.class));
        } else if (id == R.id.nav_language) {
            checkableDestination = false;
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.putExtra("language_only", true);
            startActivity(intent);
        } else if (id == R.id.nav_about) {
            checkableDestination = false;
            startActivity(new Intent(this, AboutActivity.class));
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return checkableDestination;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bannerAdView != null) {
            bannerAdView.resume();
        }
        if (currentLang != null && !currentLang.equals(LocaleHelper.getLanguage(this))) {
            recreate();
        }
    }

    @Override
    protected void onPause() {
        if (bannerAdView != null) {
            bannerAdView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (bannerAdView != null) {
            bannerAdView.destroy();
        }
        super.onDestroy();
    }
}
