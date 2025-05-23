//package com.example.narro
//
//import android.app.Activity
//import android.content.ContentValues
//import android.content.DialogInterface
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.os.Build
//import androidx.appcompat.app.AppCompatActivity
//import android.os.Bundle
//import android.os.Environment
//import android.provider.MediaStore
//import android.widget.Toast
//import androidx.camera.core.AspectRatio
//import androidx.camera.core.Camera
//import androidx.camera.core.CameraProvider
//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageCapture
//import androidx.camera.core.ImageCapture.OutputFileOptions
//import androidx.camera.core.ImageCaptureException
//import androidx.camera.core.Preview
//import androidx.camera.core.resolutionselector.AspectRatioStrategy
//import androidx.camera.core.resolutionselector.ResolutionSelector
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.example.narro.databinding.ActivityMainBinding
//import java.io.File
//import java.text.SimpleDateFormat
//import java.util.Locale
//import kotlin.math.abs
//import androidx.fragment.app.FragmentManager
//import org.tensorflow.lite.Interpreter
//import android.speech.RecognizerIntent
//import com.google.android.material.floatingactionbutton.FloatingActionButton
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.util.*
//import kotlin.math.min
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.Dialog
//import android.os.*
//import android.speech.RecognitionListener
//import android.speech.SpeechRecognizer
//import android.speech.tts.TextToSpeech
//import android.util.Log
//import android.view.*
//import android.widget.*
//import java.util.*
//
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var tflite: Interpreter
//    private val labels = listOf("baca", "berhenti", "foto", "halo", "info", "kembali", "ulang")
//    private val SAMPLE_RATE = 16000
//    private val N_MFCC = 13
//    private val MAX_LEN = 930
//    private var speechRecognizer: SpeechRecognizer? = null
//    private var speechIntent: Intent? = null
//    private lateinit var speechDialog: Dialog
//    private lateinit var speechTextView: TextView
//    private lateinit var tts: TextToSpeech
//
//
//    private val mainBinding : ActivityMainBinding by lazy {
//        ActivityMainBinding.inflate(layoutInflater)
//    }
//
//    private val multiplePermissionId = 14
//    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= 33) {
//        arrayListOf(
//            android.Manifest.permission.CAMERA,
//            Manifest.permission.RECORD_AUDIO
//        )
//    } else {
//        arrayListOf(
//            android.Manifest.permission.CAMERA,
//            Manifest.permission.RECORD_AUDIO,
//            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            android.Manifest.permission.READ_EXTERNAL_STORAGE
//        )
//    }
//
//    private lateinit var imageCapture: ImageCapture
//    private lateinit var cameraProvider: ProcessCameraProvider
//    private lateinit var camera: Camera
//    private lateinit var cameraSelector: CameraSelector
//    private var lensFacing = CameraSelector.LENS_FACING_BACK
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(mainBinding.root)
//
//        if (checkMultiplePermission()) {
//            startCamera()
//        }
//
////        mainBinding.flipCameraIB.setOnClickListener {
////            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT){
////                CameraSelector.LENS_FACING_BACK
////            }else{
////                CameraSelector.LENS_FACING_FRONT
////            }
////            bindCameraUserCases()
////        }
//
//        mainBinding.captureIB.setOnClickListener {
//            takePhoto()
//        }
//
//        mainBinding.flashToggleIB.setOnClickListener {
//            setFlashIcon(camera)
//        }
//
//        showGuide()
//        mainBinding.appInfo.setOnClickListener { showGuide() }
//
//        tts = TextToSpeech(this) { status ->
//            if (status == TextToSpeech.SUCCESS) {
//                tts.language = Locale("id", "ID")
//            }
//        }
//
//
//        val fab = findViewById<FloatingActionButton>(R.id.fab_speech)
////        fab.setOnClickListener {
////            startSpeechToText()
////        }
//
//        tflite = Interpreter(loadModelFile("mymodele.tflite"))
//
//        setupSpeechButton()
//
//    }
//    @SuppressLint("ClickableViewAccessibility")
//    private fun setupSpeechButton() {
//        val fab = findViewById<FloatingActionButton>(R.id.fab_speech)
//        fab.setOnTouchListener { _, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
//                        showSpeechDialog()
//                        startListening()
//                    } else {
//                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), multiplePermissionId)
//                        Toast.makeText(this, "Izin mikrofon diperlukan", Toast.LENGTH_SHORT).show()
//                    }
//                    true
//                }
//                MotionEvent.ACTION_UP -> {
//                    stopListening()
//                    true
//                }
//                else -> false
//            }
//        }
//    }
//
//    private fun showSpeechDialog() {
//        speechDialog = Dialog(this)
//        speechDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        speechDialog.setContentView(R.layout.dialog_speech)
//
//        speechTextView = speechDialog.findViewById(R.id.speechText)
//        speechTextView.text = "Masukkan perintah suara..."
//
////        tts.speak("Masukkan perintah suara", TextToSpeech.QUEUE_FLUSH, null, null)
//
//        speechDialog.findViewById<Button>(R.id.closeButton).setOnClickListener {
//            speechDialog.dismiss()
//        }
//
//        speechDialog.setCancelable(false)
//        speechDialog.window?.setLayout(
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.WRAP_CONTENT
//        )
//
//        speechDialog.show()
//    }
//
//
////    private fun startSpeechToText() {
////        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
////        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
////        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
////        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Masukkan perintah")
////
////        try {
////            startActivityForResult(intent, 100)
////        } catch (e: Exception) {
////            Toast.makeText(this, "Speech recognition tidak tersedia", Toast.LENGTH_SHORT).show()
////        }
////    }
////
////    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
////        super.onActivityResult(requestCode, resultCode, data)
////        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
////            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
////            val spokenText = results?.get(0) ?: ""
////            Toast.makeText(this, "Kamu bilang: $spokenText", Toast.LENGTH_SHORT).show()
////
////            runModel(spokenText)
////        }
////    }
//    private fun startListening() {
//    if (SpeechRecognizer.isRecognitionAvailable(this)) {
//        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
//        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
//            override fun onReadyForSpeech(params: Bundle?) {}
//            override fun onBeginningOfSpeech() {}
//            override fun onRmsChanged(rmsdB: Float) {}
//            override fun onBufferReceived(buffer: ByteArray?) {}
//            override fun onEndOfSpeech() {}
//            override fun onError(error: Int) {
//                val errorMsg = when (error) {
//                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
//                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
//                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
//                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
//                    SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
//                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
//                    SpeechRecognizer.ERROR_SERVER -> "Server error"
//                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
//                    else -> "Unknown error: $error"
//                }
//                speechTextView.text = "Gagal mengenali suara: $errorMsg"
//                Log.e("SpeechRecognizer", "Error: $errorMsg")
//                Handler(Looper.getMainLooper()).postDelayed({
//                    speechDialog.dismiss()
//                }, 1500)
//            }
//
//            override fun onResults(results: Bundle?) {
//                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                val text = matches?.get(0) ?: ""
//                speechTextView.text = text
//                runModel(text)
//                Handler(Looper.getMainLooper()).postDelayed({
//                    speechDialog.dismiss()
//                }, 3000)
//            }
//
//            override fun onPartialResults(partialResults: Bundle?) {}
//            override fun onEvent(eventType: Int, params: Bundle?) {}
//        })
//
//        speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
//        speechIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//        speechIntent?.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
//        speechRecognizer?.startListening(speechIntent)
//    } else {
//        Toast.makeText(this, "Speech recognition not available on this device", Toast.LENGTH_LONG).show()
//        speechDialog.dismiss()
//    }
//}
//
//    private fun stopListening() {
//        speechRecognizer?.let {
//            it.stopListening()
//            it.cancel()
//            it.destroy()
//        }
//        speechRecognizer = null
//    }
//
//
//    private fun runModel(text: String) {
//        val fakeInput = generateDummyMFCC() // Temporary placeholder
//        val inputShape = tflite.getInputTensor(0).shape() // [1, 930, 13]
//        val inputBuffer = ByteBuffer.allocateDirect(4 * inputShape[1] * inputShape[2])
//        inputBuffer.order(ByteOrder.nativeOrder())
//
//        for (row in 0 until inputShape[1]) {
//            for (col in 0 until inputShape[2]) {
//                inputBuffer.putFloat(fakeInput[row][col])
//            }
//        }
//        inputBuffer.rewind()
//
//        val outputBuffer = ByteBuffer.allocateDirect(4 * labels.size)
//        outputBuffer.order(ByteOrder.nativeOrder())
//        tflite.run(inputBuffer, outputBuffer)
//        outputBuffer.rewind()
//
//        val scores = FloatArray(labels.size) { outputBuffer.float }
//        val maxIdx = scores.indices.maxByOrNull { scores[it] } ?: -1
//        val predictedLabel = labels.getOrNull(maxIdx) ?: "Unknown"
//        Toast.makeText(this, "Prediksi: $predictedLabel", Toast.LENGTH_LONG).show()
////        handleCommand(predictedLabel)
//    }
//
//    private fun loadModelFile(filename: String): ByteBuffer {
//        try {
//            val assetFileDescriptor = assets.openFd(filename)
//            val inputStream = assetFileDescriptor.createInputStream()
//            val fileChannel = inputStream.channel
//            val startOffset = assetFileDescriptor.startOffset
//            val declaredLength = assetFileDescriptor.declaredLength
//            return fileChannel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
//        } catch (e: Exception) {
//            Toast.makeText(this, "Failed to load model: ${e.message}", Toast.LENGTH_LONG).show()
//            Log.e("TensorFlowLite", "Model loading error", e)
//            throw e // Or handle gracefully
//        }
//    }
//
//    private fun generateDummyMFCC(): Array<FloatArray> {
//        val mfcc = Array(930) { FloatArray(13) }
//        for (i in 0 until 930) {
//            for (j in 0 until 13) {
//                mfcc[i][j] = (Math.random() * 0.1).toFloat()
//            }
//        }
//        return mfcc
//    }
//
//    private fun showGuide() {
//        if (supportFragmentManager.findFragmentByTag(GuideActivity.TAG) == null) {
//            GuideActivity().show(supportFragmentManager, GuideActivity.TAG)
//        }
//    }
//
//    private fun checkMultiplePermission(): Boolean {
//        val listPermissionNeeded = arrayListOf<String>()
//        for (permission in multiplePermissionNameList) {
//            if (ContextCompat.checkSelfPermission(
//                    this,
//                    permission
//                ) != PackageManager.PERMISSION_GRANTED
//            ) {
//                listPermissionNeeded.add(permission)
//            }
//        }
//        if (listPermissionNeeded.isNotEmpty()) {
//            ActivityCompat.requestPermissions(
//                this,
//                listPermissionNeeded.toTypedArray(),
//                multiplePermissionId
//            )
//            return false
//        }
//        return true
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray,
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (requestCode == multiplePermissionId) {
//            if (grantResults.isNotEmpty()) {
//                var isGrant = true
//                for (element in grantResults) {
//                    if (element == PackageManager.PERMISSION_DENIED) {
//                        isGrant = false
//                    }
//                }
//                if (isGrant) {
//                    // here all permission granted successfully
//                    startCamera()
//                } else {
//                    var someDenied = false
//                    for (permission in permissions) {
//                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
//                                this,
//                                permission
//                            )
//                        ) {
//                            if (ActivityCompat.checkSelfPermission(
//                                    this,
//                                    permission
//                                ) == PackageManager.PERMISSION_DENIED
//                            ) {
//                                someDenied = true
//                            }
//                        }
//                    }
//                    if (someDenied) {
//                        // here app Setting open because all permission is not granted
//                        // and permanent denied
//                        appSettingOpen(this)
//                    } else {
//                        // here warning permission show
//                        warningPermissionDialog(this) { _: DialogInterface, which: Int ->
//                            when (which) {
//                                DialogInterface.BUTTON_POSITIVE ->
//                                    checkMultiplePermission()
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun startCamera(){
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener({
//            cameraProvider = cameraProviderFuture.get()
//            bindCameraUserCases()
//        }, ContextCompat.getMainExecutor((this)))
//    }
//
//    private fun aspectRatio(width: Int, height: Int): Int {
//        val previewRatio = maxOf(width, height).toDouble() / minOf(width, height)
//        return if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
//            AspectRatio.RATIO_4_3
//        } else {
//            AspectRatio.RATIO_16_9
//        }
//    }
//
//    private fun bindCameraUserCases(){
//        val screenAspectRatio = aspectRatio(
//            mainBinding.previewView.width,
//            mainBinding.previewView.height
//        )
//        val rotation = mainBinding.previewView.display.rotation
//
//        val resolutionSelector = ResolutionSelector.Builder()
//            .setAspectRatioStrategy(
//                AspectRatioStrategy(
//                    screenAspectRatio,
//                    AspectRatioStrategy.FALLBACK_RULE_AUTO
//                )
//            )
//            .build()
//
//        val preview = Preview.Builder()
//            .setResolutionSelector(resolutionSelector)
//            .setTargetRotation(rotation)
//            .build()
//            .also {
//                it.surfaceProvider = mainBinding.previewView.surfaceProvider
//            }
//
//        imageCapture = ImageCapture.Builder()
//            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
//            .setResolutionSelector(resolutionSelector)
//            .setTargetRotation(rotation)
//            .build()
//
//        cameraSelector = CameraSelector.Builder()
//            .requireLensFacing(lensFacing)
//            .build()
//
//        try {
//            cameraProvider.unbindAll()
//
//            camera = cameraProvider.bindToLifecycle(
//                this, cameraSelector,preview,imageCapture
//            )
//        } catch (e:Exception){
//            e.printStackTrace()
//        }
//    }
//
//    private fun setFlashIcon(camera: Camera) {
//        if(camera.cameraInfo.hasFlashUnit()){
//            if (camera.cameraInfo.torchState.value == 0) {
//                camera.cameraControl.enableTorch(true)
//                mainBinding.flashToggleIB.setImageResource(R.drawable.flash_off)
//            }else {
//                camera.cameraControl.enableTorch(false)
//                mainBinding.flashToggleIB.setImageResource(R.drawable.flash_on)
//            }
//        }else{
//            Toast.makeText(
//                this,
//                "Flash is Not Available",
//                Toast.LENGTH_LONG
//            ).show()
//            mainBinding.flashToggleIB.isEnabled = false
//        }
//    }
//
//    private fun takePhoto() {
//
//        val imageFolder = File(
//            Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_PICTURES),"Images"
//        )
//        if (!imageFolder.exists()){
//            imageFolder.mkdir()
//        }
//
//        val fileName = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//            .format(System.currentTimeMillis()) + ".jpg"
//
//        val contentValues = ContentValues().apply {
//            put(MediaStore.Images.Media.DISPLAY_NAME,fileName)
//            put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P){
//                put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Images")
//            }
//        }
//
//        val metadata = ImageCapture.Metadata().apply {
//            isReversedHorizontal = (lensFacing == CameraSelector.LENS_FACING_FRONT)
//        }
//
//        val outputOption =
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                OutputFileOptions.Builder(
//                    contentResolver,
//                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                    contentValues
//                ).setMetadata(metadata).build()
//            }else{
//                val imageFile = File(imageFolder, fileName)
//                OutputFileOptions.Builder(imageFile)
//                    .setMetadata(metadata).build()
//            }
//
//        imageCapture.takePicture(
//            outputOption,
//            ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                    val savedUri = outputFileResults.savedUri
////                    if (savedUri != null) {
////                        val intent = Intent(this@MainActivity, ReadActivity::class.java).apply {
////                            putExtra("image_uri", savedUri.toString())
////                        }
////                        startActivity(intent)
////                    }
//
//                    if (savedUri != null) {
//                        val intent = Intent(this@MainActivity, LoadingActivity::class.java)
//                        intent.putExtra("image_uri", savedUri.toString())
//                        startActivity(intent)
//                    }
//                }
//
//                override fun onError(exception: ImageCaptureException) {
//                    Toast.makeText(
//                        this@MainActivity,
//                        exception.message.toString(),
//                        Toast.LENGTH_LONG
//                    ).show()
//                }
//
//            }
//        )
//    }
//}

//package com.example.narro
//
//import android.Manifest
//import android.annotation.SuppressLint
//import android.app.Dialog
//import android.content.ContentValues
//import android.content.DialogInterface
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.media.AudioFormat
//import android.media.AudioRecord
//import android.media.MediaRecorder
//import android.os.*
//import android.provider.MediaStore
//import android.speech.RecognitionListener
//import android.speech.RecognizerIntent
//import android.speech.SpeechRecognizer
//import android.speech.tts.TextToSpeech
//import android.util.Log
//import android.view.MotionEvent
//import android.view.Window
//import android.view.WindowManager
//import android.widget.Button
//import android.widget.TextView
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.camera.core.*
//import androidx.camera.core.resolutionselector.AspectRatioStrategy
//import androidx.camera.core.resolutionselector.ResolutionSelector
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import be.tarsos.dsp.AudioEvent
//import be.tarsos.dsp.io.TarsosDSPAudioFormat
//import com.example.narro.databinding.ActivityMainBinding
//import com.google.android.material.floatingactionbutton.FloatingActionButton
//import org.tensorflow.lite.Interpreter
//import be.tarsos.dsp.mfcc.MFCC
//import java.io.File
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.nio.channels.FileChannel
//import java.text.SimpleDateFormat
//import java.util.*
//import kotlin.math.abs
//import kotlin.math.max
//import kotlin.math.min
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var tflite: Interpreter
//    private val labels = listOf("baca", "berhenti", "foto", "halo", "info", "kembali", "ulang")
//    private val SAMPLE_RATE = 16000
//    private val N_MFCC = 13
//    private val MAX_LEN = 930
//    private var speechRecognizer: SpeechRecognizer? = null
//    private var speechIntent: Intent? = null
//    private lateinit var speechDialog: Dialog
//    private lateinit var speechTextView: TextView
//    private lateinit var tts: TextToSpeech
//    private var audioRecord: AudioRecord? = null
//    private var isRecording = false
//    private val audioBuffer = mutableListOf<Float>()
//    private val BUFFER_SIZE = 2048 // Ukuran buffer untuk perekaman
//
//    private val mainBinding: ActivityMainBinding by lazy {
//        ActivityMainBinding.inflate(layoutInflater)
//    }
//
//    private val multiplePermissionId = 14
//    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= 33) {
//        arrayListOf(
//            Manifest.permission.CAMERA,
//            Manifest.permission.RECORD_AUDIO
//        )
//    } else {
//        arrayListOf(
//            Manifest.permission.CAMERA,
//            Manifest.permission.RECORD_AUDIO,
//            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        )
//    }
//
//    private lateinit var imageCapture: ImageCapture
//    private lateinit var cameraProvider: ProcessCameraProvider
//    private lateinit var camera: Camera
//    private lateinit var cameraSelector: CameraSelector
//    private var lensFacing = CameraSelector.LENS_FACING_BACK
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(mainBinding.root)
//
//        if (checkMultiplePermission()) {
//            startCamera()
//        }
//
//        mainBinding.captureIB.setOnClickListener {
//            takePhoto()
//        }
//
//        mainBinding.flashToggleIB.setOnClickListener {
//            setFlashIcon(camera)
//        }
//
//        showGuide()
//        mainBinding.appInfo.setOnClickListener { showGuide() }
//
//        tts = TextToSpeech(this) { status ->
//            if (status == TextToSpeech.SUCCESS) {
//                tts.language = Locale("id", "ID")
//            }
//        }
//
//        try {
//            tflite = Interpreter(loadModelFile("mymodele.tflite"))
//        } catch (e: Exception) {
//            Log.e("TensorFlowLite", "Failed to initialize model", e)
//            Toast.makeText(this, "Gagal memuat model", Toast.LENGTH_LONG).show()
//        }
//
//        setupSpeechButton()
//    }
//
//    @SuppressLint("ClickableViewAccessibility")
//    private fun setupSpeechButton() {
//        val fab = findViewById<FloatingActionButton>(R.id.fab_speech)
//        fab.setOnTouchListener { _, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
//                        showSpeechDialog()
////                        tts.speak("Masukkan perintah suara", TextToSpeech.QUEUE_FLUSH, null, null)
//                        startRecording()
//                        startListening()
//                    } else {
//                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), multiplePermissionId)
//                        Toast.makeText(this, "Izin mikrofon diperlukan", Toast.LENGTH_SHORT).show()
//                    }
//                    true
//                }
//                MotionEvent.ACTION_UP -> {
//                    stopRecording()
//                    stopListening()
//                    true
//                }
//                else -> false
//            }
//        }
//    }
//
//    private fun showSpeechDialog() {
//        speechDialog = Dialog(this)
//        speechDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
//        speechDialog.setContentView(R.layout.dialog_speech)
//
//        speechTextView = speechDialog.findViewById(R.id.speechText)
//        speechTextView.text = "Merekam perintah suara..."
//
//        speechDialog.findViewById<Button>(R.id.closeButton).setOnClickListener {
//            stopRecording()
//            stopListening()
//            speechDialog.dismiss()
//        }
//
//        speechDialog.setCancelable(false)
//        speechDialog.window?.setLayout(
//            WindowManager.LayoutParams.MATCH_PARENT,
//            WindowManager.LayoutParams.WRAP_CONTENT
//        )
//
//        speechDialog.show()
//    }
//
//    private fun startRecording() {
//        val minBufferSize = AudioRecord.getMinBufferSize(
//            SAMPLE_RATE,
//            AudioFormat.CHANNEL_IN_MONO,
//            AudioFormat.ENCODING_PCM_16BIT
//        )
//
//        if (ActivityCompat.checkSelfPermission(
//                this,
//                Manifest.permission.RECORD_AUDIO
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return
//        }
//        audioRecord = AudioRecord(
//            MediaRecorder.AudioSource.MIC,
//            SAMPLE_RATE,
//            AudioFormat.CHANNEL_IN_MONO,
//            AudioFormat.ENCODING_PCM_16BIT,
//            minBufferSize
//        )
//
//        audioBuffer.clear()
//        isRecording = true
//
//        audioRecord?.startRecording()
//
//        // Mulai thread untuk membaca data audio
//        Thread {
//            val buffer = ShortArray(BUFFER_SIZE)
//            while (isRecording) {
//                val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
//                if (read > 0) {
//                    // Konversi short ke float dan normalisasi
//                    for (i in 0 until read) {
//                        audioBuffer.add(buffer[i] / 32768.0f) // Normalisasi ke [-1, 1]
//                    }
//                }
//            }
//        }.start()
//    }
//
//    private fun stopRecording() {
//        isRecording = false
//        audioRecord?.stop()
//        audioRecord?.release()
//        audioRecord = null
//    }
//
//    private fun extractMFCC(audioData: List<Float>): Array<FloatArray> {
//        val mfccFeatures = Array(MAX_LEN) { FloatArray(N_MFCC) { 0f } }
//        val frameSize = (SAMPLE_RATE * 0.02f).toInt() // 20ms
//        val stepSize = frameSize / 2
//
//        val floatArray = audioData.toFloatArray()
//        val buffer = FloatArray(frameSize)
//
//        var currentFrame = 0
//
//        for (i in 0 until floatArray.size - frameSize step stepSize) {
//            if (currentFrame >= MAX_LEN) break
//
//            System.arraycopy(floatArray, i, buffer, 0, frameSize)
//
//            // Konversi FloatArray ke ByteArray PCM 16-bit
//            val byteBuffer = ByteBuffer.allocate(frameSize * 2)
//            byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
//            for (sample in buffer) {
//                byteBuffer.putShort((sample * Short.MAX_VALUE).toInt().toShort())
//            }
//
//            val byteArray = byteBuffer.array()
//
//            // Buat AudioEvent dan MFCC
//            val audioFormat = TarsosDSPAudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, false)
//            val audioEvent = AudioEvent(audioFormat)
//            audioEvent.setFloatBuffer(buffer)
//
//
//            val mfcc = MFCC(
//                SAMPLE_RATE.toFloat().toInt(),
//                frameSize.toFloat() / SAMPLE_RATE,
//                N_MFCC,
//                40,
//                133.33f,
//                SAMPLE_RATE / 2.0f
//            )
//
//            mfcc.process(audioEvent)
//
//            val mfccResult = mfcc.mfcc
//            if (mfccResult != null && mfccResult.size >= N_MFCC) {
//                for (j in 0 until N_MFCC) {
//                    mfccFeatures[currentFrame][j] = mfccResult[j]
//                }
//                currentFrame++
//            }
//        }
//
//        // Padding kalau kurang panjang
//        while (currentFrame < MAX_LEN) {
//            for (j in 0 until N_MFCC) {
//                mfccFeatures[currentFrame][j] = 0f
//            }
//            currentFrame++
//        }
//
//        return mfccFeatures
//    }
//
//
//    private fun startListening() {
//        if (SpeechRecognizer.isRecognitionAvailable(this)) {
//            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
//            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
//                override fun onReadyForSpeech(params: Bundle?) {
//                    speechTextView.text = "Merekam perintah suara..."
//                }
//
//                override fun onBeginningOfSpeech() {
//                    speechTextView.text = "Mendengarkan..."
//                }
//
//                override fun onRmsChanged(rmsdB: Float) {}
//                override fun onBufferReceived(buffer: ByteArray?) {}
//                override fun onEndOfSpeech() {
//                    speechTextView.text = "Memproses perintah..."
//                }
//
//                override fun onError(error: Int) {
//                    val errorMsg = when (error) {
//                        SpeechRecognizer.ERROR_AUDIO -> "Kesalahan perekaman audio"
//                        SpeechRecognizer.ERROR_CLIENT -> "Kesalahan sisi klien"
//                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin tidak cukup"
//                        SpeechRecognizer.ERROR_NETWORK -> "Kesalahan jaringan"
//                        SpeechRecognizer.ERROR_NO_MATCH -> "Tidak ada kecocokan"
//                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Pengenal suara sibuk"
//                        SpeechRecognizer.ERROR_SERVER -> "Kesalahan server"
//                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waktu bicara habis"
//                        else -> "Kesalahan tidak diketahui: $error"
//                    }
//                    speechTextView.text = "Gagal: $errorMsg"
//                    tts.speak("Perintah tidak dikenali, ulangi", TextToSpeech.QUEUE_FLUSH, null, null)
//                    Log.e("SpeechRecognizer", "Error: $errorMsg")
//                    Handler(Looper.getMainLooper()).postDelayed({
//                        speechDialog.dismiss()
//                    }, 2000)
//                }
//
//                override fun onResults(results: Bundle?) {
//                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
//                    val text = matches?.get(0)?.lowercase(Locale("id", "ID")) ?: ""
//                    speechTextView.text = "Hasil: $text"
//                    runModel(text)
//                    Handler(Looper.getMainLooper()).postDelayed({
//                        speechDialog.dismiss()
//                    }, 3000)
//                }
//
//                override fun onPartialResults(partialResults: Bundle?) {}
//                override fun onEvent(eventType: Int, params: Bundle?) {}
//            })
//
//            speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
//                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
//                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
//            }
//            speechRecognizer?.startListening(speechIntent)
//        } else {
//            Toast.makeText(this, "Pengenalan suara tidak tersedia di perangkat ini", Toast.LENGTH_LONG).show()
//            tts.speak("Pengenalan suara tidak tersedia", TextToSpeech.QUEUE_FLUSH, null, null)
//            speechDialog.dismiss()
//        }
//    }
//
//    private fun stopListening() {
//        speechRecognizer?.let {
//            it.stopListening()
//            it.cancel()
//            it.destroy()
//        }
//        speechRecognizer = null
//    }
//
//    private fun runModel(text: String) {
//        // Check if the recognized text matches any label
//        val predictedLabel = if (labels.contains(text)) {
//            text
//        } else {
//            "Unknown"
//        }
//
//        if (predictedLabel == "Unknown") {
//            speechTextView.text = "Perintah tidak dikenali: $text"
//            tts.speak("Perintah tidak dikenali, ulangi", TextToSpeech.QUEUE_FLUSH, null, null)
//            Toast.makeText(this, "Perintah tidak dikenali", Toast.LENGTH_LONG).show()
//        } else {
//            // Ekstrak MFCC dari audio yang direkam
//            val mfccFeatures = extractMFCC(audioBuffer)
//            val inputShape = tflite.getInputTensor(0).shape() // [1, 930, 13]
//            val inputBuffer = ByteBuffer.allocateDirect(4 * inputShape[1] * inputShape[2])
//            inputBuffer.order(ByteOrder.nativeOrder())
//
//            for (row in 0 until inputShape[1]) {
//                for (col in 0 until N_MFCC) {
//                    inputBuffer.putFloat(mfccFeatures[row][col])
//                }
//            }
//            inputBuffer.rewind()
//
//            val outputBuffer = ByteBuffer.allocateDirect(4 * labels.size)
//            outputBuffer.order(ByteOrder.nativeOrder())
//            tflite.run(inputBuffer, outputBuffer)
//            outputBuffer.rewind()
//
//            val scores = FloatArray(labels.size) { outputBuffer.float }
//            val maxIdx = scores.indices.maxByOrNull { scores[it] } ?: -1
//            val modelPredictedLabel = labels.getOrNull(maxIdx) ?: "Unknown"
//
//            if (modelPredictedLabel == predictedLabel) {
//                speechTextView.text = "Perintah dikenali: $predictedLabel"
//                tts.speak("Perintah $predictedLabel dikenali", TextToSpeech.QUEUE_FLUSH, null, null)
//                Toast.makeText(this, "Perintah: $predictedLabel", Toast.LENGTH_LONG).show()
//                handleCommand(predictedLabel)
//            } else {
//                speechTextView.text = "Perintah tidak dikenali: $text"
//                tts.speak("Perintah tidak dikenali, ulangi", TextToSpeech.QUEUE_FLUSH, null, null)
//                Toast.makeText(this, "Perintah tidak dikenali", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
//
//    private fun handleCommand(command: String) {
//        when (command) {
//            "foto" -> takePhoto()
//            "info" -> showGuide()
//            else -> Toast.makeText(this, "Perintah $command belum diimplementasikan", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    private fun loadModelFile(filename: String): ByteBuffer {
//        try {
//            val assetFileDescriptor = assets.openFd(filename)
//            val inputStream = assetFileDescriptor.createInputStream()
//            val fileChannel = inputStream.channel
//            val startOffset = assetFileDescriptor.startOffset
//            val declaredLength = assetFileDescriptor.declaredLength
//            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
//        } catch (e: Exception) {
//            Log.e("TensorFlowLite", "Model loading error", e)
//            throw e
//        }
//    }
//
//    private fun showGuide() {
//        if (supportFragmentManager.findFragmentByTag(GuideActivity.TAG) == null) {
//            GuideActivity().show(supportFragmentManager, GuideActivity.TAG)
//        }
//    }
//
//    private fun checkMultiplePermission(): Boolean {
//        val listPermissionNeeded = arrayListOf<String>()
//        for (permission in multiplePermissionNameList) {
//            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
//                listPermissionNeeded.add(permission)
//            }
//        }
//        if (listPermissionNeeded.isNotEmpty()) {
//            ActivityCompat.requestPermissions(this, listPermissionNeeded.toTypedArray(), multiplePermissionId)
//            return false
//        }
//        return true
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == multiplePermissionId) {
//            if (grantResults.isNotEmpty()) {
//                var isGrant = true
//                for (element in grantResults) {
//                    if (element == PackageManager.PERMISSION_DENIED) {
//                        isGrant = false
//                    }
//                }
//                if (isGrant) {
//                    startCamera()
//                } else {
//                    var someDenied = false
//                    for (permission in permissions) {
//                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
//                            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
//                                someDenied = true
//                            }
//                        }
//                    }
//                    if (someDenied) {
//                        appSettingOpen(this)
//                    } else {
//                        warningPermissionDialog(this) { _, which ->
//                            if (which == DialogInterface.BUTTON_POSITIVE) {
//                                checkMultiplePermission()
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//        cameraProviderFuture.addListener({
//            cameraProvider = cameraProviderFuture.get()
//            bindCameraUserCases()
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    private fun aspectRatio(width: Int, height: Int): Int {
//        val previewRatio = max(width, height).toDouble() / min(width, height)
//        return if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
//            AspectRatio.RATIO_4_3
//        } else {
//            AspectRatio.RATIO_16_9
//        }
//    }
//
//    private fun bindCameraUserCases() {
//        val screenAspectRatio = aspectRatio(mainBinding.previewView.width, mainBinding.previewView.height)
//        val rotation = mainBinding.previewView.display.rotation
//
//        val resolutionSelector = ResolutionSelector.Builder()
//            .setAspectRatioStrategy(AspectRatioStrategy(screenAspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO))
//            .build()
//
//        val preview = Preview.Builder()
//            .setResolutionSelector(resolutionSelector)
//            .setTargetRotation(rotation)
//            .build()
//            .also {
//                it.surfaceProvider = mainBinding.previewView.surfaceProvider
//            }
//
//        imageCapture = ImageCapture.Builder()
//            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
//            .setResolutionSelector(resolutionSelector)
//            .setTargetRotation(rotation)
//            .build()
//
//        cameraSelector = CameraSelector.Builder()
//            .requireLensFacing(lensFacing)
//            .build()
//
//        try {
//            cameraProvider.unbindAll()
//            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
//        } catch (e: Exception) {
//            Log.e("CameraX", "Failed to bind camera use cases", e)
//        }
//    }
//
//    private fun setFlashIcon(camera: Camera) {
//        if (camera.cameraInfo.hasFlashUnit()) {
//            if (camera.cameraInfo.torchState.value == 0) {
//                camera.cameraControl.enableTorch(true)
//                mainBinding.flashToggleIB.setImageResource(R.drawable.flash_off)
//            } else {
//                camera.cameraControl.enableTorch(false)
//                mainBinding.flashToggleIB.setImageResource(R.drawable.flash_on)
//            }
//        } else {
//            Toast.makeText(this, "Flash tidak tersedia", Toast.LENGTH_LONG).show()
//            mainBinding.flashToggleIB.isEnabled = false
//        }
//    }
//
//    private fun takePhoto() {
//        val imageFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Images")
//        if (!imageFolder.exists()) {
//            imageFolder.mkdir()
//        }
//
//        val fileName = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//            .format(System.currentTimeMillis()) + ".jpg"
//
//        val contentValues = ContentValues().apply {
//            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
//            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Images")
//            }
//        }
//
//        val metadata = ImageCapture.Metadata().apply {
//            isReversedHorizontal = (lensFacing == CameraSelector.LENS_FACING_FRONT)
//        }
//
//        val outputOption = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//            ImageCapture.OutputFileOptions.Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
//                .setMetadata(metadata).build()
//        } else {
//            val imageFile = File(imageFolder, fileName)
//            ImageCapture.OutputFileOptions.Builder(imageFile).setMetadata(metadata).build()
//        }
//
//        imageCapture.takePicture(
//            outputOption,
//            ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                    val savedUri = outputFileResults.savedUri
//                    if (savedUri != null) {
//                        val intent = Intent(this@MainActivity, LoadingActivity::class.java)
//                        intent.putExtra("image_uri", savedUri.toString())
//                        startActivity(intent)
//                    }
//                }
//
//                override fun onError(exception: ImageCaptureException) {
//                    Toast.makeText(this@MainActivity, exception.message.toString(), Toast.LENGTH_LONG).show()
//                }
//            }
//        )
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        tts.shutdown()
//        stopRecording()
//        stopListening()
//        tflite.close()
//    }
//}

package com.example.narro

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.narro.databinding.ActivityMainBinding
import com.example.narro.databinding.DialogSpeechBinding
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.*
import android.media.AudioRecord
import android.media.AudioFormat
import android.media.MediaRecorder
import androidx.appcompat.app.AlertDialog
import be.tarsos.dsp.AudioEvent
import be.tarsos.dsp.mfcc.MFCC
import java.io.File
import java.text.SimpleDateFormat
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.core.app.ActivityCompat
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import org.tensorflow.lite.flex.FlexDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var tflite: Interpreter
    private val labels = listOf("baca", "berhenti", "foto", "halo", "info", "kembali", "ulang")
    private val SAMPLE_RATE = 16000
    private val N_MFCC = 13
    private val MAX_LEN = 930
    val frameSize = 1024 // Test with 1024
    private lateinit var dialogBinding: DialogSpeechBinding
    private lateinit var speechDialog: AlertDialog
    private lateinit var speechTextView: TextView
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val audioBuffer = mutableListOf<Float>()
    private val BUFFER_SIZE = 2048 // Buffer size for recording

    private val mainBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val multiplePermissionId = 14
    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= 33) {
        arrayListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    } else {
        arrayListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraSelector: CameraSelector
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mainBinding.root)

        if (checkMultiplePermission()) {
            startCamera()
        }

        mainBinding.captureIB.setOnClickListener {
            takePhoto()
        }

        mainBinding.flashToggleIB.setOnClickListener {
            setFlashIcon(camera)
        }

        showGuide()
        mainBinding.appInfo.setOnClickListener { showGuide() }

        initializeTFLite()

        setupSpeechButton()
    }

    private fun initializeTFLite() {
        try {
            val tfliteOptions = Interpreter.Options()
            tfliteOptions.addDelegate(FlexDelegate()) // Enable Flex delegate
            tflite = Interpreter(loadModelFile("mymodele.tflite"), tfliteOptions)
            Log.d("TFLite", "Model loaded successfully with Flex delegate")
        } catch (e: Exception) {
            Log.e("TFLite", "Failed to initialize model", e)
            Toast.makeText(this, "Gagal memuat model", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("ClickableViewAccessibility")
    private fun setupSpeechButton() {
        val fab = findViewById<FloatingActionButton>(R.id.fab_speech)
        fab.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        showSpeechDialog()
                        startRecording()
                    } else {
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    stopRecording()
                    processAudioWithModel()
                    true
                }
                else -> false
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            showSpeechDialog()
            startRecording()
        } else {
            Toast.makeText(this, "Izin mikrofon diperlukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSpeechDialog() {
        dialogBinding = DialogSpeechBinding.inflate(layoutInflater)
        speechTextView = dialogBinding.speechText
        speechTextView.text = "Merekam perintah suara..."

        speechDialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setCancelable(false)
            .create()

        dialogBinding.closeButton.setOnClickListener {
            stopRecording()
            speechDialog.dismiss()
        }

        speechDialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        speechDialog.show()
    }

    private fun startRecording() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            max(minBufferSize, BUFFER_SIZE)
        )

        audioBuffer.clear()
        isRecording = true

        audioRecord?.startRecording()

        Thread {
            val buffer = ShortArray(BUFFER_SIZE)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, BUFFER_SIZE) ?: 0
                if (read > 0) {
                    for (i in 0 until read) {
                        audioBuffer.add(buffer[i] / 32768.0f)
                    }
                    Log.d("Recording", "Current audio buffer size: ${audioBuffer.size}")
                }
            }
        }.start()
    }

    private fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun extractMFCC(audioData: List<Float>): Array<FloatArray> {
        val frameSize = 160  // Match training: 10 ms = 160 samples at 16000 Hz
        val stepSize = 80    // Match training: 5 ms = 80 samples
        val mfccFeatures = Array(MAX_LEN) { FloatArray(N_MFCC) { 0f } }

        if (audioData.isEmpty()) {
            Log.e("MFCC", "Audio data is empty")
            return mfccFeatures
        }

        val floatArray = audioData.toFloatArray()
        if (floatArray.size < frameSize) {
            Log.e("MFCC", "Audio data too short: ${floatArray.size}, need at least $frameSize")
            return mfccFeatures
        }

        val buffer = FloatArray(frameSize) { 0f }
        val audioFormat = TarsosDSPAudioFormat(SAMPLE_RATE.toFloat(), 16, 1, true, false)
        val mfcc = MFCC(frameSize, SAMPLE_RATE.toFloat(), N_MFCC, 22, 20.0f, 4000.0f) // Match n_mels=22, minFreq=20, maxFreq=4000

        var currentFrame = 0
        for (i in 0 until floatArray.size - frameSize step stepSize) {
            if (currentFrame >= MAX_LEN) break

            val copyLength = minOf(frameSize, floatArray.size - i)
            System.arraycopy(floatArray, i, buffer, 0, copyLength)
            if (copyLength < frameSize) {
                for (j in copyLength until frameSize) buffer[j] = 0f
            }

            Log.d("MFCC", "Frame $currentFrame buffer size: ${buffer.size}, first 10: ${buffer.take(10).joinToString()}")
            val audioEvent = AudioEvent(audioFormat)
            audioEvent.setFloatBuffer(buffer)

            try {
                mfcc.process(audioEvent)
                val mfccResult = mfcc.mfcc
                if (mfccResult != null && mfccResult.size >= N_MFCC) {
                    for (j in 0 until N_MFCC) mfccFeatures[currentFrame][j] = mfccResult[j]
                    currentFrame++
                } else {
                    Log.w("MFCC", "MFCC result null or invalid for frame $currentFrame")
                    break
                }
            } catch (e: Exception) {
                Log.e("MFCC", "Error processing frame $currentFrame: ${e.message}, buffer: ${buffer.contentToString().take(100)}")
                break
            }
        }

        if (currentFrame == 0) {
            Log.w("MFCC", "No valid frames processed, returning zeros")
        }
        Log.d("MFCC", "Processed $currentFrame frames")
        return mfccFeatures
    }

    private fun processAudioWithModel() {
        speechTextView.text = "Memproses perintah..."
        if (audioBuffer.isEmpty()) {
            speechTextView.text = "Tidak ada data audio untuk diproses"
            Handler(Looper.getMainLooper()).postDelayed({ speechDialog.dismiss() }, 2000)
            return
        }

        val mfccFeatures = extractMFCC(audioBuffer)
        val inputShape = tflite.getInputTensor(0).shape()
        Log.d("TFLite", "Expected input shape: ${inputShape.contentToString()}, got: [1, $MAX_LEN, $N_MFCC]")

        val inputBuffer = ByteBuffer.allocateDirect(4 * MAX_LEN * N_MFCC)
        inputBuffer.order(ByteOrder.nativeOrder())
        for (row in 0 until MAX_LEN) {
            for (col in 0 until N_MFCC) {
                inputBuffer.putFloat(mfccFeatures[row][col])
            }
        }
        inputBuffer.rewind()
        Log.d("TFLite", "Input buffer first 10 values: ${List(10) { inputBuffer.getFloat() }.joinToString()}")
        inputBuffer.rewind()

        val outputBuffer = ByteBuffer.allocateDirect(4 * labels.size)
        outputBuffer.order(ByteOrder.nativeOrder())
        tflite.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()

        val scores = FloatArray(labels.size) { outputBuffer.float }
        Log.d("TFLite", "Scores: ${scores.contentToString()}")
        val maxIdx = scores.indices.maxByOrNull { scores[it] } ?: -1
        val predictedLabel = labels.getOrNull(maxIdx) ?: "Unknown"

        speechTextView.text = if (predictedLabel == "Unknown") {
            "Perintah tidak dikenali"
        } else {
            "Perintah dikenali: $predictedLabel"
        }

        Toast.makeText(this, speechTextView.text, Toast.LENGTH_LONG).show()
        Handler(Looper.getMainLooper()).postDelayed({ speechDialog.dismiss() }, 3000)
    }

    private fun loadModelFile(filename: String): ByteBuffer {
        try {
            val assetFileDescriptor: AssetFileDescriptor = assets.openFd(filename)
            val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            Log.e("TFLite", "Model loading error", e)
            throw e
        }
    }

    private fun showGuide() {
        if (supportFragmentManager.findFragmentByTag(GuideActivity.TAG) == null) {
            GuideActivity().show(supportFragmentManager, GuideActivity.TAG)
        }
    }

    private fun checkMultiplePermission(): Boolean {
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in multiplePermissionNameList) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                listPermissionNeeded.add(permission)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionNeeded.toTypedArray(), multiplePermissionId)
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == multiplePermissionId) {
            if (grantResults.isNotEmpty()) {
                var isGrant = true
                for (element in grantResults) {
                    if (element == PackageManager.PERMISSION_DENIED) {
                        isGrant = false
                    }
                }
                if (isGrant) {
                    startCamera()
                } else {
                    var someDenied = false
                    for (permission in permissions) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
                                someDenied = true
                            }
                        }
                    }
                    if (someDenied) {
                        appSettingOpen(this)
                    } else {
                        warningPermissionDialog(this) { _, which ->
                            if (which == DialogInterface.BUTTON_POSITIVE) {
                                checkMultiplePermission()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUserCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        return if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    private fun bindCameraUserCases() {
        val screenAspectRatio = aspectRatio(mainBinding.previewView.width, mainBinding.previewView.height)
        val rotation = mainBinding.previewView.display.rotation

        val resolutionSelector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy(screenAspectRatio, AspectRatioStrategy.FALLBACK_RULE_AUTO))
            .build()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()
            .also {
                it.setSurfaceProvider(mainBinding.previewView.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(resolutionSelector)
            .setTargetRotation(rotation)
            .build()

        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch (e: Exception) {
            Log.e("CameraX", "Failed to bind camera use cases", e)
        }
    }

    private fun setFlashIcon(camera: Camera) {
        if (camera.cameraInfo.hasFlashUnit()) {
            if (camera.cameraInfo.torchState.value == 0) {
                camera.cameraControl.enableTorch(true)
                mainBinding.flashToggleIB.setImageResource(R.drawable.flash_off)
            } else {
                camera.cameraControl.enableTorch(false)
                mainBinding.flashToggleIB.setImageResource(R.drawable.flash_on)
            }
        } else {
            Toast.makeText(this, "Flash tidak tersedia", Toast.LENGTH_LONG).show()
            mainBinding.flashToggleIB.isEnabled = false
        }
    }

    private fun takePhoto() {
        val imageFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Images")
        if (!imageFolder.exists()) {
            imageFolder.mkdir()
        }

        val fileName = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(System.currentTimeMillis()) + ".jpg"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Images")
            }
        }

        val metadata = ImageCapture.Metadata().apply {
            isReversedHorizontal = (lensFacing == CameraSelector.LENS_FACING_FRONT)
        }

        val outputOption = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            ImageCapture.OutputFileOptions.Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                .setMetadata(metadata).build()
        } else {
            val imageFile = File(imageFolder, fileName)
            ImageCapture.OutputFileOptions.Builder(imageFile).setMetadata(metadata).build()
        }

        imageCapture.takePicture(
            outputOption,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = outputFileResults.savedUri
                    if (savedUri != null) {
                        val intent = Intent(this@MainActivity, LoadingActivity::class.java)
                        intent.putExtra("image_uri", savedUri.toString())
                        startActivity(intent)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(this@MainActivity, exception.message.toString(), Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        tflite.close()
    }
}