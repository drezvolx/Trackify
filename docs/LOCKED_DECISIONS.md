# Trackify — Kontrak Keputusan Terkunci

> **Status:** Berlaku per **2026-11-23**.  
> **Sifat dokumen:** Kontrak / *source of truth*. File ini mengunci hasil keputusan yang sudah disetujui user.  
> **Aturan utama:** Kalau kode dan dokumen ini bertentangan, **jangan menebak**. Stop, laporkan, dan minta keputusan eksplisit sebelum mengubah apa pun.

---

## 0. Aturan Emas (Jangan Dilanggar)

1. **Bottom navigation final sudah disetujui. Jangan diubah** bentuk, warna, inset, animasi, atau strukturnya.
2. **Fitur pembayaran tunai saat pengiriman dilarang total** di source aktif, entity, migrasi, summary, UI, template, dan dokumentasi baru.
3. **Status bar harus rapi**: konten tidak boleh menyatu dengan bar notifikasi HP.
4. **Hilt tetap `2.60.1`**. Jangan downgrade.
5. **Build + 10 tes harus tetap hijau** sebelum perubahan dianggap selesai.
6. **Setiap perubahan besar wajib di-commit dulu**, lalu update dokumen ini jika keputusan berubah secara sah.
7. Workspace **adalah git repo**. Rollback memakai riwayat `main` dan tag `v1.0.0`, bukan folder arsip lokal.
8. **Header semua tab cukup judul, tanpa subtext**, dan wajib memakai `ScreenTitle` dari `ui/theme/TrackifyTheme.kt`.
9. **Tema aplikasi wajib mengikuti perangkat otomatis**. Menu pilihan tema manual dilarang.
10. **Orientasi wajib portrait**. Jangan menghidupkan rotasi otomatis di app.
11. **Satu sumber warna**: `res/values/colors.xml`. Dilarang warna di luar tema di UI lama maupun baru;
    Kotlin wajib lewat `TrackifyColors.kt`. Lihat §4.9.
12. **Header Catatan sudah final**: tombol ikon **Pilih/Batal** di samping **Tambah**, plus long-press sebagai
    jalur alternatif. Teks **Pilih/Batal** di header dilarang. Lihat §4.9.

---

## 1. BOTTOM NAVIGATION — ZONA PALING KRITIS

### 1.1 Lokasi & Identitas

| Item | Nilai tetap |
|---|---|
| File utama | `app/src/main/java/com/blankpages/trackify/MainActivity.kt` |
| Composable | `ReferenceNotchedBottomBar` |
| Enum tab | `app/src/main/java/com/blankpages/trackify/navigation/TrackifyNavigation.kt` → `TrackifyTab` |
| Pemanggilan | **Hanya** dari `Scaffold.bottomBar` di `MainActivity.kt` |
| Modul | **Tetap di `:app`**. Jangan dipindah ke `:feature:*`. |

**Dilarang:**
- Memindahkan bottom bar ke modul feature.
- Mengganti ke `NavigationBar`, `BottomNavigation`, `NavigationBarItem`, atau Navigation Component.
- Menghapus `ReferenceNotchedBottomBar`.
- Mengubah urutan tab.
- Mengubah nama/struktur `TrackifyTab` tanpa keputusan eksplisit.

Urutan tab final:

```kotlin
HOME, REKAP, LOKASI, CATATAN
```

`TrackifyTab.of(ordinal)` harus tetap fallback ke `HOME`.

---

### 1.2 Konfigurasi Visual Final — JANGAN DIUBAH

Nilai-nilai ini adalah hasil iterasi terakhir dan sudah disetujui user:

```kotlin
val barHeight: Dp = 64.dp
val topLift: Dp = 18.dp
val orbSize: Dp = 46.dp
val notchRadius: Dp = 30.dp
val cornerRadiusPx = with(density) { 18.dp.toPx() }
val strokeWidthPx = with(density) { 1.dp.toPx() }
val barColor = scheme.background.copy(alpha = 0.72f)
val borderColor = scheme.outlineVariant
val outlineColor = scheme.outline
```

Modifier `Box` bottom bar:

```kotlin
Modifier
    .fillMaxWidth()
    .navigationBarsPadding()
    .padding(horizontal = 14.dp, vertical = 8.dp)
    .height(barHeight + topLift)
    .onSizeChanged { barSize = it }
    .drawBehind { ... }
```

Path notch:

```kotlin
baseRectPath.addRoundRect(
    RoundRect(
        rect = Rect(0f, topLiftPx, widthPx, totalHeightPx),
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
    )
)

scoopPath.addOval(
    Rect(
        left = centerXPx - notchRadiusPx,
        top = topLiftPx - notchRadiusPx * 0.72f,
        right = centerXPx + notchRadiusPx,
        bottom = topLiftPx + notchRadiusPx * 1.28f
    )
)

notchedBarPath.op(baseRectPath, scoopPath, PathOperation.Difference)
drawPath(path = notchedBarPath, color = barColor)
drawPath(path = notchedBarPath, color = borderColor, style = strokeSpec)
```

**Artinya:**
- **Outline/notch WAJIB ada.** Jangan hapus stroke `outlineVariant` 1.dp.
- **Fill dalam outline WAJIB `scheme.background.copy(alpha = 0.72f)`.**
  - Jangan `Color.Transparent` → user bilang terlalu tipis.
  - Jangan `Color.Black` / `#141414` solid → itu kotak hitam lama yang sudah dihapus.
  - Jangan alpha `1f` → bukan desain final.
- **Area di luar/daerah belakang outline tidak boleh ada container gelap.** Konten layar harus ikut menggambar sampai ke belakang outline.
- `Scaffold.containerColor` tetap **`scheme.background`**, bukan `Color.Transparent`. Transparent membuat status bar/window background jadi putih.

---

### 1.3 Ikon & Orbit

```kotlin
animateFloatAsState(
    targetValue = currentTab.ordinal.toFloat(),
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    ),
    label = "notched_tab_index"
)
```

- Orbit mengikuti `animatedIndex`, bukan langsung `currentTab.ordinal`.
- Orbit: `Surface` `CircleShape`, warna `primary`, `BorderStroke(2.dp, outlineColor)`.
- Ikon selected: `currentTab.icon`, `onPrimary`, ukuran `21.dp`.
- Ikon unselected: `tab.icon`, `onSurfaceVariant`, ukuran `21.dp`.
- `indication = null` pada `clickable` tetap dipertahankan.

**Jangan:**
- Mengganti spring dengan tween kecepatan tinggi.
- Mengubah ukuran orbit/ikon.
- Menambah ripple.
- Menambah label teks di bawah ikon.

---

### 1.4 Insets & Status Bar — JANGAN DIUBAH

Ini bagian yang pernah bermasalah tiga kali:

1. `contentWindowInsets = WindowInsets(0)` membuat konten naik ke bar notifikasi HP.
2. `Scaffold(containerColor = Color.Transparent)` membuat area status bar tampak putih.
3. `SettingsScreen` dirender di luar `Scaffold`, sehingga tidak mendapat inset status bar dan kontennya mulai di `y≈34`.

Konfigurasi final yang WAJIB:

```kotlin
Scaffold(
    containerColor = scheme.background,
    // Status bar tetap dihormati di semua layar, termasuk Settings.
    // Bottom inset sengaja tidak dipakai supaya konten boleh lewat
    // di belakang outline bottom bar saat bottom bar ditampilkan.
    contentWindowInsets = WindowInsets.statusBars,
    bottomBar = {
        if (!inSettings) {
            ReferenceNotchedBottomBar(
                currentTab = currentTab,
                onSelectTab = { tabOrdinal = it.ordinal }
            )
        }
    }
) { padding ->
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = padding.calculateStartPadding(LocalLayoutDirection.current),
                top = padding.calculateTopPadding(),
                end = padding.calculateEndPadding(LocalLayoutDirection.current)
            )
    ) {
        if (inSettings) {
            SettingsScreen(...)
        } else {
            AnimatedContent(...) { tab -> TabContent(...) }
        }
    }
}
```

**Aturan:**
- **Satu `Scaffold` harus membungkus semua layar**, termasuk `SettingsScreen`. Jangan mengembalikan cabang `if (inSettings) ... else Scaffold(...)`.
- Bottom bar tetap dipanggil dari `Scaffold.bottomBar`, hanya dibuat saat `!inSettings`.
- `enableEdgeToEdge()` tetap dipakai.
- `WindowInsets.statusBars` **wajib**.
- **Jangan** pakai `WindowInsets(0)`.
- **Jangan** pakai `.padding(padding)` penuh pada `Box` konten, karena itu akan memotong area belakang outline.
- Padding bawah ditangani oleh masing-masing layar, bukan oleh `Scaffold`.

Padding bawah tiap tab:

| Tab | File | `contentPadding` |
|---|---|---|
| Home | `ui/home/HomeScreen.kt` | `bottom = 96.dp` |
| Rekap | `ui/spreadsheet/SpreadsheetScreen.kt` | `bottom = 96.dp` |
| Lokasi | `ui/locations/LocationsScreen.kt` | `start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp` |
| Catatan | `ui/notes/NotesScreen.kt` | `start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp` |
| Settings | `ui/settings/SettingsScreen.kt` | `bottom = 96.dp`, sekarang tetap dapat inset `statusBars` dari `Scaffold` |

