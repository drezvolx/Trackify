# Changelog

Catatan perubahan Trackify. Urutan terbaru di atas.

---

## 24 Nov 2026 — Perbaikan: kamera berhenti saat ambil foto Lokasi

### Diubah

- `LocationPhotoStore.createGalleryImageUri()` tidak lagi menandai baris MediaStore dengan `IS_PENDING = 1`.
  Penanda itu mengunci tulis hanya untuk aplikasi, sehingga kamera vendor crash saat menekan **done** dan sistem
  menampilkan "Kamera telah berhenti"/"paksa berhenti". Foto tetap masuk galeri lewat `MediaStore`, lalu salinan
  stabil disimpan di `filesDir/location_photos` sesuai kontrak.
- `finalizeGalleryImage()` dan pemanggilannya di `LocationsScreen` serta `NotesScreen` dihapus karena tidak lagi
  diperlukan. Kontrak di `docs/LOCKED_DECISIONS.md §4.3` diamandemen untuk melarang `IS_PENDING` pada URI kamera.

### Verifikasi

- Reproduksi awal: Lokasi Baru → **Ambil foto** → shutter → **done** memunculkan dialog "Kamera telah berhenti".
- Setelah perbaikan: alur yang sama kembali ke editor dengan tombol **Hapus foto**, tanpa dialog crash; baris galeri
  `Pictures/Trackify/Trackify_*.jpg` terisi dengan ukuran > 0.
- Smoke Lokasi: foto 0→1, preview di editor, **Simpan lokasi** berhasil, detail menampilkan **Foto lokasi**,
  lalu **Edit** → **Hapus foto** → **Simpan perubahan** menghilangkan foto dari detail.
- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan **36/36** tes, 0 failure.
- Grep kontrak: `COD_TOKEN=0`, `OLD_IDENTIFIERS=0`, `ALERT_DIRECT=0`, `SUBTEXT=0`, `THEME_MANUAL=0`,
  `DIRECT_BUTTONS=0`, `ALPHA_BORDER=0`, `SHADOW=0`, `OFF_SCALE_SPACING=0`; `Scaffold(` hanya di `MainActivity.kt`.
- CI `validate` hijau: run `45406085894` (`2a067e6`).
- Perubahan ini **tidak** menaikkan versi dan tidak membuat tag/rilis baru. Tag dan rilis `v1.0.0`/`v1.1.0` utuh.

---

## 24 Nov 2026 — Rilis v1.1.0

### Diubah

- `app/build.gradle.kts`: `versionCode = 2`, `versionName = "1.1"` (sebelumnya `1` / `"1.0"`).
- Rilis GitHub `v1.1.0` dibuat dari commit versi ini. Tag dan rilis `v1.0.0` tetap dipertahankan sebagai baseline.

### Verifikasi

- Gate ulang setelah bump: ktlint exit 0, `:app:assembleDebug :app:testDebugUnitTest` hijau, **36/36** tes.
- Smoke cepat: Home menampilkan `Versi terpasang: 1.1` saat bel ditekan, dan Setelan → Dukungan menampilkan
  `Versi aplikasi: 1.1`.
