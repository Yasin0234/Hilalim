# Hilalim v1.2.0 — Yasin & Beyza Canlı Meet Geliştirme Rehberi (Ortak `main` Dalı)

> **Amaç:** Etap 2 kapsamında **Muhammed Yasin Korkut**'un kalan görevlerini (**Görev A1, A2, A3, A4**) **Beyza Karakaş** ile Google Meet üzerinden **sırayla `main` dalına push atarak** (ping-pong pair programming) sıfır çakışmayla tamamlamak.  
> **Durum Özeti:** Beyza'nın modülleri (`NamazTakipActivity`, `KazaTakipActivity`, `PrayerLogEntity/Dao`, `AppDatabase v1`, 2 yeni widget, `AraclarFragment` güncellemesi) `main` üzerinde hazır. Şimdi A1–A4 modülleri **8 derlenebilir turda** tamamlanacaktır.

---

## 0. Meet Altın Kuralları (`main` Dalı Çalışma Protokolü)

Her turda sırası gelen kişi **sadece kendi turundaki adımları** uygular, derler/test eder ve pushlar; diğer kişi push gelmeden dosya düzenlemez.

1. **Tura başlarken mutlaka çek:**
   ```bash
   git checkout main
   git pull --rebase origin main
   ```
2. **Android Studio uyarısı:** `build.gradle` veya `libs.versions.toml` değiştiyse üstte çıkan **Sync Now** butonuna bas.
3. **Push öncesi zorunlu doğrulama (kırık kod pushlamak yasak):**
   ```bash
   ./gradlew.bat testDebugUnitTest assembleDebug
   ```
4. **Push komutu:**
   ```bash
   git add .
   git commit -m "feat(etap2): Tur X - <kısa özet>"
   git push origin main
   ```

| Tur | Kim Yapar? | İçerik | Sonuç |
| :--- | :--- | :--- | :--- |
| **Tur 1** | **Yasin** | Gradle (`Media3`, `WorkManager`), 6 Vektör İkon, 4 Dil `strings.xml` | Tüm kaynaklar ve bağımlılıklar hazır |
| **Tur 2** | **Beyza** | `QuranVerseEntity`, `QuranVerseFts`, `QuranDao`, `JuzHelper`, `AppDatabase v2` + Test | Room Kur'an şeması ve Cüz tablosu hazır |
| **Tur 3** | **Yasin** | `QuranImportHelper`, `Dashboard` ön yükleme, `QuranFragment` (Ayet Arama + 30 Cüz) | Ayet içi FTS4 arama ve Cüz navigasyonu çalışır |
| **Tur 4** | **Beyza** | `QuranPrefsHelper` genişletme, `QuranReciterHelper`, `QuranAudioService`, Manifest | Arka plan Media3 ses servisi hazır |
| **Tur 5** | **Yasin** | `item_verse.xml`, `activity_surah_detail.xml` (Mini Player), `SurahDetailActivity` | Sesli tilavet, 1x/3x/5x tekrar ve aktif ayet vurgusu çalışır |
| **Tur 6** | **Beyza** | `EzanPlayerService` (çoklu makam), `DndHelper`, `AlarmHelper`, `NotificationReceiver` | Makam seçimi, özel erken uyarı ve DND altyapısı hazır |
| **Tur 7** | **Yasin** | `activity_notification_settings.xml` & `NotificationSettingsActivity` UI | Kullanıcı arayüzünden makam, dakika ve DND seçilebilir |
| **Tur 8** | **İkisi Birlikte** | Canlı QA (Light/Dark, 4 Dil, Offline, Ekran Kilitli Tilavet) + Sürüm Kontrolü | Etap 2 A1–A4 tamamlandı |

---

## TUR 1 — YASİN: Bağımlılıklar, Eksik İkonlar ve 4 Dil Metinleri

> **Meet Durumu:** Yasin ekran paylaşır, Beyza izler ve kontrol eder.

### Adım 1.1 — `gradle/libs.versions.toml` Güncellemesi
**Dosya:** `gradle/libs.versions.toml`

1. **12. satırın (`room = "2.6.1"`) hemen altına (13. satıra)** şu iki satırı ekle:
```toml
media3 = "1.11.1"
work = "2.12.0"
```
2. **27. satırın (`room-compiler = ...`) hemen altına (28. satıra)** şu üç satırı ekle:
```toml
media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }
work-runtime = { group = "androidx.work", name = "work-runtime", version.ref = "work" }
```

