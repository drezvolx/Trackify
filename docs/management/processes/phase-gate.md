# Gate setiap fase

Setiap perubahan besar wajib lolos pemeriksaan berikut **sebelum** di-commit dan di-push.

1. **Backup** salinan kerja sebelum mengubah file (folder backup lokal tidak ikut di-commit).
2. **CHANGELOG.md** mendapat catatan fase: apa yang ditambahkan, diubah, dan bagaimana diverifikasi.
3. **ktlint 1.8.0** exit `0` untuk seluruh `*.kt` dan `*.kts`, kecuali `backup*/**, trackify_fix/**, **/build/**`.
4. **Build dan tes** hijau:
   ```powershell
   .\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --no-configuration-cache --console=plain
   ```
5. **Semua tes lulus**; jumlah tes dihitung dari `app/build/test-results/testDebugUnitTest/TEST-*.xml`.
6. **Grep kontrak** kosong: tidak ada token pembayaran tunai, identifier lama, `AlertDialog(` langsung, subtext
   header, toggle tema manual, tombol Material3 langsung, `outlineVariant.copy(alpha = ...)`, `shadow(...)`,
   atau spacing di luar skala 4/8/12/16/24 dp. `Scaffold(` hanya di `MainActivity.kt`.
7. **Smoke perangkat**: pasang APK debug, buka kelima tab, coba alur utama yang berubah, dan pastikan logcat tidak
   memuat `FATAL` atau `AndroidRuntime`.
8. **Commit dan push**, lalu tunggu CI `validate` hijau dan catat nomor run di `CHANGELOG.md`.

Jika salah satu gate gagal, perbaiki dan ulangi dari langkah 3. Jangan menaikkan versi rilis sebelum semua fase
selesai dan CI hijau.
