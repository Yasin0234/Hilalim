package com.korkutsoftware.hilalim.data.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;

@Entity(tableName = "prayer_logs", primaryKeys = {"dateKey", "prayerKey"})
public class PrayerLogEntity {

    public static final int STATUS_WAITING = 0;
    public static final int STATUS_PRAYED = 1;
    public static final int STATUS_CONGREGATION = 2;
    public static final int STATUS_KAZA = 3;

    @NonNull
    public String dateKey;   // "yyyy-MM-dd", e.g. "2026-11-26"

    @NonNull
    public String prayerKey; // "fajr", "dhuhr", "asr", "maghrib", "isha", "witr"

    public int status;       // 0: Bekliyor, 1: Kılındı, 2: Cemaatle, 3: Kaza

    public long updatedAt;

    public PrayerLogEntity(@NonNull String dateKey, @NonNull String prayerKey, int status, long updatedAt) {
        this.dateKey = dateKey;
        this.prayerKey = prayerKey;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    @Ignore
    public PrayerLogEntity() {
        this.dateKey = "";
        this.prayerKey = "";
        this.status = STATUS_WAITING;
        this.updatedAt = System.currentTimeMillis();
    }
}
