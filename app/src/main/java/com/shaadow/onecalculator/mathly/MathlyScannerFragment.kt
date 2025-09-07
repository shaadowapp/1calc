package com.shaadow.onecalculator.mathly

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.common.util.concurrent.ListenableFuture
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.shaadow.onecalculator.HistoryDatabase
import com.shaadow.onecalculator.R
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MathlyScannerFragment : Fragment() {
    companion object {
        private const val REQ_CAMERA_PERMISSION = 201
    }

    private var cameraExecutor: ExecutorService? = null
    private var previewView: PreviewView? = null
    private var disabledOverlay: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflate the minimal layout for the scanner fragment
        return inflater.inflate(R.layout.fragment_mathly_scanner, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previewView = view.findViewById(R.id.preview_view)
        cameraExecutor = Executors.newSingleThreadExecutor()
        disabledOverlay = view.findViewById(R.id.scanner_disabled_overlay)
        
        // Set up confirm button click listener
        val confirmButton = view.findViewById<Button>(R.id.btn_confirm)
        confirmButton?.setOnClickListener {
            val intent = Intent(requireContext(), ScannerDetailsActivity::class.java)
            startActivity(intent)
        }
        
        // Hide overlay by default
        disabledOverlay?.visibility = View.GONE

        val dao = HistoryDatabase.getInstance(requireContext()).preferenceDao()
        lifecycleScope.launch {
            dao.observeAllPreferences().collect { prefs ->
                val enabled = prefs.find { it.key == "mathly_scanner" }?.value?.toBooleanStrictOrNull() != false
                if (!enabled) {
                    disabledOverlay?.visibility = View.VISIBLE
                } else {
                    disabledOverlay?.visibility = View.GONE
                }
            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            checkCameraPermission()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView?.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview)
            } catch (exc: Exception) {
                android.util.Log.e("MathlyScannerFragment", "Camera binding failed", exc)
                android.widget.Toast.makeText(requireContext(), "Camera preview error: ${exc.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor?.shutdown()
        previewView = null
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), REQ_CAMERA_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                // Permission denied, send to BasicActivity
                val intent = android.content.Intent(requireContext(), com.shaadow.onecalculator.BasicActivity::class.java)
                startActivity(intent)
                requireActivity().finish()
            }
        }
    }

    private fun showInvoiceBottomSheet() {
        // This function will be moved to ScannerDetailsActivity
    }

    inner class ShareAppsAdapter(
        private val apps: List<ResolveInfo>,
        private val onClick: (ResolveInfo) -> Unit
    ) : RecyclerView.Adapter<ShareAppsAdapter.AppViewHolder>() {
        inner class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.app_icon)
            val label: TextView = view.findViewById(R.id.app_label)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_share_app, parent, false)
            return AppViewHolder(view)
        }
        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            val app = apps[position]
            holder.icon.setImageDrawable(app.loadIcon(requireContext().packageManager))
            holder.label.text = app.loadLabel(requireContext().packageManager)
            holder.itemView.setOnClickListener { onClick(app) }
        }
        override fun getItemCount() = apps.size
    }
} 