package com.korkutsoftware.hilalim.data.db;

import androidx.room.Entity;
import androidx.room.Fts4;

@Fts4(contentEntity = QuranVerseEntity.class)
@Entity(tableName = "quran_verses_fts")
public class QuranVerseFts {
    public String arText;
    public String trText;
}