package com.example.screenrecorder

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import androidx.core.app.NotificationCompat
import kotlinx.android.synthetic.main.recording_widget.view.*


class RecordingService: Service()  {
    private var recordView: View? = null
    private lateinit var windowManager: WindowManager
    private var isRecording = false
    private var intent: Intent? = null
    private var resultCode = 0

    private lateinit var screenSaver: ScreenSaver

    private val stopServiceNotification = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            unregisterReceiver(this)
            screenSaver.toggleScreenShare(this@RecordingService, !isRecording)
            stopSelf()
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        resultCode = intent!!.getIntExtra(RESULT_CODE, 0)
        this.intent = intent.getParcelableExtra(Intent.EXTRA_INTENT)
        return super.onStartCommand(intent, flags, startId)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        recordView = LayoutInflater.from(this).inflate(R.layout.recording_widget, null)


        val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)
        } else {
            WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT)
        }

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.addView(recordView, params)

        screenSaver = ScreenSaver(this, windowManager)

        recordView?.findViewById<Button>(R.id.record_btn)?.setOnTouchListener(object : View.OnTouchListener{
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when(event?.action){
                    MotionEvent.ACTION_DOWN -> {

                        //remember initial position
                        initialX = params.x
                        initialY = params.y

                        //get touch location
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val Xdiff = (event.rawX - initialTouchX).toInt()
                        val Ydiff = (event.rawY - initialTouchY).toInt()
                        //The check for Xdiff <10 && YDiff< 10 because sometime elements moves a little while clicking.
//So that is click event.
                        if (Xdiff < 10 && Ydiff < 10) {
                            isRecording = !isRecording
                            if (isRecording){
                                createNotification()
                                recordView?.record_btn?.visibility = View.GONE
                                screenSaver.initRecorder(this@RecordingService, windowManager)
                                screenSaver.startRecording(resultCode, intent, isRecording)
                            }
                        }
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        //calculate x and y coordinates of view
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()

                        //update layout with new X & Y coordinates
                        windowManager.updateViewLayout(recordView, params)
                        return true
                    }
                }
                return false
            }
        })

    }

    private fun createNotification() {
        createNotificationChannel()

        registerReceiver(stopServiceNotification, IntentFilter("myFilter"))
//        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            0, Intent("myFilter"),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Foreground Service")
            .setContentText("input")
            .setSmallIcon(R.drawable.ic_stop_black_24dp)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(
                NotificationManager::class.java
            )
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (recordView != null) windowManager.removeView(recordView)
    }
}