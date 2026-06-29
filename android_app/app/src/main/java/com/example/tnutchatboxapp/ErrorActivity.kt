package com.example.tnutchatboxapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.system.exitProcess

class ErrorActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)

        val errorDetails = intent.getStringExtra("error_details")
        findViewById<TextView>(R.id.error_details).text = errorDetails

        findViewById<Button>(R.id.close_button).setOnClickListener {
            // Terminate the process
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }
}