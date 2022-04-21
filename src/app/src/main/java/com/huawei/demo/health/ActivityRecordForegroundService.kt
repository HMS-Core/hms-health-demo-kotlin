/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2022-2022. All rights reserved.
 */

package com.huawei.demo.health

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.Context
import android.app.PendingIntent
import android.app.Service
import android.graphics.BitmapFactory
import android.util.Log
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message

import androidx.core.app.NotificationCompat

import com.huawei.hms.hihealth.ActivityRecordsController
import com.huawei.hms.hihealth.HuaweiHiHealth

/**
 * Defining a Frontend ActivityRecordForegroundService
 *
 * @since 2020-09-05
 */
class ActivityRecordForegroundService : Service() {

    // Internal context object
    private var context: Context? = null

    // Handler to send continue workout msg
    private var mHandler: Handler? = null

    // Handler thread
    private var mHandlerThread: HandlerThread? = null

    // HMS Health ActivityRecordsController
    private var activityRecordsController: ActivityRecordsController? = null

    override fun onCreate() {
        super.onCreate()
        context = this
        Log.i(TAG, "ActivityRecordForegroundService is create.")
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Binding a notification bar
        getNotification()
        initActivityRecordController()
        initHandler()
        // send continue ActivityRecord delayed message
        mHandler!!.sendEmptyMessageDelayed(MSG_WORKOUT_TIMEOUT, WORKOUT_TIMEOUT.toLong())
        return super.onStartCommand(intent, flags, startId)
    }


    /**
     * init ActivityRecordsController
     */
    private fun initActivityRecordController() {
        activityRecordsController = HuaweiHiHealth.getActivityRecordsController(context!!)
    }


    /**
     * init handler to handle continue ActivityRecord msg
     */
    private fun initHandler() {
        if (mHandlerThread == null) {
            Log.i(TAG, "mHandlerThread is null, begin to create")
            mHandlerThread = HandlerThread("healthkit_workout_thread_handler")
            mHandlerThread!!.start()
            mHandler = object : Handler(mHandlerThread!!.looper) {
                override fun handleMessage(msg: Message) {
                    Log.d(TAG, "meed handle js unbind msg: " + msg.what)
                    super.handleMessage(msg)
                    if (msg.what == MSG_WORKOUT_TIMEOUT) {
                        continueBackgroundActivityRecord()
                    }
                }
            }
        }
    }

    /**
     * Continue activity records run in background
     */
    fun continueBackgroundActivityRecord() {
        Log.i(TAG, "this is continue backgroundActivityRecord")

        // Call the related method of ActivityRecordsController to continue activity records run in background.
        // The input parameter can be the ID string of ActivityRecord
        val endTask = activityRecordsController!!.continueActivityRecord("MyBackgroundActivityRecordId")
        endTask.addOnSuccessListener {
            Log.i(TAG, "continue backgroundActivityRecord was successful!")
            mHandler!!.sendEmptyMessageDelayed(MSG_WORKOUT_TIMEOUT, WORKOUT_TIMEOUT.toLong())
        }.addOnFailureListener { e -> Log.i(TAG, "continue backgroundActivityRecord error " + e.message) }
    }

    /**
     * Bind the service to the notification bar so that the service can be changed to a foreground service.
     */
    private fun getNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "1").setContentTitle("ActivityRecord")
            .setContentText("ActivityRecord Ongoing")
            .setWhen(System.currentTimeMillis())
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, HealthKitActivityRecordControllerActivity::class.java),
                    0
                )
            )
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("1", "subscribeName", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "description"
            notificationManager.createNotificationChannel(channel)
        }
        notification.flags = Notification.FLAG_ONGOING_EVENT
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        mHandler!!.removeCallbacksAndMessages(null)
        Log.i(TAG, "ActivityRecordForegroundService is destroy.")
    }

    companion object {
        private val TAG = "ForegroundService"

        // Continue ActivityRecord Msg what
        private val MSG_WORKOUT_TIMEOUT = 1005

        // delayed time
        private val WORKOUT_TIMEOUT = 540000
    }
}
