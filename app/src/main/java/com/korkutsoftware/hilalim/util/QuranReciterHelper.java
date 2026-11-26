package com.korkutsoftware.hilalim.util;

import android.content.Context;

import com.korkutsoftware.hilalim.R;

import java.util.Locale;

/**
 * Kur'an tilaveti için desteklenen hafızları ve EveryAyah ses URL'lerini yönetir.
 */
public class QuranReciterHelper {

    public static final String[] RECITER_FOLDERS = {
            "Alafasy_128kbps",
            "Husary_128kbps",
            "Abdul_Basit_Murattal_192kbps"
    };

    public static String[] getReciterNames(Context context) {
        return new String[]{
                context.getString(R.string.quran_reciter_alafasy),
                context.getString(R.string.quran_reciter_husary),
                context.getString(R.string.quran_reciter_abdulbasit)
        };
    }

    public static String getReciterName(Context context, int index) {
        String[] names = getReciterNames(context);
        if (index < 0 || index >= names.length) return names[0];
        return names[index];
    }

    public static String buildVerseAudioUrl(int reciterIndex, int surahId, int verseId) {
        int safeIdx = (reciterIndex >= 0 && reciterIndex < RECITER_FOLDERS.length) ? reciterIndex : 0;
        String folder = RECITER_FOLDERS[safeIdx];
        return String.format(Locale.US, "https://everyayah.com/data/%s/%03d%03d.mp3", folder, surahId, verseId);
    }
}