package com.example.touraround


import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
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
import androidx.viewpager2.widget.ViewPager2
import com.example.touraround.Adapter.PhotoPagerAdapter
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
    private lateinit var objectDetection: ImageButton
    private lateinit var favLocation: ImageButton

    private lateinit var locationCallback: LocationCallback
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_REQUEST_CODE = 100
    private val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 101
    private val TIME_BETWEEN_UPDATES: Long = 1000 // Adjust as needed
    private val MAX_TIME_BETWEEN_UPDATES: Long = 1000
    private val MIN_DISTANCE_CHANGE_FOR_UPDATES: Float = 1f // Adjust as needed
    private var currentLocation: Location? = null
    private var selectedRadius: Int =5000
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var accelerometerReading = FloatArray(3)
    private var magnetometerReading = FloatArray(3)
    private lateinit var arrowImageView: ImageView
    private val nearbyLocations = ArrayList<com.example.touraround.Location>()
    private val locationCardViews = HashMap<com.example.touraround.Location, View>() // Initialize a HashMap
    private val destinationPoints: MutableList<Pair<String, LatLng>> = mutableListOf()
    private var currentDestinationIndex = 0
    private val radiusThreshold = 10.0

    private var calculatedArrowAngle: Float = 0.0f
    private var getAngle: Float = 0.0f
    private val rotationHistory = mutableListOf<Float>()
    private val maxHistorySize = 50 // Adjust this as needed

    private var hasRetrievedDirections = false
    private var isFirstLocationUpdate = false // Add this flag
    private var nearByLocationsAvailable= false;
    private var lastNearbyLocation: Location? = null

    private lateinit var mPermissionResultLauncher: ActivityResultLauncher<Array<String>>

    private var isCameraPermissionGranted = false
    private var isLocationPermissionGranted = false

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        // Check if the introduction overlay has been shown before
        val sharedPreferences: SharedPreferences = getSharedPreferences("prefs",MODE_PRIVATE)
        val firstStart = sharedPreferences.getBoolean("firstStart",true)

        if (firstStart) {
            // Show the introduction overlay
            showIntroductionOverlay()
        }

        previewView = findViewById(R.id.cameraPreview)
        toggleFlash = findViewById(R.id.toggleFlash)
        arrowImageView = findViewById(R.id.arrowImageView)

        val arrowImageView: ImageView = findViewById(R.id.arrowImageView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        toggleFlash.setOnClickListener {
            toggleFlashIcon()
        }
        objectDetection=findViewById<ImageButton>(R.id.objectdetect)
        objectDetection.setOnClickListener {
            // Create an instance of the CameraWithObject fragment
            val cameraFragment = CameraWithObject()

            // Load the CameraWithObject fragment into the camera_container
            supportFragmentManager.beginTransaction()
                .replace(R.id.camera_container, cameraFragment)
                .addToBackStack(null) // Optional: Add to back stack if needed
                .commit()
        }
        favLocation = findViewById<ImageButton>(R.id.favlocation)
        favLocation.setOnClickListener {
            // Create an instance of the LocationList fragment
            val locationListFragment = LocationList()

            // Load the LocationList fragment into the camera_container
            supportFragmentManager.beginTransaction()
                .replace(R.id.camera_container, locationListFragment)
                .addToBackStack(null) // Optional: Add to the back stack if needed
                .commit()
        }



        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Handle location updates here
                val location = locationResult.lastLocation
                currentLocation = location // Update currentLocation
                currentLocation?.let { nonNullLocation ->
                    logLocation(nonNullLocation)
                    // Use 'nonNullLocation' to access latitude and longitude, for example
                    if (isFirstLocationUpdate) {

                        Log.d("XXXXXXXXXXX", "Inside")
                        Log.d("Location Updates started with", "${selectedRadius}")
//                       // Call the method to retrieve nearby locations and populate the nearbyLocations list
                        nearbyLocations.clear() // Clear the existing list
                        val cameraView = CameraView()
                        // Call the method to retrieve nearby locations
                        DirectionsUtils.nearBylocations(
                            this@CameraView,
                            cameraView,
                            selectedRadius,
                            nonNullLocation
                        ) {  retrievedLocations ->
                            // Update the class-level nearbyLocations with the retrieved data
                            nearbyLocations.addAll(retrievedLocations)

                            // This is the callback when nearbyLocations are retrieved
                            // Log or use the contents of nearbyLocations
                            for (locationData in nearbyLocations) {
                                Log.d("NearbyLocationData", "Latitude: ${locationData.latitude}")
                                Log.d("NearbyLocationData", "Longitude: ${locationData.longitude}")
                                Log.d("NearbyLocationData", "Name: ${locationData.name}")
                            }

                            // Check if nearbyLocations is not empty and set nearByLocationsAvailable accordingly
                            if (nearbyLocations.isNotEmpty()) {
                                Log.d("XXXXXXXXXXX", "Passed")
                                updateUIWithNearbyLocations(nearbyLocations)
//                                nearByLocationsAvailable = true
                            } else {
                                Log.d("XXXXXXXXXXX", "Failed")
                            }
                        }
                        // Set the flag to false so this code won't run on subsequent updates
                        isFirstLocationUpdate = false
                    }
                    if (hasRetrievedDirections) {
                        val latLng = LatLng(nonNullLocation.latitude, nonNullLocation.longitude)
                        checkDistanceAndUpdate(latLng)
                    }
                }
            }
        }

        mPermissionResultLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result: Map<String, Boolean> ->

            if (result[Manifest.permission.CAMERA] != null) {
                isCameraPermissionGranted = result[Manifest.permission.CAMERA] == true
            }

            if (result[Manifest.permission.ACCESS_FINE_LOCATION] != null) {
                isLocationPermissionGranted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true
            }
            // Check if both camera and location permissions are granted
            if (isCameraPermissionGranted && isLocationPermissionGranted) {
                // Both permissions are granted, start camera and location updates
                startCamera(cameraFacing)
                startLocationUpdates()
            } else {
                // Handle the case where neither permission is granted
                // You can show a message to the user or disable functionality that requires these permissions
                showPermissionRequestDialog()
            }
        }
        requestPermission();

        val radiusSpinner: Spinner = findViewById(R.id.radiusSpinner)
        val radius= arrayOf(" 5 km "," 10 km "," 15 km "," 20 km ")
        val arrayAdapter=ArrayAdapter(this@CameraView,android.R.layout.simple_spinner_item,radius)
        radiusSpinner.adapter=arrayAdapter
        radiusSpinner?.onItemSelectedListener=object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                selectedRadius = when (p2) {
                    0 -> 5000 // 1st item selected, 5000 meters (5 km)
                    1 -> 10000 // 2nd item selected, 10000 meters (10 km)
                    2 -> 15000 // 3rd item selected, 15000 meters (15 km)
                    3 -> 20000 // 4th item selected, 20000 meters (20 km)
                    else -> 5000
                    }
                val switchView = findViewById<Switch>(R.id.switchView)
                if (switchView.isChecked) {
                    nearbyLocations.clear()
                    removeAllLocationCards()
                    isFirstLocationUpdate = true
                    stopLocationUpdates()
                    startLocationUpdates()
                    Log.d("In Spinner location Updates started with", "${selectedRadius}")
                }
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
                Toast.makeText(this@CameraView,"Nothing selected",Toast.LENGTH_LONG).show()
            }
        }
        val showButton = findViewById<ImageButton>(R.id.showRadiusSelector)
        val hideButton = findViewById<ImageButton>(R.id.hideRadiusSelector)
        val spinnerLayout = findViewById<RelativeLayout>(R.id.spinnerContainer)
        // Load slide-in and slide-out animations from XML
        val slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in)
        val slideOut =  AnimationUtils.loadAnimation(this, R.anim.slide_out)
        showButton.setOnClickListener {
            showButton.visibility = View.GONE
            spinnerLayout.visibility = View.VISIBLE
            spinnerLayout.startAnimation(slideIn)
            hideButton.visibility = View.VISIBLE
        }

        hideButton.setOnClickListener {
            spinnerLayout.startAnimation(slideOut)
            spinnerLayout.visibility = View.GONE
            hideButton.visibility = View.GONE
            showButton.visibility = View.VISIBLE
        }
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val switchView = findViewById<Switch>(R.id.switchView)
        val switchTextView = findViewById<TextView>(R.id.switchTextView)
        val messegeTextView = findViewById<TextView>(R.id.messege)
        val emptyTextView = findViewById<TextView>(R.id.emptyTextView)
        var isSwitchEnabled = true // Flag to track switch enable/disable state

        switchView.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isSwitchEnabled) {
                isSwitchEnabled = false // Disable the switch temporarily

                if (isChecked) {
                    if (isInternetAvailable()) { // Check for internet connectivity
                        // Handle switch ON state
                        switchView.trackTintList =
                            ColorStateList.valueOf(ContextCompat.getColor(this, R.color.bar))
                        switchTextView.text = "Turn off when locations are not needed"
                        isFirstLocationUpdate = true
                        messegeTextView.text = "Route may vary in distance"
                        messegeTextView.visibility = View.VISIBLE
                        Handler().postDelayed({
                            messegeTextView.visibility = View.GONE
                        }, 10000) // 10000 milliseconds = 10 seconds
                        stopLocationUpdates()
                        startLocationUpdates()
                        Log.d("Location Updates for nearby locations", "Started")
                    } else {
                        // Internet is not available, prevent the switch from changing state
                        switchView.isChecked = !isChecked
                        messegeTextView.text = "Check your internet connection"
                        messegeTextView.visibility = View.VISIBLE
                        Handler().postDelayed({
                            messegeTextView.visibility = View.GONE
                        }, 5000) // 5000 milliseconds = 5 seconds
                    }
                } else {
                    // Handle switch OFF state
                    switchView.trackTintList =
                        ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
                    switchTextView.text = "Turn on to view nearby locations"
                    messegeTextView.visibility = View.GONE
                    emptyTextView.visibility = View.GONE
                    nearbyLocations.clear()
                    removeAllLocationCards()
                }

                // Enable the switch after a delay (e.g., 2 seconds)
                Handler().postDelayed({
                    isSwitchEnabled = true
                }, 2000) // 2000 milliseconds = 2 seconds
            } else {
                // If the switch is not enabled, revert its state back
                switchView.isChecked = !isChecked
            }
        }

        val stopDirectionsText = findViewById<View>(R.id.stopDirectionsText)

        stopDirectionsText.setOnClickListener {
            // Stop navigation and reset everything here
            stopNavigationAndReset()
        }

    }
    private fun showIntroductionOverlay() {
        val introductionOverlay = findViewById<View>(R.id.introductionOverlay)
        introductionOverlay.visibility = View.VISIBLE

        val startTourButton = introductionOverlay.findViewById<Button>(R.id.startTourButton)

        startTourButton.setOnClickListener {
            introductionOverlay.visibility = View.GONE

            // Update the preference to indicate that the introduction overlay has been shown
            val prefs = getSharedPreferences("prefs", MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putBoolean("firstStart", false)
            editor.apply()
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
        val switchView = findViewById<Switch>(R.id.switchView)
        val switchTextView = findViewById<TextView>(R.id.switchTextView)
        val messegeTextView = findViewById<TextView>(R.id.messege)
        switchView.isChecked = false
        switchView.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
        switchTextView.text = "Turn on to view nearby locations"
        messegeTextView.visibility = View.GONE
        nearbyLocations.clear()
        removeAllLocationCards()
    }
    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
        } else if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, 3)
        }

        val switchView = findViewById<Switch>(R.id.switchView)
        if (switchView.isChecked) {
            val emptyTextView = findViewById<TextView>(R.id.emptyTextView)
            if (nearbyLocations.isEmpty()) {
                emptyTextView.visibility = View.VISIBLE
            } else {
                emptyTextView.visibility = View.GONE
            }
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

            // Calculate the angle based on the device's orientation
            // Calculate the absolute difference between sensorAngle and getAngle
            val angleDifference = Math.abs(sensorAngle - getAngle)

            // Check if the angle difference is greater than 5 degrees
            if (angleDifference > 5f) {
                getAngle = sensorAngle
            }

            // Transform angle to be in the range [0, 360)
//            arrowImageView.rotation = getAngle

//            if(nearByLocationsAvailable) {
            if (nearbyLocations.isNotEmpty()) {
                // Update the UI with nearby locations
                updateUIWithNearbyLocations(nearbyLocations)
            }

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
    private fun requestPermission(){
        isLocationPermissionGranted=ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        isCameraPermissionGranted=ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        val permissionRequest = mutableListOf<String>()

        if (!isLocationPermissionGranted) {
            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (!isCameraPermissionGranted) {
            permissionRequest.add(Manifest.permission.CAMERA)
        }
        if (permissionRequest.isNotEmpty()) {
            mPermissionResultLauncher.launch(permissionRequest.toTypedArray())
        } else {
            // Both permissions are already granted, start camera and location updates
            startCamera(cameraFacing)
            startLocationUpdates()
        }

    }
    private fun showPermissionRequestDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("This app requires camera and location permissions to function properly. Restart the app for the changes to take effect.")
        builder.setPositiveButton("Grant Permissions") { dialog, _ ->
            // Request the missing permission
            requestPermission()
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            // Check if permissions are granted
            if (!isLocationPermissionGranted || !isCameraPermissionGranted) {
                // Open the app settings
                openAppSettings()
            } else {
                // Permissions are granted, close the dialog
                dialog.dismiss()
            }
        }
    }
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }


