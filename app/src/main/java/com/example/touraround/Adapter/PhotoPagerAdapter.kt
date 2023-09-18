package com.example.touraround.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.touraround.R
import com.squareup.picasso.Picasso

class PhotoPagerAdapter(private val photoReferences: List<String>) :
    RecyclerView.Adapter<PhotoPagerAdapter.ViewHolder>() {
    // Add a placeholder image resource ID
    private val placeholderImageResource = R.drawable.error_placeholder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageUrl = photoReferences[position]

        if (imageUrl.isNotEmpty()) {
            // Load and display the image using Picasso
            Picasso.get()
                .load(imageUrl)
                .error(R.drawable.error_placeholder)
                .into(holder.imageView)
        } else {
            // If no photo URL is available, set the ImageView to display the placeholder image
            holder.imageView.setImageResource(placeholderImageResource)
        }
    }

    override fun getItemCount(): Int {
        return photoReferences.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imgPlace)
    }
}
