# Proses rilis

Rilis dibuat **setelah** seluruh fase selesai dan CI hijau di commit terakhir.

## 1. Bump versi

Ubah hanya `app/build.gradle.kts` pada `defaultConfig`:

```kotlin
versionCode = 2
versionName = "1.1"
```

Jalankan ulang gate di [phase-gate.md](phase-gate.md), commit, dan push ke `main`.

## 2. Tag dan GitHub Release

- Tag follow `v<versionName>` (contoh: `v1.1.0`).
- Nama rilis: `Trackify v1.1.0`.
- Target tag: commit hasil bump versi yang sudah hijau CI.
- Template deskripsi: [release-notes.md](../templates/release-notes.md).
- Jangan ubah, pindahkan, atau hapus tag dan rilis `v1.0.0`; itu baseline yang sudah dipublikasikan.

## 3. Verifikasi setelah rilis

- `GET https://api.github.com/repos/drezvolx/Trackify/releases/latest` mengembalikan tag rilis baru.
- Bel di Home menampilkan versi terbaru dan membuka halaman rilis/APK tanpa auto-install.
- Blok Dukungan di Setelan menampilkan versi aplikasi yang benar dan tautan Drezvolx/GitHub/Discord.

## 4. Jika perlu perbaikan

Buat commit perbaikan di `main`, ulangi gate, lalu terbitkan tag patch baru (misalnya `v1.1.1`). Jangan menimpa
aset rilis yang sudah diunduh pengguna.