//    private fun requestLocationPermission() {
//        ActivityCompat.requestPermissions(
//            this,
//            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
//            LOCATION_PERMISSION_REQUEST_CODE
//        )
//    }


    private fun startLocationUpdates() {
        if (hasLocationPermission()) {
            val locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(TIME_BETWEEN_UPDATES)
                .setFastestInterval(TIME_BETWEEN_UPDATES)
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


    private fun stopLocationUpdates() {
        // Check if the FusedLocationProviderClient and locationCallback are initialized
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            Log.d("LocationUpdates", "Location updates stopped.")
        }
    }
    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    private fun logLocation(location: android.location.Location) {
        Log.d("LocationUpdates", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")
    }

    private fun requestLocationAndProcessDirections(location: Location,destination:LatLng) {
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
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 123
    }
    private fun calculateAngle(orientationValues: FloatArray): Float {
        // Calculate the angle based on the device's orientation
        // You can adjust the formula as needed to control the arrow's behavior
        val azimuth = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
        return -azimuth // Invert the angle for correct rotation
    }
    fun angleFromCoordinate(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val dLon = (long2 - long1)
        val y = Math.sin(Math.toRadians(dLon)) * Math.cos(Math.toRadians(lat2))
        val x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) -
                Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(dLon))
        var brng = Math.toDegrees(Math.atan2(y, x))
        brng = (brng + 360) % 360
        return brng
    }
    fun calculateDistance(start: LatLng, end: LatLng): Double {
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

    private fun updateUIWithNearbyLocations(nearbyLocations: List<com.example.touraround.Location>) {
        val locationContainer = findViewById<LinearLayout>(R.id.locationContainer)

        // Log the details of nearbyLocations
//        for (locationInfo in nearbyLocations) {
//            Log.d("NearbyLocationInfo", "Final Location : $locationInfo")
//        }

        val locationsToRemove = HashSet(locationCardViews.keys) // Create a copy of locations in the HashMap

        for (locationInfo in nearbyLocations) {
            // Get the latitude and longitude from locationInfo
            val latitude = locationInfo.latitude
            val longitude = locationInfo.longitude
            val destination = LatLng(latitude, longitude)

            // Calculate the angle between the current location and the target location
            val angleToLocation = locationInfo.angle
            val angleToLocatioIn360 = 360-angleToLocation
            // Log the transformed angle
//            Log.d("Angle", "Angle: $getAngle")
           // Log the transformed angle
//            Log.d("Angle", "Location angle: $angleToLocatioIn360  ${locationInfo.name}")
            // Calculate the difference between the current orientation angle and the angle to the location
            val angleDifference = getAngle - angleToLocatioIn360

            // Ensure the angle difference is in the range [0, 360)
            val normalizedAngleDifference = (angleDifference + 360) % 360
//            Log.d("Angle", "Angle from current position: $normalizedAngleDifference")
            // Check if the location is within the desired angle range (e.g., ±45 degrees)
            val angleRange = 45f
            if (normalizedAngleDifference <= angleRange || normalizedAngleDifference >= 360 - angleRange) {

                if (locationCardViews.containsKey(locationInfo)) {
                    // Location is already associated with a card view, so update the content
                    val cardView = locationCardViews[locationInfo]!!
                    val tvTitle = cardView.findViewById<TextView>(R.id.tv_title)
                    val tvDistance = cardView.findViewById<TextView>(R.id.tv_distance)

                    // Set data for the views
                    tvTitle.text = locationInfo.name
                    val formattedDistance = String.format("%.2f km", locationInfo.distance / 1000.0)
                    tvDistance.text = formattedDistance

                    // Remove the location from locationsToRemove since it's still in range
                    locationsToRemove.remove(locationInfo)
                } else {
                    // Location is not associated with a card view, so create a new one
                    val cardView = layoutInflater.inflate(R.layout.location_cardview_middle, null)
                    val tvTitle = cardView.findViewById<TextView>(R.id.tv_title)
                    val tvDistance = cardView.findViewById<TextView>(R.id.tv_distance)

                    // Set data for the views
                    tvTitle.text = locationInfo.name
                    val formattedDistance = String.format("%.2f km", locationInfo.distance / 1000.0)
                    tvDistance.text = formattedDistance

                    // Add an OnClickListener to the card view
                    cardView.setOnClickListener {
                        Toast.makeText(this, "Card clicked for ${locationInfo.name}", Toast.LENGTH_SHORT).show()
                        // Remove the card view from the UI
                        locationContainer.removeView(cardView)
                        showPopupDialog(locationInfo,destination)
//                        showNavigationDirections(destination)
                    }

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

    private fun calculateTotalDistanceLeftFromCurrentLocation(distanceToDestination: Double, startIndex: Int): Double {
        var distanceFromIndex = 0.0

        // Calculate the total distance left from the current location to the last end location
        for (i in startIndex until destinationPoints.size - 1) {
            distanceFromIndex += calculateDistance(destinationPoints[i].second, destinationPoints[i + 1].second)
        }
        return distanceToDestination + distanceFromIndex
    }

    private fun showNavigationDirections(destination:LatLng) {
        val location = currentLocation // Assign currentLocation to a local variable
        if (location != null) {
            val switchBar = findViewById<LinearLayout>(R.id.switchBar)
            switchBar.visibility = View.INVISIBLE
//            nearByLocationsAvailable=false
            nearbyLocations.clear()
            removeAllLocationCards()
            val showOverlayButton = findViewById<ImageButton>(R.id.showOverlayButton)
            val hideOverlayButton = findViewById<ImageButton>(R.id.hideOverlayButton)
            val overlayLayout = findViewById<View>(R.id.layout_overlay_navigation)
            val stopDirectionsText = findViewById<View>(R.id.stopDirectionsText)
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

            requestLocationAndProcessDirections(location,destination)
        } else {
            // Handle the case where the current location is not available yet
            // You can show a message to the user or take appropriate action
        }
    }

    private fun stopNavigationAndReset() {
        val switchBar = findViewById<LinearLayout>(R.id.switchBar)
        switchBar.visibility = View.VISIBLE
        // Implement your code to stop navigation and reset everything here
        val stopDirectionsText = findViewById<View>(R.id.stopDirectionsText)
        val showOverlayButton = findViewById<ImageButton>(R.id.showOverlayButton)
        val hideOverlayButton = findViewById<ImageButton>(R.id.hideOverlayButton)
        val overlayLayout = findViewById<View>(R.id.layout_overlay_navigation)
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
    private fun removeAllLocationCards() {
        val locationContainer = findViewById<LinearLayout>(R.id.locationContainer)

        // Remove all child views (cards) from the locationContainer
        locationContainer.removeAllViews()

        // Clear the locationCardViews HashMap
        locationCardViews.clear()
    }
    // Define a function to show the popup dialog
    fun showPopupDialog(locationInfo: com.example.touraround.Location,destination:LatLng) {
        // Create a custom dialog
        val dialog = Dialog(this)

        // Set the custom layout for the dialog
        dialog.setContentView(R.layout.location_popup)

        // Set the width of the dialog to match the parent's width
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(dialog.window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        dialog.window?.attributes = layoutParams

        // Find views in the custom layout
        val tvTitle = dialog.findViewById<TextView>(R.id.tv_title)
        val tvDistance = dialog.findViewById<TextView>(R.id.tv_distance)
        val ratingBar  = dialog.findViewById<RatingBar>(R.id.ratingBar)
        val ratingText  = dialog.findViewById<TextView>(R.id.ratingText)

        // Find ViewPager2 in the custom layout
        val viewPager = dialog.findViewById<ViewPager2>(R.id.viewPager)

        // Assuming you have a list of photo references in locationInfo.photoReferences
        val photoReferences = locationInfo.photoReferences

        // Create an adapter to populate ViewPager2 with images
        val adapter = PhotoPagerAdapter(photoReferences)
        // Iterate through the photo references and log each one
        for (photoReference in photoReferences) {
            Log.d("Photo Reference", photoReference)
        }
        // Set the adapter to the ViewPager2
        viewPager.adapter = adapter

        // Check if photoReferences is empty and set the placeholder image if needed
        if (photoReferences.isEmpty()) {
            // Assuming viewPager is your ViewPager2 instance, you should set a placeholder drawable directly
            viewPager.background = ContextCompat.getDrawable(this, R.drawable.error_placeholder)
        }

        // Populate the views with data from the clicked card
        tvTitle.text = locationInfo.name
        val formattedDistance = String.format("%.2f km", locationInfo.distance / 1000.0)
        tvDistance.text = formattedDistance
        val rating =  locationInfo.rating// Replace this with your actual Double value
        ratingBar.rating = rating.toFloat()
        ratingText.text="Users : ("+locationInfo.userRatingsTotal.toString()+")"
        dialog.show()
        // Find the button view and set an OnClickListener
        val closePopUp = dialog.findViewById<ImageView>(R.id.closePopUp)
        closePopUp.setOnClickListener {
            dialog.dismiss()
        }
        val switchView = findViewById<Switch>(R.id.switchView)
        val switchTextView = findViewById<TextView>(R.id.switchTextView)
        val messegeTextView = findViewById<TextView>(R.id.messege)
        // Find the button view and set an OnClickListener
        val buttonGetNavigations = dialog.findViewById<Button>(R.id.buttonGetNavigations)
        buttonGetNavigations.setOnClickListener {
            if (isInternetAvailable()) { // Check for internet connectivity
                showNavigationDirections(destination)
                switchView.isChecked = false
                switchView.trackTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
                switchTextView.text = "Turn on to view nearby locations"
                messegeTextView.visibility = View.GONE
                dialog.dismiss()
            } else {
                val messegeTextView = findViewById<TextView>(R.id.messege)
                messegeTextView.text = "Check your internet conneection"
                messegeTextView.visibility = View.VISIBLE
                Handler().postDelayed({
                    messegeTextView.visibility = View.GONE
                }, 5000) // 10000 milliseconds = 10 seconds
            }

        }
    }

}
