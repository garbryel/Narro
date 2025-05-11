package com.example.narro

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.text.toSpannable
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.content.res.Resources
import android.view.ViewGroup
import android.widget.FrameLayout
import android.view.GestureDetector
import android.widget.ScrollView
import android.speech.tts.TextToSpeech
import java.util.Locale


class GuideActivity : BottomSheetDialogFragment(), TextToSpeech.OnInitListener {

    companion object {
        const val TAG = "BottomSheetGuide"
    }

    private var tts: TextToSpeech? = null
    private lateinit var guideText: String

    @SuppressLint("MissingInflatedId")
    override fun onCreateDialog(savedInstanceState: Bundle?): BottomSheetDialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        val view = LayoutInflater.from(context).inflate(R.layout.activity_guide, null, false)

        val rootLayout = view.findViewById<FrameLayout>(R.id.guideRoot)
        val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                dialog.dismiss()
                return true
            }
        })

        rootLayout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        val scrollView = view.findViewById<ScrollView>(R.id.scrollView)
        scrollView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            false
        }

        // Atur dan simpan teks panduan
        val spannableGuide = buildGuideText()
        guideText = spannableGuide.toString()
        view.findViewById<TextView>(R.id.guideTextView).text = spannableGuide

        dialog.setContentView(view)
        dialog.setCancelable(false)

        view.post {
            val screenHeight = Resources.getSystem().displayMetrics.heightPixels
            val maxHeight = (screenHeight * 0.75).toInt()
            rootLayout.layoutParams.height = maxHeight
            rootLayout.requestLayout()
        }

        // Inisialisasi TTS
        tts = TextToSpeech(context, this)

        return dialog
    }

    // TTS siap digunakan
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("id", "ID")
            tts?.speak(guideText, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // Bersihkan TTS saat fragment dihancurkan
    override fun onDestroyView() {
        super.onDestroyView()
        tts?.stop()
        tts?.shutdown()
    }

    private fun buildGuideText(): Spannable {
        val raw = """
            Panduan Penggunaan Aplikasi Narro

            Selamat datang di aplikasi Narro.
            Ketuk dua kali di mana saja untuk menutup panduan ini.

            Halaman Foto:
            - Tombol kiri atas digunakan untuk mengaktifkan panduan suara.
            - Tombol kanan atas digunakan untuk menyalakan atau mematikan flash (flash menyala otomatis saat dibutuhkan).
            - Tombol tengah bawah digunakan untuk mengambil foto dokumen. Setelah tombol ditekan, kamu akan diarahkan ke halaman Baca.

            Halaman Baca:
            - Tombol kiri atas digunakan untuk kembali ke halaman Foto.
            - Tombol kiri bawah digunakan untuk memulai atau menghentikan suara bacaan.
            - Tombol kanan bawah digunakan untuk mengulang bacaan dari awal.

            Perintah suara yang dapat digunakan:
            - "Halo" untuk mengaktifkan perintah suara
            - "Foto" untuk mengambil foto
            - "Info" untuk mendengarkan panduan aplikasi
            - "Baca" untuk memulai bacaan
            - "Ulang" untuk mengulang bacaan
            - "Berhenti" untuk menghentikan bacaan
            - "Kembali" untuk kembali ke halaman sebelumnya

            Setiap halaman akan disertai notifikasi suara sebagai penanda.
        """.trimIndent()

        val span = raw.toSpannable()

        fun bold(section: String) {
            val start = raw.indexOf(section)
            if (start != -1) {
                span.setSpan(
                    StyleSpan(android.graphics.Typeface.BOLD),
                    start,
                    start + section.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        bold("Panduan Penggunaan Aplikasi Narro")
        bold("Halaman Foto:")
        bold("Halaman Baca:")
        bold("Perintah suara yang dapat digunakan:")

        return span
    }
}
