package com.izhaanintellect.pasa.bot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CommandParserTest {

    private val parser = CommandParser()

    private fun createUpdate(text: String, chatId: Long = 123456L, firstName: String = "Alice"): Update {
        return Update(
            updateId = 1L,
            message = TelegramMessage(
                messageId = 100L,
                chat = Chat(id = chatId, type = "private", firstName = firstName),
                date = System.currentTimeMillis() / 1000,
                text = text,
                from = From(id = chatId, firstName = firstName)
            )
        )
    }

    @Test
    fun parse_standardSlashCommand() {
        val update = createUpdate("/locate")
        val parsed = parser.parse(update)
        assertNotNull(parsed)
        assertEquals("/locate", parsed?.command)
        assertEquals(emptyList<String>(), parsed?.args)
        assertEquals(123456L, parsed?.chatId)
    }

    @Test
    fun parse_slashCommandWithBotUsername() {
        val update = createUpdate("/lock@Pas_agent_bot 1234")
        val parsed = parser.parse(update)
        assertNotNull(parsed)
        assertEquals("/lock", parsed?.command)
        assertEquals(listOf("1234"), parsed?.args)
    }

    @Test
    fun parse_naturalLanguageKeywords() {
        val statusUpdate = createUpdate("check battery status")
        val parsedStatus = parser.parse(statusUpdate)
        assertNotNull(parsedStatus)
        assertEquals("/status", parsedStatus?.command)

        val ringUpdate = createUpdate("trigger siren now")
        val parsedRing = parser.parse(ringUpdate)
        assertNotNull(parsedRing)
        assertEquals("/ring", parsedRing?.command)
    }

    @Test
    fun parse_unrecognizedTextReturnsNull() {
        val update = createUpdate("random hello text")
        val parsed = parser.parse(update)
        assertNull(parsed)
    }
}
