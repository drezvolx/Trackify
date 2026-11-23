# Trackify — Dokumentasi Proyek

**Trackify** adalah aplikasi Android *offline-first* untuk kurir dan pengemudi pengiriman barang: mencatat rekap harian, melacak paket terkirim, menghitung pendapatan bersih, mengelola titik lokasi pelanggan (GPS), dan menyimpan catatan operasional lapangan.

> **Penting:** Keputusan yang **tidak boleh disentuh** — terutama bottom navigation — ada di [`docs/LOCKED_DECISIONS.md`](docs/LOCKED_DECISIONS.md). Baca file itu sebelum mengubah UI, arsitektur, atau build file.

---

## 1. Ringkasan Teknologi

| Area | Pilihan final |
|---|---|
| Bahasa | Kotlin `2.2.10` |
| UI | Jetpack Compose + Material 3 |
| Arsitektur | MVVM, `StateFlow`, Coroutines |
| DI | Hilt `2.60.1` + KSP `2.3.13` |
| Database | Room `2.7.1`, DB version `6`, `MIGRATION_5_6` |
| Modul | `:app`, `:core:domain`, `:core:data` |
| Lokasi | `LocationManager` / `DeviceLocationSource` |
| Notifikasi | AlarmManager + `DailyReminderReceiver` / `BootReminderReceiver` |
| Format uang | `toRupiah()` di `core:domain`, locale `id-ID` |

---

## 2. Navigasi & Fitur Utama

Navigasi utama memakai 4 tab (`TrackifyTab`), dipanggil dari `Scaffold.bottomBar` di `MainActivity.kt` melalui `ReferenceNotchedBottomBar`:

```
HOME → REKAP → LOKASI → CATATAN
```

Detail tampilan bottom nav yang sudah dikunci ada di [`docs/LOCKED_DECISIONS.md#1-bottom-navigation--zona-paling-kritis`](docs/LOCKED_DECISIONS.md).

Header semua layar memakai satu komponen `ScreenTitle` dari `ui/theme/TrackifyTheme.kt`: **cuma judul, tanpa subtext**.

| Layar | Judul |
|---|---|
| Home | `Trackify` |
| Rekap | `Rekap` |
| Lokasi | `Lokasi` |
| Catatan | `Catatan` |
| Settings | `Setelan` |

Detail kontrak header ada di [`docs/LOCKED_DECISIONS.md#17-header--judul-tab--hanya-judul`](docs/LOCKED_DECISIONS.md).

Tema aplikasi mengikuti perangkat secara otomatis (dark/light sesuai sistem). Menu **Tampilan** dan toggle tema sudah dihapus. Orientasi aplikasi dikunci portrait.

Kartu memakai satu token lewat `ui/theme/TrackifyComponents.kt` → `TrackifyCard`: rounded `20.dp`, `scheme.surface`, border `outlineVariant` `1.dp`, padding `16.dp`, judul kartu `SemiBold`.

### A. Home
- Status hari ini: masuk/libur, paket dibawa, paket terkirim.
- Kontrol cepat `+1`, `+5`, `+10` dan reset.
- Ringkasan pendapatan & progres target harian/bulanan.
- Pintasan ke Rekap, Lokasi, Catatan, dan Settings.

### B. Rekap
- Statistik bulanan: hari kerja, paket terkirim, pendapatan bersih.
- Tabel riwayat harian dengan detail tarif, bonus, potongan, dan net income.
- Edit rekap & pilih tanggal lain via `DatePicker`.
- Carryover sisa paket kemarin.
- Ekspor CSV.
- Target bulanan diubah lewat sheet; source of truth tetap `state.settings.targetAmount`.

### C. Lokasi
- Simpan titik GPS pelanggan + nama + patokan.
- Urutkan berdasarkan jarak terdekat saat izin lokasi tersedia.
- GPS live memakai `LocationManager`/`DeviceLocationSource` (bukan Fused Location).
- Buka Google Maps dan bagikan lokasi.
- Aksi cepat: tambah delivered & buat catatan tertaut.

### D. Catatan
- Catatan operasional dengan status selesai/belum.
- Pin, taut ke lokasi, pencarian cepat.
- Templat: **Titipan** dan **Kendala Kirim**.

### E. Settings
- Tarif default, bonus default, target, pengingat harian. Tema mengikuti sistem otomatis.
- Backup/Restore JSON penuh (log, lokasi, catatan, dan pengaturan; restore mengganti data lama).
- Reset data dengan konfirmasi.
- Ekspor CSV bulanan.

---

## 3. Arsitektur Modul

```
:app  ──>  :core:data  ──>  :core:domain
 : └──────────────────────>  :core:domain
```

- `:core:domain`: model, logika murni, format mata uang, templat catatan. Hanya memakai `room-common` untuk anotasi.
- `:core:data`: Room runtime, DAO, database, migrasi, repository, lokasi, notifikasi, `AppModule`.
- `:app`: UI Compose, `MainActivity`, ViewModel, navigation state, bottom bar.

Modul `:feature:*` **sengaja belum dibuat**. Bottom bar tetap berada di `:app`, dan state UI masih dipakai bersama lintas tab.

Hilt:
- `TrackifyApp` → `@HiltAndroidApp`
- `MainActivity` → `@AndroidEntryPoint`
- `TrackifyViewModel` → `@HiltViewModel`
- `AppModule` → `core:data`

---

## 4. Struktur Basis Data

Database `trackify_offline_db`, **version 6**, migrasi aktif **`MIGRATION_5_6`**, mode WAL, dan `fallbackToDestructiveMigration(true)`.

### 4.1 `daily_logs`
- `date` (PK, `YYYY-MM-DD`)
- `isWorkingDay`
- `packagesCarried`
- `packagesDelivered`
- `ratePerPackage`
- `bonus`
- `expense`
- `notes`

