package com.shaadow.onecalculator.mathly.stt

import android.content.Context
import android.util.Log
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

class VoiceInputManager(
    private val context: Context,
    private val onFinalResult: (String) -> Unit,
    private val onPartialResultCallback: (String) -> Unit,
    private val onError: (String) -> Unit
) {

    private var model: Model? = null
    private var speechService: SpeechService? = null
    
    companion object {
        private const val TAG = "VoiceInputManager"
    }

    fun initModel(onReady: () -> Unit) {
        try {
            Log.d(TAG, "Starting model initialization...")
            
            // Validate model structure first
            if (!validateModelStructure()) {
                onError("Model structure validation failed - missing required files")
                return
            }
            
            // Check if model assets exist
            val assetManager = context.assets
            val modelFiles = assetManager.list("model")
            Log.d(TAG, "Model files found: ${modelFiles?.joinToString(", ")}")
            
            StorageService.unpack(context, "model", "model",
                { unpackedModel ->
                    Log.d(TAG, "Model unpacked successfully")
                    model = unpackedModel
                    onReady()
                },
                { exception ->
                    Log.e(TAG, "Model unpack failed", exception)
                    onError("Model unpack failed: ${exception.message}")
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Model initialization error", e)
            onError("Model initialization error: ${e.message}")
        }
    }

    fun startListening() {
        try {
            val recognizer = Recognizer(model, 16000.0f)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService!!.startListening(object : RecognitionListener {
                override fun onPartialResult(hypothesis: String?) {
                    onPartialResultCallback(hypothesis ?: "")
                }

                override fun onResult(hypothesis: String?) {
                    onFinalResult(hypothesis ?: "")
                }

                override fun onFinalResult(hypothesis: String?) {
                    onFinalResult(hypothesis ?: "")
                }

                override fun onError(e: Exception?) {
                    onError(e?.message ?: "Unknown error")
                }

                override fun onTimeout() {
                    stopListening()
                    onError("Timeout")
                }
            })
        } catch (e: IOException) {
            onError("Recognizer failed: ${e.message}")
        }
    }

    fun stopListening() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
    }
    
    private fun validateModelStructure(): Boolean {
        val assetManager = context.assets
        val requiredFiles = listOf("uuid", "conf/model.conf", "conf/mfcc.conf", "am/final.mdl")
        
        for (file in requiredFiles) {
            try {
                assetManager.open("model/$file").use { }
                Log.d(TAG, "Found required file: $file")
            } catch (e: Exception) {
                Log.e(TAG, "Missing required file: $file", e)
                return false
            }
        }
        return true
    }
}