Alasan `96.dp`: konten boleh lewat di belakang outline, tetapi item terakhir tidak boleh tertutup nav.

Verifikasi bounds di emulator (1080x2460): area scroll tiap layar mulai di **`[0,108]`**, termasuk Settings. Sebelum perbaikan, Settings mulai di `[0,0]` dan teks pertama berada di `y≈34`.

---

### 1.5 Riwayat Masalah Bottom Nav (Supaya Tidak Terulang)

| Percobaan | Hasil | Keputusan |
|---|---|---|
| Fill `Color.Transparent`, outline dihapus | User: outline jangan dihapus | Kembalikan outline |
| `Scaffold.containerColor = Color.Transparent` | Area belakang jadi window background putih, status bar kacau | Kembalikan `scheme.background` |
| `contentWindowInsets = WindowInsets(0)` | Konten nyatu dengan bar notifikasi HP | Ganti ke `WindowInsets.statusBars` |
| Fill transparan penuh | User: opacity terlalu tipis | `scheme.background.copy(alpha = 0.72f)` |
| Fill `#141414` solid | User: ada kotak hitam di belakang outline | Dihapus permanen |
| `SettingsScreen` di luar `Scaffold` | Konten Settings mulai `y≈34`, nyatu dengan status bar | Satu `Scaffold` untuk semua layar; bottom bar kondisional |

**Kesimpulan final:** outline ada, fill 0.72, tidak ada kotak hitam solid, konten lewat di belakang outline, status bar tetap rapi.

---

### 1.6 Cara Verifikasi Bottom Nav

Jangan percaya Apply Changes untuk perubahan `MainActivity`. Apply Changes sering memakai APK lama.

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
adb shell am force-stop com.blankpages.trackify
adb shell monkey -p com.blankpages.trackify -c android.intent.category.LAUNCHER 1
```

Cek visual:
- Area status bar berwarna tema (dark: `#0B0B0B`), bukan putih.
- Konten mulai di bawah status bar (sekitar y≈120 pada emulator 1080x2460).
- Tidak ada blok `#141414` solid di area bottom bar.
- Outline melengkung + notch tetap terlihat.
- Ikon orbit mengikuti tab yang dipilih dengan animasi spring.

Device yang pernah dipakai: `adb-106293736G000270-Avd2mU`.

---

### 1.7 Header / Judul Tab — Hanya Judul

Keputusan final user: **semua header tab hanya menampilkan judul. Subtext dilarang.**

Satu-satunya gaya judul resmi:

```kotlin
@Composable
fun ScreenTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
        color = MaterialTheme.colorScheme.onBackground
    )
}
```

Lokasi: `app/src/main/java/com/blankpages/trackify/ui/theme/TrackifyTheme.kt`.

| Layar | File | Judul |
|---|---|---|
| Home | `ui/home/HomeScreen.kt` | `Trackify` |
| Rekap | `ui/spreadsheet/SpreadsheetScreen.kt` | `Rekap` |
| Lokasi | `ui/locations/LocationsScreen.kt` | `Lokasi` |
| Catatan | `ui/notes/NotesScreen.kt` | `Catatan` |
| Settings | `ui/settings/SettingsScreen.kt` | `Setelan` |

**Aturan:**
- Semua header wajib memanggil `ScreenTitle(text = ...)`.
- **Jangan** mengembalikan `Text` judul inline dengan `fontSize`/`style` sendiri.
- **Jangan** menambahkan `Text` subtext di bawah judul, termasuk:
  - `Pencatatan dan gaji bulanan`
  - `N rumah tersimpan`
  - `Trackify 1.0`
- `MaterialTheme.typography.headlineSmall` tidak dipakai untuk header tab.
- Perubahan gaya judul dilakukan **hanya** di `ScreenTitle`, bukan per layar.

Verifikasi emulator 1080x2460 setelah perbaikan:
- `Trackify` → `[45,145][314,248]`
- `Rekap` → `[45,142][655,245]`
- `Lokasi` → `[45,153][644,256]`
- `Catatan` → `[51,153][780,256]`
- `Setelan` → `[187,145][454,248]`

Kelima judul punya tinggi **103 px**. String subtext di atas tidak ditemukan di UI dump.

---

## 2. ARSITEKTUR & MODUL

### 2.1 Modul Final

```
settings.gradle.kts
├── :app
├── :core:domain
└── :core:data
```

**Modul `feature:*` sengaja belum dibuat.** Alasan: bottom bar tetap di `MainActivity`, dan state UI masih dipakai bareng lintas tab. Jangan membuat modul feature hanya untuk "kerapian" tanpa keputusan baru.

### 2.2 Arah Dependensi

```
:app  ──>  :core:data  ──>  :core:domain
 : └──────────────────────>  :core:domain
```

Aturan:
- `:core:domain` **hanya** `room-common:2.7.1` untuk anotasi model. **Tidak boleh** bergantung ke `room-runtime`.
- `:core:data` pemilik Room runtime, DAO, database, migrasi, repository, lokasi, notifikasi, dan `AppModule`.
- `:core:data` **dilarang** mereferensikan `MainActivity`, `R` dari `:app`, atau kelas UI apa pun.
- `:app` bergantung langsung ke `:core:domain` dan `:core:data`.

### 2.3 Versi & Dependency Kunci

| Komponen | Versi tetap | Catatan |
|---|---|---|
| Aplikasi | `versionCode = 4` / `versionName = "1.2.0"` | Rilis minor TPG; amandemen kontrak 25 Nov 2026 |
| AGP | `9.5.2` | Di `libs.versions.toml` |
| Kotlin | `2.2.10` | Di `libs.versions.toml` |
| Compose BOM | `2026.02.01` | Di `libs.versions.toml` |
| Room | `2.7.1` | `core:data` runtime; `core:domain` annotations only |
| Hilt | **`2.60.1`** | Root + `app` + `core:data` |
| KSP | `2.3.13` | Plugin root |

**Penting:** Hilt `2.57.1` gagal dengan error `Android BaseExtension not found` pada AGP `9.5.2`. **Jangan downgrade.**

### 2.4 Hilt

- `TrackifyApp` → `@HiltAndroidApp`.
- `MainActivity` → `@AndroidEntryPoint`.
- `TrackifyViewModel` → `@HiltViewModel`.
- `AppModule` → `core/data/.../di/AppModule.kt`, `@InstallIn(SingletonComponent::class)`.
- `TrackifyRepository` disuntikkan ke ViewModel.

Jangan kembali ke `viewModels()` manual dengan DAO langsung.

---

## 3. DATA & DOMAIN

### 3.1 Database

- Nama: `trackify_offline_db`.
- Versi: **`8`** (disetujui 24 Nov 2026 untuk foto catatan; versi `7` sebelumnya untuk lampiran foto lokasi).
- Migrasi aktif: **`MIGRATION_5_6`**, **`MIGRATION_6_7`**, dan **`MIGRATION_7_8`**.
- `exportSchema = false`.
- Mode WAL aktif: `setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)`.
- Query log bulanan memakai rentang `date >= startDate AND date < endExclusive`, bukan `LIKE 'YYYY-MM%'`.
- `fallbackToDestructiveMigration(true)` tetap dipakai untuk ketahanan, tapi **bukan alasan untuk menaikkan versi sembarangan**.
- Index komposit baru untuk `courier_notes`/`note_photos` **tidak** ditambahkan di batch TPG; DB tetap v8.

`MIGRATION_5_6` menghapus kolom pembayaran tunai dari `daily_logs`. **Jangan menambahkan kolom itu lagi.**

`MIGRATION_6_7` menambah kolom `photoPath` (`TEXT NOT NULL DEFAULT ''`) ke `saved_locations`.
Foto hasil kamera masuk galeri lewat `MediaStore`, lalu salinan stabilnya disimpan di `filesDir/location_photos`
dan path-nya menjadi lampiran lokasi. Hapus lokasi ikut menghapus salinan tersebut.

`MIGRATION_7_8` membuat tabel `note_photos` (`id`, `noteId`, `path`, `sortOrder`) dengan FK `CASCADE` ke
`courier_notes.id` dan index pada `noteId`. Foto catatan disimpan terpisah di `filesDir/note_photos`
(awalan `note_`); hapus catatan ikut menghapus baris dan file. Tidak ada kolom baru di `courier_notes`.

### 3.2 Entity / Model

Empat entity:
- `DailyLog` → `daily_logs`
- `SavedLocation` → `saved_locations`
- `CourierNote` → `courier_notes`
- `NotePhoto` → `note_photos` (maks. 5 per catatan, dijaga di repository + UI)

Field `DailyLog` final:

```
date, isWorkingDay, packagesCarried, packagesDelivered,
ratePerPackage, bonus, expense, notes
```

Tidak ada field pembayaran tunai.

Field `SavedLocation` final:

```
id, customerName, landmarkNote, latitude, longitude, photoPath
```

`photoPath` boleh kosong dan tidak mengubah cara `Rute`/perhitungan jarak memakai `latitude`/`longitude`.

Sejak 24 Nov 2026, input editor Lokasi memakai **satu field `URL Maps`**, bukan dua field Latitude/Longitude.
`MapUrlParser` (`core:domain`) menarik koordinat dari `@lat,lon`, `q=`, `query=`, `destination=`, `ll=`,
`center=`, `!3d...!4d...`, `%2C`, dan pasangan `lat,lon` polos; nilai di luar rentang ditolak.
`MapLinkResolver` (`core:data`) mengikuti maksimal 5 redirect short link `maps.app.goo.gl` dengan
`instanceFollowRedirects = false` dan dipanggil dari korutina IO. Koordinat hasil parse tetap disimpan
di kolom lama, sehingga schema DB tidak berubah.

