# 📱 E-Ticaret Fırsat Takip Ajanı (Android Java)

**Migros (`m`)**, **Çağrı Market (`ç`)** ve **CarrefourSA (`c`)** indirimlerini doğrudan Android telefonunuzda tarayan, aynı marka ve indirim oranındaki ürünleri gruplayan ve tarihiyle birlikte **Word (`.docx`)** + **PDF (`.pdf`)** raporu üreten Android Java uygulaması.

---

## 📦 Telefona Nasıl Kurulur? (APK Oluşturma ve Yükleme)

### Yöntem 1: Bilgisayara Hiçbir Program Kurmadan (GitHub ile Bulutta APK Üretme)
1. Ücretsiz bir **GitHub** hesabı açıp yeni bir repo (depo) oluşturun.
2. Bu klasördeki (`.github` klasörü dahil) tüm dosyaları o GitHub reposuna yükleyin.
3. GitHub sayfasında üstten **Actions** sekmesine tıklayın; **Build Android APK** işlemi otomatik başlayacaktır.
4. İşlem bitince (yaklaşık 2 dakika) sayfanın en altındaki **Artifacts > `FirsatTakip-APK`** dosyasına tıklayıp zip içindeki **`app-debug.apk`** dosyasını doğrudan telefonunuza indirin.

### Yöntem 2: Bilgisayarda Android Studio ile APK Üretme
1. Bilgisayarınıza ücretsiz **Android Studio** kurun.
2. Android Studio'da **Open** diyerek `firsat_takip_android` klasörünü seçin ve Gradle senkronizasyonunun bitmesini bekleyin.
3. Üst menüden **Build > Build App Bundle(s) / APK(s) > Build APK(s)** seçeneğine tıklayın.
4. Oluşan `app/build/outputs/apk/debug/app-debug.apk` dosyasını USB, WhatsApp, Telegram veya Google Drive ile telefonunuza gönderin.

---

### 📲 APK'yı Android Telefona Yükleme Adımları
1. Telefonunuzda indirdiğiniz **`app-debug.apk`** dosyasına dokunun.
2. İlk kez APK yüklüyorsanız Android güvenlik uyarısı gösterecektir: **Ayarlar > Bu kaynaktan izin ver** seçeneğini açın.
3. **Yükle** butonuna dokunun.
4. Uygulama açıldığında **`m`**, **`ç`**, **`c`** veya **`t`** harfini girin ya da ilgili butona dokunun; tarama bitince **PDF Aç**, **Word Aç** veya **Paylaş** butonlarını kullanın.
