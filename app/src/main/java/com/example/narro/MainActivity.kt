package com.example.narro

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.narro.databinding.ActivityMainBinding
import com.example.narro.databinding.DialogSpeechBinding
import androidx.appcompat.app.AlertDialog
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MainActivity : AppCompatActivity() {
    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var dialogBinding: DialogSpeechBinding
    private lateinit var speechDialog: AlertDialog
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var cameraSelector: CameraSelector
    private var lensFacing = CameraSelector.LENS_FACING_BACK

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

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

        setupSpeechButton()
    }

    @Suppress("ClickableViewAccessibility")
    private fun setupSpeechButton() {
        val fab = findViewById<FloatingActionButton>(R.id.fab_speech)
        fab.setOnTouchListener { _, event ->
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

//    private fun triggerSpeechRecording() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
//            showSpeechDialog()
//            speechRecognizer.startRecording()
//        } else {
//            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
//        }
//    }

    private fun handleCommand(command: String) {
        when (command) {
            "foto" -> takePhoto()
            "info" -> showGuide()
//            "berhenti" -> finish()
//            "halo" -> {
//                Toast.makeText(this, "Silakan ucapkan perintah", Toast.LENGTH_SHORT).show()
//                triggerSpeechRecording()
//            }
//            "kembali" -> finish()
//            "baca", "ulang" -> Toast.makeText(this, "Perintah $command tidak tersedia di halaman ini", Toast.LENGTH_SHORT).show()
            else -> Toast.makeText(this, "Perintah $command tidak tersedia", Toast.LENGTH_SHORT).show()
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
            android.util.Log.e("CameraX", "Failed to bind camera use cases", e)
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
}