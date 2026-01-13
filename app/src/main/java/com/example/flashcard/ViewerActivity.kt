package com.example.flashcard

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File
import java.util.ArrayList

class ViewerActivity : AppCompatActivity() {

    private val cards = ArrayList<Flashcard>()
    private var currentIndex = 0
    private var isShowingQuestion = true

    private lateinit var tvCardContent: TextView
    private lateinit var btnPrev: ImageButton
    private lateinit var btnNext: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        // Immersive Sticky Mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        val uriString = intent.getStringExtra("FILE_URI")
        if (uriString != null) {
            val uri = android.net.Uri.parse(uriString)
            cards.addAll(CsvUtils.readCsv(this, uri))
        } else {
             // Fallback for backward compatibility or direct file path if needed
             val filePath = intent.getStringExtra("FILE_PATH")
             if (filePath != null) {
                 val file = File(filePath)
                 if (file.exists()) {
                     cards.addAll(CsvUtils.readCsv(file.inputStream()))
                 }
             }
        }

        tvCardContent = findViewById(R.id.tvCardContent)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)

        findViewById<View>(R.id.cardView).setOnClickListener {
            flipCard()
        }

        btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                isShowingQuestion = true
                updateCard()
            }
        }

        btnNext.setOnClickListener {
            if (currentIndex < cards.size - 1) {
                currentIndex++
                isShowingQuestion = true
                updateCard()
            }
        }

        if (cards.isNotEmpty()) {
            updateCard()
        } else {
            tvCardContent.text = "No cards available"
            btnPrev.visibility = View.INVISIBLE
            btnNext.visibility = View.INVISIBLE
        }
    }

    private fun updateCard() {
        val card = cards[currentIndex]
        tvCardContent.text = if (isShowingQuestion) card.question else card.answer
        
        btnPrev.visibility = if (currentIndex > 0) View.VISIBLE else View.INVISIBLE
        btnNext.visibility = if (currentIndex < cards.size - 1) View.VISIBLE else View.INVISIBLE
    }

    private fun flipCard() {
        if (cards.isNotEmpty()) {
            isShowingQuestion = !isShowingQuestion
            updateCard()
        }
    }
}
