# Trackify — Peta Jalan Pengembangan Fitur

> Status: **23 Nov 2026**. Dokumen ini melengkapi `LOCKED_DECISIONS.md`, bukan menggantikannya.
> Semua usulan di bawah **tidak boleh** mengubah bottom navigation, header `ScreenTitle`, tema otomatis,
> orientasi portrait, token `TrackifyCard`/`TrackifyDialog`, struktur modul, atau menambahkan fitur pembayaran tunai.

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
- Tambah pintasan ke **Catatan** (selesai di Fase 3).
- Grafik 7 batang memakai data harian nyata dan bisa dipilih (selesai di Fase 4).
- Konfirmasi untuk aksi cepat `-1/+1/+5/+10` dan reset (selesai di Fase 5: konfirmasi juga untuk toggle **Masuk → Libur**, yang mengosongkan paket dan pengeluaran hari ini).

**Batasan:** tidak menambah subtext di header; kartu baru wajib `TrackifyCard`.

### B. Rekap
**Kondisi aktual**
- Statistik bulanan, tabel harian, editor log, carryover, ekspor CSV, sheet target, laporan WhatsApp.
- `targetRupiah` adalah draft lokal yang disengaja (source of truth tetap `settings.targetAmount`).
- DatePicker dibuka tanpa tanggal awal; input tarif/bonus/biaya dibulatkan ke integer.
- Laporan WhatsApp memakai `formatMonthlyWhatsappReport` dengan `%.1f` dan bonus tier default nol.

**Usulan**
- `rememberSaveable` untuk sub-tab, mode, sort, filter, dan pencarian (selesai di Fase 2); dialog tetap `remember`.
- DatePicker dibuka di tanggal log yang sedang diedit (selesai di Fase 3).
- Input desimal untuk tarif, bonus, dan biaya (selesai di Fase 3).
- Konfirmasi hapus log (selesai di Fase 1).
- Angka rupiah sudah lewat `toRupiah()`; rata-rata paket per hari kini memakai locale `id-ID` (selesai di Fase 3).

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
- `rememberSaveable` untuk query dan tag terpilih (selesai di Fase 2).
- Tag tetap berbasis `landmarkNote` sampai ada keputusan eksplisit untuk migrasi DB ke versi 7.
- Foto lokasi (ambil dari kamera ke galeri + pilih dari galeri) selesai di Fase 6; DB naik ke versi 7.

**Batasan:** dilarang `FusedLocationProviderClient`; DB tetap versi 6.

### D. Catatan
**Kondisi aktual**
- Tulis/edit/hapus catatan, pin, status selesai, taut lokasi, pencarian judul/isian, templat Titipan/Kendala Kirim.
- Chip templat selalu `selected = false`; parameter `isNew` diabaikan oleh wiring `MainActivity` (diperbaiki di Fase 3).
- `updatedAt` ditimpa tanggal hari ini tiap simpan (diperbaiki di Fase 3).
- Pencarian belum menyertakan nama lokasi ters taut.

**Usulan**
- Chip templat benar-benar mengisi editor dan menandai seleksi (selesai di Fase 3, lengkap dengan **Reset**).
- Wiring `isNew` diperbaiki di `MainActivity` (selesai di Fase 3).
- Pencarian ikut nama lokasi (selesai di Fase 4 lewat `customerName`).
- `updatedAt` hanya berubah saat konten berubah (selesai di Fase 3; model tidak punya `createdAt`).
- Aksi massal selesai/hapus selesai di Fase 4; pengurutan catatan selesai di Fase 5 (`Prioritas`, `Terbaru`, `Terlama`, `Judul A–Z`).

### E. Setelan
**Kondisi aktual**
- Tarif, bonus, target, pengingat 20.00, backup/restore JSON, reset, ekspor CSV bulanan.
- Pill "Pembagian gajian 2x" statis, tidak bisa diklik.
- Restore langsung berjalan tanpa konfirmasi, padahal mengganti seluruh data (diperbaiki di Fase 3).
- Subtitle backup belum menyebut catatan (diperbaiki di Fase 3); pemilih bulan CSV tidak ada di layar ini.

**Usulan**
- Konfirmasi restore sebelum full-replace (selesai di Fase 3).
- Subtitle backup menyebut log, lokasi, catatan, dan pengaturan (selesai di Fase 3).
- Pemilih bulan untuk ekspor CSV (selesai di Fase 4; belum disinkronkan dengan bulan di Rekap).
- Status "Pembagian gajian 2x" dibuat jelas sebagai informasi, bukan tombol palsu (selesai di Fase 4).

**Batasan:** tidak boleh ada menu Tampilan / toggle tema, dan jadwal pengingat tetap 20.00.

---

## 3. Rencana fase

| Fase | Isi | Status |
|---|---|---|
| 1 | GPS valid, unlink lokasi, konfirmasi hapus lokasi/log, toast `+1 Pkt` jujur | Selesai |
| 2 | `rememberSaveable` untuk Rekap, Lokasi, Catatan, Setelan | Selesai |
| 3 | Home (pintasan Catatan), Rekap (DatePicker/desimal/laporan), Catatan (templat/waktu), Setelan (konfirmasi restore/subtitle) | Selesai |
| 4 | Grafik interaktif, pencarian lokasi, aksi massal, pemilih bulan CSV, info gajian | Selesai |
| 5 | Konfirmasi aksi cepat & toggle Libur di Home, pengurutan catatan | Selesai |
| 6 | Foto lokasi (kamera → galeri + unggah galeri), DB v7 | Selesai |
| 7 | Input URL Maps (panjang + short link), hapus sistem tag | Selesai |
| 8 | Catatan ala Notion lite: blok teks + checklist | Selesai |

Fase 6 menambah `photoPath` pada `saved_locations` lewat `MIGRATION_6_7`. Foto kamera masuk galeri,
lalu salinan stabil disimpan di `filesDir/location_photos`; preview tampil di editor, detail, dan kartu
terdekat. Konfirmasi Fase 5 tetap memakai `TrackifyDialog`; tombol aksi cepat dan toggle
**Masuk/Libur** tidak diubah bentuknya. Pengurutan catatan memakai chip dengan state `rememberSaveable`.

Fase 7 mengganti dua field koordinat dengan satu field **URL Maps**. `MapUrlParser` menangani format
`@lat,lon`, query `q/query/destination/ll/center`, `!3d...!4d...`, `%2C`, dan `lat,lon` polos;
`MapLinkResolver` mengikuti short link sampai 5 redirect. Sistem tag `Langganan`/`Titip`/`Biasa`
dihapus dari filter, editor, kartu, dan logika catatan. Tidak ada migrasi DB baru.

Fase 8 menambah blok paragraf, judul, daftar, dan checklist pada editor Catatan tanpa mengubah
schema `courier_notes` (isi tetap satu field `content`). `NoteBlocks` memakai prefiks `# `, `- `,
`[ ]`, dan `[x]`; checklist disimpan sebagai teks, jadi backup/ekspor lama tetap terbaca.

Tiap fase wajib: backup sebelum edit → ktlint exit 0 → `:app:assembleDebug :app:testDebugUnitTest` hijau
→ 14 tes lulus → grep kontrak kosong → smoke test perangkat → catatan di `CHANGELOG.md`.