Sistem tag **`Langganan` / `Titip` / `Biasa` dihapus total**: tidak ada chip filter, chip kategori editor,
`inferTag()`, `tagCode`, atau penggabungan tag ke `landmarkNote`. Catatan lokasi murni teks bebas.

### 3.3 Aturan Domain

- `DailyLog.sanitized()` wajib:
  - membatasi `packagesDelivered` ≤ `packagesCarried`
  - mengosongkan paket & pendapatan saat `isWorkingDay == false`
- `toRupiah()` berada di `core/domain/.../CurrencyFormat.kt` dan memakai `Locale.forLanguageTag("id-ID")`.
- `Locale.forLanguageTag("id-ID")` juga dipakai di Home, Spreadsheet, Settings, dan Locations.
- Templat catatan "`Titipan`" dan "`Kendala Kirim`" ada di `core/domain/.../NoteTemplates.kt` + chip di `NotesScreen`.
- Sejak 24 Nov 2026, isi catatan tetap **satu field `content`**, tetapi dibaca/ditulis sebagai blok lewat
  `core/domain/.../NoteBlocks.kt`: `# ` = `HEADING`, `- ` = `BULLET`, `[ ]` / `[x]` = `TASK`
  (dengan `checked`), sisanya `PARAGRAPH`. Editor `NotesScreen` memakai chip **Teks/Judul/Daftar/Checklist**;
  checklist dicentang lewat `Checkbox` dan statusnya ikut tersimpan di `content`. Jangan menambah tabel,
  kolom DB, atau format JSON untuk blok — backup lama harus tetap terbaca.

**Bug historis yang jangan diulang:**
- `TrackifyApp` harus memanggil `previousHandler?.uncaughtException(...)` setelah logging.
- Jangan memutasi `overrideTodayLogFlow.value` dari dalam `combine` di `TrackifyViewModel`.

### 3.4 Target

- Source of truth target: `state.settings.targetAmount`.
- Home, Rekap, dan Settings memakai nilai itu langsung.
- `targetRupiah` di `SpreadsheetScreen` hanyalah **state lokal untuk sheet edit**, diinisialisasi dari `targetAmount` dan ditulis balik lewat `onUpdateTarget`. Jangan dijadikan source of truth global.

### 3.5 Fungsi Mati yang Sudah Dihapus

Jangan dikembalikan tanpa kebutuhan nyata:
- `applyAutoBonus`
- `computeTargetPace`
- `formatDailyWhatsappReport`
- `TargetPaceInfo`
- 8 preferensi lama yang tidak terpakai

---

## 4. FITUR YANG DIKUNCI

### 4.1 Fitur Pembayaran Tunai — DILARANG

Fitur ini sudah dihapus total dari:
- entity `DailyLog`
- `sanitized()`
- summary/engine
- UI
- template catatan
- dokumentasi aktif
- migrasi `MIGRATION_5_6`

Cek wajib:

```powershell
rg -n "bayar di tempat|pembayaran tunai|dibayar saat pengiriman" app/src core --glob "*.kt"
```

Hasil harus **kosong**.

> Arsip folder lama sudah dihapus. Satu-satunya cadangan adalah riwayat git dan tag `v1.0.0`.

### 4.2 Pengingat Harian

- Izin: `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`.
- Channel: `trackify_daily_reminder`.
- Jam: default `20.00`, kini milik user lewat `ReminderTime` dan prefs `reminder_hour` / `reminder_minute` (Fase 12). Alarm inexact via `setAndAllowWhileIdle`, dijadwalkan ulang setiap jam diubah.
- Receiver: `DailyReminderReceiver`, `BootReminderReceiver`.
- Karena `:core:data` tidak boleh bergantung ke `:app`:
  - launcher dibuka lewat `PackageManager.getLaunchIntentForPackage`
  - ikon notifikasi memakai `android.R.drawable.ic_dialog_info`
- Jangan mereferensikan `MainActivity` atau `R` dari `:app` di scheduler.

### 4.3 Lokasi Live

- Pakai `LocationManager` / `DeviceLocationSource`.
- **Jangan** pakai `FusedLocationProviderClient`.
- Wajib cek: izin, GPS aktif, akurasi, umur fix.
- Rationale izin permanen harus tetap ada.
- Izin `CAMERA` dipakai untuk **Ambil foto**; hasilnya masuk galeri lewat `MediaStore` dan disalin ke storage aplikasi.
  Baris MediaStore yang diserahkan ke kamera **tidak boleh** memakai `IS_PENDING = 1`: penanda itu mengunci tulis
  hanya untuk Trackify, sehingga kamera vendor crash saat menekan **done**. Kamera menulis JPEG lewat izin URI
  `TakePicture`; `finalizeGalleryImage()` tidak dipakai lagi.
- Izin `INTERNET` dipakai untuk dua hal: mengikuti redirect short link Google Maps dan GET publik
  `https://api.github.com/repos/drezvolx/Trackify/releases/latest` untuk pengecekan versi (Fase 18). Tidak ada data
  aplikasi yang dikirim, tidak ada auto-install, dan semua fitur inti tetap offline.

### 4.4 Backup / Reset

- Export backup memakai `repository.readBackupSettings()` untuk menyertakan tarif, bonus, target, dan flag pengingat. `themeMode` sengaja tidak diekspor karena tema selalu mengikuti sistem.
- Schema backup naik ke `8` (Fase 14). Backup schema `5`, `6`, dan `7` tetap bisa dibaca; `photoPath` memakai `""` jika tidak ada, kunci `photos` yang absen dibaca sebagai daftar kosong, dan bagian `settings` memakai default jika tidak ada. `decodeBackup` tidak menolak angka schema lama.
- Import backup memakai `repository.restoreSnapshot(decoded.value)` lalu `repository.restoreBackupSettings(decoded.value.settings)`.
- `restoreSnapshot` **mengganti penuh** isi tabel (clear dulu, baru isi), bukan menambah.
- Setelah restore, VM menyegarkan `overrideTodayLogFlow`, `defaultRateFlow`, `settingsFlow`, dan jadwal pengingat.
- Tombol reset di Settings wajib memanggil `vm.resetAllData { ... }` dari `MainActivity`, bukan hanya `Toast`.
- `DailyReminderReceiver` wajib membaca `AppPreferences.dailyReminderEnabled` sebelum menjadwalkan ulang.
- Jika izin notifikasi ditolak, Settings wajib memanggil `onToggleReminder(false)` agar prefs dan UI sinkron.

### 4.5 Tema Otomatis & Orientasi

Keputusan final user: **tema mengikuti perangkat**, dan **tidak ada menu pilihan tema**.

Aturan:

- `MainActivity` memanggil `TrackifyTheme { ... }` **tanpa argumen**.
- `TrackifyTheme` memakai default `isSystemInDarkTheme()` dari Compose dan meneruskannya lewat
  `LocalTrackifyDarkTheme`; `TrackifyGradients.background()` / `.hero()` membaca local itu (Fase 15).
- `enableEdgeToEdge()` dipanggil **satu kali**, tanpa argumen, sehingga `SystemBarStyle.auto` mengikuti night mode sistem.
- **Dilarang** mengembalikan:
  - menu **Tampilan** / toggle **Dark**–**White** di Settings;
  - parameter `isDarkTheme` / `onToggleTheme` di `SettingsScreen`;
  - `setDarkTheme()` di `TrackifyViewModel`;
  - `setThemeMode()` di `TrackifyRepository`;
  - pemakaian `state.settings.themeMode` untuk menentukan tema.
- `themeMode` di `AppSettings` / `TrackifySettings` / `AppPreferences` hanya **legacy untuk kompatibilitas backup**. Jangan dipakai untuk UI.
- Window background XML mengikuti sistem:
  - `res/values/themes.xml` → parent `android:Theme.Material.Light.NoActionBar`, background `#FFF1F1F1`.
  - `res/values-night/themes.xml` → parent `android:Theme.Material.NoActionBar`, background `#FF1E1F22`.
- Activity tidak perlu override `@style/Theme.Trackify`; cukup di level application.
- Rotasi otomatis dimatikan lewat `android:screenOrientation="portrait"` pada `MainActivity` di `AndroidManifest.xml`.
- `tools:ignore="LockedOrientationActivity"` dipakai agar lint tidak menganggap portrait sebagai masalah.

Verifikasi emulator (1080x2460):

| Mode sistem | Status bar | Background | Surface kartu |
|---|---|---|---|
| Light | `(245,245,245)` | `(245,245,245)` | `(255,255,255)` |
| Dark | `(11,11,11)` | `(11,11,11)` | `(20,20,20)` |

UI dump Settings: `Tampilan`, `Tema`, `Dark`, dan `White` tidak ditemukan. `Setelan` tetap ada.

`dumpsys activity activities` menunjukkan `requestedOrientation=SCREEN_ORIENTATION_PORTRAIT`.

### 4.6 Kartu & Tipografi

Kartu standar ada di `ui/theme/TrackifyComponents.kt` sebagai `TrackifyCard`.

Token final:

