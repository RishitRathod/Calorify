package com.example.foodcalories

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.foodcalories.databinding.ActivityCameraBinding
import com.example.foodcalories.detection.FruitVegetableClassifier
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var classifier: FruitVegetableClassifier
    
    private var isProcessing = false
    
    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize classifier
        classifier = FruitVegetableClassifier(this)
        
        // Set up capture button
        binding.captureButton.setOnClickListener {
            captureAndAnalyzePhoto()
        }
        
        // Set up back button
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        
        cameraExecutor = Executors.newSingleThreadExecutor()
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
            
            imageCapture = ImageCapture.Builder().build()
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Camera permissions not granted.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    
    private fun captureAndAnalyzePhoto() {
        if (isProcessing) return
        
        isProcessing = true
        binding.instructionText.text = "Processing..."
        binding.captureButton.isEnabled = false
        
        val imageCapture = imageCapture ?: return
        
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            File(cacheDir, "captured_image_${System.currentTimeMillis()}.jpg")
        ).build()
        
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    isProcessing = false
                    binding.instructionText.text = "Point camera at fruit or vegetable and tap capture"
                    binding.captureButton.isEnabled = true
                }
                
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: return
                    
                    try {
                        val bitmap = BitmapFactory.decodeFile(savedUri.path)
                        if (bitmap != null) {
                            Log.d(TAG, "Image captured successfully, size: ${bitmap.width}x${bitmap.height}")
                            classifier.classifyImage(bitmap) { result ->
                                runOnUiThread {
                                    Log.d(TAG, "Classification complete: ${result.label} (${result.confidence})")
                                    
                                    // Return result to MainActivity
                                    val intent = Intent().apply {
                                        putExtra("detected_item", result.label)
                                        putExtra("confidence", result.confidence)
                                    }
                                    setResult(RESULT_OK, intent)
                                    finish()
                                }
                            }
                        } else {
                            Log.e(TAG, "Failed to decode captured image")
                            runOnUiThread {
                                isProcessing = false
                                binding.instructionText.text = "Image capture failed. Try again."
                                binding.captureButton.isEnabled = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing captured image", e)
                        runOnUiThread {
                            isProcessing = false
                            binding.instructionText.text = "Processing failed. Try again."
                            binding.captureButton.isEnabled = true
                        }
                    }
                }
            }
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        classifier.close()
    }
}
