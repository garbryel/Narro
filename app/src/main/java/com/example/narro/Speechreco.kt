package com.example.narro

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.TimeUnit

class SpeechRecognizer(
    private val context: Context,
    private val onResult: (String, Float) -> Unit,
    private val onError: (String) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val SAMPLE_RATE = 16000
    private val BUFFER_SIZE = 2048
    private val audioBuffer = mutableListOf<Short>()
    private val API_URL = "http://192.168.1.5:8000/predict"

    fun startRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onError("Izin mikrofon diperlukan")
            return
        }

        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferSize, BUFFER_SIZE)
        )

        audioBuffer.clear()
        isRecording = true
        audioRecord?.startRecording()

        Thread {
            val buffer = ShortArray(BUFFER_SIZE)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                if (read > 0) {
                    synchronized(audioBuffer) {
                        for (i in 0 until read) {
                            audioBuffer.add(buffer[i])
                        }
                    }
                }
            }
        }.start()
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null

        val outputFile = File(context.externalCacheDir, "audio.wav")
        saveToWavFile(outputFile)
        sendToAPI(outputFile)
    }

    private fun saveToWavFile(outputFile: File) {
        val headerSize = 44
        val sampleCount = audioBuffer.size
        val byteRate = SAMPLE_RATE * 2
        val totalDataSize = sampleCount * 2 + headerSize

        val outputStream = FileOutputStream(outputFile)
        outputStream.write("RIFF".toByteArray())
        outputStream.write(intToByteArray(totalDataSize - 8))
        outputStream.write("WAVE".toByteArray())
        outputStream.write("fmt ".toByteArray())
        outputStream.write(intToByteArray(16))
        outputStream.write(shortToByteArray(1))
        outputStream.write(shortToByteArray(1))
        outputStream.write(intToByteArray(SAMPLE_RATE))
        outputStream.write(intToByteArray(byteRate))
        outputStream.write(shortToByteArray(2))
        outputStream.write(shortToByteArray(16))
        outputStream.write("data".toByteArray())
        outputStream.write(intToByteArray(sampleCount * 2))

        synchronized(audioBuffer) {
            val byteBuffer = ByteBuffer.allocate(sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in audioBuffer) {
                byteBuffer.putShort(sample)
            }
            outputStream.write(byteBuffer.array())
            audioBuffer.clear()
        }
        outputStream.close()
    }

    private fun sendToAPI(file: File) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (file.length() == 0L) {
                    withContext(Dispatchers.Main) {
                        onError("Error: WAV file is empty")
                    }
                    return@launch
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build()

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.name, file.asRequestBody("audio/wav".toMediaType()))
                    .build()

                val request = Request.Builder()
                    .url(API_URL)
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val command = json.optString("command", "Unknown")
                    val confidence = if (json.has("confidence")) json.getDouble("confidence").toFloat() else 0.0f
                    withContext(Dispatchers.Main) {
                        onResult(command, confidence)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        onError("API error: ${response.code} - ${response.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Error: ${e.message}, Cause: ${e.cause}")
                }
            }
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value).array()
    }
}