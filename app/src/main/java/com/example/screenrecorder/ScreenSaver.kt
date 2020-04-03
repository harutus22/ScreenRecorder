package com.example.screenrecorder

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.SparseIntArray
import android.view.Surface
import android.widget.ToggleButton
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

class ScreenSaver(mainActivity: Activity) {

    private var mMediaProjectionManager: MediaProjectionManager
    private var mMediaProjection: MediaProjection? = null
    private var mVirtualDisplay: VirtualDisplay? = null
    private lateinit var mMediaProjectionCallback: MediaProjectionCallback
    private var mMediaRecorder: MediaRecorder

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

    init {
        val metrics = DisplayMetrics()
        mainActivity.windowManager.defaultDisplay.getMetrics(metrics)
        mScreenDensity = metrics.densityDpi

        DISPLAY_HEIGHT = metrics.heightPixels
        DISPLAY_WIDTH = metrics.widthPixels

        mMediaRecorder = MediaRecorder()
        mMediaProjectionManager =
            mainActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    fun toggleScreenShare(context: Activity) {
        if (((context.toggle_button) as ToggleButton).isChecked) {
            initRecorder(context)
            recordScreen(context)
        } else {
            mMediaRecorder.stop()
            mMediaRecorder.reset()
            stopRecordScreen()

            //make it visible in your gallery
            val contentUri = Uri.fromFile(File(mVideoUri))
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = contentUri
            context.application.sendBroadcast(mediaScanIntent)

            context.videoView.setVideoURI(Uri.parse(mVideoUri))
            context.videoView.start()
        }
    }

    private fun recordScreen(context: Activity) {
        if (mMediaProjection == null) {
            context.startActivityForResult(
                mMediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_CODE
            )
            return
        }

        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder.start()
    }

    private fun createVirtualDisplay(): VirtualDisplay {
        return mMediaProjection!!.createVirtualDisplay(
            "MainActivity", DISPLAY_WIDTH,
            DISPLAY_HEIGHT, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mMediaRecorder.surface, null, null
        )
    }

    private fun initRecorder(context: Activity) {
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)

            mVideoUri =
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                    "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)}" +
                            "${StringBuilder("/Screen_Record").append(
                                SimpleDateFormat("dd-MM-yyyy-hh-mm-ss")
                                    .format(Date())
                            ).append(".mp4")}"
                } else {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, UUID.randomUUID().toString())
                        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/Screen_Record")
                    }
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        .toString()
                }

            mMediaRecorder.setOutputFile(mVideoUri)
            mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT)
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            mMediaRecorder.setVideoEncodingBitRate(512 * 1000)
            mMediaRecorder.setVideoFrameRate(30)

            val rotation = context.windowManager.defaultDisplay.rotation
            val orientation = ORIENTATIONS.get(rotation + 90)
            mMediaRecorder.setOrientationHint(orientation)
            mMediaRecorder.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    fun startRecording(resultCode: Int, data: Intent?, context: Activity) {
        mMediaProjectionCallback = MediaProjectionCallback(context.toggle_button)
        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data!!)
        mMediaProjection!!.registerCallback(mMediaProjectionCallback, null)
        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder.start()
    }

    private inner class MediaProjectionCallback(val toggle_button: ToggleButton) :
        MediaProjection.Callback() {
        override fun onStop() {
            if (toggle_button.isChecked) {
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
        if (mMediaProjection != null) {
            mMediaProjection?.unregisterCallback(mMediaProjectionCallback)
            mMediaProjection?.stop()
            mMediaProjection = null
        }
    }
}