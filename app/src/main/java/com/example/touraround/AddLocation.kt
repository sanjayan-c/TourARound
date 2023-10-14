    package com.example.touraround

    import LocationData
    import android.content.Intent
    import android.os.Bundle
    import android.location.Location
    import android.util.Log
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.Button
    import android.widget.EditText
    import android.widget.TextView
    import android.widget.Toast
    import androidx.appcompat.app.AppCompatActivity
    import androidx.core.content.ContentProviderCompat.requireContext
    import androidx.fragment.app.DialogFragment
    import androidx.fragment.app.Fragment
    import com.google.android.gms.maps.model.LatLng
    import com.google.firebase.database.DatabaseReference
    import com.google.firebase.database.ValueEventListener
    import com.google.firebase.database.DatabaseError
    import com.google.firebase.database.DataSnapshot
    import com.google.firebase.database.FirebaseDatabase

//    class AddLocation: DialogFragment() {
//
//
//        // Declare Firebase database reference
//        private lateinit var database: DatabaseReference
//        //private var currentLocation: Location? = null
//        private var latitude: Double = 0.0
//        private var longitude: Double = 0.0
//
//        companion object {
//            fun newInstance(latitude: Double, longitude: Double): AddLocation {
//                val fragment = AddLocation()
//                val args = Bundle()
//                args.putDouble("latitude", latitude)
//                args.putDouble("longitude", longitude)
//                fragment.arguments = args
//                return fragment
//            }
//        }
//
//        override fun onCreateView(
//            inflater: LayoutInflater, container: ViewGroup?,
//            savedInstanceState: Bundle?
//        ): View? {
//            val view = inflater.inflate(R.layout.add_location, container, false)
//
//            //currentLocation = arguments?.getParcelable("CurrentLocation")
//            // Retrieve latitude and longitude from the arguments
//            // Retrieve latitude and longitude from the arguments
//            latitude = arguments?.getDouble("latitude", 0.0) ?: 0.0
//            longitude = arguments?.getDouble("longitude", 0.0) ?: 0.0
//            Log.e("Received Location", "Latitude: $latitude, Longitude: $longitude")
//
//
//            // Initialize Firebase database reference
//            database = FirebaseDatabase.getInstance().getReference("Fav Location")
//            val connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected")
//            connectedRef.addValueEventListener(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    val connected = snapshot.getValue(Boolean::class.java)
//                    if (connected == true) {
//                        Log.d("Firebase", "Connected to Firebase Realtime Database")
//                    } else {
//                        Log.d("Firebase", "Not connected to Firebase Realtime Database")
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    Log.e("Firebase", "Connection check failed: $error")
//                }
//            })
//
//            val saveButton = view.findViewById<Button>(R.id.saveButton)
//            val locationNameEditText = view.findViewById<EditText>(R.id.locationTitle)
//            val locationDescEditText = view.findViewById<EditText>(R.id.locationDesc)
//            val locationTextView = view.findViewById<TextView>(R.id.currentLoc)
//
//            // Display the received latitude and longitude
//            locationTextView.text = "Latitude: $latitude, Longitude: $longitude"
//
//            saveButton.setOnClickListener {
//                val locationName = locationNameEditText.text.toString()
//                val locationDesc = locationDescEditText.text.toString()
//
//                // Push the data to Firebase and get the generated key
//                val locationRef = database.push()
//                val uniqueKey = locationRef.key // Get the Firebase-generated key
//
//                // Set the unique key in the LocationData object
//                val locationData = LocationData(uniqueKey!!, locationName, locationDesc, latitude, longitude)
//
//                locationRef.setValue(locationData)
//                    .addOnSuccessListener {
//                        locationNameEditText.text.clear()
//                        locationDescEditText.text.clear()
//
//                        Toast.makeText(requireContext(), "Successfully Saved", Toast.LENGTH_SHORT)
//                            .show()
//                        Log.d("Firebase", "Data saved successfully")
//                    }
//                    .addOnFailureListener { e ->
//                        Toast.makeText(requireContext(), "Failed to save: $e", Toast.LENGTH_SHORT)
//                            .show()
//                        Log.e("Firebase", "Failed to save: $e")
//                    }
//
//                // Close the dialog
//                dismiss()
//            }
//
//            return view
//        }
//    }

    class AddLocation : AppCompatActivity() {

        // Declare Firebase database reference
        private lateinit var database: DatabaseReference
        private var latitude: Double = 0.0
        private var longitude: Double = 0.0

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.add_location)

            // Retrieve latitude and longitude from the intent extras
            latitude = intent.getDoubleExtra("latitude", 0.0)
            longitude = intent.getDoubleExtra("longitude", 0.0)
            // Initialize Firebase database reference
            database = FirebaseDatabase.getInstance().getReference("Fav Location")

            val saveButton = findViewById<Button>(R.id.saveButton)
            val locationNameEditText = findViewById<EditText>(R.id.locationTitle)
            val locationDescEditText = findViewById<EditText>(R.id.locationDesc)
            val locationTextView = findViewById<TextView>(R.id.currentLoc)

            // Display the received latitude and longitude
            locationTextView.text = "Latitude: $latitude, Longitude: $longitude"

            saveButton.setOnClickListener {
                val locationName = locationNameEditText.text.toString()
                val locationDesc = locationDescEditText.text.toString()

                // Push the data to Firebase and get the generated key
                val locationRef = database.push()
                val uniqueKey = locationRef.key // Get the Firebase-generated key

                // Set the unique key in the LocationData object
                val locationData = LocationData(uniqueKey!!, locationName, locationDesc, latitude, longitude)

                locationRef.setValue(locationData)
                    .addOnSuccessListener {
                        locationNameEditText.text.clear()
                        locationDescEditText.text.clear()

                        Toast.makeText(this, "Successfully Saved", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save: $e", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