**Kodun Anlamı:**
- `media3-exoplayer`: Ayet ses dosyalarını (`.mp3`) internetten stream ederek çalan modern Google medya oynatıcısıdır (`MediaPlayer`'dan daha kararlı, buffering ve hata yönetimi güçlüdür).
- `media3-session`: Arka planda çalan sesin Android kilit ekranı ve bildirim çubuğu ile konuşmasını sağlar.
- `work-runtime`: İleride arka plan görevleri ve periyodik işlemler için Gradle kontratına eklenir.

---

### Adım 1.2 — `app/build.gradle` Güncellemesi
**Dosya:** `app/build.gradle`

**50. satırın (`annotationProcessor libs.room.compiler`) hemen altına (51. satıra)** şu satırları ekle:
```gradle
    implementation libs.media3.exoplayer
    implementation libs.media3.session
    implementation libs.work.runtime
```
Ardından Android Studio'da **Sync Now** butonuna bas.

**Kodun Anlamı:** Version catalog (`libs.versions.toml`) içinde tanımladığımız kütüphaneleri `:app` modülünün derleme sınıf yoluna (`implementation`) dahil eder.

---

### Adım 1.3 — Eksik Vektör İkonlarının Oluşturulması
`app/src/main/res/drawable/` klasöründe bulunmayan 6 ikonu oluşturuyoruz (aksi halde sonraki XML adımları derlenmez).

1. **Yeni Dosya:** `app/src/main/res/drawable/ic_play_arrow.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M8,5v14l11,-7z" />
</vector>
```

2. **Yeni Dosya:** `app/src/main/res/drawable/ic_pause.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M6,19h4V5H6v14zm8,-14v14h4V5h-4z" />
</vector>
```

3. **Yeni Dosya:** `app/src/main/res/drawable/ic_skip_previous.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M6,6h2v12H6zm3.5,6l8.5,6V6z" />
</vector>
```

4. **Yeni Dosya:** `app/src/main/res/drawable/ic_skip_next.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M6,18l8.5,-6L6,6v12zM16,6v12h2V6h-2z" />
</vector>
```

5. **Yeni Dosya:** `app/src/main/res/drawable/ic_repeat.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M7,7h10v3l4,-4 -4,-4v3H5v6h2V7zm10,10H7v-3l-4,4 4,4v-3h12v-6h-2v4z" />
</vector>
```

6. **Yeni Dosya:** `app/src/main/res/drawable/ic_dnd.xml`
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zm5,11H7v-2h10v2z" />
</vector>
```

**Kodun Anlamı:** Tüm ikonlar beyaz (`#FFFFFFFF`) vektör olarak tanımlanır; böylece XML içinde `app:tint="@color/gold"` veya `app:tint="?attr/colorSecondary"` verdiğimizde Light/Dark temaya otomatik uyum sağlarlar.

---

### Adım 1.4 — 4 Dilde `strings.xml` Güncellemesi (Tek Seferde Çakışmasız)
Aşağıdaki metin bloklarını her dil dosyasının **en sonundaki `</resources>` etiketinin hemen üstüne** ekle.

1. **`app/src/main/res/values/strings.xml` — 335. satırdaki `</resources>` öncesine (334. satırın altına):**
```xml
    <!-- Yasin Etap 2: Kur'an Arama, Cüz, Sesli Tilavet & Gelişmiş Ezan -->
    <string name="quran_tab_search_verses">Ayet Ara</string>
    <string name="quran_tab_juz">Cüzler</string>
    <string name="quran_search_verse_hint">Ayet meali ara (en az 3 harf)…</string>
    <string name="quran_search_min_chars">Ayet içi arama için en az 3 karakter yazın.</string>
    <string name="quran_search_empty">Aramanızla eşleşen ayet bulunamadı.</string>
    <string name="quran_juz_format">%d. Cüz</string>
    <string name="quran_juz_start_format">Başlangıç: %1$s • %2$d. Ayet</string>
    <string name="quran_reciter_select">Hafız Seçin</string>
    <string name="quran_reciter_alafasy">Mishary Rashid Alafasy</string>
    <string name="quran_reciter_husary">Mahmoud Khalil Al-Husary</string>
    <string name="quran_reciter_abdulbasit">Abdul Basit (Murattal)</string>
    <string name="quran_repeat_format">%dx</string>
    <string name="quran_audio_loading">Ses yükleniyor…</string>
    <string name="quran_audio_network_error">Ses dosyası yüklenemedi. İnternet bağlantınızı kontrol edin.</string>
    <string name="quran_audio_notif_channel">Kur\'an Tilaveti</string>
    <string name="quran_audio_notif_desc">Arka planda sesli Kur\'an tilaveti kontrolleri</string>
    <string name="quran_play_verse_desc">Ayeti Dinle</string>
    <string name="quran_pause_verse_desc">Duraklat</string>
    <string name="notif_ezan_sound_title">Ezan Makamı / Sesi</string>
    <string name="ezan_sound_classic">Klasik Ezan (Dahili)</string>
    <string name="ezan_sound_istanbul">İstanbul Makamı</string>
    <string name="ezan_sound_makkah">Mekke / Medine Ezanı</string>
    <string name="ezan_sound_short">Kısa Tekbir / Uyarı Sesi</string>
    <string name="ezan_sound_silent">Sadece Sessiz Bildirim</string>
    <string name="notif_pre_minutes_title">Erken Uyarı Süresi</string>
    <string name="notif_pre_minutes_value">%d dakika önce</string>
    <string name="notif_pre_minutes_dynamic">%d Dakika Önce Hatırlat</string>
    <string name="notification_pre_custom_title">Ezan Vaktine %d Dk Kaldı</string>
    <string name="notif_dnd_title">Namaz Vaktinde Sessize Al (DND)</string>
    <string name="notif_dnd_desc">Vakitten 5 dk sonra telefonu 20 dk sessize alır</string>
    <string name="notif_dnd_permission_title">Rahatsız Etmeyin İzni Gerekli</string>
    <string name="notif_dnd_permission_msg">Namaz vakitlerinde telefonu otomatik sessize alabilmek için ayarlar ekranından Hilalim uygulamasına izin vermelisiniz.</string>
    <string name="notif_dnd_go_settings">Ayarlara Git</string>
```

2. **`app/src/main/res/values-en/strings.xml` — 315. satırdaki `</resources>` öncesine (314. satırın altına):**
```xml
    <!-- Yasin Etap 2: Quran Search, Juz, Audio & Ezan -->
    <string name="quran_tab_search_verses">Search Verses</string>
    <string name="quran_tab_juz">Juz (1-30)</string>
    <string name="quran_search_verse_hint">Search verse translation (min 3 chars)…</string>
    <string name="quran_search_min_chars">Enter at least 3 characters to search verses.</string>
    <string name="quran_search_empty">No matching verses found.</string>
    <string name="quran_juz_format">Juz %d</string>
    <string name="quran_juz_start_format">Starts: %1$s • Verse %2$d</string>
    <string name="quran_reciter_select">Select Reciter</string>
    <string name="quran_reciter_alafasy">Mishary Rashid Alafasy</string>
    <string name="quran_reciter_husary">Mahmoud Khalil Al-Husary</string>
    <string name="quran_reciter_abdulbasit">Abdul Basit (Murattal)</string>
    <string name="quran_repeat_format">%dx</string>
    <string name="quran_audio_loading">Loading audio…</string>
    <string name="quran_audio_network_error">Could not load audio. Please check your internet connection.</string>
    <string name="quran_audio_notif_channel">Quran Recitation</string>
    <string name="quran_audio_notif_desc">Background Quran audio playback controls</string>
    <string name="quran_play_verse_desc">Play Verse</string>
    <string name="quran_pause_verse_desc">Pause</string>
    <string name="notif_ezan_sound_title">Adhan Sound / Maqam</string>
    <string name="ezan_sound_classic">Classic Adhan (Built-in)</string>
    <string name="ezan_sound_istanbul">Istanbul Maqam</string>
    <string name="ezan_sound_makkah">Makkah / Madinah Adhan</string>
    <string name="ezan_sound_short">Short Alert Tone</string>
    <string name="ezan_sound_silent">Silent Notification Only</string>
    <string name="notif_pre_minutes_title">Early Reminder Lead Time</string>
    <string name="notif_pre_minutes_value">%d minutes before</string>
    <string name="notif_pre_minutes_dynamic">Remind %d Minutes Before</string>
    <string name="notification_pre_custom_title">%d Min Left Until Prayer</string>
    <string name="notif_dnd_title">Mute During Prayer (DND)</string>
    <string name="notif_dnd_desc">Mutes phone 5 min after prayer for 20 minutes</string>
    <string name="notif_dnd_permission_title">Do Not Disturb Access Needed</string>
    <string name="notif_dnd_permission_msg">Please grant Do Not Disturb access to Hilalim in settings to mute your phone automatically during prayer times.</string>
    <string name="notif_dnd_go_settings">Open Settings</string>
```

3. **`app/src/main/res/values-de/strings.xml` — 315. satırdaki `</resources>` öncesine (314. satırın altına):**
```xml
    <!-- Yasin Etap 2: Koran-Suche, Dschuz, Audio & Adhan -->
    <string name="quran_tab_search_verses">Verssuche</string>
    <string name="quran_tab_juz">Dschuz (1-30)</string>
    <string name="quran_search_verse_hint">Versübersetzung suchen (min. 3 Zeichen)…</string>
    <string name="quran_search_min_chars">Geben Sie mindestens 3 Zeichen für die Verssuche ein.</string>
    <string name="quran_search_empty">Keine passenden Verse gefunden.</string>
    <string name="quran_juz_format">Dschuz %d</string>
    <string name="quran_juz_start_format">Beginn: %1$s • Vers %2$d</string>
    <string name="quran_reciter_select">Rezitator wählen</string>
    <string name="quran_reciter_alafasy">Mishary Rashid Alafasy</string>
    <string name="quran_reciter_husary">Mahmoud Khalil Al-Husary</string>
    <string name="quran_reciter_abdulbasit">Abdul Basit (Murattal)</string>
    <string name="quran_repeat_format">%dx</string>
    <string name="quran_audio_loading">Audio wird geladen…</string>
    <string name="quran_audio_network_error">Audio konnte nicht geladen werden. Bitte prüfen Sie Ihre Internetverbindung.</string>
    <string name="quran_audio_notif_channel">Koran-Rezitation</string>
    <string name="quran_audio_notif_desc">Steuerung für Hintergrund-Rezitation</string>
    <string name="quran_play_verse_desc">Vers abspielen</string>
    <string name="quran_pause_verse_desc">Pause</string>
    <string name="notif_ezan_sound_title">Adhan-Ton / Maqam</string>
    <string name="ezan_sound_classic">Klassischer Adhan (Integriert)</string>
    <string name="ezan_sound_istanbul">Istanbul-Maqam</string>
    <string name="ezan_sound_makkah">Mekka / Medina Adhan</string>
    <string name="ezan_sound_short">Kurzer Hinweiston</string>
    <string name="ezan_sound_silent">Nur lautlose Benachrichtigung</string>
    <string name="notif_pre_minutes_title">Vorlaufzeit der Erinnerung</string>
    <string name="notif_pre_minutes_value">%d Minuten vorher</string>
    <string name="notif_pre_minutes_dynamic">%d Minuten vorher erinnern</string>
    <string name="notification_pre_custom_title">Noch %d Min. bis zum Gebet</string>
    <string name="notif_dnd_title">Während des Gebets stumm (DND)</string>
    <string name="notif_dnd_desc">Schaltet das Telefon 5 Min. nach Gebetsbeginn für 20 Min. stumm</string>
    <string name="notif_dnd_permission_title">Bitte-nicht-stören-Berechtigung nötig</string>
    <string name="notif_dnd_permission_msg">Bitte erteilen Sie Hilalim in den Einstellungen die Berechtigung für Bitte nicht stören.</string>
    <string name="notif_dnd_go_settings">Einstellungen</string>
```

4. **`app/src/main/res/values-ar/strings.xml` — 315. satırdaki `</resources>` öncesine (314. satırın altına):**
```xml
    <!-- Yasin Etap 2: البحث في القرآن والأجزاء والتلاوة والأذان -->
    <string name="quran_tab_search_verses">بحث الآيات</string>
    <string name="quran_tab_juz">الأجزاء</string>
    <string name="quran_search_verse_hint">ابحث في معاني الآيات (٣ أحرف على الأقل)…</string>
    <string name="quran_search_min_chars">أدخل ٣ أحرف على الأقل للبحث في الآيات.</string>
    <string name="quran_search_empty">لم يتم العثور على آيات مطابقة.</string>
    <string name="quran_juz_format">الجزء %d</string>
    <string name="quran_juz_start_format">البداية: %1$s • الآية %2$d</string>
    <string name="quran_reciter_select">اختر القارئ</string>
    <string name="quran_reciter_alafasy">مشاري راشد العفاسي</string>
    <string name="quran_reciter_husary">محمود خليل الحصري</string>
    <string name="quran_reciter_abdulbasit">عبد الباسط عبد الصمد (مرتل)</string>
    <string name="quran_repeat_format">%dx</string>
    <string name="quran_audio_loading">جاري تحميل الصوت…</string>
    <string name="quran_audio_network_error">تعذر تحميل الصوت. يرجى التحقق من الاتصال بالإنترنت.</string>
    <string name="quran_audio_notif_channel">تلاوة القرآن</string>
    <string name="quran_audio_notif_desc">عناصر التحكم بتلاوة القرآن في الخلفية</string>
    <string name="quran_play_verse_desc">تشغيل الآية</string>
    <string name="quran_pause_verse_desc">إيقاف مؤقت</string>
    <string name="notif_ezan_sound_title">صوت / مقام الأذان</string>
    <string name="ezan_sound_classic">الأذان الكلاسيكي (مدمج)</string>
    <string name="ezan_sound_istanbul">مقام إسطنبول</string>
    <string name="ezan_sound_makkah">أذان مكة / المدينة</string>
    <string name="ezan_sound_short">نغمة تنبيه قصيرة</string>
    <string name="ezan_sound_silent">إشعار صامت فقط</string>
    <string name="notif_pre_minutes_title">وقت التنبيه المسبق</string>
    <string name="notif_pre_minutes_value">قبل %d دقيقة</string>
    <string name="notif_pre_minutes_dynamic">التنبيه قبل %d دقيقة</string>
    <string name="notification_pre_custom_title">متبقي %d دقيقة على الصلاة</string>
    <string name="notif_dnd_title">الوضع الصامت أثناء الصلاة</string>
    <string name="notif_dnd_desc">يكتم الهاتف بعد الأذان بـ ٥ دقائق لمدة ٢٠ دقيقة</string>
    <string name="notif_dnd_permission_title">مطلوب إذن عدم الإزعاج</string>
    <string name="notif_dnd_permission_msg">يرجى منح تطبيق هلالي إذن الوصول إلى عدم الإزعاج من الإعدادات لكتم الصوت تلقائياً.</string>
    <string name="notif_dnd_go_settings">الانتقال إلى الإعدادات</string>
```

### Adım 1.5 — Yasin Doğrulama & Push
```bash
./gradlew.bat testDebugUnitTest assembleDebug
git add .
git commit -m "chore(etap2): add Media3, WorkManager, audio icons and 4-locale strings"
git push origin main
```

---

## TUR 2 — BEYZA: Room Kur'an Şeması (Entity, FTS4, DAO, JuzHelper & AppDatabase v2)

> **Meet Durumu:** Beyza `git pull --rebase origin main` yapar ve ekran paylaşır. (Beyza zaten `AppDatabase` v1'i yazdığı için v2 güncellemesini yapması en güvenli yoldur.)

### Adım 2.1 — `QuranVerseEntity.java` Oluşturma
**Yeni Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/data/db/QuranVerseEntity.java`

```java
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
```

**Kodun Anlamı:**
- `indices` içinde `(surahId, verseId)` için `unique = true` verilmesi, aynı ayetin iki kez eklenmesini engeller ve sure açılış sorgularını hızlandırır.
- `juz` indeksi cüze göre listelemeyi hızlandırır.

---

### Adım 2.2 — `QuranVerseFts.java` Oluşturma
**Yeni Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/data/db/QuranVerseFts.java`

```java
package com.korkutsoftware.hilalim.data.db;

import androidx.room.Entity;
import androidx.room.Fts4;

@Fts4(contentEntity = QuranVerseEntity.class)
@Entity(tableName = "quran_verses_fts")
public class QuranVerseFts {
    public String arText;
    public String trText;
}
```

**Kodun Anlamı:**
- `@Fts4(contentEntity = QuranVerseEntity.class)`: SQLite'ın **Full-Text Search (Tam Metin Arama)** sanal tablosunu `quran_verses` tablosuna bağlar. `6236` ayet içinde `LIKE '%sabır%'` araması yerine kelime indeksli çok hızlı arama yapmamızı sağlar.
- `contentEntity` sayesinde veriler diskte iki kez kopyalanmaz, Room otomatik senkronize eder.

---

### Adım 2.3 — `QuranDao.java` Oluşturma
**Yeni Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/data/db/QuranDao.java`

```java
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
```

**Kodun Anlamı:**
- `searchVersesFts`: Kelime bazlı FTS4 araması yapar (`"sabır*"` gibi önek eşleşmesiyle çok hızlıdır).
- `searchVersesLike`: Türkçe ekli aramalar (`"sabredin"` içinde `"sabred"` veya noktalama içeren ifadeler) FTS4 kelime kökünü yakalamazsa yedek (`fallback`) olarak devreye girer.

---

### Adım 2.4 — `JuzHelper.java` Oluşturma (30 Cüz Tablosu)
**Yeni Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/util/JuzHelper.java`

```java
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
```

**Kodun Anlamı:** `assets/quran_verses.json` dosyasında cüz alanı yer almadığı için (`surah_id`, `id`, `ar`, `tr` var), bu sınıf standart Mushaf sınırlarını sabit tutar. Hem Room aktarımı sırasında her ayetin `juz` sütununu doldurur hem de Cüz sekmesinde 30 kartı üretir.

---

### Adım 2.5 — `AppDatabase.java` Güncellemesi (v1 -> v2)
**Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/data/db/AppDatabase.java`

**9–15. satırları** aşağıdakiyle değiştir:
```java
@Database(
        entities = {PrayerLogEntity.class, QuranVerseEntity.class, QuranVerseFts.class},
        version = 2,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "hilalim_database";
    private static volatile AppDatabase INSTANCE;

    public abstract PrayerLogDao prayerLogDao();

    public abstract QuranDao quranDao();
```

**Kodun Anlamı:** `PrayerLogEntity` korunarak Kur'an tabloları eklenir. Cihazda daha önce v1 veritabanı oluştuğu için `version = 2` yapılır; `fallbackToDestructiveMigration()` sayesinde kurulum çökmeden temizce yeni şemaya geçer (Kur'an verisi zaten JSON'dan yeniden yüklenir).

---

### Adım 2.6 — `AppCalculatorsTest.java` içine Cüz Birim Testi Ekleme
**Dosya:** `app/src/test/java/com/korkutsoftware/hilalim/AppCalculatorsTest.java`

1. **5. satırın (`import org.junit.Test;`) üstüne** ekle:
```java
import com.korkutsoftware.hilalim.util.JuzHelper;
```
2. **84. satırın (`}` metot sonu, 85. satırdaki sınıf kapanış `}` öncesine)** ekle:
```java
    @Test
    public void juzHelper_mapsBoundariesCorrectly() {
        assertEquals(30, JuzHelper.getAllJuzList().size());
        assertEquals(1, JuzHelper.getJuzForVerse(1, 1));
        assertEquals(1, JuzHelper.getJuzForVerse(2, 141));
        assertEquals(2, JuzHelper.getJuzForVerse(2, 142));
        assertEquals(23, JuzHelper.getJuzForVerse(36, 28));
        assertEquals(30, JuzHelper.getJuzForVerse(78, 1));
        assertEquals(30, JuzHelper.getJuzForVerse(114, 6));
    }
```

### Adım 2.7 — Beyza Doğrulama & Push
```bash
./gradlew.bat testDebugUnitTest assembleDebug
git add .
git commit -m "feat(etap2): add Quran Room entity, FTS4, DAO, JuzHelper and AppDatabase v2"
git push origin main
```

---

## TUR 3 — YASİN: `QuranImportHelper`, `Dashboard` Ön Yükleme ve `QuranFragment` (Ayet Arama + Cüzler)

> **Meet Durumu:** Yasin `git pull --rebase origin main` yapar ve ekran paylaşır.

### Adım 3.1 — `QuranImportHelper.java` Oluşturma
**Yeni Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/util/QuranImportHelper.java`

```java
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
```

**Kodun Anlamı:**
- `AtomicBoolean IN_PROGRESS`: Uygulama aynı anda hem `Dashboard` hem `QuranFragment` içinden import çağırırsa iki iş parçacığının çakışmasını önler.
- `dao.getVerseCount() > 6000` kontrolü: Eğer `AppDatabase` v1->v2 göçünde tablo sıfırlandıysa ama `SharedPreferences` bayrağı eski kaldıysa bile veriyi otomatik tekrar doldurur!

---

### Adım 3.2 — `Dashboard.java` İçinde Ön Yükleme Başlatma
**Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/Dashboard.java`

1. **27. satırın (`import com.korkutsoftware.hilalim.util.LocaleHelper;`) altına** ekle:
```java
import com.korkutsoftware.hilalim.util.QuranImportHelper;
```
2. **101. satırdaki `}` (`if (savedInstanceState == null)` bloğunun kapanışı) hemen altına (102. satıra)** ekle:
```java
        QuranImportHelper.importIfNeededAsync(this);
