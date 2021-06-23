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

package com.huawei.demo.health.auth

import java.util.Locale

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

import com.huawei.demo.health.R
import com.huawei.hms.hihealth.HiHealthStatusCodes
import com.huawei.hms.hihealth.HuaweiHiHealth
import com.huawei.hms.hihealth.SettingController
import com.huawei.hms.hihealth.data.Scopes
import com.huawei.hms.support.api.entity.common.CommonConstant

/**
 * Check authorization result of HUAWEI Health to HUAWEI Health Kit by JAVA API
 *
 * @since 2020-09-18
 */
class HealthKitAuthActivity : Activity() {

    // Internal context object of the activity
    private var mContext: Context? = null

    // Object of SettingController
    private var mSettingController: SettingController? = null

    // display authorization title
    private var authTitle: TextView? = null

    // display authorization description
    private var authDesc: TextView? = null

    // confirm result
    private var confirm: Button? = null

    // retry HUAWEI health authorization
    private var authRetry: Button? = null

    // display context
    private var mContextLayout: LinearLayout? = null

    // authorization title back icon
    private var mAuthBackIcon: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_auth)

        initView()
        initService()

        // Start the authorization process.
        requestAuth()
    }

    /**
     * Add scopes to apply for and obtains the authorization process Intent.
     */
    private fun requestAuth() {
        // Add scopes to apply for. The following only shows an example.
        // Developers need to add scopes according to their specific needs.
        val scopes = arrayOf(
            // View and save steps in HUAWEI Health Kit.
            Scopes.HEALTHKIT_STEP_READ, Scopes.HEALTHKIT_STEP_WRITE,
            // View and save height and weight in HUAWEI Health Kit.
            Scopes.HEALTHKIT_HEIGHTWEIGHT_READ, Scopes.HEALTHKIT_HEIGHTWEIGHT_WRITE,
            // View and save the heart rate data in HUAWEI Health Kit.
            Scopes.HEALTHKIT_HEARTRATE_READ, Scopes.HEALTHKIT_HEARTRATE_WRITE,
            // View and save activityRecord in HUAWEI Health Kit.
            Scopes.HEALTHKIT_ACTIVITY_RECORD_READ, Scopes.HEALTHKIT_ACTIVITY_RECORD_WRITE
        )

        // Obtains the authorization process Intent.
        // True indicates that the health app authorization process is enabled. False indicates disabled.
        val intent = mSettingController!!.requestAuthorizationIntent(scopes, true)

        // The authorization process page is displayed.
        startActivityForResult(intent, REQUEST_AUTH)
    }

    /**
     * Handling authorization result responses
     *
     * @param requestCode (indicating the request code for displaying the authorization screen)
     * @param resultCode (indicating the authorization activity result code)
     * @param data (indicating the authorization result response)
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle only the authorized responses
        if (requestCode == REQUEST_AUTH) {
            // Obtain the authorization response from the intent.
            val result = mSettingController!!.parseHealthKitAuthResultFromIntent(data)
            if (result == null) {
                Log.w(TAG, "authorization fail")
                return
            }

            // Check whether the authorization result is successful.
            if (result.isSuccess) {
                Log.i(TAG, "authorization success")
                buildSuccessView()
            } else {
                Log.w(TAG, "authorization fail, errorCode:" + result.errorCode)
                buildFailView(result.errorCode)
            }
        }
    }

    private fun initView() {
        mContextLayout = this.findViewById(R.id.health_context)
        mContextLayout!!.visibility = View.GONE

        authTitle = findViewById(R.id.health_auth_desc_title)
        authDesc = findViewById(R.id.health_auth_desc)
        confirm = findViewById(R.id.health_auth_confirm)
        authRetry = findViewById(R.id.health_auth_retry)
        mAuthBackIcon = findViewById(R.id.health_auth_back_icon)

        confirm!!.visibility = View.GONE
        authRetry!!.visibility = View.GONE

        authRetry!!.setOnClickListener { view -> requestAuth() }

        confirm!!.setOnClickListener { view -> this.finish() }

        mAuthBackIcon!!.setOnClickListener { view -> onBackPressed() }
    }

    private fun initService() {
        mContext = this
        Log.i(TAG, "HiHealthKitClient connect to service")
        mSettingController = HuaweiHiHealth.getSettingController(mContext!!)
    }

    private fun buildFailView(errorCode: Int) {
        mContextLayout!!.visibility = View.VISIBLE
        authDesc!!.visibility = View.VISIBLE
        authRetry!!.visibility = View.VISIBLE
        confirm!!.visibility = View.GONE

        authTitle!!.setText(R.string.healthkit_auth_failure)

        // Authentication failure message. if error message is not null, displayed based on the error code.
        val failedResult = StringBuilder()
        if (errorCode == HiHealthStatusCodes.HUAWEI_ID_SIGNIN_ERROR || CommonConstant.RETCODE.SIGN_IN_AUTH <= errorCode && errorCode <= CommonConstant.RETCODE.SIGN_IN_EXECUTING) {
            failedResult
                .append(String.format(Locale.ENGLISH, resources.getString(R.string.healthkit_auth_result1_fail), "HealthKitDemo"))
            failedResult.append(resources.getString(R.string.healthkit_auth_result2_fail_error))
                .append(errorCode)
                .append(System.lineSeparator())
        } else {
            failedResult
                .append(String.format(Locale.ENGLISH, resources.getString(R.string.healthkit_auth_result1), "HealthKitDemo"))
        }

        failedResult.append(resources.getString(R.string.healthkit_auth_result2_fail))
        if (errorCode != 0 && errorCode != HiHealthStatusCodes.HEALTH_APP_NOT_AUTHORISED) {
            failedResult.append(resources.getString(R.string.healthkit_auth_result2_fail_error))
            if (errorCode == HiHealthStatusCodes.NON_HEALTH_USER) {
                failedResult.append(resources.getString(R.string.healthkit_auth_fail_error_50038))
            } else if (errorCode == HiHealthStatusCodes.UNTRUST_COUNTRY_CODE) {
                failedResult.append(resources.getString(R.string.healthkit_auth_fail_error_50040))
            } else if (errorCode == HiHealthStatusCodes.NO_NETWORK) {
                failedResult.append(resources.getString(R.string.healthkit_auth_fail_error_50030))
            } else if (errorCode == HiHealthStatusCodes.UNKNOWN_AUTH_ERROR) {
                failedResult.append(resources.getString(R.string.healthkit_auth_fail_error_50005))
            } else {
                failedResult.append(resources.getString(R.string.healthkit_auth_fail_error_50011))
            }
        }

        failedResult.append(resources.getString(R.string.healthkit_auth_fail_tips1))
        failedResult.append(resources.getString(R.string.healthkit_auth_fail_tips2))
        authDesc!!.text = failedResult.toString()
    }

    private fun buildSuccessView() {
        mContextLayout!!.visibility = View.VISIBLE
        authDesc!!.visibility = View.GONE
        authRetry!!.visibility = View.GONE
        confirm!!.visibility = View.VISIBLE
        authTitle!!.setText(R.string.healthkit_auth_success)
        authTitle!!.visibility = View.VISIBLE
    }

    companion object {
        private val TAG = "DevDemo" + "KitAuth"

        /**
         * Request code for displaying the sign in authorization screen using the startActivityForResult method.
         * The value can be defined by developers.
         */
        private val REQUEST_AUTH = 1000
    }
}