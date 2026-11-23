# Changelog

Catatan perubahan Trackify. Urutan terbaru di atas.

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
