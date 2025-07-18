package com.shaadow.onecalculator.mathly

// FileProvider imports
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.shaadow.onecalculator.R
import java.io.File
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.Canvas
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.LinearLayout

class ScannerDetailsActivity : AppCompatActivity() {

    private lateinit var etDiscount: EditText
    private lateinit var tvSubtotal: TextView
    private lateinit var tvTotalAmount: TextView
    private var subtotal = 40.47

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner_details)

        // Expand/collapse logic for shop details
        val shopHeader = findViewById<LinearLayout>(R.id.section_shop_details_header)
        val shopContent = findViewById<LinearLayout>(R.id.section_shop_details_content)
        val shopExpandIcon = findViewById<ImageView>(R.id.iv_shop_expand)
        shopExpandIcon.rotation = 0f // Ensure initial state is down
        shopHeader.setOnClickListener {
            if (shopContent.visibility == View.VISIBLE) {
                shopContent.visibility = View.GONE
                shopExpandIcon.animate().rotation(0f).setDuration(200).start()
            } else {
                shopContent.visibility = View.VISIBLE
                shopExpandIcon.animate().rotation(180f).setDuration(200).start()
            }
        }

        // Expand/collapse logic for customer details
        val customerHeader = findViewById<LinearLayout>(R.id.section_customer_details_header)
        val customerContent = findViewById<LinearLayout>(R.id.section_customer_details_content)
        val customerExpandIcon = findViewById<ImageView>(R.id.iv_customer_expand)
        customerExpandIcon.rotation = 0f // Ensure initial state is down
        customerHeader.setOnClickListener {
            if (customerContent.visibility == View.VISIBLE) {
                customerContent.visibility = View.GONE
                customerExpandIcon.animate().rotation(0f).setDuration(200).start()
            } else {
                customerContent.visibility = View.VISIBLE
                customerExpandIcon.animate().rotation(180f).setDuration(200).start()
            }
        }

        // Download and Share buttons
        val downloadButton = findViewById<Button>(R.id.btn_download_invoice)
        val shareButton = findViewById<ImageButton>(R.id.btn_share_invoice)

        // Back button logic
        val backButton = findViewById<ImageButton>(R.id.btn_back)
        backButton.setOnClickListener {
            // Open HomeActivity with scanner tab selected
            val intent = Intent(this, com.shaadow.onecalculator.HomeActivity::class.java)
            intent.putExtra("navigate_to_scanner", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }

        downloadButton.setOnClickListener {
            generateAndSaveInvoice(justSave = true)
        }
        shareButton.setOnClickListener {
            generateAndSaveInvoice(justSave = false)
        }
    }

    private fun calculateTotal() {
        val discountText = etDiscount.text.toString()
        val discount = if (discountText.isNotEmpty()) {
            try {
                discountText.toDouble()
            } catch (e: NumberFormatException) {
                0.0
            }
        } else {
            0.0
        }

        val discountAmount = subtotal * (discount / 100)
        val total = subtotal - discountAmount

        tvSubtotal.text = "₹%.2f".format(subtotal)
        tvTotalAmount.text = "₹%.2f".format(total)
    }

    private fun generateAndSaveInvoice(justSave: Boolean) {
        val shopName = findViewById<EditText>(R.id.et_shop_name).text.toString().ifBlank { "Shaadow - 1Calculator" }
        val shopAddress = findViewById<EditText>(R.id.et_shop_address).text.toString()
        val customerName = findViewById<EditText>(R.id.et_customer_name).text.toString()
        val customerEmail = findViewById<EditText>(R.id.et_customer_email).text.toString()
        val customerPhone = findViewById<EditText>(R.id.et_customer_phone).text.toString()
        val discount = "0" // You can add a discount field if needed
        val products = listOf(
            DemoProduct("Sample Product 1", "123456", 1, 12.99),
            DemoProduct("Sample Product 2", "987654", 2, 7.49),
            DemoProduct("Premium Product", "555888", 1, 19.99)
        )
        val total = products.sumOf { it.price * it.quantity }

        val invoicesDir = File(getExternalFilesDir(null), "invoices")
        if (!invoicesDir.exists()) invoicesDir.mkdirs()
        val now = Date()
        val sdf = SimpleDateFormat("yyyyMMddHHmm", Locale.US)
        val datePart = sdf.format(now)
        val randomDigits = (100..999).random()
        val jpegFileName = "INV_1CALC_${datePart}${randomDigits}.jpg"
        val jpegFile = File(invoicesDir, jpegFileName)
        generateInvoiceJpegWithShop(
            this,
            jpegFile,
            shopName,
            shopAddress,
            customerName,
            customerEmail,
            customerPhone,
            discount,
            products,
            total
        )
        if (justSave) {
            Toast.makeText(this, "Invoice saved to: ${jpegFile.absolutePath}", Toast.LENGTH_LONG).show()
        } else {
            val jpegUri = FileProvider.getUriForFile(this, "${packageName}.provider", jpegFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, jpegUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Share Invoice"))
        }
    }

    private fun generateInvoiceJpegWithShop(
        context: Context,
        file: File,
        shopName: String,
        shopAddress: String,
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        discount: String,
        products: List<DemoProduct>,
        total: Double
    ) {
        val width = 1080
        val height = 1800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)
        val paint = android.graphics.Paint()
        paint.isAntiAlias = true

        // Shop details
        paint.textSize = 48f
        paint.color = android.graphics.Color.BLACK
        paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        canvas.drawText(shopName, 40f, 90f, paint)
        paint.textSize = 28f
        paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        canvas.drawText(shopAddress, 40f, 140f, paint)

        // Header
        paint.textSize = 60f
        paint.color = android.graphics.Color.BLACK
        paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        canvas.drawText("INVOICE", 40f, 220f, paint)

        // Customer details
        paint.textSize = 32f
        paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        var y = 300f
        canvas.drawText("Customer: $customerName", 40f, y, paint)
        y += 40f
        canvas.drawText("Email: $customerEmail", 40f, y, paint)
        y += 40f
        canvas.drawText("Phone: $customerPhone", 40f, y, paint)
        y += 60f

        // Table header
        paint.textSize = 36f
        paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        canvas.drawText("Product", 40f, y, paint)
        canvas.drawText("Qty", 500f, y, paint)
        canvas.drawText("Price", 700f, y, paint)
        canvas.drawText("Total", 900f, y, paint)
        y += 40f
        paint.strokeWidth = 2f
        canvas.drawLine(40f, y, 1040f, y, paint)
        y += 40f

        // Table rows
        paint.textSize = 32f
        paint.typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        for (product in products) {
            canvas.drawText(product.name, 40f, y, paint)
            canvas.drawText(product.quantity.toString(), 500f, y, paint)
            canvas.drawText("₹%.2f".format(product.price), 700f, y, paint)
            canvas.drawText("₹%.2f".format(product.price * product.quantity), 900f, y, paint)
            y += 40f
        }
        y += 20f
        paint.strokeWidth = 2f
        canvas.drawLine(40f, y, 1040f, y, paint)
        y += 50f

        // Discount and total
        paint.textSize = 36f
        paint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD)
        val discountValue = discount.toDoubleOrNull() ?: 0.0
        val discountAmount = total * (discountValue / 100)
        val finalTotal = total - discountAmount
        canvas.drawText("Subtotal: ₹%.2f".format(total), 700f, y, paint)
        y += 40f
        canvas.drawText("Discount: $discount%", 700f, y, paint)
        y += 40f
        canvas.drawText("Total: ₹%.2f".format(finalTotal), 700f, y, paint)

        java.io.FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
    }

    data class DemoProduct(val name: String, val id: String, val quantity: Int, val price: Double)

    // Get all apps that support sharing the PDF
    private fun getShareableApps(context: Context, pdfUri: android.net.Uri): List<ResolveInfo> {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }

    // Launch share intent for selected app
    private fun shareToApp(context: Context, pdfUri: android.net.Uri, app: ResolveInfo) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage(app.activityInfo.packageName)
            setClassName(app.activityInfo.packageName, app.activityInfo.name)
        }
        context.startActivity(intent)
    }

    // Adapter for share apps
    class ShareAppAdapter(
        private val context: Context,
        private val apps: List<ResolveInfo>,
        private val pdfUri: android.net.Uri
    ) : RecyclerView.Adapter<ShareAppAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val icon: ImageView = itemView.findViewById(R.id.app_icon)
            val name: TextView = itemView.findViewById(R.id.app_label)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_share_app, parent, false)
            return ViewHolder(view)
        }
        override fun getItemCount(): Int = apps.size
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = apps[position]
            val pm = context.packageManager
            holder.icon.setImageDrawable(app.loadIcon(pm))
            holder.name.text = app.loadLabel(pm)
            holder.itemView.setOnClickListener {
                (context as? ScannerDetailsActivity)?.shareToApp(context, pdfUri, app)
            }
        }
    }

    private fun showInvoiceBottomSheet() {
        val pdfFile = File(getExternalFilesDir(null), "invoice.pdf")
        InvoiceShareBottomSheet(this, pdfFile).show()
    }
} 