- CI `validate` hijau untuk commit bump: run `45259931729` (`de57390`).
- Tag `v1.1.0` dan GitHub Release **Trackify v1.1.0** diterbitkan
  (<https://github.com/drezvolx/Trackify/releases/tag/v1.1.0>, release id `455966363`). `GET releases/latest`
  mengembalikan `v1.1.0`. Tag dan rilis `v1.0.0` tidak diubah (`a81d6ca`) dan tetap menjadi baseline.

---

## 24 Nov 2026 — Fase 18: pengecek rilis & kredit

### Ditambahkan

- Bel `TrackifyIcons.Bell` di header Home membuka `TrackifyDialog` **Pembaruan Trackify**. Isinya versi terpasang
  (dibaca dari `PackageManager`), versi rilis, dan aksi **Buka GitHub**; **Unduh APK** muncul hanya bila aset `.apk`
  tersedia. **Tutup** selalu tersedia. Tidak ada unduh/pasang otomatis.
- `app/src/main/java/com/blankpages/trackify/update/`: `ReleaseVersion` (parsing numerik `v1.0.0`/`1.1`/`-beta`),
  `ReleaseInfo` + `ReleaseInfoParser` (payload GitHub releases), `ReleaseUpdateChecker` (`HttpURLConnection`,
  timeout 8 detik, `User-Agent`, tanpa library jaringan baru), dan `openExternalUrl`.
- `TrackifyViewModel.checkForUpdate()` menyimpan hasil ke `TrackifyUiState.latestRelease` / `checkingRelease`;
  satu permintaan per tekan. `MainActivity` meneruskan `appVersion` dan state ke Home serta Setelan.
- Blok Dukungan Setelan menambah **Kredit & versi**: Drezvolx, `https://github.com/drezvolx`,
  `https://discord.com/users/1182612195419369562`, dan versi aplikasi.
- Dokumentasi: `docs/credits.md`, `docs/management/README.md`, `docs/management/processes/{phase-gate,release-process}.md`,
  `docs/management/templates/release-notes.md`, dan README profesional.

### Diubah

- `docs/LOCKED_DECISIONS.md §4.3` diamandemen: `INTERNET` kini mencakup GET publik ke
  `releases/latest` selain redirect Maps. Tidak ada data aplikasi yang dikirim; fitur inti tetap offline.
- `docs/FEATURE_ROADMAP.md`: Fase 18 `Selesai` dengan rincian kontrak pengecek rilis.
- `TrackifyCard`, `TrackifyDialog`, `Scaffold` tunggal, bottom nav, `ScreenTitle`, tema otomatis, dan orientasi
  portrait tidak berubah. `versionCode`/`versionName` belum di-bump; itu dilakukan setelah Fase 18 hijau.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan **36/36** tes (6 tes baru
  `ReleaseUpdateTest`), 0 failure.
- Grep kontrak: token lama 0, `AlertDialog(` langsung 0, `DIRECT_BUTTONS=0`, `SCAFFOLD_FILES` hanya `MainActivity.kt`.
- Smoke perangkat: bel Home membuka dialog **Pembaruan Trackify** dengan `Versi terpasang: 1.0` dan
  `Versi terbaru sudah terpasang (v1.0.0)` (GET GitHub berhasil di perangkat); **Buka GitHub** dan **Tutup** tampil;
  tutup kembali ke Home. Setelan → **Dukung** menampilkan `Kredit & versi`, Drezvolx, kedua URL, dan versi `1.0`;
  tombol **Kembali** berhasil. Tidak ada `FATAL`/`AndroidRuntime` di logcat.
- CI `validate` hijau: run `45258866368` (`3b12289`).

---

## 24 Nov 2026 — Fase 17: tombol outline & spacing konsisten

### Ditambahkan

- `ui/theme/TrackifyButtons.kt`: tiga helper bersama — `TrackifyButton`, `TrackifyTextButton`, dan
  `TrackifyIconButton`. Semuanya memakai `BorderStroke(1.dp, scheme.outlineVariant)`, container transparan,
  dan tanpa shadow. Helper menerima `contentColor` sehingga aksi destruktif tetap memakai `scheme.error`
  tanpa mengubah fill tombol.
- Border `1.dp outlineVariant` ditambahkan ke tombol aksi berbentuk `Box` di Home, Rekap, Lokasi, dan Setelan;
  tombol di hero Home memakai `hero.outlineVariant` agar tetap kontras di atas gradien.

### Diubah

- Seluruh pemakaian langsung `Button`, `OutlinedButton`, `TextButton`, `IconButton`, dan `OutlinedIconButton`
  di Home/Rekap/Lokasi/Catatan/Setelan diganti helper Trackify. Kontrak grep: `DIRECT_BUTTONS=0`,
  `TRACKIFY_BUTTONS=66`.
- Spacing padding/`spacedBy`/`PaddingValues` dinormalisasi ke skala 4/8/12/16/24 dp. `96.dp` bottom clearance,
  `1.dp` border/divider, dan ukuran ikon tidak disentuh. Tidak ada `outlineVariant.copy(alpha = ...)` lagi.
- `TrackifyCard`, `TrackifyDialog`, `Scaffold`, bottom nav, `ScreenTitle`, gradien, dan `96.dp` tidak berubah.
  Kata "bentuk tombol tidak diubah" di catatan Fase 6 kini dibaca sebagai label/aksi/state, bukan styling;
  Fase 17 secara eksplisit mengubah styling outline sesuai persetujuan roadmap.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan **30/30** tes, 0 failure.
- Grep kontrak: `COD_TOKEN=0`, `OLD_IDENTIFIERS=0`, `ALERT_DIRECT=0`, `SUBTEXT=0`, `THEME_MANUAL=0`,
  `DIRECT_BUTTONS=0`, `ALPHA_BORDER=0`, `SHADOW=0`, `OFF_SCALE_SPACING=0`; `Scaffold(` hanya di `MainActivity.kt`.
- Smoke perangkat: kelima tab terbuka; alur Catatan → **Tambah** → editor → **Batal** berhasil; tidak ada
  `FATAL`/`AndroidRuntime` di logcat.
- Tentang shadow: tidak ada nilai `box-shadow` CSS yang terdokumentasi di repo, jadi elevation tetap `0.dp`
  sesuai desain flat; tidak ada `shadow(...)` yang ditambahkan.
- CI `validate` hijau: run `45256244308` (`164394a`).

---

## 24 Nov 2026 — Fase 16: typography tebal & angka tabular

### Ditambahkan

- `GeistFamily` mendapat alias `geist_bold` untuk `FontWeight.ExtraBold` (800) tanpa aset font baru, dan token
  `TabularFigures` (`MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")`) dipakai eksplisit
  pada angka besar Home. Semua slot tipografi memang sudah memakai `tnum` lewat `TextStyle.geist()`.
- Empat tampilan koordinat di Lokasi (GPS live, baris daftar, dialog detail, kartu terdekat) memakai
  `FontFamily.Monospace`, sehingga titik desimal dan digit sejajar.

### Diubah

- `ScreenTitle` memakai `FontWeight.ExtraBold` (tetap 28.sp / 32.sp / -0.6.sp, tanpa subtext, satu komponen
  untuk kelima tab).
- Blok `HEADING` di Catatan lebih berat dari paragraf: pratinjau kartu dan editor memakai `ExtraBold`;
  paragraf tetap `Normal`. Penanda `#` ikut `ExtraBold`.
- Judul kartu tetap `SemiBold` dan judul dialog tetap `titleMedium` — token `TrackifyCard`/`TrackifyDialog`
  tidak berubah. Fase ini hanya typography; spacing dan border ditangani Fase 17.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan **30/30** tes, 0 failure.
- Grep kontrak: `COD_TOKEN=0`, `OLD_IDENTIFIERS=0`, `ALERT_DIRECT=0`, `SUBTEXT=0`, `THEME_MANUAL=0`;
  `Scaffold(` hanya di `MainActivity.kt`; 4 pemakaian `FontFamily.Monospace` dan 3 token `TabularFigures`.
- Smoke perangkat: Home, Lokasi, Catatan terbuka tanpa crash; koordinat `-6.390512, 106.866109` dan daftar
  tetap terbaca; logcat tidak ada `FATAL`/`AndroidRuntime`.
- CI `validate` hijau: run `45253631064` (`0480f1c`).

---

## 24 Nov 2026 — Fase 15: tema gradien monokrom

### Ditambahkan

- `ui/theme/TrackifyGradients`: satu-satunya sumber warna gradien. Dark **Royal Charcoal** — latar
  `#25262a → #1e1f22 → #17181a`, hero `#4c4e51 → #2c2d31 → #1e2023`; light **Pure Charcoal** — latar
  `#fafafa → #f1f1f1 → #e8e8e8`, hero `#3a3a3c → #232324 → #161617`.
- `LocalTrackifyDarkTheme` di `TrackifyTheme.kt` meneruskan hasil `isSystemInDarkTheme()` ke token gradien.
  `TrackifyTheme { ... }` tetap dipanggil tanpa argumen di `MainActivity`; tidak ada preferensi tema baru.

### Diubah

- Root `LazyColumn` Home, Rekap (`SpreadsheetScreen`), Lokasi, Catatan, dan Setelan memakai
  `Modifier.background(TrackifyGradients.background())`. `Scaffold` tetap satu dan hanya di `MainActivity`.
- Hero "Terkirim hari ini" di Home memakai `TrackifyGradients.hero()` dengan teks putih
  (`HeroInk` / `HeroMute`) supaya kontras di kedua tema. Kartu lain tetap memakai token `scheme.surface`
  dan border `outlineVariant`.
- Palet solid pendamping (`TrackifyMockupPalette`) dan `res/values/colors.xml` diselaraskan dengan stop gradien.
  Tidak ada perubahan pada `ScreenTitle`, bottom nav, `TrackifyCard`/`TrackifyDialog`, atau alur data.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan **30/30** tes, 0 failure.
- Grep kontrak: `COD_TOKEN=0`, `OLD_IDENTIFIERS=0`, `ALERT_DIRECT=0`, `SUBTEXT=0`, `THEME_MANUAL=0`;
  `Scaffold(` hanya di `MainActivity.kt`; kelima layar memakai `TrackifyGradients.background()`.
- Smoke perangkat: screenshot dianalisis per piksel. Mode dark: latar `#202124 → #1a1b1d`, hero
  `#4a4c4f → #1e2023`. Setelah `cmd uimode night no` + restart: latar `#f3f3f3 → #ececec`, hero
  `#39393a → #161617`. Mode perangkat dikembalikan ke `night yes`; aplikasi tidak crash.
- CI `validate` hijau: run `45251653184` (`23818b1`).

---

## 24 Nov 2026 — Fase 14: template opsional + multi-foto catatan, DB v8

### Ditambahkan

- `core:domain`: entity `NotePhoto` (`note_photos`, FK `noteId` → `courier_notes.id` dengan `CASCADE`, index `noteId`) dan konstanta `MAX_PHOTOS_PER_NOTE = 5` di `core:data`.
- `NoteTemplates` kini punya opsi **Kosong**: memilihnya langsung membuka editor blank dengan judul, isi, dan daftar foto kosong. Templat `Titipan` / `Kendala Kirim` tetap opsional.
- Editor Catatan menampilkan **Foto catatan (n/5)** dengan tombol **Ambil foto** dan **Dari galeri**, thumbnail, dan **Hapus** per foto. Kartu catatan menampilkan thumbnail (dibatasi `PerformanceConfig.maxPreviewItems`).
- Foto catatan disalin ke `filesDir/note_photos` berawalan `note_` memakai `LocationPhotoStore.persist(..., directory, prefix)` yang kini generik; `file_paths.xml` mendapat entri `note_photos`.
- `NotePhotoBackupTest` (2 tes): round-trip `photos` di backup schema `8` dan penerimaan backup schema `7` tanpa `photos`.

### Diubah

- DB naik ke versi **8** lewat `MIGRATION_7_8` (buat tabel `note_photos` + index); `restoreSnapshot` mengosongkan dan mengisi ulang tabel foto. Tanpa perubahan pada `DailyLog`, `SavedLocation`, atau `courier_notes`.
- `BackupPayload.notePhotos`, `encodeBackup` menulis `"schema": 8` dan array `photos` (maks. 5, diurutkan) per catatan; `decodeBackup` membaca `photos` secara opsional sehingga backup lama tetap diterima.
- `TrackifyRepository.upsertNoteWithPhotos` / `deleteNoteWithPhotos` menjaga batas 5 path unik dan menghapus file saat catatan dihapus. `TrackifyUiState.notePhotos` menyuap UI; `MainActivity` meneruskan 3 argumen `onSaveNote`.
- `TrackifyViewModel.upsertNote` / `deleteNote` menerima daftar path; `incrementTodayDelivered` sudah tidak ada sejak Fase 13. Total tes **30/30**.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan **30/30** tes, 0 failure.
- Grep kontrak: `COD_TOKEN=0`, `OLD_IDENTIFIERS=0`, `ALERT_DIRECT=0`, `SUBTEXT=0`, `THEME_MANUAL=0`, `OLD_LOCATION_ACTIONS=0`; DB versi 8 dan `MIGRATION_7_8` aktif.
- Smoke perangkat: install di atas data Fase 13 tidak crash dan lokasi lama ("Gerai") tetap ada (migrasi v7→v8); chip **Kosong** membuka editor blank; galeri menambah foto sampai `5/5`; **Hapus** per foto menurunkan ke `4/5`; menyimpan catatan menampilkan 4 thumbnail di kartu dan 4 file `note_*.jpg` di `filesDir/note_photos`; menghapus catatan mengosongkan folder dan tabel.
- CI `validate` hijau: run `45249537846` (`f602e21`).

---

## 24 Nov 2026 — Fase 13: Lokasi murni + bagikan foto & teks

### Ditambahkan

- `core:domain`: `LocationShareText.build` menghasilkan teks final: `FOTO`, `-------`, `Nama Penerima : …`, `Detail/Patokan : …`, `Maps : …`, baris kosong, `by Trackify.`
- `ui/locations/LocationSharing`: `ACTION_SEND` dengan foto dari `filesDir/location_photos` lewat `FileProvider` (`EXTRA_STREAM` + `FLAG_GRANT_READ_URI_PERMISSION`) dan teks di `EXTRA_TEXT`; tanpa foto, pakai `ACTION_SEND` teks biasa.
- Manifest mendeklarasikan `androidx.core.content.FileProvider` dengan authority `com.blankpages.trackify.fileprovider` dan `res/xml/file_paths.xml` untuk `location_photos/`.
- Tombol **Bagikan** juga tersedia di detail lokasi.

### Diubah

- Kartu Nearby tidak lagi menampilkan **+1 Pkt** dan **Catat**; helper `createLinkedNote`, callback `onQuickDelivered`/`onCreateLinkedNote`, dan VM `incrementTodayDelivered` dihapus. `upsertNote` tetap dipakai tab Catatan.
- `LocationShareTextTest` (2 tes) mengecek susunan teks persis dan baris kosong. Total tes **28/28**.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan **28/28** tes, 0 failure.
- Smoke perangkat: tab Lokasi tidak lagi memuat tombol `+1 Pkt`/`Catat`; mengetuk **Bagikan** di kartu maupun di detail membuka chooser dengan teks `FOTO / ------- / Nama Penerima : Gerai / Detail/Patokan : / Maps : … / (kosong) / by Trackify.`
- Grep kontrak: `COD_TOKEN=0`, `OLD_IDENTIFIERS=0`, `ALERT_DIRECT=0`, `SUBTEXT=0`, `THEME_MANUAL=0`, `OLD_LOCATION_ACTIONS=0`.
- CI `validate` hijau: run `45244256633` (`80ec5cc`).

---

## 24 Nov 2026 — Fase 12: jam notifikasi bisa diatur user

### Ditambahkan

- `core:domain`: `ReminderTime` (jam 0–23, menit 0–59, `label` seperti `20.00`, `DEFAULT = 20.00`, `of()` untuk nilai dari prefs/backup yang bisa di luar rentang).
- `AppPreferences` menyimpan `reminder_hour` / `reminder_minute`; `TrackifySettings`, `TrackifyRepository`, dan `AppSettings` meneruskan `reminderTime`. Tanpa migrasi DB.
- Setelan → **Pengingat** menampilkan `Notifikasi pukul …` dan tombol **Atur jam**; dialog `TimePicker` 24 jam memakai `TrackifyDialog` dengan **Batal** / **Simpan**.
- Backup JSON menyimpan `reminderHour` / `reminderMinute` di `settings`; backup lama tanpa keduanya memakai default `20.00`.

### Diubah

- `DailyReminderScheduler` tidak lagi memakai konstanta `20.00`: jam dibaca dari prefs setiap kali dijadwalkan, sehingga perubahan langsung berlaku. Deskripsi channel tidak menyebut jam tetap.
- `TrackifyViewModel.updateReminderTime` menyimpan nilai dan menjadwalkan ulang alarm. Tombol **Atur jam** wired di `MainActivity`.
- `ReminderTimeTest` (4 tes): default lama, clamp nilai, round-trip backup, dan fallback backup legacy. Total tes **26/26**.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan **26/26** tes, 0 failure.
- Smoke perangkat: membuka **Atur jam** → memilih `07.05` → label berubah menjadi `Notifikasi pukul 07.05`; setelah force-stop + peluncuran ulang nilai tetap `07.05`; `dumpsys alarm` menunjukkan alarm `DailyReminderReceiver` aktif. Jam dikembalikan ke `20.00` dan alarm terjadwal ulang; tanggal gajian uji dikembalikan ke `25`.
- Grep kontrak: `COD_TOKEN=0`, `OLD_IDENTIFIERS=0`, `ALERT_DIRECT=0`, `SUBTEXT=0`, `THEME_MANUAL=0`.
- CI `validate` hijau: run `45242683391` (`50ee254`).

---

## 24 Nov 2026 — Fase 11: flow gajian & periode bisa diatur user

### Ditambahkan

- `core:domain`: `WageMode` (`PER_PACKAGE` / `FLAT_DAILY`), `WagePeriod` (`DAILY` / `WEEKLY` / `MONTHLY`), dan `WageCalculator` untuk menghitung pendapatan, agregat periode, target periode, dan label. Fungsi murni, tanpa Android.
- `AppPreferences` + `TrackifySettings` + `AppSettings` menyimpan mode, nominal tetap harian, periode, dan tanggal gajian. Default: **Tarif per paket + Bulanan + tanggal 25 + nominal Rp2.500**, sehingga data lama tidak berubah.
- Setelan → **Skema gaji** menambah pilihan mode (outline button), field **Nominal per hari** (hanya muncul di mode nominal tetap), pilihan periode Harian/Mingguan/Bulanan, dan field **Tanggal gajian** (hanya muncul di Bulanan). Ringkasan otomatis menampilkan `Periode … • target … • gajian tanggal …`.
- Backup JSON menyimpan keempat nilai baru di `settings`; backup schema lama tanpa kunci ini tetap diterima dengan default.

### Diubah

- Ringkasan Home dan Rekap memakai kalkulator gaji baru. Mode nominal tetap menghitung `hari kerja × nominal + bonus − pengeluaran` tanpa mengubah `DailyLog` atau schema DB.
- Home menampilkan `Target ${periode}`; progres dan agregat mengikuti periode yang dipilih. Rekap menampilkan label periode di samping target.
- `WageSettingsTest` (4 tes) mengecek mode lama, mode nominal tetap, ringkasan, dan pembagian target periode.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan **22/22** tes (baseline 14 + 4 performa + 4 gaji).
- Smoke perangkat: mengubah ke **Nominal tetap per hari** membuat `Bersih hari ini` menjadi Rp2.500 walau 0 paket; memilih **Mingguan** mengubah label Home jadi `Target Mingguan` dengan target Rp1.000.000 (4M/4); pengaturan dikembalikan ke `Tarif per paket + Bulanan` dan Home kembali ke `Target Bulanan 67%`.
- Grep kontrak: `COD_TOKEN=0`, `OLD_IDENTIFIERS=0`, `ALERT_DIRECT=0`, `SUBTEXT=0`, `THEME_MANUAL=0`.
- CI `validate` hijau: run `45238728969` (`d40d2fe`); run `45238270209` (`832957e`) juga hijau.

---

## 24 Nov 2026 — Fase 10: engine performa & profil perangkat

### Ditambahkan

- `core:domain`: `PerformanceTier` (`LOW`/`BALANCED`/`HIGH`), `PerformanceConfig`, dan `PerformancePolicy` — aturan murni Kotlin yang memetakan tier ke batas ukuran foto, kualitas JPEG, ukuran preview, interval GPS, jarak minimum GPS, dan batas jumlah preview. Tidak ada import Android di sini, jadi bisa diuji tanpa emulator.
- `core:data`: `DevicePerformance` membaca `ActivityManager.isLowRamDevice`, `Runtime.maxMemory()`, dan `Build.VERSION.SDK_INT` **sekali** lalu meng-cache hasilnya. Tidak ada pengaturan manual; profil dihitung otomatis dari perangkat.
- `PerformancePolicyTest` (4 tes) menguji sample size pangkat dua, urutan tier, dan konsistensi batas preview.

### Diubah

- `LocationPhotoStore.persist()` kini menerima `PerformanceConfig`: foto dibaca sekali sebagai byte, ukuran dibaca dari bounds, lalu di-decode dengan `inSampleSize` dan dikompres dengan kualitas sesuai tier. Foto lama tetap terbaca; hanya foto baru yang melalui downsample.
- `LocationPhotoStore.load()` memakai `maxPreviewDimension`, sehingga preview kartu/detail tidak menyimpan bitmap penuh di memory.
- `DeviceLocationSource.startContinuous()` memakai `gpsIntervalMillis` dan `gpsMinDistanceMeters` dari profil, menggantikan hardcode `5_000L / 10f`. Permintaan satu fix (`requestFresh`) sengaja tidak diubah agar tetap responsif.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan **18/18** tes (baseline 14 + 4 tes performa).
- Grep kontrak: `COD_TOKEN=0`, `OLD_IDENTIFIERS=0`, `ALERT_DIRECT=0`, `SUBTEXT=0`, `THEME_MANUAL=0`.
- Smoke perangkat: membuka Lokasi memunculkan `GPS live: -6,36243, 106,86367 • Akurasi ±3 m`; memilih foto dari galeri memunculkan tombol **Hapus foto** (persist berhasil), foto tersimpan setelah `Simpan perubahan`, lalu dihapus kembali untuk membersihkan data uji. Tidak ada `FATAL EXCEPTION`/`OutOfMemory`.
- CI `validate` hijau: run `45233578272` (`d81a819`).

---

## 24 Nov 2026 — Fase 9: input paket pakai teks

### Ditambahkan

- Home: field **Dibawa** dan **Dikirim** dengan keyboard numerik. User bisa mengetik jumlah persis, lalu menekan **Simpan paket**; tombol `-1`, `+1`, `+5`, `+10` tetap tersedia seperti sebelumnya.
- State field disinkronkan ulang dari `DailyLog` saat nilai berubah lewat aksi lain, sehingga tidak ada dua sumber kebenaran.

### Diubah

- Commit input teks tetap melewati `DailyLog.sanitized()`: `dikirim` dipotong ke `dibawa` bila user mengetik angka lebih besar, dan nilai negatif ditolak. Konfirmasi aksi cepat Fase 5 tidak berubah.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan 14/14 tes.
- Smoke test perangkat: mengetik `12` dibawa + `7` dikirim menghasilkan `7 dari 12 paket` (58%, Rp12.600); input `3` dibawa + `9` dikirim ter-clamp menjadi `dari 3 paket`; data uji dikembalikan ke `0 dari 0 paket`.
- CI `validate` hijau: run `45232043597` (`baf47d2`).

---

## 24 Nov 2026 — Fase 8: catatan blok ala Notion lite

### Ditambahkan

- Editor Catatan kini memakai blok: **Teks** (paragraf), **Judul**, **Daftar**, dan **Checklist**. Chip toolbar menambah blok baru; setiap blok bisa diedit dan dihapus, checklist bisa dicentang.
- `NoteBlocks` (`core:domain`) membaca dan menulis blok dari/ke satu field `content` yang sudah ada: `# ` = Judul, `- ` = Daftar, `[ ]` / `[x]` = Checklist, sisanya paragraf. Tidak ada tabel, database, kolom, atau schema DB baru.
- Kartu Catatan menampilkan preview hingga 4 blok dengan penanda yang sesuai (`#`, `•`, `[ ]` / `[x]`), bukan satu paragraf polos.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan 14/14 tes.
- Harness parser: kelima tipe blok terbaca dengan `lineIndex` benar dan `serialize(parse(x)) == x` (roundtrip identik).
- Smoke test perangkat: membuat catatan berisi paragraf + judul + daftar + checklist, mencentang checklist, menyimpan, lalu membuka ulang — semua blok dan status centang tetap ada; preview tampil di kartu. Catatan uji dihapus.
- Bottom nav, `ScreenTitle`, tema otomatis, dan kontrak UI lain tidak berubah.
- CI `validate` hijau: [run 45226188604](https://github.com/drezvolx/Trackify/actions/runs/45226188604).

---

## 24 Nov 2026 — Fase 7: URL Maps & hapus sistem tag

### Diubah

- Editor Lokasi: field **Latitude** dan **Longitude** diganti satu field **URL Maps**. Tempel URL Google Maps apa adanya; tombol **Pakai Koordinat GPS Saat Ini** kini mengisi field itu dengan URL yang memuat koordinat GPS.
- `MapUrlParser` (`core:domain`) menangani `@lat,lon`, `q=`, `query=`, `destination=`, `ll=`, `center=`, `!3d...!4d...`, `%2C`, dan pasangan `lat,lon` polos dengan validasi rentang; koordinat hasil parse tetap disimpan di kolom DB lama.
- `MapLinkResolver` (`core:data`) mengikuti redirect short link `maps.app.goo.gl` maksimal 5 kali dengan `instanceFollowRedirects = false` dan timeout 8 detik, dipanggil dari korutina IO.
- Sistem tag **Langganan / Titip / Biasa** dihapus total: chip filter, chip kategori editor, `inferTag()`, kode inisial tag, dan penggabungan tag ke catatan patokan. Catatan lokasi sekarang murni teks bebas.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan 14/14 tes.
- Parser diuji langsung untuk 10 contoh (semua format di atas + koordinat di luar rentang + teks bukan URL): 8 format valid menghasilkan `-6.2,106.8`, 2 ditolak.
- Smoke test perangkat: chip tag hilang dari daftar dan editor, field **URL Maps** tampil; menempel `https://maps.google.com/?q=-6.25,106.85` menghasilkan lokasi dengan koordinat `-6.25000, 106.85000` di detail dan kartu. Lokasi uji dihapus.
- Izin `INTERNET` dari Fase 6 dipakai resolver; tidak ada perubahan DB, bottom nav, `ScreenTitle`, atau tema.
- CI `validate` hijau: [run 45223902757](https://github.com/drezvolx/Trackify/actions/runs/45223902757).

---

## 23 Nov 2026 — Fase 6: foto lampiran lokasi

### Ditambahkan

- Editor Lokasi: tombol **Ambil foto** (izin kamera → `MediaStore`, foto masuk galeri) dan **Dari galeri**. Salinan stabil disimpan di `filesDir/location_photos` supaya tetap bisa dibuka.
- Preview foto tampil di editor, dialog detail, dan kartu **Terdekat**; tombol **Hapus foto** menghapus salinan aplikasi.
- `SavedLocation.photoPath`, `MIGRATION_6_7`, dan backup schema `7` (backup lama schema 5/6 tetap bisa dibaca).
- Izin `CAMERA` dan `INTERNET` ditambahkan; `WRITE_EXTERNAL_STORAGE` hanya untuk Android ≤ 9.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan 14/14 tes.
- Kontrak diperbarui di `docs/LOCKED_DECISIONS.md` §3, §4.3, §4.4, dan §9 sesuai keputusan user.
- Smoke test perangkat: install di atas DB lama sukses, lokasi **Gerai** tetap ada; **Dari galeri** berhasil memilih gambar, preview tampil, file tersalin, dan tetap tampil setelah force-stop/restart (kartu + dialog detail); menghapus lokasi ikut menghapus file fotonya. Tombol **Ambil foto** berhasil membuka kamera, tetapi aplikasi kamera bawaan perangkat uji force-stop sebelum capture, jadi jalur kamera belum terverifikasi sampai hasil foto. Data uji dihapus dan galeri uji dibersihkan.
- CI `validate` hijau: [run 45221394811](https://github.com/drezvolx/Trackify/actions/runs/45221394811).

---

## 23 Nov 2026 — Fase 5: konfirmasi Home & pengurutan catatan

### Ditambahkan

- Home: aksi cepat `-1`, `+1`, `+5`, `+10` kini membuka `TrackifyDialog` konfirmasi sebelum mengubah jumlah paket; tombolnya sendiri tidak berubah bentuk.
- Home: toggle **Masuk → Libur** juga dikonfirmasi karena mengosongkan paket dan pengeluaran hari ini. Toggle **Libur → Masuk** tetap langsung.
- Catatan: chip **Urut** dengan pilihan `Prioritas` (default), `Terbaru`, `Terlama`, dan `Judul A–Z`; state memakai `rememberSaveable`.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan 14/14 tes.
- Kontrak tetap: `ScreenTitle` tanpa subtext, kartu/dialog memakai token `TrackifyCard`/`TrackifyDialog`, tidak ada tab atau menu tema baru.
- Smoke test perangkat: dialog konfirmasi `+1`/`-1` tampil dan perubahan angka benar-benar terjadi setelah **Ya, perbarui**; toggle **Masuk → Libur** memunculkan konfirmasi; chip **Urut** tampil di Catatan. Data uji dikembalikan ke `54/54` lewat editor Rekap.

---

## 23 Nov 2026 — Dukung dev via QRIS

### Ditambahkan

- Setelan: seksi **Dukung** berisi kartu **Dukung dev** memakai `TrackifyCard`; saat ditekan membuka `TrackifyDialog` berisi gambar QRIS.
- Aset `app/src/main/res/drawable/qris_support.jpg` berisi **kode QR saja** (632×632), dipotong otomatis dari screenshot 1136×1600 dengan margin 40 px; payload QR terverifikasi identik dengan aslinya.
- Dialog menampilkan QR dalam kotak `300.dp` memakai `ContentScale.Fit` supaya tidak terpotong, plus tombol **Tutup**.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan 14/14 tes.
- Tidak menambah tab navigasi, header tetap `ScreenTitle`, dan larangan fitur pembayaran tunai tetap berlaku.
- Smoke test perangkat: seksi **Dukung** dan kartu **Dukung dev** muncul di bawah **Data**; menekan kartu membuka dialog berisi gambar QRIS dan tombol **Tutup**.
- Verifikasi screenshot perangkat (1080×2460): QR di dialog tetap terdeteksi dan payload-nya identik dengan aset sumber, sehingga tidak terpotong saat ditampilkan.

---

## 23 Nov 2026 — Penghapusan jejak fitur pembayaran tunai

### Ditambahkan

- Larangan fitur pembayaran tunai tetap berlaku, tetapi ditulis tanpa menyebut nama fiturnya supaya tidak ada jejak literal di dokumen aktif.

### Dihilangkan

- Seluruh kemunculan nama fitur tersebut dari `CHANGELOG.md`, `Documentation.md`, `docs/FEATURE_ROADMAP.md`, dan `docs/LOCKED_DECISIONS.md`.
- `trackify_dump.txt`, semua folder `backup_*`, dan `trackify_fix` dari workspace lokal. Arsip itu tidak ikut di-build dan sebagian masih menyimpan kode lama; riwayat git serta tag `v1.0.0` kini menjadi cadangan resmi satu-satunya.

### Verifikasi

- Scan seluruh file (kecuali `.git`, `build`, `.gradle`): 0 kemunculan literal maupun identifier lama.
- Source aktif tetap 0; ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` hijau dengan 14/14 tes.

---

## 23 Nov 2026 — Fase 4: sisa usulan per tab

### Ditambahkan

- Home: batang grafik 7 hari kini bisa dipilih; ringkasan hari terpilih tampil di bawah label, dengan warna batang aktif memakai `primary`.
- Catatan: tombol **Pilih** mengaktifkan mode seleksi berisi checkbox per catatan, aksi **Tandai selesai**, dan hapus massal lewat konfirmasi `TrackifyDialog`.
- Setelan: pemilih bulan CSV memakai tombol `‹`/`›`; subtitle ekspor mengikuti bulan yang dipilih dan ekspor memakai `exportCsvForMonth`.

### Diubah

- `TrackifyEngine.filterNotes` menerima daftar lokasi dan ikut mencocokkan `customerName` saat mencari catatan.
- Status "Pembagian gajian" tampil sebagai teks `2x (info)`, bukan pill menyerupai tombol.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` `BUILD SUCCESSFUL`; 14/14 tes lulus.
- Grep kontrak tetap kosong; CI `validate` sukses pada run `45203998438` untuk `6ca2636`.
- Smoke test perangkat: grafik berpindah ke "Sel 11-17 • 91 paket" saat batang diketuk, mode Pilih menampilkan aksi massal dan `Hapus (1)` setelah satu catatan dicentang, Setelan menampilkan `Bulan CSV` serta `2x (info)`.

---

## 23 Nov 2026 — Fase 3: penyempurnaan per tab

### Ditambahkan

- Home: pintasan **Catatan** di samping tombol **Buka** pada bagian Lokasi, mengarah ke `TrackifyTab.CATATAN`.
- Rekap: `sanitizeDecimalInput` menerima satu pemisah desimal (titik atau koma) untuk tarif, bonus, dan potongan; keyboard memakai `KeyboardType.Decimal`.
- Setelan: konfirmasi lewat `TrackifyDialog` sebelum memilih file restore; subtitle backup kini menyebut log, lokasi, catatan, dan setelan.

### Diubah

- Rekap: DatePicker dibuka pada tanggal log yang sedang diedit, atau tanggal pertama bulan terpilih bila bukan bulan berjalan.
- Laporan WhatsApp: rata-rata paket per hari diformat dengan locale `id-ID`, bukan format default perangkat.
- Catatan: chip templat kini menandai seleksi, bisa di-reset, dan mengisi editor; flag `isNew` di wiring `MainActivity` kini dipakai untuk memaksa id baru.
- Catatan: `updatedAt` tidak ditimpa bila menyimpan edit tanpa perubahan judul, isi, pin, atau lokasi tertaut.

### Tidak dilakukan

- Grafik 7 batang interaktif dan pemilih bulan CSV masih menjadi usulan berikutnya agar layout dan kontrak tetap stabil.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` `BUILD SUCCESSFUL`; 14/14 tes lulus.
- Grep field lama, `WindowInsets(0)`, `FusedLocationProviderClient`, dan `AlertDialog` langsung tetap kosong; `AlertDialog` hanya ada di dalam token `TrackifyDialog`.
- CI `validate` sukses pada run `45200069573` untuk `deb8dc9`; run `45198808613` (Fase 2) dan `45198332967` (Fase 1) juga sukses.
- Smoke test perangkat: pintasan Catatan di Home berpindah ke tab Catatan dan chip templat membuka editor yang sudah terisi. Pengujian lanjutan terhenti karena perangkat dipakai aplikasi lain.

---

## 23 Nov 2026 — Fase 2: state UI bertahan

### Diubah

- Rekap: `subTab`, `viewMode`, `sortDesc`, `filterMode`, `showSearch`, dan `searchQuery` kini memakai `rememberSaveable`.
- Lokasi: query pencarian dan tag terpilih kini memakai `rememberSaveable`.
- Catatan: query pencarian dan filter status kini memakai `rememberSaveable`.
- Setelan: draft tarif, bonus, target, dan status pengingat memakai `rememberSaveable` dengan key yang sama seperti sebelumnya.
- State dialog dan objek data (`DailyLog`, `SavedLocation`, `CourierNote`) sengaja tetap memakai `remember` karena tidak bisa disimpan di `Bundle`.
- `targetRupiah` di Rekap tetap `remember(targetAmount)` karena merupakan draft lokal yang harus mengikuti source of truth.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` `BUILD SUCCESSFUL`; 14/14 tes lulus.

---

## 23 Nov 2026 — Fase 1 pengembangan fitur & peta jalan

### Ditambahkan

- `docs/FEATURE_ROADMAP.md`: pemetaan kondisi aktual dan usulan pengembangan per tab, lengkap dengan rencana tiga fase dan batasan kontrak. Tautannya juga ditambahkan ke `README.md`.

### Diperbaiki

- `DeviceLocationSource` kini menolak fix GPS yang lebih tua dari 60 detik atau memiliki akurasi lebih dari 200 m, baik untuk `lastKnown`, `requestFresh`, maupun update berkelanjutan.
- Menghapus lokasi memakai transaksi Room: `linkedLocationId` pada catatan dikosongkan lebih dulu agar tombol Rute tidak mengacu ke data yang sudah hilang.
- Konfirmasi hapus lokasi dan hapus log kini lewat `TrackifyDialog`, menggantikan aksi langsung tanpa konfirmasi.
- `incrementTodayDelivered` mengembalikan `Boolean`; toast `+1 Pkt` hanya muncul saat jumlah benar-benar berubah.

### Verifikasi

- ktlint 1.8.0 exit 0; `:app:assembleDebug :app:testDebugUnitTest` `BUILD SUCCESSFUL`; 14/14 tes lulus.
- Grep field lama, subtext header, dan tema manual tetap kosong.
- Backup sebelum perubahan: `backup_before_feature_phase1_20261123_213934`.

---

## 23 Nov 2026 — Perbaikan CI

### Diperbaiki

- Run CI pertama gagal di langkah `Set up Android SDK` karena `android-actions/setup-android@v3` masih mencoba memasang paket `tools` yang sudah tidak disediakan Google.
- Workflow naik ke `actions/checkout@v5`, `actions/setup-java@v5`, dan `android-actions/setup-android@v4`.
- Komponen SDK `platforms;android-37.0` dan `build-tools;37.0.0` sekarang dipasang eksplisit sebelum build.
- Run CI kedua gagal di langkah `Install Android SDK components` karena `platforms;android-37` bukan ID paket yang valid; ID yang benar adalah `platforms;android-37.0` (sesuai SDK lokal dan katalog Google channel-0).
- Langkah penerimaan lisensi dihapus karena sudah dilakukan otomatis oleh `setup-android@v4`.
- Tag `v1.0.0` dipindahkan ke commit final yang sudah terverifikasi CI hijau.

### Verifikasi

- Perintah ktlint dengan glob yang dipakai CI sudah dijalankan lokal dan keluar dengan exit 0.
- Run CI `45193097358` (commit `ebf4e6f`) berakhir `success`: checkout, JDK 21, Android SDK, install komponen SDK, ktlint, build, dan 14 tes JVM semuanya hijau.
- Tag `v1.0.0` kini menunjuk ke commit dengan CI hijau, bukan lagi commit sebelum perbaikan workflow.

---

## 23 Nov 2026 — CI & rilis v1.0.0

### Ditambahkan

- `.github/workflows/ci.yml`: otomatis menjalankan ktlint 1.8.0 (checksum terverifikasi), `:app:assembleDebug`, dan `:app:testDebugUnitTest` pada push ke `main`, pull request, dan trigger manual.
- Badge status CI di `README.md`.
- Tag rilis beranotasi `v1.0.0` pada commit versi stabil ini.

### Tidak diubah

- UI, kontrak bottom navigation, tema otomatis, alur data, dan konfigurasi rilis tetap seperti versi terverifikasi.

---

## 23 Nov 2026 — Verifikasi smoke test debug final

### Verifikasi

- Debug APK final di-install ulang dan dibuka: Home, Rekap, Lokasi, Catatan, dan Setelan semuanya render normal dengan PID aktif.
- Bottom nav tetap tampil di empat tab utama dan hilang total di Setelan; hanya tombol `Kembali` yang tersedia.
- Menekan `Kembali` dari Setelan mengembalikan Home beserta bottom nav tanpa crash.

---

## 23 Nov 2026 — Gaya kode idiomatik & konvensi komentar

### Diubah

- Delapan file yang sebelumnya memakai wildcard import kini memakai import eksplisit: `TrackifyViewModel`, `TrackifyComponents`, `TrackifyEngineTest`, `HomeScreen`, `SettingsScreen`, `NotesScreen`, `LocationsScreen`, dan `SpreadsheetScreen`.
- Wildcard import sisa di `TrackifyEngine`, `ExampleInstrumentedTest`, dan `TrackifyDatabase` juga diganti import eksplisit supaya seluruh source aktif konsisten.
- `.editorconfig` ditambahkan: ktlint `android_studio`, maksimal 120 kolom, trailing comma, `function-naming` dimatikan untuk Composable PascalCase, dan `filename` dimatikan agar `TrackifyNavigation.kt` tetap memuat `TrackifyTab`.
- `ktlint --format` diterapkan ke seluruh source aktif dan build file, sehingga import ordering, trailing comma, wrapping, dan final newline konsisten tanpa mengubah perilaku atau string.
- Seluruh komentar `//` di source aktif dan build file diseragamkan ke `// Topic > ...` dalam bahasa Inggris, misalnya `// Theme & System bar > Follow the device via SystemBarStyle.auto.`.
- Import/local yang tidak dipakai dibersihkan, termasuk `headerDate` dan formatter terkait di `HomeScreen`.

### Diperbaiki

- Build gagal setelah refactor karena `androidx.compose.foundation.layout.weight` (internal) dan `androidx.compose.runtime.rememberDatePickerState` salah import. Import di `LocationsScreen` dan `SpreadsheetScreen` diperbaiki.
- `JournalMode` ternyata enum bersarang di `RoomDatabase`, bukan kelas paket `androidx.room`. Referensi di `TrackifyDatabase` dipakai sebagai `RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING`.

### Verifikasi

- `ktlint` check exit 0 untuk seluruh source aktif dan build file.
- `:app:assembleDebug` dan `:app:testDebugUnitTest` `BUILD SUCCESSFUL`; 14/14 tes lulus, 0 failure, 0 warning compiler.

---

## 23 Nov 2026 — Baseline profile

### Ditambahkan

- Modul `:baselineprofile` dengan `BaselineProfileRule.collect(...)` untuk cold start: `pressHome()` lalu `startActivityAndWait()`.
- `androidx.profileinstaller` di `:app` supaya profil langsung dipakai pada instalasi lokal (tanpa Play Store).
- Varian `nonMinifiedRelease` dibuat eksplisit: AGP 9 mewariskan `optimization` dari `release` lewat `initWith`, jadi R8 harus dimatikan di varian penghasil profil.
- Profil hasil generate di `app/src/release/generated/baselineProfiles/` (`baseline-prof.txt` dan `startup-prof.txt`), sementara output build modul `:baselineprofile` diabaikan lewat `.gitignore`.

### Diubah

- `settings.gradle.kts`, version catalog, dan root build script mendaftarkan plugin `com.android.test` dan `androidx.baselineprofile` `1.5.0`.

### Verifikasi

- `:app:generateReleaseBaselineProfile` sukses di perangkat API 34; `baseline.prof` ikut terkemas di APK release (2,01 MB).
- Setelah dexopt stabil, cold start release 10 kali: **340–394 ms** (median ~353 ms), lebih cepat dari ~418 ms sebelum profil. Beberapa kali pertama setelah install lebih lambat karena ProfileInstaller masih mengompilasi profil.
- UI release tetap normal: Home, bottom nav, dan PID aktif.

---

## 23 Nov 2026 — Lisensi MIT

### Ditambahkan

- `License.md` dengan MIT License: kode boleh dipakai, dimodifikasi, dan didistribusikan, tetapi notice lisensi serta atribusi copyright wajib ikut serta pada salinan atau bagian substantif.
- Tautan ke lisensi di `README.md`, pada bagian Dokumentasi dan Lisensi.

### Tidak diubah

- Fitur, UI, alur data, dan konfigurasi build tetap seperti versi sebelumnya.

---

## 23 Nov 2026 — Startup & konsistensi field (`593e64e`)

### Diubah

- `TrackifyViewModel` mengambil satu snapshot `SharedPreferences` saat startup, lalu menjadwalkan alarm pengingat di `Dispatchers.Default` supaya tidak mem-blok frame pertama.
- Pipeline `uiState` memakai `flowOn(Dispatchers.Default)` untuk sanitasi dan agregasi bulanan.
- Jadwal alarm saat restore/reset juga dijalankan di worker, dengan flag pengingat dari state terbaru (bukan baca prefs ulang).
- Semua `OutlinedTextField` memakai helper `trackifyFieldColors()` dengan border `outlineVariant` dan radius `12.dp`, termasuk field editor Rekap yang sebelumnya memakai default Material3.
- Sisa ikon Material (`Settings`, `LocationOn`, `KeyboardArrowLeft/Right`, `Search`) diganti `TrackifyIcons`.
- Dependency lifecycle Compose disamakan ke `2.11.0`.

### Dihilangkan

- `androidx.compose.material:material-icons-extended` — tidak dipakai lagi setelah semua ikon bersumber dari `TrackifyIcons`.

### Ditambahkan

- 4 unit test backup/restore di domain: roundtrip data + pengaturan, penolakan header asing, fallback schema lama, dan clamp `packagesDelivered <= packagesCarried`.
- `org.json:json:20240303` sebagai dependensi test JVM.

### Diperbaiki

- Build release gagal langsung tutup tanpa log crash. Penyebabnya `packageScope` pada konfigurasi R8 AGP 9. R8 default dipakai dan terverifikasi normal.

### Verifikasi

- `BUILD SUCCESSFUL` untuk `:app:assembleDebug`, `:app:testDebugUnitTest`, dan `:app:assembleRelease`; 0 warning compiler.
- 14/14 unit test lulus, 0 failure.
- Cold start release (signed, 6 kali): **383–433 ms** TotalTime, median ~418 ms; APK 1,93 MB.
- Debug tetap normal; UI dan PID aktif setelah force-stop + launch.

---

## 23 Nov 2026 — Icon pack solid (`9e9cd0e`)

### Ditambahkan

- Ikon solid baru: `Bell`, `Target`, `Wallet`, `ChartBar`, `Route`, `Link`, `Info`, `Warning`, `Close`, `Refresh`, `Backup`, `Restore`, `TemplateTitipan`, `TemplateKendala`.
- Helper `solid()` dan `solidCut()` untuk path filled dengan `PathFillType.EvenOdd` sebagai pengganti garis.
- `README.md` sebagai ringkasan singkat aplikasi.

### Diubah

- Semua ikon lama di `TrackifyIcons.kt` dari **outline/stroke** menjadi **solid/filled**.
- Detail ikon memakai *negative space*, bukan garis tipis.
- Semua ikon tetap `24×24`, satu warna, dan bisa di-`tint` oleh `MaterialTheme`.
- Nama konstanta dan alias lama tetap dipertahankan, sehingga UI tidak perlu diubah.

### Dihilangkan

- `stroke = SolidColor(...)`, `strokeLineWidth`, `StrokeCap`, dan `StrokeJoin` dari helper ikon.

### Tidak diubah

- `ReferenceNotchedBottomBar`, `TrackifyTab`, notch, orbit, spring, dan `Scaffold.bottomBar`.
- Alur data UI → `TrackifyViewModel` → `TrackifyRepository` → DAO/Room.

---

## 23 Nov 2026 — README (`5a80ae3`)

### Ditambahkan

- `README.md` berisi deskripsi aplikasi, daftar fitur per layar, teknologi, struktur modul, cara build, dan tautan dokumentasi.

---

## 23 Nov 2026 — Baseline terverifikasi (`6283cbd`)

### Ditambahkan

- Tema otomatis mengikuti sistem: light saat sistem light, dark saat sistem dark.
- `TrackifyCard`, `TrackifyDialog`, dan `ScreenTitle` sebagai token UI bersama.
- Backup/restore JSON schema `6`, termasuk pengaturan aplikasi.
- Restore penuh: data lama dibersihkan sebelum data backup dimasukkan.
- Reset data: tabel database dan `SharedPreferences` dikosongkan.
- Kunci orientasi portrait di `AndroidManifest.xml`.

### Diubah

- Header semua tab jadi seragam: hanya judul, `28.sp`, `Bold`, tanpa subtext.
- Dialog dan kartu memakai `MaterialTheme.colorScheme` yang sama.
- Status bar memakai `WindowInsets.statusBars`.
- `bottomBar` hanya ditampilkan ketika tidak berada di Settings.

### Dihilangkan

- Menu **Tampilan** dan toggle tema manual dari Settings.
- Field dan istilah fitur pembayaran tunai dari source aktif.
- `AlertDialog(` langsung di luar wrapper `TrackifyDialog`.
- Subtext pada header semua tab.

### Diperbaiki

- Flag simpan Lokasi: `editingTarget != null` untuk edit, `null` untuk lokasi baru.
- Bug lokasi baru yang sebelumnya salah memanggil `updateLocation`.

### Verifikasi

- `BUILD SUCCESSFUL`.
- 10/10 unit test lulus, 0 failure, 0 warning.
- Alur tambah/edit/hapus Lokasi & Catatan persist setelah force-stop/relaunch.
- Backup → reset → restore mengembalikan 9 log, 1 lokasi, 1 catatan, dan pengaturan.