- bentuk `RoundedCornerShape(20.dp)`
- warna `scheme.surface`
- border `BorderStroke(1.dp, scheme.outlineVariant)`
- padding konten `16.dp`
- judul kartu `FontWeight.SemiBold`
- judul layar tetap `FontWeight.ExtraBold` lewat `ScreenTitle` (Fase 16; sebelumnya `Bold`)

`TrackifyCard` sekarang dipakai untuk:
- kartu kosong dan kartu catatan di `NotesScreen`;
- kartu filter GPS di `LocationsScreen`.

Kartu lain yang sudah mengikuti token yang sama (Home, Rekap, Settings, daftar Lokasi, kartu Nearby) tidak perlu diubah bentuknya. Untuk kartu baru, wajib pakai `TrackifyCard`.

Jangan memakai:
- `outlineVariant.copy(alpha = 0.55f/0.6f)` untuk border kartu;
- `RoundedCornerShape(8.dp/11.dp/14.dp/16.dp)` untuk kartu utama;
- `FontWeight.Bold` untuk judul kartu.

### 4.7 Dialog

Semua dialog wajib memakai `TrackifyDialog` dari `ui/theme/TrackifyComponents.kt`.

Token final (sama dengan `TrackifyCard`):

- bentuk `RoundedCornerShape(20.dp)`
- `containerColor = scheme.surface`
- `titleContentColor = scheme.onSurface`
- `textContentColor = scheme.onSurfaceVariant`
- `tonalElevation = 0.dp`
- judul dialog `FontWeight.SemiBold`

Alasan: default `AlertDialog` Material3 memakai `surfaceContainer`, sehingga warnanya beda dengan kartu `scheme.surface` di layar. User mengeluhkan hal ini pada dialog `+` di Lokasi dan Catatan.

`TrackifyDialog` sekarang dipakai untuk:
- dialog editor Lokasi (baru/edit), detail Lokasi, dan konfirmasi hapus Lokasi;
- dialog editor Catatan (baru/edit) dan konfirmasi hapus Catatan.

Dilarang memakai `AlertDialog(` langsung di file UI. Jika butuh dialog baru, tambahkan lewat `TrackifyDialog`. `DialogProperties` diimpor dari `androidx.compose.ui.window.DialogProperties` (bukan `material3`).

Pengecualian: `DatePickerDialog` dari Material3 tetap memakai komponen bawaan karena API warnanya tidak stabil di Compose BOM `2026.02.01`. Ini bukan dialog buatan app dan tidak dipakai di tombol `+`.

Catatan token: `AlertDialog` tidak menerima border, jadi `TrackifyDialog` memakai **warna dan shape** `TrackifyCard`, bukan border `outlineVariant`-nya.

### 4.8 Flag simpan Lokasi & hasil uji perangkat

Flag `isEdit` yang dikirim dari `LocationsScreen` ke `vm.saveLocation(location, isEdit)` wajib:

```kotlin
onSaveLocation(saved, editingTarget != null)
```

Jangan pakai `editingTarget == null`. Bug ini pernah membuat lokasi **baru** memanggil `dao.updateLocation()` (0 baris) sehingga tidak pernah masuk DB. Lokasi **existing** harus tetap memanggil `updateLocation`; lokasi baru memanggil `insertLocation`.

Uji langsung di emulator `adb-106293736G000270-Avd2mU` (1080x2460), 23 Nov 2026:

| Alur | Hasil |
|---|---|
| Dialog `+` Lokasi & Catatan, light | container `#FFFFFF` = `scheme.surface` light, background `#F5F5F5` |
| Dialog `+` Lokasi & Catatan, dark | container `#141414` = `scheme.surface` dark, background `#0B0B0B` |
| Tambah Catatan `TESTNOTE123` | masuk `courier_notes.id=2`, tetap ada setelah force-stop |
| Edit Catatan | via `Edit Catatan` → `Simpan` |
| Hapus Catatan | icon `Hapus` di editor, row hilang dari UI dan DB |
| Tambah Lokasi `TESTLOKASI7` | masuk `saved_locations.id=2` setelah perbaikan flag, persist setelah restart |
| Edit Lokasi | nama berubah jadi `TESTLOKASI7EDIT` di UI dan DB (`id=2`) |
| Hapus Lokasi | lewat `Detail` → `Hapus`, UI dan DB kembali 1 lokasi |
| Quick action `+1 Pkt` | tidak mengubah angka karena log hari ini sudah `54/54`; sesuai batas `packagesDelivered <= packagesCarried` |
| Backup | `/sdcard/Download/Trackify_Backup.json`, schema `6`, 9 log / 1 lokasi / 1 catatan + prefs (rate 1800, bonus 0, target 4.000.000, reminder false) |
| Reset | `Hapus permanen` mengosongkan semua tabel dan `shared_prefs`; UI balik ke default |
| Restore | DB kembali 9/1/1 dan prefs kembali 1800/0/4.000.000/false |
| Restore full-replace | catatan `STALENOTE` yang ditambahkan setelah backup hilang setelah restore |

Catatan uji: saat keyboard terbuka, tombol dialog bergeser ke atas (mis. `Simpan lokasi` dari `y≈1748` ke `y≈1431`). Selalu `uiautomator dump` terbaru sebelum tap. Row **Hapus** di Settings juga perlu di-scroll ke atas jika berada dekat navigation bar.

---

### 4.9 Sumber warna tunggal & header Catatan

Keputusan final user 24 Nov 2026: **tema harus konsisten dan tidak boleh ada warna lain selain tema,
baik di UI lama maupun UI baru.** Ini kontrak permanen, bukan preferensi sementara.

**Sumber warna:**

- `app/src/main/res/values/colors.xml` adalah **satu-satunya** definisi warna aplikasi, termasuk drawable
  launcher. Semua token wajib monokrom netral (charcoal, abu, putih, alpha-nya). Hijau Android `#3DDC84`
  sudah dihapus dan launcher memakai token charcoal.
- `app/src/main/java/com/blankpages/trackify/ui/theme/TrackifyColors.kt` adalah satu-satunya jembatan
  Compose ke token tersebut. File Kotlin UI dilarang memanggil `Color(0x…)`, `Color.White`, `Color.Black`,
  `Color.Red`, `Color.Green`, `Color.Blue`, `Color.Yellow`, `Color.Gray`, `Color.Transparent`,
  `Color.Unspecified`, atau `colorResource(...)` secara langsung.
- `TrackifyMockupPalette`, `MockupDarkColorScheme`, dan `MockupLightColorScheme` tidak lagi dipakai.
  `mockupDarkColorScheme()` / `mockupLightColorScheme()` bersifat `@Composable` dan mengisi **semua** slot
  `ColorScheme` dengan token supaya default Material3 (error merah, tertiary ungu, scrim hitam) tidak bocor.
- `TrackifyGradients` tidak lagi menyimpan hex; gradien latar/hero serta `HeroInk`/`HeroMute`/`HeroTrack`/
  `HeroLine`/`HeroChipInk` membaca `TrackifyColors`. Stop gradien Fase 15 tidak berubah nilainya.
- Turunan transparan diperbolehkan **hanya** dari warna bertema, misalnya
  `scheme.background.copy(alpha = 0.72f)` pada bottom nav (sudah dikunci di §1.2). Dilarang memakai
  `Color.Transparent` mentah di helper tombol, Home, atau Rekap; memakai `TrackifyColors.Transparent`.
- `TrackifyIcons` tidak menuliskan literal warna: path memakai mask opaque `TrackifyColors.IconMask` dari
  facade. RGB mask selalu ditimpa `tint`, tetapi alpha 1 wajib ada supaya `ColorFilter.tint` punya area
  untuk diwarnai. Tanpa mask ini semua ikon jadi transparan (path `Color.Unspecified` alpha 0).
- Setiap pemanggilan `Icon(...)` di UI wajib menyebut `tint` dengan token skema/hero yang sesuai latarnya.
  `TrackifyButton`/`TrackifyTextButton`/`TrackifyIconButton` juga menyediakan `LocalContentColor` sebagai
  pengaman, tetapi ikon tanpa `tint` eksplisit pernah hilang dan dilarang dipakai sebagai satu-satunya jalan.
- CI `validate` menjalankan **Theme color guard**: grep literal warna di Kotlin di luar facade, grep hex di
  XML di luar `colors.xml`, dan pemeriksaan spread RGB tiap token. Gate lokal wajib sama hijau sebelum commit.

**Header Catatan (final):**

- Judul tetap `ScreenTitle(text = "Catatan")` tanpa subtext.
- Di sebelah kanan judul, urutannya: tombol ikon bulat **Pilih** (`TrackifyIcons.Check`) / **Batal**
  (`TrackifyIcons.Close`), jarak `8.dp`, lalu tombol **Tambah**. Keduanya tinggi `36.dp`; ikon `18.dp`;
  warna `scheme.onSurface`. `contentDescription` tetap "Pilih catatan" / "Batal pilih".
- Teks **Pilih** / **Batal** di header dilarang (sumber ketidakrapian yang dilaporkan user).
- Long-press kartu catatan masuk mode seleksi dan langsung memilih catatan itu; long-press berikutnya
  menoggle. Checkbox di kartu tetap jalur tap saat mode seleksi aktif.
- Bar aksi seleksi memakai `TrackifyCard`: label `n dipilih`, tombol **Selesai**, dan tombol **Hapus**
  dengan `contentColor = scheme.error`. Hapus massal tetap lewat `TrackifyDialog` konfirmasi.