```

**Kodun Anlamı:** Kullanıcı `Dashboard`'u açtığı anda arka planda sessizce Kur'an veritabanı hazırlanır; kullanıcı Kur'an sekmesine geçtiğinde arama anında çalışır.

---

### Adım 3.3 — `fragment_quran.xml` İçine 4 Sekmeli ChipGroup Ekleme
**Dosya:** `app/src/main/res/layout/fragment_quran.xml`

**123. satırdan 152. satıra kadar olan** `<!-- Mode Filter Chips ... -->` bloğunu (`chip_group_quran_mode`) aşağıdaki `HorizontalScrollView` + `ChipGroup` bloğuyla tamamen değiştir:
```xml
    <!-- Mode Filter Chips (Surahs / Verse Search / Juz / Bookmarks) -->
    <HorizontalScrollView
        android:id="@+id/chip_group_scroll"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:clipToPadding="false"
        android:paddingHorizontal="20dp"
        android:scrollbars="none"
        app:layout_constraintTop_toBottomOf="@id/search_container">

        <com.google.android.material.chip.ChipGroup
            android:id="@+id/chip_group_quran_mode"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:selectionRequired="true"
            app:singleLine="true"
            app:singleSelection="true">

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_quran_surahs"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:checked="true"
                android:text="@string/quran_tab_surahs" />

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_quran_search_verses"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/quran_tab_search_verses" />

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_quran_juz"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/quran_tab_juz" />

            <com.google.android.material.chip.Chip
                android:id="@+id/chip_quran_bookmarks"
                style="@style/Widget.Material3.Chip.Filter"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/quran_tab_bookmarks" />
        </com.google.android.material.chip.ChipGroup>
    </HorizontalScrollView>
```

**Ayrıca 164. satırda** `card_continue_reading` içindeki `app:layout_constraintTop_toBottomOf` özelliğini yeni kapsayıcıya bağla:
```xml
        app:layout_constraintTop_toBottomOf="@id/chip_group_scroll"
```

**Kodun Anlamı:** 4 chip küçük ekranlarda veya Almanca/Arapça uzun metinlerde alt satıra taşıp tasarımı bozmasın diye `HorizontalScrollView` + `app:singleLine="true"` içine alındı. `card_continue_reading` artık `@id/chip_group_scroll` altına bağlanır.

---

### Adım 3.4 — `QuranFragment.java` Güncellemesi (Ayet İçi FTS4 Arama & 30 Cüz)
**Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/QuranFragment.java`

1. **Import bloğuna (24. satır `import com.korkutsoftware.hilalim.util.QuranPrefsHelper;` altına)** ekle:
```java
import android.util.SparseArray;

import com.korkutsoftware.hilalim.data.db.AppDatabase;
import com.korkutsoftware.hilalim.data.db.QuranDao;
import com.korkutsoftware.hilalim.data.db.QuranVerseEntity;
import com.korkutsoftware.hilalim.util.JuzHelper;
import com.korkutsoftware.hilalim.util.QuranImportHelper;

import java.util.concurrent.ExecutorService;
```

2. **37–51. satırlar arasındaki alanları** aşağıdakiyle değiştir:
```java
    private static final int MODE_SURAHS = 0;
    private static final int MODE_SEARCH_VERSES = 1;
    private static final int MODE_JUZ = 2;
    private static final int MODE_BOOKMARKS = 3;

    private RecyclerView rv;
    private SurahAdapter surahAdapter;
    private VerseSearchAdapter verseSearchAdapter;
    private JuzAdapter juzAdapter;
    private BookmarkAdapter bookmarkAdapter;
    private View cardContinueReading;
    private View btnClearSearch;
    private EditText etSearch;
    private TextView txtLastSurahName, txtBookmarksEmpty;
    private Chip chipSurahs, chipSearchVerses, chipJuz, chipBookmarks;

    private final SparseArray<String> surahNameMap = new SparseArray<>();
    private final ExecutorService searchExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearchRunnable;

    private int lastSurahId = 1;
    private String lastSurahName = "Fatiha";
    private int lastVerseId = 1;
    private int currentMode = MODE_SURAHS;
    private String currentSearchQuery = "";
```

3. **62–157. satırlar arasını (`onViewCreated`, `onResume`, `switchMode`, `loadBookmarksIntoAdapter`, `updateEmptyState`, `updateContinueCardVisibility`)** aşağıdakiyle değiştir:
```java
        cardContinueReading = view.findViewById(R.id.card_continue_reading);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        txtLastSurahName = view.findViewById(R.id.txt_last_surah_name);
        txtBookmarksEmpty = view.findViewById(R.id.txt_bookmarks_empty);
        chipSurahs = view.findViewById(R.id.chip_quran_surahs);
        chipSearchVerses = view.findViewById(R.id.chip_quran_search_verses);
        chipJuz = view.findViewById(R.id.chip_quran_juz);
        chipBookmarks = view.findViewById(R.id.chip_quran_bookmarks);
        etSearch = view.findViewById(R.id.et_search);

        if (cardContinueReading != null) {
            cardContinueReading.setOnClickListener(v -> openSurah(lastSurahId, lastSurahName, lastVerseId));
        }

        rv = view.findViewById(R.id.rv_surah);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        surahAdapter = new SurahAdapter(new ArrayList<>());
        verseSearchAdapter = new VerseSearchAdapter(new ArrayList<>());
        juzAdapter = new JuzAdapter(JuzHelper.getAllJuzList());
        bookmarkAdapter = new BookmarkAdapter(new ArrayList<>());
        rv.setAdapter(surahAdapter);

        QuranImportHelper.importIfNeededAsync(requireContext());
        loadSurahsAsync();

        if (chipSurahs != null) chipSurahs.setOnClickListener(v -> switchMode(MODE_SURAHS));
        if (chipSearchVerses != null) chipSearchVerses.setOnClickListener(v -> switchMode(MODE_SEARCH_VERSES));
        if (chipJuz != null) chipJuz.setOnClickListener(v -> switchMode(MODE_JUZ));
        if (chipBookmarks != null) chipBookmarks.setOnClickListener(v -> switchMode(MODE_BOOKMARKS));

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
        }

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString() : "";
                boolean hasQuery = currentSearchQuery.length() > 0;
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(hasQuery ? View.VISIBLE : View.GONE);
                }
                updateContinueCardVisibility();
                applyCurrentFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        refreshContinueReadingCard();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshContinueReadingCard();
        if (currentMode == MODE_BOOKMARKS) {
            loadBookmarksIntoAdapter();
        }
    }

    @Override
    public void onDestroyView() {
        if (pendingSearchRunnable != null) {
            mainHandler.removeCallbacks(pendingSearchRunnable);
        }
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        searchExecutor.shutdown();
        super.onDestroy();
    }

    private void switchMode(int mode) {
        currentMode = mode;
        updateContinueCardVisibility();
        if (etSearch != null) {
            etSearch.setHint(mode == MODE_SEARCH_VERSES
                    ? R.string.quran_search_verse_hint
                    : R.string.sure_ara);
        }

        if (mode == MODE_BOOKMARKS) {
            loadBookmarksIntoAdapter();
            rv.setAdapter(bookmarkAdapter);
        } else if (mode == MODE_SEARCH_VERSES) {
            rv.setAdapter(verseSearchAdapter);
            applyCurrentFilter();
        } else if (mode == MODE_JUZ) {
            if (txtBookmarksEmpty != null) txtBookmarksEmpty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            rv.setAdapter(juzAdapter);
            juzAdapter.filter(currentSearchQuery);
        } else {
            if (txtBookmarksEmpty != null) txtBookmarksEmpty.setVisibility(View.GONE);
            rv.setVisibility(View.VISIBLE);
            rv.setAdapter(surahAdapter);
            surahAdapter.getFilter().filter(currentSearchQuery);
        }
    }

    private void applyCurrentFilter() {
        if (currentMode == MODE_BOOKMARKS) {
            bookmarkAdapter.filter(currentSearchQuery);
            updateEmptyState();
        } else if (currentMode == MODE_SEARCH_VERSES) {
            scheduleVerseSearch(currentSearchQuery);
        } else if (currentMode == MODE_JUZ) {
            juzAdapter.filter(currentSearchQuery);
        } else {
            surahAdapter.getFilter().filter(currentSearchQuery);
        }
    }

    private void scheduleVerseSearch(String rawQuery) {
        if (pendingSearchRunnable != null) {
            mainHandler.removeCallbacks(pendingSearchRunnable);
        }
        String trimmed = rawQuery != null ? rawQuery.trim() : "";
        if (trimmed.length() < 3) {
            verseSearchAdapter.setItems(new ArrayList<>());
            if (txtBookmarksEmpty != null) {
                txtBookmarksEmpty.setText(R.string.quran_search_min_chars);
                txtBookmarksEmpty.setVisibility(View.VISIBLE);
            }
            rv.setVisibility(View.GONE);
            return;
        }
        if (txtBookmarksEmpty != null) txtBookmarksEmpty.setVisibility(View.GONE);
        rv.setVisibility(View.VISIBLE);

        pendingSearchRunnable = () -> runVerseSearchAsync(trimmed);
        mainHandler.postDelayed(pendingSearchRunnable, 220);
    }

    private void runVerseSearchAsync(String query) {
        Context ctx = getContext();
        if (ctx == null) return;
        Context appCtx = ctx.getApplicationContext();

        searchExecutor.execute(() -> {
            if (!QuranImportHelper.isImported(appCtx)) {
                QuranImportHelper.importBlocking(appCtx);
            }
            QuranDao dao = AppDatabase.getInstance(appCtx).quranDao();
            String sanitized = query.replace("\"", "").replace("*", "").trim();
            List<QuranVerseEntity> results = new ArrayList<>();
            if (!sanitized.isEmpty()) {
                try {
                    results = dao.searchVersesFts(sanitized + "*");
                } catch (Exception ignored) {
                }
                if (results == null || results.isEmpty()) {
                    results = dao.searchVersesLike(sanitized);
                }
            }
            final List<QuranVerseEntity> finalResults = results != null ? results : new ArrayList<>();
            mainHandler.post(() -> {
                if (!isAdded() || currentMode != MODE_SEARCH_VERSES) return;
                verseSearchAdapter.setItems(finalResults);
                if (txtBookmarksEmpty != null) {
                    boolean empty = finalResults.isEmpty();
                    txtBookmarksEmpty.setText(R.string.quran_search_empty);
                    txtBookmarksEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    rv.setVisibility(empty ? View.GONE : View.VISIBLE);
                }
            });
        });
    }

    private void loadBookmarksIntoAdapter() {
        if (getContext() == null) return;
        List<QuranPrefsHelper.Bookmark> list = QuranPrefsHelper.getBookmarks(getContext());
        bookmarkAdapter.setItems(list);
        bookmarkAdapter.filter(currentSearchQuery);
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (currentMode != MODE_BOOKMARKS || txtBookmarksEmpty == null) return;
        boolean empty = bookmarkAdapter.getItemCount() == 0;
        txtBookmarksEmpty.setText(R.string.quran_bookmarks_empty);
        txtBookmarksEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rv.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void updateContinueCardVisibility() {
        if (cardContinueReading == null) return;
        boolean hasQuery = currentSearchQuery != null && currentSearchQuery.length() > 0;
        cardContinueReading.setVisibility((hasQuery || currentMode != MODE_SURAHS) ? View.GONE : View.VISIBLE);
    }
```

4. **`loadSurahsAsync()` içinde (eski 199. satır civarı `surahAdapter.setItems(loaded);` öncesi)** `surahNameMap`'i doldur:
```java
                if (isAdded() && surahAdapter != null && loaded != null) {
                    surahNameMap.clear();
                    for (SurahItem s : loaded) {
                        surahNameMap.put(s.id, s.name);
                    }
                    surahAdapter.setItems(loaded);
                    if (juzAdapter != null) {
                        juzAdapter.notifyDataSetChanged();
                    }
                }
```

