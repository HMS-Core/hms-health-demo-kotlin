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

import java.util.ArrayList;

import android.content.Intent
import android.os.Bundle
import android.util.Log;
import android.view.View

import androidx.appcompat.app.AppCompatActivity

import com.huawei.demo.health.auth.HealthKitAuthActivity
import com.huawei.hms.common.ApiException
import com.huawei.hms.support.account.AccountAuthManager
import com.huawei.hms.support.account.request.AccountAuthParamsHelper

class HealthKitMainActivity : AppCompatActivity() {
    private val TAG = "KitConnectActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_kit_main)
    }

    /**
     * Data Controller
     *
     * @param view UI object
     */
    fun healthDataControllerOnclick(view: View) {
        val intent = Intent(this, HealthKitDataControllerActivity::class.java)
        startActivity(intent)
    }

    /**
     * Setting Controller
     *
     * @param view UI object
     */
    fun healthSettingControllerOnclick(view: View) {
        val intent = Intent(this, HealthKitSettingControllerActivity::class.java)
        startActivity(intent)
    }

    /**
     * Auto Recorder
     *
     * @param view UI object
     */
    fun healthAutoRecorderOnClick(view: View) {
        val intent = Intent(this, HealthKitAutoRecorderControllerActivity::class.java)
        startActivity(intent)
    }

    /**
     * Activity Records Controller
     *
     * @param view UI object
     */
    fun healthActivityRecordOnClick(view: View) {
        val intent = Intent(this, HealthKitActivityRecordControllerActivity::class.java)
        startActivity(intent)
    }

    /**
     * signing In and applying for Scopes
     *
     * @param view UI object
     */
    fun onLoginClick(view: View) {
        val intent = Intent(this, HealthKitAuthActivity::class.java)
        startActivity(intent)
    }

    /**
     * health Records Controller
     *
     * @param view UI object
     */
    fun hihealthHealthControllerOnclick(view: View) {
        val intent = Intent(this, HealthKitHealthRecordControllerActivity::class.java)
        startActivity(intent)
    }
    
    
    /**
     * To improve privacy security, your app should allow users to cancel authorization.
     * After calling this function, you need to call login and authorize again.
     *
     * @param view (indicating a UI object)
     */
    fun cancelScope(view: View) {
        val authParams = AccountAuthParamsHelper().setAccessToken().setScopeList(ArrayList()).createParams()
        val authService = AccountAuthManager.getService(applicationContext, authParams)
        authService.cancelAuthorization().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Processing after successful cancellation of authorization
                Log.i(TAG, "cancelAuthorization success")
            } else {
                // Processing after failed cancellation of authorization
                val exception = task.exception
                Log.i(TAG, "cancelAuthorization fail")
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.e(TAG, "cancelAuthorization fail for errorCode: $statusCode")
                }
            }
        }
    }
}
