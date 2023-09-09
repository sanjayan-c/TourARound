package com.example.touraround

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class CameraView : AppCompatActivity(), SensorEventListener {
    private lateinit var toggleFlash: ImageButton
    private lateinit var previewView: PreviewView
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
    private var camera: Camera? = null
    private var isFlashOn = false // Track the flashlight state

    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    private val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 101
    private val TIME_BETWEEN_UPDATES: Long = 1000 // Adjust as needed
    private val MIN_TIME_BETWEEN_UPDATES: Long = 1000
    private val MIN_DISTANCE_CHANGE_FOR_UPDATES: Float = 1f // Adjust as needed
    private var currentLocation: Location? = null

    //private val destination = LatLng(6.971339883324587, 79.87446757262208) // Mattakuliya Food City
    //private val destination = LatLng(6.96557381762747, 79.86631999619358) // St. James Church
    //private val destination = LatLng(6.914869207457449, 79.97295522337072) // SLIIT Malabe
    private val destination = LatLng(6.967464608431239, 79.86920268732987)

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)
    private lateinit var arrowImageView: ImageView

    private val destinationPoints: MutableList<Pair<String, LatLng>> = mutableListOf()
    private var currentDestinationIndex = 0
    private val radiusThreshold = 10.0

    private var calculatedArrowAngle: Float = 0.0f

    private val rotationHistory = mutableListOf<Float>()
    private val maxHistorySize = 50 // Adjust this as needed

    private var hasRetrievedDirections = false // rack if directions have been retrieved

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startCamera(cameraFacing)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {

        // Get a reference to the system's sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // Obtain a reference to the device's accelerometer sensor
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // Obtain a reference to the device's magnetometer sensor
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        previewView = findViewById(R.id.cameraPreview)
        toggleFlash = findViewById(R.id.toggleFlash)
        arrowImageView = findViewById(R.id.arrowImageView)

        // Initially, set arrowImageView visibility to INVISIBLE
        arrowImageView.visibility = View.INVISIBLE

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

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Handle location updates here
                val location = locationResult.lastLocation
                currentLocation = location // Update currentLocation
                currentLocation?.let { nonNullLocation ->
                    logLocation(nonNullLocation)
                    // Use 'nonNullLocation' to access latitude and longitude, for example
                }
                if (hasRetrievedDirections) {
                    // Call checkDistanceAndUpdate when hasRetrievedDirections is true
                    checkDistanceAndUpdate(LatLng(location.latitude, location.longitude))
                }
            }
        }

        // Check for location permission and request it if not granted
        if (hasLocationPermission()) {
            // Permission already granted, start location updates
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }

        val selectDestinationImageView = findViewById<ImageView>(R.id.selectDestination)
        val stopDirectionsText = findViewById<View>(R.id.stopDirectionsText)

        selectDestinationImageView.setOnClickListener {
            // Show navigation directions here
            showNavigationDirections()
        }

        stopDirectionsText.setOnClickListener {
            // Stop navigation and reset everything here
            stopNavigationAndReset()
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
        } else if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, 3)
        }

        val rotationMatrix = FloatArray(9)
        val success = SensorManager.getRotationMatrix(
            rotationMatrix,
            null,
            accelerometerReading,
            magnetometerReading
        )
        if (success) {
            val orientationValues = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientationValues)

            // Calculate the angle based on the device's orientation
            val sensorAngle = calculateAngle(orientationValues)

            // Smooth the rotation using a moving average
            rotationHistory.add(sensorAngle)
            if (rotationHistory.size > maxHistorySize) {
                rotationHistory.removeAt(0)
            }

            // Calculate the average rotation from history
            val smoothedRotation = rotationHistory.average().toFloat()

            // Combine the sensor angle and the calculated angle
            val finalArrowAngle = smoothedRotation + calculatedArrowAngle

            // Update the arrow's rotation with the finalArrowAngle
            arrowImageView.rotation = finalArrowAngle
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Handle accuracy changes if needed
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
                    "Flash is not available currently!!",
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

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // On older Android versions, background location permission is not needed
            true
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    private fun requestBackgroundLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf( Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, check for background location permission
                if (hasBackgroundLocationPermission()) {
                    // Background location permission is granted or not needed, start location updates
                    startLocationUpdates()
                } else {
                    // Request background location permission
                    requestBackgroundLocationPermission()
                }
            } else {
                // Permission denied, handle accordingly
                // You can show a message to the user or disable location-related functionality
            }
        } else if (requestCode == BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Background location permission granted, start location updates
                startLocationUpdates()
            } else {
                // Background location permission denied, handle accordingly
                // You can show a message to the user or disable background location-related functionality
            }
        }
    }

    private fun startLocationUpdates() {
        if (hasLocationPermission()) {
            val locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(TIME_BETWEEN_UPDATES)
                .setFastestInterval(MIN_TIME_BETWEEN_UPDATES)
                .setSmallestDisplacement(MIN_DISTANCE_CHANGE_FOR_UPDATES)
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                Log.d("LocationUpdates", "Location updates started.")
            } catch (securityException: SecurityException) {
                // Handle SecurityException if it occurs
                Log.e("LocationUpdates", "Error requesting location updates: ${securityException.message}")
            }
        }
    }

