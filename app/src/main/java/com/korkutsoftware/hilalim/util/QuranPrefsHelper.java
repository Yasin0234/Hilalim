package com.korkutsoftware.hilalim.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Shared preferences helper for Quran reading position, bookmarks, font scale, and translation visibility.
 */
public class QuranPrefsHelper {

    public static final String PREFS_QURAN = "hilalim_quran_prefs";
    public static final String KEY_LAST_SURAH_ID = "last_surah_id";
    public static final String KEY_LAST_SURAH_NAME = "last_surah_name";
    public static final String KEY_LAST_VERSE_ID = "last_verse_id";
    private static final String KEY_BOOKMARKS = "quran_bookmarks";
    private static final String KEY_FONT_LEVEL = "quran_font_level"; // 0: Small, 1: Medium, 2: Large, 3: XL
    private static final String KEY_SHOW_TRANSLATION = "quran_show_translation";

    private static final Gson GSON = new Gson();

    public static class Bookmark {
        public int surahId;
        public String surahName;
        public int verseId;
        public String ar;
        public String tr;

        public Bookmark(int surahId, String surahName, int verseId, String ar, String tr) {
            this.surahId = surahId;
            this.surahName = surahName;
            this.verseId = verseId;
            this.ar = ar;
            this.tr = tr;
        }
    }

    public static void saveLastRead(Context context, int surahId, String surahName, int verseId) {
        if (context == null) return;
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE).edit();
        editor.putInt(KEY_LAST_SURAH_ID, surahId);
        if (surahName != null && !surahName.isEmpty()) {
            editor.putString(KEY_LAST_SURAH_NAME, surahName);
        }
        if (verseId > 0) {
            editor.putInt(KEY_LAST_VERSE_ID, verseId);
        }
        editor.apply();
    }

    public static int getLastSurahId(Context context) {
        return context.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE).getInt(KEY_LAST_SURAH_ID, 1);
    }

    public static String getLastSurahName(Context context) {
        return context.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE).getString(KEY_LAST_SURAH_NAME, "Fatiha");
    }

    public static int getLastVerseId(Context context) {
        return context.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE).getInt(KEY_LAST_VERSE_ID, 1);
    }

    public static int getFontLevel(Context context) {
        return context.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE).getInt(KEY_FONT_LEVEL, 1);
    }

    public static void setFontLevel(Context context, int level) {
        context.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE).edit()
                .putInt(KEY_FONT_LEVEL, Math.max(0, Math.min(3, level)))
                .apply();
    }

    public static float getArabicTextSizeSp(int level) {
        switch (level) {
            case 0: return 19f;
            case 2: return 27f;
            case 3: return 31f;
            default: return 23f;
        }
    }

    public static float getTranslationTextSizeSp(int level) {
        switch (level) {
            case 0: return 13f;
            case 2: return 17f;
            case 3: return 19f;
            default: return 15f;
        }
    }

    public static boolean isShowTranslation(Context context) {
        return context.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE).getBoolean(KEY_SHOW_TRANSLATION, true);
    }

    public static void setShowTranslation(Context context, boolean show) {
        context.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_SHOW_TRANSLATION, show)
                .apply();
    }

    public static List<Bookmark> getBookmarks(Context context) {
        if (context == null) return new ArrayList<>();
        String json = context.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE).getString(KEY_BOOKMARKS, null);
        if (json == null) return new ArrayList<>();
        try {
            Type type = new TypeToken<List<Bookmark>>() {}.getType();
            List<Bookmark> list = GSON.fromJson(json, type);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static boolean isBookmarked(Context context, int surahId, int verseId) {
        for (Bookmark b : getBookmarks(context)) {
            if (b.surahId == surahId && b.verseId == verseId) return true;
        }
        return false;
    }

    /**
     * Toggles bookmark state for a verse and returns true if now bookmarked, false if removed.
     */
    public static boolean toggleBookmark(Context context, Bookmark bookmark) {
        List<Bookmark> list = getBookmarks(context);
        boolean removed = false;
        Iterator<Bookmark> it = list.iterator();
        while (it.hasNext()) {
            Bookmark existing = it.next();
            if (existing.surahId == bookmark.surahId && existing.verseId == bookmark.verseId) {
                it.remove();
                removed = true;
            }
        }
        if (!removed) {
            list.add(0, bookmark);
        }
        context.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE).edit()
                .putString(KEY_BOOKMARKS, GSON.toJson(list))
                .apply();
        return !removed;
    }
}
