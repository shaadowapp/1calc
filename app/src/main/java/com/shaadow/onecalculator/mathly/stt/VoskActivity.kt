// Copyright 2019 Alpha Cephei Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.shaadow.onecalculator.mathly.stt

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.ToggleButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.shaadow.onecalculator.R
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import java.io.IOException

class VoskActivity : Activity(), RecognitionListener {
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null
    private var resultView: TextView? = null

    public override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContentView(R.layout.main)

        // Setup layout
        resultView = findViewById<TextView>(R.id.result_text)
        setUiState(STATE_START)

        findViewById<View?>(R.id.recognize_file).setOnClickListener(View.OnClickListener { view: View? -> recognizeFile() })
        findViewById<View?>(R.id.recognize_mic).setOnClickListener(View.OnClickListener { view: View? -> recognizeMicrophone() })
        (findViewById<View?>(R.id.pause) as ToggleButton).setOnCheckedChangeListener(CompoundButton.OnCheckedChangeListener { view: CompoundButton?, isChecked: Boolean ->
            pause(
                isChecked
            )
        })

        LibVosk.setLogLevel(LogLevel.INFO)

        // Check if user has given permission to record audio, init the model after permission is granted
        val permissionCheck: Int = ContextCompat.checkSelfPermission(
            getApplicationContext(),
            Manifest.permission.RECORD_AUDIO
        )
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.RECORD_AUDIO),
                PERMISSIONS_REQUEST_RECORD_AUDIO
            )
        } else {
            initModel()
        }
    }

    private fun initModel() {
        StorageService.unpack(
            this, "model-en-us", "model",
            object : StorageService.Callback<Model?> {
                override fun onComplete(result: Model?) {
                    this@VoskActivity.model = result
                    setUiState(STATE_READY)
                }
            },
            object : StorageService.Callback<IOException?> {
                override fun onComplete(result: IOException?) {
                    setErrorState("Failed to unpack the model" + (result?.message ?: ""))
                }
            })
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Recognizer initialization is a time-consuming and it involves IO,
                // so we execute it in async task
                initModel()
            } else {
                finish()
            }
        }
    }

    public override fun onDestroy() {
        super.onDestroy()

        speechService?.stop()
        speechService?.shutdown()

        speechStreamService?.stop()
    }

    public override fun onResult(hypothesis: String?) {
        resultView!!.append(hypothesis + "\n")
    }

    public override fun onFinalResult(hypothesis: String?) {
        resultView!!.append(hypothesis + "\n")
        setUiState(STATE_DONE)
        if (speechStreamService != null) {
            speechStreamService = null
        }
    }

    public override fun onPartialResult(hypothesis: String?) {
        resultView!!.append(hypothesis + "\n")
    }

    public override fun onError(exception: Exception?) {
        setErrorState(exception?.message)
    }

    public override fun onTimeout() {
        setUiState(STATE_DONE)
    }

    private fun setUiState(state: Int) {
        when (state) {
            STATE_START -> {
                resultView?.setText(R.string.preparing)
                resultView?.setMovementMethod(ScrollingMovementMethod())
                findViewById<View?>(R.id.recognize_file).setEnabled(false)
                findViewById<View?>(R.id.recognize_mic).setEnabled(false)
                findViewById<View?>(R.id.pause).setEnabled((false))
            }

            STATE_READY -> {
                resultView?.setText(R.string.ready)
                (findViewById<View?>(R.id.recognize_mic) as Button).setText(R.string.recognize_microphone)
                findViewById<View?>(R.id.recognize_file).setEnabled(true)
                findViewById<View?>(R.id.recognize_mic).setEnabled(true)
                findViewById<View?>(R.id.pause).setEnabled((false))
            }

            STATE_DONE -> {
                (findViewById<View?>(R.id.recognize_file) as Button).setText(R.string.recognize_file)
                (findViewById<View?>(R.id.recognize_mic) as Button).setText(R.string.recognize_microphone)
                findViewById<View?>(R.id.recognize_file).setEnabled(true)
                findViewById<View?>(R.id.recognize_mic).setEnabled(true)
                findViewById<View?>(R.id.pause).setEnabled((false))
                (findViewById<View?>(R.id.pause) as ToggleButton).setChecked(false)
            }

            STATE_FILE -> {
                (findViewById<View?>(R.id.recognize_file) as Button).setText(R.string.stop_file)
                resultView!!.setText(getString(R.string.starting))
                findViewById<View?>(R.id.recognize_mic).setEnabled(false)
                findViewById<View?>(R.id.recognize_file).setEnabled(true)
                findViewById<View?>(R.id.pause).setEnabled((false))
            }

            STATE_MIC -> {
                (findViewById<View?>(R.id.recognize_mic) as Button).setText(R.string.stop_microphone)
                resultView!!.setText(getString(R.string.say_something))
                findViewById<View?>(R.id.recognize_file).setEnabled(false)
                findViewById<View?>(R.id.recognize_mic).setEnabled(true)
                findViewById<View?>(R.id.pause).setEnabled((true))
            }

            else -> throw IllegalStateException("Unexpected value: " + state)
        }
    }

    private fun setErrorState(message: String?) {
        resultView!!.setText(message)
        (findViewById<View?>(R.id.recognize_mic) as Button).setText(R.string.recognize_microphone)
        findViewById<View?>(R.id.recognize_file).setEnabled(false)
        findViewById<View?>(R.id.recognize_mic).setEnabled(false)
    }

    private fun recognizeFile() {
        if (speechStreamService != null) {
            setUiState(STATE_DONE)
            speechStreamService?.stop()
            speechStreamService = null
        } else {
            setUiState(STATE_FILE)
            try {
                val rec: Recognizer = Recognizer(
                    model, 16000f, "[\"one zero zero zero one\", " +
                            "\"oh zero one two three four five six seven eight nine\", \"[unk]\"]"
                )

                val ais = getAssets().open(
                    "10001-90210-01803.wav"
                )
                if (ais.skip(44) != 44L) throw IOException("File too short")

                speechStreamService = SpeechStreamService(rec, ais, 16000f)
                speechStreamService?.start(this)
            } catch (e: IOException) {
                setErrorState(e.message)
            }
        }
    }

    private fun recognizeMicrophone() {
        if (speechService != null) {
            setUiState(STATE_DONE)
            speechService?.stop()
            speechService = null
        } else {
            setUiState(STATE_MIC)
            try {
                val rec: Recognizer = Recognizer(model, 16000.0f)
                speechService = SpeechService(rec, 16000.0f)
                speechService?.startListening(this)
            } catch (e: IOException) {
                setErrorState(e.message)
            }
        }
    }


    private fun pause(checked: Boolean) {
        speechService?.setPause(checked)
    }

    companion object {
        private const val STATE_START = 0
        private const val STATE_READY = 1
        private const val STATE_DONE = 2
        private const val STATE_FILE = 3
        private const val STATE_MIC = 4

        /* Used to handle permission request */
        private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
    }
}
