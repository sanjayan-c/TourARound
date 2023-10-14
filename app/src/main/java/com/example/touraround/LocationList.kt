package com.example.touraround

import LocationData
import android.app.AlertDialog
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
//import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.touraround.Adapter.FavLocationAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class LocationList :AppCompatActivity() {

    private lateinit var database : DatabaseReference
    private lateinit var dataList : ArrayList<LocationData>
    private lateinit var adapter : FavLocationAdapter
    var databaseReference:DatabaseReference?=null
    var eventListener:ValueEventListener?=null
    lateinit var LocationName:ArrayList<String>
    lateinit var LocationDesc:ArrayList<String>
    private var isEditingMode = false
    private lateinit var backButton: ImageButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
            setContentView(R.layout.location_list)
        val recyclerView = findViewById<RecyclerView>(R.id.locationrecyclerView)
        //val search: SearchView = findViewById(R.id.search)

            // Retrieve the location from the intent
            val currentLocation = intent.getParcelableExtra<Location>("CurrentLocation")
            if (currentLocation != null) {
                // Log the location data
                Log.e("Translator", "Latitude: ${currentLocation.latitude}, Longitude: ${currentLocation.longitude}")
            }

            val gridLayoutManager = GridLayoutManager(this@LocationList, 1)
            recyclerView.layoutManager = gridLayoutManager
            //search.clearFocus()

        dataList = ArrayList()
        adapter = FavLocationAdapter(this@LocationList, dataList, this)
        recyclerView.adapter = adapter
        //adapter.directionClickListener = this // Set the listener
        databaseReference = FirebaseDatabase.getInstance().getReference("Fav Location")
        val dialogBuilder = AlertDialog.Builder(this@LocationList)
        val dialog = dialogBuilder.create()


        eventListener = databaseReference!!.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dataList.clear()
                for (itemSnapshot in snapshot.children) {
                    val dataClass = itemSnapshot.getValue(LocationData::class.java)
                    if (dataClass != null) {
                        dataList.add(dataClass)
                        Log.d("DataList", "Location: ${dataClass.locationName}, Desc: ${dataClass.locationDesc}")
                    }
                }
                adapter.notifyDataSetChanged()
                dialog.dismiss()
            }
            override fun onCancelled(error: DatabaseError) {
                dialog.dismiss()
            }


        })

        findViewById<View>(R.id.fab).setOnClickListener {
                if (currentLocation != null) {
                    showAddLocationActivity(currentLocation)
                } else {
                    // Handle the case where currentLocation is null
                    Toast.makeText(this,    "Current location is null", Toast.LENGTH_SHORT).show()
                }
            }

        fetchData()

        backButton = findViewById<ImageButton>(R.id.backbtn)
        backButton.setOnClickListener {
            // Handle the click event (e.g., navigate to a previous screen)
            val intent = Intent(this, CameraView::class.java)
            startActivity(intent)
            finish()
        }


    }
    private fun showAddLocationActivity(currentLocation: Location) {
        val latitude = currentLocation.latitude
        val longitude = currentLocation.longitude
        Log.e("Passed Location", "Latitude: $latitude, Longitude: $longitude")
        val intent = Intent(this, AddLocation::class.java)
        intent.putExtra("latitude", latitude)
        intent.putExtra("longitude", longitude)
        startActivity(intent)
    }

    private fun fetchData() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Fav Location")

        val dialogBuilder = AlertDialog.Builder(this@LocationList)
        val dialog = dialogBuilder.create()
        dialog.show()

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                dataList.clear()
                for (itemSnapshot in snapshot.children) {
                    val dataClass = itemSnapshot.getValue(LocationData::class.java)
                    if (dataClass != null) {
                        dataList.add(dataClass)
                        Log.d("DataList", "Location: ${dataClass.locationName}, Desc: ${dataClass.locationDesc}")
                    }
                }
                adapter.notifyDataSetChanged()
                dialog.dismiss()
            }

            override fun onCancelled(error: DatabaseError) {
                dialog.dismiss()
                Toast.makeText(this@LocationList, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
