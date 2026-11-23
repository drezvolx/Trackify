# Changelog

Catatan perubahan Trackify. Urutan terbaru di atas.

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
