/*
 * Copyright 2020. Huawei Technologies Co., Ltd. All rights reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 *   http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.demo.health

import java.util.Date

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity

import com.huawei.hms.hihealth.AutoRecorderController
import com.huawei.hms.hihealth.HuaweiHiHealth
import com.huawei.hms.hihealth.data.DataType
import com.huawei.hms.hihealth.data.SamplePoint
import com.huawei.hms.hihealth.options.OnSamplePointListener
import java.text.SimpleDateFormat


class HealthKitAutoRecorderControllerActivity : AppCompatActivity() {
    private val TAG = "AutoRecorderTest"

    // Line separators for the display on the UI
    private val SPLIT = "*******************************" + System.lineSeparator()

    // HMS Health AutoRecorderController
    private var autoRecorderController: AutoRecorderController? = null

    private var mContext: Context? = null

    // Text control that displays action information on the page
    private var logInfoView: TextView? = null

    // Defining a Dynamic Broadcast Receiver
    private var receiver: MyReceiver? = null

    // Record Start Times
    internal var count = 0

    // WakeLock
    private var wl: PowerManager.WakeLock? = null

    /**
     * add app to the battery optimization trust list, to avoid the app be killed
     *
     * @param activity activity
     */
    fun ignoreBatteryOptimization(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                /**
                 * Check whether the current app is added to the battery optimization trust list,
                 * If not, a dialog box is displayed for you to add a battery optimization trust list.
                 */
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                val hasIgnored = powerManager.isIgnoringBatteryOptimizations(activity.packageName)
                if (!hasIgnored) {
                    val newIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    newIntent.data = Uri.parse("package:" + activity.packageName)
                    startActivity(newIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }
    }

    @SuppressLint("InvalidWakeLockTag")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_autorecorder)
        mContext = this
        logInfoView = findViewById(R.id.auto_recorder_log_info) as TextView
        logInfoView!!.setMovementMethod(ScrollingMovementMethod.getInstance())
        initData()
        val pm = mContext!!.getSystemService(Context.POWER_SERVICE) as PowerManager
        wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG)
        wl!!.acquire()
        Log.i(TAG, " wakelock wl.acquire(); ")
    }

    override fun onResume() {
        super.onResume()
        ignoreBatteryOptimization(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        wl!!.release()
        wl = null
        Log.i(TAG, " wakelock wl.release(); ")
    }


    private fun initData() {
        intent = Intent()
        intent!!.setPackage(packageName)
        intent!!.action = "HealthKitService"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.i(TAG, "signIn onActivityResult")
        super.onActivityResult(requestCode, resultCode, data)

        autoRecorderController =
            HuaweiHiHealth.getAutoRecorderController(this@HealthKitAutoRecorderControllerActivity)
    }

    /**
     * Returns the callback data in SamplePoint mode.
     *
     * @param samplePoint Reported data
     */
    private fun showSamplePoint(samplePoint: SamplePoint?) {
        if (samplePoint != null) {
            logger("Sample point type: " + samplePoint.dataType.name)
            for (field in samplePoint.dataType.fields) {
                logger("Field: " + field.name + " Value: " + samplePoint.getFieldValue(field))
                logger(stampToData(System.currentTimeMillis().toString()))
            }
        } else {
            logger("samplePoint is null!! ")
            logger(SPLIT)
        }
    }

    /**
     * Timestamp conversion function
     *
     * @param timeStr Timestamp
     * @return Time in date format
     */
    private fun stampToData(timeStr: String): String {
        val res: String
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val it = java.lang.Long.parseLong(timeStr)
        val date = Date(it)
        res = simpleDateFormat.format(date)
        return res
    }

    /**
     * start record By DataType
     *
     * @param view the button view
     */
    fun startRecordByType(view: View) {
        if (count < 1) {
            startService(intent)
            // Registering a Broadcast Receiver
            receiver = MyReceiver()
            val filter = IntentFilter()
            filter.addAction("HealthKitService")
            this.registerReceiver(receiver, filter)
            count++
        } else {
            this.unregisterReceiver(receiver)
            count--
            startService(intent)
            // Registering a Broadcast Receiver
            receiver = MyReceiver()
            val filter = IntentFilter()
            filter.addAction("HealthKitService")
            this.registerReceiver(receiver, filter)
            count++
        }
    }

    /**
     * dynamic broadcast receiver
     */
    inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val bundle = intent.extras
            val samplePoint = bundle!!.get("SamplePoint") as SamplePoint
            showSamplePoint(samplePoint)
        }
    }

    /**
     * stop record By DataType
     *
     * @param view the button view
     */
    fun stopRecordByType(view: View) {
        logger("stopRecordByType")
        if (autoRecorderController == null) {
            autoRecorderController = HuaweiHiHealth.getAutoRecorderController(this)
        }

        autoRecorderController!!.stopRecord(DataType.DT_CONTINUOUS_STEPS_TOTAL, onSamplePointListener)
            .addOnCompleteListener { taskResult ->
                // the interface won't always success, if u use the onComplete interface, u should add the judgement
                // of result is successful or not. the fail reason include:
                // 1.the app hasn't been granted the scropes
                // 2.this type is not supported so far
                if (taskResult.isSuccessful) {
                    logger("onComplete stopRecordByType Successful")
                } else {
                    logger("onComplete stopRecordByType Failed")
                }
            }
            .addOnSuccessListener {
                // u could call addOnSuccessListener to print something
                logger("onSuccess stopRecordByType Successful")
                logger(SPLIT)
            }
            .addOnFailureListener { e ->
                // otherwise u could call addOnFailureListener to catch the fail result
                logger("onFailure stopRecordByType Failed: " + e.message)
                logger(SPLIT)
            }
        if (count > 0) {
            stopService(intent)
            this.unregisterReceiver(receiver)
            count--
        }
    }

    private fun logger(string: String) {
        Log.i(TAG, string)
        logInfoView!!.append(string + System.lineSeparator())
        val offset = logInfoView!!.lineCount * logInfoView!!.lineHeight
        if (offset > logInfoView!!.height) {
            logInfoView!!.scrollTo(0, offset - logInfoView!!.height)
        }
    }

    /**
     * construct OnSamplePointListener
     */
    private val onSamplePointListener = OnSamplePointListener { }
}
