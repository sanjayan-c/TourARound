package com.example.touraround

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private val SPLASH_TIMEOUT: Long = 2000 // 2 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.splash_screen)

        // Delay for the specified time and then launch the main activity
        Handler().postDelayed({
            val intent = Intent(this, CameraView::class.java)
            startActivity(intent)
            finish()
        }, SPLASH_TIMEOUT)
    }
}
