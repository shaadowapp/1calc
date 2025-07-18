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

import android.content.Context
import android.content.res.AssetManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import org.vosk.Model
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Provides utility methods to sync model files to external storage to allow
 * C++ code access them. Relies on file named "uuid" to track updates.
 */
object StorageService {
    internal val TAG: String = StorageService::class.java.getSimpleName()

    fun unpack(
        context: Context,
        sourcePath: String,
        targetPath: String,
        completeCallback: Callback<Model?>,
        errorCallback: Callback<IOException?>
    ) {
        val executor: Executor =
            Executors.newSingleThreadExecutor() // change according to your requirements
        val handler: Handler = Handler(Looper.getMainLooper())
        executor.execute(Runnable {
            try {
                val outputPath = sync(context, sourcePath, targetPath)
                val model: Model = Model(outputPath)
                handler.post(Runnable { completeCallback.onComplete(model) })
            } catch (e: IOException) {
                handler.post(Runnable { errorCallback.onComplete(e) })
            }
        })
    }

    @Throws(IOException::class)
    fun sync(context: Context, sourcePath: String, targetPath: String): String {
        val assetManager: AssetManager = context.getAssets()

        val externalFilesDir = context.getExternalFilesDir(null)
        if (externalFilesDir == null) {
            throw IOException(
                ("cannot get external files dir, "
                        + "external storage state is " + Environment.getExternalStorageState())
            )
        }

        val targetDir = File(externalFilesDir, targetPath)
        val resultPath = File(targetDir, sourcePath).getAbsolutePath()
        val sourceUUID = readLine(assetManager.open(sourcePath + "/uuid"))
        try {
            val targetUUID = readLine(FileInputStream(File(targetDir, sourcePath + "/uuid")))
            if (targetUUID == sourceUUID) return resultPath
        } catch (e: FileNotFoundException) {
            // ignore
        }
        deleteContents(targetDir)

        copyAssets(assetManager, sourcePath, targetDir)

        // Copy uuid
        copyFile(assetManager, sourcePath + "/uuid", targetDir)

        return resultPath
    }

    @Throws(IOException::class)
    private fun readLine(`is`: InputStream?): String {
        return BufferedReader(InputStreamReader(`is`)).readLine()
    }

    private fun deleteContents(dir: File): Boolean {
        val files = dir.listFiles()
        var success = true
        if (files != null) {
            for (file in files) {
                if (file.isDirectory()) {
                    success = success and deleteContents(file)
                }
                if (!file.delete()) {
                    success = false
                }
            }
        }
        return success
    }

    @Throws(IOException::class)
    private fun copyAssets(assetManager: AssetManager, path: String, outPath: File?) {
        val assets: Array<String?>? = assetManager.list(path)
        if (assets == null) {
            return
        }
        if (assets.size == 0) {
            if (!path.endsWith("uuid")) copyFile(assetManager, path, outPath)
        } else {
            val dir = File(outPath, path)
            if (!dir.exists()) {
                Log.v(TAG, "Making directory " + dir.getAbsolutePath())
                if (!dir.mkdirs()) {
                    Log.v(TAG, "Failed to create directory " + dir.getAbsolutePath())
                }
            }
            for (asset in assets) {
                copyAssets(assetManager, path + "/" + asset, outPath)
            }
        }
    }

    @Throws(IOException::class)
    private fun copyFile(assetManager: AssetManager, fileName: String, outPath: File?) {
        val `in`: InputStream?

        Log.v(TAG, "Copy " + fileName + " to " + outPath)
        `in` = assetManager.open(fileName)
        val out: OutputStream = FileOutputStream(outPath.toString() + "/" + fileName)

        val buffer = ByteArray(4000)
        var read: Int
        while ((`in`!!.read(buffer).also { read = it }) != -1) {
            out.write(buffer, 0, read)
        }
        `in`.close()
        out.close()
    }

    interface Callback<R> {
        fun onComplete(result: R?)
    }
}
