package com.blankpages.trackify.domain

data class NoteTemplate(val title: String, val content: String)

/** Templat catatan yang sering dipakai kurir. */
val noteTemplates = listOf(
    NoteTemplate(
        title = "Titipan",
        content = "TITIPAN\n" +
            "Pelanggan: \n" +
            "Penerima: \n" +
            "No. HP penerima: \n" +
            "Batas waktu: \n" +
            "Keterangan: "
    ),
    NoteTemplate(
        title = "Kendala Kirim",
        content = "KENDALA KIRIM\n" +
            "Lokasi: \n" +
            "Penyebab: \n" +
            "Status: Menunggu konfirmasi\n" +
            "Tindak lanjut: "
    )
)
