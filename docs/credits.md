# Kredit Trackify

Dokumen ini merangkum pihak dan komponen yang membuat Trackify dapat dibangun. Kredit ini juga tampil ringkas di
**Setelan → Dukung → Kredit & versi** di dalam aplikasi.

## Pengembang

- **Drezvolx** — pengembang dan pemilik proyek.
  - GitHub: <https://github.com/drezvolx>
  - Discord: <https://discord.com/users/1182612195419369562>
  - Repositori: <https://github.com/drezvolx/Trackify>

## Aset

- **Geist** — font yang dipakai untuk seluruh tipografi (`geist_regular`, `geist_medium`, `geist_semibold`,
  `geist_bold`). Aset font menggunakan SIL Open Font License 1.1.
  - <https://github.com/vercel/geist-font>

## Komponen perangkat lunak

- **Kotlin** dan **Jetpack Compose** — bahasa dan toolkit UI.
- **Material 3** — sistem desain, warna, dan tipografi.
- **AndroidX** (Activity Compose, Core KTX, Lifecycle, Profile Installer) — fondasi aplikasi Android.
- **Room** — penyimpanan lokal.
- **Hilt** — dependency injection.
- **Kotlin Coroutines dan Flow** — konkurensi dan aliran data.
- **JUnit** dan **org.json** — pengujian unit dan parsing payload rilis GitHub.

## Lisensi aplikasi

Trackify dirilis di bawah [MIT License](../License.md). Atribusi copyright tetap disertakan pada salinan atau
bagian substantif perangkat lunak.

## Tautan rilis

Versi terbaru dapat dilihat di <https://github.com/drezvolx/Trackify/releases>. Tombol bel di Home memeriksa
endpoint publik tersebut dan hanya membuka browser; Trackify tidak memasang pembaruan secara otomatis.