//    override fun onStop() {
//        super.onStop()
//        fusedLocationClient.removeLocationUpdates(locationCallback)
//        Log.d("LocationUpdates", "Location updates stopped.")
//    }

    private fun logLocation(location: Location) {
        Log.d("LocationUpdates", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
    }

    private fun requestLocationAndProcessDirections(location: Location) {
        // Pass 'location' to your directions processing code here
        val currentLatLng = LatLng(location.latitude, location.longitude)

        if (!hasRetrievedDirections) {
            // Retrieve directions only the first time
            GlobalScope.launch(Dispatchers.IO) {
                val directionsResult = DirectionsUtils.getDirections(
                    this@CameraView,
                    currentLatLng,
                    destination
                )
                processDirectionsResult(directionsResult)

                // Log statements to execute after directions have been retrieved
                println("Location result received.")
                println("Destination Points: $destinationPoints")
                checkDistanceAndUpdate(currentLatLng)
            }
            hasRetrievedDirections = true
        }

        // Log the current GPS coordinates
        println("Current Location: ${currentLatLng.latitude}, ${currentLatLng.longitude}")
        checkDistanceAndUpdate(currentLatLng)
    }
    private fun processDirectionsResult(directionsResult: DirectionsResult?) {
        if (directionsResult != null) {
            // Process the directions result, e.g., extract route information, duration, etc.
            val route = directionsResult.routes[0]
            val legs: List<DirectionsLeg> = route.legs.toList()

            for (leg in legs) {
                val steps: List<DirectionsStep> = leg.steps.toList()
                val distance = leg.distance.inMeters
                val duration = leg.duration.humanReadable
                val startAddress = leg.startAddress
                val endAddress = leg.endAddress

                // Log directions details
                println("Distance: $distance")
                println("Duration: $duration")
                println("Start Address: $startAddress")
                println("End Address: $endAddress")

                for (step in steps) {
                    val instruction = step.htmlInstructions
                    val stepDistance = step.distance.humanReadable
                    val stepDuration = step.duration.humanReadable
                    val startLocation = step.startLocation
                    val endLocation = step.endLocation

                    // Log turn-by-turn instruction
                    println("Instruction: $instruction")
                    println("Distance: $stepDistance")
                    println("Duration: $stepDuration")
                    println("Start Location: ${startLocation.lat}, ${startLocation.lng}")
                    println("End Location: ${endLocation.lat}, ${endLocation.lng}")

                    // Add the end location of each step to the destinationPoints list
                    destinationPoints.add(Pair(instruction, LatLng(endLocation.lat, endLocation.lng)))
                }
            }
        }
    }

    private fun checkDistanceAndUpdate(currentLatLng: LatLng) {
        Log.d("checkDistanceAndUpdate", "Checking distance and updating")
        Log.d("checkDistanceAndUpdate:","$currentDestinationIndex")
        println("Destination Points: $destinationPoints")

        if (currentDestinationIndex <= destinationPoints.size - 1) {

           // Check if we have reached the radius of the current destination
            val distanceToDestination = calculateDistance(
                currentLatLng,
                destinationPoints[currentDestinationIndex].second
            )

            // Update distance to the next turn in the TextView
            val textViewRemainingDistance = findViewById<TextView>(R.id.textRemainingDistance)
            // Show total distance left in the TextView
            val textViewRemainingTotalDistance = findViewById<TextView>(R.id.textRemainingTotalDistance)

            // Calculate the total distance left
            val totalDistanceLeft = calculateTotalDistanceLeftFromCurrentLocation(distanceToDestination, currentDestinationIndex)
            if(totalDistanceLeft<1000){
                // Round the distance to the nearest decimal number with one decimal place
                val roundedTotalDistance = String.format("%.0f", totalDistanceLeft)
                textViewRemainingTotalDistance.text = "${roundedTotalDistance} m"
            }else{
                // Round the distance to the nearest decimal number with one decimal place
                val roundedTotalDistance = String.format("%.2f", totalDistanceLeft/1000.00)
                textViewRemainingTotalDistance.text = "${roundedTotalDistance} km"
            }

            println("Distance to Destination: $distanceToDestination meters")
            println("Current Destination Index: $currentDestinationIndex")
            // Display the coordinates of the next destination
            val nextDestination2 = destinationPoints[currentDestinationIndex].second
            println("First Next Destination Coordinates: ${nextDestination2.latitude}, ${nextDestination2.longitude}")

            //Show distance left to the next turn
            if (distanceToDestination > radiusThreshold && distanceToDestination<1000) {
                // Round the distance to the nearest decimal number with one decimal place
                val roundedDistance = String.format("%.0f", distanceToDestination)
                textViewRemainingDistance.text = "Next turn in: ${roundedDistance} m"
            }else if(distanceToDestination>=1000){
                val roundedDistance = String.format("%.2f", distanceToDestination/1000.00)
                textViewRemainingDistance.text = "Next turn in: ${roundedDistance} km"
            }else if (distanceToDestination <= radiusThreshold) {
                if (currentDestinationIndex == destinationPoints.size - 1) {
                    textViewRemainingDistance.text = "Destination has arrived"
                    textViewRemainingTotalDistance.setTextColor(Color.parseColor("#D55B07"))
                    textViewRemainingTotalDistance.text = "0 m"
                    // Update the arrow's visibility on the main thread
                    runOnUiThread {
                        arrowImageView.visibility = View.INVISIBLE
                    }
                    currentDestinationIndex++
                    return
                    // Handle logic for reaching the final destination
                } else {
                    // Debugging: Print relevant values
                    println("Check Distance to Destination: $distanceToDestination meters")
                    // Move to the next destination point
                    currentDestinationIndex++
                    println("New Current Destination Index: $currentDestinationIndex")
                    val currentInstruction = destinationPoints[currentDestinationIndex].first
                    // Remove HTML tags and content within <div> tags
                    val cleanInstruction = currentInstruction
                        .replace(Regex("<[^>]*>"), "") // Remove HTML tags
                        .replace(Regex("\\(.*?\\)"), "") // Remove content within parentheses
                    // Add spaces between concatenated words (e.g., "StPass" becomes "St Pass")
                    val cleanInstructionWithSpaces = cleanInstruction.replace(Regex("(?<=\\w)(?=[A-Z])"), " ")
                    textViewRemainingDistance.text = "$cleanInstructionWithSpaces"
                    // Display the cleanInstruction for 5 seconds
                    // Delay further processing for 5 seconds
                    val handler = Handler()
                    handler.postDelayed({
                        // Continue with further processing after 5 seconds if needed
                    }, 5000) // 5000 milliseconds = 5 seconds
                }
            }

            // Calculate the angle to the next destination point
            val angle = angleFromCoordinate(
                currentLatLng.latitude,
                currentLatLng.longitude,
                destinationPoints[currentDestinationIndex].second.latitude,
                destinationPoints[currentDestinationIndex].second.longitude
            )
            // Update the currentArrowAngle with the calculated angle
            calculatedArrowAngle = angle.toFloat()
            // Update the arrow's visibility on the main thread
            runOnUiThread {
                arrowImageView.visibility = View.VISIBLE
            }
            println("Angle to Next Destination: $angle degrees")
            // Display the coordinates of the next destination
            val nextDestination = destinationPoints[currentDestinationIndex].second
            println("Next Destination Coordinates: ${nextDestination.latitude}, ${nextDestination.longitude}")
        }
    }
    private fun calculateAngle(orientationValues: FloatArray): Float {
        // Calculate the angle based on the device's orientation
        val angleFromNorth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
        return -angleFromNorth // Invert the angle for correct rotation
    }
    private fun angleFromCoordinate(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val dLon = (long2 - long1)
        val y = Math.sin(Math.toRadians(dLon)) * Math.cos(Math.toRadians(lat2))
        val x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) -
                Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(dLon))
        var brng = Math.toDegrees(Math.atan2(y, x))
        brng = (brng + 360) % 360
        return brng
    }
    private fun calculateDistance(start: LatLng, end: LatLng): Double {
        // Haversine formula to calculate the distance between two LatLng points
        val radius = 6371 // Earth's radius in kilometers
        val lat1 = Math.toRadians(start.latitude)
        val lat2 = Math.toRadians(end.latitude)
        val lon1 = Math.toRadians(start.longitude)
        val lon2 = Math.toRadians(end.longitude)

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1
        //The square of half the chord length between the two points on the Earth's surface
        val a = Math.sin(dLat / 2).pow(2) + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2).pow(2)
        //The central angle between the two points on the Earth's surface.
        val c = 2 * Math.asin(Math.sqrt(a))

        return radius * c * 1000 // Convert to meters
    }
    private fun calculateTotalDistanceLeftFromCurrentLocation(distanceToDestination: Double, startIndex: Int): Double {
        var distanceFromIndex = 0.0

        // Calculate the total distance left from the current location to the last end location
        for (i in startIndex until destinationPoints.size - 1) {
            distanceFromIndex += calculateDistance(destinationPoints[i].second, destinationPoints[i + 1].second)
        }
        return distanceToDestination + distanceFromIndex
    }

    private fun showNavigationDirections() {
        val location = currentLocation // Assign currentLocation to a local variable
        if (location != null) {
            val showOverlayButton = findViewById<ImageButton>(R.id.showOverlayButton)
            val hideOverlayButton = findViewById<ImageButton>(R.id.hideOverlayButton)
            val overlayLayout = findViewById<View>(R.id.layout_overlay_navigation)
            val stopDirectionsText = findViewById<View>(R.id.stopDirectionsText)
            val selectDestinationImageView = findViewById<ImageView>(R.id.selectDestination)
            selectDestinationImageView.visibility = View.INVISIBLE
            stopDirectionsText.visibility = View.VISIBLE
            showOverlayButton.visibility = View.VISIBLE
            showOverlayButton.setOnClickListener {
                val slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up)
                overlayLayout.startAnimation(slideUp)
                overlayLayout.visibility = View.VISIBLE

                showOverlayButton.visibility = View.GONE
                hideOverlayButton.visibility = View.VISIBLE
            }

            hideOverlayButton.setOnClickListener {
                val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
                overlayLayout.startAnimation(slideDown)

                slideDown.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {}
                    override fun onAnimationEnd(animation: Animation?) {
                        overlayLayout.visibility = View.GONE
                        hideOverlayButton.visibility = View.GONE
                        showOverlayButton.visibility = View.VISIBLE
                    }
                    override fun onAnimationRepeat(animation: Animation?) {}
                })
            }

            requestLocationAndProcessDirections(location)
        } else {
            // Handle the case where the current location is not available yet
            // You can show a message to the user or take appropriate action
        }
    }

    private fun stopNavigationAndReset() {
        // Implement your code to stop navigation and reset everything here
        val stopDirectionsText = findViewById<View>(R.id.stopDirectionsText)
        val selectDestinationImageView = findViewById<ImageView>(R.id.selectDestination)
        val showOverlayButton = findViewById<ImageButton>(R.id.showOverlayButton)
        val hideOverlayButton = findViewById<ImageButton>(R.id.hideOverlayButton)
        val overlayLayout = findViewById<View>(R.id.layout_overlay_navigation)

        selectDestinationImageView.visibility = View.VISIBLE
        stopDirectionsText.visibility = View.INVISIBLE

        showOverlayButton.visibility = View.GONE
        hideOverlayButton.visibility = View.GONE
        overlayLayout.visibility = View.GONE

        // Hide the arrowImageView
        runOnUiThread {
            arrowImageView.visibility = View.INVISIBLE
        }

        // Clear any directions or destinationPoints
        destinationPoints.clear()
        currentDestinationIndex = 0
        hasRetrievedDirections = false

//        // Clear any location updates
//        fusedLocationClient.removeLocationUpdates(locationCallback)

        // Clear any other relevant data or variables

        // Optionally, update UI elements to reflect the reset state, e.g., clear TextViews
        val textViewRemainingDistance = findViewById<TextView>(R.id.textRemainingDistance)
        val textViewRemainingTotalDistance = findViewById<TextView>(R.id.textRemainingTotalDistance)
        textViewRemainingDistance.text = ""
        textViewRemainingTotalDistance.text = ""
    }

}