- `TrackifyCard` menerima `onLongClick` opsional lewat `combinedClickable`; tampilan kartu tidak berubah.

---

## 5. FILE KRITIS

| File | Kenapa krit |
|---|---|
| `app/src/main/java/com/blankpages/trackify/MainActivity.kt` | Bottom nav final, Scaffold insets, status bar |
| `app/src/main/java/com/blankpages/trackify/navigation/TrackifyNavigation.kt` | `TrackifyTab` + urutan tab |
| `app/src/main/java/com/blankpages/trackify/ui/theme/TrackifyTheme.kt` | `ScreenTitle`: satu-satunya gaya header tab; tema sistem otomatis |
| `app/src/main/java/com/blankpages/trackify/ui/theme/TrackifyColors.kt` | Facade Compose: satu-satunya pembaca token warna di Kotlin |
| `app/src/main/java/com/blankpages/trackify/ui/theme/TrackifyComponents.kt` | `TrackifyCard` + `TrackifyDialog`: token kartu & dialog tunggal, `onLongClick` opsional |
| `app/src/main/res/values/colors.xml` | Sumber warna tunggal; semua token monokrom netral |
| `app/src/main/res/values/themes.xml` | Window background light |
| `app/src/main/res/values-night/themes.xml` | Window background dark |
| `app/src/main/java/com/blankpages/trackify/ui/locations/LocationsScreen.kt` | Padding bawah 96.dp + kartu Lokasi |
| `app/src/main/java/com/blankpages/trackify/ui/notes/NotesScreen.kt` | Padding bawah 96.dp |
| `app/src/main/java/com/blankpages/trackify/ui/home/HomeScreen.kt` | Target + padding bawah |
| `app/src/main/java/com/blankpages/trackify/ui/spreadsheet/SpreadsheetScreen.kt` | Target local draft + padding bawah |
| `app/src/main/java/com/blankpages/trackify/ui/settings/SettingsScreen.kt` | Target + reset/backup + padding bawah |
| `app/src/main/java/com/blankpages/trackify/ui/TrackifyViewModel.kt` | State UI bersama + Hilt VM |
| `app/src/main/java/com/blankpages/trackify/TrackifyApp.kt` | `@HiltAndroidApp` + crash handler |
| `app/src/main/AndroidManifest.xml` | Izin, receiver, dan orientasi portrait |
| `app/build.gradle.kts` | Hilt 2.60.1 + modul internal |
| `build.gradle.kts` | Plugin Hilt/KSP |
| `settings.gradle.kts` | Daftar modul |
| `gradle/libs.versions.toml` | Versi AGP/Kotlin/Compose |
| `core/domain/src/main/java/.../Models.kt` | Entity tanpa field pembayaran tunai |
| `core/domain/src/main/java/.../TrackifyEngine.kt` | Logika murni + backup CSV/JSON |
| `core/domain/src/main/java/.../CurrencyFormat.kt` | `toRupiah()` |
| `core/data/src/main/java/.../TrackifyDatabase.kt` | DAO, DB v6, `MIGRATION_5_6` |
| `core/data/src/main/java/.../TrackifyRepository.kt` | Repository + `restoreSnapshot` |
| `core/data/src/main/java/.../di/AppModule.kt` | Hilt providers |
| `app/src/test/java/com/blankpages/trackify/TrackifyEngineTest.kt` | 10 tes yang harus lulus |

---

## 6. CHECKLIST SEBELUM & SESUDAH EDIT

### Sebelum edit
- [ ] Baca file aktual dulu. Sering terjadi reset eksternal pada `MainActivity.kt`, `LocationsScreen.kt`, dan `NotesScreen.kt`.
- [ ] Pastikan tidak menyentuh konfigurasi bottom nav di §1.
- [ ] Pastikan semua header memakai `ScreenTitle` dan tidak ada subtext (§1.7).
- [ ] Pastikan tidak ada menu tema manual dan orientasi tetap portrait (§4.5).
- [ ] Kartu baru wajib memakai `TrackifyCard` (§4.6).
- [ ] Dialog baru wajib memakai `TrackifyDialog`, tidak `AlertDialog` langsung (§4.7).
- [ ] Pastikan flag simpan Lokasi memakai `editingTarget != null` (§4.8).
- [ ] Buat backup jika perubahan menyentuh source/build files.

### Sesudah edit
- [ ] Jalankan verifikasi di §7.
- [ ] Pastikan grep fitur pembayaran tunai kosong.
- [ ] Pastikan bottom nav masih sesuai §1.
- [ ] Pastikan judul seragam dan subtext tidak kembali (§1.7).
- [ ] Pastikan tema berubah saat `cmd uimode night yes/no` dan Settings tanpa `Tampilan` (§4.5).
- [ ] Pastikan `requestedOrientation=SCREEN_ORIENTATION_PORTRAIT` (§4.5).
- [ ] Update dokumen ini jika ada keputusan baru yang disetujui.

---

## 7. PERINTAH VERIFIKASI WAJIB

```powershell
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain
```

Expected:
- `BUILD SUCCESSFUL`
- **10/10 tes lulus**
- 0 warning compiler

Cek fitur pembayaran tunai:

```powershell
rg -n "bayar di tempat|pembayaran tunai|dibayar saat pengiriman" app/src core --glob "*.kt"
```

Expected: tidak ada output.

Cek subtext header:

```powershell
rg -n "Pencatatan dan gaji bulanan|rumah tersimpan|Trackify 1\\.0" app/src --glob "*.kt"
```

Expected: tidak ada output.

Cek tema manual & rotasi:

```powershell
rg -n "Tampilan|onToggleTheme|isDarkTheme|setDarkTheme|setThemeMode|state\.settings\.themeMode" app/src --glob "*.kt"
adb shell dumpsys activity activities | Select-String "requestedOrientation=SCREEN_ORIENTATION_PORTRAIT"
```

Expected: grep kosong, orientation portrait.

Cek tema otomatis:

```powershell
adb shell cmd uimode night no
adb shell am force-stop com.blankpages.trackify
adb shell monkey -p com.blankpages.trackify -c android.intent.category.LAUNCHER 1
adb shell cmd uimode night yes
adb shell am force-stop com.blankpages.trackify
adb shell monkey -p com.blankpages.trackify -c android.intent.category.LAUNCHER 1
```

Expected (Fase 15): light background `#FAFAFA → #F1F1F1 → #E8E8E8`, hero `#3A3A3C → #232324 → #161617`;
dark background `#25262A → #1E1F22 → #17181A`, hero `#4C4E51 → #2C2D31 → #1E2023`. Tema mengikuti perangkat.

Cek sumber warna (contract permanen, §4.9):

```powershell
# CI `validate` menjalankan Theme color guard:
# 1. grep Color(0x...)/Color.X/colorResource(...) di *.kt/*.kts di luar TrackifyColors.kt
# 2. grep #hex di *.xml di luar res/values/colors.xml
# 3. tiap token colors.xml wajib 8 digit AARRGGBB dengan spread RGB <= 6
```

Expected: ketiga pemeriksaan bersih dan langkah `Theme color guard` hijau di CI.

Install & cek visual:

```powershell
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
adb shell am force-stop com.blankpages.trackify
adb shell monkey -p com.blankpages.trackify -c android.intent.category.LAUNCHER 1
```

---

## 8. BACKUP & ROLLBACK

### 8.1 Cadangan Resmi

Semua folder arsip lokal sudah dihapus. Cadangan resmi adalah riwayat git di `main` dan tag `v1.0.0`.

Untuk kembali ke titik rilis:

```powershell
git checkout v1.0.0
```

Untuk melihat perubahan antar titik, pakai `git log --oneline` dan `git diff`.

### 8.2 Aturan Rollback

- Jangan memakai salinan folder lama sebagai source; struktur modul dan field sudah berubah sejak kontrak dibuat.
- Rollback penuh ke titik sebelum rilis bisa mengembalikan fitur yang sudah dilarang di §4.1.
- Setelah rollback, jalankan ulang seluruh verifikasi di §7 sebelum melanjutkan.

### 8.3 Prosedur Rollback Aman

1. Stop Gradle / tutup IDE sync.
2. Salin folder backup yang dimaksud ke lokasi sementara.
3. Bandingkan file yang akan ditimpa.
4. Timpa hanya file yang memang perlu dikembalikan.
5. Jalankan ulang §7.
6. Verifikasi visual bottom nav di emulator/HP.

---

## 9. PERUBAHAN KONTRAK

Kalau user menyetujui perubahan desain/arsitektur baru:

1. Update kode.
2. Commit perubahan ke `main` sebelum melanjutkan; riwayat git menggantikan folder backup.
3. Update dokumen ini pada bagian yang relevan.
4. Tambahkan catatan di §1.5 atau §9 berisi alasan perubahan.
5. Jalankan ulang §7.

**Tanpa langkah di atas, perubahan dianggap tidak sah.**

### 23 Nov 2026 — Fase 6: lampiran foto lokasi

User menyetujui perluasan DB ke versi `7`, kolom `photoPath`, izin `CAMERA`, dan `INTERNET`
(untuk short link Maps di fase berikutnya). Alasan: lokasi perlu bukti foto yang bisa diambil langsung
dari kamera maupun dipilih dari galeri, dan tetap tersimpan per lokasi. Bottom navigation, header,
tema, orientasi, dan daftar tab tidak berubah.

