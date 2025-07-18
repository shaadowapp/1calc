package com.shaadow.onecalculator.mathly

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.shaadow.onecalculator.R
import java.io.File
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import android.widget.GridLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class InvoiceShareBottomSheet(
    context: Context,
    private val pdfFile: File
) : BottomSheetDialog(context, com.google.android.material.R.style.Theme_Design_BottomSheetDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_invoice, null)
        setContentView(sheetView)

        // Set the system bottom sheet container's background to dark and rounded
        findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.background =
            androidx.core.content.ContextCompat.getDrawable(context, R.drawable.bg_bottom_card)

        // Make the bottom sheet full width
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Lock the bottom sheet so it can't be dragged above 60% of the screen, but allow a 35% chunk
        val displayMetrics = context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val peekHeight = (screenHeight * 0.35).toInt()
        val expandedHeight = (screenHeight * 0.6).toInt()
        sheetView.layoutParams?.height = expandedHeight
        behavior.peekHeight = peekHeight
        behavior.isFitToContents = false
        behavior.expandedOffset = screenHeight - expandedHeight
        behavior.halfExpandedRatio = 0.5f
        behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HALF_EXPANDED
        behavior.isDraggable = true

        // Share Button (MaterialButton)
        val shareButton = sheetView.findViewById<MaterialButton?>(R.id.btn_share_invoice)
        shareButton?.setOnClickListener {
            sharePdfWithSystemSharesheet()
            dismiss()
        }

        // Download Button
        val downloadButton = sheetView.findViewById<Button>(R.id.btn_download_invoice)
        downloadButton.setOnClickListener {
            // TODO: Implement download logic (e.g., copy to Downloads)
            dismiss()
        }

        // Manual share section
        val allManualShareApps = listOf(
            ManualShareApp("Print", "com.android.printspooler", R.drawable.ic_print),
            ManualShareApp("WhatsApp", "com.whatsapp", R.drawable.ic_whatsapp),
            ManualShareApp("Telegram", "org.telegram.messenger", R.drawable.ic_telegram),
            ManualShareApp("Gmail", "com.google.android.gm", R.drawable.ic_gmail),
            ManualShareApp("Drive", "com.google.android.apps.docs", R.drawable.ic_drive),
            ManualShareApp("Bluetooth", "com.android.bluetooth", R.drawable.ic_bluetooth),
            ManualShareApp("Messages", "com.google.android.apps.messaging", R.drawable.ic_messages)
        )
        // Generate unique invoice JPEG filename and folder
        val invoicesDir = File(context.getExternalFilesDir(null), "invoices")
        if (!invoicesDir.exists()) invoicesDir.mkdirs()
        val now = Date()
        val sdf = SimpleDateFormat("yyyyMMddHHmm", Locale.US)
        val datePart = sdf.format(now)
        val randomDigits = (100..999).random()
        val jpegFileName = "INV_1CALC_${datePart}${randomDigits}.jpg"
        val jpegFile = File(invoicesDir, jpegFileName)
        val jpegUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", jpegFile)
        val grid = sheetView.findViewById<GridLayout>(R.id.grid_manual_share_apps)
        grid.removeAllViews()
        val maxApps = 10
        val appsToShow = allManualShareApps.filter { app ->
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, jpegUri)
                setPackage(app.packageName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            shareIntent.resolveActivity(context.packageManager) != null
        }.take(maxApps)
        for (app in appsToShow) {
            val itemView = LayoutInflater.from(context).inflate(R.layout.item_share_app, grid, false)
            val icon = itemView.findViewById<ImageView>(R.id.app_icon)
            val name = itemView.findViewById<TextView>(R.id.app_label)
            icon.setImageResource(app.iconRes)
            name.text = app.label
            itemView.setOnClickListener {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, jpegUri)
                    setPackage(app.packageName)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(shareIntent)
            }
            val params = GridLayout.LayoutParams().apply {
                width = GridLayout.LayoutParams.WRAP_CONTENT
                height = GridLayout.LayoutParams.WRAP_CONTENT
            }
            itemView.layoutParams = params
            grid.addView(itemView)
        }
    }

    private fun sharePdfWithSystemSharesheet() {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share PDF using"))
    }

    data class ManualShareApp(val label: String, val packageName: String, val iconRes: Int)
} 