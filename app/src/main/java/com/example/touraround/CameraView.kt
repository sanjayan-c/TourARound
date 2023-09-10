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
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
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
import kotlin.math.pow


class CameraView : AppCompatActivity(), SensorEventListener {
    private lateinit var toggleFlash: ImageButton
    private lateinit var previewView: PreviewView
    private var cameraFacing = CameraSelector.LENS_FACING_BACK
    private var camera: Camera? = null
    private var isFlashOn = false // Track the flashlight state
    val locationPermission = Manifest.permission.ACCESS_FINE_LOCATION


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
    private lateinit var nearbyLocations: List<Locations>

    private var calculatedArrowAngle: Float = 0.0f
    private var getAngle: Float = 0.0f

    private var hasRetrievedDirections = false
    var isFirstLocationUpdate = true // Add this flag
    private var filteredLocations: List<LocationInfo> = mutableListOf()

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
        arrowImageView = findViewById(R.id.arrowImageView)

        val arrowImageView: ImageView = findViewById(R.id.arrowImageView)
        // Show arrow initially
//        arrowImageView.visibility = View.VISIBLE

        // Log the contents of nearbyLocations
        nearbyLocations = LocationData.placesList

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
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

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Handle location updates here
                val location = locationResult.lastLocation
                currentLocation = location // Update currentLocation
                currentLocation?.let { nonNullLocation ->
                    logLocation(nonNullLocation)
                    // Use 'nonNullLocation' to access latitude and longitude, for example
                }
                if(isFirstLocationUpdate) {
                    getNearbyLocations(location)
                    // Set the flag to false so this code won't run on subsequent updates
                    isFirstLocationUpdate = false
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


        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        if (nearbyLocations.isNotEmpty()) {
            // Log the contents of nearbyLocations
            for (nearbylocation in nearbyLocations) {
                Log.d("NearbyLocations", "Location in list: $nearbylocation")
            }
        } else {
            Log.d("NearbyLocations", "Location list is empty.")
        }

        // Access placesList directly


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
            getAngle = calculateAngle(orientationValues)
            // Transform angle to be in the range [0, 360)
//            arrowImageView.rotation = getAngle

            // Update the UI with nearby locations
            updateUIWithNearbyLocations(filteredLocations)
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

    private fun logLocation(location: android.location.Location) {
        Log.d("LocationUpdates", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
    }

    private fun getNearbyLocations(location: Location) {

        val currentLatLng = LatLng(location.latitude, location.longitude)
        // Process the current location here
        val maxDistance = 1000.0 // Adjust the maximum distance as needed (in meters)
        // Assuming you have a list of nearby locations stored in `nearbyLocations`
        filteredLocations = filterNearbyLocations(currentLatLng, nearbyLocations, maxDistance)

        // Pass the filtered locations to the UI update function
        updateUIWithNearbyLocations(filteredLocations)
    }

    private fun processDirectionsResult(directionsResult: DirectionsResult?) {
        if (directionsResult != null) {
            // Process the directions result, e.g., extract route information, duration, etc.
            val route = directionsResult.routes[0]
            val legs: List<DirectionsLeg> = route.legs.toList()

            for (leg in legs) {
                val steps: List<DirectionsStep> = leg.steps.toList()
                val distance = leg.distance.humanReadable
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
                }
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
    }
    private fun calculateAngle(orientationValues: FloatArray): Float {
        // Calculate the angle based on the device's orientation
        // You can adjust the formula as needed to control the arrow's behavior
        val azimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
        return -azimuth // Invert the angle for correct rotation
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
    private fun filterNearbyLocations(
        currentLocation: LatLng,
        nearbyLocations: List<Locations>,
        maxDistance: Double,
    ): List<LocationInfo> {
        val filteredLocations = mutableListOf<LocationInfo>()

        for (location in nearbyLocations) {
            val distance = calculateDistance(
                currentLocation,
                LatLng(location.latitude, location.longitude)
            )
            Log.d("Distance is", "Distance is: $distance")
            // Check if the location is within the specified radius and angle range
            if (distance <= maxDistance) {
                val angle = angleFromCoordinate(
                    currentLocation.latitude,
                    currentLocation.longitude,
                    location.latitude,
                    location.longitude)
                filteredLocations.add(LocationInfo(location, angle, distance))
            }
        }
        for (filteredLocation in filteredLocations) {
            Log.d("FilteredLocations", "Filtered Location: $filteredLocation")
        }
        return filteredLocations
    }

    private val locationCardViews = HashMap<LocationInfo, View>() // Initialize a HashMap

    private fun updateUIWithNearbyLocations(nearbyLocations: List<LocationInfo>) {
        val locationContainer = findViewById<LinearLayout>(R.id.locationContainer)

        // Log the details of nearbyLocations
        for (locationInfo in nearbyLocations) {
            Log.d("NearbyLocationInfo", "Final Location : $locationInfo")
        }

        val locationsToRemove = HashSet(locationCardViews.keys) // Create a copy of locations in the HashMap

        for (locationInfo in nearbyLocations) {
            // Calculate the angle between the current location and the target location
            val angleToLocation = locationInfo.angle
            val angleToLocatioIn360 = 360-angleToLocation
            // Log the transformed angle
            Log.d("Angle", "Angle: $getAngle")
            // Log the transformed angle
            Log.d("Angle", "angleToLocation: $angleToLocatioIn360")
            // Calculate the difference between the current orientation angle and the angle to the location
            val angleDifference = getAngle - angleToLocatioIn360

            // Ensure the angle difference is in the range [0, 360)
            val normalizedAngleDifference = (angleDifference + 360) % 360

            // Check if the location is within the desired angle range (e.g., ±45 degrees)
            val angleRange = 45f
            if (normalizedAngleDifference <= angleRange || normalizedAngleDifference >= 360 - angleRange) {
                // The location is within the angle range
                if (locationCardViews.containsKey(locationInfo)) {
                    // Location is already associated with a card view, so update the content
                    val cardView = locationCardViews[locationInfo]!!
                    val tvTitle = cardView.findViewById<TextView>(R.id.tv_title)
                    val tvDistance = cardView.findViewById<TextView>(R.id.tv_distance)

                    // Set data for the views
                    tvTitle.text = locationInfo.location.name
                    val formattedDistance = String.format("%.2f km", locationInfo.distance / 1000.0)
                    tvDistance.text = formattedDistance

                    // Remove the location from locationsToRemove since it's still in range
                    locationsToRemove.remove(locationInfo)
                } else {
                    // Location is not associated with a card view, so create a new one
                    val cardView = layoutInflater.inflate(R.layout.location_cardview, null)
                    val tvTitle = cardView.findViewById<TextView>(R.id.tv_title)
                    val tvDistance = cardView.findViewById<TextView>(R.id.tv_distance)

                    // Set data for the views
                    tvTitle.text = locationInfo.location.name
                    val formattedDistance = String.format("%.2f km", locationInfo.distance / 1000.0)
                    tvDistance.text = formattedDistance

                    // Add the card view to the UI
                    locationContainer.addView(cardView)

                    // Associate the location with the card view in the HashMap
                    locationCardViews[locationInfo] = cardView
                }
            }
        }

        // Remove locations that are no longer in range
        for (locationInfoToRemove in locationsToRemove) {
            val cardViewToRemove = locationCardViews[locationInfoToRemove]
            if (cardViewToRemove != null) {
                // Remove the card view from the UI
                locationContainer.removeView(cardViewToRemove)

                // Remove the location from the HashMap
                locationCardViews.remove(locationInfoToRemove)
            }
        }
    }



}