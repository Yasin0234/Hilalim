package com.korkutsoftware.hilalim.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.korkutsoftware.hilalim.data.db.AppDatabase;
import com.korkutsoftware.hilalim.data.db.QuranDao;
import com.korkutsoftware.hilalim.data.db.QuranVerseEntity;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * İlk açılışta assets/quran_verses.json verisini arka planda tek seferde Room veritabanına aktarır.
 */
public class QuranImportHelper {

    private static final String TAG = "QuranImportHelper";
    private static final String KEY_IMPORTED = "quran_db_imported_v1";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean IN_PROGRESS = new AtomicBoolean(false);

    private static class JsonSurah {
        int surah_id;
        List<JsonVerse> verses;
    }

    private static class JsonVerse {
        int id;
        String ar;
        String tr;
    }

    public static boolean isImported(Context context) {
        if (context == null) return false;
        SharedPreferences prefs = context.getSharedPreferences(QuranPrefsHelper.PREFS_QURAN, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_IMPORTED, false);
    }

    public static void importIfNeededAsync(Context context) {
        if (context == null) return;
        Context appCtx = context.getApplicationContext();
        if (isImported(appCtx) || !IN_PROGRESS.compareAndSet(false, true)) {
            return;
        }
        EXECUTOR.execute(() -> {
            try {
                importBlocking(appCtx);
            } finally {
                IN_PROGRESS.set(false);
            }
        });
    }

    public static synchronized void importBlocking(Context context) {
        if (context == null) return;
        Context appCtx = context.getApplicationContext();
        QuranDao dao = AppDatabase.getInstance(appCtx).quranDao();

        if (isImported(appCtx) && dao.getVerseCount() > 6000) {
            return;
        }

        try (InputStream is = appCtx.getAssets().open("quran_verses.json")) {
            int size = is.available();
            byte[] buffer = new byte[size];
            int offset = 0;
            while (offset < size) {
                int read = is.read(buffer, offset, size - offset);
                if (read == -1) break;
                offset += read;
            }
            String json = new String(buffer, 0, offset, StandardCharsets.UTF_8);
            Type type = new TypeToken<List<JsonSurah>>() {}.getType();
            List<JsonSurah> surahs = new Gson().fromJson(json, type);
            if (surahs == null || surahs.isEmpty()) return;

            List<QuranVerseEntity> entities = new ArrayList<>(6300);
            for (JsonSurah surah : surahs) {
                if (surah.verses == null) continue;
                for (JsonVerse verse : surah.verses) {
                    int juz = JuzHelper.getJuzForVerse(surah.surah_id, verse.id);
                    entities.add(new QuranVerseEntity(
                            surah.surah_id,
                            verse.id,
                            juz,
                            0,
                            verse.ar != null ? verse.ar : "",
                            verse.tr != null ? verse.tr : ""
                    ));
                }
            }

            dao.insertAll(entities);
            appCtx.getSharedPreferences(QuranPrefsHelper.PREFS_QURAN, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_IMPORTED, true)
                    .apply();
        } catch (Exception e) {
            Log.e(TAG, "Kur'an Room aktarımı başarısız", e);
        }
    }
}