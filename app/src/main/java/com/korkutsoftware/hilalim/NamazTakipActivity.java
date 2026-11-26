package com.korkutsoftware.hilalim;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.korkutsoftware.hilalim.data.db.AppDatabase;
import com.korkutsoftware.hilalim.data.db.PrayerLogDao;
import com.korkutsoftware.hilalim.data.db.PrayerLogEntity;
import com.korkutsoftware.hilalim.util.LocaleHelper;
import com.korkutsoftware.hilalim.util.PrayerStreakHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NamazTakipActivity extends BaseActivity {

    private TextView txtStreakCount, txtMonthlyRate, txtSelectedDate;
    private LinearLayout layoutWeeklyIndicator, layoutPrayerCardsContainer;
    private Calendar selectedCalendar = Calendar.getInstance();

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PrayerLogDao prayerLogDao;

    private static class PrayerConfig {
        final String key;
        final int titleResId;
        final int iconResId;

        PrayerConfig(String key, int titleResId, int iconResId) {
            this.key = key;
            this.titleResId = titleResId;
            this.iconResId = iconResId;
        }
    }

    private final PrayerConfig[] prayers = new PrayerConfig[]{
            new PrayerConfig(PrayerStreakHelper.FAJR, R.string.imsak, R.drawable.ic_imsak),
            new PrayerConfig(PrayerStreakHelper.DHUHR, R.string.ogle, R.drawable.ic_ogle),
            new PrayerConfig(PrayerStreakHelper.ASR, R.string.ikindi, R.drawable.ic_ikindi),
            new PrayerConfig(PrayerStreakHelper.MAGHRIB, R.string.aksam, R.drawable.ic_aksam),
            new PrayerConfig(PrayerStreakHelper.ISHA, R.string.yatsi, R.drawable.ic_yatsi),
            new PrayerConfig(PrayerStreakHelper.WITR, R.string.vakit_vitir, R.drawable.ic_pray)
    };

    private final Map<String, View> prayerCardViews = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_namaz_takip);
        applySystemBarInsets(findViewById(android.R.id.content));

        prayerLogDao = AppDatabase.getInstance(this).prayerLogDao();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_today).setOnClickListener(v -> {
            selectedCalendar = Calendar.getInstance();
            loadDateAndStats();
        });

        txtStreakCount = findViewById(R.id.txt_streak_count);
        txtMonthlyRate = findViewById(R.id.txt_monthly_rate);
        txtSelectedDate = findViewById(R.id.txt_selected_date);
        layoutWeeklyIndicator = findViewById(R.id.layout_weekly_indicator);
        layoutPrayerCardsContainer = findViewById(R.id.layout_prayer_cards_container);

        findViewById(R.id.btn_prev_day).setOnClickListener(v -> {
            selectedCalendar.add(Calendar.DAY_OF_YEAR, -1);
            loadDateAndStats();
        });

        findViewById(R.id.btn_next_day).setOnClickListener(v -> {
            selectedCalendar.add(Calendar.DAY_OF_YEAR, 1);
            loadDateAndStats();
        });

        buildPrayerCards();
        loadDateAndStats();
    }

    private void buildPrayerCards() {
        layoutPrayerCardsContainer.removeAllViews();
        prayerCardViews.clear();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (PrayerConfig config : prayers) {
            View card = inflater.inflate(R.layout.item_prayer_track, layoutPrayerCardsContainer, false);
            ImageView imgIcon = card.findViewById(R.id.img_prayer_icon);
            TextView txtTitle = card.findViewById(R.id.txt_prayer_title);

            imgIcon.setImageResource(config.iconResId);
            txtTitle.setText(config.titleResId);

            MaterialButton btnPrayed = card.findViewById(R.id.btn_action_prayed);
            MaterialButton btnCongregation = card.findViewById(R.id.btn_action_congregation);
            MaterialButton btnKaza = card.findViewById(R.id.btn_action_kaza);

            btnPrayed.setOnClickListener(v -> updatePrayerStatus(config.key, PrayerLogEntity.STATUS_PRAYED));
            btnCongregation.setOnClickListener(v -> updatePrayerStatus(config.key, PrayerLogEntity.STATUS_CONGREGATION));
            btnKaza.setOnClickListener(v -> updatePrayerStatus(config.key, PrayerLogEntity.STATUS_KAZA));

            prayerCardViews.put(config.key, card);
            layoutPrayerCardsContainer.addView(card);
        }
    }

    private void updatePrayerStatus(String prayerKey, int targetStatus) {
        vibrateLight();
        String dateKey = PrayerStreakHelper.DATE_FORMAT.format(selectedCalendar.getTime());

        executor.execute(() -> {
            PrayerLogEntity existing = prayerLogDao.getLog(dateKey, prayerKey);
            int newStatus = (existing != null && existing.status == targetStatus)
                    ? PrayerLogEntity.STATUS_WAITING
                    : targetStatus;

            PrayerLogEntity updated = new PrayerLogEntity(dateKey, prayerKey, newStatus, System.currentTimeMillis());
            prayerLogDao.insertOrUpdate(updated);

            mainHandler.post(this::loadDateAndStats);
        });
    }

    private void loadDateAndStats() {
        updateDateHeader();

        String selectedDateKey = PrayerStreakHelper.DATE_FORMAT.format(selectedCalendar.getTime());

        Calendar monthStart = (Calendar) selectedCalendar.clone();
        monthStart.set(Calendar.DAY_OF_MONTH, 1);
        String monthStartKey = PrayerStreakHelper.DATE_FORMAT.format(monthStart.getTime());

        Calendar monthEnd = (Calendar) selectedCalendar.clone();
        monthEnd.set(Calendar.DAY_OF_MONTH, monthEnd.getActualMaximum(Calendar.DAY_OF_MONTH));
        String monthEndKey = PrayerStreakHelper.DATE_FORMAT.format(monthEnd.getTime());

        executor.execute(() -> {
            List<PrayerLogEntity> dayLogs = prayerLogDao.getLogsForDate(selectedDateKey);
            List<PrayerLogEntity> monthLogs = prayerLogDao.getLogsBetween(monthStartKey, monthEndKey);
            List<PrayerLogEntity> allLogs = prayerLogDao.getAllLogs();

            // Last 7 days for weekly indicator
            Calendar weekStart = Calendar.getInstance();
            weekStart.add(Calendar.DAY_OF_YEAR, -6);
            String weekStartKey = PrayerStreakHelper.DATE_FORMAT.format(weekStart.getTime());
            String todayKey = PrayerStreakHelper.DATE_FORMAT.format(Calendar.getInstance().getTime());
            List<PrayerLogEntity> last7DaysLogs = prayerLogDao.getLogsBetween(weekStartKey, todayKey);

            int streak = PrayerStreakHelper.calculateStreak(allLogs, Calendar.getInstance());
            int dayOfMonth = selectedCalendar.get(Calendar.DAY_OF_MONTH);
            int monthlyRate = PrayerStreakHelper.calculateMonthlySuccessRate(monthLogs, dayOfMonth);

            mainHandler.post(() -> {
                bindDayLogs(dayLogs);
                txtStreakCount.setText(getString(R.string.streak_format, streak));
                txtMonthlyRate.setText(getString(R.string.monthly_rate_format, monthlyRate));
                renderWeeklyDots(last7DaysLogs);
            });
        });
    }

    private void bindDayLogs(List<PrayerLogEntity> dayLogs) {
        Map<String, Integer> statusMap = new HashMap<>();
        if (dayLogs != null) {
            for (PrayerLogEntity log : dayLogs) {
                statusMap.put(log.prayerKey, log.status);
            }
        }

        for (PrayerConfig config : prayers) {
            View card = prayerCardViews.get(config.key);
            if (card == null) continue;

            int status = statusMap.containsKey(config.key) ? statusMap.get(config.key) : PrayerLogEntity.STATUS_WAITING;

            TextView badge = card.findViewById(R.id.txt_status_badge);
            MaterialButton btnPrayed = card.findViewById(R.id.btn_action_prayed);
            MaterialButton btnCongregation = card.findViewById(R.id.btn_action_congregation);
            MaterialButton btnKaza = card.findViewById(R.id.btn_action_kaza);

            resetButtonStyle(btnPrayed);
            resetButtonStyle(btnCongregation);
            resetButtonStyle(btnKaza);

            int gold = ContextCompat.getColor(this, R.color.gold);
            int charcoal = ContextCompat.getColor(this, R.color.deep_charcoal);

            switch (status) {
                case PrayerLogEntity.STATUS_PRAYED:
                    badge.setText(R.string.status_prayed);
                    badge.setBackgroundResource(R.drawable.bg_gold_gradient);
                    badge.setTextColor(charcoal);
                    setActiveButtonStyle(btnPrayed, gold, charcoal);
                    break;
                case PrayerLogEntity.STATUS_CONGREGATION:
                    badge.setText(R.string.status_congregation);
                    badge.setBackgroundResource(R.drawable.bg_gold_gradient);
                    badge.setTextColor(charcoal);
                    setActiveButtonStyle(btnCongregation, gold, charcoal);
                    break;
                case PrayerLogEntity.STATUS_KAZA:
                    badge.setText(R.string.status_kaza);
                    badge.setBackgroundResource(R.drawable.bg_countdown_pill);
                    badge.setTextColor(ContextCompat.getColor(this, R.color.white));
                    setActiveButtonStyle(btnKaza, gold, charcoal);
                    break;
                default:
                    badge.setText(R.string.status_waiting);
                    badge.setBackgroundResource(R.drawable.bg_countdown_pill);
                    badge.setTextColor(ContextCompat.getColor(this, R.color.white));
                    break;
            }
        }
    }

    private void setActiveButtonStyle(MaterialButton btn, int bgColor, int textColor) {
        btn.setBackgroundColor(bgColor);
        btn.setTextColor(textColor);
        btn.setStrokeColorResource(android.R.color.transparent);
    }

    private void resetButtonStyle(MaterialButton btn) {
        btn.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));
        btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        btn.setStrokeColorResource(R.color.card_stroke);
    }

    private void renderWeeklyDots(List<PrayerLogEntity> last7DaysLogs) {
        layoutWeeklyIndicator.removeAllViews();

        Map<String, List<PrayerLogEntity>> map = new HashMap<>();
        if (last7DaysLogs != null) {
            for (PrayerLogEntity log : last7DaysLogs) {
                if (!map.containsKey(log.dateKey)) {
                    map.put(log.dateKey, new java.util.ArrayList<>());
                }
                map.get(log.dateKey).add(log);
            }
        }

        Calendar cursor = Calendar.getInstance();
        cursor.add(Calendar.DAY_OF_YEAR, -6);

        String lang = LocaleHelper.getLanguage(this);
        SimpleDateFormat dayFormat = new SimpleDateFormat("EE", new Locale(lang));

        int goldColor = ContextCompat.getColor(this, R.color.gold);
        int mutedColor = ContextCompat.getColor(this, R.color.text_muted);

        for (int i = 0; i < 7; i++) {
            String dateKey = PrayerStreakHelper.DATE_FORMAT.format(cursor.getTime());
            List<PrayerLogEntity> dayList = map.get(dateKey);
            int completedCount = PrayerStreakHelper.getDayCompletedCount(dayList);

            LinearLayout dotCol = new LinearLayout(this);
            dotCol.setOrientation(LinearLayout.VERTICAL);
            dotCol.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            dotCol.setLayoutParams(params);

            TextView txtDay = new TextView(this);
            String dayInitial = dayFormat.format(cursor.getTime());
            if (!dayInitial.isEmpty()) {
                dayInitial = dayInitial.substring(0, 1).toUpperCase(new Locale(lang));
            }
            txtDay.setText(dayInitial);
            txtDay.setTextColor(mutedColor);
            txtDay.setTextSize(11);
            txtDay.setGravity(android.view.Gravity.CENTER);

            TextView txtDot = new TextView(this);
            if (completedCount >= 5) {
                txtDot.setText("●");
                txtDot.setTextColor(goldColor);
            } else if (completedCount > 0) {
                txtDot.setText("◐");
                txtDot.setTextColor(goldColor);
            } else {
                txtDot.setText("○");
                txtDot.setTextColor(mutedColor);
            }
            txtDot.setTextSize(18);
            txtDot.setGravity(android.view.Gravity.CENTER);

            dotCol.addView(txtDay);
            dotCol.addView(txtDot);
            layoutWeeklyIndicator.addView(dotCol);

            cursor.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void updateDateHeader() {
        String lang = LocaleHelper.getLanguage(this);
        SimpleDateFormat sdf = new SimpleDateFormat("d MMMM yyyy", new Locale(lang));
        String formatted = sdf.format(selectedCalendar.getTime());

        Calendar today = Calendar.getInstance();
        boolean isToday = selectedCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR)
                && selectedCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);

        if (isToday) {
            formatted = formatted + " (" + getString(R.string.today_label) + ")";
        }
        txtSelectedDate.setText(formatted);
    }

    private void vibrateLight() {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(30);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
