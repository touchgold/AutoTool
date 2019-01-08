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
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
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
    val MSG_BACK_MAIN = 104
    val MSG_REFRESH_PAPER = 105
    val MSG_DETAIL_LOOPER = 106

    private var mWebViewNode: AccessibilityNodeInfo ?= null
    private var mRootViewNode: AccessibilityNodeInfo ?= null

    private var mRetryCount = 0
    private var mIsShowResent = false
    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            Log.d(TAG, "handler = " + msg.what)
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
                        mWebViewNode = findByClassName(rootInActiveWindow, "android.webkit.WebView")
                    }
                    mWebViewNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    sendEmptyMessageDelayed(MSG_SCROLL, Random().nextInt(5) * 1000L)
                    Log.d(TAG, "scroll ...")
                }
                MSG_BACK_MAIN -> {
                    if (mTaskStack.size > 0) {
                        Log.d(TAG, "mainPage enter 1")
                        mIsPaperTask = false
                        mRetryCount = 0
                        removeMessages(MSG_BACK_MAIN)
                        mainPage()
                        return
                    }
                    val recyclerNode = findByClassName(rootInActiveWindow, "android.support.v7.widget.RecyclerView", true)
                    if (recyclerNode != null && recyclerNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                        Log.d(TAG, "mainPage scroll success")
                        Log.d(TAG, "mainPage enter 2")
                        mIsPaperTask = false
                        mainPage()
                        mRetryCount = 0
                        removeMessages(MSG_BACK_MAIN)
                    } else {
//                        Log.d(TAG, "mainPage scroll fail retry")
                        mRetryCount++
                        if (mRetryCount > 5) {
                            if (!clickByText(mRootViewNode,"刷新")) {
                                Log.d(TAG, "mainPage click k9")
                                clickById(rootInActiveWindow, "com.jifen.qukan:id/k9")
                                performGlobalAction(GLOBAL_ACTION_BACK)
                            }
                            sendEmptyMessageDelayed(MSG_BACK_MAIN, 5 * 1000)
                            mRetryCount = 0
                        }
                        removeMessages(MSG_BACK_MAIN)
                        sendEmptyMessageDelayed(MSG_BACK_MAIN, 2 * 1000)
                    }
                }
                MSG_REFRESH_PAPER -> {
                    refresh()
                }
                MSG_DETAIL_LOOPER -> {
                    detailLoop()
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
        if(event == null || rootInActiveWindow == null) {
            return
        }
        if (SharePreferenceUtil.getBoolean(Constants.ALL_TASK)) {
            mHandler.removeCallbacksAndMessages(null)
            return
        }

        mRootViewNode = rootInActiveWindow
        if (SharePreferenceUtil.getBoolean(Constants.SHIWAN_TASK)) {
            InstallDialogHelper.autoInstall(this, mRootViewNode, event)
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
//                            InstallDialogHelper.reset()
                            InstallDialogHelper.autoInstall(this, rootInActiveWindow, event)
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
                                var scrollView = findByClassName(rootInActiveWindow, "android.widget.ListView")
                                scrollView?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                            }
                        }
                    }
                }

                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || !mIsPaperTask) {
                    //看文章
                    if (SharePreferenceUtil.getBoolean(Constants.PAPER_TASK)) {
                        readPaperTask(rootInActiveWindow)
                    }
                }

                QttHelper.autoWork(this, rootInActiveWindow, event)

                rootInActiveWindow?.recycle()
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

            MiTouTiaoPackage -> {
                MiZhuanHelper.autoWork(this, rootInActiveWindow, event)
            }
            MiZhuanPackage -> {
                MiZhuanHelper.autoWork(this, rootInActiveWindow, event)
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
    private var mTaskStack: Stack<AccessibilityNodeInfo> = Stack()
    private fun readPaperTask(rootNodeInfo: AccessibilityNodeInfo?) {
        Log.d(TAG, "readPaperTask mTaskStack.size = " + mTaskStack.size)
        mainPage()
    }

    private var lastNode :AccessibilityNodeInfo ?= null
    private fun mainPage() {
        Log.d(TAG, "mainPage taskStack.size=" + mTaskStack.size)
        if (!mIsPaperTask && mTaskStack.size == 0) {
            var paperArray = mutableListOf<AccessibilityNodeInfo>()
            var videoArray = mutableListOf<AccessibilityNodeInfo>()
            var zhidingArray = mutableListOf<AccessibilityNodeInfo>()

            findTextArray(rootInActiveWindow, Regex("[0-9]+评"),  paperArray)
            findTextArray(rootInActiveWindow, Regex("视频"), videoArray, true)
            findTextArray(rootInActiveWindow, Regex("置顶"), zhidingArray, true)

            for (videoItem in videoArray) {
                var videoRect = Rect()
                videoItem.getBoundsInScreen(videoRect)
                var removeIndex = -1
                for (i in 0 until paperArray.size) {
                    var textRect = Rect()
                    paperArray[i].getBoundsInScreen(textRect)
                    if (abs(videoRect.top - textRect.top) < 10) {
                        removeIndex = i
                    }
                }
                if (removeIndex != -1) {
                    Log.d(TAG, "readPaperTask remove　视频　" + paperArray[removeIndex].text)
                    paperArray.removeAt(removeIndex)
                }
            }

            for (zhiDingItem in zhidingArray) {
                val zhiDingRect = Rect()
                zhiDingItem.getBoundsInScreen(zhiDingRect)
                var removeIndex = -1
                for (i in 0 until paperArray.size) {
                    var textRect = Rect()
                    paperArray[i].getBoundsInScreen(textRect)
                    if (abs(zhiDingRect.top - textRect.top) < 10) {
                        removeIndex = i
                    }
                }
                if (removeIndex != -1) {
                    Log.d(TAG, "readPaperTask remove　置顶　" + paperArray[removeIndex].text)
                    paperArray.removeAt(removeIndex)
                }
            }


            if (paperArray.size == 0) {
                mHandler.sendEmptyMessageDelayed(MSG_BACK_MAIN, 3 * 1000)
                return
            }
            if (paperArray[0] == lastNode) {
                clickByText(rootInActiveWindow,"刷新")
                mHandler.sendEmptyMessageDelayed(MSG_BACK_MAIN, 3 * 1000)
                return
            }
            lastNode = paperArray[0]
            for (item in paperArray) {
                Log.d(TAG, "readPaperTask " + item.text)
                mTaskStack.push(item)
            }
            mIsPaperTask = false
        }

        while (!mIsPaperTask && mTaskStack.size > 0) {
            var taskItem = mTaskStack.pop()
            mIsPaperTask = clickByNode(rootInActiveWindow, taskItem)
            if (mIsPaperTask) {
                Log.d(TAG, "enter " + taskItem.text)
                mHandler.removeMessages(MSG_DETAIL_LOOPER)
                mHandler.sendEmptyMessageDelayed(MSG_DETAIL_LOOPER, 500)
                break
            }
            Log.d(TAG, "clickNode false " + taskItem.text)
        }
        if (!mIsPaperTask) {
            mHandler.sendEmptyMessageDelayed(MSG_BACK_MAIN, 500)
        }
    }


    private var mStopFlag = false
    private var mSingleThreadExecutor = Executors.newSingleThreadExecutor()
    private fun detailLoop() {
        Log.d(TAG, "detailLoop  enter ... $mIsPaperTask")
                //进入详情页
        toast("进入文章详情")
        mStopFlag = false
        mSingleThreadExecutor.execute{
            Log.d(TAG, "detailLoop size 1 =" + mTaskStack.size)
            Log.d(TAG, "detailLoop enter detail")
            for (i in 0..5) {
                Thread.sleep(4 * 1000)
                if (mStopFlag) {
                    Log.d(TAG, "detailLoop stopSelf")
                    stopSelf()
                }
                mWebViewNode = findByClassName(rootInActiveWindow, "android.webkit.WebView")
                mWebViewNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                Log.d(TAG, "detailLoop detail scroll... $i")

            }

            Thread.sleep(2 * 1000)
            performGlobalAction(GLOBAL_ACTION_BACK)
            mHandler.sendEmptyMessageDelayed(MSG_BACK_MAIN, 2 * 1000)
            Log.d(TAG, "detailLoop GLOBAL_ACTION_BACK ....  ")
            launch (UI){
                toast("返回主页")
            }
        }
    }

    private fun refresh() {
//        var ret = clickByText(rootInActiveWindow,"刷新")
//        Log.d(TAG, "刷新数据! $ret")
//        mLoading = false
        val rececylerViewNode = findByClassName(rootInActiveWindow, "android.support.v7.widget.RecyclerView")
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
//        if ( clickByText(rootInActiveWindow,"完成") ||
//                clickByText(rootInActiveWindow,"安装") ||
//                clickByText(rootInActiveWindow,"确认") ||
//                clickByText(rootInActiveWindow,"继续") ||
//                clickByText(rootInActiveWindow,"下一步") ||
//                clickById(rootInActiveWindow, "com.android.packageinstaller:id/decide_to_continue") ||
//                clickById(rootInActiveWindow, "com.android.packageinstaller:id/action_positive") ||
//                clickByText(rootInActiveWindow,"继续安装") ||
//                clickByText(rootInActiveWindow,"打开阅读")
//        ) {
//            return true
//        }
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
