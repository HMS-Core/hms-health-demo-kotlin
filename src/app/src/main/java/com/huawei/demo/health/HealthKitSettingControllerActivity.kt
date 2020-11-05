package com.huawei.demo.health

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.*
import com.huawei.hms.common.ApiException
import com.huawei.hms.hihealth.*
import com.huawei.hms.hihealth.data.DataCollector
import com.huawei.hms.hihealth.data.DataType
import com.huawei.hms.hihealth.data.Field
import com.huawei.hms.hihealth.data.SampleSet
import com.huawei.hms.hihealth.options.DataTypeAddOptions
import com.huawei.hms.hihealth.options.ReadOptions
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class HealthKitSettingControllerActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    /**
     * Custom data type read permission
     */
    val HEALTHKIT_SELF_DEFINING_DATA_READ = "https://www.huawei.com/healthkit/selfdefining.read"

    /**
     * Custom data type write permission
     */
    val HEALTHKIT_SELF_DEFINING_DATA_WRITE = "https://www.huawei.com/healthkit/selfdefining.write"

    /**
     * Custom data type read / write permission
     */
    val HEALTHKIT_SELF_DEFINING_DATA_BOTH = "https://www.huawei.com/healthkit/selfdefining.both"

    private val TAG = "SettingController"

    // Line separators for the display on the UI
    private val SPLIT = "*******************************" + System.lineSeparator()

    // The container that stores Field
    private val SPINNERLIST = ArrayList<Field>()

    // The container that stores Field name
    private val SPINNERLISTSTR = ArrayList<String>()

    // Object of DataController for fitness and health data, providing APIs for read/write, batch read/write, and listening
    private var dataController: DataController? = null

    // Object of DataController for fitness and health data, providing APIs for read/write, batch read/write, and listening
    private var settingController: SettingController? = null

    // Internal context object of the activity
    private var context: Context? = null

    // EditText for setting data type name information on the UI
    private var dataTypeNameView: EditText? = null

    // TextView for displaying operation information on the UI
    private var logInfoView: TextView? = null

    // drop-down box of Field name
    private var spinner: Spinner? = null

    // drop-down box adapter
    private var adapter: ArrayAdapter<String>? = null

    // The field value you choose, default value is Field.FIELD_BPM
    private var selectedField = Field.FIELD_STEPS

    // The field name value you choose
    private var selectedFieldStr = "FIELD_STEPS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_setting_controller)
        context = this

        initActivityView()
        initDataController()
    }

    /**
     * Implementation of OnItemSelectedListener interface.
     * Assign a value to the variable
     */
    override fun onItemSelected(arg0: AdapterView<*>, arg1: View, arg2: Int, arg3: Long) {
        if (SPINNERLIST.isNotEmpty() && arg2 < SPINNERLIST.size) {
            selectedField = SPINNERLIST[arg2]
            selectedFieldStr = SPINNERLISTSTR[arg2]
            logger("your choice is ：$selectedFieldStr")
        }
    }

    override fun onNothingSelected(arg0: AdapterView<*>) {}

    /**
     * Initialize Activity view.
     */
    @SuppressLint("SetTextI18n")
    private fun initActivityView() {
        logInfoView = findViewById(R.id.setting_controller_log_info)
        logInfoView!!.movementMethod = ScrollingMovementMethod.getInstance()

        for (field in Field::class.java.declaredFields) {
            if (field.type != Field::class.java) {
                continue
            }
            try {
                SPINNERLIST.add(field.get(Field::class.java) as Field)
                SPINNERLISTSTR.add(field.name)
            } catch (e: IllegalAccessException) {
                logger("initActivityView: catch an IllegalAccessException")
            }

        }

        SPINNERLISTSTR.size
        val array = SPINNERLISTSTR.toTypedArray()

        spinner = findViewById(R.id.spinner01)
        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, array)
        spinner!!.adapter = adapter
        spinner!!.onItemSelectedListener = this
        spinner!!.setSelection(SPINNERLIST.indexOf(selectedField))

        dataTypeNameView = findViewById(R.id.data_type_name_text)
        dataTypeNameView!!.setText(this.packageName + ".anyExtendName")
        dataTypeNameView!!.isFocusable = true
        dataTypeNameView!!.requestFocus()
        dataTypeNameView!!.isFocusableInTouchMode = true
    }

    /**
     * Initialize variable of mSignInHuaweiId.
     */
    private fun initDataController() {
        // create HiHealth Options, donnot add any datatype here.
        val hiHealthOptions = HiHealthOptions.builder().build()
        // get AuthHuaweiId by HiHealth Options.
        // get DataController.
        dataController = context?.let {
            HuaweiHiHealth.getDataController(
                it,
                HuaweiIdAuthManager.getExtendedAuthResult(hiHealthOptions)
            )
        }

        settingController = context?.let {
            HuaweiHiHealth.getSettingController(
                it,
                HuaweiIdAuthManager.getExtendedAuthResult(hiHealthOptions)
            )
        }
    }

    /**
     * add new DataType.
     * you need two object to add new DataType: DataTypeAddOptions and SettingController.
     * specify the field by drop-down box, You cannot add DataType with duplicate DataType's name.
     * You can add multiple field，For simplicity, only one field is added here.
     *
     * @param view (indicating a UI object)
     */
    fun addNewDataType(view: View) {
        // get DataType name from EditText view,
        // The name must start with package name, and End with a custom name.
        val dataTypeName = dataTypeNameView!!.text.toString()
        // create DataTypeAddOptions,You must specify the Field that you want to add,
        // You can add multiple Fields here.
        val dataTypeAddOptions =
            DataTypeAddOptions.Builder().setName(dataTypeName).addField(selectedField).build()

        // create SettingController and add new DataType
        // The added results are displayed in the phone screen
        context?.let {
            settingController!!.addDataType(dataTypeAddOptions)
                .addOnFailureListener { e -> printFailureMessage(e, "addNewDataType") }
                .addOnCompleteListener { task ->
                    val res = if (task.isSuccessful) "success" else "failed"
                    logger("add dataType of $selectedFieldStr is $res")
                    if (task.exception != null) {
                        logger("getException is " + task.exception.toString())
                    }
                }
        }
    }

    /**
     * read DataType.
     * Get DataType with the specified name
     *
     * @param view (indicating a UI object)
     */
    fun readDataType(view: View) {
        // data type name
        val dataTypeName = dataTypeNameView!!.text.toString()

        // create SettingController and get DataType with the specified name
        // The results are displayed in the phone screen
        settingController!!.readDataType(dataTypeName)
            .addOnFailureListener { e -> printFailureMessage(e, "readDataType") }
            .addOnCompleteListener {task ->
                if (task.isSuccessful()) {
                    logger("DataType : " + task.getResult())
                }
            }
    }

    /**
     * disable HiHealth.
     * After calling this function, HiHealth will cancel All your Records.
     *
     * @param view (indicating a UI object)
     */
    fun disableHiHealth(view: View) {
        // create SettingController and disable HiHealth (cancel All your Records).
        // The results are displayed in the phone screen.
        settingController!!.disableHiHealth()
            .addOnFailureListener { e -> printFailureMessage(e, "disableHiHealth") }
            .addOnCompleteListener { task ->
                val res = if (task.isSuccessful) "success" else "failed"
                logger("disableHiHealth is $res")
            }
    }

    /**
     * check whether the Huawei Health app authorise access to HealthKit.
     * After calling this function, if you do not authorise, we will pop the windows to Health app authorization windows.
     *
     * @param view (indicating a UI object)
     */
    fun checkAuthorization(view: View) {
        // check whether the Huawei Health app authorise access to HealthKit.
        // if you do not authorise, we will pop the windows to Health app authorization windows.
        settingController!!.checkHealthAppAuthorisation()
            .addOnFailureListener { e -> printFailureMessage(e, "readTodaySummationFromDevice") }
            .addOnCompleteListener { task ->
                val res = if (task.isSuccessful) "success" else "failed"
                logger("checkAuthorization is $res")
            }
    }

    /**
     * get whether the Huawei Health app authorise access to HealthKit.
     * After calling this function, return true means authorised, false means not authorised.
     *
     * @param view (indicating a UI object)
     */
    fun getAuthorization(view: View) {
        // get whether the Huawei Health app authorise access to HealthKit.
        // After calling this function, return true means authorised, false means not authorised.
        settingController!!.getHealthAppAuthorisation()
            .addOnFailureListener { e -> printFailureMessage(e, "getHealthAppAuthorisation") }
            .addOnCompleteListener { task ->
                val res = if (task.isSuccessful) "success" else "failed"
                logger("getHealthAppAuthorisation is $res")
            }
    }

    /**
     * Use the data controller to add a sampling dataset.
     *
     * @param view (indicating a UI object)
     * @throws ParseException (indicating a failure to parse the time string)
     */
    @Throws(ParseException::class)
    fun insertSelfData(view: View) {
        // 0. create new DataType.
        val fieldsList = ArrayList<Field>()
        fieldsList.add(selectedField)
        val selfDataType = DataType(
            dataTypeNameView!!.text.toString(), HEALTHKIT_SELF_DEFINING_DATA_READ,
            HEALTHKIT_SELF_DEFINING_DATA_WRITE, HEALTHKIT_SELF_DEFINING_DATA_BOTH, fieldsList
        )

        // 1. Build a DataCollector object.
        val dataCollector = DataCollector.Builder().setPackageName(context)
            .setDataType(selfDataType)
            .setDataStreamName(selectedFieldStr)
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .build()

        // 2. Create a sampling dataset set based on the data collector.
        val sampleSet = SampleSet.create(dataCollector)

        // 3. Build the start time, end time, and incremental step count for a DT_CONTINUOUS_STEPS_DELTA sampling point.
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.SIMPLIFIED_CHINESE)
        val startDate = dateFormat.parse("2020-03-17 09:00:00")
        val endDate = dateFormat.parse("2020-03-17 09:05:00")
        val intValue = 1000
        val floatValue = 10.0f
        val strValue = "hello"

        // 4. Build a DT_CONTINUOUS_STEPS_DELTA sampling point.
        val samplePoint = sampleSet.createSamplePoint()
            .setTimeInterval(startDate.time, endDate.time, TimeUnit.MILLISECONDS)
        try {
            selectedField.format

            when (selectedField.format) {
                Field.FORMAT_INT32 -> samplePoint.getFieldValue(selectedField).setIntValue(intValue)
                Field.FORMAT_FLOAT -> samplePoint.getFieldValue(selectedField).setFloatValue(
                    floatValue
                )
                Field.FORMAT_STRING -> samplePoint.getFieldValue(selectedField).setStringValue(
                    strValue
                )
                Field.FORMAT_MAP -> samplePoint.getFieldValue(selectedField).setKeyValue(
                    "hello",
                    100.0f
                )
                else -> logger("ERROR : Field format does not match any of the specified Format")
            }
        } catch (e: Exception) {
            logger("ERROR : The Field you selected does not support specified value")
            return
        }

        // 5. Save a DT_CONTINUOUS_STEPS_DELTA sampling point to the sampling dataset.
        // You can repeat steps 3 through 5 to add more sampling points to the sampling dataset.
        sampleSet.addSample(samplePoint)

        // 6. Call the data controller to insert the sampling dataset into the Health platform.
        val insertTask = dataController!!.insert(sampleSet)

        // 7. Calling the data controller to insert the sampling dataset is an asynchronous operation.
        // Therefore, a listener needs to be registered to monitor whether the data insertion is successful or not.
        insertTask.addOnSuccessListener {
            logger("Success insert an SampleSet into HMS core")
            showSampleSet(sampleSet)
            logger(SPLIT)
        }.addOnFailureListener { e -> printFailureMessage(e, "insert") }
    }

    /**
     * Use the data controller to query the sampling dataset by specific criteria.
     *
     * @param view (indicating a UI object)
     * @throws ParseException (indicating a failure to parse the time string)
     */
    @Throws(ParseException::class)
    fun readSelfData(view: View) {
        // 0. create new DataType.
        val fieldsList = ArrayList<Field>()
        fieldsList.add(selectedField)
        val selfDataType = DataType(
            dataTypeNameView!!.text.toString(), HEALTHKIT_SELF_DEFINING_DATA_READ,
            HEALTHKIT_SELF_DEFINING_DATA_WRITE, HEALTHKIT_SELF_DEFINING_DATA_BOTH, fieldsList
        )

        // 1. Build the condition for data query: a DataCollector object.
        val dataCollector = DataCollector.Builder().setPackageName(context)
            .setDataType(selfDataType)
            .setDataStreamName(selectedFieldStr)
            .setDataGenerateType(DataCollector.DATA_TYPE_RAW)
            .build()

        // 2. Build the time range for the query: start time and end time.
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.SIMPLIFIED_CHINESE)
        val startDate = dateFormat.parse("2020-03-17 09:00:00")
        val endDate = dateFormat.parse("2020-03-17 09:05:00")

        // 3. Build the condition-based query objec
        val readOptions = ReadOptions.Builder().read(dataCollector)
            .setTimeRange(startDate.time, endDate.time, TimeUnit.MILLISECONDS)
            .build()

        // 4. Use the specified condition query object to call the data controller to query the sampling dataset.
        val readReplyTask = dataController!!.read(readOptions)

        // 5. Calling the data controller to query the sampling dataset is an asynchronous operation.
        // Therefore, a listener needs to be registered to monitor whether the data query is successful or not.
        readReplyTask.addOnSuccessListener { readReply ->
            logger("Success read an SampleSets from HMS core")
            for (sampleSet in readReply.sampleSets) {
                showSampleSet(sampleSet)
            }
            logger(SPLIT)
        }.addOnFailureListener { e -> printFailureMessage(e, "read") }
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

        if (e is ApiException) run {
            val eCode = (e as ApiException).getStatusCode()
            val errorMsg = HiHealthStatusCodes.getStatusCodeMessage(eCode)
            logger("$api failure $eCode:$errorMsg")
            return
        } else if (isNum.matches()) {
            val errorMsg = HiHealthStatusCodes.getStatusCodeMessage(Integer.parseInt(errorCode.toCharArray().toString()))
            logger("$api failure $errorCode:$errorMsg")
            return
        } else {
            logger("$api failure $errorCode")
        }
        logger(SPLIT)
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
     * TextView to send the operation result logs to the logcat and to the UI
     *
     * @param string (indicating the log string)
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
