package com.example.narro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.*

class ReadActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private var imageUri: Uri? = null
    private lateinit var resultText: TextView
    private lateinit var btnPlay: ImageButton
    private lateinit var btnReplay: ImageButton
    private lateinit var btnBack: ImageButton
    private var tts: TextToSpeech? = null
    private var isSpeaking = false
    private var textToRead = ""
    private var lastCharIndex = 0
    private lateinit var ttsHelper: NotificationActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_baca)

        ttsHelper = NotificationActivity(this)

        val resultImage = findViewById<ImageView>(R.id.resultImage)
        btnPlay = findViewById(R.id.btnPlay)
        btnReplay = findViewById(R.id.btnReplay)
        btnBack = findViewById(R.id.btnBack)
        resultText = findViewById(R.id.resultText)
        resultText.movementMethod = android.text.method.ScrollingMovementMethod()

        tts = TextToSpeech(this, this)

        imageUri = intent.getStringExtra("image_uri")?.let { Uri.parse(it) }
        textToRead = intent.getStringExtra("ocr_text") ?: "" //LoadingActivity
        resultText.text = textToRead

        if (imageUri != null) {
            resultImage.setImageURI(imageUri)
        }

        btnPlay.setOnClickListener { toggleSpeech() }
        btnReplay.setOnClickListener { replaySpeech() }
//        btnBack.setOnClickListener {
//            Handler(Looper.getMainLooper()).postDelayed({
//                ttsHelper.speak("kembali ke halaman baca")
//            }, 2000)
//            deleteImage()
//            tts?.stop()
//            finish()
//        }

        btnBack.setOnClickListener {
            deleteImage()
            tts?.speak("kembali ke halaman foto", TextToSpeech.QUEUE_FLUSH, null, "BACK_UTTERANCE")
        }

        setupTTSListener()
    }


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("id", "ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                resultText.text = "Bahasa tidak didukung"
            } else {
                // Pastikan notifikasi suara dipanggil setelah TTS siap
                if (textToRead.isEmpty()) {
                    ttsHelper.speak("tidak ada teks terdeteksi")
                } else {
                    ttsHelper.speak("proses selesai, anda berada di halaman baca")
                }
            }
        }
    }

    private fun setupTTSListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    btnPlay.setImageResource(R.drawable.baseline_play_circle_24)
                    isSpeaking = false
                    lastCharIndex = 0 // Reset setelah selesai

                    if (utteranceId == "BACK_UTTERANCE") {
                        finish()
                    }
                }
            }

            override fun onError(utteranceId: String?) {}

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                lastCharIndex = start // Simpan posisi terakhir saat TTS membaca
            }
        })
    }

    private fun toggleSpeech() {
        if (textToRead.isNotEmpty()) {
            if (isSpeaking) {
                tts?.stop()
                btnPlay.setImageResource(R.drawable.baseline_play_circle_24)
                isSpeaking = false
            } else {
                val remainingText = textToRead.substring(lastCharIndex) // Ambil teks dari posisi terakhir
                tts?.speak(remainingText, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_ID")
                btnPlay.setImageResource(R.drawable.baseline_pause_circle_24)
                isSpeaking = true
            }
        }
    }

    private fun replaySpeech() {
        if (textToRead.isNotEmpty()) {
            tts?.stop()
            lastCharIndex = 0 // Reset ke awal
            tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_ID")
            btnPlay.setImageResource(R.drawable.baseline_pause_circle_24)
            isSpeaking = true
        }
    }

    private fun deleteImage() {
        imageUri?.let {
            contentResolver.delete(it, null, null)
        }
    }


    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        ttsHelper.shutdown()
        super.onDestroy()
    }

}


