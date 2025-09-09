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
import android.widget.LinearLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import android.util.Log
import android.widget.ProgressBar
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HotAppsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hot_apps)
        supportActionBar?.hide()

        // Initialize views
        recycler = findViewById(R.id.hot_apps_recycler)
        progressBar = findViewById(R.id.progress_bar)
        emptyState = findViewById(R.id.empty_state)

        // Setup back button with padding
        findViewById<ImageButton>(R.id.btn_back).setOnClickListener { finish() }

        // Setup RecyclerView
        recycler.layoutManager = LinearLayoutManager(this)

        // Load hot apps (local data for now, can be easily switched to Firebase later)
        loadHotApps()
    }

    private fun loadHotApps() {
        progressBar.visibility = View.VISIBLE
        recycler.visibility = View.GONE
        emptyState.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Hardcoded FavTunes data
                val hotApps = listOf(
                    HotApp(
                        id = "favtunes",
                        name = "FavTunes",
                        shortDesc = "Choose FavTunes now for songs & playlists.",
                        fullDesc = "FavTunes, by Shaadow, is a music app that offers a personalized and hassle-free listening experience. It features a vast collection of over 10,000 songs and a sleek user interface. The app provides custom playlists based on your preferences, without requiring any login or personal information.",
                        features = listOf(
                            "Personalized Playlists",
                            "No Login/Signup Required",
                            "Offline music playback",
                            "10,000+ Songs Collection",
                            "Sleek and Clean UI",
                            "High-quality audio streaming"
                        ),
                        iconUrl = null, // Will use favtunes_logo drawable
                        playStoreUrl = "https://play.google.com/store/apps/details?id=com.shaadow.tunes",
                        isExpanded = false
                    )
                )

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    recycler.adapter = HotAppsAdapter(hotApps.toMutableList())
                    recycler.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                Log.e("HotAppsActivity", "Error loading hot apps: ${e.message}")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    showEmptyState()
                }
            }
        }
    }


    private fun showEmptyState() {
        recycler.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        emptyState.text = "No hot apps available at the moment"
    }

    data class HotApp(
        val id: String,
        val name: String,
        val shortDesc: String,
        val fullDesc: String,
        val features: List<String>,
        val iconUrl: String?,
        val playStoreUrl: String,
        var isExpanded: Boolean = false
    )

    inner class HotAppsAdapter(private val items: MutableList<HotApp>) : RecyclerView.Adapter<HotAppsAdapter.HotAppViewHolder>() {

        inner class HotAppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val container: ConstraintLayout = view.findViewById(R.id.app_container)
            val icon: ImageView = view.findViewById(R.id.app_icon)
            val name: TextView = view.findViewById(R.id.app_name)
            val shortDesc: TextView = view.findViewById(R.id.app_short_desc)
            val fullDesc: TextView = view.findViewById(R.id.app_full_desc)
            val featuresContainer: LinearLayout = view.findViewById(R.id.features_container)
            val expandButton: ImageButton = view.findViewById(R.id.btn_expand)
            val cta: TextView = view.findViewById(R.id.btn_cta)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotAppViewHolder {
            val view = layoutInflater.inflate(R.layout.item_hot_app_catalog, parent, false)
            return HotAppViewHolder(view)
        }

        override fun onBindViewHolder(holder: HotAppViewHolder, position: Int) {
            val item = items[position]

            holder.name.text = item.name
            holder.shortDesc.text = item.shortDesc
            holder.fullDesc.text = item.fullDesc

            // Handle icon (use favtunes_logo for FavTunes)
            if (item.name.contains("FavTunes", ignoreCase = true)) {
                holder.icon.setImageResource(R.drawable.favtunes_logo)
            } else {
                holder.icon.setImageResource(R.drawable.ic_hot_apps)
            }

            // Setup expandable functionality
            updateExpandedState(holder, item)

            holder.expandButton.setOnClickListener {
                item.isExpanded = !item.isExpanded
                updateExpandedState(holder, item)
                notifyItemChanged(position)
            }

            holder.cta.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.playStoreUrl))
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("HotAppsActivity", "Error opening Play Store: ${e.message}")
                }
            }
        }

        private fun updateExpandedState(holder: HotAppViewHolder, item: HotApp) {
            if (item.isExpanded) {
                holder.fullDesc.visibility = View.VISIBLE
                holder.featuresContainer.visibility = View.VISIBLE
                holder.expandButton.setImageResource(android.R.drawable.arrow_up_float)
                holder.expandButton.contentDescription = "Collapse details"

                // Populate features
                holder.featuresContainer.removeAllViews()
                item.features.forEach { feature ->
                    val featureView = layoutInflater.inflate(R.layout.item_feature, holder.featuresContainer, false)
                    val featureText = featureView.findViewById<TextView>(R.id.feature_text)
                    featureText.text = "• $feature"
                    holder.featuresContainer.addView(featureView)
                }
            } else {
                holder.fullDesc.visibility = View.GONE
                holder.featuresContainer.visibility = View.GONE
                holder.expandButton.setImageResource(android.R.drawable.arrow_down_float)
                holder.expandButton.contentDescription = "Expand details"
            }
        }

        override fun getItemCount() = items.size
    }
}