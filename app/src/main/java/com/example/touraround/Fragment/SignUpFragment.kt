package com.example.touraround.Fragment

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.touraround.Login
import com.example.touraround.R
import com.example.touraround.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpFragment : Fragment() {

    private lateinit var cus_login_email : EditText
    private lateinit var cus_login_password : EditText
    private lateinit var cus_login_email_exception : TextView
    private lateinit var cus_login_password_exception : TextView
    private lateinit var cus_login_confirm_password : EditText
    private lateinit var cus_passwords_not_match : TextView
    private lateinit var cus_signup_button : Button
    private lateinit var userAuth: FirebaseAuth
    private lateinit var userDbRef: DatabaseReference
    private lateinit var cusimgConfirmPasswordVisibility: ImageView
    private lateinit var cusimgPasswordVisibility: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_up, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userAuth = FirebaseAuth.getInstance()
        cus_login_email = view.findViewById(R.id.cus_login_email)
        cus_login_password = view.findViewById(R.id.cus_login_password)
        cus_login_email_exception = view.findViewById(R.id.cus_login_email_exception)
        cus_login_password_exception = view.findViewById(R.id.cus_login_password_exception)
        cus_login_confirm_password = view.findViewById(R.id.cus_login_confirm_password)
        cus_passwords_not_match = view.findViewById(R.id.cus_passwords_not_match)
        cus_signup_button = view.findViewById(R.id.cus_signup_button)
        cusimgPasswordVisibility = view.findViewById(R.id.cusimgPasswordVisibility)
        cusimgConfirmPasswordVisibility = view.findViewById(R.id.cusimgConfirmPasswordVisibility)

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

        cusimgConfirmPasswordVisibility.setOnClickListener {
            if (cus_login_confirm_password.transformationMethod == PasswordTransformationMethod.getInstance()) {
                cus_login_confirm_password.transformationMethod = HideReturnsTransformationMethod.getInstance()
                cusimgConfirmPasswordVisibility.setImageResource(R.drawable.visibility_off)
            } else {
                cus_login_confirm_password.transformationMethod = PasswordTransformationMethod.getInstance()
                cusimgConfirmPasswordVisibility.setImageResource(R.drawable.visibility)
            }
            // Move the cursor to the end of the text
            cus_login_confirm_password.setSelection(cus_login_confirm_password.text.length)
        }

        cus_signup_button.setOnClickListener{
            cus_login_email_exception.visibility = View.GONE
            cus_login_password_exception.visibility = View.GONE
            cus_passwords_not_match.visibility = View.GONE

            val password = cus_login_password.text.toString()
            val confirmPassword = cus_login_confirm_password.text.toString()
            val email = cus_login_email.text.toString()
            Log.d("email",email)
            Log.d("password",password)

            if(email=="" || password=="") {
                if (email == "") {
                    cus_login_email_exception.visibility = View.VISIBLE
                    cus_login_email_exception.text = "Enter Your Email"
                }
                if (password == "") {
                    cus_login_password_exception.visibility = View.VISIBLE
                    cus_login_password_exception.text = "Enter Your Password"
                }
            }else {
                Log.d("else","else")
                if (password == confirmPassword) {
                    val name = ""
                    val contactNumber = ""
                    val details = ""
                    val emergency = ""
                    signUp(name, email, password, emergency, contactNumber,details)
                } else {
                    cus_passwords_not_match.visibility = View.VISIBLE
                    cus_passwords_not_match.text = "Passwords doesn't match"
                }
            }
        }

    }

    private fun signUp(name: String, email: String, password: String,emergency:String,contactNumber: String,details: String) {
        userAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(ContentValues.TAG, "createUserWithEmail:success")

                    // Send email verification
                    val user = userAuth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                addUserToDatabase(name,email,userAuth.currentUser?.uid!!,emergency,contactNumber,details)
                                Log.d(ContentValues.TAG, "Email verification sent.")
                                Toast.makeText(
                                    requireContext(),
                                    "Verification email sent. Please check your email.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent= Intent(requireContext(), Login::class.java)
                                startActivity(intent)
                                // You can navigate to the login screen or perform any other actions here
                            } else {
                                Log.e(ContentValues.TAG, "Failed to send verification email.", verificationTask.exception)
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to send verification email. Please try again later.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    // You may want to navigate to the login screen or another activity here
                } else {
                    // If sign-up fails, check the error code
                    val errorCode = (task.exception as FirebaseAuthException).errorCode
                    if (errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                        // Email already exists
                        Toast.makeText(
                            requireContext(),
                            "Email already exists",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        // Handle other sign-up errors
                        Log.w(ContentValues.TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(requireContext(), "Some error has occurred", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    private fun addUserToDatabase(name:String,email:String,uid:String,emergency:String,number:String,details:String){
        userDbRef= FirebaseDatabase.getInstance().getReference()
        userDbRef.child("user").child(uid).setValue(User(name, email, uid,emergency,number,details))
    }

}