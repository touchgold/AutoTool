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
import com.tanjinc.autotool.utils.AppUtils
import com.tanjinc.autotool.utils.PermissionUtil
import com.tanjinc.autotool.utils.SharePreferenceUtil
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Exception


class MainActivity : AppCompatActivity(), View.OnClickListener {

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

        SharePreferenceUtil.putBoolean(Constants.ALL_TASK, false)
        switchAllTaskBtn.setOnCheckedChangeListener { _, isChecked ->
            SharePreferenceUtil.putBoolean(Constants.ALL_TASK, isChecked)
            Toast.makeText(this,  if (isChecked) "取消任务" else "打开任务", Toast.LENGTH_SHORT).show()
        }

        yaoqingmaTv.text = "趣头条邀请码: $QTT_CODE"
        miCodeTv.text = "米赚邀请码: $MIZHUAN_CODE"
    }


    override fun onClick(v: View?) {
        when(v) {
            qiandaoBtn -> {
                if (checkAccessibilityPermission()) {
                    cleanTask()
                    SharePreferenceUtil.putBoolean(Constants.QIANDAO_TASK, true)
                    startQuToutiao()
                }
            }
            dfttIcon -> {
                if (checkAccessibilityPermission()) {
                    cleanTask()
                    SharePreferenceUtil.putBoolean(Constants.PAPER_TASK, true)
                    startDongFangTT()
                }
            }
            qttIcon -> {
                if (checkAccessibilityPermission()) {
                    cleanTask()
                    SharePreferenceUtil.putBoolean(Constants.PAPER_TASK, true)
                    startQuToutiao()
                }
            }
            shiwanBtn -> {
                if (checkAccessibilityPermission()) {
                    cleanTask()
                    SharePreferenceUtil.putBoolean(Constants.SHIWAN_TASK, true)
                    startQuToutiao()
                }
            }
            testBtn -> {
                startBackground()
            }
            settingBtn -> {
                PermissionUtil.openAccessibility(this, sAccessibilityServiceName)
            }
            flowBtn -> {
                cleanTask()
                SharePreferenceUtil.putBoolean(Constants.FLOW_TASK, true)
                startQuToutiao()
            }
            mizhuanIcon -> {
                cleanTask()
                startMiZhuan()
            }
            miCodeTv -> {
                copyText(MIZHUAN_CODE)
                toastShort("米赚邀请码已复制")
            }
            yaoqingmaTv -> {
                copyText(QTT_CODE)
                toastShort("趣头条邀请码已复制")
            }
            dongfangCodeTv -> {
                copyText(DONGFANG_CODE)
                toastShort("东方头条邀请码已复制")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBroadcastReceiver)
    }



    private fun checkAccessibilityPermission() : Boolean {
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
        SharePreferenceUtil.putBoolean(Constants.FLOW_TASK, false)

    }

    private fun startQuToutiao() {
        if (!AppUtils.isAvilible(this, "com.jifen.qukan")) {
            toastShort("趣头条未安装")
            return
        }
        if (!checkAccessibilityPermission()) {
            return
        }
        Log.d(TAG, "startQuToutiao")
        val intent = Intent()
        intent.setClassName("com.jifen.qukan", "com.jifen.qkbase.main.MainActivity")
        startActivity(intent)
    }

    private fun startMiZhuan() {
        if (!AppUtils.isAvilible(this, AutoClickService.MiZhuanPackage)) {
            toastShort("米赚未安装")
            return
        }
        if (!checkAccessibilityPermission()) {
            return
        }
        Log.d(TAG, "startMiZhuan")
        startActivity(packageManager.getLaunchIntentForPackage("me.mizhuan"));
    }

    private fun startDongFangTT() {
        if (!AppUtils.isAvilible(this, AutoClickService.DongFangTTPackage)) {
            toastShort("东方头条 未安装")
            return
        }
        if (!checkAccessibilityPermission()) {
            return
        }
        Log.d(TAG, "startMiZhuan")
        startActivity(packageManager.getLaunchIntentForPackage("com.songheng.eastnews"))
    }

    private fun requestPermission() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(
                mediaProjectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION)
    }


    override fun onResume() {
        super.onResume()
        GlobalScope.launch {
            mIsPermissionGain = PermissionUtil.isAccessibilitySettingsOn(mActivity, sAccessibilityServiceName)
            launch(Main) {
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

    private fun copyText(copyStr:String) {
        val clipData = ClipData.newPlainText("text", copyStr)
        val clipManager: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipManager.primaryClip = clipData
    }

    private fun Context.toastShort(message:String) =
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()


    companion object {
        const val MSG_START_QUTOUTIAO = "start_activity"
        const val QTT_CODE = "A220137685"
        const val MIZHUAN_CODE = "300362641"
        const val DONGFANG_CODE = "028514563"
    }
}