5. **Dosyanın en sonundaki `}` (eski 397. satır) hemen öncesine** şu iki yeni adapter sınıfını ekle:
```java
    private class VerseSearchAdapter extends RecyclerView.Adapter<VerseSearchAdapter.ViewHolder> {
        private final List<QuranVerseEntity> items = new ArrayList<>();

        VerseSearchAdapter(List<QuranVerseEntity> initial) {
            if (initial != null) items.addAll(initial);
        }

        void setItems(List<QuranVerseEntity> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_surah, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            QuranVerseEntity v = items.get(position);
            String surahName = surahNameMap.get(v.surahId);
            if (surahName == null || surahName.isEmpty()) {
                surahName = getString(R.string.quran_surah_format, v.surahId);
            }
            holder.txtId.setText(v.surahId + ":" + v.verseId);
            holder.txtNameTr.setText(surahName + " • " + getString(R.string.quran_verse_format, v.verseId));
            holder.txtNameAr.setText("");
            holder.txtDesc.setText(v.trText != null ? v.trText : "");
            String finalName = surahName;
            holder.itemView.setOnClickListener(view -> openSurah(v.surahId, finalName, v.verseId));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtId, txtNameTr, txtNameAr, txtDesc;

            ViewHolder(View v) {
                super(v);
                txtId = v.findViewById(R.id.surah_id);
                txtNameTr = v.findViewById(R.id.surah_name_tr);
                txtNameAr = v.findViewById(R.id.surah_name_ar);
                txtDesc = v.findViewById(R.id.surah_desc);
            }
        }
    }

    private class JuzAdapter extends RecyclerView.Adapter<JuzAdapter.ViewHolder> {
        private final List<JuzHelper.JuzInfo> fullItems = new ArrayList<>();
        private final List<JuzHelper.JuzInfo> visibleItems = new ArrayList<>();

        JuzAdapter(List<JuzHelper.JuzInfo> items) {
            if (items != null) {
                fullItems.addAll(items);
                visibleItems.addAll(items);
            }
        }

        void filter(String query) {
            visibleItems.clear();
            if (query == null || query.trim().isEmpty()) {
                visibleItems.addAll(fullItems);
            } else {
                Locale trLocale = new Locale("tr");
                String q = query.toLowerCase(trLocale).trim();
                for (JuzHelper.JuzInfo j : fullItems) {
                    String sName = surahNameMap.get(j.startSurahId, "");
                    String label = getString(R.string.quran_juz_format, j.juzNumber) + " " + sName;
                    if (label.toLowerCase(trLocale).contains(q) || String.valueOf(j.juzNumber).equals(q)) {
                        visibleItems.add(j);
                    }
                }
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_surah, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            JuzHelper.JuzInfo j = visibleItems.get(position);
            String surahName = surahNameMap.get(j.startSurahId);
            if (surahName == null || surahName.isEmpty()) {
                surahName = getString(R.string.quran_surah_format, j.startSurahId);
            }
            holder.txtId.setText(String.valueOf(j.juzNumber));
            holder.txtNameTr.setText(getString(R.string.quran_juz_format, j.juzNumber));
            holder.txtNameAr.setText("");
            holder.txtDesc.setText(getString(R.string.quran_juz_start_format, surahName, j.startVerseId));
            String finalName = surahName;
            holder.itemView.setOnClickListener(v -> openSurah(j.startSurahId, finalName, j.startVerseId));
        }

        @Override
        public int getItemCount() {
            return visibleItems.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView txtId, txtNameTr, txtNameAr, txtDesc;

            ViewHolder(View v) {
                super(v);
                txtId = v.findViewById(R.id.surah_id);
                txtNameTr = v.findViewById(R.id.surah_name_tr);
                txtNameAr = v.findViewById(R.id.surah_name_ar);
                txtDesc = v.findViewById(R.id.surah_desc);
            }
        }
    }
```

**Kodun Anlamı:**
- `scheduleVerseSearch`: Kullanıcı yazarken her tuşta veritabanını yormamak için `220ms` debounce uygular; 3 karakterden azsa uyarı metni gösterir.
- `runVerseSearchAsync`: Önce Room FTS4 ile arar (`query*`), Türkçe eklerden dolayı boş dönerse `LIKE` sorgusuna düşer. Tıklanan sonuç `SurahDetailActivity`'yi `verse_id` ile tam o ayete kaydırarak açar.
- `JuzAdapter`: Mevcut `item_surah.xml` kart tasarımını yeniden kullanır, böylece ekstra layout dosyası yazmadan UI birebir uyumlu kalır.

### Adım 3.5 — Yasin Doğrulama & Push
```bash
./gradlew.bat testDebugUnitTest assembleDebug
git add .
git commit -m "feat(etap2): implement Quran Room import, FTS4 verse search and 30 Juz navigation"
git push origin main
```

---

## TUR 4 — BEYZA: Ses Tercihleri, Hafız Yardımcıları ve `QuranAudioService` (Media3)

> **Meet Durumu:** Beyza `git pull --rebase origin main` yapar ve ekran paylaşır.

### Adım 4.1 — `QuranPrefsHelper.java` Genişletmesi
**Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/util/QuranPrefsHelper.java`

1. **23. satırın (`private static final String KEY_SHOW_TRANSLATION = ...`) altına (24. satıra)** ekle:
```java
    private static final String KEY_RECITER_INDEX = "quran_reciter_index";
    private static final String KEY_REPEAT_COUNT = "quran_repeat_count";
```

2. **113. satırın (`setShowTranslation` kapanış `}` sonrası, 114. satıra)** ekle:
```java
    public static int getReciterIndex(Context context) {
        if (context == null) return 0;
        return context.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE).getInt(KEY_RECITER_INDEX, 0);
    }

    public static void setReciterIndex(Context context, int index) {
        if (context == null) return;
        context.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE).edit()
                .putInt(KEY_RECITER_INDEX, Math.max(0, Math.min(2, index)))
                .apply();
    }

    public static int getRepeatCount(Context context) {
        if (context == null) return 1;
        int val = context.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE).getInt(KEY_REPEAT_COUNT, 1);
        return (val == 3 || val == 5) ? val : 1;
    }

    public static void setRepeatCount(Context context, int count) {
        if (context == null) return;
        int safe = (count == 3 || count == 5) ? count : 1;
        context.getSharedPreferences(PREFS_QURAN, Context.MODE_PRIVATE).edit()
                .putInt(KEY_REPEAT_COUNT, safe)
                .apply();
    }
```

**Kodun Anlamı:** Kullanıcının seçtiği hafızı (`0..2`) ve ezber tekrar sayısını (`1x / 3x / 5x`) kalıcı olarak saklar.

---

### Adım 4.2 — `QuranReciterHelper.java` Oluşturma
**Yeni Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/util/QuranReciterHelper.java`

```java
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
```

**Kodun Anlamı:** EveryAyah sunucusundaki 3 haneli sure + 3 haneli ayet formatını (`002255.mp3` gibi) üretir. Saf Java metodu olduğu için birim testiyle de doğrulanabilir.

---

### Adım 4.3 — `QuranAudioService.java` Oluşturma (ExoPlayer + MediaSession + Bildirim)
**Yeni Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/QuranAudioService.java`

```java
package com.korkutsoftware.hilalim;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;

