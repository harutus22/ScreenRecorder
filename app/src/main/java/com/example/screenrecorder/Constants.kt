package com.example.screenrecorder

import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager

const val REQUEST_CODE = 1000
const val REQUEST_PERMISSIONS = 1001
const val RESULT_CODE = "result_code"
const val CHANNEL_ID = "ForegroundServiceChannel"
const val DRAW_OVER_OTHER_APP_PERMISSION = 2048

var mMediaProjectionManager: MediaProjectionManager? = null
var mMediaProjection: MediaProjection? = null