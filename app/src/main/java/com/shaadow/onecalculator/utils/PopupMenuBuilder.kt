package com.shaadow.onecalculator.utils

import android.content.Context
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.annotation.DrawableRes

class PopupMenuBuilder(private val context: Context, private val anchor: View) {
    data class Item(
        val id: Int,
        val title: String,
        @DrawableRes val iconRes: Int? = null,
        val onClick: (() -> Boolean)? = null
    )

    private val items = mutableListOf<Item>()
    private var onDismiss: (() -> Unit)? = null

    fun addItem(item: Item): PopupMenuBuilder {
        items.add(item)
        return this
    }

    fun addItems(vararg items: Item): PopupMenuBuilder {
        this.items.addAll(items)
        return this
    }

    fun setOnDismissListener(listener: () -> Unit): PopupMenuBuilder {
        onDismiss = listener
        return this
    }

    fun show() {
        val popup = PopupMenu(context, anchor)
        items.forEachIndexed { index, item ->
            val menuItem = popup.menu.add(Menu.NONE, item.id, index, item.title)
            item.iconRes?.let { menuItem.setIcon(it) }
        }
        popup.setOnMenuItemClickListener { menuItem ->
            items.find { it.id == menuItem.itemId }?.onClick?.invoke() ?: false
        }
        onDismiss?.let { popup.setOnDismissListener { it() } }
        popup.show()
    }

    companion object {
        fun show(context: Context, anchor: View, items: List<Item>, onDismiss: (() -> Unit)? = null) {
            PopupMenuBuilder(context, anchor)
                .addItems(*items.toTypedArray())
                .apply { onDismiss?.let { setOnDismissListener(it) } }
                .show()
        }
    }
} 