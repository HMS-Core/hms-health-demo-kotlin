package com.huawei.demo.health

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.huawei.demo.health.auth.HealthKitAuthActivity

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
}
