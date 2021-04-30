/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.huawei.demo.health.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.huawei.demo.health.R
import com.huawei.hms.common.ApiException
import com.huawei.hms.hihealth.HiHealthOptions
import com.huawei.hms.hihealth.HuaweiHiHealth
import com.huawei.hms.hihealth.SettingController
import com.huawei.hms.hihealth.data.Scopes
import com.huawei.hms.support.api.entity.auth.Scope
import com.huawei.hms.support.hwid.HuaweiIdAuthAPIManager
import com.huawei.hms.support.hwid.HuaweiIdAuthManager
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParams
import com.huawei.hms.support.hwid.request.HuaweiIdAuthParamsHelper
import java.util.ArrayList

/**
 * Check authorization result of HUAWEI Health to HUAWEI Health Kit by JAVA API
 *
 * @since 2020-09-18
 */
class HealthKitAuthClientActivity : AppCompatActivity() {

    private var mContext: Context? = null

    // HUAWEI Health kit SettingController
    private var mSettingController: SettingController? = null

    // display authorization result
    private var authDescTitle: TextView? = null

    // display authorization failure message
    private var authFailTips: TextView? = null

    // Login in to the HUAWEI ID and authorize
    private var loginAuth: Button? = null

    // confirm result
    private var confirm: Button? = null

