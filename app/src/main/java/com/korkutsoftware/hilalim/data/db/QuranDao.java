package com.korkutsoftware.hilalim.data.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface QuranDao {

    @Query("SELECT * FROM quran_verses WHERE surahId = :surahId ORDER BY verseId ASC")
    List<QuranVerseEntity> getVersesBySurah(int surahId);

    @Query("SELECT * FROM quran_verses WHERE juz = :juz ORDER BY surahId ASC, verseId ASC")
    List<QuranVerseEntity> getVersesByJuz(int juz);

    @Query("SELECT quran_verses.* FROM quran_verses " +
            "JOIN quran_verses_fts ON quran_verses.id = quran_verses_fts.rowid " +
            "WHERE quran_verses_fts MATCH :ftsQuery " +
            "ORDER BY quran_verses.surahId ASC, quran_verses.verseId ASC LIMIT 100")
    List<QuranVerseEntity> searchVersesFts(String ftsQuery);

    @Query("SELECT * FROM quran_verses " +
            "WHERE trText LIKE '%' || :query || '%' OR arText LIKE '%' || :query || '%' " +
            "ORDER BY surahId ASC, verseId ASC LIMIT 100")
    List<QuranVerseEntity> searchVersesLike(String query);

    @Query("SELECT COUNT(*) FROM quran_verses")
    int getVerseCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<QuranVerseEntity> verses);

    @Query("DELETE FROM quran_verses")
    void deleteAll();
}