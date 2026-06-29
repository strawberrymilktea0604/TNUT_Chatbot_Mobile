package com.example.tnutchatboxapp

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tnutchatboxapp.auth.LoginActivity
import com.example.tnutchatboxapp.utils.SessionManager

class WelcomeActivity : AppCompatActivity() {

    private lateinit var logoImageView: ImageView
    private lateinit var welcomeTitle: TextView
    private lateinit var welcomeSubtitle: TextView
    private lateinit var startButton: Button
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        sessionManager = SessionManager(this)
        
        // Check if user is already logged in
        if (!sessionManager.fetchAuthToken().isNullOrEmpty()) {
            navigateToMain()
            return
        }

        initViews()
        startAnimations()
        setupClickListener()
    }

    private fun initViews() {
        logoImageView = findViewById(R.id.logoImageView)
        welcomeTitle = findViewById(R.id.welcomeTitle)
        welcomeSubtitle = findViewById(R.id.welcomeSubtitle)
        startButton = findViewById(R.id.startButton)
    }

    private fun startAnimations() {
        // Animation for logo - scale up with bounce
        val logoAnimation = AnimationUtils.loadAnimation(this, R.anim.bounce_in)
        logoImageView.startAnimation(logoAnimation)

        // Animation for title - slide in from left
        val titleAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        titleAnimation.startOffset = 300
        welcomeTitle.startAnimation(titleAnimation)

        // Animation for subtitle - slide in from left with delay
        val subtitleAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_left)
        subtitleAnimation.startOffset = 600
        welcomeSubtitle.startAnimation(subtitleAnimation)

        // Animation for button - slide in from bottom
        val buttonAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_in_bottom)
        buttonAnimation.startOffset = 900
        startButton.startAnimation(buttonAnimation)
    }

    private fun setupClickListener() {
        startButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            // Apply slide transition from left to right
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }
    
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        // Disable back button to prevent going back to splash
        super.onBackPressed()
    }
}
