package com.shaadow.onecalculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import androidx.lifecycle.lifecycleScope
import android.widget.Button
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import android.view.GestureDetector
import android.view.MotionEvent
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.graphics.Color
import android.view.View
import android.widget.TextView
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import android.app.Activity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.os.Handler
import android.widget.ImageView

class HistoryActivity : AppCompatActivity() {
    private lateinit var adapter: HistoryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var db: HistoryDatabase
    private lateinit var noHistoryFound: TextView
    private var historyJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_history)

        recyclerView = findViewById(R.id.history_recycler)
        recyclerView.layoutManager = LinearLayoutManager(this)
        noHistoryFound = findViewById(R.id.no_history_found)
        db = HistoryDatabase.getInstance(this)

        setupRecyclerView()
        loadHistory()
        setupBackButton()
        setupFloatingActionButton()
        setupClickOutsideToClearFocus()
        setupClearAllButton()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter(
            onItemClick = { /* Optionally handle item click */ },
            onDeleteClick = { historyItem -> deleteHistoryItem(historyItem) }
        )
        recyclerView.adapter = adapter
    }

    private fun loadHistory() {
        historyJob?.cancel()
        historyJob = lifecycleScope.launch {
            db.historyDao().getAllHistory().collect { entities ->
                val items = entities.map {
                    HistoryItem(
                        id = it.id.toLong(),
                        expression = it.expression,
                        result = it.result,
                        timestamp = it.timestamp,
                        source = it.source
                    )
                }
                if (items.isEmpty()) {
                    noHistoryFound.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    noHistoryFound.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    adapter.submitList(items)
                }
            }
        }
    }

    private fun deleteHistoryItem(historyItem: HistoryItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            db.historyDao().deleteById(historyItem.id.toInt())
        }
    }

    private fun setupBackButton() {
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }
    }

    private fun setupFloatingActionButton() {
        val fabCalculator = findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.fab_calculator)
        fabCalculator.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }
    }

    private fun setupClickOutsideToClearFocus() {
        // Set up click listener on the root view to clear focus when clicking outside
        findViewById<View>(android.R.id.content).setOnClickListener {
            val textInputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.textInputLayout)
            val searchInput = textInputLayout.editText
            if (searchInput?.hasFocus() == true) {
                searchInput.clearFocus()
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
            }
        }
        // Remove any gesture logic
    }

    private fun setupSearch() {
        val textInputLayout = findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.textInputLayout)
        val searchInput = textInputLayout?.editText ?: return

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                adapter.setSearchQuery(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        
        // Handle clear button click
        textInputLayout.setEndIconOnClickListener {
            searchInput.text?.clear()
            searchInput.clearFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
            adapter.setSearchQuery("")
        }
    }

    private fun setupClearAllButton() {
        findViewById<View>(R.id.btn_clear_all).setOnClickListener {
            android.app.AlertDialog.Builder(this)
                .setTitle("Clear All History")
                .setMessage("Are you sure you want to delete all history?")
                .setPositiveButton("Yes") { _, _ ->
                    val db = HistoryDatabase.getInstance(this)
                    lifecycleScope.launch {
                        db.historyDao().clearAll()
                        runOnUiThread { recreate() }
                    }
                }
                .setNegativeButton("No", null)
                .show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        historyJob?.cancel()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
        super.onBackPressed()
    }
} 