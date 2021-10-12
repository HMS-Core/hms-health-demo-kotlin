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

import java.text.DateFormat
import java.util.Arrays
import java.util.Calendar
import java.util.Date
import java.util.HashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity

import com.huawei.hms.hihealth.ActivityRecordsController
import com.huawei.hms.hihealth.DataController
import com.huawei.hms.hihealth.HiHealthActivities
import com.huawei.hms.hihealth.HuaweiHiHealth
import com.huawei.hms.hihealth.HiHealthStatusCodes
import com.huawei.hms.hihealth.data.ActivityRecord
import com.huawei.hms.hihealth.data.ActivitySummary
import com.huawei.hms.hihealth.data.DataCollector
import com.huawei.hms.hihealth.data.DataType
import com.huawei.hms.hihealth.data.Field
import com.huawei.hms.hihealth.data.PaceSummary
import com.huawei.hms.hihealth.data.SamplePoint
import com.huawei.hms.hihealth.data.SampleSet
import com.huawei.hms.hihealth.options.ActivityRecordInsertOptions
import com.huawei.hms.hihealth.options.ActivityRecordReadOptions
import com.huawei.hms.hihealth.options.DeleteOptions

class HealthKitActivityRecordControllerActivity : AppCompatActivity() {
    private val TAG = "ActivityRecordSample"

    // Line separators for the display on the UI
    private val SPLIT = "*******************************" + System.lineSeparator()

    // Internal context object
    private var context: Context? = null

    // ActivityRecordsController for managing activity records
    private var activityRecordsController: ActivityRecordsController? = null

    // DataController for deleting activity records
    private var dataController: DataController? = null

