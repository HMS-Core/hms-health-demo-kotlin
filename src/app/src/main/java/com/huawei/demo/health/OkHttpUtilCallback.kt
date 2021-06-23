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

import java.io.IOException

import java.util.function.Consumer

import android.util.Log

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

/**
 * Callback for OkHttp request
 *
 * @since 2020-09-27
 */
class OkHttpUtilCallback(consumer: Consumer<String>) : Callback {

    private val consumer: Consumer<String>?

    init {
        this.consumer = consumer
    }

    // If request fail, make a toast to indicate the failure.
    override fun onFailure(call: Call, e: IOException) {
        val stringBuilder = StringBuilder("Request error: ").append(call.request().url().toString())
            .append(" ")
            .append(e.message)

        Log.e(TAG, stringBuilder.toString())
        consumer!!.accept(REQUEST_ERROR)
    }

    @Throws(IOException::class)
    override fun onResponse(call: Call, response: Response) {
        Log.d(TAG, "onResponse: $response")
        if (consumer == null) {
            return
        }

        // Check whether the request is successful. If yes, invoke the Consumer to process the response. Otherwise, pass
        // REQUEST_ERROR code.
        if (response.isSuccessful && response.body() != null) {
            consumer.accept(response.body()!!.string())
        } else {
            consumer.accept(REQUEST_ERROR)
        }
    }

    companion object {
        private val TAG = "OkHttpUtilCallback"

        /**
         * Request error code
         */
        val REQUEST_ERROR = "500"
    }
}