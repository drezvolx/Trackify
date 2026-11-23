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
- Tarif, bonus, target, pengingat (default 20.00, bisa diatur sejak Fase 12), backup/restore JSON, reset, ekspor CSV bulanan.
- Pill "Pembagian gajian 2x" statis, tidak bisa diklik.
- Restore langsung berjalan tanpa konfirmasi, padahal mengganti seluruh data (diperbaiki di Fase 3).
- Subtitle backup belum menyebut catatan (diperbaiki di Fase 3); pemilih bulan CSV tidak ada di layar ini.

**Usulan**
- Konfirmasi restore sebelum full-replace (selesai di Fase 3).
- Subtitle backup menyebut log, lokasi, catatan, dan pengaturan (selesai di Fase 3).
- Pemilih bulan untuk ekspor CSV (selesai di Fase 4; belum disinkronkan dengan bulan di Rekap).
- Status "Pembagian gajian 2x" dibuat jelas sebagai informasi, bukan tombol palsu (selesai di Fase 4).

**Batasan:** tidak boleh ada menu Tampilan / toggle tema. Jadwal pengingat milik user (default 20.00), tapi tidak boleh menambah toggle tema atau notifikasi baru di luar pengingat harian.

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
| 9 | Input paket Dibawa/Dikirim pakai teks | Selesai |
| 10 | Engine performa & device profile untuk perangkat low-end | Selesai |
| 11 | Flow gajian & periode yang bisa diatur user | Selesai |
| 12 | Jam notifikasi bisa diatur user | Selesai |
| 13 | Lokasi murni + bagikan foto & teks | Selesai |
| 14 | Template Notion opsional + multi-foto catatan, DB v8 | Selesai |
| 15 | Tema gradien monokrom: Royal Charcoal / Pure Charcoal | Rencana |
| 16 | Typography tebal di judul + gaya khas tiap layout | Rencana |
| 17 | Tombol outline + spacing/typography konsisten | Rencana |
| 18 | Tombol update, kredit, folder manajemen, README/docs | Rencana |

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

Fase 9 menambah dua field angka **Dibawa** dan **Dikirim** di Home. Tombol `-1/+1/+5/+10` tetap ada,
tetapi user bisa mengetik jumlah persis lewat keyboard numerik. Nilai lokal di-commit lewat tombol
**Simpan paket**; `DailyLog.sanitized()` tetap memotong `dikirim` agar tidak melebihi `dibawa`.

### Rencana lanjutan yang sudah dipetakan (Fase 10–18)

- **Fase 10 — Performa (selesai):** `DevicePerformance` membaca `isLowRamDevice`, API level, dan memori sekali
  saat start, lalu `PerformancePolicy` memetakannya ke `PerformanceConfig`. Config mengatur downsample foto,
  kualitas JPEG, ukuran preview, interval/jarak GPS, dan batas jumlah preview. Tidak mengubah aturan domain.
- **Fase 11 — Gaji & periode (selesai):** `AppPreferences` menyimpan `wageMode` (`tarif per paket` /
  `nominal tetap per hari`), nominal harian, `wagePeriod` (`Harian`/`Mingguan`/`Bulanan`), dan tanggal gajian.
  `WageCalculator` (domain murni) menghitung pendapatan per hari, ringkasan, agregat periode, dan target periode.
  Default **Tarif per paket + Bulanan + tanggal 25 + Rp2.500** dipertahankan sehingga data lama tidak berubah.
  Semua setting menjadi milik user; backup JSON ikut menyimpan keempat nilai itu. Tidak ada migrasi DB.
- **Fase 12 — Notifikasi (selesai):** jam pengingat pindah dari konstanta `20.00` ke prefs
  (`reminder_hour` / `reminder_minute`) lewat model `ReminderTime` di domain. Setelan menampilkan
  `Notifikasi pukul …` dan tombol **Atur jam** dengan `TimePicker` 24 jam di dalam `TrackifyDialog`.
  `DailyReminderScheduler` membaca prefs setiap dijadwalkan, jadi alarm langsung diperbarui tanpa
  install ulang. Backup JSON ikut menyimpan `reminderHour` / `reminderMinute`; backup lama memakai
  default `20.00`. Tidak ada izin baru dan tidak ada notifikasi tambahan.
- **Fase 13 — Lokasi & bagikan (selesai):** aksi **+1 Pkt** dan **Catat** dihapus dari kartu Nearby beserta
  callback dan VM `incrementTodayDelivered`. Tombol **Bagikan** (kartu dan detail) mengirim foto dari
  `filesDir/location_photos` lewat `FileProvider` plus teks persis:
  `FOTO / ------- / Nama Penerima : … / Detail/Patokan : … / Maps : … / (baris kosong) / by Trackify.`
  Tidak ada kolom DB, izin, atau field `SavedLocation` baru.
- **Fase 14 — Catatan (selesai):** template Notion bersifat opsional; chip **Kosong** membuka editor blank.
  Foto catatan didukung hingga 5 foto per catatan lewat entity `NotePhoto` / tabel `note_photos` dan
  `MIGRATION_7_8` (DB versi **8**). Foto disalin ke `filesDir/note_photos` dengan awalan `note_` dan dihapus
  bersama catatan. Backup schema `8` mengekspor array `photos` per catatan; import schema `7` tanpa kunci
  itu tetap diterima. Tidak ada perubahan pada `courier_notes`, blok `NoteBlocks`, bottom nav, header, atau tema.
- **Fase 15 — Tema:** dark **Royal Charcoal** (`#25262a → #1e1f22 → #17181a`, hero `#4c4e51 → #1e2023`)
  dan light **Pure Charcoal** (`#fafafa → #f1f1f1 → #e8e8e8`, hero `#3a3a3c → #161617`). Gradien dipasang
  di container tiap screen; `Scaffold` tetap hanya di `MainActivity`, dan tidak ada toggle tema manual.
- **Fase 16 — Typography:** judul `ExtraBold`, angka Home `tnum`, koordinat Lokasi monospaced, judul blok
  Catatan lebih berat dari paragraf. `ScreenTitle` tetap tanpa subtext.
- **Fase 17 — Tombol:** komponen tombol dengan outline `1.dp outlineVariant` di kedua tema, skala spacing
  4/8/12/16/24, shadow diterjemahkan dari CSS ke elevation Compose.
- **Fase 18 — Update & kredit:** bel di header Home mengecek `GET /repos/drezvolx/Trackify/releases/latest`,
  menampilkan dialog versi + tombol unduh/buka GitHub (alur flexible, tanpa auto-install). Blok Dukungan
  menampilkan versi aplikasi, **Drezvolx**, <https://github.com/drezvolx>, dan
  <https://discord.com/users/1182612195419369562>. `credits.md`, struktur folder nested, dan README profesional
  menyusul di fase ini.

Tiap fase wajib: backup sebelum edit → ktlint exit 0 → `:app:assembleDebug :app:testDebugUnitTest` hijau
→ semua tes lulus (baseline 30) → grep kontrak kosong → smoke test perangkat → catatan di `CHANGELOG.md`.
