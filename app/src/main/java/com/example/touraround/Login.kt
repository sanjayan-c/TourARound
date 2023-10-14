package com.example.touraround

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.touraround.Fragment.LoginFragment
import com.example.touraround.Fragment.SignUpFragment

class Login : AppCompatActivity() {

    private lateinit var cus_text_register: TextView
    private lateinit var cus_text_register2: TextView
    private lateinit var text_dont_have_account2: TextView
    private lateinit var text_dont_have_account: TextView
    private lateinit var cus_enter: LinearLayout
    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        cus_text_register = findViewById(R.id.cus_text_register)
        cus_text_register2 = findViewById(R.id.cus_text_register2)
        text_dont_have_account = findViewById(R.id.text_dont_have_account)
        text_dont_have_account2 = findViewById(R.id.text_dont_have_account2)
        cus_enter = findViewById(R.id.cus_enter)


        val registerString = "Sign Up"
        val mSpannableString = SpannableString(registerString)
        mSpannableString.setSpan(UnderlineSpan(), 0, mSpannableString.length, 0)
        val registerString2 = "Login"
        val mSpannableString2 = SpannableString(registerString2)
        mSpannableString2.setSpan(UnderlineSpan(), 0, mSpannableString2.length, 0)
        cus_text_register.text = mSpannableString
        cus_text_register2.text = mSpannableString2

        val loginFragment = LoginFragment()
        val signupFragment = SignUpFragment()

        // Set the initial fragment (e.g., Transactions)
        setFragment(loginFragment)

        cus_text_register.setOnClickListener{
            setFragment(signupFragment)
            cus_text_register.visibility = View.GONE
            text_dont_have_account.visibility = View.GONE
            cus_text_register2.visibility = View.VISIBLE
            text_dont_have_account2.visibility = View.VISIBLE
        }
        cus_text_register2.setOnClickListener {
            setFragment(loginFragment)
            cus_text_register2.visibility = View.GONE
            text_dont_have_account2.visibility = View.GONE
            cus_text_register.visibility = View.VISIBLE
            text_dont_have_account.visibility = View.VISIBLE
        }
        cus_enter.setOnClickListener {
            val intent = Intent(this,CameraView::class.java)
            finish()
            startActivity(intent)
        }


    }
    private fun setFragment(fragment: Fragment) {
        val fragmentManager: FragmentManager = supportFragmentManager
        val transaction: FragmentTransaction = fragmentManager.beginTransaction()

        // Replace the fragment container with the new fragment
        transaction.replace(R.id.cusLoginFragmentContainer, fragment)

        // Commit the transaction
        transaction.commit()

        currentFragment = fragment
    }
}