package com.example.narro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.narro.databinding.ActivityBacaBinding
import com.example.narro.databinding.DialogSpeechBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.*

class ReadActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var binding: ActivityBacaBinding
    private lateinit var dialogBinding: DialogSpeechBinding
    private lateinit var speechDialog: AlertDialog
    private lateinit var speechRecognizer: SpeechRecognizer
    private var imageUri: Uri? = null
    private var tts: TextToSpeech? = null
    private var isSpeaking = false
    private var textToRead = ""
    private var lastCharIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBacaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi SpeechRecognizer
        speechRecognizer = SpeechRecognizer(
            context = this,
            onResult = { command, confidence ->
                dialogBinding.speechText.text = "Perintah: $command (Confidence: ${String.format("%.2f", confidence)})"
                Toast.makeText(this, "Perintah: $command", Toast.LENGTH_LONG).show()
                handleCommand(command)
                speechDialog.dismiss()
            },
            onError = { error ->
                dialogBinding.speechText.text = "Error: $error"
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                speechDialog.dismiss()
            }
        )

        tts = TextToSpeech(this, this)

        val resultImage = binding.resultImage
        val btnPlay = binding.btnPlay
        val btnReplay = binding.btnReplay
        val btnBack = binding.btnBack
        val resultText = binding.resultText
        resultText.movementMethod = android.text.method.ScrollingMovementMethod()

        imageUri = intent.getStringExtra("image_uri")?.let { Uri.parse(it) }
        textToRead = intent.getStringExtra("ocr_text") ?: ""
        resultText.text = textToRead

        if (imageUri != null) {
            resultImage.setImageURI(imageUri)
        }

        btnPlay.setOnClickListener { toggleSpeech() }
        btnReplay.setOnClickListener { replaySpeech() }
        btnBack.setOnClickListener {
            deleteImage()
            tts?.speak("kembali ke halaman foto", TextToSpeech.QUEUE_FLUSH, null, "BACK_UTTERANCE")
        }

        setupTTSListener()
        setupSpeechButton()
    }

    @Suppress("ClickableViewAccessibility")
    private fun setupSpeechButton() {
        binding.fabSpeech.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        showSpeechDialog()
                        speechRecognizer.startRecording()
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    speechRecognizer.stopRecording()
                    true
                }
                else -> false
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            showSpeechDialog()
            speechRecognizer.startRecording()
        } else {
            Toast.makeText(this, "Izin mikrofon diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSpeechDialog() {
        dialogBinding = DialogSpeechBinding.inflate(layoutInflater)
        speechDialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.speechText.text = "Merekam perintah suara..."
        dialogBinding.closeButton.setOnClickListener {
            speechRecognizer.stopRecording()
            speechDialog.dismiss()
        }

        speechDialog.window?.setLayout(
            android.view.WindowManager.LayoutParams.MATCH_PARENT,
            android.view.WindowManager.LayoutParams.WRAP_CONTENT
        )
        speechDialog.show()
    }

    private fun handleCommand(command: String) {
        when (command) {
            "kembali" -> {
                deleteImage()
                tts?.speak("kembali ke halaman foto", TextToSpeech.QUEUE_FLUSH, null, "BACK_UTTERANCE")
            }
            "berhenti" -> {
                if (isSpeaking) {
                    toggleSpeech() // Stop reading
                    Toast.makeText(this, "Bacaan dihentikan", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Tidak ada bacaan yang sedang berjalan", Toast.LENGTH_SHORT).show()
                }
            }
            "ulang" -> replaySpeech()
            "baca" -> {
                if (textToRead.isNotEmpty()) {
                    toggleSpeech() // Start reading
                } else {
                    Toast.makeText(this, "Tidak ada teks untuk dibaca", Toast.LENGTH_SHORT).show()
                }
            }
//            "halo" -> {
//                Toast.makeText(this, "Silakan ucapkan perintah", Toast.LENGTH_SHORT).show()
//                // Simulate a touch down event to start recording
//                triggerSpeechRecording()
//            }
            else -> Toast.makeText(this, "Perintah $command tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("id", "ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                binding.resultText.text = "Bahasa tidak didukung"
            } else {
                if (textToRead.isEmpty()) {
                    tts?.speak("tidak ada teks terdeteksi", TextToSpeech.QUEUE_FLUSH, null, null)
                } else {
                    tts?.speak("proses selesai, anda berada di halaman baca", TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
        }
    }

    private fun setupTTSListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    binding.btnPlay.setImageResource(R.drawable.baseline_play_circle_24)
                    isSpeaking = false
                    lastCharIndex = 0
                    if (utteranceId == "BACK_UTTERANCE") {
                        finish()
                    }
                }
            }
            override fun onError(utteranceId: String?) {}
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                lastCharIndex = start
            }
        })
    }

    private fun toggleSpeech() {
        if (textToRead.isNotEmpty()) {
            if (isSpeaking) {
                tts?.stop()
                binding.btnPlay.setImageResource(R.drawable.baseline_play_circle_24)
                isSpeaking = false
            } else {
                val remainingText = textToRead.substring(lastCharIndex)
                tts?.speak(remainingText, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_ID")
                binding.btnPlay.setImageResource(R.drawable.baseline_pause_circle_24)
                isSpeaking = true
            }
        }
    }

    private fun replaySpeech() {
        if (textToRead.isNotEmpty()) {
            tts?.stop()
            lastCharIndex = 0
            tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_ID")
            binding.btnPlay.setImageResource(R.drawable.baseline_pause_circle_24)
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
        super.onDestroy()
    }
}