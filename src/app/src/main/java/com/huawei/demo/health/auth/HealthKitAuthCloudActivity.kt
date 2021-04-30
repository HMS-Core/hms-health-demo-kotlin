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
import androidx.appcompat.app.AppCompatActivity

import com.google.gson.JsonParser
import com.huawei.demo.health.OkHttpUtilCallback
import com.huawei.demo.health.R

import okhttp3.OkHttpClient
import okhttp3.Request

import java.util.function.Consumer

/**
 * Check authorization result of HUAWEI Health to HUAWEI Health Kit by Restful API
 *
 * @since 2020-09-18
 */
class HealthKitAuthCloudActivity : AppCompatActivity() {

    private var mContext: Context? = null

    // display authorization result
    private var authDescTitle: TextView? = null

    // display authorization failure message
    private var authFailTips: TextView? = null

    // confirm result
    private var confirm: Button? = null

    // retry authorization
    private var authRetry: Button? = null

    // Login in to the HUAWEI ID and authorize
    private var loginAuth: Button? = null

    /**
     * accessToken for http request
     */
    private var accessToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_auth)

        initView()
        initService()
    }

    /**
     * Method of handling HUAWEI Health authorization result
     *
     * @param requestCode indicating the request code for Health authorization Activity
     * @param resultCode indicating the authorization result code
     * @param data indicating the authorization result. but data is null, you need to query authorization result.
     */
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

        authDescTitle!!.setVisibility(View.GONE)
        authFailTips!!.visibility = View.GONE
        confirm!!.visibility = View.GONE
        authRetry!!.visibility = View.GONE

        // listener to login HUAWEI ID and authorization
        loginAuth!!.setOnClickListener { view ->
            val intent = Intent(this, HealthKitCloudLogin::class.java)
            startActivityForResult(intent, REQUEST_SIGN_IN_LOGIN)
        }

        // listener to retry authorization
        authRetry!!.setOnClickListener { view -> checkOrAuthorizeHealth() }

        // finish this Activity
        confirm!!.setOnClickListener { view -> this.finish() }
    }

    /**
     * Method of handling authorization result responses
     *
     * @param requestCode (indicating the request code for displaying the authorization screen)
     * @param data (indicating the authorization result response)
     */
    private fun handleSignInResult(requestCode: Int, data: Intent?) {
        if (requestCode != REQUEST_SIGN_IN_LOGIN || data == null) {
            return
        }
        Log.d(TAG, "HMS handleSignInResult")
        accessToken = data!!.getStringExtra("accessToken")

        checkOrAuthorizeHealth()
    }

    /**
     * Method of handling the HAUWEI Health authorization Activity response
     *
     * @param requestCode (indicating the request code for displaying the HUAWEI Health authorization screen)
     */
    private fun handleHealthAuthResult(requestCode: Int) {
        // Determine whether request code is HUAWEI Health authorization Activity
        if (requestCode != REQUEST_HEALTH_AUTH) {
            return
        }

        // Query the authorization result after the HUAWEI health authorization Activity is returned
        queryHealthAuthorization()
    }

    private fun initService() {
        mContext = this
        // get accessToken from intent
        accessToken = intent.getStringExtra("accessToken")
    }

    /**
     * Check HUAWEI Health authorization status By restful api.
     * if not, start HUAWEI Health authorization Activity for user authorization.
     */
    private fun checkOrAuthorizeHealth() {
        Log.d(TAG, "begint to checkOrAuthorizeHiHealthPrivacy")
        // 1. Build a PopupWindow as progress dialog for time-consuming operation.
        val popupWindow = initPopupWindow()

        // 2. Build restful request to query HUAWEI Health authorization status.
        val privacyRequest = buildPrivacyRequest(accessToken)

        // 3. Sending an HTTP Request Asynchronously, and build user-defined Callback for response. This Callback init
        // with an anonymous Consumer to handle query result for checkOrAuthorizeHiHealthPrivacy.
        val mClient = OkHttpClient()
        mClient.newCall(privacyRequest).enqueue(
            OkHttpUtilCallback(Consumer { response ->
                Log.i(TAG, "checkOrAuthorizeHiHealthPrivacy success response:$response")
                // Update View with result, call View.Post() to ensure run on the user interface thread.
                window.decorView.post {
                    // Dismiss the PopupWindow
                    popupWindow.dismiss()

                    // request error
                    if (OkHttpUtilCallback.REQUEST_ERROR.equals(response)) {
                        buildFailView(OkHttpUtilCallback.REQUEST_ERROR)
                        return@post
                    }

                    // Parse response Json to get authorization result
                    val parser = JsonParser()
                    val opinion = parser.parse(response).asJsonArray.get(0).asJsonObject.get("opinion").asInt
                    // If HUAWEI Health is authorized, build success View.
                    if (opinion == AUTH_ENABLED) {
                        buildSuccessView()
                        return@post
                    }

                    // If not, start HUAWEI Health authorization Activity by schema with User-defined requestCode.
                    val healthKitSchemaUri = Uri.parse(HEALTH_APP_SETTING_DATA_SHARE_HEALTHKIT_ACTIVITY_SCHEME)
                    val intent = Intent(Intent.ACTION_VIEW, healthKitSchemaUri)
                    // Before start, Determine whether the HUAWEI health authorization Activity can be opened.
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivityForResult(intent, REQUEST_HEALTH_AUTH)
                    } else {
                        buildFailView(RESOLVE_ACTIVITY_ERROR)
                    }
                }
            })
        )
    }

    /**
     * Query Huawei Health authorization result.
     */
    private fun queryHealthAuthorization() {
        Log.d(TAG, "begint to queryPrivacyAuthorization")
        // 1. Build a PopupWindow as progress dialog for time-consuming operation
        val popupWindow = initPopupWindow()

        // 2. Build restful request to query HUAWEI Health authorization status.
        val privacyRequest = buildPrivacyRequest(accessToken)

        // 3. Sending an HTTP Request Asynchronously, and build user-defined Callback for response. This Callback init
        // with an anonymous Consumer to handle query result for queryPrivacyAuthorization.
        val mClient = OkHttpClient()
        mClient.newCall(privacyRequest).enqueue(
            OkHttpUtilCallback(Consumer { response ->
                Log.i(TAG, "queryPrivacyAuthorization success response:$response")
                // Update View with result, call View.Post() to ensure run on the user interface thread.
                window.decorView.post {
                    // Dismiss the PopupWindow
                    popupWindow.dismiss()

                    // request error
                    if (OkHttpUtilCallback.REQUEST_ERROR.equals(response)) {
                        buildFailView(OkHttpUtilCallback.REQUEST_ERROR)
                        return@post
                    }

                    // Parse response Json to get authorization result
                    val parser = JsonParser()
                    val opinion = parser.parse(response).asJsonArray.get(0).asJsonObject.get("opinion").asInt

                    // If HUAWEI Health is authorized, build success View. if Not, build fail view.
                    if (opinion == AUTH_ENABLED) {
                        buildSuccessView()
                    } else {
                        buildFailView(null)
                    }
                }
            })
        )
    }

    private fun buildFailView(errorMessage: String?) {
        authDescTitle!!.setText(R.string.health_auth_health_kit_fail)
        authFailTips!!.visibility = View.VISIBLE
        authRetry!!.visibility = View.VISIBLE
        confirm!!.visibility = View.GONE

        // If can't resolve HUAWEI Health Authorization Activity, remind the user to install supported version APP.
        if (errorMessage == OkHttpUtilCallback.REQUEST_ERROR) {
            authFailTips!!.text = (resources.getString(R.string.health_auth_health_kit_fail_tips_exception))
        } else if (RESOLVE_ACTIVITY_ERROR == errorMessage) {
            authFailTips!!.text = resources.getString(R.string.health_auth_health_kit_fail_tips_install)
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

    /**
     * Build restful request to query HUAWEI Health authorization status
     *
     * @param accessToken header Authorization params for request
     * @return Request to query HUAWEI Health authorization status
     */
    private fun buildPrivacyRequest(accessToken: Any?): Request {
        return Request.Builder().url(CLOUD_PRIVACY_URL)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + accessToken!!)
            .get()
            .build()
    }

    /**
     * init popupWindow as progress dialog.
     *
     * @return instance of popupWindow
     */
    private fun initPopupWindow(): PopupWindow {
        val popupWindow = PopupWindow()
        popupWindow.height = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow.width = ViewGroup.LayoutParams.WRAP_CONTENT
        popupWindow.isFocusable = true
        val view = LayoutInflater.from(this).inflate(R.layout.activity_waitting, null)
        popupWindow.contentView = view

        window.decorView.post {
            popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
            authDescTitle!!.setVisibility(View.VISIBLE)
            loginAuth!!.setVisibility(View.GONE) }
        return popupWindow
    }

    companion object {
        private val TAG = "HealthKitAuthCloud"

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
         * Error Code: can not resolve HUAWEI Health Authorization Activity
         */
        private val RESOLVE_ACTIVITY_ERROR = "50033"

        /**
         * Scheme of Huawei Health Authorization Activity
         */
        private val HEALTH_APP_SETTING_DATA_SHARE_HEALTHKIT_ACTIVITY_SCHEME =
            "huaweischeme://healthapp/achievement?module=kit"

        /**
         * URL of query Huawei Health Authorization result
         */
        private val CLOUD_PRIVACY_URL =
            "https://health-api.cloud.huawei.com/healthkit/v1/profile/privacyRecords";

        /**
         * Huawei Health authorization enabled
         */
        private val AUTH_ENABLED = 1
    }
}
