package com.example.screenrecorder

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
import android.view.WindowManager
import android.widget.ToggleButton
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*

class ScreenSaver(mainActivity: Context, windowManager: WindowManager) {

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
        windowManager.defaultDisplay.getMetrics(metrics)
        mScreenDensity = metrics.densityDpi

        DISPLAY_HEIGHT = metrics.heightPixels
        DISPLAY_WIDTH = metrics.widthPixels

        mMediaRecorder = MediaRecorder()
//        mMediaProjectionManager =
//            mainActivity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    fun toggleScreenShare(context: Context, isRecording: Boolean) {
        if (isRecording) {
            recordScreen()
        } else {
            mMediaRecorder.stop()
            mMediaRecorder.reset()
            stopRecordScreen()

            //make it visible in your gallery
            val contentUri = Uri.fromFile(File(mVideoUri))
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = contentUri
            context.applicationContext.sendBroadcast(mediaScanIntent)

//            context.videoView.setVideoURI(Uri.parse(mVideoUri))
//            context.videoView.start()
        }
    }

    fun recordScreen() {
//        if (mMediaProjection == null) {
//            mainActivity.startActivityForResult(
//                mMediaProjectionManager?.createScreenCaptureIntent(),
//                REQUEST_CODE
//            )
//            return
//        }
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

    fun initRecorder(context: Context, windowManager: WindowManager) {
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

            val rotation = windowManager.defaultDisplay.rotation
            val orientation = ORIENTATIONS.get(rotation + 90)
            mMediaRecorder.setOrientationHint(orientation)
            mMediaRecorder.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun startRecording(resultCode: Int, data: Intent?, toggle_button: Boolean) {
        mMediaProjectionCallback = MediaProjectionCallback(toggle_button)
        mMediaProjection = mMediaProjectionManager?.getMediaProjection(Activity.RESULT_OK, data!!)
        mMediaProjection!!.registerCallback(mMediaProjectionCallback, null)
        mVirtualDisplay = createVirtualDisplay()
        mMediaRecorder.start()
    }

    private inner class MediaProjectionCallback(var isRecording: Boolean) :
        MediaProjection.Callback() {
        override fun onStop() {
            if (isRecording) {
                isRecording = false
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