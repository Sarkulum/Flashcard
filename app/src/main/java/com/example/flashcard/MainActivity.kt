package com.example.flashcard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SetAdapter
    private val sets = ArrayList<UriPair>()
    private var currentDirUri: Uri? = null

    data class UriPair(val name: String, val uri: Uri)

    private val directoryPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            saveStorageUri(uri)
            currentDirUri = uri
            loadSets()
        }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { importCsv(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Immersive Mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = SetAdapter(sets) { pair -> showOptionsDialog(pair) }
        recyclerView.adapter = adapter

        findViewById<ExtendedFloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showCreateDialog()
        }

        findViewById<FloatingActionButton>(R.id.fabImport).setOnClickListener {
            importLauncher.launch("text/comma-separated-values")
        }

        loadStorageUri()
    }

    override fun onResume() {
        super.onResume()
        loadSets()
    }

    private fun loadStorageUri() {
        val prefs = getSharedPreferences("FlashcardPrefs", Context.MODE_PRIVATE)
        val uriString = prefs.getString("storage_uri", null)
        if (uriString != null) {
            currentDirUri = Uri.parse(uriString)
        }
        // If null, we default to internal filesDir (handled in loadSets logic)
    }

    private fun saveStorageUri(uri: Uri) {
        val prefs = getSharedPreferences("FlashcardPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("storage_uri", uri.toString()).apply()
    }

    private fun loadSets() {
        sets.clear()
        if (currentDirUri != null) {
            try {
                val dir = DocumentFile.fromTreeUri(this, currentDirUri!!)
                dir?.listFiles()?.forEach { file ->
                    if (file.name?.endsWith(".csv") == true) {
                        sets.add(UriPair(file.name!!.removeSuffix(".csv"), file.uri))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback or error handling
            }
        } else {
            // Default internal storage
            val files = filesDir.listFiles { _, name -> name.endsWith(".csv") }
            files?.forEach { file ->
                sets.add(UriPair(file.nameWithoutExtension, Uri.fromFile(file)))
            }
        }
        adapter.notifyDataSetChanged()
    }

    private fun showCreateDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_create_set, null)
        val etSetName = view.findViewById<EditText>(R.id.etSetName)

        AlertDialog.Builder(this)
            .setView(view)
            .setPositiveButton("Create") { _, _ ->
                val name = etSetName.text.toString()
                if (name.isNotBlank()) {
                    createSet(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createSet(name: String) {
        val fileName = if (name.endsWith(".csv")) name else "$name.csv"
        
        if (currentDirUri != null) {
             val dir = DocumentFile.fromTreeUri(this, currentDirUri!!)
             val file = dir?.createFile("text/csv", fileName)
             // Some providers append extension automatically, some don't. 
             // Ideally we check if exists.
             if (file != null) {
                 openEditor(file.uri)
             }
        } else {
            val file = File(filesDir, fileName)
            if (!file.exists()) {
                file.createNewFile()
            }
            openEditor(Uri.fromFile(file))
        }
    }
    
    private fun openEditor(uri: Uri) {
        val intent = Intent(this, EditSetActivity::class.java)
        intent.putExtra("FILE_URI", uri.toString())
        startActivity(intent)
    }

    private fun importCsv(uri: Uri) {
        // Import means copy to current storage
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val name = "Imported_${System.currentTimeMillis()}.csv"
                
                if (currentDirUri != null) {
                    val dir = DocumentFile.fromTreeUri(this, currentDirUri!!)
                    val newFile = dir?.createFile("text/csv", name)
                    if (newFile != null) {
                        contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                } else {
                    val file = File(filesDir, name)
                    FileOutputStream(file).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                loadSets()
                Toast.makeText(this, "Imported successfully", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showOptionsDialog(pair: UriPair) {
        val options = arrayOf("View", "Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle(pair.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> { // View
                        val intent = Intent(this, ViewerActivity::class.java)
                        intent.putExtra("FILE_URI", pair.uri.toString())
                        startActivity(intent)
                    }
                    1 -> { // Edit
                        openEditor(pair.uri)
                    }
                    2 -> { // Delete
                        if (currentDirUri != null) {
                             DocumentFile.fromSingleUri(this, pair.uri)?.delete()
                        } else {
                             File(pair.uri.path ?: "").delete()
                        }
                        loadSets()
                    }
                }
            }
            .show()
    }

    // Menu for changing storage location
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Change Storage Location").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            directoryPicker.launch(null)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class SetAdapter(private val sets: List<UriPair>, private val onClick: (UriPair) -> Unit) :
        RecyclerView.Adapter<SetAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvSetName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_set, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val pair = sets[position]
            holder.tvName.text = pair.name
            holder.itemView.setOnClickListener { onClick(pair) }
        }

        override fun getItemCount() = sets.size
    }
}
