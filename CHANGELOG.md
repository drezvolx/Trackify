# Changelog

Catatan perubahan Trackify. Urutan terbaru di atas.

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
- Istilah **COD** dari source aktif.
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
