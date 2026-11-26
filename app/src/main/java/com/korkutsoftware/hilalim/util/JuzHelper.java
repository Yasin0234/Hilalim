package com.korkutsoftware.hilalim.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Standart Medine Mushafı'na göre 30 Cüzün başlangıç (sure, ayet) bilgisini tutar.
 */
public class JuzHelper {

    public static class JuzInfo {
        public final int juzNumber;
        public final int startSurahId;
        public final int startVerseId;

        public JuzInfo(int juzNumber, int startSurahId, int startVerseId) {
            this.juzNumber = juzNumber;
            this.startSurahId = startSurahId;
            this.startVerseId = startVerseId;
        }
    }

    // [cuzIndex][0 = startSurahId, 1 = startVerseId]
    private static final int[][] JUZ_STARTS = {
            {1, 1},     // 1. Cüz: Fâtiha 1
            {2, 142},   // 2. Cüz: Bakara 142
            {2, 253},   // 3. Cüz: Bakara 253
            {3, 93},    // 4. Cüz: Âl-i İmrân 93
            {4, 24},    // 5. Cüz: Nisâ 24
            {4, 148},   // 6. Cüz: Nisâ 148
            {5, 82},    // 7. Cüz: Mâide 82
            {6, 111},   // 8. Cüz: En'âm 111
            {7, 88},    // 9. Cüz: A'râf 88
            {8, 41},    // 10. Cüz: Enfâl 41
            {9, 93},    // 11. Cüz: Tevbe 93
            {11, 6},    // 12. Cüz: Hûd 6
            {12, 53},   // 13. Cüz: Yûsuf 53
            {15, 1},    // 14. Cüz: Hicr 1
            {17, 1},    // 15. Cüz: İsrâ 1
            {18, 75},   // 16. Cüz: Kehf 75
            {21, 1},    // 17. Cüz: Enbiyâ 1
            {23, 1},    // 18. Cüz: Mü'minûn 1
            {25, 21},   // 19. Cüz: Furkân 21
            {27, 56},   // 20. Cüz: Neml 56
            {29, 46},   // 21. Cüz: Ankebût 46
            {33, 31},   // 22. Cüz: Ahzâb 31
            {36, 28},   // 23. Cüz: Yâsîn 28
            {39, 32},   // 24. Cüz: Zümer 32
            {41, 47},   // 25. Cüz: Fussilet 47
            {46, 1},    // 26. Cüz: Ahkâf 1
            {51, 31},   // 27. Cüz: Zâriyât 31
            {58, 1},    // 28. Cüz: Mücâdele 1
            {67, 1},    // 29. Cüz: Mülk 1
            {78, 1}     // 30. Cüz: Nebe 1
    };

    public static List<JuzInfo> getAllJuzList() {
        List<JuzInfo> list = new ArrayList<>(30);
        for (int i = 0; i < JUZ_STARTS.length; i++) {
            list.add(new JuzInfo(i + 1, JUZ_STARTS[i][0], JUZ_STARTS[i][1]));
        }
        return list;
    }

    /**
     * Verilen (sure, ayet) ikilisinin hangi cüze (1..30) ait olduğunu hesaplar.
     */
    public static int getJuzForVerse(int surahId, int verseId) {
        int result = 1;
        for (int i = 0; i < JUZ_STARTS.length; i++) {
            int s = JUZ_STARTS[i][0];
            int v = JUZ_STARTS[i][1];
            if (surahId > s || (surahId == s && verseId >= v)) {
                result = i + 1;
            } else {
                break;
            }
        }
        return result;
    }
}