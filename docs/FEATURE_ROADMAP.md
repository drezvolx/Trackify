# Trackify — Peta Jalan Pengembangan Fitur

> Status: **23 Nov 2026**. Dokumen ini melengkapi `LOCKED_DECISIONS.md`, bukan menggantikannya.
> Semua usulan di bawah **tidak boleh** mengubah bottom navigation, header `ScreenTitle`, tema otomatis,
> orientasi portrait, token `TrackifyCard`/`TrackifyDialog`, struktur modul, atau menambahkan COD.

---

## 1. Temuan yang perlu didahulukan

| # | Temuan | Lokasi | Alasan |
|---|---|---|---|
| 1 | Fix GPS diterima tanpa cek akurasi dan umur | `core/data/.../location/DeviceLocationSource.kt` | Bertentangan dengan `LOCKED_DECISIONS.md §4.3` |
| 2 | Hapus lokasi menyisakan `linkedLocationId` di catatan | `TrackifyRepository.deleteLocation` | Tombol **Rute** di Catatan mengacu ke id yang sudah hilang |
| 3 | `+1 Pkt` selalu toast sukses walaupun data tidak berubah | `LocationsScreen` + `TrackifyViewModel.incrementTodayDelivered` | `sanitized()` memotong jumlah, tapi UI tetap mengabarkan sukses |
| 4 | State filter/tab/dialog memakai `remember` | Rekap, Lokasi, Catatan, Setelan | Hilang saat pindah tab atau process death |
| 5 | Hapus log dan hapus lokasi tanpa konfirmasi | `SpreadsheetScreen`, `LocationsScreen` | Risiko kehilangan data |

---

## 2. Pemetaan per fitur

### A. Home
**Kondisi aktual**
- Kartu status hari ini, kontrol `+1/+5/+10/-1`, ringkasan pendapatan, target, pintasan Rekap/Lokasi/Setelan.
- "Lokasi berikutnya" hanya `savedLocations.firstOrNull()` dengan thumbnail statis `LOK`.
- Grafik 7 batang bersifat dekoratif, tanpa nilai dan interaksi.

**Usulan**
- Tambah pintasan ke **Catatan** (sudah dijanjikan `Documentation.md`, belum ada di UI).
- Grafik 7 batang memakai data harian nyata dan bisa dipilih.
- Konfirmasi untuk aksi cepat `+1/+5/+10` dan reset.

**Batasan:** tidak menambah subtext di header; kartu baru wajib `TrackifyCard`.

### B. Rekap
**Kondisi aktual**
- Statistik bulanan, tabel harian, editor log, carryover, ekspor CSV, sheet target, laporan WhatsApp.
- `targetRupiah` adalah draft lokal yang disengaja (source of truth tetap `settings.targetAmount`).
- DatePicker dibuka tanpa tanggal awal; input tarif/bonus/biaya dibulatkan ke integer.
- Laporan WhatsApp memakai `formatMonthlyWhatsappReport` dengan `%.1f` dan bonus tier default nol.

**Usulan**
- `rememberSaveable` untuk sub-tab, mode, sort, filter, pencarian, dan dialog.
- DatePicker dibuka di tanggal log yang sedang diedit.
- Input desimal untuk tarif, bonus, dan biaya.
- Konfirmasi hapus log.
- Format angka laporan memakai locale `id-ID` lewat `toRupiah()`.

**Batasan:** jangan menghidupkan kembali `applyAutoBonus`, `computeTargetPace`, atau `TargetPaceInfo`.

### C. Lokasi
**Kondisi aktual**
- Simpan/edit/hapus titik, urutkan berdasarkan jarak, GPS live, Rute, Bagikan, `+1 Pkt`, `Catat`.
- Tag bukan kolom, melainkan ditebak dari `landmarkNote` (`inferTag()`).
- Karti "Terdekat" memiliki area peta kosong setinggi `112.dp`.

**Usulan (Fase 1)**
- Validasi akurasi (`<= 200 m`) dan umur fix (`<= 60 s`) di `DeviceLocationSource`.
- Hapus lokasi memakai `@Transaction`: hapus tautan catatan dulu, lalu hapus titik.
- Konfirmasi hapus lewat `TrackifyDialog`.
- Toast `+1 Pkt` hanya saat jumlah benar-benar berubah.

**Usulan (lanjutan)**
- `rememberSaveable` untuk query dan tag terpilih.
- Tag tetap berbasis `landmarkNote` sampai ada keputusan eksplisit untuk migrasi DB ke versi 7.

**Batasan:** dilarang `FusedLocationProviderClient`; DB tetap versi 6.

### D. Catatan
**Kondisi aktual**
- Tulis/edit/hapus catatan, pin, status selesai, taut lokasi, pencarian judul/isian, templat Titipan/Kendala Kirim.
- Chip templat selalu `selected = false`; parameter `isNew` diabaikan oleh wiring `MainActivity`.
- `updatedAt` ditimpa tanggal hari ini tiap simpan, sehingga tidak ada waktu buat.
- Pencarian belum menyertakan nama lokasi ters taut.

**Usulan**
- Chip templat benar-benar mengisi editor dan menandai seleksi.
- Wiring `isNew` diperbaiki di `MainActivity`.
- Pencarian ikut nama lokasi.
- `createdAt` dipertahankan saat edit; `updatedAt` hanya berubah saat konten berubah.
- Sort dan aksi massal (selesai/hapus) — butuh keputusan terpisah karena menyentuh layout kartu.

### E. Setelan
**Kondisi aktual**
- Tarif, bonus, target, pengingat 20.00, backup/restore JSON, reset, ekspor CSV bulanan.
- Pill "Pembagian gajian 2x" statis, tidak bisa diklik.
- Restore langsung berjalan tanpa konfirmasi, padahal mengganti seluruh data.
- Subtitle backup belum menyebut catatan; pemilih bulan CSV tidak ada di layar ini.

**Usulan**
- Konfirmasi restore sebelum full-replace.
- Subtitle backup menyebut log, lokasi, catatan, dan pengaturan.
- Pemilih bulan untuk ekspor CSV atau sinkronkan dengan bulan terpilih di Rekap.
- Status "Pembagian gajian 2x" dibuat jelas sebagai informasi, bukan tombol palsu.

**Batasan:** tidak boleh ada menu Tampilan / toggle tema, dan jadwal pengingat tetap 20.00.

---

## 3. Rencana fase

| Fase | Isi | Status |
|---|---|---|
| 1 | GPS valid, unlink lokasi, konfirmasi hapus lokasi/log, toast `+1 Pkt` jujur | Dikerjakan |
| 2 | `rememberSaveable` untuk Rekap, Lokasi, Catatan, Setelan | Belum |
| 3 | Home (pintasan Catatan + grafik nyata), Rekap (DatePicker/desimal/laporan), Catatan (templat/search/waktu), Setelan (konfirmasi restore/CSV) | Belum |

Tiap fase wajib: backup sebelum edit → ktlint exit 0 → `:app:assembleDebug :app:testDebugUnitTest` hijau
→ 14 tes lulus → grep kontrak kosong → smoke test perangkat → catatan di `CHANGELOG.md`.
