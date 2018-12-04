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
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.findByViewName
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.findTextArray
import com.tanjinc.autotool.utils.PrintUtils
import com.tanjinc.autotool.utils.ProcessUtils
import com.tanjinc.autotool.utils.SharePreferenceUtil
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.*
import kotlin.math.abs

class AutoClickService : AccessibilityService() {
    companion object {
        val TAG = "AutoClickService"
        val QutoutiaoPackage = "com.jifen.qukan" //趣头条包名
        val NewsPackageName = "com.meizu.media.reader" // 资讯
        val mTargetPackageName = "com.tanjinc.autotool"
    }
    private lateinit var mBrocardReceiver:MyBroadcastReceiver
    private var mClickGetCoin:Boolean = false
    private var mAdCloseBtnId = "com.jifen.qukan:id/pb"
    private val mPacketInstaller = "com.samsung.android.packageinstaller"

    private var mIsScrollIng = false
    private var mIsNotificationTask = false
    private var mIsInstalling = false

    val MSG_REFRESH_VIDEO = 100
    val MSG_RETURN_QU = 101
    val MSG_KILL = 102
    val MSG_SCROLL = 103
    val MSG_BACK = 104
    val MSG_REFRESH_PAPER = 105

    private var mWebViewNode: AccessibilityNodeInfo ?= null

