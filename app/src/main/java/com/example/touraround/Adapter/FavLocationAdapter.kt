package com.example.touraround.Adapter

import LocationData
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.model.AdapterClass
import androidx.recyclerview.widget.RecyclerView
import com.example.touraround.EditLocationActivity
import com.example.touraround.R

class FavLocationAdapter(private val context: android.content.Context, private val datalist: ArrayList<LocationData>) : RecyclerView.Adapter<ViewHolderClass>() {

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
        holder.cardView.setOnClickListener {
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
    val cardView: CardView = itemView.findViewById(R.id.recCard)

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
}

