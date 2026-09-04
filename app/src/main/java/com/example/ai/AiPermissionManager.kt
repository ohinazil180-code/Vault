package com.example.ai

/**
 * AEGIS AI Permission Levels:
 * 0 = Chat only (Default)
 * 1 = User-selected text
 * 2 = User-selected document
 * 3 = User-selected folder/content
 * 4 = Temporary vault context
 *
 * CRITICAL SECURITY INVARIANT:
 * Vault PIN, master key, recovery secret, biometric info, and encryption keys
 * MUST NEVER be sent to the AI backend or external services.
 */
object AiPermissionManager {
    const val LEVEL_0_CHAT_ONLY = 0
    const val LEVEL_1_SELECTED_TEXT = 1
    const val LEVEL_2_SELECTED_DOC = 2
    const val LEVEL_3_SELECTED_FOLDER = 3
    const val LEVEL_4_TEMP_VAULT = 4

    data class Disclosure(
        val level: Int,
        val title: String,
        val description: String,
        val previewSnippet: String,
        val warning: String = "Vault master keys, PINs, recovery secrets, and unselected files remain strictly isolated and inaccessible."
    )

    fun createDisclosureForNote(noteTitle: String, noteContent: String): Disclosure {
        val snippet = if (noteContent.length > 150) noteContent.take(150) + "..." else noteContent
        return Disclosure(
            level = LEVEL_1_SELECTED_TEXT,
            title = "Share Note with AEGIS AI?",
            description = "You are granting temporary Level 1 permission for AEGIS AI to analyze the text of note '$noteTitle'.",
            previewSnippet = snippet
        )
    }

    fun createDisclosureForFile(fileName: String, mimeType: String, sizeBytes: Long): Disclosure {
        val sizeKb = (sizeBytes / 1024).coerceAtLeast(1)
        return Disclosure(
            level = LEVEL_2_SELECTED_DOC,
            title = "Share Document with AEGIS AI?",
            description = "You are granting temporary Level 2 permission for AEGIS AI to process '$fileName' ($mimeType, ~$sizeKb KB).",
            previewSnippet = "File: $fileName | Type: $mimeType | Size: $sizeKb KB"
        )
    }

    fun sanitizeText(input: String): String {
        // Redact any potential accidental leakage of PIN or master key format
        return input.replace(Regex("(?i)(pin|password|secret|masterkey)\\s*[:=]\\s*\\S+"), "$1: [REDACTED_BY_AEGIS_SHIELD]")
    }
}
