package com.example.tnutchatboxapp

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.appcompat.app.AlertDialog
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

class MyApplication : Application() {

    companion object {
        lateinit var instance: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Set up the default uncaught exception handler
        Thread.setDefaultUncaughtExceptionHandler(
            GlobalExceptionHandler(this)
        )
    }
}

class GlobalExceptionHandler(private val application: Application) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        val stringWriter = StringWriter()
        exception.printStackTrace(PrintWriter(stringWriter))
        val stackTrace = stringWriter.toString()

        val intent = Intent(application, ErrorActivity::class.java)
        intent.putExtra("error_details", stackTrace)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        application.startActivity(intent)

        // Terminate the process
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }
}