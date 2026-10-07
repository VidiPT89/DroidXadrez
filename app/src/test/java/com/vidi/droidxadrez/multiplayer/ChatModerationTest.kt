package com.vidi.droidxadrez.multiplayer

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatModerationTest {
    @Test fun masksOffensiveWordsOnly() {
        assertEquals("és um •••", ChatModeration.mask("és um IDIOTA!"))
        assertEquals("what the ••• hell", ChatModeration.mask("what the fucking hell"))
        assertEquals("•••", ChatModeration.mask("Estúpido"))
    }

    @Test fun leavesInnocentWordsAlone() {
        // PT-PT: "bicha" is a queue and "puto" a kid; "Dickens"/"cockpit" only share a prefix.
        for (text in listOf("boa jogada!", "estou na bicha", "o puto ganhou", "Dickens e cockpit", "xeque-mate")) {
            assertEquals(text, ChatModeration.mask(text))
        }
    }
}
