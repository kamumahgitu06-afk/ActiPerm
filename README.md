# ActiPerm - Cek Izin & Bersihkan HP

**Dibuat oleh Yaudah Iya**

[![Platform](https://img.shields.io/badge/Android-7.0%2B%20(API%2024%E2%80%9336)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Tech](https://img.shields.io/badge/Kotlin-Jetpack%20Compose%20M3-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Privacy](https://img.shields.io/badge/Privasi-100%25%20Lokal%20Tanpa%20Server-00C853)](#-mengapa-query_all_packages-di-actiperm-aman)

**ActiPerm** adalah aplikasi **audit izin berbahaya, deteksi risiko privasi, dan pengelola izin aplikasi (Package Manager Pro)** untuk Android dengan tema gelap bergaya antivirus dan antarmuka sepenuhnya berbahasa Indonesia.

**100% Lokal & Privat:** Seluruh pemindaian aplikasi, analisis risiko, ekspor laporan, hingga pencabutan/pemberian izin berjalan **sepenuhnya di HP Anda tanpa mengirim data apa pun ke server eksternal**.

> Suka dengan aplikasi ini dan ingin mendukung pengembangan fitur gratis lainnya?
>
> **[Sawer Gue Disini!](https://saweria.co/Yaudahiya9)**

---

## Fitur Utama ActiPerm

### 1. Pemindaian Aplikasi & Dasbor Keamanan (`QUERY_ALL_PACKAGES`)
- Memindai seluruh aplikasi terinstal langsung melalui sistem Android (`PackageManager`) lengkap dengan ikon asli, nama aplikasi, nama paket, versi, ukuran APK/data, Target SDK, sumber installer, dan waktu terakhir dipakai.
- **Dasbor Keamanan** menampilkan total aplikasi (Unduhan vs Bawaan), jumlah izin berbahaya tak wajar, grafik batang distribusi risiko, dan tombol **Pindai Ulang**.

### 2. Skor Risiko 0–100 & Whitelist Cerdas Berbasis Fungsi
Setiap aplikasi dianalisis dan diberi skor risiko **0–100**:
- **Aman (Hijau):** Izin normal atau wajar sesuai fungsi utama aplikasi.
- **Mencurigakan (Kuning):** Meminta izin di luar fungsi inti atau izin tambahan yang perlu diawasi.
- **Berbahaya (Merah):** Meminta/mengizinkan kombinasi izin sensitif yang tidak wajar atau berpola penyalahgunaan.

Aturan klasifikasi cerdas agar minim *false positive*:
- **Whitelist Aplikasi Wajar (Hijau):**
  - *Chatting & Sosial Media* (contoh: **WhatsApp**, Telegram, Instagram, Facebook): Kamera, Mikrofon, Kontak, Lokasi, Penyimpanan berlabel **"Izin Wajar Untuk Fungsinya"**.
  - *E-Commerce* (contoh: **Shopee**, Tokopedia, Lazada): Lokasi, Kamera, Mikrofon, Media dianggap wajar untuk alamat pengiriman, foto/video ulasan, dan live.
  - *Dompet Digital & M-Banking Resmi* (contoh: **DANA**, OVO, GoPay, bank resmi dengan validasi signature rilis): Kamera QRIS, Lokasi verifikasi, dan Kontak transfer dianggap wajar.
- **Deteksi Ketat Aplikasi Berisiko Tinggi (Merah):**
  - **Pinjol / Keuangan Berisiko:** Kombinasi izin Kontak + SMS + Lokasi + Log Panggilan / Accessibility langsung ditandai **Berbahaya (Merah)**.
  - **Caller ID Pengumpul Kontak** (contoh: **Getcontact**, Truecaller, Sync.ME): Tetap ditandai **Merah / Berbahaya** karena pengumpulan & unggah daftar kontak.
  - **Operator Seluler** (myIM3, AXISNET, MyTelkomsel, bima+): Hanya Nomor/Status Telepon yang wajar; Lokasi & Kontak ditandai tidak wajar.
  - **Utilitas Sederhana** (Kalkulator, Senter, Wallpaper): Jika meminta Lokasi/Mic/Kontak/SMS langsung dianggap **Berbahaya**.

### 3. Kategori `Sistem - Wajar` Lintas Semua Merk HP
Otomatis mendeteksi merk perangkat (`Build.MANUFACTURER` / `Build.BRAND`), partisi sistem (`/system`, `/system_ext`, `/product`, `/vendor`, `/odm`, `/apex`, `/oem`), dan *updated system apps* untuk semua ekosistem HP:
- **Samsung**, **Xiaomi / Redmi / POCO**, **Oppo**, **Vivo / iQOO**, **Realme**, **Infinix / Tecno / Itel (Transsion)**, **Asus**, **Huawei / Honor**, serta **Google Pixel / AOSP**.
- Layanan inti seperti UI Sistem, Pengaturan, Shell, Layanan Google Play, hingga aplikasi bawaan seperti *Perekam Suara*, *Samsung Notes*, dan *Jam* diklasifikasikan akurat ke **Aplikasi Bawaan / `Sistem - Wajar` (Skor 0, Hijau)** dan dikeluarkan dari hitungan berbahaya.

### 4. Kompatibel Penuh Android 7 hingga Android 15+
- Tetap mendukung **`minSdk = 24` (Android 7.0 Nougat)** hingga **`targetSdk = 36`** (Android 15/16).
- Penyesuaian aturan izin per versi OS:
  - **Android 7–10:** Izin yang belum diizinkan runtime tidak dihitung sebagai ancaman aktif; pemindaian menggunakan fallback aman per paket pada API 24–25 untuk mencegah `TransactionTooLargeException`.
  - **Android 11+ (Scoped Storage):** Izin penyimpanan untuk File Manager / Galeri ditangani secara proporsional.
  - **Android 12+:** `BLUETOOTH_SCAN` / `NEARBY_DEVICES` wajar untuk aplikasi IoT/TWS.
  - **Android 13+:** `POST_NOTIFICATIONS` & `READ_MEDIA_*` dinilai wajar.
  - **Android 14+:** `SCHEDULE_EXACT_ALARM` & `USE_FULL_SCREEN_INTENT` wajar untuk Alarm/Jam/Kalender.
  - **Android 15+:** Izin yang terkena *Auto-Revoke* pada aplikasi menganggur tidak dihitung sebagai bahaya.

### 5. Manajemen Izin PRO (Package Manager Pro)
- **Kontrol Izin Per Aplikasi (`[Cabut]` / `[Berikan]`):** Kelola izin runtime berbahaya langsung dari halaman detail aplikasi dengan badge **`Bisa Diatur Langsung`** / **`Butuh Pengaturan Manual`**.
- **3 Mode Eksekusi Otomatis:**
  1. **Mode A — Shizuku (Utama):** Eksekusi langsung `pm grant` & `pm revoke` tanpa root, dilengkapi halaman pengecekan status **Shizuku**.
  2. **Mode B — ADB / Tanpa Root:** Panduan aktivasi 1x dan salinan perintah `adb shell pm grant/revoke`.
  3. **Mode C — Fallback Pengaturan Sistem:** Jika Shizuku maupun Root tidak aktif, tombol `[Cabut]`/`[Berikan]` langsung membuka **Pengaturan Info Aplikasi Android** agar tidak pernah error.
- **Aksi Massal (Batch Action):** Tombol *Cabut Semua Izin Lokasi Berbahaya* & *Cabut Semua Izin Kamera Aplikasi Mencurigakan* di Dasbor dengan dialog konfirmasi daftar target.
- **Tab Riwayat Persisten:** Mencatat seluruh riwayat pencabutan/pemberian izin lengkap dengan waktu dan mode eksekusi.
- **Proteksi Sistem Kritis (`Aplikasi Sistem - Dilindungi`):** Mengunci paket vital OS (`android`, `com.android.systemui`, `com.android.settings`, `com.google.android.gms`, dll.) di detail aplikasi dan melewatinya pada aksi massal untuk mencegah *bootloop*.

### 6. Filter Lengkap, Export `.TXT`, & Laporkan Salah Deteksi
- **Filter Merk HP & Sensor:** Filter daftar aplikasi berdasarkan merk OEM, sensor inti (**Lokasi, Kamera, Mic**), tab **Kelola Izin**, serta filter aplikasi bawaan yang menganggur (beserta antrean penonaktifan massal).
- **Export Laporan Lengkap (`.txt`):** Simpan seluruh hasil audit aplikasi dan daftar izinnya menjadi file teks `.txt` yang mudah dibagikan atau diarsipkan.
- **Laporkan Salah Deteksi:** Laporkan jika ada aplikasi yang salah klasifikasi langsung dari halaman detail aplikasi.

---

## Cara Install dari GitHub Releases

1. Buka halaman **Releases** di repositori ini: **[ActiPerm Releases](https://github.com/kamumahgitu06-afk/ActiPerm/releases)**.
2. Pada rilis terbaru (**v1.0**), unduh file **`app-debug.apk`**.
3. Buka file APK tersebut di HP Android Anda (minimal Android 7.0 Nougat).
4. Jika muncul peringatan keamanan Android, aktifkan **Izinkan dari sumber ini / Install unknown apps**, lalu tekan **Install**.
5. Setelah terpasang, buka **ActiPerm** — pemindaian aplikasi berjalan otomatis.
6. *(Opsional)* Untuk melihat kapan aplikasi terakhir kali dipakai & fitur aplikasi bawaan menganggur, aktifkan **Izin Akses Penggunaan (Usage Access)** melalui banner di Dasbor.

---

## Mengapa `QUERY_ALL_PACKAGES` di ActiPerm Aman?

ActiPerm memerlukan izin `QUERY_ALL_PACKAGES` karena fungsi utamanya adalah **auditor keamanan & pengelola izin untuk seluruh aplikasi terinstal di perangkat Anda** — sama seperti manajer paket atau antivirus lokal.

- **Tanpa Koneksi Server Pribadi / Tanpa Izin `INTERNET` Pengintai:** ActiPerm memproses seluruh data paket, manifest izin, dan riwayat secara **offline 100% di HP Anda**.
- **Tidak Ada Pengumpulan Data, Analitik, maupun Iklan:** Daftar aplikasi Anda tidak pernah diunggah ke mana pun.
- **Ekspor Dikendalikan Pengguna:** File laporan `.txt` hanya dibuat ketika Anda sendiri menekan tombol Ekspor.

---

## Cara Tes Fitur Shizuku di HP Android

Untuk menggunakan **Mode A (Cabut/Berikan Izin Langsung Tanpa Root)**:

1. Instal aplikasi **Shizuku** resmi (`moe.shizuku.privileged.api`) dari Play Store atau GitHub RikkaApps.
2. Jalankan layanan Shizuku di HP Anda:
   - **Android 11 ke atas (Tanpa PC):**
     1. Aktifkan *Opsi Pengembang (Developer Options)* → nyalakan **Wireless Debugging (Proses debug nirkabel)**.
     2. Buka aplikasi **Shizuku** → pilih **Pairing** → masukkan kode pairing → tekan **Start**.
   - **Android 7 – 10 (Via ADB Kabel dari PC, 1x setelah reboot):**
     1. Aktifkan *USB Debugging* dan sambungkan HP ke PC.
     2. Jalankan perintah berikut di terminal PC:
        ```bash
        adb shell sh /storage/emulated/0/Android/data/moe.shizuku.privileged.api/start.sh
        ```
3. Buka **ActiPerm**, pilih tab **Shizuku** di pojok kanan bawah, lalu tekan **Cek Shizuku**.
4. Tekan **Minta Izin Shizuku** dan pilih **Izinkan sepanjang waktu** saat pop-up Shizuku muncul.
5. Status berubah menjadi **Shizuku Aktif**. Anda kini dapat menekan tombol **`[Cabut]`** / **`[Berikan]`** langsung di detail aplikasi atau menjalankan **Aksi Massal** di Dasbor.

---

## Tautan & Karya Lainnya

- **GitHub Creator:** [https://github.com/kamumahgitu06-afk](https://github.com/kamumahgitu06-afk)
- **ActiScan:** [https://github.com/kamumahgitu06-afk/ActiScan](https://github.com/kamumahgitu06-afk/ActiScan)
- **Dukung Kreator:** [Sawer Gue Disini!](https://saweria.co/Yaudahiya9)

---

*Dibuat oleh Yaudah Iya*
