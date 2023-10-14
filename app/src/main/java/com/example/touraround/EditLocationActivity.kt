package com.example.touraround

import LocationData
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class EditLocationActivity : AppCompatActivity() {
    private lateinit var id: String
    private lateinit var locationData: LocationData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.update_location)

        // Retrieve data from the intent
        val locationName = intent.getStringExtra("locationName") ?: ""
        id = intent.getStringExtra("id") ?: ""
        val locationDesc = intent.getStringExtra("locationDesc") ?: ""
        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)

        // Ensure you have initialized locationData here before using it
        locationData = LocationData(id,locationName, locationDesc, latitude, longitude)

        // Check if the unique key is valid
        if (id.isNotEmpty()) {
            // Create a reference to the Firebase database using the unique key
            val databaseReference = FirebaseDatabase.getInstance().getReference("Fav Location").child(id)
            Log.d("ReceivedLocationActivity", "Received id: $id")

            // Add a ValueEventListener to retrieve the data
            databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Retrieve the location data
                        val data = snapshot.getValue(LocationData::class.java)

                        // Update locationData with the retrieved data
                        if (data != null) {
                            locationData = data
                            Log.d("Done&ReceivedLocationActivity", "Retrieved location data: $locationData")

                            // Once you have locationData, update the UI
                            updateLocation()
                        }
                    } else {
                        // Handle the case where the data doesn't exist
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle any errors
                }
            })
        } else {
            Log.e("YourLogTag", "Unique Key empty")
        }

        val updateButton = findViewById<Button>(R.id.update)
        val locationNameEditText = findViewById<EditText>(R.id.editLocationTitle)
        val locationDescEditText = findViewById<EditText>(R.id.editLocationDesc)

        // Retrieve the current LocationName and LocationDesc values from the database
        locationNameEditText.setText(locationData.locationName)
        locationDescEditText.setText(locationData.locationDesc)

        updateButton.setOnClickListener {
            // Retrieve the updated LocationName and LocationDesc from the UI elements
            val updatedName = locationNameEditText.text.toString()
            val updatedDescription = locationDescEditText.text.toString()

            // Create a reference to the Firebase database
            val databaseReference: DatabaseReference = FirebaseDatabase.getInstance().getReference("Fav Location").child(id)

            // Update only LocationName and LocationDesc fields in the database
            val updatedData = mapOf(
                "locationName" to updatedName,
                "locationDesc" to updatedDescription
            )

            databaseReference.updateChildren(updatedData)
                .addOnSuccessListener {
                    // Data updated successfully
                    // You can show a message or perform any other action
                    Log.d("UpdateLocation", "Data updated successfully")
                    Toast.makeText(this,"Updated Successfully",Toast.LENGTH_SHORT)
                    val intent = Intent(this, LocationList::class.java)
                    startActivity(intent)
                }
                .addOnFailureListener { e ->
                    // Handle any errors during the update
                    Log.e("UpdateLocation", "Error updating data: $e")
                }
        }

        val cancelButton = findViewById<Button>(R.id.cancelbtn)

        cancelButton.setOnClickListener {
            // Create an Intent to navigate back to the LocationList activity
            val intent = Intent(this, LocationList::class.java)
            startActivity(intent)
        }


    }

    private fun updateLocation() {
        // Now you have the locationData, and you can perform the update logic here
        val LocationEditText = findViewById<EditText>(R.id.editLocationTitle)
        val descriptionEditText = findViewById<EditText>(R.id.editLocationDesc)
        val latLongEditText = findViewById<TextView>(R.id.currentLoc)

        Log.d("LocationData", "Name: ${locationData.locationName}")
        Log.d("LocationData", "Description: ${locationData.locationDesc}")
        Log.d("LocationData", "Latitude: ${locationData.latitude}")
        Log.d("LocationData", "Longitude: ${locationData.longitude}")

        // Update UI with the location data
        LocationEditText.setText(locationData.locationName)
        descriptionEditText.setText(locationData.locationDesc)

        // Concatenate and display latitude and longitude
        val latLong = "Latitude: ${locationData.latitude}, Longitude: ${locationData.longitude}"
        latLongEditText.text = latLong  // Use .text to set the text


    }
}