import com.korkutsoftware.hilalim.util.QuranPrefsHelper;
import com.korkutsoftware.hilalim.util.QuranReciterHelper;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class QuranAudioService extends Service {

    private static final String CHANNEL_ID = "Hilalim_Quran_Audio_Channel";
    private static final int NOTIFICATION_ID = 9101;

    public static final String ACTION_PLAY_VERSE = "com.korkutsoftware.hilalim.ACTION_QURAN_PLAY_VERSE";
    public static final String ACTION_TOGGLE_PLAY_PAUSE = "com.korkutsoftware.hilalim.ACTION_QURAN_TOGGLE";
    public static final String ACTION_NEXT_VERSE = "com.korkutsoftware.hilalim.ACTION_QURAN_NEXT";
    public static final String ACTION_PREV_VERSE = "com.korkutsoftware.hilalim.ACTION_QURAN_PREV";
    public static final String ACTION_STOP = "com.korkutsoftware.hilalim.ACTION_QURAN_STOP";
    public static final String ACTION_SEEK_TO = "com.korkutsoftware.hilalim.ACTION_QURAN_SEEK";
    public static final String ACTION_RELOAD_SETTINGS = "com.korkutsoftware.hilalim.ACTION_QURAN_RELOAD";

    public static final String EXTRA_SURAH_ID = "extra_surah_id";
    public static final String EXTRA_SURAH_NAME = "extra_surah_name";
    public static final String EXTRA_VERSE_ID = "extra_verse_id";
    public static final String EXTRA_MAX_VERSES = "extra_max_verses";
    public static final String EXTRA_SEEK_POS_MS = "extra_seek_pos_ms";

    public interface AudioStateListener {
        void onAudioStateChanged(boolean active, boolean playing, int surahId, int verseId,
                                 String surahName, long positionMs, long durationMs, int repeatTarget);
    }

    private static final Set<AudioStateListener> LISTENERS = new CopyOnWriteArraySet<>();

    private static volatile boolean active = false;
    private static volatile boolean playing = false;
    private static volatile int currentSurahId = 1;
    private static volatile int currentVerseId = 1;
    private static volatile int currentMaxVerses = 7;
    private static volatile String currentSurahName = "";
    private static volatile long currentPosMs = 0L;
    private static volatile long currentDurMs = 0L;

    private ExoPlayer player;
    private MediaSession mediaSession;
    private int repeatRemaining = 1;
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && active) {
                currentPosMs = Math.max(0L, player.getCurrentPosition());
                long dur = player.getDuration();
                currentDurMs = dur != C.TIME_UNSET ? Math.max(0L, dur) : 0L;
                notifyListeners();
                if (player.isPlaying()) {
                    progressHandler.postDelayed(this, 400);
                }
            }
        }
    };

    public static void addListener(AudioStateListener listener) {
        if (listener != null) {
            LISTENERS.add(listener);
        }
    }

    public static void removeListener(AudioStateListener listener) {
        if (listener != null) {
            LISTENERS.remove(listener);
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static boolean isPlayingNow() {
        return active && playing;
    }

    public static int getCurrentSurahId() {
        return currentSurahId;
    }

    public static int getCurrentVerseId() {
        return currentVerseId;
    }

    public static String getCurrentSurahName() {
        return currentSurahName;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build();
        player = new ExoPlayer.Builder(this)
                .setAudioAttributes(attrs, true)
                .setHandleAudioBecomingNoisy(true)
                .build();
        mediaSession = new MediaSession.Builder(this, player).build();

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                playing = isPlaying;
                updateNotification();
                progressHandler.removeCallbacks(progressRunnable);
                if (isPlaying) {
                    progressHandler.post(progressRunnable);
                } else {
                    notifyListeners();
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    long dur = player.getDuration();
                    currentDurMs = dur != C.TIME_UNSET ? Math.max(0L, dur) : 0L;
                    notifyListeners();
                } else if (playbackState == Player.STATE_ENDED) {
                    handleVerseEnded();
                }
            }

            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Toast.makeText(getApplicationContext(), R.string.quran_audio_network_error, Toast.LENGTH_LONG).show();
                stopAudioService();
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        switch (action) {
            case ACTION_PLAY_VERSE:
                currentSurahId = intent.getIntExtra(EXTRA_SURAH_ID, 1);
                currentVerseId = intent.getIntExtra(EXTRA_VERSE_ID, 1);
                currentMaxVerses = Math.max(1, intent.getIntExtra(EXTRA_MAX_VERSES, 7));
                String sName = intent.getStringExtra(EXTRA_SURAH_NAME);
                currentSurahName = sName != null ? sName : getString(R.string.quran_surah_format, currentSurahId);
                repeatRemaining = QuranPrefsHelper.getRepeatCount(this);
                startForeground(NOTIFICATION_ID, buildNotification());
                playCurrentVerse();
                break;

            case ACTION_TOGGLE_PLAY_PAUSE:
                if (player != null && active) {
                    if (player.isPlaying()) {
                        player.pause();
                    } else {
                        player.play();
                    }
                }
                break;

            case ACTION_NEXT_VERSE:
                if (active && currentVerseId < currentMaxVerses) {
                    currentVerseId++;
                    repeatRemaining = QuranPrefsHelper.getRepeatCount(this);
                    playCurrentVerse();
                }
                break;

            case ACTION_PREV_VERSE:
                if (active && currentVerseId > 1) {
                    currentVerseId--;
                    repeatRemaining = QuranPrefsHelper.getRepeatCount(this);
                    playCurrentVerse();
                }
                break;

            case ACTION_SEEK_TO:
                if (player != null && active) {
                    long posMs = intent.getLongExtra(EXTRA_SEEK_POS_MS, 0L);
                    player.seekTo(Math.max(0L, posMs));
                    currentPosMs = Math.max(0L, posMs);
                    notifyListeners();
                }
                break;

            case ACTION_RELOAD_SETTINGS:
                if (active) {
                    repeatRemaining = QuranPrefsHelper.getRepeatCount(this);
                    playCurrentVerse();
                }
                break;

            case ACTION_STOP:
                stopAudioService();
                break;
        }
        return START_NOT_STICKY;
    }

    private void playCurrentVerse() {
        if (player == null) return;
        active = true;
        currentPosMs = 0L;
        currentDurMs = 0L;
        int reciterIdx = QuranPrefsHelper.getReciterIndex(this);
        String url = QuranReciterHelper.buildVerseAudioUrl(reciterIdx, currentSurahId, currentVerseId);
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        player.play();
        updateNotification();
        notifyListeners();
    }

    private void handleVerseEnded() {
        if (repeatRemaining > 1) {
            repeatRemaining--;
            if (player != null) {
                player.seekTo(0);
                player.play();
            }
            return;
        }
        if (currentVerseId < currentMaxVerses) {
            currentVerseId++;
            repeatRemaining = QuranPrefsHelper.getRepeatCount(this);
            playCurrentVerse();
        } else {
            stopAudioService();
        }
    }

    private void stopAudioService() {
        active = false;
        playing = false;
        currentPosMs = 0L;
        currentDurMs = 0L;
        progressHandler.removeCallbacks(progressRunnable);
        if (player != null) {
            player.stop();
        }
        notifyListeners();
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void notifyListeners() {
        int repeatTarget = QuranPrefsHelper.getRepeatCount(this);
        for (AudioStateListener listener : LISTENERS) {
            listener.onAudioStateChanged(
                    active,
                    playing,
                    currentSurahId,
                    currentVerseId,
                    currentSurahName,
                    currentPosMs,
                    currentDurMs,
                    repeatTarget
            );
        }
    }

    private void updateNotification() {
        if (!active) return;
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.quran_audio_notif_channel),
                    NotificationManager.IMPORTANCE_LOW
            );
            ch.setDescription(getString(R.string.quran_audio_notif_desc));
            ch.setShowBadge(false);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(ch);
        }

        Intent openIntent = new Intent(this, SurahDetailActivity.class);
        openIntent.putExtra("surah_id", currentSurahId);
        openIntent.putExtra("surah_name", currentSurahName);
        openIntent.putExtra("verse_id", currentVerseId);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPending = PendingIntent.getActivity(
                this, 201, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent togglePending = PendingIntent.getService(
                this, 202,
                new Intent(this, QuranAudioService.class).setAction(ACTION_TOGGLE_PLAY_PAUSE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent nextPending = PendingIntent.getService(
                this, 203,
                new Intent(this, QuranAudioService.class).setAction(ACTION_NEXT_VERSE),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        PendingIntent stopPending = PendingIntent.getService(
                this, 204,
                new Intent(this, QuranAudioService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String reciterName = QuranReciterHelper.getReciterName(this, QuranPrefsHelper.getReciterIndex(this));
        int toggleIcon = playing ? R.drawable.ic_pause : R.drawable.ic_play_arrow;
        String toggleText = getString(playing ? R.string.quran_pause_verse_desc : R.string.quran_play_verse_desc);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_quran_rounded)
                .setContentTitle(currentSurahName + " • " + getString(R.string.quran_verse_format, currentVerseId))
                .setContentText(reciterName)
                .setContentIntent(contentPending)
                .setOngoing(playing)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .addAction(toggleIcon, toggleText, togglePending)
                .addAction(R.drawable.ic_skip_next, getString(R.string.desc_next_surah), nextPending)
                .addAction(R.drawable.ic_close, getString(R.string.ezan_stop_action), stopPending)
                .build();
    }

    @Override
    public void onDestroy() {
        progressHandler.removeCallbacks(progressRunnable);
        active = false;
        playing = false;
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }
}
```

**Kodun Anlamı:**
- `setAudioAttributes(attrs, true)` ikinci parametresi `true` verilerek **AudioFocus** yönetimi otomatik yapılır (telefon çalınca durur).
- `setHandleAudioBecomingNoisy(true)` kulaklık çıkarılınca tilaveti otomatik durdurur.
- `handleVerseEnded()`: Ayet bitince `repeatRemaining` 1'den büyükse aynı ayeti başa sarar (`1x/3x/5x` ezber modu); hakları bitince sonraki ayete geçer.
- `onPlayerError()`: İnternet kapalıysa veya sunucu hatasında uygulamayı çökertmek yerine uyarı `Toast` gösterip servisi temizce kapatır.

---

### Adım 4.4 — `AndroidManifest.xml` İçine Servis Kaydı
**Dosya:** `app/src/main/AndroidManifest.xml`

**132. satırın (`EzanPlayerService` kapanış `/>`) hemen altına (133. satıra)** ekle:
```xml
        <service
            android:name=".QuranAudioService"
            android:exported="false"
            android:foregroundServiceType="mediaPlayback" />
```

---

### Adım 4.5 — `AppCalculatorsTest.java` İçine Hafız URL Birim Testi Ekleme
**Dosya:** `app/src/test/java/com/korkutsoftware/hilalim/AppCalculatorsTest.java`

1. `JuzHelper` importunun altına ekle:
```java
import com.korkutsoftware.hilalim.util.QuranReciterHelper;
```
2. Sınıf sonu `}` öncesine ekle:
```java
    @Test
    public void quranReciterHelper_buildsZeroPaddedUrls() {
        assertEquals(
                "https://everyayah.com/data/Alafasy_128kbps/001001.mp3",
                QuranReciterHelper.buildVerseAudioUrl(0, 1, 1)
        );
        assertEquals(
                "https://everyayah.com/data/Husary_128kbps/002255.mp3",
                QuranReciterHelper.buildVerseAudioUrl(1, 2, 255)
        );
        assertEquals(
                "https://everyayah.com/data/Abdul_Basit_Murattal_192kbps/114006.mp3",
                QuranReciterHelper.buildVerseAudioUrl(2, 114, 6)
        );
    }
```

### Adım 4.6 — Beyza Doğrulama & Push
```bash
./gradlew.bat testDebugUnitTest assembleDebug
git add .
git commit -m "feat(etap2): add QuranReciterHelper, QuranAudioService with Media3 and repeat support"
git push origin main
```

---

## TUR 5 — YASİN: Mini Audio Player Bar, Ayet Play İkonu & `SurahDetailActivity` Entegrasyonu

> **Meet Durumu:** Yasin `git pull --rebase origin main` yapar ve ekran paylaşır.

### Adım 5.1 — `item_verse.xml` Güncellemesi (Kart ID + Play İkonu)
**Dosya:** `app/src/main/res/layout/item_verse.xml`

1. **2. satırdaki `<com.google.android.material.card.MaterialCardView` etiketinin içine (3. satıra)** `android:id` ekle:
```xml
    android:id="@+id/card_verse"
```
2. **49. satırın (`</View>` spacer kapanışının) hemen altına (50. satıra)** `btn_play_verse` ikonunu ekle:
```xml
            <ImageView
                android:id="@+id/btn_play_verse"
                android:layout_width="36dp"
                android:layout_height="36dp"
                android:layout_marginEnd="4dp"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:clickable="true"
                android:contentDescription="@string/quran_play_verse_desc"
                android:focusable="true"
                android:padding="7dp"
                android:src="@drawable/ic_play_arrow"
                app:tint="?attr/colorSecondary" />
```

**Kodun Anlamı:** Her ayet kartının üst araç çubuğunda kalp/paylaş/kopyala yanına küçük bir dinle butonu eklenir. `card_verse` ID'si sayesinde o an çalan ayet altın kenarlıkla vurgulanabilecektir.

---

### Adım 5.2 — `activity_surah_detail.xml` Güncellemesi (Hafız Butonu + Mini Player Bar)
**Dosya:** `app/src/main/res/layout/activity_surah_detail.xml`

1. **78. satırdaki `<TextView android:id="@+id/btn_text_settings"` öncesine** Hafız seçici butonunu ekle:
```xml
            <ImageView
                android:id="@+id/btn_reciter"
                android:layout_width="36dp"
                android:layout_height="36dp"
                android:layout_marginEnd="6dp"
                android:background="@drawable/bg_hero_icon"
                android:clickable="true"
                android:contentDescription="@string/quran_reciter_select"
                android:focusable="true"
                android:foreground="?attr/selectableItemBackgroundBorderless"
                android:padding="8dp"
                android:src="@drawable/ic_volume_up"
                app:tint="@color/gold" />
```

2. **129. satırdaki `app:layout_constraintBottom_toBottomOf="parent"`** satırını (`rv_verses` içinde) şununla değiştir:
```xml
        app:layout_constraintBottom_toTopOf="@id/mini_player_card"
```

3. **157. satırın (`</TextView>` yani `txt_no_content` kapanışının) hemen altına (158. satıra, kök `</androidx.constraintlayout.widget.ConstraintLayout>` kapanışından önce)** Mini Audio Player Bar'ı ekle:
```xml
    <!-- Sabit Mini Audio Player Bar -->
    <com.google.android.material.card.MaterialCardView
        android:id="@+id/mini_player_card"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginHorizontal="14dp"
        android:layout_marginBottom="10dp"
        android:visibility="gone"
        app:cardCornerRadius="20dp"
        app:cardElevation="8dp"
        app:layout_constraintBottom_toBottomOf="parent"
        app:strokeColor="@color/gold"
        app:strokeWidth="1dp">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_dark_gradient_card"
            android:orientation="vertical"
            android:paddingHorizontal="14dp"
            android:paddingTop="10dp"
            android:paddingBottom="8dp">

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <ImageView
                    android:id="@+id/btn_mini_play_pause"
                    android:layout_width="38dp"
                    android:layout_height="38dp"
                    android:background="@drawable/bg_hero_icon"
                    android:clickable="true"
                    android:contentDescription="@string/quran_play_verse_desc"
                    android:focusable="true"
                    android:padding="8dp"
                    android:src="@drawable/ic_pause"
                    app:tint="@color/gold" />

                <LinearLayout
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_marginHorizontal="10dp"
                    android:layout_weight="1"
                    android:orientation="vertical">

                    <TextView
                        android:id="@+id/txt_mini_title"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:ellipsize="end"
                        android:maxLines="1"
                        android:textColor="@color/white"
                        android:textSize="13sp"
                        android:textStyle="bold" />

                    <TextView
                        android:id="@+id/txt_mini_reciter"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:ellipsize="end"
                        android:maxLines="1"
                        android:textColor="@color/gold"
                        android:textSize="11sp" />
                </LinearLayout>

                <TextView
                    android:id="@+id/btn_mini_repeat"
                    android:layout_width="wrap_content"
                    android:layout_height="32dp"
                    android:layout_marginEnd="4dp"
                    android:background="@drawable/bg_hero_icon"
                    android:clickable="true"
                    android:focusable="true"
                    android:gravity="center"
                    android:minWidth="36dp"
                    android:paddingHorizontal="8dp"
                    android:text="@string/quran_repeat_format"
                    android:textColor="@color/gold"
                    android:textSize="12sp"
                    android:textStyle="bold" />

                <ImageView
                    android:id="@+id/btn_mini_prev"
                    android:layout_width="32dp"
                    android:layout_height="32dp"
                    android:background="?attr/selectableItemBackgroundBorderless"
                    android:clickable="true"
                    android:contentDescription="@string/desc_prev_surah"
                    android:focusable="true"
                    android:padding="5dp"
                    android:src="@drawable/ic_skip_previous"
                    app:tint="@color/white" />

                <ImageView
                    android:id="@+id/btn_mini_next"
                    android:layout_width="32dp"
                    android:layout_height="32dp"
                    android:background="?attr/selectableItemBackgroundBorderless"
                    android:clickable="true"
                    android:contentDescription="@string/desc_next_surah"
                    android:focusable="true"
                    android:padding="5dp"
                    android:src="@drawable/ic_skip_next"
                    app:tint="@color/white" />

                <ImageView
                    android:id="@+id/btn_mini_close"
                    android:layout_width="32dp"
                    android:layout_height="32dp"
                    android:background="?attr/selectableItemBackgroundBorderless"
                    android:clickable="true"
                    android:contentDescription="@string/ezan_stop_action"
                    android:focusable="true"
                    android:padding="6dp"
                    android:src="@drawable/ic_close"
                    app:tint="@color/gold" />
            </LinearLayout>

            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="4dp"
                android:gravity="center_vertical"
                android:orientation="horizontal">

                <SeekBar
                    android:id="@+id/seek_mini_audio"
                    android:layout_width="0dp"
                    android:layout_height="wrap_content"
                    android:layout_weight="1"
                    android:progressTint="@color/gold"
                    android:thumbTint="@color/gold" />

                <TextView
                    android:id="@+id/txt_mini_time"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:layout_marginStart="6dp"
                    android:text="00:00 / 00:00"
                    android:textColor="@color/white"
                    android:textSize="11sp" />
            </LinearLayout>
        </LinearLayout>
    </com.google.android.material.card.MaterialCardView>
```

**Kodun Anlamı:** `rv_verses` alt kenarı `mini_player_card` üstüne sabitlendiği için Mini Player görünür olduğunda son ayetin okunmasını veya alt butonları kesinlikle kapatmaz (Edge-to-Edge uyumlu).

---

### Adım 5.3 — `SurahDetailActivity.java` Entegrasyonu
**Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/SurahDetailActivity.java`

1. **Import bloğuna (25. satır `import com.korkutsoftware.hilalim.util.ShareCardHelper;` altına)** ekle:
```java
import android.content.Intent;
import android.widget.SeekBar;

import androidx.core.graphics.ColorUtils;

import com.google.android.material.card.MaterialCardView;
import com.korkutsoftware.hilalim.data.db.AppDatabase;
import com.korkutsoftware.hilalim.data.db.QuranVerseEntity;
import com.korkutsoftware.hilalim.util.QuranImportHelper;
import com.korkutsoftware.hilalim.util.QuranReciterHelper;

import java.util.Locale;
```

2. **Sınıf imzasını (`public class SurahDetailActivity extends BaseActivity` — 36. satır)** şu şekilde güncelle:
```java
public class SurahDetailActivity extends BaseActivity implements QuranAudioService.AudioStateListener {
```

3. **46. satırın (`private VerseAdapter currentAdapter = null;` sonrası)** yeni alanları ekle:
```java
    private View miniPlayerCard;
    private ImageView btnMiniPlayPause;
    private TextView txtMiniTitle, txtMiniReciter, txtMiniRepeat, txtMiniTime;
    private SeekBar seekMiniAudio;
    private boolean userSeeking = false;
    private int playingVerseId = -1;
```

4. **73. satırın (`btnTextSettings` listener kapanışı sonrası, `rv = findViewById(R.id.rv_verses);` üstüne)** ekle:
```java
        View btnReciter = findViewById(R.id.btn_reciter);
        if (btnReciter != null) {
            btnReciter.setOnClickListener(v -> showReciterDialog());
        }
        setupMiniPlayer();
```

5. **102. satırın (`onPause` öncesine)** `onStart` ve `onStop` ekle:
```java
    @Override
    protected void onStart() {
        super.onStart();
        QuranAudioService.addListener(this);
        syncMiniPlayerInitialState();
    }

    @Override
    protected void onStop() {
        QuranAudioService.removeListener(this);
        super.onStop();
    }
```

6. **155. satırın (`showTextSettingsDialog` kapanışı sonrası, `navigateSurah` üstüne)** Mini Player ve Hafız Seçici metodlarını ekle:
```java
    private void setupMiniPlayer() {
        miniPlayerCard = findViewById(R.id.mini_player_card);
        btnMiniPlayPause = findViewById(R.id.btn_mini_play_pause);
        txtMiniTitle = findViewById(R.id.txt_mini_title);
        txtMiniReciter = findViewById(R.id.txt_mini_reciter);
        txtMiniRepeat = findViewById(R.id.btn_mini_repeat);
        txtMiniTime = findViewById(R.id.txt_mini_time);
        seekMiniAudio = findViewById(R.id.seek_mini_audio);

        if (btnMiniPlayPause != null) {
            btnMiniPlayPause.setOnClickListener(v -> sendAudioAction(QuranAudioService.ACTION_TOGGLE_PLAY_PAUSE));
        }
        View btnPrev = findViewById(R.id.btn_mini_prev);
        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> sendAudioAction(QuranAudioService.ACTION_PREV_VERSE));
        }
        View btnNext = findViewById(R.id.btn_mini_next);
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> sendAudioAction(QuranAudioService.ACTION_NEXT_VERSE));
        }
        View btnClose = findViewById(R.id.btn_mini_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> sendAudioAction(QuranAudioService.ACTION_STOP));
        }
        if (txtMiniRepeat != null) {
            txtMiniRepeat.setOnClickListener(v -> {
                int current = QuranPrefsHelper.getRepeatCount(this);
                int next = current == 1 ? 3 : (current == 3 ? 5 : 1);
                QuranPrefsHelper.setRepeatCount(this, next);
                txtMiniRepeat.setText(getString(R.string.quran_repeat_format, next));
            });
        }
        if (seekMiniAudio != null) {
            seekMiniAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    userSeeking = true;
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    userSeeking = false;
                    Intent intent = new Intent(SurahDetailActivity.this, QuranAudioService.class);
                    intent.setAction(QuranAudioService.ACTION_SEEK_TO);
                    intent.putExtra(QuranAudioService.EXTRA_SEEK_POS_MS, (long) seekBar.getProgress());
                    startService(intent);
                }
            });
        }
    }

    private void syncMiniPlayerInitialState() {
        if (QuranAudioService.isActive()) {
            onAudioStateChanged(
                    true,
                    QuranAudioService.isPlayingNow(),
                    QuranAudioService.getCurrentSurahId(),
                    QuranAudioService.getCurrentVerseId(),
                    QuranAudioService.getCurrentSurahName(),
                    0L,
                    0L,
                    QuranPrefsHelper.getRepeatCount(this)
            );
        } else if (miniPlayerCard != null) {
            miniPlayerCard.setVisibility(View.GONE);
            playingVerseId = -1;
        }
    }

    private void showReciterDialog() {
        int currentIdx = QuranPrefsHelper.getReciterIndex(this);
        String[] reciters = QuranReciterHelper.getReciterNames(this);
        new AlertDialog.Builder(this)
                .setTitle(R.string.quran_reciter_select)
                .setSingleChoiceItems(reciters, currentIdx, (dialog, which) -> {
                    QuranPrefsHelper.setReciterIndex(this, which);
                    if (QuranAudioService.isActive()) {
                        sendAudioAction(QuranAudioService.ACTION_RELOAD_SETTINGS);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void playVerse(int verseId) {
        if (currentAdapter == null || currentAdapter.items.isEmpty()) return;
        int maxVerses = currentAdapter.items.size();
        if (QuranAudioService.isActive()
                && QuranAudioService.getCurrentSurahId() == currentSurahId
                && QuranAudioService.getCurrentVerseId() == verseId) {
            sendAudioAction(QuranAudioService.ACTION_TOGGLE_PLAY_PAUSE);
            return;
        }
        Intent intent = new Intent(this, QuranAudioService.class);
        intent.setAction(QuranAudioService.ACTION_PLAY_VERSE);
        intent.putExtra(QuranAudioService.EXTRA_SURAH_ID, currentSurahId);
        intent.putExtra(QuranAudioService.EXTRA_SURAH_NAME, currentSurahName);
        intent.putExtra(QuranAudioService.EXTRA_VERSE_ID, verseId);
        intent.putExtra(QuranAudioService.EXTRA_MAX_VERSES, maxVerses);
        ContextCompat.startForegroundService(this, intent);
    }

    private void sendAudioAction(String action) {
        Intent intent = new Intent(this, QuranAudioService.class);
        intent.setAction(action);
        startService(intent);
    }

    @Override
    public void onAudioStateChanged(boolean active, boolean playing, int surahId, int verseId,
                                    String surahName, long positionMs, long durationMs, int repeatTarget) {
        mainHandler.post(() -> {
            if (isFinishing() || isDestroyed() || miniPlayerCard == null) return;
            if (!active) {
                miniPlayerCard.setVisibility(View.GONE);
                if (playingVerseId != -1) {
                    playingVerseId = -1;
                    if (currentAdapter != null) currentAdapter.notifyDataSetChanged();
                }
                return;
            }
            miniPlayerCard.setVisibility(View.VISIBLE);
            if (btnMiniPlayPause != null) {
                btnMiniPlayPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
            }
            if (txtMiniTitle != null) {
                txtMiniTitle.setText(surahName + " • " + getString(R.string.quran_verse_format, verseId));
            }
            if (txtMiniReciter != null) {
                txtMiniReciter.setText(QuranReciterHelper.getReciterName(this, QuranPrefsHelper.getReciterIndex(this)));
            }
            if (txtMiniRepeat != null) {
                txtMiniRepeat.setText(getString(R.string.quran_repeat_format, repeatTarget));
            }
            if (seekMiniAudio != null && !userSeeking) {
                int max = durationMs > 0 ? (int) durationMs : 100;
                seekMiniAudio.setMax(max);
                seekMiniAudio.setProgress(durationMs > 0 ? (int) Math.min(positionMs, durationMs) : 0);
            }
            if (txtMiniTime != null) {
                txtMiniTime.setText(durationMs > 0
                        ? formatMs(positionMs) + " / " + formatMs(durationMs)
                        : getString(R.string.quran_audio_loading));
            }

            int newHighlight = (surahId == currentSurahId) ? verseId : -1;
            if (newHighlight != playingVerseId) {
                playingVerseId = newHighlight;
                if (currentAdapter != null) {
                    currentAdapter.notifyDataSetChanged();
                    if (playingVerseId > 0) {
                        for (int i = 0; i < currentAdapter.items.size(); i++) {
                            if (currentAdapter.items.get(i).id == playingVerseId) {
                                rv.smoothScrollToPosition(i);
                                break;
                            }
                        }
                    }
                }
            } else if (currentAdapter != null) {
                currentAdapter.notifyDataSetChanged();
            }
        });
    }

    private String formatMs(long ms) {
        long totalSec = Math.max(0L, ms / 1000L);
        long min = totalSec / 60L;
        long sec = totalSec % 60L;
        return String.format(Locale.US, "%02d:%02d", min, sec);
    }
```

7. **`loadVersesFromJson(int surahId)` başına (262. satır `try {` hemen altına)** Room'dan hızlı okuma desteği ekle:
```java
        try {
            if (QuranImportHelper.isImported(this)) {
                List<QuranVerseEntity> dbVerses = AppDatabase.getInstance(this).quranDao().getVersesBySurah(surahId);
                if (dbVerses != null && !dbVerses.isEmpty()) {
                    List<VerseItem> mapped = new ArrayList<>(dbVerses.size());
                    for (QuranVerseEntity e : dbVerses) {
                        VerseItem vi = new VerseItem();
                        vi.id = e.verseId;
                        vi.ar = e.arText;
                        vi.tr = e.trText;
                        mapped.add(vi);
                    }
                    return mapped;
                }
            }
```
*(Eski `try {` satırını silmeden hemen üstüne/yerine koy, çift `try` açma!)*

8. **`VerseAdapter` içinde (`onBindViewHolder` ve `ViewHolder`, 320–406. satırlar):**
- `ViewHolder` alanlarına (394. satır civarı):
  `MaterialCardView cardVerse;` ve `ImageView btnPlay;` ekle; `ViewHolder(View v)` içinde bağla:
  ```java
  cardVerse = v.findViewById(R.id.card_verse);
  btnPlay = v.findViewById(R.id.btn_play_verse);
  ```
- `onBindViewHolder` sonuna (365. satır `btnCopy` listener altına) aktif ayet vurgusu ve Play tıklamasını ekle:
```java
            boolean isThisPlayingVerse = (item.id == playingVerseId);
            if (holder.cardVerse != null) {
                int strokeWidthPx = Math.round(getResources().getDisplayMetrics().density * (isThisPlayingVerse ? 2 : 1));
                holder.cardVerse.setStrokeWidth(strokeWidthPx);
                holder.cardVerse.setStrokeColor(ContextCompat.getColor(
                        SurahDetailActivity.this,
                        isThisPlayingVerse ? R.color.gold : R.color.card_stroke
                ));
                int baseCardBg = ContextCompat.getColor(SurahDetailActivity.this, R.color.card_bg);
                int bg = isThisPlayingVerse
                        ? ColorUtils.compositeColors(
                                ContextCompat.getColor(SurahDetailActivity.this, R.color.selected_lang_bg),
                                baseCardBg)
                        : baseCardBg;
                holder.cardVerse.setCardBackgroundColor(bg);
            }
            if (holder.btnPlay != null) {
                boolean showPause = isThisPlayingVerse && QuranAudioService.isPlayingNow();
                holder.btnPlay.setImageResource(showPause ? R.drawable.ic_pause : R.drawable.ic_play_arrow);
                holder.btnPlay.setOnClickListener(v -> playVerse(item.id));
            }
```

**Kodun Anlamı:**
- `ColorUtils.compositeColors(...)` ile yarı saydam altın vurgusu kart arka planının üstüne bindirilir; hem Light modda hem Dark modda net görünür.
- Ayet otomatik ilerledikçe `rv.smoothScrollToPosition(i)` çalışarak okunan ayeti kullanıcı ekranına getirir.

### Adım 5.4 — Yasin Doğrulama & Push
```bash
./gradlew.bat testDebugUnitTest assembleDebug
git add .
git commit -m "feat(etap2): integrate Mini Audio Player, verse highlighting and reciter dialog"
git push origin main
```

---

## TUR 6 — BEYZA: Çoklu Ezan Makamı, Özel Erken Uyarı & Namazda DND Altyapısı

> **Meet Durumu:** Beyza `git pull --rebase origin main` yapar ve ekran paylaşır.

### Adım 6.1 — `AndroidManifest.xml` İçine DND İzni Ekleme
**Dosya:** `app/src/main/AndroidManifest.xml`

**13. satırın (`<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />`) altına** ekle:
```xml
    <uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
```

**Kodun Anlamı:** Android 6+ cihazlarda `NotificationManager.setInterruptionFilter()` (Rahatsız Etmeyin modunu açıp kapatma) çağrısının `SecurityException` vermemesi için manifest izni şarttır.

---

### Adım 6.2 — `DndHelper.java` Oluşturma
**Yeni Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/util/DndHelper.java`

```java
package com.korkutsoftware.hilalim.util;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;

/**
 * Namaz vakti sonrasında telefonu geçici olarak Rahatsız Etmeyin (DND) moduna alır ve geri döndürür.
 */
public class DndHelper {

    public static final String PREF_DND_ENABLED = "notif_dnd_enabled";
    private static final String KEY_PREV_FILTER = "dnd_prev_filter";
    private static final String KEY_DND_ACTIVE_BY_APP = "dnd_active_by_app";

    public static boolean hasPolicyAccess(Context context) {
        if (context == null) return false;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm != null && nm.isNotificationPolicyAccessGranted();
    }

    public static void openPolicySettings(Context context) {
        if (context == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void enableDndForPrayer(Context context) {
        if (!hasPolicyAccess(context)) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        SharedPreferences prefs = context.getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        int current = nm.getCurrentInterruptionFilter();
        if (current != NotificationManager.INTERRUPTION_FILTER_ALL) {
            return; // Kullanıcı zaten kendisi DND açmışsa dokunma
        }
        prefs.edit()
                .putInt(KEY_PREV_FILTER, current)
                .putBoolean(KEY_DND_ACTIVE_BY_APP, true)
                .apply();
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
    }

    public static void restoreDndAfterPrayer(Context context) {
        if (!hasPolicyAccess(context)) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        SharedPreferences prefs = context.getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        if (!prefs.getBoolean(KEY_DND_ACTIVE_BY_APP, false)) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            int prev = prefs.getInt(KEY_PREV_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL);
            nm.setInterruptionFilter(prev);
        }
        prefs.edit().putBoolean(KEY_DND_ACTIVE_BY_APP, false).apply();
    }
}
```

**Kodun Anlamı:** Kullanıcı telefonu zaten elle sessize aldıysa (`current != INTERRUPTION_FILTER_ALL`) namaz bitince telefonun sesini yanlışlıkla açmaz; yalnızca uygulamanın kendisi DND başlattıysa eski haline döndürür.

---

### Adım 6.3 — `EzanPlayerService.java` Güncellemesi (5 Makam/Ses Modu)
**Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/EzanPlayerService.java`

1. **Import bloğuna (12. satır sonrası)** ekle:
```java
import android.content.SharedPreferences;
import android.media.RingtoneManager;
```

2. **28. satırın (`public static final String EXTRA_PRAYER_NAME = ...`) altına** makam sabitlerini ekle:
```java
    public static final String PREF_EZAN_SOUND_INDEX = "notif_ezan_sound_index";
    public static final int SOUND_CLASSIC = 0;
    public static final int SOUND_ISTANBUL = 1;
    public static final int SOUND_MAKKAH = 2;
    public static final int SOUND_SHORT_TONE = 3;
    public static final int SOUND_SILENT = 4;
```

3. **101–128. satırlar arasındaki `startAudio()` metodunu** aşağıdakiyle değiştir:
```java
    private void startAudio() {
        releasePlayer();
        SharedPreferences prefs = getSharedPreferences("hilalim_prefs", MODE_PRIVATE);
        int soundIndex = prefs.getInt(PREF_EZAN_SOUND_INDEX, SOUND_CLASSIC);
        if (soundIndex == SOUND_SILENT) {
            stopEzan();
            return;
        }

        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());

            Uri soundUri = resolveEzanSoundUri(soundIndex);
            mediaPlayer.setDataSource(this, soundUri);

            mediaPlayer.setOnCompletionListener(mp -> stopEzan());
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                stopEzan();
                return true;
            });
            mediaPlayer.setOnPreparedListener(mp -> {
                playing = true;
                mp.start();
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "Error playing Ezan audio", e);
            stopEzan();
        }
    }

    private Uri resolveEzanSoundUri(int soundIndex) {
        if (soundIndex == SOUND_SHORT_TONE) {
            Uri notifUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            if (notifUri != null) return notifUri;
        }
        int resId = R.raw.ezan;
        if (soundIndex == SOUND_ISTANBUL) {
            int custom = getResources().getIdentifier("ezan_istanbul", "raw", getPackageName());
            if (custom != 0) resId = custom;
        } else if (soundIndex == SOUND_MAKKAH) {
            int custom = getResources().getIdentifier("ezan_mekke", "raw", getPackageName());
            if (custom != 0) resId = custom;
        }
        return Uri.parse("android.resource://" + getPackageName() + "/" + resId);
    }
```

**Kodun Anlamı:**
- `ezan_istanbul.ogg` veya `ezan_mekke.ogg` dosyalarını `res/raw/` içine koyduğunuz anda `getIdentifier` ile otomatik çalar; henüz koyulmadıysa da çökmeden dahili `R.raw.ezan` sesine düşer!
- `SOUND_SHORT_TONE` seçildiğinde sistemin kısa bildirim zilini çalar, `SOUND_SILENT` seçildiğinde ses çalmadan servisi sonlandırır.

---

### Adım 6.4 — `AlarmHelper.java` Güncellemesi (Özel Erken Uyarı Dakikası & DND Zamanlayıcı)
**Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/util/AlarmHelper.java`

1. **27–36. satırlar arasındaki `TYPE_*` sabitlerini ve `TYPE_OFFSETS_MS` dizisini** aşağıdakiyle değiştir:
```java
    public static final int TYPE_EXACT = 0;
    public static final int TYPE_PRE_45 = 1; // Özelleştirilebilir erken uyarı (15/30/45/60 dk)
    public static final int TYPE_PRE_15 = 2;
    public static final int TYPE_DND_START = 3;
    public static final int TYPE_DND_END = 4;

    public static final String PREF_PRE_CUSTOM_MINUTES = "notif_pre_custom_minutes";
    private static final int MAX_ALARM_TYPES = 5;
```

2. **46–47. satırlardaki (`boolean pre45Enabled = ...`) hemen altına** ekle:
```java
        int customPreMinutes = prefs.getInt(PREF_PRE_CUSTOM_MINUTES, 45);
        boolean dndEnabled = prefs.getBoolean(DndHelper.PREF_DND_ENABLED, false) && DndHelper.hasPolicyAccess(context);
```

3. **94–120. satırlar arasındaki `for (int type = 0; ...)` döngüsünü** aşağıdakiyle değiştir:
```java
                long[] offsetsMs = {
                        0L,
                        -customPreMinutes * 60L * 1000L,
                        -15L * 60L * 1000L,
                        5L * 60L * 1000L,
                        25L * 60L * 1000L
                };

                for (int type = 0; type < offsetsMs.length; type++) {
                    if (type == TYPE_PRE_45 && !pre45Enabled) continue;
                    if (type == TYPE_PRE_15 && (!pre15Enabled || customPreMinutes == 15)) continue;
                    if ((type == TYPE_DND_START || type == TYPE_DND_END) && (!dndEnabled || "sunrise".equals(PRAYER_KEYS[i]))) {
                        continue;
                    }
                    long triggerMs = basePrayerTimeMs + offsetsMs[type];

                    // Bugünkü vakit geçtiyse yarının önbellek saatini kullan
                    if (triggerMs <= nowMs) {
                        triggerMs = tomorrowPrayerTimeMs + offsetsMs[type];
                    }

                    int requestCode = (i * 10) + type;

                    Intent intent = new Intent(context, NotificationReceiver.class);
                    intent.putExtra("prayer_key", PRAYER_KEYS[i]);
                    intent.putExtra("type_index", type);

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    scheduleSingleAlarm(alarmManager, triggerMs, pendingIntent);
                }
```

4. **170. satırdaki `cancelAllAlarms` içindeki `for (int type = 0; type < TYPE_OFFSETS_MS.length; type++)`** satırını şu şekilde güncelle:
```java
            for (int type = 0; type < MAX_ALARM_TYPES; type++) {
```

**Kodun Anlamı:**
- Erken uyarı artık `15 / 30 / 45 / 60 dk` olarak ayarlanabilir. Eğer kullanıcı `15 dk` seçerse `TYPE_PRE_15` iki kez bildirim atmasın diye `customPreMinutes == 15` kontrolüyle tek bildirim gönderilir.
- Güneş (`sunrise`) vakti namaz vakti olmadığı için DND tetiklenmez; diğer 5 vakitte vakitten `+5 dk` sonra DND açılır, `+25 dk` sonra kapatılır.

---

### Adım 6.5 — `NotificationReceiver.java` Güncellemesi
**Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/NotificationReceiver.java`

1. **18. satırın (`import com.korkutsoftware.hilalim.util.AlarmHelper;`) altına** ekle:
```java
import com.korkutsoftware.hilalim.util.DndHelper;
```

2. **37–43. satırlar arasındaki `if (prayerKey != null)` bloğunu** aşağıdakiyle değiştir:
```java
        if (prayerKey != null) {
            if (typeIndex == AlarmHelper.TYPE_DND_START) {
                DndHelper.enableDndForPrayer(context);
            } else if (typeIndex == AlarmHelper.TYPE_DND_END) {
                DndHelper.restoreDndAfterPrayer(context);
            } else {
                showNotification(context, prayerKey, typeIndex);
            }
            // Ertesi günün alarmlarını yenile ve widget/çubuğu güncelle
            AlarmHelper.rescheduleAlarmsFromCache(context);
            PrayerWidgetProvider.updateAllWidgets(context);
        }
```

3. **92–108. satırlar arasındaki `titleText` / `contentText` hesaplamasını** aşağıdakiyle değiştir:
```java
        SharedPreferences prefs = context.getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        int customMinutes = prefs.getInt(AlarmHelper.PREF_PRE_CUSTOM_MINUTES, 45);

        String titleText;
        String contentText;
        if (typeIndex == AlarmHelper.TYPE_PRE_45) {
            titleText = context.getString(R.string.notification_pre_custom_title, customMinutes);
            contentText = context.getString(R.string.notification_pre_45_content, prayerName);
        } else if (typeIndex == AlarmHelper.TYPE_PRE_15) {
            titleText = context.getString(R.string.notification_pre_15_title);
            contentText = context.getString(R.string.notification_pre_15_content, prayerName);
        } else {
            titleText = context.getString(R.string.notification_exact_title);
            contentText = context.getString(R.string.notification_exact_content, prayerName);
        }
```
*(Eğer aşağıdaki 124. satırda yeniden `SharedPreferences prefs = ...` tanımlanmışsa, yinelenen `SharedPreferences` kelimesini kaldırıp yukarıdaki `prefs` değişkenini kullan!)*

4. **125–126. satırlardaki `if (typeIndex == AlarmHelper.TYPE_EXACT && ezanEnabled && ...)` şartını** şu şekilde güncelle:
```java
        int soundIndex = prefs.getInt(EzanPlayerService.PREF_EZAN_SOUND_INDEX, EzanPlayerService.SOUND_CLASSIC);
        if (typeIndex == AlarmHelper.TYPE_EXACT && ezanEnabled
                && soundIndex != EzanPlayerService.SOUND_SILENT
                && !"sunrise".equals(prayerKey)) {
```

### Adım 6.6 — Beyza Doğrulama & Push
```bash
./gradlew.bat testDebugUnitTest assembleDebug
git add .
git commit -m "feat(etap2): add multi-maqam Adhan playback, custom lead time and DND scheduler"
git push origin main
```

---

## TUR 7 — YASİN: `NotificationSettingsActivity` UI (Makam Seçici, Süre Seçici & DND)

> **Meet Durumu:** Yasin `git pull --rebase origin main` yapar ve ekran paylaşır.

### Adım 7.1 — `activity_notification_settings.xml` Güncellemesi
**Dosya:** `app/src/main/res/layout/activity_notification_settings.xml`

1. **213. satırdaki `<!-- Pre 45 Toggle -->` yorumunun hemen üstüne** **"Ezan Makamı / Sesi"** seçim satırını ekle:
```xml
                    <!-- Ezan Sound / Maqam Picker -->
                    <LinearLayout
                        android:id="@+id/row_ezan_sound"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:background="?attr/selectableItemBackground"
                        android:clickable="true"
                        android:focusable="true"
                        android:gravity="center_vertical"
                        android:orientation="horizontal"
                        android:paddingVertical="8dp">

                        <ImageView
                            android:layout_width="24dp"
                            android:layout_height="24dp"
                            android:contentDescription="@null"
                            android:src="@drawable/ic_mosque"
                            app:tint="?attr/colorSecondary" />

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_marginHorizontal="14dp"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="@string/notif_ezan_sound_title"
                                android:textColor="?attr/colorOnSurface"
                                android:textSize="15sp"
                                android:textStyle="bold" />

                            <TextView
                                android:id="@+id/txt_ezan_sound_value"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="@string/ezan_sound_classic"
                                android:textColor="?attr/colorSecondary"
                                android:textSize="12sp" />
                        </LinearLayout>

                        <ImageView
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:contentDescription="@null"
                            android:src="@drawable/ic_forward_arrow"
                            app:tint="?attr/colorOnSurfaceVariant" />
                    </LinearLayout>

                    <com.google.android.material.divider.MaterialDivider
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginVertical="6dp" />
```

2. **Mevcut `switch_pre_45` satırındaki `<TextView` (226–234. satırlar) etiketine `android:id="@+id/txt_pre_custom_label"`** ekle ve hemen altına **Erken Uyarı Süresi Seçici + DND Anahtarını** ekle:
- `switch_pre_45`'in içindeki `TextView`'a şunu ekle:
```xml
                        <TextView
                            android:id="@+id/txt_pre_custom_label"
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_marginHorizontal="14dp"
                            android:layout_weight="1"
                            android:text="@string/notif_pre_45_toggle"
                            android:textColor="?attr/colorOnSurface"
                            android:textSize="15sp" />
```
- **241. satırdaki `MaterialDivider` hemen altına** şu iki bloğu ekle:
```xml
                    <!-- Erken Uyarı Süresi Seçici (15 / 30 / 45 / 60 dk) -->
                    <LinearLayout
                        android:id="@+id/row_pre_minutes"
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:background="?attr/selectableItemBackground"
                        android:clickable="true"
                        android:focusable="true"
                        android:gravity="center_vertical"
                        android:orientation="horizontal"
                        android:paddingVertical="8dp">

                        <ImageView
                            android:layout_width="24dp"
                            android:layout_height="24dp"
                            android:contentDescription="@null"
                            android:src="@drawable/ic_alarm"
                            app:tint="?attr/colorSecondary" />

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_marginHorizontal="14dp"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="@string/notif_pre_minutes_title"
                                android:textColor="?attr/colorOnSurface"
                                android:textSize="15sp"
                                android:textStyle="bold" />

                            <TextView
                                android:id="@+id/txt_pre_minutes_value"
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:textColor="?attr/colorSecondary"
                                android:textSize="12sp" />
                        </LinearLayout>

                        <ImageView
                            android:layout_width="20dp"
                            android:layout_height="20dp"
                            android:contentDescription="@null"
                            android:src="@drawable/ic_forward_arrow"
                            app:tint="?attr/colorOnSurfaceVariant" />
                    </LinearLayout>

                    <com.google.android.material.divider.MaterialDivider
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginVertical="6dp" />

                    <!-- Namazda Sessize Al (DND) -->
                    <LinearLayout
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:gravity="center_vertical"
                        android:orientation="horizontal"
                        android:paddingVertical="6dp">

                        <ImageView
                            android:layout_width="24dp"
                            android:layout_height="24dp"
                            android:contentDescription="@null"
                            android:src="@drawable/ic_dnd"
                            app:tint="?attr/colorSecondary" />

                        <LinearLayout
                            android:layout_width="0dp"
                            android:layout_height="wrap_content"
                            android:layout_marginHorizontal="14dp"
                            android:layout_weight="1"
                            android:orientation="vertical">

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="@string/notif_dnd_title"
                                android:textColor="?attr/colorOnSurface"
                                android:textSize="15sp"
                                android:textStyle="bold" />

                            <TextView
                                android:layout_width="wrap_content"
                                android:layout_height="wrap_content"
                                android:text="@string/notif_dnd_desc"
                                android:textColor="?attr/colorOnSurfaceVariant"
                                android:textSize="12sp" />
                        </LinearLayout>

                        <com.google.android.material.materialswitch.MaterialSwitch
                            android:id="@+id/switch_dnd"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content" />
                    </LinearLayout>

                    <com.google.android.material.divider.MaterialDivider
                        android:layout_width="match_parent"
                        android:layout_height="wrap_content"
                        android:layout_marginVertical="6dp" />
```

---

### Adım 7.2 — `NotificationSettingsActivity.java` Güncellemesi
**Dosya:** `app/src/main/java/com/korkutsoftware/hilalim/NotificationSettingsActivity.java`

1. **Import bloğuna (12. satır `import com.korkutsoftware.hilalim.util.OngoingPrayerNotificationHelper;` altına)** ekle:
```java
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.korkutsoftware.hilalim.util.DndHelper;
```

2. **18. satırın (`private MaterialButton btnTestEzan;`) altına** ekle:
```java
    private TextView txtEzanSoundValue, txtPreMinutesValue, txtPreCustomLabel;
    private MaterialSwitch switchDnd;
```

3. **44. satırın (`bindSwitch(R.id.switch_isha, ...)` altına, `btnTestEzan = ...` üstüne)** ekle:
```java
        txtEzanSoundValue = findViewById(R.id.txt_ezan_sound_value);
        txtPreMinutesValue = findViewById(R.id.txt_pre_minutes_value);
        txtPreCustomLabel = findViewById(R.id.txt_pre_custom_label);
        switchDnd = findViewById(R.id.switch_dnd);

        View rowEzanSound = findViewById(R.id.row_ezan_sound);
        if (rowEzanSound != null) {
            rowEzanSound.setOnClickListener(v -> showEzanSoundDialog());
        }
        View rowPreMinutes = findViewById(R.id.row_pre_minutes);
        if (rowPreMinutes != null) {
            rowPreMinutes.setOnClickListener(v -> showPreMinutesDialog());
        }

        setupDndSwitch();
        refreshSummaryTexts();
```

4. **65. satırdaki `onResume()` içine** `refreshSummaryTexts();` ve DND izin senkronizasyonu ekle:
```java
    @Override
    protected void onResume() {
        super.onResume();
        updateTestButtonUI();
        refreshSummaryTexts();
        if (switchDnd != null) {
            boolean enabled = prefs.getBoolean(DndHelper.PREF_DND_ENABLED, false) && DndHelper.hasPolicyAccess(this);
            switchDnd.setChecked(enabled);
        }
    }
```

5. **Dosyanın en sonundaki `}` (89. satır) öncesine** yardımcı dialog metodlarını ekle:
```java
    private void showEzanSoundDialog() {
        String[] options = {
                getString(R.string.ezan_sound_classic),
                getString(R.string.ezan_sound_istanbul),
                getString(R.string.ezan_sound_makkah),
                getString(R.string.ezan_sound_short),
                getString(R.string.ezan_sound_silent)
        };
        int current = prefs.getInt(EzanPlayerService.PREF_EZAN_SOUND_INDEX, EzanPlayerService.SOUND_CLASSIC);
        new AlertDialog.Builder(this)
                .setTitle(R.string.notif_ezan_sound_title)
                .setSingleChoiceItems(options, current, (dialog, which) -> {
                    prefs.edit().putInt(EzanPlayerService.PREF_EZAN_SOUND_INDEX, which).apply();
                    refreshSummaryTexts();
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showPreMinutesDialog() {
        final int[] values = {15, 30, 45, 60};
        String[] labels = new String[values.length];
        int currentMin = prefs.getInt(AlarmHelper.PREF_PRE_CUSTOM_MINUTES, 45);
        int checkedIdx = 2;
        for (int i = 0; i < values.length; i++) {
            labels[i] = getString(R.string.notif_pre_minutes_value, values[i]);
            if (values[i] == currentMin) checkedIdx = i;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.notif_pre_minutes_title)
                .setSingleChoiceItems(labels, checkedIdx, (dialog, which) -> {
                    prefs.edit().putInt(AlarmHelper.PREF_PRE_CUSTOM_MINUTES, values[which]).apply();
                    refreshSummaryTexts();
                    AlarmHelper.rescheduleAlarmsFromCache(this);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void setupDndSwitch() {
        if (switchDnd == null) return;
        boolean current = prefs.getBoolean(DndHelper.PREF_DND_ENABLED, false) && DndHelper.hasPolicyAccess(this);
        switchDnd.setChecked(current);
        switchDnd.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!buttonView.isPressed()) return;
            if (isChecked && !DndHelper.hasPolicyAccess(this)) {
                buttonView.setChecked(false);
                new AlertDialog.Builder(this)
                        .setTitle(R.string.notif_dnd_permission_title)
                        .setMessage(R.string.notif_dnd_permission_msg)
                        .setPositiveButton(R.string.notif_dnd_go_settings, (d, w) -> DndHelper.openPolicySettings(this))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
                return;
            }
            prefs.edit().putBoolean(DndHelper.PREF_DND_ENABLED, isChecked).apply();
            AlarmHelper.rescheduleAlarmsFromCache(this);
        });
    }

    private void refreshSummaryTexts() {
        if (txtEzanSoundValue != null) {
            String[] options = {
                    getString(R.string.ezan_sound_classic),
                    getString(R.string.ezan_sound_istanbul),
                    getString(R.string.ezan_sound_makkah),
                    getString(R.string.ezan_sound_short),
                    getString(R.string.ezan_sound_silent)
            };
            int idx = prefs.getInt(EzanPlayerService.PREF_EZAN_SOUND_INDEX, EzanPlayerService.SOUND_CLASSIC);
            if (idx < 0 || idx >= options.length) idx = 0;
            txtEzanSoundValue.setText(options[idx]);
        }
        int mins = prefs.getInt(AlarmHelper.PREF_PRE_CUSTOM_MINUTES, 45);
        if (txtPreMinutesValue != null) {
            txtPreMinutesValue.setText(getString(R.string.notif_pre_minutes_value, mins));
        }
        if (txtPreCustomLabel != null) {
            txtPreCustomLabel.setText(getString(R.string.notif_pre_minutes_dynamic, mins));
        }
    }
```

**Kodun Anlamı:**
- `if (!buttonView.isPressed()) return;` kontrolü, `onResume` içinde programatik olarak `setChecked(...)` çağrıldığında gereksiz yere izin penceresinin açılmasını önler.
- Kullanıcı izin ekranından geri döndüğünde `onResume` izin durumunu tazeler.

### Adım 7.3 — Yasin Doğrulama & Push
```bash
./gradlew.bat testDebugUnitTest assembleDebug
git add .
git commit -m "feat(etap2): add Adhan sound picker, custom lead time and DND toggle UI"
git push origin main
```

---

## TUR 8 — İKİNİZ BİRLİKTE: Canlı Meet Test & Kabul Listesi

Beyza son kez `git pull --rebase origin main` alır ve ikiniz gerçek cihaz / emülatörde şu 6 senaryoyu test edersiniz:

1. **Ayet Arama (FTS4):** Kur'an sekmesinde `Ayet Ara` chip'ine bas, `"sabır"` ve `"namaz"` yaz → 1 saniye altında sonuç gelmeli, tıklayınca `SurahDetailActivity` tam o ayete kaymalı.
2. **30 Cüz Navigasyonu:** `Cüzler` chip'ine bas → `14. Cüz` (`Hicr 1`) ve `30. Cüz` (`Nebe 1`) doğru sureye açılmalı.
3. **Sesli Tilavet & Mini Player:** Herhangi bir ayette Play ikonuna bas → altın kenarlık (`2dp`) yanmalı, Mini Player'da süre ilerlemeli, `3x` seçip bitince aynı ayet 3 kez tekrar edip sonraki ayete geçmeli, ekranı kapatınca bildirim çubuğundan Duraklat/Sonraki çalışmalı.
4. **Offline Dayanıklılık:** Uçak modunu açıp ayete Play bas → uygulama çökmeden `İnternet bağlantınızı kontrol edin` mesajı göstermeli.
5. **Ezan Makamı & DND:** `Bildirim & Ezan Yönetimi` ekranında `Kısa Tekbir` seçip `Ezanı Test Et`'e bas; `Erken Uyarı Süresi`'ni `60 dk` yap; `DND` anahtarına basınca Android izin ekranı açılmalı.
6. **4 Dil + Karanlık Mod:** `tr`, `en`, `de`, `ar` (özellikle Arapça RTL hizalaması) ve Dark Mode altında `fragment_quran`, `activity_surah_detail` ve `activity_notification_settings` görsel olarak kontrol edilir.

> **Not (İsteğe Bağlı Ek Ses Dosyası):** Eğer `İstanbul Makamı` ve `Mekke / Medine Ezanı` için farklı dahili sesler koymak isterseniz `app/src/main/res/raw/` içine `ezan_istanbul.ogg` ve `ezan_mekke.ogg` dosyalarını eklemeniz yeterlidir; `EzanPlayerService` kodunda başka hiçbir değişiklik yapmadan otomatik tanıyacaktır.
