# Trackify

[![CI](https://github.com/drezvolx/Trackify/actions/workflows/ci.yml/badge.svg)](https://github.com/drezvolx/Trackify/actions/workflows/ci.yml)
[![Release](https://img.shields.io/github/v/release/drezvolx/Trackify)](https://github.com/drezvolx/Trackify/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](License.md)

**Trackify** adalah aplikasi Android offline-first untuk kurir dan pengemudi pengiriman barang. Catat paket harian,
hitung pendapatan, simpan titik lokasi pelanggan, dan kelola catatan lapangan — semuanya tersimpan lokal di
perangkat. Koneksi internet hanya dipakai untuk membuka Maps dan mengecek rilis terbaru.

## Fitur utama

| Tab | Fungsi |
|---|---|
| **Home** | Status hari ini, paket dibawa/terkirim, penghasilan, target, aksi cepat, dan pengecek pembaruan. |
| **Rekap** | Statistik bulanan, riwayat harian, editor log, carryover, target, dan ekspor CSV. |
| **Lokasi** | Titik GPS, akurasi dan umur fix, navigasi Maps, foto, serta bagikan detail lokasi. |
| **Catatan** | Catatan operasional, pin, templat, hingga lima foto per catatan, dan tautan lokasi. |
| **Setelan** | Tarif, mode gaji, target, jam pengingat, backup/restore JSON, QRIS dukungan, dan kredit. |

Navigasi utama: `HOME → REKAP → LOKASI → CATATAN`. Setelan dibuka dari ikon di Home.

## Arsitektur

```text
app/                  UI Compose, navigasi, ViewModel, dan pemeriksa rilis GitHub
core/domain/          model data, kalkulasi gaji, dan aturan bisnis murni
core/data/            Room (DB v8), repository, preferensi, dan penjadwalan notifikasi
baselineprofile/      baseline profile untuk cold start
docs/                 keputusan terkunci, roadmap, kredit, dan panduan manajemen
```

Stack: Kotlin · Jetpack Compose · Material 3 · MVVM · Coroutines & Flow · Hilt `2.60.1` · Room · R8 default ·
Baseline Profile. Hilt tidak boleh di-downgrade, dan modul tidak ditambahkan tanpa keputusan baru.

## Membangun

Prasyarat: JDK 21, Android SDK platform 37, dan Gradle wrapper yang disertakan.

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain
```

APK debug: `app/build/outputs/apk/debug/app-debug.apk`.

Untuk pemeriksaan gaya:

```powershell
java -jar ktlint-cli-1.8.0-all.jar '**/*.kt' '**/*.kts' \
  '!backup*/**' '!trackify_fix/**' '!**/build/**'
```

## Rilis

Rilis terbaru: <https://github.com/drezvolx/Trackify/releases>. Tombol bel di Home memeriksa
`GET /repos/drezvolx/Trackify/releases/latest`, menampilkan versi di dialog, lalu membuka halaman rilis atau
mengunduh APK bila tersedia. **Trackify tidak memasang pembaruan secara otomatis.**

Proses bump versi, tag, dan penerbitan rilis ada di
[docs/management/processes/release-process.md](docs/management/processes/release-process.md).

## Dokumentasi

- [Documentation.md](Documentation.md) — gambaran proyek dan detail fitur.
- [CHANGELOG.md](CHANGELOG.md) — catatan per fase dan nomor CI.
- [docs/LOCKED_DECISIONS.md](docs/LOCKED_DECISIONS.md) — kontrak UI/build; baca sebelum mengubah kode.
- [docs/FEATURE_ROADMAP.md](docs/FEATURE_ROADMAP.md) — status dan rincian fase.
- [docs/credits.md](docs/credits.md) — kredit pengembang, font, dan dependensi.
- [docs/management/](docs/management/) — checklist fase, proses rilis, dan template catatan rilis.

## Dukungan

Dikembangkan oleh **Drezvolx** — <https://github.com/drezvolx> ·
<https://discord.com/users/1182612195419369562>. QRIS dukungan tersedia di tab Setelan.

## Lisensi

Trackify dirilis di bawah [MIT License](License.md). Atribusi copyright dan notice lisensi wajib tetap disertakan
pada salinan atau bagian substantif perangkat lunak. Font Geist menggunakan SIL Open Font License 1.1; rinciannya
ada di [docs/credits.md](docs/credits.md).

> Tema mengikuti pengaturan sistem secara otomatis. Aplikasi dikunci portrait dan tetap sepenuhnya fungsional
> tanpa internet.
