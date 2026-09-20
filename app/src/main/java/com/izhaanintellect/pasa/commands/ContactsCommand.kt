package com.izhaanintellect.pasa.commands

import android.content.Context
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves contacts from the device phonebook.
 */
@Singleton
class ContactsCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/contacts"
    override val description = "List device contacts"
    override val usage = "/contacts [search_term]"

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val searchQuery = args.joinToString(" ").trim()
        val maxResults = 50

        return try {
            val contacts = mutableListOf<Pair<String, String>>()
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            val selection = if (searchQuery.isNotBlank()) {
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            } else null

            val selectionArgs = if (searchQuery.isNotBlank()) {
                arrayOf("%$searchQuery%")
            } else null

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext() && contacts.size < maxResults) {
                    val name = it.getString(nameIdx) ?: "Unknown"
                    val number = it.getString(numIdx) ?: ""
                    contacts.add(name to number)
                }
            }

            if (contacts.isEmpty()) {
                val suffix = if (searchQuery.isNotBlank()) " matching \"$searchQuery\"" else ""
                return CommandResult(
                    success = true,
                    message = "📇 <b>Contacts</b>\n━━━━━━━━━━━━━━━━━━━━\n<i>No contacts found$suffix.</i>"
                )
            }

            val entries = contacts.mapIndexed { i, (name, number) ->
                "${i + 1}. <b>$name</b>\n   📞 <code>$number</code>"
            }

            val header = if (searchQuery.isNotBlank()) {
                "📇 <b>Contacts</b> — \"$searchQuery\""
            } else {
                "📇 <b>Contacts</b>"
            }

            val message = "$header (${contacts.size})\n" +
                    "━━━━━━━━━━━━━━━━━━━━\n" +
                    entries.joinToString("\n")

            CommandResult(success = true, message = message)
        } catch (e: SecurityException) {
            CommandResult(
                success = false,
                message = "📇 <b>Contacts Error</b>\n━━━━━━━━━━━━━━━━━━━━\n⚠️ READ_CONTACTS permission not granted."
            )
        } catch (e: Exception) {
            CommandResult(
                success = false,
                message = "📇 <b>Contacts Error</b>\n━━━━━━━━━━━━━━━━━━━━\n❌ ${e.message}"
            )
        }
    }
}