    // Text view for displaying operation information on the UI
    private var logInfoView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        setContentView(R.layout.activity_health_activityrecord)
        init()
    }

    /**
     * Initialization
     */
    private fun init() {
        dataController = context?.let { HuaweiHiHealth.getDataController(it) }
        activityRecordsController =
            context?.let { HuaweiHiHealth.getActivityRecordsController(it) }
        logInfoView = findViewById(R.id.activity_records_controller_log_info)
        logInfoView!!.movementMethod = ScrollingMovementMethod.getInstance()
    }

    /**
     * Start an activity record
     *
     * @param view indicating a UI object
     */
    open fun beginActivityRecord(view: View?): Unit {
        logger(SPLIT + "this is MyActivityRecord Begin")
        val startTime = Calendar.getInstance().timeInMillis
        val activitySummary: ActivitySummary? = getActivitySummary()

        // Build an ActivityRecord object
        val activityRecord =
            ActivityRecord.Builder().setId("MyBeginActivityRecordId")
                .setName("BeginActivityRecord")
                .setDesc("This is ActivityRecord begin test!")
                .setActivityTypeId(HiHealthActivities.RUNNING)
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .setActivitySummary(activitySummary)
                .setTimeZone("+0800")
                .build()
        checkConnect()

        // Add a listener for the ActivityRecord start success
        val beginTask =
            activityRecordsController!!.beginActivityRecord(activityRecord)

        // Add a listener for the ActivityRecord start failure
        beginTask.addOnSuccessListener { logger("MyActivityRecord begin success") }
            .addOnFailureListener { e -> printFailureMessage(e, "beginActivityRecord") }
    }

    private fun getActivitySummary(): ActivitySummary? {
        val activitySummary = ActivitySummary()
        val paceSummary = PaceSummary()
        paceSummary.avgPace = 247.27626
        paceSummary.bestPace = 212.0
        val partTimeMap: MutableMap<String, Double> =
            HashMap()
        partTimeMap["1.0"] = 456.0
        paceSummary.partTimeMap = partTimeMap
        val paceMap: MutableMap<String, Double> =
            HashMap()
        paceMap["1.0"] = 263.0
        paceSummary.paceMap = paceMap
        val sportHealthPaceMap: MutableMap<String, Double> =
            HashMap()
        sportHealthPaceMap["102802480"] = 535.0
        paceSummary.sportHealthPaceMap = sportHealthPaceMap
        activitySummary.paceSummary = paceSummary
        return activitySummary
    }

    /**
     * Stop an activity record
     *
     * @param view indicating a UI object
     */
    fun endActivityRecord(view: View) {
        logger(SPLIT + "this is MyActivityRecord End")

        // Call the related method of ActivityRecordsController to stop activity records.
        // The input parameter can be the ID string of ActivityRecord or null
        // Stop an activity record of the current app by specifying the ID string as the input parameter
        // Stop activity records of the current app by specifying null as the input parameter
        val endTask = activityRecordsController!!.endActivityRecord("MyBeginActivityRecordId")
        endTask.addOnSuccessListener { activityRecords ->
            logger("MyActivityRecord End success")
            // Return the list of activity records that have stopped
            if (activityRecords.size > 0) {
                for (activityRecord in activityRecords) {
                    dumpActivityRecord(activityRecord)
                }
            } else {
                // Null will be returnded if none of the activity records has stopped
                logger("MyActivityRecord End response is null")
            }
        }.addOnFailureListener { e -> printFailureMessage(e, "endActivityRecord") }
    }

    /**
     * Add an activity record to the Health platform
     *
     * @param view indicating a UI object
     */
    fun addActivityRecord(view: View) {
        logger(SPLIT + "this is MyActivityRecord Add")

        // Build the time range of the request object: start time and end time
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        val endTime = cal.timeInMillis
        cal.add(Calendar.HOUR_OF_DAY, -1)
        val startTime = cal.timeInMillis

        val activitySummary = ActivitySummary()
        val paceSummary = PaceSummary()
        paceSummary.avgPace = 247.27626
        paceSummary.bestPace = 212.0
        val partTimeMap: MutableMap<String, Double> =
            HashMap()
        partTimeMap["1.0"] = 456.0
        paceSummary.partTimeMap = partTimeMap
        val paceMap: MutableMap<String, Double> =
            HashMap()
        paceMap["1.0"] = 263.0
        paceSummary.paceMap = paceMap
        val sportHealthPaceMap: MutableMap<String, Double> =
            HashMap()
        sportHealthPaceMap["102802480"] = 535.0
        paceSummary.sportHealthPaceMap = sportHealthPaceMap
        activitySummary.setPaceSummary(paceSummary)


        // 创建一个总步数统计的数据采集器
        // ActivitySummary 用来承载统计数据
        val dataCollector2: DataCollector = DataCollector.Builder()
            .setDataType(DataType.DT_CONTINUOUS_STEPS_TOTAL)
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .setPackageName(applicationContext)
            .setDataCollectorName("test1")
            .build()
        val samplePoint1 = SamplePoint.Builder(dataCollector2).build()
            .setTimeInterval(startTime + 1L, startTime + 300000L, TimeUnit.MILLISECONDS);
        samplePoint1.getFieldValue(Field.FIELD_STEPS).setIntValue(1024)
        activitySummary.dataSummary = Arrays.asList(samplePoint1)

        // Build the activity record request object
        val activityRecord = ActivityRecord.Builder().setName("AddActivityRecord")
            .setDesc("This is ActivityRecord add test!")
            .setId("MyAddActivityRecordId")
            .setActivityTypeId(HiHealthActivities.RUNNING)
            .setStartTime(startTime, TimeUnit.MILLISECONDS)
            .setEndTime(endTime, TimeUnit.MILLISECONDS)
            .setActivitySummary(activitySummary)
            .setTimeZone("+0800")
            .build()

        // Build the dataCollector object
        val dataCollector = DataCollector.Builder().setDataType(DataType.DT_CONTINUOUS_STEPS_DELTA)
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .setPackageName(context)
            .setDataCollectorName("AddActivityRecord")
            .build()

        // Build the sampling sampleSet based on the dataCollector
        val sampleSet = SampleSet.create(dataCollector)

        // Build the (DT_CONTINUOUS_STEPS_DELTA) sampling data object and add it to the sampling dataSet
        val samplePoint =
            sampleSet.createSamplePoint().setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
        samplePoint.getFieldValue(Field.FIELD_STEPS_DELTA).setIntValue(1024)
        sampleSet.addSample(samplePoint)

        // Build the activity record addition request object
        val insertRequest = ActivityRecordInsertOptions.Builder().setActivityRecord(activityRecord)
            .addSampleSet(sampleSet).build()

        checkConnect()

        // Call the related method in the ActivityRecordsController to add activity records
        val addTask = activityRecordsController!!.addActivityRecord(insertRequest)
        addTask.addOnSuccessListener { logger("ActivityRecord add was successful!") }
            .addOnFailureListener { e -> printFailureMessage(e, "addActivityRecord") }
    }

    /**
     * Read historical activity records
     *
     * @param view indicating a UI object
     */
    fun getActivityRecord(view: View) {
        logger(SPLIT + "this is MyActivityRecord Get")

        // Build the time range of the request object: start time and end time
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        val endTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = cal.timeInMillis

        // Build the request body for reading activity records
        val readRequest = ActivityRecordReadOptions.Builder()
            .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
            .readActivityRecordsFromAllApps()
            .read(DataType.DT_CONTINUOUS_STEPS_DELTA)
            .build()

        checkConnect()

        // Call the read method of the ActivityRecordsController to obtain activity records
        // from the Health platform based on the conditions in the request body
        val getTask = activityRecordsController!!.getActivityRecord(readRequest)
        getTask.addOnSuccessListener { activityRecordReply ->
            logger("Get ActivityRecord was successful!")
            // Print ActivityRecord and corresponding activity data in the result
            val activityRecordList = activityRecordReply.activityRecords
            for (activityRecord in activityRecordList) {
                if (activityRecord == null) {
                    continue;
                }
                dumpActivityRecord(activityRecord)
                if (activityRecord.getActivitySummary() != null) {
                    printActivitySummary(activityRecord.getActivitySummary());
                }
                for (sampleSet in activityRecordReply.getSampleSet(activityRecord)) {
                    dumpSampleSet(sampleSet)
                }
            }
        }.addOnFailureListener { e -> printFailureMessage(e, "getActivityRecord") }
    }

    fun printActivitySummary(activitySummary: ActivitySummary) {
        val dataSummary = activitySummary.dataSummary
        Log.i(TAG, "\n打印统计数据: ")
        Log.i(TAG, "\nActivitySummary\n\t DataSummary: ")
        for (samplePoint in dataSummary) {
            Log.i(
                TAG,
                """
	 samplePoint: 
	 DataCollector${samplePoint.dataCollector}
	 DataType${samplePoint.dataType}
	 StartTime${samplePoint.getStartTime(TimeUnit.MILLISECONDS)}
	 EndTime""" + samplePoint.getEndTime(
                    TimeUnit.MILLISECONDS
                )
                        + "\n\t SamplingTime" + samplePoint.getSamplingTime(TimeUnit.MILLISECONDS) + "\n\t FieldValues" + samplePoint.fieldValues
            )
        }
        // 以下打印配速信息
        val paceSummary = activitySummary.paceSummary
        Log.i(
            TAG,
            """
	 PaceSummary: 
	 AvgPace${paceSummary.avgPace}
	 BestPace${paceSummary.bestPace}
	 PaceMap${paceSummary.paceMap}
	 PartTimeMap${paceSummary.partTimeMap}
	 SportHealthPaceMap${paceSummary.sportHealthPaceMap}"""
        )
    }

    /**
     * Delete activity record
     *
     * @param view indicating a UI object
     */
    fun deleteActivityRecord(view: View) {
        logger(SPLIT + "this is MyActivityRecord Delete")

        // Build the time range of the request object: start time and end time
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        val endTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -2)
        val startTime = cal.timeInMillis

        // Build the request body for reading activity records
        val readRequest = ActivityRecordReadOptions.Builder()
            .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
            .read(DataType.DT_CONTINUOUS_STEPS_DELTA)
            .build()

        // Call the read method of the ActivityRecordsController to obtain activity records
        // from the Health platform based on the conditions in the request body
        val getTask = activityRecordsController!!.getActivityRecord(readRequest)
        getTask.addOnSuccessListener { activityRecordReply ->
            Log.i(TAG, "Reading ActivityRecord  response status " + activityRecordReply.status)
            val activityRecords = activityRecordReply.activityRecords

            // Get ActivityRecord and corresponding activity data in the result
            for (activityRecord in activityRecords) {
                val deleteOptions = DeleteOptions.Builder().addActivityRecord(activityRecord)
                    .setTimeInterval(
                        activityRecord.getStartTime(TimeUnit.MILLISECONDS),
                        activityRecord.getEndTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS
                    )
                    .build()
                logger("begin delete ActivitiRecord is :" + activityRecord.id)

                // Delete ActivityRecord
                val deleteTask = dataController!!.delete(deleteOptions)
                deleteTask.addOnSuccessListener { logger("delete ActivitiRecord is Success:" + activityRecord.id) }
                    .addOnFailureListener { e -> printFailureMessage(e, "delete") }
            }
        }.addOnFailureListener { e -> printFailureMessage(e, "delete") }
    }

    /**
     * Print the SamplePoint in the SampleSet object as an output.
     *
     * @param sampleSet indicating the sampling dataset)
     */
    private fun dumpSampleSet(sampleSet: SampleSet) {
        logger("Returned for SamplePoint and Data type: " + sampleSet.dataType.name)
        for (dp in sampleSet.samplePoints) {
            val dateFormat = DateFormat.getDateInstance()
            logger("SamplePoint:")
            logger("DataCollector:" + dp.dataCollector.toString())
            logger("\tType: " + dp.dataType.name)
            logger("\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)))
            logger("\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)))
            for (field in dp.dataType.fields) {
                logger("\tField: " + field.toString() + " Value: " + dp.getFieldValue(field))
            }
        }
    }

    /**
     * Print the ActivityRecord object as an output.
     *
     * @param activityRecord indicating an activity record
     */
    private fun dumpActivityRecord(activityRecord: ActivityRecord) {
        val dateFormat = DateFormat.getDateInstance()
        val timeFormat = DateFormat.getTimeInstance()
        logger(
            ("Returned for ActivityRecord: " + activityRecord.name + "\n\tActivityRecord Identifier is "
                    + activityRecord.id + "\n\tActivityRecord created by app is " + activityRecord.packageName
                    + "\n\tDescription: " + activityRecord.desc + "\n\tStart: "
                    + dateFormat.format(activityRecord.getStartTime(TimeUnit.MILLISECONDS)) + " "
                    + timeFormat.format(activityRecord.getStartTime(TimeUnit.MILLISECONDS)) + "\n\tEnd: "
                    + dateFormat.format(activityRecord.getEndTime(TimeUnit.MILLISECONDS)) + " "
                    + timeFormat.format(activityRecord.getEndTime(TimeUnit.MILLISECONDS)) + "\n\tActivity:"
                    + activityRecord.activityType)
        )
    }

    /**
     * Check the object connection
     */
    private fun checkConnect() {
        if (activityRecordsController == null) {
            activityRecordsController = HuaweiHiHealth.getActivityRecordsController(this)
        }
    }

    /**
     * Print error code and error information for an exception.
     *
     * @param exception indicating an exception object
     * @param api api name
     */
    private fun printFailureMessage(exception: Exception, api: String) {
        val errorCode = exception.message
        val pattern = Pattern.compile("[0-9]*")
        val isNum = pattern.matcher(errorCode!!.toCharArray().toString())
        if (isNum.matches()) {
            val errorMsg =
                HiHealthStatusCodes.getStatusCodeMessage(
                    Integer.parseInt(
                        errorCode.toCharArray().toString()
                    )
                )
            logger("$api failure $errorCode:$errorMsg")
        } else {
            logger("$api failure $errorCode")
        }
        logger(SPLIT)
    }

    /**
     * Send the operation result logs to the logcat and TextView control on the UI
     *
     * @param string indicating the log string
     */
    private fun logger(string: String) {
        Log.i(TAG, string)
        logInfoView!!.append(string + System.lineSeparator())
        val offset = logInfoView!!.lineCount * logInfoView!!.lineHeight
        if (offset > logInfoView!!.height) {
            logInfoView!!.scrollTo(0, offset - logInfoView!!.height)
        }
    }
}
