package com.tanjinc.autotool

import android.app.Activity
import android.content.*
import android.media.projection.MediaProjectionManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*
import android.view.View
import android.widget.Toast
import com.tanjinc.autotool.utils.PermissionUtil
import com.tanjinc.autotool.utils.SharePreferenceUtil
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    val REQUEST_MEDIA_PROJECTION = 18
    val sAccessibilityServiceName = AutoClickService::class.java.name

    var mIsPermissionGain = false
    lateinit var mActivity: AppCompatActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val intentFilter = IntentFilter(MSG_START_QUTOUTIAO)
        registerReceiver(mBroadcastReceiver, intentFilter)

        mActivity = this
        permissionWarmTv.setOnClickListener {
            PermissionUtil.openAccessibility(this, sAccessibilityServiceName)
        }

        startBtn.setOnClickListener {
            PermissionUtil.openAccessibility(this, AutoClickService::class.java.name)
//            requestPermission()
//            startService((Intent(this, WorkService::class.java)))
//            startQuToutiao()
//            sendBroadcast(Intent("StartWork"))
        }

        settingBtn.setOnClickListener {
            PermissionUtil.openAccessibility(this, sAccessibilityServiceName)
        }

        testBtn.setOnClickListener {
//            startBackground()
            startQuToutiao()
//            val isEnable = PermissionUtil.isNotificationEnabled(this)
//            Toast.makeText(this, if(isEnable) "enable" else "not enable", Toast.LENGTH_SHORT).show()
        }
        qiandaoBtn.setOnClickListener {
            if (checkAccesibilityPermission()) {
                cleanTask()
                SharePreferenceUtil.putBoolean(Constants.QIANDAO_TASK, true)
                startQuToutiao()
            }
        }
        readPaperBtn.setOnClickListener {
            if (checkAccesibilityPermission()) {
                cleanTask()
                SharePreferenceUtil.putBoolean(Constants.PAPER_TASK, true)
                startQuToutiao()
            }
        }
        cancelBtn.setOnClickListener {
            cleanTask()
        }

        shiwanBtn.setOnClickListener {
            if (checkAccesibilityPermission()) {
                cleanTask()
                SharePreferenceUtil.putBoolean(Constants.SHIWAN_TASK, true)
                startQuToutiao()
            }
        }
        videoBtn.setOnClickListener {
            if (checkAccesibilityPermission()) {
                cleanTask()
                SharePreferenceUtil.putBoolean(Constants.VIDEO_TASK, true)
                startQuToutiao()
            }
        }
        yaoqingmaTv.setOnClickListener {

        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBroadcastReceiver)
    }


    private fun checkAccesibilityPermission() : Boolean {
        if (!mIsPermissionGain) {
            Toast.makeText(this, "没有开启辅助功能权限，请打开", Toast.LENGTH_SHORT).show()
        }
        return mIsPermissionGain
    }
    private fun cleanTask() {
        SharePreferenceUtil.putBoolean(Constants.SHIWAN_TASK, false)
        SharePreferenceUtil.putBoolean(Constants.QIANDAO_TASK, false)
        SharePreferenceUtil.putBoolean(Constants.VIDEO_TASK, false)
        SharePreferenceUtil.putBoolean(Constants.PAPER_TASK, false)

    }

    private fun startQuToutiao() {
        if (!checkAccesibilityPermission()) {
            return
        }
        Log.d(TAG, "startQuToutiao")
        val intent = Intent()
        intent.setClassName("com.jifen.qukan", "com.jifen.qkbase.main.MainActivity")
        startActivity(intent)
    }

    private fun requestPermission() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION)
    }


    override fun onResume() {
        super.onResume()
        launch {
            mIsPermissionGain = PermissionUtil.isAccessibilitySettingsOn(mActivity, sAccessibilityServiceName)
            launch(UI) {
                permissionWarmTv.visibility = if (mIsPermissionGain) View.GONE else View.VISIBLE
            }

//            PermissionUtil.checkUsageStateAccessPermission(mActivity)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_MEDIA_PROJECTION ->
                if (resultCode == Activity.RESULT_OK && data != null) {
                    FloatWindowsService.Companion.setResultData(data)
                    startService(Intent(applicationContext, FloatWindowsService::class.java))
                }
        }

    }

    private fun startBackground() {
        try {
            val intent = Intent("android.intent.action.MAIN")
            intent.component = ComponentName("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity")
            startActivity(intent)
        } catch (e: Exception) {

        }
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "receive " + intent.action)
            when(intent.action) {
                MSG_START_QUTOUTIAO -> startQuToutiao()
            }
        }
    }

    companion object {
        const val MSG_START_QUTOUTIAO = "start_activity"
    }
}
