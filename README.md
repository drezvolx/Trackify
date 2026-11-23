# Trackify

[![CI](https://github.com/drezvolx/Trackify/actions/workflows/ci.yml/badge.svg)](https://github.com/drezvolx/Trackify/actions/workflows/ci.yml)

Aplikasi Android **offline-first** untuk kurir dan pengemudi pengiriman barang. Trackify membantu mencatat paket harian, menghitung pendapatan, menyimpan lokasi pelanggan, dan mengelola catatan lapangan — semuanya tersimpan lokal di perangkat.

## Fitur

- **Home** — status hari ini, paket dibawa/terkirim, pendapatan, target, dan aksi cepat.
- **Rekap** — statistik bulanan, riwayat harian, edit log, carryover, dan ekspor CSV.
- **Lokasi** — titik GPS pelanggan, rute, Maps, bagikan lokasi, dan aksi cepat paket.
- **Catatan** — catatan operasional, pin, taut lokasi, pencarian, dan templat.
- **Setelan** — tarif, bonus, target, pengingat harian, backup/restore JSON, dan reset data.

Navigasi utama: `Home → Rekap → Lokasi → Catatan`.

## Teknologi

Kotlin · Jetpack Compose · Material 3 · MVVM · Coroutines & Flow · Hilt · Room

Modul: `:app`, `:core:domain`, `:core:data`.

## Struktur

```text
app/          UI, navigasi, ViewModel, dan konfigurasi aplikasi
core/domain   model data dan logika bisnis
core/data     Room, repository, preferensi, dan penjadwalan notifikasi
docs/         dokumentasi dan keputusan yang terkunci
```

## Build

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain
```

APK debug ada di:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Dokumentasi

- [Documentation.md](Documentation.md) — gambaran proyek dan detail fitur.
- [docs/LOCKED_DECISIONS.md](docs/LOCKED_DECISIONS.md) — keputusan yang tidak boleh diubah, terutama bottom navigation dan tema.
- [docs/FEATURE_ROADMAP.md](docs/FEATURE_ROADMAP.md) — pemetaan fitur dan rencana pengembangan lanjutan.
- [License.md](License.md) — lisensi penggunaan dan syarat atribusi.

> Baca `LOCKED_DECISIONS.md` sebelum mengubah UI, arsitektur, atau build file.

## Lisensi

Trackify dilisensikan di bawah [MIT License](License.md). Pihak lain boleh menggunakan, memodifikasi, dan mendistribusikan kode ini, dengan syarat notice lisensi dan atribusi copyright tetap disertakan pada salinan atau bagian substantif dari perangkat lunak.

## Catatan

Tema mengikuti pengaturan sistem secara otomatis (light/dark). Aplikasi dikunci dalam orientasi portrait dan tidak memerlukan koneksi internet untuk penggunaan utama.
