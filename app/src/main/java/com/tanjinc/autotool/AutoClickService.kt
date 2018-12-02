package com.tanjinc.autotool

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.tanjinc.autotool.utils.PrintUtils
import com.tanjinc.autotool.utils.ProcessUtils
import com.tanjinc.autotool.utils.SharePreferenceUtil
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.lang.Exception
import java.util.*
import kotlin.math.abs

class AutoClickService : AccessibilityService() {
    val TAG = "AutoClickService"
    val mQutoutiaoPackage = "com.jifen.qukan" //趣头条包名
    val mTargetPackageName = "com.tanjinc.autotool"
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

    private var mWebViewNode: AccessibilityNodeInfo ?= null

    private var mIsShowResent = false
    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what) {
                MSG_REFRESH_VIDEO -> {
                    if(clickByText("刷新")) {
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
        var rootInActiveWindow = rootInActiveWindow
        if (event != null && rootInActiveWindow != null) {
            when(event.packageName) {
                mQutoutiaoPackage -> {
                    timeReward()
                    closeAdDialog()

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
                                    delay(2 * 1000)
                                    Log.d(TAG, "notification= "+notification.tickerText)
                                    toast("趣头条推送:" + notification.tickerText)
                                    delay(5 * 1000)
                                    performGlobalAction(GLOBAL_ACTION_BACK)
                                    mIsNotificationTask = false
                                }
                            }
                        }
                    }

                    if (SharePreferenceUtil.getBoolean(Constants.VIDEO_TASK)) {
                        if (clickByText("小视频")) {
                            mHandler.sendEmptyMessageDelayed(100, 20 * 1000)
                            return
                        }
                    }


                    //看文章
                    if (SharePreferenceUtil.getBoolean(Constants.PAPER_TASK)) {
                        readPaperTask(rootInActiveWindow)
                        return
                    }

                    if (SharePreferenceUtil.getBoolean(Constants.SHIWAN_TASK)) {
                        clickByText("我的")
                        clickByText("推荐应用")
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
                        clickByText("进行中...")
                    }


                    clickByText("立即试玩")
                    installTask()
                    if (!clickByText("领取奖励")) {
                        findByText(rootInActiveWindow, "打开", "null", true)?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }

                    if (SharePreferenceUtil.getBoolean(Constants.QIANDAO_TASK)) {
                        recommendAppTask()
                        clickByText("我的福利")

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
                    val readPage = false
                    if (readPage) {
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
                        clickByText("趣头条")
                        mIsShowResent = false
                    }
                }

                else -> {
                    if (mIsInstalling) {
                        val currentPackageName = ProcessUtils.getTopActivityPackageName(MyApplication.getApplication())
                        Log.d(TAG, "currentProcess $currentPackageName")
                    }
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
//        clickByText("头条")
        if (mLoading) {
            Log.d(TAG, "readPaperTask isloading")
            return
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
                    mPaperArray.removeAt(removeIndex)
                }
            }
        }

        if (mPaperArray.size > 0) {
            if (mCurrentPaperIndex < mPaperArray.size) {
                var rect = Rect()
                var item = mPaperArray[mCurrentPaperIndex]
                item.getBoundsInScreen(rect)
                Log.d(TAG, " rect " + rect.toString() + " ==" + item.text)

                if (!mIsPaperTask) {
                    mIsPaperTask = clickByNode(item)
                    if (mIsPaperTask) {
                        mCurrentPaperIndex++
                        mHandler.sendEmptyMessageDelayed(MSG_SCROLL, 5 * 1000)
                        launch {
                            delay(30 * 1000)
                            mHandler.removeMessages(MSG_SCROLL)
                            performGlobalAction(GLOBAL_ACTION_BACK)
                            mIsPaperTask = false
                            mWebViewNode = null
                            delay(1* 1000)
                            readPaperTask(rootInActiveWindow)
                        }
                    }
                }
            } else {
                if (!mIsPaperTask) {
                    mLoading = true
                    mPaperArray.clear()
                    mCurrentPaperIndex = 0
//                    clickByText("刷新")
                    val rececylerViewNode = findByViewName(rootInActiveWindow, "android.support.v7.widget.RecyclerView")
                    rececylerViewNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    launch {
                        delay(3* 1000) //延时3秒等待加载
                        mLoading = false
                        rececylerViewNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                    }
                }
            }
        }


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
        if ( clickByText("完成") ||
                clickByText("安装") ||
                clickByText("确认") ||
                clickByText("继续") ||
                clickByText("下一步") ||
                clickById("com.android.packageinstaller:id/decide_to_continue") ||
                clickById("com.android.packageinstaller:id/action_positive") ||
                clickByText("继续安装") ||
                clickByText("打开阅读")
        ) {
            return true
        }
        return false
    }
    //领取时段奖励
    private fun timeReward() {
        clickByText("领取")
        clickByText("我知道了")
    }

    private fun closeAdDialog() {
        clickById(mAdCloseBtnId)
    }

    private fun recommendAppTask() {
        clickByText("我的")
        clickByText("推荐应用")

    }


    private fun clickByNode(nodeInfo: AccessibilityNodeInfo): Boolean {
        if (nodeInfo.isClickable) {
            return nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else if (nodeInfo.parent != null){
            return nodeInfo.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        return false
    }
    private fun clickByText(text:String) :Boolean{
        if (rootInActiveWindow == null) {
            return false
        }
        try {
            val targetNodeInfo = rootInActiveWindow?.findAccessibilityNodeInfosByText(text)
            var clicked = false
            if(targetNodeInfo != null && targetNodeInfo.size> 0 ) {
                for (i in 0 until targetNodeInfo.size) {
                    if (targetNodeInfo[i]?.text == text) {
                        clicked = targetNodeInfo[i].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        if (!clicked) {
                            clicked = targetNodeInfo[i]?.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)!!
                        }
                        if (clicked) {
                            Log.d(TAG, "clickByText click success $text")
                            return true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
        Log.d(TAG, "clickByText click fail $text")

        return false
    }

    private fun clickById(id:String?) :Boolean{
        if (rootInActiveWindow == null) {
            return false
        }
        var clicked = false
        try {

            val targetNodeInfo = rootInActiveWindow.findAccessibilityNodeInfosByViewId(id)
            if (targetNodeInfo.size > 0) {
                if (targetNodeInfo[0].isClickable) {
                    clicked = targetNodeInfo[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)

                } else {
                    clicked = targetNodeInfo[0].parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
        } catch (e:Exception) {
            clicked = false
        }
        return clicked
    }

    private fun clickByRule() :Boolean{
        var targetNode = findByText(rootInActiveWindow, "人试玩", "")
        if (targetNode != null) {
            targetNode.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            return true
        }
        return false
    }


    private fun findByViewName(rootNodeInfo: AccessibilityNodeInfo?, text: String) : AccessibilityNodeInfo?{
        if (rootNodeInfo == null) {
            return null
        }
        var targetNodeInf:AccessibilityNodeInfo ?= null
        try {

            for (i in 0 until rootNodeInfo.childCount) {
                var nodeI = rootNodeInfo.getChild(i)
                if (nodeI != null) {
                    Log.d(TAG, " findByViewName className= " + nodeI.className)
                    if (nodeI.className != null && nodeI.className.contains(text) && nodeI.isScrollable) {
                        targetNodeInf = nodeI
                        break
                    } else {
                        targetNodeInf = findByViewName(nodeI, text)
                    }
                }
                if (targetNodeInf != null) {
                    return targetNodeInf
                }
            }

        } catch (exception:Exception) {

        }
        return targetNodeInf
    }


    private fun findTextArray(rootNodeInfo: AccessibilityNodeInfo?, text: String, textArray: MutableList<AccessibilityNodeInfo>, end:Boolean = false){
        if (rootNodeInfo == null) {
            return
        }

        for (i in 0 until rootNodeInfo.childCount) {
            var nodeI = rootNodeInfo.getChild(if(!end) i else rootNodeInfo.childCount - 1- i)
            if (nodeI != null) {
                if (nodeI.text != null && nodeI.text.contains(text) ) {
                    Log.d(TAG, " findByText success text= " + nodeI.text)
                    textArray.add(nodeI)
                    break
                } else {
                    findTextArray(nodeI, text, textArray, end)
                }
            }
        }
    }
    //遍历查找
    private fun findByText(rootNodeInfo: AccessibilityNodeInfo?, text: String, excText:String = "null", end:Boolean = false) : AccessibilityNodeInfo?{
        if (rootNodeInfo == null) {
            return null
        }
        var targetNodeInf:AccessibilityNodeInfo ?= null
        try {

            for (i in 0 until rootNodeInfo.childCount) {
                var nodeI = rootNodeInfo.getChild(if(!end) i else rootNodeInfo.childCount - 1- i)
                if (nodeI != null) {
                    if (nodeI.text != null && nodeI.text.contains(text) && !nodeI.text.contains(excText)) {
                        Log.d(TAG, " findByText success text= " + nodeI.text)
                        targetNodeInf = nodeI
                        break
                    } else {
                        targetNodeInf = findByText(nodeI, text, excText)
                    }
                }
                if (targetNodeInf != null) {
                    return targetNodeInf
                }
            }

        } catch (exception:Exception) {

        }
        return targetNodeInf
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