### 24 Nov 2026 — Fase 7: URL Maps & hapus sistem tag

User meminta field Latitude/Longitude diganti dengan satu field URL Maps (termasuk short link) dan
sistem tag `Langganan`/`Titip`/`Biasa` dihapus karena dianggap tidak penting. Keputusan diambil lewat
persetujuan eksplisit; koordinat hasil parse tetap memakai kolom lama, jadi tidak ada migrasi DB baru.

### 24 Nov 2026 — Fase 8: editor blok catatan

User memilih opsi "blok teks + checklist" untuk catatan ala Notion lite. Ruang lingkup yang disetujui:
empat jenis blok (**Teks**, **Judul**, **Daftar**, **Checklist**), isi tetap satu field `content` dengan
prefiks teks, tanpa schema DB baru, tabel, atau format khusus. Alasan: catatan perlu struktur ringan dan
checklist yang bisa dicentang, tetapi backup/ekspor lama harus tetap kompatibel. Kontrak UI lain
(bottom nav, `ScreenTitle`, tema otomatis, `TrackifyCard`/`TrackifyDialog`) tidak berubah.

### 24 Nov 2026 — Fase 9: input paket pakai teks

User meminta jumlah paket **Dibawa** dan **Dikirim** bisa diketik, bukan hanya ditekan berulang lewat
`-1/+1/+5/+10`. Keputusan: tombol lama tetap dipertahankan sebagai pelengkap; field teks memakai keyboard
numerik dan tombol **Simpan paket**. Aturan domain tidak berubah — `DailyLog.sanitized()` tetap membatasi
`packagesDelivered ≤ packagesCarried`, termasuk saat user mengetik angka lebih besar dari jumlah dibawa.

### 24 Nov 2026 — Fase 10: performa otomatis, tanpa toggle

Profil perangkat ditentukan otomatis dari `isLowRamDevice`, `Runtime.maxMemory()`, dan API level, lalu
dihitung sekali dan di-cache. Tidak ada pengaturan manual dan tidak ada perubahan aturan domain. Foto baru
di-downsample sesuai tier sebelum disimpan; preview memakai batas lebih kecil. GPS kontinu memakai interval
dan jarak minimum dari config, sedangkan `requestFresh()` sengaja tetap segera agar tombol refresh responsif.
Keputusan ini juga menyiapkan batas jumlah preview yang akan dipakai foto catatan di Fase 14.

### 24 Nov 2026 — Fase 11: mode gaji & periode milik user

User meminta cara hitung gaji dan periode tampilan bisa diatur, bukan hardcode. Keputusan:

- Default tetap **Tarif per paket** (`WageMode.PER_PACKAGE`) dan **Bulanan** (`WagePeriod.MONTHLY`),
  tanggal gajian `25`, nominal tetap harian `Rp2.500`. Data lama otomatis mempertahankan perilaku lama.
- Mode **Nominal tetap per hari** (`FLAT_DAILY`) menghitung `nominal + bonus − pengeluaran` untuk hari kerja,
  tanpa mengubah `DailyLog`, `sanitized()`, atau schema DB. Hari libur tetap `Rp0`.
- Periode hanya mengubah **label, agregat, dan pembagian target** di Home/Rekap: harian `target/30`,
  mingguan `target/4`, bulanan `target` penuh. Tanggal gajian hanya dipakai sebagai label di mode Bulanan,
  bukan penjadwalan notifikasi baru.
- Backup JSON schema `7` tetap, tetapi bagian `settings` kini berisi `wageMode`, `flatDailyAmount`,
  `wagePeriod`, dan `paydayDate`. Backup lama tanpa kunci itu dibaca dengan default di atas.
- Tidak ada kolom DB baru, tidak ada migrasi, dan tidak ada perubahan pada bottom nav, header, tema,
  atau token UI lain.

### 24 Nov 2026 — Fase 12: jam pengingat milik user

User meminta jam notifikasi tidak hardcode `20.00`. Keputusan:

- `ReminderTime` di `:core:domain` menjadi satu-satunya representasi jam: `hour 0..23`, `minute 0..59`,
  `label` berformat `HH.mm`. `DEFAULT = 20.00` dipakai saat tidak ada nilai tersimpan.
- Jam disimpan di `SharedPreferences` sebagai `reminder_hour` dan `reminder_minute`, bukan kolom DB.
  Tidak ada migrasi schema.
- Setelan memakai `TimePicker` 24 jam di dalam `TrackifyDialog` dengan tombol **Batal** / **Simpan**.
  Label `Notifikasi pukul …` mengikuti nilai terpilih. Tidak ada toggle tema dan tidak ada notifikasi baru.
- `DailyReminderScheduler` membaca prefs setiap kali dipanggil, jadi perubahan jam langsung menjadwalkan
  ulang alarm `DailyReminderReceiver` yang sama. Izin tetap `POST_NOTIFICATIONS` + `RECEIVE_BOOT_COMPLETED`.
- Backup JSON schema masih `7`, kini menyimpan `reminderHour` / `reminderMinute`; backup lama tanpa kunci
  itu dibaca dengan default `20.00`.
- Pengaturan lain tidak berubah: bottom nav, header `ScreenTitle`, gradien monokrom, orientasi portrait,
  dan token `TrackifyCard`/`TrackifyDialog`.

### 24 Nov 2026 — Fase 13: lokasi murni, berbagi foto + teks

User meminta tab Lokasi fokus ke lokasi. Keputusan:

- **+1 Pkt** dan **Catat** dihapus total dari kartu Nearby, termasuk callback `onQuickDelivered` /
  `onCreateLinkedNote`, helper `createLinkedNote`, dan VM `incrementTodayDelivered`. `upsertNote` tetap
  dipakai tab Catatan.
- Tombol **Bagikan** tersedia di kartu dan detail. Memakai `Intent.ACTION_SEND`, `EXTRA_TEXT`, dan
  `EXTRA_STREAM` bila foto ada. Foto diambil dari `filesDir/location_photos` dan dibagikan lewat
  `FileProvider` authority `com.blankpages.trackify.fileprovider` (`file_paths.xml`) dengan
  `FLAG_GRANT_READ_URI_PERMISSION`. Tanpa foto, berbagi tetap jalan dengan teks saja.
- Teks final (diformat di `:core:domain` `LocationShareText`, bisa diuji tanpa Android):
  `Nama Penerima : …` / `Detail/Patokan : …` / `Maps : …` / `by Trackify.` — tepat 4 baris, tanpa header `FOTO`,
  garis pemisah, atau baris kosong. Keputusan 24 Nov 2026 menggantikan format 7 baris Fase 13.
- Field `SavedLocation` final tetap `id, customerName, landmarkNote, latitude, longitude, photoPath`.
  Tidak ada migrasi DB, tidak ada izin baru, tidak ada perubahan bottom nav/header/tema/token UI.

### 24 Nov 2026 — Fase 14: template opsional, multi-foto catatan, DB v8

User meminta templat tidak memaksa dan catatan bisa membawa beberapa foto. Keputusan:

- Templat Notion **opsional**. Chip **Kosong** menggantikan "Reset" dan langsung membuka editor blank;
  `Titipan` / `Kendala Kirim` tetap tersedia. Tidak ada templat baru yang dipaksakan.
- Isi catatan tetap **satu field `content`** dengan `NoteBlocks`; tidak ada tabel/kolom untuk blok.
  Foto memakai entity terpisah `NotePhoto` → tabel `note_photos` dan `MIGRATION_7_8` (DB versi `8`).
- Maksimal **5 foto per catatan**. Batas dijaga di `MAX_PHOTOS_PER_NOTE` (repository) dan UI; path unik,
  diurutkan lewat `sortOrder`. `upsertNoteWithPhotos` menimpa daftar foto saat menyimpan ulang.
- Foto kamera tetap lewat `MediaStore`, salinan stabilnya untuk catatan masuk `filesDir/note_photos`
  dengan awalan `note_` (lokasi tetap `location_photos`/`loc_`). `file_paths.xml` menyediakan keduanya.
  Menghapus catatan menghapus baris `note_photos` **dan** file salinannya.
- Backup schema `8`: setiap objek `courier_notes` punya kunci `photos` berisi array path (maks. 5).
  `decodeBackup` memakai `optJSONArray("photos")`, jadi backup schema `7` dan lama tetap terbaca tanpa
  kunci tersebut. `restoreSnapshot` mengosongkan tabel foto sebelum mengisi ulang.
- `TrackifyUiState.notePhotos` adalah satu-satunya sumber path foto untuk UI. `onSaveNote` kini menerima
  3 argumen `(CourierNote, Boolean, List<String>)`; callback lama 2 argumen dilarang.
- Tidak ada perubahan pada bottom nav, `ScreenTitle`, tema otomatis, orientasi portrait, `DailyLog`,
  `SavedLocation`, atau token `TrackifyCard`/`TrackifyDialog`. Izin tidak bertambah.

### 24 Nov 2026 — Fase 15: gradien Royal Charcoal / Pure Charcoal

User menyetujui pergantian palet solid menjadi gradien monokrom. Keputusan:

- `TrackifyGradients.kt` adalah **satu-satunya** sumber gradien (nilainya kini dibaca dari token `colors.xml`
  lewat `TrackifyColors`, lihat §4.9):
  - dark latar `#25262A → #1E1F22 → #17181A`; dark hero `#4C4E51 → #2C2D31 → #1E2023`;
  - light latar `#FAFAFA → #F1F1F1 → #E8E8E8`; light hero `#3A3A3C → #232324 → #161617`.
