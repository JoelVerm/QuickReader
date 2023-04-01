package com.j4a.quickreader

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Callable


class MainActivity : AppCompatActivity() {
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null
    private var imageToScreenCropRect: Rect? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (allPermissionsGranted()) {
            startCamera()
        } else
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )

        val shootButton = findViewById<Button>(R.id.shootButton)
        shootButton.setOnClickListener {
            takePhoto {
                val imageToScreenCropRect = imageToScreenCropRect ?: return@takePhoto
                val qr = ImageBitReader(it).read(imageToScreenCropRect)
                it.close()
                for (row in qr)
                    Log.d("QR code", row.contentToString())
                val decoder = QRDecoder(qr)
                val text = decoder.readQR()

                val intent = Intent(this, ReaderResult::class.java)
                intent.putExtra("QRResult", text)
                startActivity(intent)
            }
        }

        val generatorbutton = findViewById<Button>(R.id.generatorbutton)
        generatorbutton.setOnClickListener {
            val intent = Intent(this, GenerateQr::class.java)
            startActivity(intent)
        }

        val settingsbutton = findViewById<Button>(R.id.settingsbutton)
        settingsbutton.setOnClickListener {
            val intent = Intent(this, Settings::class.java)
            startActivity(intent)
        }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.cameraPreview).surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

            imageToScreenCropRect = preview.resolutionInfo?.cropRect

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto(callback: (ImageProxy) -> Unit) {
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    callback(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("QR Error", exception.toString())
                }
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "QuickReader"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}