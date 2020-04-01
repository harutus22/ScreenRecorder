package com.example.screenrecorder

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.SparseIntArray
import android.view.Surface
import android.view.View
import android.widget.Toast
import android.widget.ToggleButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var mMediaProjectionManager: MediaProjectionManager
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private lateinit var mMediaProjectionCallback: MediaProjectionCallback
    private lateinit var mMediaRecorder: MediaRecorder

    private var mVideoUri = ""

    private var mScreenDensity = 0

    companion object {
        private var DISPLAY_WIDTH = 720
        private var DISPLAY_HEIGHT = 1280
        private val ORIENTATIONS = SparseIntArray()
    }

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 90)
        ORIENTATIONS.append(Surface.ROTATION_90, 0)
        ORIENTATIONS.append(Surface.ROTATION_180, 270)
        ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        mScreenDensity = metrics.densityDpi

        DISPLAY_HEIGHT = metrics.heightPixels
        DISPLAY_WIDTH = metrics.widthPixels

        mMediaRecorder = MediaRecorder()
        mMediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

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
                toggleScreenShare(it)
            }
        }
    }

    private fun toggleScreenShare(view: View) {
        if (((view) as ToggleButton).isChecked) {
            initRecorder()
            recordScreen()
        } else {
            mMediaRecorder.stop()
            mMediaRecorder.reset()
            stopRecordScreen()

            videoView.setVideoURI(Uri.parse(mVideoUri))
            videoView.start()
        }
    }

    private fun recordScreen() {
        if (mMediaProjection == null){
            startActivityForResult(mMediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE)
            return
        }

        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder.start()
    }

    private fun createVirtualDisplay(): VirtualDisplay {
        return mMediaProjection!!.createVirtualDisplay("MainActivity", DISPLAY_WIDTH,
            DISPLAY_HEIGHT, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mMediaRecorder.surface, null, null)
    }

    private fun initRecorder() {
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)


            mVideoUri =
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)}" +
                        "${StringBuilder("/Screen_Record").append(
                            SimpleDateFormat("dd-MM-yyyy-hh-mm-ss")
                                .format(Date())
                        ).append(".mp4")}"
            } else {
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, UUID.randomUUID().toString())
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/ThermalCamera")
                }
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    .toString()
            }

            mMediaRecorder.setOutputFile(mVideoUri)
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT)
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mMediaRecorder.setVideoEncodingBitRate(512*1000)
            mMediaRecorder.setVideoFrameRate(30)

            val rotation = windowManager.defaultDisplay.rotation
            val orientation = ORIENTATIONS.get(rotation + 90)
            mMediaRecorder.setOrientationHint(orientation)
            mMediaRecorder.prepare()
        } catch (e: IOException){
            e.printStackTrace()
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this@MainActivity, arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
            ), REQUEST_PERMISSIONS
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE){
            Toast.makeText(this, "Unknown Error", Toast.LENGTH_SHORT).show()
        }

        if (resultCode != Activity.RESULT_OK){
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            toggle_button.isChecked = false
        }

        mMediaProjectionCallback = MediaProjectionCallback()
        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data!!)
        mMediaProjection!!.registerCallback(mMediaProjectionCallback, null)
        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder.start()
    }

    private inner class MediaProjectionCallback: MediaProjection.Callback() {
        override fun onStop() {
            if (toggle_button.isChecked){
                toggle_button.isChecked = false
                mMediaRecorder.stop()
                mMediaRecorder.reset()
            }
            mMediaProjection = null
            stopRecordScreen()
            super.onStop()
        }
    }

    private fun stopRecordScreen() {
        if (mVirtualDisplay == null) return
        mVirtualDisplay?.release()
        destroyMediaProjection()
    }

    private fun destroyMediaProjection() {
        if (mMediaProjection != null){
            mMediaProjection?.unregisterCallback(mMediaProjectionCallback)
            mMediaProjection?.stop()
            mMediaProjection = null
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] + grantResults[1] == PackageManager.PERMISSION_GRANTED){
                    toggleScreenShare(toggle_button)
                } else {
                    showSnackBar()
                }
                return
            }
        }
    }

    private fun showSnackBar(){
        toggle_button.isChecked = false
        Snackbar.make(constraint_layout, "Permissions", Snackbar.LENGTH_INDEFINITE)
            .setAction("Enable") {
                requestPermission()
            }.show()
    }
}
