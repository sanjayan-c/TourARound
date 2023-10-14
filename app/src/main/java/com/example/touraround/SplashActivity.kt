package com.example.touraround

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    private val SPLASH_TIMEOUT: Long = 2000 // 2 seconds
    private lateinit var cus_login_register : LinearLayout
    private lateinit var appLoginGuest : Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        cus_login_register = findViewById(R.id.cus_login_register)
        appLoginGuest = findViewById(R.id.appLoginGuest)
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val userId = firebaseUser?.uid

        // Delay for the specified time and then launch the main activity
        Handler().postDelayed({
            if(userId==null) {
                cus_login_register.visibility = View.VISIBLE
            }
            appLoginGuest.visibility = View.VISIBLE
        }, SPLASH_TIMEOUT)

        appLoginGuest.setOnClickListener{
            val intent = Intent(this, CameraView::class.java)
            startActivity(intent)
            finish()
        }
        cus_login_register.setOnClickListener{
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
    }
}
