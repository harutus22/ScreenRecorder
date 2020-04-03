package com.example.screenrecorder

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var screenSaver: ScreenSaver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        screenSaver = ScreenSaver(this)

        toggle_button.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                + ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.RECORD_AUDIO
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this@MainActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        this@MainActivity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    showSnackBar()
                } else {
                    requestPermission()
                }
            } else {
                screenSaver.toggleScreenShare(this)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE) {
            Toast.makeText(this, "Unknown Error", Toast.LENGTH_SHORT).show()
            return
        } else {

            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                toggle_button.isChecked = false
                return
            } else {
                screenSaver.startRecording(resultCode, data, this)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    screenSaver.toggleScreenShare(this)
                } else {
                    showSnackBar()
                }
                return
            }
        }
    }

    fun showSnackBar() {
        toggle_button.isChecked = false
        Snackbar.make(constraint_layout, "Permissions", Snackbar.LENGTH_INDEFINITE)
            .setAction("Enable") {
                requestPermission()
            }.show()
    }

    fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            ), REQUEST_PERMISSIONS
        )
    }

}
