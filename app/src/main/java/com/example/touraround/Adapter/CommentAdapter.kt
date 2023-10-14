package com.example.touraround.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.RecyclerView
import com.example.touraround.Comment
import com.example.touraround.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase


class CommentAdapter(private val comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {


    val firebaseUser = FirebaseAuth.getInstance().currentUser
    val userId = firebaseUser?.uid
    val userEmail = firebaseUser?.email
   inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewComment: TextView = itemView.findViewById(R.id.textViewComment)
        val user: TextView = itemView.findViewById(R.id.User)

        val editButton = itemView.findViewById<Button>(R.id.editButton)
        val deleteButton = itemView.findViewById<Button>(R.id.deleteButton)



       fun bind(comment: Comment ,currentUserUid: String) {


           val isEditable = comment.uid == currentUserUid


           textViewComment.text = comment.text
           user.text = comment.uname

           if (isEditable) {
               // If the comment is editable by the current user, show an EditText
               val editText = EditText(itemView.context)
               editText.setText(comment.text)
               editText.hint = "Edit your comment"

               // Replace the TextView with the EditText
               (textViewComment.parent as? ViewGroup)?.removeView(textViewComment)
               (user.parent as? ViewGroup)?.removeView(user)

               val container = itemView.findViewById<LinearLayout>(R.id.container)
               container.addView(editText)

               editButton.setOnClickListener {
                   // Handle the edit button click for an editable comment
                   comment.commentId?.let { it1 -> handleEditButtonClick(it1, editText.text.toString()) }
               }
           } else {
               // If the comment is not editable, show the TextView
               textViewComment.text = comment.text
               user.text = comment.uid

               editButton.visibility = View.GONE
               deleteButton.visibility = View.GONE

               editButton.setOnClickListener {
                   // Handle edit button click for a non-editable comment
                   comment.commentId?.let { it1 -> handleEditButtonClick(it1, "$comment") }
               }
           }


           deleteButton.setOnClickListener {
               // Handle delete button click
               comment.commentId?.let { it1 -> handleDeleteButtonClick(it1) }
           }
       }
   }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.recyclerviewcomments, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
//        holder.textViewComment.text = comments[position].text
//        holder.user.text = comments[position].uid
        holder.bind(comments[position],"$userId")



    }

    override fun getItemCount(): Int {
        return comments.size
    }

    private fun handleEditButtonClick(commentId: String,newComment:String) {
        // Assuming you have a reference to your Firebase database
        val commentsRef = FirebaseDatabase.getInstance().getReference("comments")

        // Update the comment in the database
        commentsRef.child(commentId).child("text").setValue("$newComment")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {

                    // Comment updated successfully
                   // Toast.makeText(, "Comment edited", Toast.LENGTH_SHORT).show()
                } else {
                    // Error updating comment
                    //Toast.makeText(this@CommentAdapter, "Error editing comment", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun handleDeleteButtonClick(commentId: String) {
        // Assuming you have a reference to your Firebase database
        val commentsRef = FirebaseDatabase.getInstance().getReference("comments")

        // Remove the comment from the database
        commentsRef.child(commentId).removeValue()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Comment deleted successfully
                //    Toast.makeText(this, "Comment deleted", Toast.LENGTH_SHORT).show()

                    // If you are in an Activity, use the following code to finish the activity

                    //Toast.makeText(context, "Comment edited", Toast.LENGTH_SHORT).show()
                } else {
                    // Error deleting comment
                   // Toast.makeText(this@CommentAdapter, "Error deleting comment", Toast.LENGTH_SHORT).show()
                }
            }
    }

}
