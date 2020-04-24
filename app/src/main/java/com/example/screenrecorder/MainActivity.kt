package com.example.screenrecorder

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.delay

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)

        mMediaProjectionManager =
           getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager



        if (!Settings.canDrawOverlays(this)){
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, DRAW_OVER_OTHER_APP_PERMISSION)
        } else {

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
                getMediaProjection()
            }
//        }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DRAW_OVER_OTHER_APP_PERMISSION){
            requestPermission()
        } else {
            if (requestCode != REQUEST_CODE) {
                Toast.makeText(this, "Unknown Error", Toast.LENGTH_SHORT).show()
                return
            } else {

                if (resultCode != Activity.RESULT_OK) {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                    return
                } else {
                    startMyService(data)
                }
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
                    getMediaProjection()
                } else {
                    showSnackBar()
                }
                return
            }
        }
    }

    fun getMediaProjection(){
        if (mMediaProjection == null) {
            startActivityForResult(
                mMediaProjectionManager?.createScreenCaptureIntent(),
                REQUEST_CODE
            )
        }
    }

    private fun showSnackBar() {
        Snackbar.make(constraint_layout, "Permissions", Snackbar.LENGTH_INDEFINITE)
            .setAction("Enable") {
                requestPermission()
            }.show()
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            ), REQUEST_PERMISSIONS
        )
    }

    private fun startMyService(intent: Intent?){
        val service = Intent(baseContext, RecordingService::class.java)
        service.putExtra(Intent.EXTRA_INTENT, intent)
        service.setAction(Context.ACTIVITY_SERVICE)
        ContextCompat.startForegroundService(this, service)
        finish()
    }
}
