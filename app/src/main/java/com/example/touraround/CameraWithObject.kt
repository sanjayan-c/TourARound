package com.example.touraround

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import org.tensorflow.lite.task.vision.detector.Detection
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.util.LinkedList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraWithObject : AppCompatActivity(), ObjectDetectorHelper.DetectorListener {

    private val TAG = "ObjectDetection"

    private lateinit var objectDetectorHelper: ObjectDetectorHelper
    private lateinit var bitmapBuffer: Bitmap
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var gpuDelegate: GpuDelegate
    private lateinit var backButton: ImageButton

    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService

    override fun onResume() {
        super.onResume()

        // Make sure that all permissions are still present, since the
        // user could have removed them while the app was in paused state.
    }
    override fun onBackPressed() {
        super.onBackPressed()
        finish() // Finish the activity
    }


    override fun onDestroy() {
        super.onDestroy()
        // Release camera resources
        if (cameraProvider != null) {
            cameraProvider?.unbindAll()
        }

        // Shut down our background executor
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }

    override fun onPause() {
        super.onPause()

        // Release the camera when the activity goes into the background
        if (cameraProvider != null) {
            cameraProvider?.unbindAll()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.object_detection)

        objectDetectorHelper = ObjectDetectorHelper(
            context = applicationContext,
            objectDetectorListener = this
        )
        // Initialize our background executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Wait for the views to be properly laid out
        findViewById<View>(R.id.view_finder).post {
            // Set up the camera and its use cases
            setUpCamera()
        }
        // Attach listeners to UI control widgets
        initBottomSheetControls()

        backButton = findViewById<ImageButton>(R.id.backbtn)
        backButton.setOnClickListener {
            // Handle the click event (e.g., navigate to a previous screen)
            val intent = Intent(this, CameraView::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun initBottomSheetControls() {
        // When clicked, lower detection score threshold floor
        findViewById<View>(R.id.threshold_minus).setOnClickListener {
            if (objectDetectorHelper.threshold >= 0.1f) {
                objectDetectorHelper.threshold = objectDetectorHelper.threshold.minus(0.1f)
                updateControlsUi()
            }
        }

        // When clicked, raise detection score threshold floor
        findViewById<View>(R.id.threshold_plus).setOnClickListener {
            if (objectDetectorHelper.threshold <= 0.8f) {
                objectDetectorHelper.threshold = objectDetectorHelper.threshold.plus(0.1f)
                updateControlsUi()
            }
        }

        // When clicked, reduce the number of objects that can be detected at a time
        findViewById<View>(R.id.max_results_minus).setOnClickListener {
            if (objectDetectorHelper.maxResults > 1) {
                objectDetectorHelper.maxResults--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of objects that can be detected at a time
        findViewById<View>(R.id.max_results_plus).setOnClickListener {
            if (objectDetectorHelper.maxResults < 5) {
                objectDetectorHelper.maxResults++
                updateControlsUi()
            }
        }

        // When clicked, decrease the number of threads used for detection
        findViewById<View>(R.id.threads_minus).setOnClickListener {
            if (objectDetectorHelper.numThreads > 1) {
                objectDetectorHelper.numThreads--
                updateControlsUi()
            }
        }

        // When clicked, increase the number of threads used for detection
        findViewById<View>(R.id.threads_plus).setOnClickListener {
            if (objectDetectorHelper.numThreads < 4) {
                objectDetectorHelper.numThreads++
                updateControlsUi()
            }
        }

        // When clicked, change the underlying hardware used for inference. Current options are CPU,
        // GPU, and NNAPI
        val spinnerDelegate = findViewById<Spinner>(R.id.spinner_delegate)
        spinnerDelegate.setSelection(0, false)
        spinnerDelegate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                objectDetectorHelper.currentDelegate = p2
                updateControlsUi()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                // No operation
            }
        }

        // When clicked, change the underlying model used for object detection
        val spinnerModel = findViewById<Spinner>(R.id.spinner_model)
        spinnerModel.setSelection(0, false)
        spinnerModel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                objectDetectorHelper.currentModel = p2
                updateControlsUi()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                // No operation
            }
        }
    }

    // Update the values displayed in the bottom sheet. Reset the detector.
    private fun updateControlsUi() {
        val maxResultsValue = findViewById<TextView>(R.id.max_results_value)
        maxResultsValue.text = objectDetectorHelper.maxResults.toString()

        val thresholdValue = findViewById<TextView>(R.id.threshold_value)
        thresholdValue.text = String.format("%.2f", objectDetectorHelper.threshold)

        val threadsValue = findViewById<TextView>(R.id.threads_value)
        threadsValue.text = objectDetectorHelper.numThreads.toString()

        // Needs to be cleared instead of reinitialized because the GPU
        // delegate needs to be initialized on the thread using it when applicable
        objectDetectorHelper.clearObjectDetector()
        val overlay = findViewById<ObjectOverlay>(R.id.overlay)
        overlay.clear()
    }

    // Initialize CameraX and prepare to bind the camera use cases
    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                // CameraProvider
                cameraProvider = cameraProviderFuture.get()

                // Build and bind the camera use cases
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    // Declare and bind preview, capture, and analysis use cases
    @SuppressLint("UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        // CameraProvider
        val cameraProvider =
            cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector - makes the assumption that we're only using the back camera
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()

        // Preview. Only using the 4:3 ratio because this is the closest to our models
        preview =
            Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(findViewById<View>(R.id.view_finder).display.rotation)
                .build()

        // ImageAnalysis. Using RGBA 8888 to match how our models work
        imageAnalyzer =
            ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(findViewById<View>(R.id.view_finder).display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

        // The analyzer can then be assigned to the instance
        imageAnalyzer?.setAnalyzer(cameraExecutor) { image ->
            if (!::bitmapBuffer.isInitialized || bitmapBuffer.isRecycled) {
                // The image rotation and RGB image buffer are initialized only once
                // the analyzer has started running
                bitmapBuffer = Bitmap.createBitmap(
                    image.width,
                    image.height,
                    Bitmap.Config.ARGB_8888
                )
            }

            detectObjects(image)
        }

        // Must unbind the use cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use cases can be passed here -
            // the camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            val viewFinder = findViewById<PreviewView>(R.id.view_finder)

            // Attach the viewfinder's surface provider to the preview use case
            preview?.setSurfaceProvider(viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun detectObjects(image: ImageProxy) {
        // Copy out RGB bits to the shared bitmap buffer
        image.use { bitmapBuffer.copyPixelsFromBuffer(image.planes[0].buffer) }

        val imageRotation = image.imageInfo.rotationDegrees
        // Pass Bitmap and rotation to the object detector helper for processing and detection
        objectDetectorHelper.detect(bitmapBuffer, imageRotation)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation = findViewById<View>(R.id.view_finder).display.rotation
    }

    // Update UI after objects have been detected. Extracts the original image height/width
    // to scale and place bounding boxes properly through OverlayView
    override fun onResults(
        results: MutableList<Detection>?,
        inferenceTime: Long,
        imageHeight: Int,
        imageWidth: Int
    ) {
        runOnUiThread {
            val inferenceTimeVal = findViewById<TextView>(R.id.inference_time_val)
            inferenceTimeVal.text = String.format("%d ms", inferenceTime)

            // Pass necessary information to OverlayView for drawing on the canvas
            val overlay = findViewById<ObjectOverlay>(R.id.overlay)
            overlay.setResults(results ?: LinkedList<Detection>(), imageHeight, imageWidth)

            // Force a redraw
            overlay.invalidate()
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
        }
    }
}



