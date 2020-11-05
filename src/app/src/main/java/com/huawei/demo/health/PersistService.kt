package com.huawei.demo.health

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.Nullable
import androidx.core.app.NotificationCompat
import com.huawei.hmf.tasks.OnFailureListener
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.hms.hihealth.AutoRecorderController
import com.huawei.hms.hihealth.HiHealthOptions
import com.huawei.hms.hihealth.HuaweiHiHealth
import com.huawei.hms.hihealth.data.DataType
import com.huawei.hms.hihealth.options.OnSamplePointListener
import com.huawei.hms.support.hwid.HuaweiIdAuthManager


class PersistService : Service() {
    private val TAG = "PersistService"

    // HMS Health AutoRecorderController
    private var autoRecorderController: AutoRecorderController? = null

    private var context: Context? = null

    override fun onCreate() {
        super.onCreate()
        context = this
        val options = HiHealthOptions.builder().build()
        val signInHuaweiId = HuaweiIdAuthManager.getExtendedAuthResult(options)
        autoRecorderController = HuaweiHiHealth.getAutoRecorderController(context as PersistService, signInHuaweiId)
        Log.i(TAG, "service is create.")
    }
    @Nullable
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // Invoke the real-time callback interface of the HealthKit.
        getRemoteService()
        // Binding a notification bar
        getNotification()
        return super.onStartCommand(intent, flags, startId)
    }

    // Create a listener and return callback data.
    private val onSamplePointListener = OnSamplePointListener { samplePoint ->
        // The step count, time, and type data reported by the pedometer is called back to the app through
        // samplePoint.
        val intent = Intent()
        intent.putExtra("SamplePoint", samplePoint)
        intent.action = "HealthKitService"
        // Transmits service data to activities through broadcast.
        sendBroadcast(intent)
    }

    /**
     * Callback Interface for Starting the Total Step Count
     */
    private fun getRemoteService() {
        // This interface supports the data type of DT_CONTINUOUS_STEPS_TOTAL.
        val dataType = DataType.DT_CONTINUOUS_STEPS_TOTAL
        // Start recording real-time steps.
        autoRecorderController!!.startRecord(dataType, onSamplePointListener)
            .addOnSuccessListener(OnSuccessListener<Void> { Log.i(TAG, "record steps success... ") })
            .addOnFailureListener(OnFailureListener { Log.i(TAG, "report steps failed... ") })
    }

    /**
     * Bind the service to the notification bar so that the service can be changed to a foreground service.
     */
    private fun getNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, "1").setContentTitle("Real-time step counting")
            .setContentText("Real-time step counting...")
            .setWhen(System.currentTimeMillis())
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(
                PendingIntent.getActivity(this, 0, Intent(this, HealthKitAutoRecorderControllerActivity::class.java), 0)
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
        Log.i(TAG, "PersistService is destroy.")
    }
}
