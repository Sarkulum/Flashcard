package com.example.flashcard

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.ArrayList

object CsvUtils {

    fun readCsv(context: Context, uri: Uri): List<Flashcard> {
        val cards = ArrayList<Flashcard>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                cards.addAll(readCsv(inputStream))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return cards
    }

    fun readCsv(inputStream: InputStream): List<Flashcard> {
        val cards = ArrayList<Flashcard>()
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val content = reader.readText()
            val rows = parseCsv(content)
            for (row in rows) {
                if (row.size >= 2) {
                    cards.add(Flashcard(row[0].trim(), row[1].trim()))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return cards
    }

    private fun parseCsv(content: String): List<List<String>> {
        val rows = ArrayList<List<String>>()
        var currentRow = ArrayList<String>()
        val currentField = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < content.length) {
            val c = content[i]
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < content.length && content[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(c)
                }
            } else {
                when (c) {
                    '"' -> inQuotes = true
                    ',' -> {
                        currentRow.add(currentField.toString())
                        currentField.setLength(0)
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString())
                        rows.add(currentRow)
                        currentRow = ArrayList()
                        currentField.setLength(0)
                    }
                    '\r' -> { /* ignore */ }
                    else -> currentField.append(c)
                }
            }
            i++
        }
        // Handle last row if no newline at end
        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
             currentRow.add(currentField.toString())
             rows.add(currentRow)
        }
        
        // Filter out empty rows
        return rows.filter { it.isNotEmpty() && (it.size > 1 || it[0].isNotBlank()) }
    }

    fun writeCsv(context: Context, uri: Uri, cards: List<Flashcard>) {
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                writeCsv(outputStream, cards)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun writeCsv(outputStream: OutputStream, cards: List<Flashcard>) {
        try {
            val writer = OutputStreamWriter(outputStream)
            for (card in cards) {
                writer.append(escape(card.question))
                writer.append(",")
                writer.append(escape(card.answer))
                writer.append("\n")
            }
            writer.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun escape(text: String): String {
        var result = text.replace("\"", "\"\"")
        if (result.contains(",") || result.contains("\n") || result.contains("\"")) {
            result = "\"$result\""
        }
        return result
    }
}
