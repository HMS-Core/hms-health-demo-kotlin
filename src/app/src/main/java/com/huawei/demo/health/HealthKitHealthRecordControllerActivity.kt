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
import java.text.DateFormat.getTimeInstance
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

import android.content.Context
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity

import com.huawei.hms.hihealth.HealthRecordController
import com.huawei.hms.hihealth.HuaweiHiHealth
import com.huawei.hms.hihealth.HiHealthStatusCodes
import com.huawei.hms.hihealth.data.DataCollector
import com.huawei.hms.hihealth.data.DataType
import com.huawei.hms.hihealth.data.Field
import com.huawei.hms.hihealth.data.HealthDataTypes
import com.huawei.hms.hihealth.data.HealthFields
import com.huawei.hms.hihealth.data.HealthRecord
import com.huawei.hms.hihealth.data.SamplePoint
import com.huawei.hms.hihealth.data.SampleSet
import com.huawei.hms.hihealth.options.HealthRecordInsertOptions
import com.huawei.hms.hihealth.options.HealthRecordReadOptions
import com.huawei.hms.hihealth.options.HealthRecordUpdateOptions

/**
 * 功能描述
 *
 * @since 2021-06-11
 */

class HealthKitHealthRecordControllerActivity : AppCompatActivity() {
    private val TAG = "HealthRecordSample";

    // Line separators for the display on the UI
    private val SPLIT = "*******************************" + System.lineSeparator()

    // Internal context object
    private var context: Context? = null

    // HealthRecordController for managing healthRecord records
    private var healthRecordController: HealthRecordController? = null

    // Text view for displaying operation information on the UI
    private var logInfoView : TextView? = null;

