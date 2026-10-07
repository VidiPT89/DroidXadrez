package com.vidi.droidxadrez.multiplayer

import android.content.Intent
import android.net.Uri
import java.text.Normalizer
import java.time.Instant

/**
 * Keeps the online chat within store content rules: offensive words are masked before display,
 * the opponent can be muted, and abuse can be reported by email. Same lists as web and iOS.
 */
object ChatModeration {
    const val REPORT_EMAIL = "ividi.dev@gmail.com"

    /** Prefix stems (plurals, inflections). PT-PT: "bicha" is a queue and "puto" a kid, so neither. */
    private val blockedStems = listOf(
        "caralh", "foda", "fodas", "fodid", "merda", "paneleir", "panasc", "cabrao", "otario",
        "idiota", "imbecil", "estupid", "atrasad", "mongoloid", "retardad", "filhodaputa",
        "fuck", "shit", "bitch", "cunt", "pussy", "asshole", "bastard", "slut", "whore",
        "retard", "faggot", "nigg", "idiot", "moron", "stupid",
    )

    /** Short words that would hit innocent ones as stems ("Dickens", "cockpit"), so exact only. */
    private val blockedWords = setOf(
        "puta", "putas", "cona", "pila", "corno", "cornos", "fode", "fdp", "crl", "pqp", "vsf", "vtnc",
        "dick", "cock", "fag", "kys", "rape",
    )

    private fun fold(word: String): String =
        Normalizer.normalize(word, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase()
            .filter { it.isLetterOrDigit() }

    /** Replaces each offending word with "•••", leaving the rest of the message intact. */
    fun mask(text: String): String = text.split(" ").joinToString(" ") { word ->
        val folded = fold(word)
        val blocked = folded.isNotEmpty() && (folded in blockedWords || blockedStems.any { folded.startsWith(it) })
        if (blocked) "•••" else word
    }

    /** A pre-filled email to the developer with the evidence needed to act on a report. */
    fun reportIntent(roomCode: String?, opponentName: String, recentMessages: List<String>): Intent {
        val quoted = recentMessages.takeLast(10).joinToString("\n") { "> $it" }.ifEmpty { "(nenhuma)" }
        val body = "Sala: ${roomCode ?: "?"}\nAdversário: $opponentName\nData: ${Instant.now()}\n\n" +
            "Mensagens recentes do adversário:\n$quoted\n\nDescreve o problema:\n"
        return Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORT_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, "Denúncia DroidXadrez — sala ${roomCode ?: "?"}")
            putExtra(Intent.EXTRA_TEXT, body)
        }
    }
}
