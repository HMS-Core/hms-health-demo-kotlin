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

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.Locale
import java.util.regex.Pattern

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity

import com.huawei.hmf.tasks.OnFailureListener
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.hms.hihealth.DataController
import com.huawei.hms.hihealth.HiHealthStatusCodes
import com.huawei.hms.hihealth.HuaweiHiHealth
import com.huawei.hms.hihealth.data.DataCollector
import com.huawei.hms.hihealth.data.DataType
import com.huawei.hms.hihealth.data.Field
import com.huawei.hms.hihealth.data.SamplePoint
import com.huawei.hms.hihealth.data.SampleSet
import com.huawei.hms.hihealth.options.DeleteOptions
import com.huawei.hms.hihealth.options.ReadOptions
import com.huawei.hms.hihealth.options.UpdateOptions

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class HealthKitDataControllerActivity : AppCompatActivity() {
    private val TAG = "DataController"

    // Line separators for the display on the UI
    private val SPLIT = "*******************************" + System.lineSeparator()

    // Object of controller for fitness and health data, providing APIs for read/write, batch read/write, and listening
    private var dataController: DataController? = null

    // Internal context object of the activity
    private var context: Context? = null

    // TextView for displaying operation information on the UI
    private var logInfoView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_datacontroller)
        context = this
        logInfoView = findViewById(R.id.data_controller_log_info)
        logInfoView!!.movementMethod = ScrollingMovementMethod.getInstance()
        initDataController()
    }

    /**
     * Initialize a data controller object.
     */
    private fun initDataController() {
        // obtain the data controller object.
        dataController = context!!.let { HuaweiHiHealth.getDataController(it) }
    }

    /**
     * Use the data controller to add a sampling dataset.
     *
     * @param view (indicating a UI object)
     * @throws ParseException (indicating a failure to parse the time string)
     */
    @Throws(ParseException::class)
    fun insertData(view: View) {
        // 1. Build a DataCollector object.
        val dataCollector = DataCollector.Builder().setPackageName(context)
            .setDataType(DataType.DT_CONTINUOUS_STEPS_DELTA)
            .setDataStreamName("STEPS_DELTA")
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .build()

        // 2. Create a sampling dataset set based on the data collector.
        val sampleSet = SampleSet.create(dataCollector)

        // 3. Build the start time, end time, and incremental step count for a DT_CONTINUOUS_STEPS_DELTA sampling point.
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.SIMPLIFIED_CHINESE)
        val startDate = dateFormat.parse("2020-08-24 09:00:00")
        val endDate = dateFormat.parse("2020-08-24 09:05:00")
        val stepsDelta = 200

        // 4. Build a DT_CONTINUOUS_STEPS_DELTA sampling point.
        val samplePoint = sampleSet.createSamplePoint()
            .setTimeInterval(startDate.time, endDate.time, TimeUnit.MILLISECONDS)
        samplePoint.getFieldValue(Field.FIELD_STEPS_DELTA).setIntValue(stepsDelta)
        // If the written step count data needs to be displayed on the homepage of the Huawei Health App,
        // you need to use addMetadata to add the following metadata to the sampling point
        samplePoint.addMetadata("motion_type", "RUN")

        // 5. Save a DT_CONTINUOUS_STEPS_DELTA sampling point to the sampling dataset.
        // You can repeat steps 3 through 5 to add more sampling points to the sampling dataset.
        sampleSet.addSample(samplePoint)

        // 6. Call the data controller to insert the sampling dataset into the Health platform.
        val insertTask = dataController!!.insert(sampleSet)

        // 7. Calling the data controller to insert the sampling dataset is an asynchronous operation.
        // Therefore, a listener needs to be registered to monitor whether the data insertion is successful or not.
        insertTask!!.addOnSuccessListener {
            logger("Success insert an SampleSet into HMS core")
            showSampleSet(sampleSet)
            logger(SPLIT)
        }!!.addOnFailureListener { e -> printFailureMessage(e, "insert") }
    }

    /**
     * Use the data controller to delete the sampling data by specific criteria.
     *
     * @param view (indicating a UI object)
     * @throws ParseException (indicating a failure to parse the time string)
     */
    @Throws(ParseException::class)
    fun deleteData(view: View) {
        // 1. Build the condition for data deletion: a DataCollector object.
        val dataCollector = DataCollector.Builder().setPackageName(context)
            .setDataType(DataType.DT_CONTINUOUS_STEPS_DELTA)
            .setDataStreamName("STEPS_DELTA")
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .build()

        // 2. Build the time range for the deletion: start time and end time.
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.SIMPLIFIED_CHINESE)
        val startDate = dateFormat.parse("2020-08-24 09:00:00")
        val endDate = dateFormat.parse("2020-08-24 09:05:00")

        // 3. Build a parameter object as the conditions for the deletion.
        val deleteOptions = DeleteOptions.Builder().addDataCollector(dataCollector)
            .setTimeInterval(startDate.time, endDate.time, TimeUnit.MILLISECONDS)
            .build()

        // 4. Use the specified condition deletion object to call the data controller to delete the sampling dataset.
        val deleteTask = dataController!!.delete(deleteOptions)

        // 5. Calling the data controller to delete the sampling dataset is an asynchronous operation.
        // Therefore, a listener needs to be registered to monitor whether the data deletion is successful or not.
        deleteTask!!.addOnSuccessListener {
            logger("Success delete sample data from HMS core")
            logger(SPLIT)
        }!!.addOnFailureListener { e -> printFailureMessage(e, "delete") }
    }

    /**
     * Use the data controller to modify the sampling data by specific criteria.
     *
     * @param view (indicating a UI object)
     * @throws ParseException (indicating a failure to parse the time string)
     */
    @Throws(ParseException::class)
    fun updateData(view: View) {
        // 1. Build the condition for data update: a DataCollector object.
        val dataCollector = DataCollector.Builder().setPackageName(context)
            .setDataType(DataType.DT_CONTINUOUS_STEPS_DELTA)
            .setDataStreamName("STEPS_DELTA")
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .build()

        // 2. Build the sampling dataset for the update: create a sampling dataset
        // for the update based on the data collector.
        val sampleSet = SampleSet.create(dataCollector)

        // 3. Build the start time, end time, and incremental step count for
        // a DT_CONTINUOUS_STEPS_DELTA sampling point for the update.
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.SIMPLIFIED_CHINESE)
        val startDate = dateFormat.parse("2020-08-24 09:00:00")
        val endDate = dateFormat.parse("2020-08-24 09:05:00")
        val stepsDelta = 300

        // 4. Build a DT_CONTINUOUS_STEPS_DELTA sampling point for the update.
        val samplePoint = sampleSet.createSamplePoint()
            .setTimeInterval(startDate.time, endDate.time, TimeUnit.MILLISECONDS)
        samplePoint.getFieldValue(Field.FIELD_STEPS_DELTA).setIntValue(stepsDelta)
        // If the updated step count data needs to be displayed on the homepage of the Huawei Health App,
        // you need to use addMetadata to add the following metadata to the sampling point
        samplePoint.addMetadata("motion_type", "RUN")

        // 5. Add an updated DT_CONTINUOUS_STEPS_DELTA sampling point to the sampling dataset for the update.
        // You can repeat steps 3 through 5 to add more updated sampling points to the sampling dataset for the update.
        sampleSet.addSample(samplePoint)

        // 6. Build a parameter object for the update.
        // Note: (1) The start time of the modified object updateOptions cannot be greater than the minimum
        // value of the start time of all sample data points in the modified data sample set
        // (2) The end time of the modified object updateOptions cannot be less than the maximum value of the
        // end time of all sample data points in the modified data sample set
        val updateOptions = UpdateOptions.Builder()
            .setTimeInterval(startDate.time, endDate.time, TimeUnit.MILLISECONDS)
            .setSampleSet(sampleSet)
            .build()

        // 7. Use the specified parameter object for the update to call the
        // data controller to modify the sampling dataset.
        val updateTask = dataController!!.update(updateOptions)

        // 8. Calling the data controller to modify the sampling dataset is an asynchronous operation.
        // Therefore, a listener needs to be registered to monitor whether the data update is successful or not.
        updateTask!!.addOnSuccessListener {
            logger("Success update sample data from HMS core")
            logger(SPLIT)
        }!!.addOnFailureListener { e -> printFailureMessage(e, "update") }
    }

    /**
     * Use the data controller to query the sampling dataset by specific criteria.
     *
     * @param view (indicating a UI object)
     * @throws ParseException (indicating a failure to parse the time string)
     */
    @Throws(ParseException::class)
    fun readData(view: View) {
        // 1. Build the time range for the query: start time and end time.
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.SIMPLIFIED_CHINESE)
        val startDate = dateFormat.parse("2020-08-24 09:00:00")
        val endDate = dateFormat.parse("2020-08-24 09:05:00")

        // 2. Build the condition-based query objec
        val readOptions = ReadOptions.Builder().read(DataType.DT_CONTINUOUS_STEPS_DELTA)
            .setTimeRange(startDate.time, endDate.time, TimeUnit.MILLISECONDS)
            .build()

        // 3. Use the specified condition query object to call the data controller to query the sampling dataset.
        val readReplyTask = dataController!!.read(readOptions)

        // 4. Calling the data controller to query the sampling dataset is an asynchronous operation.
        // Therefore, a listener needs to be registered to monitor whether the data query is successful or not.
        readReplyTask!!.addOnSuccessListener { readReply ->
            logger("Success read an SampleSets from HMS core")
            for (sampleSet in readReply.sampleSets) {
                showSampleSet(sampleSet)
            }
            logger(SPLIT)
        }!!.addOnFailureListener { e -> printFailureMessage(e, "read") }
    }

    /**
     * Use the data controller to query the summary data of the current day by data type.
     *
     * @param view (indicating a UI object)
     */
    fun readToday(view: View) {
        // 1. Use the specified data type (DT_CONTINUOUS_STEPS_DELTA) to call the data controller to query
        // the summary data of this data type of the current day.
        val todaySummationTask =
            dataController!!.readTodaySummation(DataType.DT_CONTINUOUS_STEPS_DELTA)

        // 2. Calling the data controller to query the summary data of the current day is an
        // asynchronous operation. Therefore, a listener needs to be registered to monitor whether
        // the data query is successful or not.
        // Note: In this example, the inserted data time is fixed at 2020-08-24 09:05:00.
        // When commissioning the API, you need to change the inserted data time to the current date
        // for data to be queried.
        todaySummationTask!!.addOnSuccessListener { sampleSet ->
            logger("Success read today summation from HMS core")
            if (sampleSet != null) {
                showSampleSet(sampleSet)
            }
            logger(SPLIT)
        }
        todaySummationTask.addOnFailureListener { e ->
            printFailureMessage(
                e,
                "readTodaySummation"
            )
        }
    }

    /**
     * read the latest data basing on data type
     *
     * @param view (indicating a UI object)
     */
    fun readLatestData(view: View) {
        // 1. Use the specified data type (DT_INSTANTANEOUS_HEIGHT) to call the data controller to query
        // the latest data of this data type.
        val dataTypes = ArrayList<DataType>()
        dataTypes.add(DataType.DT_INSTANTANEOUS_HEIGHT)
        val readLatestDatas = dataController!!.readLatestData(dataTypes)

        // 2. Calling the data controller to query the latest data is an asynchronous operation.
        // Therefore, a listener needs to be registered to monitor whether the data query is successful or not.
        readLatestDatas.addOnSuccessListener(OnSuccessListener<Map<DataType, SamplePoint>> { samplePointMap ->
            logger("Success read latest data from HMS core")
            if (samplePointMap != null) {
                for (dataType in dataTypes) {
                    if (samplePointMap.containsKey(dataType)) {
                        showSamplePoint(samplePointMap[dataType]!!)
                    } else {
                        logger("The DataType " + dataType.name + " has no latest data")
                    }
                }
            }
        })
        readLatestDatas.addOnFailureListener(OnFailureListener { e ->
            val errorCode = e.message
            val errorMsg = HiHealthStatusCodes.getStatusCodeMessage(Integer.parseInt(errorCode!!))
            logger("$errorCode: $errorMsg")
        })
    }


    /**
     * Use the data controller to query the summary data of the daily by data type.
     *
     * @param view (indicating a UI object)
     */
    fun readDaily(view: View) {
        // 1. Initialization start and end time, The first four digits of the shaping data represent the year,
        // the middle two digits represent the month, and the last two digits represent the day
        val endTime = 20200827
        val startTime = 20200818

        // 1. Use the specified data type (DT_CONTINUOUS_STEPS_DELTA), start and end time to call the data
        // controller to query the summary data of this data type of the daily
        val daliySummationTask =
            dataController!!.readDailySummation(DataType.DT_CONTINUOUS_STEPS_DELTA, startTime, endTime)

        // 2. Calling the data controller to query the summary data of the daily is an
        // asynchronous operation. Therefore, a listener needs to be registered to monitor whether
        // the data query is successful or not.
        // Note: In this example, the read data time is fixed at 20200827 and 20200818.
        // When commissioning the API, you need to change the read data time to the current date
        // for data to be queried.
        daliySummationTask.addOnSuccessListener { sampleSet ->
            logger("Success read daily summation from HMS core")
            if (sampleSet != null) {
                showSampleSet(sampleSet)
            }
            logger(SPLIT)
        }
        daliySummationTask.addOnFailureListener { e -> printFailureMessage(e, "readTodaySummation") }
    }

    /**
     * Clear all user data from the device and cloud.
     *
     * @param view (indicating a UI object)
     */
    fun clearCloudData(view: View) {
        // 1. Call the clearAll method of the data controller to delete data
        // inserted by the current app from the device and cloud.
        val clearTask = dataController!!.clearAll()

        // 2. Calling the data controller to clear user data from the device and cloud is an asynchronous operation.
        // Therefore, a listener needs to be registered to monitor whether the clearance is successful or not.
        clearTask!!.addOnSuccessListener {
            logger("clearAll success")
            logger(SPLIT)
        }.addOnFailureListener { e -> printFailureMessage(e, "clearAll") }
    }

    /**
     * Print the SamplePoint in the SampleSet object as an output.
     *
     * @param sampleSet (indicating the sampling dataset)
     */
    private fun showSampleSet(sampleSet: SampleSet) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.SIMPLIFIED_CHINESE)

        for (samplePoint in sampleSet.samplePoints) {
            logger("Sample point type: " + samplePoint.dataType.name)
            logger("Start: " + dateFormat.format(Date(samplePoint.getStartTime(TimeUnit.MILLISECONDS))))
            logger("End: " + dateFormat.format(Date(samplePoint.getEndTime(TimeUnit.MILLISECONDS))))
            for (field in samplePoint.dataType.fields) {
                logger("Field: " + field.name + " Value: " + samplePoint.getFieldValue(field))
            }
        }
    }

    /**
     * Print the SamplePoint as an output.
     *
     * @param samplePoint (indicating the sampling point)
     */
    private fun showSamplePoint(samplePoint: SamplePoint) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.SIMPLIFIED_CHINESE)

        logger("Sample point type: " + samplePoint.dataType.name)
        logger("Start: " + dateFormat.format(Date(samplePoint.getStartTime(TimeUnit.MILLISECONDS))))
        logger("End: " + dateFormat.format(Date(samplePoint.getEndTime(TimeUnit.MILLISECONDS))))
        for (field in samplePoint.dataType.fields) {
            logger("Field: " + field.name + " Value: " + samplePoint.getFieldValue(field))
        }
        logger(System.lineSeparator())
    }

    /**
     * Printout failure exception error code and error message
     *
     * @param e Exception object
     * @param api Interface name
     */
    private fun printFailureMessage(e: Exception, api: String) {
        val errorCode = e.message
        val pattern = Pattern.compile("[0-9]*")
        val isNum = pattern.matcher(errorCode!!.toCharArray().toString())
        if (isNum.matches()) {
            val errorMsg =
                HiHealthStatusCodes.getStatusCodeMessage(Integer.parseInt(errorCode.toCharArray().toString()))
            logger("$api failure $errorCode:$errorMsg")
        } else {
            logger("$api failure $errorCode")
        }
        logger(SPLIT)
    }

    /**
     * TextView to send the operation result logs to the logcat and to the UI
     *
     * @param string (indicating the log string)
     */
    private fun logger(string: String) {
        Log.i(TAG, string)
        logInfoView!!.append(string + System.lineSeparator())
        val offset = logInfoView!!.lineCount.times(logInfoView!!.lineHeight)
        if (offset > logInfoView!!.height) {
            logInfoView!!.scrollTo(0, offset - logInfoView!!.height)
        }
    }
}
