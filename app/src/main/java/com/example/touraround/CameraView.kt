package com.example.touraround


import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.maps.model.DirectionsLeg
import com.google.maps.model.DirectionsResult
import com.google.maps.model.DirectionsStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class CameraView : AppCompatActivity() {
    private lateinit var toggleFlash: ImageButton
    private lateinit var previewView: PreviewView
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
    private var camera: Camera? = null
    private var isFlashOn = false // Track the flashlight state

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val destination = LatLng(6.971339883324587, 79.87446757262208) // Los Angeles


    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera(cameraFacing)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.cameraPreview)
        toggleFlash = findViewById(R.id.toggleFlash)

        val arrowImageView: ImageView = findViewById(R.id.arrowImageView)
        // Show arrow initially
        arrowImageView.visibility = View.VISIBLE

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            activityResultLauncher.launch(Manifest.permission.CAMERA)
        } else {
            startCamera(cameraFacing)
        }

        toggleFlash.setOnClickListener {
            toggleFlashIcon()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

//        requestLocationAndProcessDirections()
    }

    private fun startCamera(cameraFacing: Int) {
        val aspectRatio = aspectRatio(previewView.width, previewView.height)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetAspectRatio(aspectRatio)
                .build()

            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(windowManager.defaultDisplay.rotation)
                .build()

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(cameraFacing)
                .build()

            cameraProvider.unbindAll()

            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            preview.setSurfaceProvider(previewView.surfaceProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleFlashIcon() {
        val currentCamera = camera
        if (currentCamera != null && currentCamera.cameraInfo.hasFlashUnit()) {
            isFlashOn = !isFlashOn
            currentCamera.cameraControl.enableTorch(isFlashOn)
            toggleFlash.setImageResource(
                if (isFlashOn) R.drawable.flash_off else R.drawable.flash_on
            )
        } else {
            runOnUiThread {
                Toast.makeText(
                    this,
                    "Flash is not available currently!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        return if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

//    private fun requestLocationAndProcessDirections() {
//        val locationRequest = LocationRequest.create().apply {
//            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
//            interval = 10000 // Update interval in milliseconds
//        }
//
//        val locationCallback = object : LocationCallback() {
//            override fun onLocationResult(locationResult: LocationResult?) {
//                locationResult?.lastLocation?.let { location ->
//                    val currentLatLng = LatLng(location.latitude, location.longitude)
//
//                    // Process the current location here
//                    GlobalScope.launch(Dispatchers.IO) {
//                        val directionsResult =
//                            DirectionsUtils.getDirections(this@CameraView, currentLatLng, destination)
//                        processDirectionsResult(directionsResult)
//                    }
//
//                    // Remove the location updates since we only need the current location once
//                    fusedLocationClient.removeLocationUpdates(this)
//                }
//            }
//        }
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//            == PackageManager.PERMISSION_GRANTED
//        ) {
//            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
//        } else {
//            ActivityCompat.requestPermissions(
//                this,
//                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//                LOCATION_PERMISSION_REQUEST_CODE
//            )
//        }
//    }
//
//    private fun processDirectionsResult(directionsResult: DirectionsResult?) {
//        if (directionsResult != null) {
//            // Process the directions result, e.g., extract route information, duration, etc.
//            val route = directionsResult.routes[0]
//            val legs: List<DirectionsLeg> = route.legs.toList()
//
//            for (leg in legs) {
//                val steps: List<DirectionsStep> = leg.steps.toList()
//                val distance = leg.distance.humanReadable
//                val duration = leg.duration.humanReadable
//                val startAddress = leg.startAddress
//                val endAddress = leg.endAddress
//
//                // Log directions details
//                println("Distance: $distance")
//                println("Duration: $duration")
//                println("Start Address: $startAddress")
//                println("End Address: $endAddress")
//
//                for (step in steps) {
//                    val instruction = step.htmlInstructions
//                    val stepDistance = step.distance.humanReadable
//                    val stepDuration = step.duration.humanReadable
//                    val startLocation = step.startLocation
//                    val endLocation = step.endLocation
//
//                    // Log turn-by-turn instruction
//                    println("Instruction: $instruction")
//                    println("Distance: $stepDistance")
//                    println("Duration: $stepDuration")
//                    println("Start Location: ${startLocation.lat}, ${startLocation.lng}")
//                    println("End Location: ${endLocation.lat}, ${endLocation.lng}")
//                }
//            }
//        }
//    }
//
//    companion object {
//        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
//    }
}