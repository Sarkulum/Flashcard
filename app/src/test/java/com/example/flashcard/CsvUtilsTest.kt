package com.example.flashcard

import org.junit.Test
import org.junit.Assert.*
import java.io.File

class CsvUtilsTest {
    @Test
    fun testParseAttachedCsv() {
        val file = File("/home/valerie/AndroidStudioProjects/Flashcard/flashcard.csv")
        assertTrue("flashcard.csv should exist at " + file.absolutePath, file.exists())

        val cards = CsvUtils.readCsv(file.inputStream())
        
        println("Loaded ${cards.size} cards")
        
        // Check Line 1 (Simple)
        val card0 = cards[0]
        println("Card 0 Question: '${card0.question}'")
        println("Card 0 Answer: '${card0.answer}'")

        // 1:Wer haftet ...,Der Inhaber ...
        // Expected: Question="Wer haftet...", Answer="Der Inhaber..." (1: is line number in display)
        assertTrue("Question should start with Wer haftet, but was '${card0.question}'", card0.question.startsWith("Wer haftet"))
        assertTrue("Answer should start with Der Inhaber, but was '${card0.answer}'", card0.answer.startsWith("Der Inhaber"))

        // Check Line 4 (Quoted with comma inside)
        // 4:"Was ist ..., ... ?","Ihre ..." -> Content: "Was ...","Ihre ..."
        val card3 = cards[3]
        
        println("Card 3 Question: ${card3.question}")
        println("Card 3 Answer: ${card3.answer}")
        
        // My parser strips quotes.
        assertFalse("Should NOT contain '4:' prefix", card3.question.startsWith("4:"))
        assertTrue("Should contain text inside quotes", card3.question.contains("Was ist der Hauptnachteil"))
        assertTrue("Should preserve comma inside quotes", card3.question.contains("Einzelunternehmung, der"))
        
        // Answer should be parsed correctly too
        assertTrue("Answer should match", card3.answer.contains("Ihre mangelnde Kapitalstärke"))
        
        assertEquals(60, cards.size)
    }
}
