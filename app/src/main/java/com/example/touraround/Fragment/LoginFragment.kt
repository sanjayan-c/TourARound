package com.example.touraround.Fragment

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.touraround.CameraView
import com.example.touraround.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference

class LoginFragment : Fragment() {

    private lateinit var cus_login_email : EditText
    private lateinit var cus_login_password : EditText
    private lateinit var cus_login_no_username_password : TextView
    private lateinit var cusimgPasswordVisibility : ImageView
    private lateinit var cus_login_button : Button
    private lateinit var userAuth: FirebaseAuth
    private lateinit var userDbRef: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userAuth = FirebaseAuth.getInstance()
        cus_login_email = view.findViewById(R.id.cus_login_email)
        cus_login_password = view.findViewById(R.id.cus_login_password)
        cusimgPasswordVisibility = view.findViewById(R.id.cusimgPasswordVisibility)
        cus_login_button = view.findViewById(R.id.cus_login_button)
        cus_login_no_username_password = view.findViewById(R.id.cus_login_no_username_password)

        cusimgPasswordVisibility.setOnClickListener {
            if (cus_login_password.transformationMethod == PasswordTransformationMethod.getInstance()) {
                cus_login_password.transformationMethod = HideReturnsTransformationMethod.getInstance()
                cusimgPasswordVisibility.setImageResource(R.drawable.visibility_off)
            } else {
                cus_login_password.transformationMethod = PasswordTransformationMethod.getInstance()
                cusimgPasswordVisibility.setImageResource(R.drawable.visibility)
            }
            // Move the cursor to the end of the text
            cus_login_password.setSelection(cus_login_password.text.length)
        }

        cus_login_button.setOnClickListener{
            cus_login_no_username_password.visibility = View.GONE
            val password = cus_login_password.text.toString()
            val email = cus_login_email.text.toString()

            if(email=="" || password=="") {
                cus_login_no_username_password.visibility = View.VISIBLE
                cus_login_no_username_password.text = "Enter Email and Password"
            }else{
                cus_login_no_username_password.visibility = View.GONE
                login(email,password)
            }
        }

    }
    private fun login(email:String,password:String){
        userAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    //logging in
                    val intent= Intent(requireContext(),CameraView::class.java)
                    startActivity(intent)
                } else {

                    // Check the error message
                    val errorMessage = task.exception?.message
                    if (errorMessage != null) {
                        if (errorMessage.contains("password")) {
                            // Incorrect password
                            Toast.makeText(
                                requireContext(),
                                "Incorrect password",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (errorMessage.contains("no user record")) {
                            // Email not found
                            Toast.makeText(
                                requireContext(),
                                "No account found for this email",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Other error, show a generic message
                            Toast.makeText(
                                requireContext(),
                                "Login failed. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // Unexpected error, show a generic message
                        Toast.makeText(
                            requireContext(),
                            "Login failed. Please try again.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }
//    private fun login(email:String,password:String){
//        userAuth.signInWithEmailAndPassword(email, password)
//            .addOnCompleteListener(requireActivity()) { task ->
//                if (task.isSuccessful) {
//                    val user = userAuth.currentUser
//                    if (user != null && user.isEmailVerified) {
//                        // User is authenticated and their email is verified
//                        Log.d(ContentValues.TAG, "signInWithEmail:success")
//
//                        // Proceed to the next screen or perform any other actions
//                        val intent = Intent(requireContext(), CameraView::class.java)
//                        startActivity(intent)
//                    } else {
//                        // User is authenticated but their email is not verified
//                        Toast.makeText(
//                            requireContext(),
//                            "Please verify your email address first.",
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                } else {
//                    // If sign-in fails, display a message to the user.
//                    Log.w(ContentValues.TAG, "signInWithEmail:failure", task.exception)
//                    Toast.makeText(requireContext(), "Authentication failed.", Toast.LENGTH_SHORT).show()
//                }
//            }
//    }
}