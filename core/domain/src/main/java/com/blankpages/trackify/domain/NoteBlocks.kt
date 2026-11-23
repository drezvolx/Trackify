package com.blankpages.trackify.domain

/** Representasi ringan blok catatan ala Notion. Isi tetap satu field teks di DB. */
data class NoteBlock(val type: Type, val text: String, val lineIndex: Int, val checked: Boolean = false) {
    enum class Type { PARAGRAPH, HEADING, BULLET, TASK }
}

object NoteBlocks {
    private val checkedTask = Regex("^\\[x\\]\\s?(.*)$", RegexOption.IGNORE_CASE)
    private val uncheckedTask = Regex("^\\[ \\]\\s?(.*)$")

    fun parse(content: String): List<NoteBlock> {
        if (content.isBlank()) return emptyList()
        return content.lines().mapIndexed { index, line ->
            val checkedMatch = checkedTask.matchEntire(line)
            val uncheckedMatch = uncheckedTask.matchEntire(line)
            when {
                line.startsWith("# ") -> NoteBlock(NoteBlock.Type.HEADING, line.removePrefix("# "), index)
                line.startsWith("- ") -> NoteBlock(NoteBlock.Type.BULLET, line.removePrefix("- "), index)
                checkedMatch != null -> NoteBlock(NoteBlock.Type.TASK, checkedMatch.groupValues[1], index, true)
                uncheckedMatch != null -> NoteBlock(NoteBlock.Type.TASK, uncheckedMatch.groupValues[1], index)
                else -> NoteBlock(NoteBlock.Type.PARAGRAPH, line, index)
            }
        }
    }

    fun serialize(blocks: List<NoteBlock>): String = blocks.joinToString("\n") { block ->
        when (block.type) {
            NoteBlock.Type.HEADING -> "# ${block.text}"
            NoteBlock.Type.BULLET -> "- ${block.text}"
            NoteBlock.Type.TASK -> if (block.checked) "[x] ${block.text}" else "[ ] ${block.text}"
            NoteBlock.Type.PARAGRAPH -> block.text
        }
    }.trimEnd()
}
