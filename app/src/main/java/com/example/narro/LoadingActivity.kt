package com.example.narro

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class LoadingActivity : AppCompatActivity() {
    private var imageUri: Uri? = null
    private lateinit var ttsHelper: NotificationActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        // Initialize ttsHelper dan menunggu TTS siap
        ttsHelper = NotificationActivity(this)

        // Tunggu sampai TTS siap, baru bicara
        Handler(Looper.getMainLooper()).postDelayed({
            ttsHelper.speak("memproses")
        }, 700) // Delay 0.7 detik untuk memastikan TTS siap

        imageUri = intent.getStringExtra("image_uri")?.let { Uri.parse(it) }

        if (imageUri != null) {
            processImageAndGoToReadActivity(imageUri!!)
        } else {
            finish()
        }
    }

    private fun processImageAndGoToReadActivity(uri: Uri) {
        val image = InputImage.fromFilePath(this, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val intent = Intent(this, ReadActivity::class.java)
                intent.putExtra("image_uri", uri.toString())
                intent.putExtra("ocr_text", visionText.text) // Kirim hasil OCR
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                finish()
            }
    }

    override fun onDestroy() {
        ttsHelper.shutdown()
        super.onDestroy()
    }
}