- `TrackifyTheme` tetap dipanggil tanpa argumen dan memakai `isSystemInDarkTheme()`. Nilainya diteruskan
  lewat `LocalTrackifyDarkTheme` (CompositionLocal) ke `TrackifyGradients`. **Dilarang** menyimpan preferensi
  tema, menambah toggle, atau membaca `themeMode` untuk UI.
- Gradien latar dipasang di root `LazyColumn` kelima layar: Home, `SpreadsheetScreen` (Rekap), Lokasi,
  Catatan, Setelan. `Scaffold.containerColor` tetap `scheme.background`; satu `Scaffold` hanya di `MainActivity`.
- Hero "Terkirim hari ini" di Home memakai gradien hero dan teks `HeroInk` / `HeroMute` (putih) agar kontras
  di kedua tema. Ini satu-satunya kartu yang boleh keluar dari token `scheme.surface`; kartu lain tetap
  `scheme.surface` + border `outlineVariant`.
- `res/values/colors.xml` dan window background XML diselaraskan ke stop tengah gradien;
  `TrackifyMockupPalette` sudah digantikan `TrackifyColors` (§4.9). `ScreenTitle`, bottom nav, orientasi
  portrait, dan token dialog tidak berubah.

### 24 Nov 2026 — Fase 16: typography

User menyetujui judul lebih tebal, angka tabular, dan koordinat monospaced. Keputusan:

- `ScreenTitle` satu-satunya gaya header tab: 28.sp / 32.sp / `letterSpacing -0.6.sp` / tanpa subtext,
  kini `FontWeight.ExtraBold`. Tidak boleh ada `Text` judul inline di layar. `FontWeight.ExtraBold` di-mapping
  ke aset `geist_bold` yang sama dengan `Bold` — **tidak ada aset font baru**.
- `tnum` sudah diaktifkan global oleh `TextStyle.geist()` pada semua slot `MockupTypography`. Token
  `TabularFigures` dipakai eksplisit di angka besar Home sebagai penanda niat; tidak mengubah angka lain.
- Koordinat Lokasi (GPS live, baris daftar, dialog detail, kartu terdekat) memakai `FontFamily.Monospace`.
  Teks campuran (patokan atau koordinat) di Home sengaja tidak diubah.
- Judul blok `HEADING` Catatan memakai `ExtraBold` di pratinjau kartu dan editor; paragraf tetap `Normal`.
  Penanda `#` ikut `ExtraBold`.
- Judul kartu tetap `SemiBold`, judul dialog tetap `titleMedium`. Spacing, border, dan bentuk tombol bukan bagian
  Fase 16 (Fase 17). Bottom nav, gradien, tema otomatis, dan orientasi portrait tidak berubah.

### 24 Nov 2026 — Fase 17: tombol outline & spacing

User menyetujui seragamnya tombol dan spacing. Keputusan:

- `ui/theme/TrackifyButtons.kt` adalah sumber token tombol: `TrackifyButton`, `TrackifyTextButton`, dan
  `TrackifyIconButton`. Semua memakai `BorderStroke(1.dp, scheme.outlineVariant)`, container transparan,
  dan elevation/shadow `0`. Aksi destruktif memakai `contentColor = scheme.error`, **bukan** fill merah.
- Pemakaian langsung `Button`, `OutlinedButton`, `TextButton`, `IconButton`, dan `OutlinedIconButton` di lima
  layar **dilarang** (target grep `DIRECT_BUTTONS=0`). Tombol aksi berbentuk `Box` wajib punya border
  `1.dp outlineVariant`; di hero Home memakai `hero.outlineVariant`. Dilarang `outlineVariant.copy(alpha = ...)`.
- Skala spacing hanya **4/8/12/16/24 dp** untuk `padding`, `spacedBy`, dan `PaddingValues`. Pengecualian tetap:
  `96.dp` bottom clearance (kontrak bottom nav), `1.dp` border/divider, ukuran ikon, dan `0.dp`.
- Tidak ada nilai `box-shadow` CSS yang terdokumentasi di repo, sehingga terjemahan CSS → elevation Compose
  ditetapkan **elevation `0.dp`** (flat), termasuk `tonalElevation = 0.dp` pada `TrackifyDialog`.
  Jangan menambah `shadow(...)` tanpa keputusan baru.
- Catatan Fase 6 ("tombol aksi cepat dan toggle **Masuk/Libur** tidak diubah bentuknya") kini dibaca sebagai
  **label, aksi, dan state** yang tidak berubah; styling outline diatur Fase 17. `TrackifyCard`,
  `TrackifyDialog`, `Scaffold` tunggal, `ScreenTitle`, gradien, dan bottom nav tidak berubah.

### 24 Nov 2026 — Fase 18: pengecek rilis, kredit, dan dokumentasi rilis

User menyetujui penambahan pengecek pembaruan dan kredit. Keputusan:

- Bel memakai `TrackifyIcons.Bell` di header Home, ditemani tombol Setelan. Bukan `SmallTopAppBar`, bukan judul
  baru, dan `ScreenTitle` tetap satu-satunya gaya header tanpa subtext.
- Satu tekan bel = satu GET ke `https://api.github.com/repos/drezvolx/Trackify/releases/latest`. Implementasi
  `ReleaseUpdateChecker` memakai `HttpURLConnection` dengan timeout 8 detik dan `runCatching`, bukan library jaringan
  baru. Parsing JSON dan perbandingan versi murni JVM (`ReleaseInfoParser`, `ReleaseVersion`) dan diuji di
  `app/src/test`.
- Dialog wajib `TrackifyDialog`. Isinya versi terpasang (dari `PackageManager`, karena BuildConfig tidak aktif),
  versi rilis, dan aksi **Buka GitHub**; **Unduh APK** hanya muncul bila aset `.apk` tersedia. **Tutup** selalu ada.
  Dilarang mengunduh otomatis, memasang otomatis, atau mengubah data aplikasi saat mengecek rilis.
- Blok Dukungan di Setelan menampilkan versi aplikasi, **Drezvolx**, <https://github.com/drezvolx>, dan
  <https://discord.com/users/1182612195419369562>. QRIS dan fungsi backup tidak diubah.
- Kontrak §4.3 diamandemen: `INTERNET` kini juga dipakai untuk GET rilis publik di atas. Fitur inti tetap offline.
- Dokumentasi baru: `docs/credits.md`, `docs/management/README.md`, `docs/management/processes/*.md`,
  `docs/management/templates/release-notes.md`, dan README profesional. Tag `v1.0.0` adalah baseline dan tidak
  boleh diubah saat rilis berikutnya diterbitkan.

### 24 Nov 2026 — Ikon Coolicons round & gaya kode idiomatic

User meminta seluruh ikon aplikasi memakai Coolicons bergaya round dari file Figma yang ditunjuk, tanpa varian
sudut runcing/tajam, dan menetapkan gaya penulisan kode idiomatic sebagai aturan tetap. Keputusan:

- Sumber ikon adalah Coolicons resmi: Figma Community `800815864899415771`. Karena file Figma tidak bisa dibuka
  langsung oleh alat, SVG diambil dari repo resmi publisher `krystonschwarze/coolicons` (commit `ce57bcda`),
  gaya `fill`, sudut `round`. `ui/theme/TrackifyIcons.kt` adalah satu-satunya definisi `ImageVector` aplikasi;
  path SVG disimpan sebagai `pathData` dan diparse memakai `PathParser` Compose.
- Dilarang memakai gaya selain `fill`, sudut runcing/tajam, ikon Material bawaan, `painterResource` untuk ikon,
  atau menambah aset SVG/drawable ikon baru di luar `TrackifyIcons.kt`. Nama properti lama dipertahankan sebagai
  API internal;
  perubahan bentuk ikon tidak mengubah label, aksi, atau kontrak navigasi.
- `PinFilled` memakai `pin`, `PinOutlined` memakai `pin-off` supaya status sematan tetap terbedakan. Alias
  `Settings`, `AddLocation`, `Navigation`, `Add`, `PinFilled`, dan `TemplateKendala` tetap mengarah ke sumber
  Coolicons yang sama.
- Gaya idiomatic ditanam sebagai kontrak yang tidak boleh diubah: ktlint `1.8.0` wajib exit `0` untuk seluruh
  `*.kt`/`*.kts` (kecuali `backup*/`, `trackify_fix/`, dan `**/build/**`), `.editorconfig` memakai
  `ktlint_code_style = android_studio` dan trailing comma, panggilan Compose memakai named argument, expression
  body bila jelas, `val` sebagai default, tanpa wildcard import, dan KDoc ringkas. Dilarang memformat manual yang
  bertabrakan dengan ktlint.
- CI `validate` menjalankan ktlint dengan checksum tetap dan grep kontrak ikon. Gate lokal wajib ktlint `0`,
  `:app:assembleDebug :app:testDebugUnitTest`, grep kontrak, smoke perangkat, dan catatan `CHANGELOG.md`.
- Perubahan ini tidak menaikkan versi serta tidak membuat tag/rilis baru. Tag dan rilis `v1.0.0`/`v1.1.0` utuh.

### 24 Nov 2026 — Format Bagikan Lokasi disederhanakan

