package com.korkutsoftware.hilalim.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PrayerLogDao {

    @Query("SELECT * FROM prayer_logs WHERE dateKey = :dateKey")
    List<PrayerLogEntity> getLogsForDate(String dateKey);

    @Query("SELECT * FROM prayer_logs WHERE dateKey = :dateKey AND prayerKey = :prayerKey LIMIT 1")
    PrayerLogEntity getLog(String dateKey, String prayerKey);

    @Query("SELECT * FROM prayer_logs WHERE dateKey BETWEEN :startDateKey AND :endDateKey")
    List<PrayerLogEntity> getLogsBetween(String startDateKey, String endDateKey);

    @Query("SELECT * FROM prayer_logs ORDER BY dateKey DESC")
    List<PrayerLogEntity> getAllLogs();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(PrayerLogEntity log);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateAll(List<PrayerLogEntity> logs);

    @Query("DELETE FROM prayer_logs WHERE dateKey = :dateKey AND prayerKey = :prayerKey")
    void deleteLog(String dateKey, String prayerKey);
}
