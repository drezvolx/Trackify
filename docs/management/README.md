# Manajemen proyek Trackify

Folder ini berisi dokumen operasional untuk memelihara Trackify setelah rilis. Dokumen produk dan kontrak UI tetap
berada di luar folder ini:

- [LOCKED_DECISIONS.md](../LOCKED_DECISIONS.md) — kontrak yang tidak boleh diubah tanpa persetujuan.
- [FEATURE_ROADMAP.md](../FEATURE_ROADMAP.md) — status fase dan rincian fitur.
- [credits.md](../credits.md) — kredit pengembang, font, dan dependensi.

## Struktur

```text
docs/management/
├── README.md                         indeks ini
├── processes/
│   ├── phase-gate.md                 pemeriksaan wajib sebelum commit fase
│   └── release-process.md            langkah bump versi, tag, dan GitHub Release
└── templates/
    └── release-notes.md              template catatan rilis
```

Gunakan checklist di `processes/` setiap kali mengubah kode. Jangan mengubah kontrak di `LOCKED_DECISIONS.md`
tanpa mencatat keputusan baru di bagian §9.
