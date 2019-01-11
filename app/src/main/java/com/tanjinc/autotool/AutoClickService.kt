package com.tanjinc.autotool

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.clickById
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.clickByNode
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.clickByText
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.findByText
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.findByClassName
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.findTextArray
import com.tanjinc.autotool.utils.PrintUtils
import com.tanjinc.autotool.utils.ProcessUtils
import com.tanjinc.autotool.utils.SharePreferenceUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.abs

class AutoClickService : AccessibilityService() {
    companion object {
        val TAG = "AutoClickService"
        val QutoutiaoPackage = "com.jifen.qukan" //趣头条包名
        val NewsPackageName = "com.meizu.media.reader" // 资讯
        val mTargetPackageName = "com.tanjinc.autotool"
        val MiZhuanPackage = "me.mizhuan"
        val MiTouTiaoPackage = "me.toutiaoapp"
        val DongFangTTPackage = "com.songheng.eastnews"
        val YTTPackage = "com.expflow.reading"
    }
    private var mIsInstalling = false


    private var mHelpers = mutableListOf<BaseHelper>()
    private var mRootViewNode: AccessibilityNodeInfo ?= null


    override fun onCreate() {
        super.onCreate()
        mHelpers.add(MyttHelper())
        mHelpers.add(DongFangTTHelper())
        mHelpers.add(QuTTHelper())
    }

    override fun onInterrupt() {
    }

    fun Context.toast(msg:CharSequence) {
        Toast.makeText(MyApplication.getApplication(), msg, Toast.LENGTH_SHORT).show()
    }
    override fun onServiceConnected() {
        super.onServiceConnected()
        //设置关心的事件类型
        var info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        info.notificationTimeout = 100;//两个相同事件的超时时间间隔
        serviceInfo = info;
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        PrintUtils.printEvent(event)
        if(event == null || rootInActiveWindow == null) {
            return
        }
        if (SharePreferenceUtil.getBoolean(Constants.ALL_TASK)) {
            return
        }

        mRootViewNode = rootInActiveWindow
        for (helper in mHelpers) {
            if(event.packageName == helper.getPacketName()) {
                helper.autoWork(this, rootInActiveWindow, event)
            }
        }

        when(event.packageName) {


            MiTouTiaoPackage -> {
                MiZhuanHelper.autoWork(this, rootInActiveWindow, event)
            }
            MiZhuanPackage -> {
                MiZhuanHelper.autoWork(this, rootInActiveWindow, event)
            }
            YTTPackage -> {
                RttHelper.autoWork(this, rootInActiveWindow, event)
            }
            else -> {
                if (mIsInstalling) {
                    val currentPackageName = ProcessUtils.getTopActivityPackageName(MyApplication.getApplication())
                    Log.d(TAG, "currentProcess $currentPackageName")
                }
            }
        }

    }


    inner class MyBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, intent?.action)
            when(intent?.action) {
                "StartWork"-> {
                    Log.d(TAG, rootInActiveWindow.className.toString())
                }
            }
        }

    }

}
