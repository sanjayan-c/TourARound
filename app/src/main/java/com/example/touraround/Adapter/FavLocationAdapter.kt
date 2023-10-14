package com.example.touraround.Adapter

import LocationData
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.model.AdapterClass
import androidx.recyclerview.widget.RecyclerView
import com.example.touraround.EditLocationActivity
import com.example.touraround.LocationList
import com.example.touraround.R
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class FavLocationAdapter(private val context: Context,
                         private val datalist: ArrayList<LocationData>,
                         private val locationList: LocationList) : RecyclerView.Adapter<ViewHolderClass>() {
    interface DirectionClickListener {
        fun onDirectionClick(destinationId: LatLng)
    }

    var directionClickListener: DirectionClickListener? = null


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolderClass {
        val itemView=LayoutInflater.from(parent.context).inflate(R.layout.location_recycler,parent,false)
        return ViewHolderClass(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolderClass, position: Int) {
        val currentItem = datalist[position]

        holder.rvLocName.text=currentItem.locationName
        holder.rvLocDesc.text=currentItem.locationDesc

        Log.d("FavLocationAdapter", "Item at position $position - Name: ${currentItem.locationName}, Desc: ${currentItem.locationDesc}")
        holder.editLocBtn.setOnClickListener {
            val intent = Intent(context, EditLocationActivity::class.java)

            // Pass data as extras to the intents

            intent.putExtra("locationName", currentItem.locationName)
            intent.putExtra("id", currentItem.id)
            intent.putExtra("locationDesc", currentItem.locationDesc)
            intent.putExtra("latitude", currentItem.latitude) // Pass latitude
            intent.putExtra("longitude", currentItem.longitude) // Pass longitude

            Log.d("FavLocationPassed", "Card clicked - Name: ${currentItem.locationName}, Desc: ${currentItem.locationDesc}, Latitude: ${currentItem.latitude}, Longitude: ${currentItem.longitude}, Id:${currentItem.id}")

            context.startActivity(intent)
        }

        holder.deleteLocButton.setOnClickListener {
            // Retrieve the ID of the data row you want to delete from 'currentItem'
            val idToDelete = currentItem.id

            val databaseReference = FirebaseDatabase.getInstance().getReference("Fav Location")

            // Create a reference to the data node with the specified ID
            val dataNodeReference = databaseReference.child(idToDelete)

            // Remove the data node from the database
            dataNodeReference.removeValue()
                .addOnSuccessListener {
                    // Data was successfully deleted
                    // You can show a message or perform any other action here

                    // Additionally, you should remove the item from the adapter's data list
                    datalist.remove(currentItem)
                    notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    // Handle any errors during the deletion
                    Log.e("DeleteData", "Error deleting data: $e")
                }

//            holder.directionLocButton.setOnClickListener {
//                val locationId = currentItem.id // Get the ID of the selected item
//                getLocationDetailxsById(databaseReference, locationId) { latitude, longitude ->
//                    val destination = LatLng(latitude, longitude)
//                    directionClickListener?.onDirectionClick(destination)
//                }
//            }



        }

    }



    override fun getItemCount(): Int {
        return datalist.size
    }
    fun updateItem(updatedLocationData: LocationData) {
        // Find the position of the item to update in the datalist
        val position = datalist.indexOfFirst { it.id == updatedLocationData.id }

        if (position != -1) {
            // Update the item in the datalist
            datalist[position] = updatedLocationData

            // Notify the adapter that the item at the given position has changed
            notifyItemChanged(position)
            Log.d("FavLocationAdapter", "Item updated at position $position - ID: ${updatedLocationData.id}")

        }
    }

}
class ViewHolderClass(itemView: View) : RecyclerView.ViewHolder(itemView) {

    val rvLocName: TextView
    val rvLocDesc: TextView
    val editLocBtn: AppCompatImageButton = itemView.findViewById(R.id.editLoc)
    val deleteLocButton: ImageButton = itemView.findViewById(R.id.deleteLoc)
    val directionLocButton: ImageButton = itemView.findViewById(R.id.directionLoc)


    init {
        rvLocName = itemView.findViewById(R.id.locTitle)
        rvLocDesc = itemView.findViewById(R.id.locDesc)
//        cardView.setOnClickListener {
//            val intent = Intent(itemView.context, EditLocationActivity::class.java)
//
//            // Pass data as extras to the intent
//            intent.putExtra("locationName", rvLocName.text.toString())
//            intent.putExtra("locationDesc", rvLocDesc.text.toString())
//            Log.d("ViewHolderClass", "Card clicked - Name: ${rvLocName.text}, Desc: ${rvLocDesc.text}")
//
//
//            // Start the destination Activity
//            itemView.context.startActivity(intent)
//        }

}
//    private fun getLocationDetailsById(
//        databaseReference: DatabaseReference,
//        locationId: String,
//        callback: (Double, Double) -> Unit
//    ) {
//        databaseReference.orderByChild("id").equalTo(locationId)
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(dataSnapshot: DataSnapshot) {
//                    if (dataSnapshot.exists()) {
//                        for (locationSnapshot in dataSnapshot.children) {
//                            val latitude = locationSnapshot.child("latitude").getValue(Double::class.java)
//                            val longitude = locationSnapshot.child("longitude").getValue(Double::class.java)
//
//                            if (latitude != null && longitude != null) {
//                                callback(latitude, longitude)
//                                return
//                            }
//                        }
//                    }
//
//                    // Handle the case where data for the given ID doesn't exist
//                    callback(0.0, 0.0) // You can choose appropriate default values
//                }
//
//                override fun onCancelled(databaseError: DatabaseError) {
//                    // Handle errors, e.g., databaseError.toException()
//                }
//            })
//
//}

}

