package com.shaadow.onecalculator

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import android.widget.Button
import android.view.MenuItem
import android.widget.PopupMenu
import android.content.Intent

class TodoActivity : AppCompatActivity() {
    private val viewModel: TodoViewModel by viewModels()
    private lateinit var todoAdapter: TodoAdapter
    // private lateinit var sharedPreferences: android.content.SharedPreferences
    // private val PREFS_KEY = "todo_list"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_todo)
        supportActionBar?.hide()

        // sharedPreferences = getSharedPreferences("todo_prefs", Context.MODE_PRIVATE)
        // val todoList = loadTodos().toMutableList()

        val recyclerView = findViewById<RecyclerView>(R.id.todo_recycler)
        val emptyView = findViewById<TextView>(R.id.empty_todo_list)
        val fab = findViewById<FloatingActionButton>(R.id.fab_add_todo)
        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        val btnSettings = findViewById<ImageButton>(R.id.btn_settings)
        btnSettings.setOnClickListener { showSettingsPopupMenu(it) }

        todoAdapter = TodoAdapter(
            onCheck = { todo ->
                viewModel.updateTodo(todo.copy(done = !todo.done))
            },
            onDelete = { todo ->
                viewModel.deleteTodo(todo)
            },
            onEdit = { todo ->
                showEditTodoDialog(todo)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = todoAdapter

        lifecycleScope.launchWhenStarted {
            viewModel.todos.collectLatest { todos ->
                todoAdapter.submitList(todos)
                emptyView.visibility = if (todos.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        fab.setOnClickListener {
            showAddTodoDialog()
        }
        btnBack.setOnClickListener { finish() }
    }

    private fun showAddTodoDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_todo, null)
        val input = dialogView.findViewById<EditText>(R.id.edit_task_text)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm)
        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        title.text = getString(R.string.add_todo)
        btnConfirm.text = getString(R.string.add_todo)
        val dialog = AlertDialog.Builder(this, R.style.DialogStyle_Todo)
            .setView(dialogView)
            .create()
        btnConfirm.setOnClickListener {
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.addTodo(text)
                dialog.dismiss()
            } else {
                input.error = getString(R.string.add_todo)
            }
        }
        dialog.show()
    }

    private fun showEditTodoDialog(todo: TodoEntity) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_todo, null)
        val input = dialogView.findViewById<EditText>(R.id.edit_task_text)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm)
        val title = dialogView.findViewById<TextView>(R.id.dialog_title)
        title.text = getString(R.string.edit)
        btnConfirm.text = getString(R.string.edit)
        input.setText(todo.text)
        val dialog = AlertDialog.Builder(this, R.style.DialogStyle_Todo)
            .setView(dialogView)
            .create()
        btnConfirm.setOnClickListener {
            val newText = input.text.toString().trim()
            if (newText.isNotEmpty() && newText != todo.text) {
                viewModel.updateTodo(todo.copy(text = newText))
                dialog.dismiss()
            } else {
                input.error = getString(R.string.edit)
            }
        }
        dialog.show()
    }

    private fun showSettingsPopupMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(0, 1, 0, getString(R.string.settings))
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                1 -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun updateEmptyView(todoList: List<TodoItem>, emptyView: TextView) {
        emptyView.visibility = if (todoList.isEmpty()) View.VISIBLE else View.GONE
    }

    // private fun saveTodos(todoList: List<TodoItem>) {
    //     val set = todoList.map { "${it.text}||${it.done}" }.toSet()
    //     sharedPreferences.edit().putStringSet(PREFS_KEY, set).apply()
    // }

    // private fun loadTodos(): List<TodoItem> {
    //     val set = sharedPreferences.getStringSet(PREFS_KEY, emptySet()) ?: emptySet()
    //     return set.map {
    //         val parts = it.split("||")
    //         TodoItem(parts[0], parts.getOrNull(1)?.toBoolean() ?: false)
    //     }
    // }

    data class TodoItem(var text: String, var done: Boolean)

    inner class TodoAdapter(
        private var items: List<TodoEntity> = emptyList(),
        val onCheck: (TodoEntity) -> Unit,
        val onDelete: (TodoEntity) -> Unit,
        val onEdit: (TodoEntity) -> Unit
    ) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {
        fun submitList(newItems: List<TodoEntity>) {
            items = newItems
            notifyDataSetChanged()
        }
        inner class TodoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val text: TextView = view.findViewById(R.id.todo_text)
            val check: ImageButton = view.findViewById(R.id.btn_check)
            val delete: ImageButton = view.findViewById(R.id.btn_delete)
            val edit: ImageButton = view.findViewById(R.id.btn_edit)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_todo, parent, false)
            return TodoViewHolder(view)
        }
        override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
            val item = items[position]
            holder.text.text = item.text
            holder.text.paint.isStrikeThruText = item.done
            if (item.done) {
                holder.check.setImageResource(R.drawable.ic_check_circle)
                holder.check.setColorFilter(ContextCompat.getColor(this@TodoActivity, R.color.green_4caf50))
                holder.text.alpha = 0.5f
            } else {
                holder.check.setImageResource(R.drawable.ic_circle)
                holder.check.setColorFilter(ContextCompat.getColor(this@TodoActivity, R.color.white_ffffff))
                holder.text.alpha = 1.0f
            }
            holder.check.setOnClickListener { onCheck(item) }
            holder.delete.setOnClickListener { onDelete(item) }
            holder.edit.setOnClickListener { onEdit(item) }
        }
        override fun getItemCount() = items.size
    }
} 