    private var healthRecordIdFromInsertResult: String? = "defaultValueId"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_healthrecord)
        context = this
        init()
    }

    private fun init() {
        healthRecordController = context?.let { HuaweiHiHealth.getHealthRecordController(it) }
        logInfoView = findViewById<View>(R.id.activity_records_controller_log_info) as TextView
        logInfoView!!.movementMethod = ScrollingMovementMethod.getInstance()
    }

    /**
     * Add an health record to the Health platform
     *
     * @param view indicating a UI object
     */
    fun addHealthRecord(view: View) {
        logger(SPLIT + "this is MyHealthRecord Add")
        // Build the time range of the request object: start time and end time
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        val endTime = cal.timeInMillis
        cal.add(Calendar.HOUR_OF_DAY, -1)
        val startTime = cal.timeInMillis

        // 1.Create a collector that carries the heart rate detail data type and a sampleSetList that stores the detail data.
        val dataCollector =
            DataCollector.Builder().setDataType(DataType.DT_INSTANTANEOUS_HEART_RATE)
                .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
                .setPackageName(context)
                .setDataStreamName("such as step count")
                .build()
        val sampleSet = SampleSet.create(dataCollector)
        // The preset time span is 5 minutes, and the heart rate detail point is set to 88.
        val samplePoint =
            sampleSet.createSamplePoint().setTimeInterval(startTime + 300000L, startTime + 300000L, TimeUnit.MILLISECONDS)
        samplePoint.getFieldValue(Field.FIELD_BPM).setDoubleValue(88.0)
        sampleSet.addSample(samplePoint)
        // sampleSetList is used to store health details.
        val sampleSetList = ArrayList<SampleSet>()
        sampleSetList.add(sampleSet)

        // 2.Create a collector and statistics point to carry the heart rate statistics data type.
        val dataCollector1 = DataCollector.Builder()
            .setDataType(DataType.POLYMERIZE_CONTINUOUS_HEART_RATE_STATISTICS)
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .setPackageName(context)
            .setDataStreamName("such as step count")
            .build()
        // samplePointList is used to store statistics points.
        val samplePointList = ArrayList<SamplePoint>()
        // Constructing Heart Rate Statistics Points
        val samplePoint1 = SamplePoint.Builder(dataCollector1).build()
        samplePoint1.getFieldValue(Field.FIELD_AVG).setDoubleValue(90.0)
        samplePoint1.getFieldValue(Field.FIELD_MAX).setDoubleValue(100.0)
        samplePoint1.getFieldValue(Field.FIELD_MIN).setDoubleValue(80.0)
        samplePoint1.setTimeInterval(startTime + 1L, startTime + 300000L, TimeUnit.MILLISECONDS)
        samplePointList.add(samplePoint1)

        // 3.Construct a health record collector (using the bradycardia health data type as an example) and construct a health record structure.
        val dataCollector2 = DataCollector.Builder()
            .setDataType(HealthDataTypes.DT_HEALTH_RECORD_BRADYCARDIA)
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .setPackageName(context)
            .setDataStreamName("such as step count")
            .build()

        val healthRecordBuilder = HealthRecord.Builder(dataCollector2).setSubDataSummary(samplePointList)
            .setSubDataDetails(sampleSetList)
            .setStartTime(startTime, TimeUnit.MILLISECONDS)
            .setEndTime(endTime, TimeUnit.MILLISECONDS)
        // Set a value for each field of the bradycardia health data type.
        healthRecordBuilder.setFieldValue(HealthFields.FIELD_THRESHOLD, 40.0)
        healthRecordBuilder.setFieldValue(HealthFields.FIELD_AVG_HEART_RATE, 44.0)
        healthRecordBuilder.setFieldValue(HealthFields.FIELD_MAX_HEART_RATE, 48.0)
        healthRecordBuilder.setFieldValue(HealthFields.FIELD_MIN_HEART_RATE, 40.0)
        val healthRecord = healthRecordBuilder.build()

        val insertOptions = HealthRecordInsertOptions.Builder().setHealthRecord(healthRecord).build()

        healthRecordController!!.addHealthRecord(insertOptions).addOnSuccessListener { healthRecordId ->
            // Save the healthRecordId returned after the insertion is successful.
            // The healthRecordId is used to update the scenario.
            healthRecordIdFromInsertResult = healthRecordId
            logger("health record add was successful,please save the healthRecordId:\n$healthRecordId")
        }.addOnFailureListener { e -> printFailureMessage(e, "getHealthRecord") }
    }

    /**
     * updates health records of a specified HealthRecordID
     *
     * @param view indicating a UI object
     */
    fun updateHealthRecord(view: View) {
        logger(SPLIT + "this is MyHealthRecord Update")
        // Build the time range of the request object: start time and end time
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        val endTime = cal.timeInMillis
        cal.add(Calendar.HOUR_OF_DAY, -1)
        val startTime = cal.timeInMillis

        // 1.Create a collector that carries the heart rate detail data type and a sampleSetList that stores the detail data.
        val dataCollector =
            DataCollector.Builder().setDataType(DataType.DT_INSTANTANEOUS_HEART_RATE)
                .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
                .setPackageName(context)
                .setDataStreamName("such as step count")
                .build()
        val sampleSet = SampleSet.create(dataCollector)
        // The preset time span is 5 minutes, and the heart rate detail point is set to 90.
        val samplePoint =
            sampleSet.createSamplePoint().setTimeInterval(startTime + 300000L, startTime + 300000L, TimeUnit.MILLISECONDS)
        samplePoint.getFieldValue(Field.FIELD_BPM).setDoubleValue(90.0)
        sampleSet.addSample(samplePoint)
        // sampleSetList is used to store health details.
        val sampleSetList = ArrayList<SampleSet>()
        sampleSetList.add(sampleSet)

        // 2.Create a collector and statistics point to carry the heart rate statistics data type.
        val dataCollector1 = DataCollector.Builder()
            .setDataType(DataType.POLYMERIZE_CONTINUOUS_HEART_RATE_STATISTICS)
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .setPackageName(context)
            .setDataStreamName("such as step count")
            .build()
        // samplePointList is used to store statistics points.
        val samplePointList = ArrayList<SamplePoint>()
        // Constructing Heart Rate Statistics Points
        val samplePoint1 = SamplePoint.Builder(dataCollector1).build()
        samplePoint1.getFieldValue(Field.FIELD_AVG).setDoubleValue(90.0)
        samplePoint1.getFieldValue(Field.FIELD_MAX).setDoubleValue(100.0)
        samplePoint1.getFieldValue(Field.FIELD_MIN).setDoubleValue(80.0)
        samplePoint1.setTimeInterval(startTime + 1L, startTime + 300000L, TimeUnit.MILLISECONDS)
        samplePointList.add(samplePoint1)

        // 3.Construct a health record collector (using the bradycardia health data type as an example) and construct a health record structure.
        val dataCollector2 = DataCollector.Builder()
            .setDataType(HealthDataTypes.DT_HEALTH_RECORD_BRADYCARDIA)
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .setPackageName(context)
            .setDataStreamName("such as step count")
            .build()

        val healthRecordBuilder = HealthRecord.Builder(dataCollector2).setSubDataSummary(samplePointList)
            .setSubDataDetails(sampleSetList)
            .setStartTime(startTime, TimeUnit.MILLISECONDS)
            .setEndTime(endTime, TimeUnit.MILLISECONDS)
        // Set a value for each field of the bradycardia health data type.
        healthRecordBuilder.setFieldValue(HealthFields.FIELD_THRESHOLD, 42.0)
        healthRecordBuilder.setFieldValue(HealthFields.FIELD_AVG_HEART_RATE, 45.0)
        healthRecordBuilder.setFieldValue(HealthFields.FIELD_MAX_HEART_RATE, 48.0)
        healthRecordBuilder.setFieldValue(HealthFields.FIELD_MIN_HEART_RATE, 42.0)
        val healthRecord = healthRecordBuilder.build()

        // 4.Construct the updateOptions to be updated and carry the healthRecordId returned after the insertion is successful
        val updateOptions = HealthRecordUpdateOptions.Builder().setHealthRecord(healthRecord)
            .setHealthRecordId(healthRecordIdFromInsertResult)
            .build()

        healthRecordController!!.updateHealthRecord(updateOptions)
            .addOnSuccessListener { logger("update healthRecord success") }
            .addOnFailureListener { e -> printFailureMessage(e, "UpdateHealthRecord") }
    }

    /**
     * Read historical health records
     *
     * @param view indicating a UI object
     */
    fun getHealthRecord(view: View) {
        logger(SPLIT + "this is MyHealthRecord Get")

        // Build the time range of the request object: start time and end time
        val cal = Calendar.getInstance()
        val now = Date()
        cal.time = now
        val endTime = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = cal.timeInMillis

        // Build the request body for reading HealthRecord records
        val subDataTypeList = ArrayList<DataType>()
        subDataTypeList.add(DataType.DT_INSTANTANEOUS_HEART_RATE)
        val healthRecordReadOptions =
            HealthRecordReadOptions.Builder().setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                .readHealthRecordsFromAllApps()
                .readByDataType(HealthDataTypes.DT_HEALTH_RECORD_BRADYCARDIA)
                .setSubDataTypeList(subDataTypeList)
                .build()

        // Call the get method of the HealthRecordController to obtain health records
        // from the Health platform based on the conditions in the request body
        val task = healthRecordController!!.getHealthRecord(healthRecordReadOptions)
        task.addOnSuccessListener { readResponse ->
            logger("Get HealthRecord was successful!")
            // Print HealthRecord and corresponding health data in the result
            val recordList = readResponse.healthRecords
            for (record in recordList) {
                if (record == null) {
                    continue
                }
                dumpHealthRecord(record)
                logger("Print detailed data points associated with health records")
                for (dataSet in record.subDataDetails) {
                    dumpDataSet(dataSet)
                }
            }
        }
        task.addOnFailureListener { e -> printFailureMessage(e, "getHealthRecord") }
    }

    /**
     * Print the SamplePoint in the SampleSet object as an output.
     *
     * @param sampleSet indicating the sampling dataSet
     */
    private fun dumpDataSet(sampleSet: SampleSet) {
        logger("Returned for SamplePoint and Data type: " + sampleSet.dataType.name)
        for (dp in sampleSet.samplePoints) {
            val dateFormat = getTimeInstance()
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
     * Print the HealthRecord object as an output.
     *
     * @param healthRecord indicating a health record
     */
    private fun dumpHealthRecord(healthRecord: HealthRecord?) {
        logger("Print health record summary information!")
        val dateFormat = DateFormat.getDateInstance()
        val timeFormat = DateFormat.getTimeInstance()
        if (healthRecord != null) {
            logger(
                "\tHealthRecordIdentifier: " + healthRecord!!.healthRecordId + "\n\tpackageName: "
                        + healthRecord!!.dataCollector.packageName + "\n\tStartTime: "
                        + dateFormat.format(healthRecord!!.getStartTime(TimeUnit.MILLISECONDS)) + " "
                        + timeFormat.format(healthRecord!!.getStartTime(TimeUnit.MILLISECONDS)) + "\n\tEndTime: "
                        + dateFormat.format(healthRecord!!.getEndTime(TimeUnit.MILLISECONDS)) + " "
                        + timeFormat.format(healthRecord!!.getEndTime(TimeUnit.MILLISECONDS)) + "\n\tHealthRecordDataType: "
                        + healthRecord!!.dataCollector.dataType.name + "\n\tHealthRecordDataCollectorId: "
                        + healthRecord!!.dataCollector.dataStreamId + "\n\tmetaData: " + healthRecord!!.metadata
                        + "\n\tFileValueMap: " + healthRecord!!.fieldValues
            )

            if (healthRecord!!.subDataSummary != null && !healthRecord!!.subDataSummary.isEmpty()) {
                showSamplePoints(healthRecord!!.subDataSummary)
            }
        }
    }

    /**
     * Print the SamplePoint in the SamplePointList object as an output.
     *
     * @param subDataSummary Indicates the list of sample data.
     */
    private fun showSamplePoints(subDataSummary: List<SamplePoint>) {
        val sDateFormat: SimpleDateFormat
        sDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        for (dp in subDataSummary) {
            showSamplePoint(sDateFormat, dp)
        }
    }

    private fun showSamplePoint(dateFormat: SimpleDateFormat, samplePoint: SamplePoint) {
        logger("Sample point type: " + samplePoint.dataType.name)
        logger("Start: " + dateFormat.format(Date(samplePoint.getStartTime(TimeUnit.MILLISECONDS))))
        logger("End: " + dateFormat.format(Date(samplePoint.getEndTime(TimeUnit.MILLISECONDS))))
        for (field in samplePoint.dataType.fields) {
            logger("Field: " + field.name + " Value: " + samplePoint.getFieldValue(field))
        }
        logger(System.lineSeparator())
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

    /**
     * Send the operation exception logs to the logcat and TextView control on the UI
     *
     * @param e the exception
     * @param api the calling api
     */
    private fun printFailureMessage(e: Exception?, api: String) {
        val errorCode = e?.message
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
}