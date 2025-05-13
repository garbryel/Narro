package com.example.narro

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class NotificationActivity(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var isReady = false

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("id", "ID"))
            isReady = result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    fun speak(text: String) {
        if (isReady && text.isNotEmpty()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_ID")
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
