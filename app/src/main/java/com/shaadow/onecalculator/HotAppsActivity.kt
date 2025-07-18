package com.shaadow.onecalculator

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup

class HotAppsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hot_apps)
        supportActionBar?.hide()

        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }
        val recycler = findViewById<RecyclerView>(R.id.hot_apps_recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = HotAppsAdapter(getHotApps())
    }

    private fun getHotApps(): List<HotApp> = listOf(
        HotApp(
            name = "Mathly Voice",
            desc = "Voice-powered math assistant.",
            iconRes = R.drawable.ic_microphone,
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.shaadow.mathlyvoice"
        ),
        HotApp(
            name = "Unit Converter Pro",
            desc = "All-in-one unit converter.",
            iconRes = R.drawable.ic_unit_calcs,
            playStoreUrl = "https://play.google.com/store/apps/details?id=com.shaadow.unitconverterpro"
        )
        // Add more apps here
    )

    data class HotApp(val name: String, val desc: String, val iconRes: Int, val playStoreUrl: String)

    inner class HotAppsAdapter(private val items: List<HotApp>) : RecyclerView.Adapter<HotAppsAdapter.HotAppViewHolder>() {
        inner class HotAppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageButton = view.findViewById(R.id.app_icon)
            val name: TextView = view.findViewById(R.id.app_name)
            val desc: TextView = view.findViewById(R.id.app_desc)
            val cta: TextView = view.findViewById(R.id.btn_cta)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotAppViewHolder {
            val view = layoutInflater.inflate(R.layout.item_hot_app, parent, false)
            return HotAppViewHolder(view)
        }
        override fun onBindViewHolder(holder: HotAppViewHolder, position: Int) {
            val item = items[position]
            holder.icon.setImageResource(item.iconRes)
            holder.name.text = item.name
            holder.desc.text = item.desc
            holder.cta.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.playStoreUrl))
                startActivity(intent)
            }
        }
        override fun getItemCount() = items.size
    }
} 