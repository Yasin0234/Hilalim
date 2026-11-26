package com.korkutsoftware.hilalim.data.db;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "quran_verses",
        indices = {
                @Index(value = {"surahId", "verseId"}, unique = true),
                @Index(value = {"juz"})
        }
)
public class QuranVerseEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public int surahId;
    public int verseId;
    public int juz;
    public int page;
    public String arText;
    public String trText;

    public QuranVerseEntity(int surahId, int verseId, int juz, int page, String arText, String trText) {
        this.surahId = surahId;
        this.verseId = verseId;
        this.juz = juz;
        this.page = page;
        this.arText = arText;
        this.trText = trText;
    }
}