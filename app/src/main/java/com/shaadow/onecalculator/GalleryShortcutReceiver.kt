package com.shaadow.onecalculator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class GalleryShortcutReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "com.shaadow.onecalculator.GALLERY_SHORTCUT") {
            val position = intent.getIntExtra("shortcut_position", -1)
            val folderId = intent.getLongExtra("shortcut_folder_id", -1)

            Log.d("GalleryShortcutReceiver", "Received gallery shortcut broadcast - Position: $position, Folder ID: $folderId")

            // Start the gallery activity with the shortcut extras
            val galleryIntent = Intent(context, MediaGalleryActivity::class.java).apply {
                putExtra("is_shortcut_access", true)
                putExtra("shortcut_position", position)
                if (folderId != -1L) {
                    putExtra("shortcut_folder_id", folderId)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context?.startActivity(galleryIntent)
        }
    }
}