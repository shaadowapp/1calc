package com.shaadow.onecalculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TodoViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = TodoDatabase.getInstance(app).todoDao()
    val todos: StateFlow<List<TodoEntity>> = dao.getAllTodos().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun addTodo(text: String) = viewModelScope.launch {
        dao.insert(TodoEntity(text = text))
    }

    fun updateTodo(todo: TodoEntity) = viewModelScope.launch {
        dao.update(todo)
    }

    fun deleteTodo(todo: TodoEntity) = viewModelScope.launch {
        dao.delete(todo)
    }
} 