    // retry HUAWEI health authorization
    private var authRetry: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_auth)

        initView()
        initService()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle the sign-in response.
        handleSignInResult(requestCode, data)

        // Handle the HAUWEI Health authorization Activity response.
        handleHealthAuthResult(requestCode)
    }

    private fun initView() {
        authDescTitle = findViewById(R.id.health_auth_desc_title)
        authFailTips = findViewById(R.id.health_auth_fail_tips)
        loginAuth = findViewById(R.id.health_login_auth)
        confirm = findViewById(R.id.health_auth_confirm)
        authRetry = findViewById(R.id.health_auth_retry)

        authDescTitle!!.visibility = View.GONE
        authFailTips!!.visibility = View.GONE
        confirm!!.visibility = View.GONE
        authRetry!!.visibility = View.GONE

        // listener to login HUAWEI ID and authorization
        loginAuth!!.setOnClickListener { view -> signIn() }

        // listener to retry HUAWEI Health authorization
        authRetry!!.setOnClickListener { view -> checkOrAuthorizeHealth() }

        // finish this Activity
        confirm!!.setOnClickListener { view -> this.finish() }
    }

    private fun initService() {
        mContext = this
        Log.i(TAG, "HiHealthKitClient connect to service")
        // Initialize SettingController
        val fitnessOptions = HiHealthOptions.builder().build()
        val signInHuaweiId = HuaweiIdAuthManager.getExtendedAuthResult(fitnessOptions)
        mSettingController = HuaweiHiHealth.getSettingController(mContext!!, signInHuaweiId)
    }

    /**
     * Sign-in and authorization method.
     * The authorization screen will display up if authorization has not granted by the current account.
     */
    private fun signIn() {
        Log.i(TAG, "begin sign in")
        val scopeList = ArrayList<Scope>()

        // Add scopes to apply for. The following only shows an example.
        // Developers need to add scopes according to their specific needs.

        // View and save steps in HUAWEI Health Kit.
        scopeList.add(Scope(Scopes.HEALTHKIT_STEP_READ))
        scopeList.add(Scope(Scopes.HEALTHKIT_STEP_WRITE))

        // View and save height and weight in HUAWEI Health Kit.
        scopeList.add(Scope(Scopes.HEALTHKIT_HEIGHTWEIGHT_READ))
        scopeList.add(Scope(Scopes.HEALTHKIT_HEIGHTWEIGHT_WRITE))

        // View and save the heart rate data in HUAWEI Health Kit.
        scopeList.add(Scope(Scopes.HEALTHKIT_HEARTRATE_READ))
        scopeList.add(Scope(Scopes.HEALTHKIT_HEARTRATE_WRITE))

        // View and save activityRecord in HUAWEI Health Kit.
        scopeList.add(Scope(Scopes.HEALTHKIT_ACTIVITY_RECORD_READ))
        scopeList.add(Scope(Scopes.HEALTHKIT_ACTIVITY_RECORD_WRITE))

        // View and save sleep in HUAWEI Health Kit.
        scopeList.add(Scope(Scopes.HEALTHKIT_SLEEP_READ))
        scopeList.add(Scope(Scopes.HEALTHKIT_SLEEP_WRITE))

        // Configure authorization parameters.
        val authParamsHelper = HuaweiIdAuthParamsHelper(HuaweiIdAuthParams.DEFAULT_AUTH_REQUEST_PARAM)
        val authParams = authParamsHelper.setIdToken().setAccessToken().setScopeList(scopeList).createParams()

        // Initialize the HuaweiIdAuthService object.
        val authService = HuaweiIdAuthManager.getService(this.applicationContext, authParams)

        // Silent sign-in. If authorization has been granted by the current account,
        // the authorization screen will not display. This is an asynchronous method.
        val authHuaweiIdTask = authService.silentSignIn()

        val context = this

        // Add the callback for the call result.
        authHuaweiIdTask.addOnSuccessListener { huaweiId ->
            // The silent sign-in is successful.
            Log.i(TAG, "silentSignIn success")
            Toast.makeText(context, "silentSignIn success", Toast.LENGTH_LONG).show()

            // anfter Huawei ID authorization, perform Huawei Health authorization.
            checkOrAuthorizeHealth()
        }.addOnFailureListener { exception ->
            // The silent sign-in fails.
            // This indicates that the authorization has not been granted by the current account.
            if (exception is ApiException) {
                Log.i(TAG, "sign failed status:" + exception.statusCode)
                Log.i(TAG, "begin sign in by intent")

                // Call the sign-in API using the getSignInIntent() method.
                val signInIntent = authService.signInIntent

                // Display the authorization screen by using the startActivityForResult() method of the activity.
                // Developers can change HealthKitAuthClientActivity to the actual activity.
                this@HealthKitAuthClientActivity.startActivityForResult(signInIntent, REQUEST_SIGN_IN_LOGIN)
            }
        }
    }

    /**
     * Method of handling authorization result responses
     *
     * @param requestCode (indicating the request code for displaying the authorization screen)
     * @param data (indicating the authorization result response)
     */
    private fun handleSignInResult(requestCode: Int, data: Intent?) {
        // Handle only the authorized responses
        if (requestCode != REQUEST_SIGN_IN_LOGIN) {
            return
        }

        // Obtain the authorization response from the intent.
        val result = HuaweiIdAuthAPIManager.HuaweiIdAuthAPIService.parseHuaweiIdFromIntent(data)
        if (result != null) {
            Log.d(TAG, "handleSignInResult status = " + result.status + ", result = " + result.isSuccess)
            if (result.isSuccess) {
                Log.d(TAG, "sign in is success")

                // Obtain the authorization result.
                val authResult = HuaweiIdAuthAPIManager.HuaweiIdAuthAPIService.parseHuaweiIdFromIntent(data)
                Log.d(TAG, "sign in is success authResult$authResult")

                // anfter Huawei ID authorization, perform Huawei Health authorization.
                checkOrAuthorizeHealth()
            }
        }
    }

    /**
     * Method of handling the HAUWEI Health authorization Activity response
     *
     * @param requestCode (indicating the request code for displaying the HUAWEI Health authorization screen)
     */
    private fun handleHealthAuthResult(requestCode: Int) {
        if (requestCode != REQUEST_HEALTH_AUTH) {
            return
        }

        queryHealthAuthorization()
    }

    /**
     * Check HUAWEI Health authorization status.
     * if not, start HUAWEI Health authorization Activity for user authorization.
     */
    private fun checkOrAuthorizeHealth() {
        Log.d(TAG, "begint to checkOrAuthorizeHealth")
        // 1. Build a PopupWindow as progress dialog for time-consuming operation
        val popupWindow = initPopupWindow()

        // 2. Calling SettingController to query HUAWEI Health authorization status.
        // This method is asynchronous, so need to build a listener for result.
        val authTask = mSettingController!!.healthAppAuthorisation
        authTask.addOnSuccessListener { result ->
            window.decorView.post {
                popupWindow.dismiss()

                Log.i(TAG, "checkOrAuthorizeHealth get result success")
                // If HUAWEI Health is authorized, build success View.
                if (java.lang.Boolean.TRUE == result) {
                    buildSuccessView()
                } else {
                    // If not, start HUAWEI Health authorization Activity by schema with User-defined requestCode.
                    val healthKitSchemaUri = Uri.parse(HEALTH_APP_SETTING_DATA_SHARE_HEALTHKIT_ACTIVITY_SCHEME)
                    val intent = Intent(Intent.ACTION_VIEW, healthKitSchemaUri)
                    // Before start, Determine whether the HUAWEI health authorization Activity can be opened.
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivityForResult(intent, REQUEST_HEALTH_AUTH)
                    } else {
                        buildFailView(APP_HEALTH_NOT_INSTALLED)
                    }
                }
            }
        }.addOnFailureListener { exception ->
            window.decorView.post {
                popupWindow.dismiss()

                // The method has encountered an exception. Show exception tips in the View.
                if (exception != null) {
                    Log.i(TAG, "checkOrAuthorizeHealth has exception")
                    buildFailView(exception.message)
                }
            }
        }
    }

    /**
     * Query Huawei Health authorization result.
     */
    private fun queryHealthAuthorization() {
        Log.d(TAG, "begint to queryHealthAuthorization")
        // 1. Build a PopupWindow as progress dialog for time-consuming operation
        val popupWindow = initPopupWindow()

        // 2. Calling SettingController to query HUAWEI Health authorization status.
        // This method is asynchronous, so need to build a listener for result.
        val queryTask = mSettingController!!.healthAppAuthorisation
        queryTask.addOnSuccessListener { result ->
            window.decorView.post {
                popupWindow.dismiss()

                Log.i(TAG, "queryHealthAuthorization result is" + result!!)
                // Show authorization result in view.
                if (java.lang.Boolean.TRUE == result) {
                    buildSuccessView()
                } else {
                    buildFailView(null)
                }
            }
        }.addOnFailureListener { exception ->
            window.decorView.post {
                popupWindow.dismiss()

                // The method has encountered an exception. Show exception tips in the View.
                if (exception != null) {
                    Log.i(TAG, "queryHealthAuthorization has exception")
                    buildFailView(exception.message)
                }
            }
        }
    }

    private fun buildFailView(errorMessage: String?) {
        authDescTitle!!.setText(R.string.health_auth_health_kit_fail)
        authFailTips!!.visibility = View.VISIBLE
        authRetry!!.visibility = View.VISIBLE
        confirm!!.visibility = View.GONE

        // Authentication failure message. if error message is not null, displayed based on the error code.
        if (APP_HEALTH_NOT_INSTALLED == errorMessage) {
            authFailTips!!.text = resources.getString(R.string.health_auth_health_kit_fail_tips_update)
        } else {
            authFailTips!!.text = resources.getString(R.string.health_auth_health_kit_fail_tips_connect)
        }
    }

    private fun buildSuccessView() {
        authDescTitle!!.setText(R.string.health_auth_health_kit_success)
        authRetry!!.visibility = View.GONE
        authFailTips!!.visibility = View.GONE
        confirm!!.visibility = View.VISIBLE
    }

    private fun initPopupWindow(): PopupWindow {
        val popupWindow = PopupWindow()
        popupWindow.height = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow.width = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow.isFocusable = true
        val view = LayoutInflater.from(this).inflate(R.layout.activity_waitting, null)
        popupWindow.contentView = view

        window.decorView.post {
            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
            authDescTitle!!.visibility = View.VISIBLE
            loginAuth!!.visibility = View.GONE
        }
        return popupWindow
    }

    companion object {
        private val TAG = "HealthKitAuthClient"

        /**
         * Request code for displaying the sign in authorization screen using the startActivityForResult method.
         * The value can be defined by developers.
         */
        private val REQUEST_SIGN_IN_LOGIN = 1002

        /**
         * Request code for displaying the HUAWEI Health authorization screen using the startActivityForResult method.
         * The value can be defined by developers.
         */
        private val REQUEST_HEALTH_AUTH = 1003

        /**
         * Scheme of Huawei Health Authorization Activity
         */
        private val HEALTH_APP_SETTING_DATA_SHARE_HEALTHKIT_ACTIVITY_SCHEME =
            "huaweischeme://healthapp/achievement?module=kit"
        /**
         * Error Code: can not resolve HUAWEI Health Authorization Activity
         * The value can be defined by developers.
         */
        private val APP_HEALTH_NOT_INSTALLED = "50033"
    }
}
