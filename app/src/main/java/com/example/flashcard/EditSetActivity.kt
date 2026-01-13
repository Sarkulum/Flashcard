package com.example.flashcard

import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.util.ArrayList

class EditSetActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CardAdapter
    private val cards = ArrayList<Flashcard>()
    private var currentUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_set)

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        val uriString = intent.getStringExtra("FILE_URI")
        if (uriString != null) {
            currentUri = Uri.parse(uriString)
        } else {
            val filePath = intent.getStringExtra("FILE_PATH")
            if (filePath != null) {
                currentUri = Uri.fromFile(File(filePath))
            }
        }

        currentUri?.let {
            cards.addAll(CsvUtils.readCsv(this, it))
            title = getFileName(it)
        }

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CardAdapter(cards) { index -> showEditDialog(index) }
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fabAddCard).setOnClickListener {
            showEditDialog(-1)
        }
    }

    private fun getFileName(uri: Uri): String {
        return if (uri.scheme == "content") {
            DocumentFile.fromSingleUri(this, uri)?.name ?: "Unknown"
        } else {
            File(uri.path ?: "").nameWithoutExtension
        }
    }

    override fun onPause() {
        super.onPause()
        currentUri?.let { CsvUtils.writeCsv(this, it, cards) }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Rename Set").setIcon(android.R.drawable.ic_menu_edit).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            showRenameDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showRenameDialog() {
        val input = EditText(this)
        input.hint = "New Name"
        input.setText(title)
        AlertDialog.Builder(this)
            .setTitle("Rename Set")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString()
                if (newName.isNotBlank()) {
                    renameSet(newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renameSet(newName: String) {
        val uri = currentUri ?: return
        val finalName = if (newName.endsWith(".csv")) newName else "$newName.csv"

        if (uri.scheme == "content") {
             try {
                 val doc = DocumentFile.fromSingleUri(this, uri)
                 doc?.renameTo(finalName)
                 title = finalName.removeSuffix(".csv")
             } catch (e: Exception) {
                 e.printStackTrace()
             }
        } else {
             val file = File(uri.path ?: "")
             val newFile = File(file.parent, finalName)
             if (file.renameTo(newFile)) {
                 currentUri = Uri.fromFile(newFile)
                 title = newFile.nameWithoutExtension
             }
        }
    }

    private fun showEditDialog(index: Int) {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_card, null)
        val etQuestion = view.findViewById<EditText>(R.id.etQuestion)
        val etAnswer = view.findViewById<EditText>(R.id.etAnswer)

        if (index >= 0) {
            etQuestion.setText(cards[index].question)
            etAnswer.setText(cards[index].answer)
        }

        val builder = AlertDialog.Builder(this)
            .setTitle(if (index >= 0) "Edit Card" else "New Card")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val q = etQuestion.text.toString()
                val a = etAnswer.text.toString()
                if (index >= 0) {
                    cards[index].question = q
                    cards[index].answer = a
                    adapter.notifyItemChanged(index)
                } else {
                    cards.add(Flashcard(q, a))
                    adapter.notifyItemInserted(cards.size - 1)
                }
            }
            .setNegativeButton("Cancel", null)

        if (index >= 0) {
            builder.setNeutralButton("Delete") { _, _ ->
                cards.removeAt(index)
                adapter.notifyItemRemoved(index)
            }
        }
        
        builder.show()
    }

    class CardAdapter(private val cards: List<Flashcard>, private val onClick: (Int) -> Unit) :
        RecyclerView.Adapter<CardAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvQuestion: TextView = view.findViewById(R.id.tvQuestion)
            val tvAnswer: TextView = view.findViewById(R.id.tvAnswer)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
            // Use the item layout, maybe improve it later?
            val layout = R.layout.item_card_edit
            val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val card = cards[position]
            holder.tvQuestion.text = card.question
            holder.tvAnswer.text = card.answer
            holder.itemView.setOnClickListener { onClick(position) }
        }

        override fun getItemCount() = cards.size
    }
}
