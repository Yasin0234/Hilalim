# Hilalim Ezan Vakti - Gizlilik Politikası

**Son Güncelleme Tarihi:** 23 Şubat 2026

**Korkut Yazılım A.Ş.** ("Şirket", "Biz") olarak, **Hilalim Ezan Vakti** ("Uygulama") kullanıcılarımızın ("Kullanıcı", "Siz") gizliliğine ve kişisel verilerinin korunmasına büyük önem veriyoruz. İşbu Gizlilik Politikası, Uygulamamızı kullanırken toplanan, işlenen ve saklanan veriler hakkında sizi bilgilendirmek amacıyla hazırlanmıştır.

---

## 1. Toplanan Veriler ve Kullanım Amaçları

Hilalim Ezan Vakti, temel işlevlerini yerine getirebilmek için sınırlı miktarda veri kullanır.

### a. Konum Verisi (Location Data)
- **Toplanan Veri:** Hassas Konum (`ACCESS_FINE_LOCATION`) ve Yaklaşık Konum (`ACCESS_COARSE_LOCATION`).
- **Kullanım Amacı:** Bulunduğunuz konuma en uygun ve doğru ezan vakitlerini hesaplamak, Kıble yönünü hassas şekilde belirlemek ve konumunuza özel vakit çizelgesini sunmak.
- **Saklama ve Paylaşım:** Konum verileriniz yalnızca anlık hesaplamalar için cihazınızda işlenir; sunucularımızda saklanmaz, profil çıkarma veya reklam takibi amacıyla kullanılmaz ve üçüncü taraflarla paylaşılmaz.

### b. Cihaz İçi Yerel Depolama Verileri (Local Data)
- **Toplanan Veri:** Zikirmatik sayıları, seçilen varsayılan şehir/konum, bildirim tercihleri ve uygulama içi ayarlar.
- **Kullanım Amacı:** Uygulamayı her açtığınızda tercihlerinizi hatırlamak ve zikir verilerinizi kaybetmemenizi sağlamak.
- **Saklama ve Paylaşım:** Bu veriler tamamen cihazınızın yerel hafızasında (SharedPreferences / Yerel Veritabanı) saklanır. Şirketimiz veya uzak sunucular ile paylaşılmaz.

### c. İletişim / Sunucu İstek Verileri
- **Toplanan Veri:** Namaz vakitlerini güncellerken kullanılan standart IP adresi ve teknik cihaz istek bilgileri.
- **Kullanım Amacı:** Güvenli ağ iletişimi sağlamak ve güncel ezan vakti verilerini API sunucularımızdan çekmek (`INTERNET` izni).

---

## 2. Uygulama Tarafından İstenen İzinler ve Nedenleri

| İzin Adı | Amaç |
| :--- | :--- |
| **`ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`** | Bulunduğunuz yerin ezan vakitlerini ve Kıble açısını doğru hesaplamak için kullanılır. |
| **`POST_NOTIFICATIONS`** | Ezan vakitlerinde bildirim ve hatırlatıcı gönderebilmek için kullanılır. |
| **`SCHEDULE_EXACT_ALARM`** | Ezan vakitlerinde tam zamanında alarm ve sesli uyarı çalabilmek için gereklidir. |
| **`RECEIVE_BOOT_COMPLETED`** | Cihazınız yeniden başlatıldığında ezan vakti alarmlarının aksamadan kurulmasını sağlar. |
| **`VIBRATE`** | Zikirmatik kullanımı ve bildirim esnasında dokunsal geri bildirim (titreşim) sağlamak için kullanılır. |
| **`INTERNET`** | Güncel namaz vakitlerini ve dini gün verilerini sunucudan indirmek için kullanılır. |

---

## 3. Üçüncü Taraf Servisleri ve Paylaşım

Hilalim Ezan Vakti, kişisel verilerinizi hiçbir koşulda satmaz, kiralamaz veya pazarlama amacıyla üçüncü taraf şirketlerle paylaşmaz.

Uygulama içinde yalnızca aşağıdaki standart altyapı hizmetleri kullanılabilir:
- **Google Play Hizmetleri (Google Play Services / Location API):** Cihazınızın konumunu güvenli ve hızlı bir şekilde tespit etmek amacıyla Google servisleri kullanılır. Google'ın privacy politikasına [Google Privacy Policy](https://policies.google.com/privacy) adresinden ulaşabilirsiniz.

---

## 4. Veri Güvenliği

Toplanan veya yerel olarak saklanan verilerin güvenliği bizim için esastır. Cihazınızda saklanan veriler Android işletim sisteminin sunduğu güvenli depolama alanlarında muhafaza edilir. Sunucu ile iletişimde SSL/TLS şifreleme protokolleri kullanılır.

---

## 5. Çocukların Gizliliği (Children's Privacy)

Uygulamamız her yaştan kullanıcıya (çocuklar dahil) uygundur. 13 yaşın altındaki çocuklardan bilerek ve isteyerek herhangi bir kişisel veri toplamıyoruz.

---

## 6. KVKK ve GDPR Kapsamındaki Haklarınız

6698 sayılı Kişisel Verilerin Korunması Kanunu (KVKK) ve Avrupa Genel Veri Koruma Tüzüğü (GDPR) kapsamında;
- Hakkınızda işlenen bir kişisel veri bulunup bulunmadığını öğrenme,
- Cihazınızdaki verileri dilediğiniz zaman uygulama verilerini temizleyerek veya uygulamayı kaldırarak silme,
- İzin verdiğiniz konum ve bildirim izinlerini cihazınızın **Ayarlar > Uygulamalar > Hilalim** bölümünden dilediğiniz zaman iptal etme hakkına sahipsiniz.

---

## 7. Gizlilik Politikasındaki Değişiklikler

İşbu Gizlilik Politikası zaman zaman güncellenebilir. Politikada yapılan güncellemeler bu sayfada veya uygulama içerisindeki duyurular aracılığıyla yayınlanacaktır.

---

## 8. İletişim

Gizlilik Politikamız veya kişisel verilerinizle ilgili her türlü soru, görüş ve talepleriniz için bizimle iletişime geçebilirsiniz:

- **Şirket Unvanı:** Korkut Yazılım A.Ş. / Demiral Medya
- **Uygulama Adı:** Hilalim Ezan Vakti
- **E-posta:** info@korkutsoftware.com
- **Web Sitesi:** [https://www.korkutsoftware.com](https://www.korkutsoftware.com)
