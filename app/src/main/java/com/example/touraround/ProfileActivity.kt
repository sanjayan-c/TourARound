package com.example.touraround

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {

    private lateinit var cus_login_name : EditText
    private lateinit var cus_login_emer_contact : EditText
    private lateinit var cus_emergency_name : EditText
    private lateinit var cus_login_emer_details : EditText
    private lateinit var cus_inavlid_number : TextView
    private lateinit var cus_signup_button : Button
    private lateinit var backInTopBar : ImageView
    private lateinit var userMenu : ImageView
    private lateinit var userAuth: FirebaseAuth
    private lateinit var userDbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        userAuth = FirebaseAuth.getInstance()
        val firebaseUser = userAuth.currentUser
        val userId = firebaseUser?.uid
        cus_login_name = findViewById(R.id.cus_login_name)
        cus_login_emer_contact = findViewById(R.id.cus_login_emer_contact)
        cus_emergency_name = findViewById(R.id.cus_emergency_name)
        cus_login_emer_details = findViewById(R.id.cus_login_emer_details)
        cus_inavlid_number = findViewById(R.id.cus_inavlid_number)
        cus_signup_button = findViewById(R.id.cus_signup_button)
        backInTopBar = findViewById(R.id.backInTopBar)
        userMenu = findViewById(R.id.userMenu)
        backInTopBar.visibility = View.VISIBLE
        userMenu.visibility = View.GONE

        // Set an initial hint
        cus_login_emer_contact.hint = "Emergency Contact No"
        // Create an InputFilter that restricts the input to 9 digits
        val maxLength = 13
        val inputFilter = InputFilter { source, start, end, dest, dstart, dend ->
            val newInput = dest.subSequence(0, dstart).toString() +
                    source.subSequence(start, end) +
                    dest.subSequence(dend, dest.length).toString()
            if (newInput.length > maxLength) {
                return@InputFilter ""
            }
            null
        }

        // Apply the InputFilter to the EditText
        cus_login_emer_contact.filters = arrayOf(inputFilter)

        // Create an OnFocusChangeListener
        cus_login_emer_contact.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // On focus, set the hint to just "+94" to show it clearly
                // Set "+94" as a prefix which cannot be edited
                cus_login_emer_contact.setText("+94 ")
                cus_login_emer_contact.setSelection(cus_login_emer_contact.text.length)
            } else {
                // When focus is lost, restore the complete hint
                cus_login_emer_contact.hint = "Emergency Contact No"
            }
        }

        backInTopBar.setOnClickListener{
            val intent = Intent(this, CameraView::class.java)
            startActivity(intent)
            finish()
        }

        // Assume you have a reference to the Firebase Database
        val databaseReference = FirebaseDatabase.getInstance().getReference("user")
        Log.d("YourTag", "Name: $userId")
        var email : String ? = null
        if (userId != null) {
            val userReference = databaseReference.child(userId)

            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val user = dataSnapshot.getValue(User::class.java)

                        // Now you can access the user's data
                        val name = user?.name
                        val details = user?.details
                        val emergencyName = user?.emergencyContactName
                        val emergencyNumber = user?.emergencyContactNumber
                        email= user?.email

                        Log.d("YourTag", "Name: $name")
                        Log.d("YourTag", "Details: $details")
                        Log.d("YourTag", "Emergency Name: $emergencyName")
                        Log.d("YourTag", "Emergency Number: $emergencyNumber")

                        // Update your UI with the retrieved data
                        cus_login_name.text = Editable.Factory.getInstance().newEditable(name)
                        cus_login_emer_contact.text = Editable.Factory.getInstance().newEditable(emergencyNumber)
                        cus_emergency_name.text = Editable.Factory.getInstance().newEditable(emergencyName)
                        cus_login_emer_details.text = Editable.Factory.getInstance().newEditable(details)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
        }

        cus_signup_button.setOnClickListener {
            // Get the values from your EditText fields
            val newName = cus_login_name.text.toString()
            val newDetails = cus_login_emer_details.text.toString()
            val newEmergencyName = cus_emergency_name.text.toString()
            val newEmergencyNumber = cus_login_emer_contact.text.toString()

            // Update the data in the Firebase Realtime Database
            val userId = firebaseUser?.uid
            if (userId != null) {
                val userReference = databaseReference.child(userId)

                // Create a User object with the updated data
                val updatedUser = User(newName,email,userId, newEmergencyName, newEmergencyNumber,newDetails)

                // Update the data in the database
                userReference.setValue(updatedUser)

                // You can also display a success message here if needed
                Toast.makeText(this, "Data updated successfully", Toast.LENGTH_SHORT).show()
            }
        }


    }
}