    private var mIsShowResent = false
    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what) {
                MSG_REFRESH_VIDEO -> {
                    if(clickByText(rootInActiveWindow,"刷新")) {
                        Toast.makeText(MyApplication.getApplication(), "刷新视频", Toast.LENGTH_SHORT).show()
                    }
                    if (SharePreferenceUtil.getBoolean(Constants.VIDEO_TASK)) {
                        removeMessages(MSG_REFRESH_VIDEO)
                        sendEmptyMessageDelayed(MSG_REFRESH_VIDEO, 20 * 1000)
                    }
                }
                MSG_RETURN_QU -> {

                }
                MSG_KILL -> {
                }
                MSG_SCROLL -> {
                    if (mWebViewNode == null ) {
                        mWebViewNode = findByViewName(rootInActiveWindow, "android.webkit.WebView")
                    }
                    mWebViewNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    sendEmptyMessageDelayed(MSG_SCROLL, Random().nextInt(5) * 1000L)
                    Log.d(TAG, "scroll ...")
                }
                MSG_BACK -> {
                    mLoading = true
                    mWebViewNode = null
                    removeMessages(MSG_SCROLL)
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    Log.d(TAG, "readPaperTask back to main")
                    Thread{
                        Thread.sleep(2 * 1000)
                        clickByText(rootInActiveWindow,"刷新")
                        Thread.sleep(3 * 1000)
                        mIsPaperTask = false
                        Log.d(TAG, "readPaperTask refresh, ok, $mLoading")
                        readPaperTask(rootInActiveWindow)
                    }.start()
                }
                MSG_REFRESH_PAPER -> {
                    refresh()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
//        mBrocardReceiver = MyBroadcastReceiver()
//        registerReceiver(mBrocardReceiver, IntentFilter("StartWork"))
    }
    override fun onInterrupt() {
    }

    private fun toast(msg:CharSequence) {
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
        var rootInActiveWindow = rootInActiveWindow
        if(event == null || rootInActiveWindow == null) {
            return
        }
        when(event.packageName) {
            QutoutiaoPackage -> {
                //读取通知栏
                if(mIsNotificationTask) {
                    return
                }
                when(event.eventType) {
                    AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                        var data = event.parcelableData
                        if (data is Notification) {
                            val notification: Notification = data
                            notification.contentIntent.send()
                            mIsNotificationTask = true
                            launch {
                                Log.d(TAG, "notification= "+notification.tickerText)
                                delay(5 * 1000)
                                performGlobalAction(GLOBAL_ACTION_BACK)
                                mIsNotificationTask = false
                            }
                        }
                    }
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                        timeReward()
                        closeAdDialog()

                        if (SharePreferenceUtil.getBoolean(Constants.VIDEO_TASK)) {
                            if (clickByText(rootInActiveWindow,"小视频")) {
                                mHandler.sendEmptyMessageDelayed(100, 20 * 1000)
                                return
                            }
                        }



                        if (SharePreferenceUtil.getBoolean(Constants.SHIWAN_TASK)) {
                            clickByText(rootInActiveWindow,"我的")
                            clickByText(rootInActiveWindow,"推荐应用")
                            if (findByText(rootInActiveWindow, "当前任务已抢光","xxxx") != null) {
                                SharePreferenceUtil.putBoolean(Constants.SHIWAN_TASK, false)
                                toast("没有试玩应用，过段时间再来！")
                                return
                            }

                            var targetNode = findByText(rootInActiveWindow, "人试玩", "")
                            if (targetNode != null) {
                                targetNode.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                mIsInstalling = true
                                return
                            }
                            clickByText(rootInActiveWindow,"进行中...")

                            val webNodeInfo = findByViewName(rootInActiveWindow, "android.webkit.WebView")
                            if (webNodeInfo!= null && webNodeInfo.isScrollable && !mIsScrollIng) {
                                launch {

                                    mIsScrollIng = true
                                    var i = 0
                                    while ( i < 5) {
                                        webNodeInfo.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                                        delay(Random().nextInt(10) * 1000)
                                        i++
                                    }

                                    performGlobalAction(GLOBAL_ACTION_BACK)
                                    mIsScrollIng = false
                                }
                            }
                        }


                        clickByText(rootInActiveWindow,"立即试玩")
                        installTask()
                        if (!clickByText(rootInActiveWindow,"领取奖励")) {
                            findByText(rootInActiveWindow, "打开", "null", true)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        }

                        if (SharePreferenceUtil.getBoolean(Constants.QIANDAO_TASK)) {
                            recommendAppTask()
                            clickByText(rootInActiveWindow,"我的福利")

                            var nodeInfo = findByText(rootInActiveWindow, "+120","已领")
                            var isClicked:Boolean ?= false
                            if (nodeInfo != null ) {
                                isClicked = when {
                                    nodeInfo.isClickable -> nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    nodeInfo.parent.isClickable -> nodeInfo.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                    else -> nodeInfo.parent?.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                }
                            }
                            if (isClicked != null && isClicked) {
                                Log.d(TAG, "task enter")
                                launch {
                                    delay(35 * 1000) //延迟35秒
//                                mHandler.sendEmptyMessageDelayed(MSG_KILL, 35 * 1000);
                                    mIsShowResent = true

                                    performGlobalAction(GLOBAL_ACTION_RECENTS)
                                }
                            } else {
                                var scrollView = findByViewName(rootInActiveWindow, "android.widget.ListView")
                                scrollView?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                            }
                        }
                    }
                }

                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ) {
                    //看文章
//                    if (SharePreferenceUtil.getBoolean(Constants.PAPER_TASK)) {
//                        readPaperTask(rootInActiveWindow)
//                        return
//                    }
                }

                rootInActiveWindow?.recycle()
            }
            "com.tanjinc.autotool" -> {
                val nodeArray = rootInActiveWindow.findAccessibilityNodeInfosByViewId("$mTargetPackageName:id/testBtn")
                if (nodeArray.size > 0) {
                    nodeArray[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
            "com.android.packageinstaller" ->  installTask()
            "com.samsung.android.packageinstaller" -> installTask()
            "com.android.systemui" -> {
                val nodeInfo = rootInActiveWindow.findAccessibilityNodeInfosByViewId("com.android.systemui:id/img")
                if (mIsShowResent) {
                    clickByText(rootInActiveWindow,"趣头条")
                    mIsShowResent = false
                }
            }
            NewsPackageName -> {
                NewsHelper.autoWork(rootInActiveWindow)
            }

            else -> {
                if (mIsInstalling) {
                    val currentPackageName = ProcessUtils.getTopActivityPackageName(MyApplication.getApplication())
                    Log.d(TAG, "currentProcess $currentPackageName")
                }
            }
        }

    }

    private var mIsPaperTask = false
    var mPaperArray = mutableListOf<AccessibilityNodeInfo>()
    var mVideoArray = mutableListOf<AccessibilityNodeInfo>()

    private var mCurrentPaperIndex = 0;
    private var mLoading = false
    private fun readPaperTask(rootNodeInfo: AccessibilityNodeInfo?) {

        Log.d(TAG, "readPaperTask ")
//        clickByText(rootInActiveWindow,"头条")
//        if (mLoading) {
//            Log.d(TAG, "readPaperTask isloading")
//            return
//        }
        if (mIsPaperTask) {
            //进入详情页
            mWebViewNode = findByViewName(rootInActiveWindow, "android.webkit.WebView")
            if (mWebViewNode != null) {
                Log.d(TAG, "readPaperTask enter to detail")
                Thread {
                    for (i in 0..6) {
                        Thread.sleep(5 * 1000)
                        mWebViewNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                        Log.d(TAG, "readPaperTask detail scroll... $i")

                    }
                    Thread.sleep(2 * 1000)
                    performGlobalAction(GLOBAL_ACTION_BACK)

                }.start()
                return
            } else {
                Log.d(TAG, "readPaperTask back to Main")
            }

        }
        if (mPaperArray.size == 0) {
            mVideoArray.clear()
            findTextArray(rootInActiveWindow, "评", mPaperArray)
            findTextArray(rootInActiveWindow, "视频", mVideoArray)
            for (videoItem in mVideoArray) {
                var videoRect = Rect()
                videoItem.getBoundsInScreen(videoRect)
                var removeIndex = -1
                for (i in 0 until mPaperArray.size) {
                    var textRect = Rect()
                    mPaperArray[i].getBoundsInScreen(textRect)
                    if (abs(videoRect.top - textRect.top) < 10) {
                        removeIndex = i
                    }
                }
                if (removeIndex != -1) {
                    Log.d(TAG, "readPaperTask remove" + mPaperArray[removeIndex].text)
                    mPaperArray.removeAt(removeIndex)
                }
            }
            if (mPaperArray.size == 0) {
                clickByText(rootInActiveWindow,"刷新")
                return
            }
            for (item in mPaperArray) {
                Log.d(TAG, "readPaperTask " + item.text)
            }
        }

//        if (mPaperArray.size > 0 && mCurrentPaperIndex < mPaperArray.size) {
//            var item = mPaperArray[mCurrentPaperIndex]
//            mIsPaperTask = clickByNode(rootInActiveWindow, item)
//            mCurrentPaperIndex++
//        }

//        if (mPaperArray.size > 0) {
//            if (mCurrentPaperIndex < mPaperArray.size) {
//                var item = mPaperArray[mCurrentPaperIndex]
//                if (!mIsPaperTask) {
//                    mIsPaperTask = clickByNode(rootInActiveWindow, item)
//                    Log.d(TAG, "readPaperTask $mIsPaperTask click "+ item.text )
//                    if (mIsPaperTask) {
//                        Log.d(TAG, "readPaperTask enter detail "+ item.text)
////
////                        mHandler.sendEmptyMessageDelayed(MSG_SCROLL, 5 * 1000)
////                        mHandler.sendEmptyMessageDelayed(MSG_BACK, 20 * 1000)
//                    } else {
//
//                    }
//                    mCurrentPaperIndex++
//                    if (mCurrentPaperIndex == mPaperArray.size) {
//                        mIsPaperTask = false
//                    }
//                }
//            } else {
//                if (!mIsPaperTask) {
//                    Log.d(TAG, "readPaperTask mCurrentPaperIndex=$mCurrentPaperIndex")
//                    mPaperArray.clear()
//                    mCurrentPaperIndex = 0
//                    mLoading = true
//
//                    clickByText(rootInActiveWindow,"刷新")
//                    mHandler.sendEmptyMessageDelayed(MSG_REFRESH_PAPER, 2 * 1000)
//                }
//            }
//        }
    }

    private fun refresh() {
//        var ret = clickByText(rootInActiveWindow,"刷新")
//        Log.d(TAG, "刷新数据! $ret")
//        mLoading = false
        val rececylerViewNode = findByViewName(rootInActiveWindow, "android.support.v7.widget.RecyclerView")
        rececylerViewNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    private var mLooper = 1;
    private fun printRoot(rootNodeInfo: AccessibilityNodeInfo?, stringBuffer: StringBuffer) {
        if (rootNodeInfo == null || rootNodeInfo.childCount == 0) {
            return
        }
        for (i in 0 until mLooper) {
            stringBuffer.append("*")
        }
        stringBuffer.append(rootNodeInfo.className)
        stringBuffer.append("(" + rootNodeInfo.text +")\n")
        if (rootNodeInfo.childCount > 0) {
            for (i in 0 until rootNodeInfo.childCount) {
                var child1 = rootNodeInfo.getChild(i)
                printRoot(child1, stringBuffer)
            }
        } else {
            stringBuffer.append(rootNodeInfo.className)
            stringBuffer.append("\n")
        }
        mLooper++
        stringBuffer.append("\n")
    }
    private fun installTask() : Boolean{
        if ( clickByText(rootInActiveWindow,"完成") ||
                clickByText(rootInActiveWindow,"安装") ||
                clickByText(rootInActiveWindow,"确认") ||
                clickByText(rootInActiveWindow,"继续") ||
                clickByText(rootInActiveWindow,"下一步") ||
                clickById(rootInActiveWindow, "com.android.packageinstaller:id/decide_to_continue") ||
                clickById(rootInActiveWindow, "com.android.packageinstaller:id/action_positive") ||
                clickByText(rootInActiveWindow,"继续安装") ||
                clickByText(rootInActiveWindow,"打开阅读")
        ) {
            return true
        }
        return false
    }
    //领取时段奖励
    private fun timeReward() {
        clickByText(rootInActiveWindow, "领取")
        clickByText(rootInActiveWindow, "我知道了")
    }

    private fun closeAdDialog() {
        clickById(rootInActiveWindow, mAdCloseBtnId)
    }

    private fun recommendAppTask() {
        clickByText(rootInActiveWindow, "我的")
        clickByText(rootInActiveWindow,"推荐应用")

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