User mengubah `LocationShareText.build` menjadi empat baris: `Nama Penerima : …`, `Detail/Patokan : …`,
`Maps : …`, dan `by Trackify.`. Header `FOTO`, garis pemisah `-------`, dan baris kosong dihapus total.
KDoc, `LocationShareTextTest`, `docs/FEATURE_ROADMAP.md`, dan kontrak ini disamakan ke format tersebut.
Tidak ada perubahan pada `Intent.ACTION_SEND`, lampiran foto, `FileProvider`, atau field `SavedLocation`.

### 24 Nov 2026 — Header Catatan rapi & satu sumber warna

User melaporkan tombol **Pilih** di samping **Tambah** tidak rapi dan meminta audit tema sampai ke akar:
"jangan pernah ada warna lain selain tema ... di ui lama ataupun baru, catat itu dan ingat". Keputusan:

- Tombol **Pilih/Batal** di header Catatan diganti tombol ikon Coolicons resmi (`Check`/`Close`) yang sejajar
  dengan **Tambah**; long-press kartu menjadi jalur alternatif masuk seleksi; bar aksi seleksi memakai
  `TrackifyCard` dengan label jumlah, **Selesai**, dan **Hapus**. Teks **Pilih/Batal** di header dilarang.
- `res/values/colors.xml` menjadi satu-satunya sumber warna, dibaca Kotlin lewat facade `TrackifyColors`.
  Seluruh hex di Kotlin/gradien/launcher dihapus, hijau launcher `#3DDC84` diganti charcoal, dan semua slot
  `ColorScheme` diisi token agar default Material3 tidak menyisipkan warna lain.
- CI `validate` mendapat langkah **Theme color guard**. Aturan ini bersifat permanen untuk UI lama maupun baru.
- Tidak ada perubahan pada bottom nav, `ScreenTitle`, tema otomatis, orientasi portrait, `TrackifyCard`/
  `TrackifyDialog`, token tombol/spacing, versi, tag, atau rilis.

### 24 Nov 2026 — Kontras teks & ikon mengikuti latar (polish lanjutan)

User meminta warna teks dan ikon menyesuaikan kartu/hero: "masa iya di dalam cardnya textnya ada yg ga keliatan".
Keputusan lanjutan dari kontrak §4.9:

- Hero Home memakai `hero.*` secara eksplisit untuk **semua** teks, field, dan tombol di atas gradien. Field
  `Dibawa`/`Dikirim` dan tombol **Simpan paket** tidak boleh mewarisi `onSurface` skema global.
- `trackifyFieldColors()` mengoverride warna teks, label, placeholder, cursor, selection, dan border. Versi
  Material3 di proyek belum menerima `selectionHandleColor`, jadi seleksi diatur lewat `TextSelectionColors`.
- `TrackifyButton`, `TrackifyTextButton`, dan `TrackifyIconButton` menerima `contentColor`/`borderColor`;
  tombol ikon menyediakan `LocalContentColor`. `disabledContentColor` memakai `onSurfaceVariant` tanpa alpha
  supaya tidak jadi teks abu transparan. Border tombol tetap `outlineVariant` 1.dp (Fase 17 tidak berubah).
- Teks di atas `surfaceVariant` wajib `onSurface`, bukan `onSurfaceVariant` (yang hanya 4,41:1 di tema terang).
  Berlaku untuk placeholder "LOK" (Home), inisial nama (Lokasi), catatan di dialog detail Lokasi, toggle
  Masuk/Libur, label Dibawa/Terkirim, segmen Kartu/Grid, dan chip filter Rekap.
- Batang grafik non-unggulan memakai `onSurfaceVariant`; lintasan progress bar tetap `surfaceVariant` karena
  memang background track.
- `FilterChip` Catatan menetapkan `selectedLeadingIconColor`/`selectedTrailingIconColor = onPrimary` agar
  ikon centang tidak hilang saat suatu hari ditambahkan.
- Tidak ada token hex baru, tidak ada ikon baru, dan tidak ada perubahan versi (`2` / `1.1`), tag, atau rilis.

### 24 Nov 2026 — Semua ikon tampil lagi (mask alpha ikon)

User melaporkan "iconnya pada hilang". Smoke perangkat membuktikan yang hilang bukan hanya ikon tombol,
melainkan **semua** ikon: header, bottom nav, pin, dan ikon kartu.

- `TrackifyIcons.coolicon()` kembali memakai `fill = SolidColor(TrackifyColors.IconMask)`. Sebelumnya `fill`
  dihapus untuk mematuhi theme guard, sehingga path memakai `Color.Unspecified` dengan alpha 0. `Icon`
  mewarnai lewat `ColorFilter.tint`, jadi path transparan tetap tak terlihat berapapun `tint`-nya.
- `IconMask` disimpan di facade `TrackifyColors.kt` (bukan literal di file ikon). Nilainya hanya mask alpha;
  RGB tidak pernah tampil karena ditimpa `tint`, jadi tidak ada warna UI baru.
- Ikon **Plus** di header Lokasi/Catatan dan **CompassArrow** pada tombol **Rute** kini memakai
  `tint = scheme.onSurface`, mengikuti teks `TrackifyButton`.
- `TrackifyButton` membungkus `content` dengan `CompositionLocalProvider(LocalContentColor provides
  contentColor)` supaya ikon turunan tetap mengikuti `contentColor` (termasuk override hero).
- Aturan permanen di §4.9: path ikon wajib mask opaque dari facade dan semua `Icon(...)` wajib `tint`
  eksplisit; `LocalContentColor` hanya pengaman.
- Tidak ada token warna baru di `colors.xml`, ikon Coolicons tetap `fill/round`, tidak ada perubahan versi,
  tag, atau rilis.

### 25 Nov 2026 — Rilis patch v1.1.1 (amandemen versi)

Atas permintaan eksplisit user, kontrak versi yang sebelumnya dikunci dibuka **hanya untuk patch ini**:

- `versionCode = 3`, `versionName = "1.1.1"` menggantikan `2` / `"1.1"`.
- Tag dan rilis baru `v1.1.1` diterbitkan dengan aset `.apk`. Tag/release `v1.0.0` dan `v1.1.0` tidak diubah.
- Pengecheck pembaruan memakai `ReleaseVersion`, jadi `v1.1.1` dianggap lebih baru dari `v1.1.0`.
- Versi berikutnya tetap memerlukan keputusan eksplisit; amandemen ini tidak otomatis mengizinkan bump lain.

### 25 Nov 2026 — TPG: anggaran adaptif tanpa mengubah kontrak visual atau DB

Trackify Performance Governor (TPG) mengikuti prinsip
<https://developer.android.com/topic/performance/overview?hl=id>: ukur → perbaiki → ukur ulang.

- Modul `:core:performance` berisi logika murni JVM: `FrameHealthTracker`, `AdaptiveBudgetPolicy`,
  `QueryGuardPolicy`, dan `TrackifyGovernor`. Tidak ada dependensi Android, jadi bisa diuji di JVM.
- Sinyal frame diambil dari `Choreographer` di `MainActivity` hanya saat `ON_START`..`ON_STOP`.
  perubahan grade frame diteruskan ke daftar Catatan/Lokasi lewat listener governor.
- Aturan permanen: anggaran TPG hanya mengatur **kapan** kerja berat dilakukan dan seberapa sensitif
  deteksi query. `previewQuota` wajib sama dengan `PerformanceConfig.maxPreviewItems`; TPG tidak boleh
  mengubah jumlah foto yang tampil, warna, ikon, teks, bottom nav, format Bagikan Lokasi 4 baris,
  tema otomatis, orientasi portrait, atau token UI lain.
- Fase 10 tidak diduplikasi: `DevicePerformance`, `PerformancePolicy.sampleSize`, filter akurasi/umur GPS,
  dan downsample foto saat persist tetap menjadi sumber keputusan perangkat.
- Room `2.7.1` hanya mengekspos `onQuery(sqlQuery, bindArgs)` tanpa timestamp mulai/selesai. Karena itu
  `RoomQueryGuard` **dilarang mengklaim durasi query**; ia hanya melaporkan bentuk SQL berpotensi lambat
  (`LIKE + ORDER BY`, sort tanpa index, `UPDATE linkedLocationId`) dengan deduplikasi 60 detik.
- `restoreSnapshot` tetap full-replace dan format JSON tetap kompatibel dengan schema lama. Normalisasi
  dilakukan tepat sebelum upsert: entitas dideduplikasi, foto dengan `noteId <= 0`, orphan, path kosong,
  atau melebihi 5 per catatan dibuang. `decodeBackup` tetap tidak menolak schema lama.
- Database tetap v8 dengan `MIGRATION_5_6`, `MIGRATION_6_7`, `MIGRATION_7_8`. Index komposit dan
  migrasi v9 memerlukan amandemen kontrak terpisah.
- Aturan autobackup belum diputuskan; `backup_rules.xml` dan `data_extraction_rules.xml` sengaja tidak diubah.
- R8 default sengaja dipertahankan (paket scope pernah membuat APK keluar instan).
- Perubahan TPG awalnya belum dirilis; setelah keputusan eksplisit user, dirilis sebagai `v1.2.0`
  (`versionCode = 4`). Tag dan rilis `v1.1.1` tidak diubah.
- Atas keputusan yang sama: DB tetap v8 (tidak ada amandemen v9), dan aturan autobackup masih ditunda.