> Field fitur pembayaran tunai sudah dihapus permanen. Jangan ditambahkan kembali.

### 4.2 `saved_locations`
- `id` (PK, auto-generate)
- `customerName`
- `landmarkNote`
- `latitude`, `longitude`

### 4.3 `courier_notes`
- `id` (PK, auto-generate)
- `title`, `content`
- `isPinned`
- `updatedAt`
- `linkedLocationId`
- `isResolved`

`DailyLog.sanitized()` memastikan:
- `packagesDelivered` tidak melebihi `packagesCarried`
- hari libur tidak punya paket/pendapatan

---

## 5. Struktur Source

```
app/src/main/java/com/blankpages/trackify/
├── MainActivity.kt                  # Bottom nav final + Scaffold insets
├── TrackifyApp.kt                   # @HiltAndroidApp + crash handler
├── navigation/TrackifyNavigation.kt # TrackifyTab
├── ui/
│   ├── TrackifyViewModel.kt
│   ├── theme/
│   ├── home/HomeScreen.kt
│   ├── spreadsheet/SpreadsheetScreen.kt
│   ├── locations/LocationsScreen.kt
│   ├── notes/NotesScreen.kt
│   └── settings/SettingsScreen.kt
└── ...

core/domain/src/main/java/com/blankpages/trackify/domain/
├── Models.kt
├── TrackifyEngine.kt
├── CurrencyFormat.kt
└── NoteTemplates.kt

core/data/src/main/java/com/blankpages/trackify/
├── data/
│   ├── TrackifyRepository.kt
│   ├── local/{AppPreferences,TrackifyDatabase}.kt
│   ├── location/DeviceLocationSource.kt
│   └── notification/{DailyReminderScheduler,...}.kt
└── di/AppModule.kt
```

---

## 6. Perilaku Penting yang Sudah Dikunci

- Bottom bar: outline + notch + fill `scheme.background.copy(alpha = 0.72f)`, tanpa kotak hitam solid.
- Status bar: `contentWindowInsets = WindowInsets.statusBars`, konten tidak menyatu dengan notifikasi HP.
- Satu `Scaffold` membungkus semua layar termasuk Settings; bottom bar hanya disembunyikan saat `inSettings`.
- Header semua tab/layar: `ScreenTitle`, judul saja, tanpa subtext, `FontWeight.ExtraBold` (Fase 16).
- Tema otomatis mengikuti sistem; tidak ada menu `Tampilan` / toggle Dark–White.
- Orientasi portrait; rotasi otomatis dinonaktifkan.
- Kartu memakai `TrackifyCard` (20.dp / surface / outlineVariant / padding 16.dp / SemiBold).
- Semua dialog memakai `TrackifyDialog` (20.dp / `scheme.surface` / `onSurface` / `onSurfaceVariant` / elevation 0 / judul SemiBold), bukan `AlertDialog` default.
- Konten boleh lewat di belakang outline; padding bawah tiap tab/layar `96.dp`.
- Fitur pembayaran tunai hilang total dari source aktif.
- Hilt tidak boleh downgrade dari `2.60.1`.
- Live location tidak boleh memakai `FusedLocationProviderClient`.
- Import backup memakai `repository.restoreSnapshot(...)` dan mengganti penuh data lama.
- Reset data memakai `resetAllData()` + konfirmasi.
- Flag simpan Lokasi: `editingTarget != null` untuk edit, `== null` untuk lokasi baru. Bug sebelumnya terbalik sehingga lokasi baru gagal masuk DB.
- Uji perangkat 23 Nov 2026: light/dialog `#FFFFFF`, dark/dialog `#141414`; tambah/edit/hapus Lokasi & Catatan persist after restart; backup schema `6`; reset mengosongkan DB + prefs; restore mengembalikan 9 log / 1 lokasi / 1 catatan + prefs dan menghapus data baru yang dibuat setelah backup.

Detail lengkap dan alasannya ada di [`docs/LOCKED_DECISIONS.md`](docs/LOCKED_DECISIONS.md).

---

## 7. Verifikasi

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain
```

Expected: `BUILD SUCCESSFUL`, 10/10 tes lulus, 0 warning.

Cek subtext header:

```powershell
rg -n "Pencatatan dan gaji bulanan|rumah tersimpan|Trackify 1\\.0" app/src --glob "*.kt"
```

Expected: tidak ada output.

Cek fitur pembayaran tunai:

```powershell
rg -n "bayar di tempat|pembayaran tunai|dibayar saat pengiriman" app/src core --glob "*.kt"
```

Expected: kosong.

Cek tema manual tidak kembali:

```powershell
rg -n "Tampilan|onToggleTheme|isDarkTheme|setDarkTheme|setThemeMode|state\.settings\.themeMode" app/src --glob "*.kt"
```

Expected: kosong.

Cek orientasi:

```powershell
adb shell dumpsys activity activities | Select-String "requestedOrientation=SCREEN_ORIENTATION_PORTRAIT"
```

Untuk perubahan `MainActivity`, jangan hanya mengandalkan Apply Changes:

```powershell
adb install -r "app\build\outputs\apk\debug\app-debug.apk"
adb shell am force-stop com.blankpages.trackify
adb shell monkey -p com.blankpages.trackify -c android.intent.category.LAUNCHER 1
```

---

## 8. Backup

Arsip folder lokal sudah dihapus. Cadangan resmi memakai riwayat git di `main` dan tag `v1.0.0`.

Untuk kembali ke titik rilis, gunakan commit yang ditandai `v1.0.0`. Riwayat git adalah satu-satunya sumber rollback; jangan mengandalkan salinan folder lama.

Laporan bug/insiden tetap memakai [`report.md`](report.md).
