package com.tanjinc.autotool

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.graphics.Rect
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.tanjinc.autotool.utils.AccessibilityUtil
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.findTextArray
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.lang.Math.abs

/**
 * Author by tanjincheng, Date on 19-1-11.
 */
class QuTTHelper : BaseHelper() {

    private var mIsInDetail = false
    private var mIsPaperTask = false
    private var mRefresh = false
    private var mTargetNodeArray = mutableListOf<AccessibilityNodeInfo>()
    private var mCurrentIndex = 0

    private var mAdCloseBtnId = "com.jifen.qukan:id/pb"

    private val sListView = "android.support.v7.widget.RecyclerView"
    private val sDetailRoot= "android.webkit.WebView"
    private val sLoadMore = "android.widget.ProgressBar"
    private var mMainPageRoot: AccessibilityNodeInfo ?= null
    private var mIsLastNode = false
    private var mLastNodeText:String = ""
    private var mCount = 0

    companion object {
        const val TAG = "QuTTHelper"
        const val SCROLL_DELAY = 5 * 1000L
    }

    override fun getPacketName(): String? {
        return "com.jifen.qukan"
    }

    override fun autoWork(service: AutoClickService, rootNode: AccessibilityNodeInfo?, event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ) {
            timeReward(rootNode)
            closeAdDialog(rootNode)
        } else if(event.eventType ==  AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED ) {
            val data = event.parcelableData
//            if (data is Notification) {
//                val notification: Notification = data
//                notification.contentIntent.send()
//                GlobalScope.launch {
//                    Log.d(AutoClickService.TAG, "notification= "+notification.tickerText)
//                    delay(5 * 1000)
//                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
//                }
//            }
        }

        if (!mIsInDetail && isInDetail(rootNode)) {
            detailLoop(service, rootNode)
        } else {
            if (!mIsPaperTask) {
                mainPage(rootNode)
            }
        }
    }

    override fun isInDetail(rootNode: AccessibilityNodeInfo?): Boolean {
        return AccessibilityUtil.findByClassName(rootNode, sDetailRoot) != null
                && AccessibilityUtil.findByText(rootNode, "我来说两句...", strict = true) != null
    }

    override fun getFilterRegex(): Regex {
        return Regex("[0-9]+评")
    }

    private fun mainPage(rootInActiveWindow: AccessibilityNodeInfo?) {
        Log.d(TAG, "mIsInDetail=$mIsInDetail mRefresh=$mRefresh mCurrentIndex=$mCurrentIndex size=${mTargetNodeArray.size}")
        mMainPageRoot = rootInActiveWindow
        if (!mIsInDetail) {
            mTargetNodeArray.clear()
            if (mRefresh) {
                if (AccessibilityUtil.findByClassName(rootInActiveWindow, sLoadMore) != null) {
                    AccessibilityUtil.clickByText(rootInActiveWindow, "刷新", true)
                    return
                }
                val scrollViewNode = AccessibilityUtil.findByClassName(rootInActiveWindow, sListView,  true)
                if (scrollViewNode != null) {
                    if (scrollViewNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                        Log.d(TAG, "scroll success")
                        //等待2秒
                        mCurrentIndex = 0
                        mRefresh = false
                        mIsPaperTask = true
                        GlobalScope.launch {
                            delay(2 * 1000)
                            mIsPaperTask = false
                        }
                        return
                    } else {
                        Log.d(TAG, "scroll fail")
                        mRefresh = true
                    }
                }
            }
            Log.d(TAG, "mainPage ..." )
            AccessibilityUtil.findTextArray(rootInActiveWindow, getFilterRegex(), mTargetNodeArray)
            filterTargetNode(rootInActiveWindow, mTargetNodeArray)
            Log.d(TAG, "mTargetNodeArray size = ${mTargetNodeArray.size}")
            if (mTargetNodeArray.size == 0) {
                mRefresh = true
                return
            } else if (mLastNodeText == mTargetNodeArray[mTargetNodeArray.size-1].text) {
                mCount++
                if (mCount > 3) {
                    Log.d(TAG, "mTargetNodeArray 到达底部，刷新 mLastNodeText = $mLastNodeText")
                    //到达底部，刷新
                    AccessibilityUtil.clickByText(rootInActiveWindow, "刷新", true)
                    mRefresh = true
                    mCount = 0
                    return
                }
            }

            for (item in mTargetNodeArray) {
                Log.d(TAG, "readPaperTask " + item.text)
                mLastNodeText = item.text.toString()
            }

            while (mTargetNodeArray.size > 0 && mCurrentIndex < mTargetNodeArray.size) {
                var taskItem = mTargetNodeArray[mCurrentIndex]
                mCurrentIndex++

                mIsPaperTask = AccessibilityUtil.clickByNode(rootInActiveWindow, taskItem)
                Log.d(TAG, "click ${taskItem.text} $mIsPaperTask")

                if (mCurrentIndex == mTargetNodeArray.size) {
                    mCurrentIndex = 0
                    mRefresh = true
                    break
                }
                if (mIsPaperTask) {
                    Log.d(TAG, "enter " + taskItem.text)
//                    Toast.makeText(MyApplication.getApplication(), "进入详情页：${taskItem.text}", Toast.LENGTH_SHORT).show()
                    break
                } else {
                    Log.d(TAG, "clickNode false " + taskItem.text)
                }
            }
        }
    }

    private fun detailLoop(service: AutoClickService, rootNode: AccessibilityNodeInfo?) {
        //进入详情页
        GlobalScope.launch {
            mIsInDetail = true
            Log.d(TAG, "detailLoop enter detail")
            for (i in 0..5) {
                delay(SCROLL_DELAY)
                val webViewNode = AccessibilityUtil.findByClassName(rootNode, sDetailRoot)
                webViewNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                Log.d(TAG, "detailLoop detail scroll... $i")
//                AccessibilityUtil.clickByText(rootNode, "查看全文", true)

            }
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            Log.d(TAG, "detailLoop GLOBAL_ACTION_BACK ....  ")

            launch {
                delay(2 * 1000)
                Log.d(TAG, "detailLoop reset ....  ")
                mIsInDetail = false
                mIsPaperTask = false
//                mainPage(mMainPageRoot)
            }
        }
    }

    private fun filterTargetNode(rootInActiveWindow: AccessibilityNodeInfo?, targetNodeArray:MutableList<AccessibilityNodeInfo>) {
        var videoArray = mutableListOf<AccessibilityNodeInfo>()
        var zhidingArray = mutableListOf<AccessibilityNodeInfo>()

        findTextArray(rootInActiveWindow, Regex("视频"), videoArray, true)
        findTextArray(rootInActiveWindow, Regex("置顶"), zhidingArray, true)

        for (videoItem in videoArray) {
            var videoRect = Rect()
            videoItem.getBoundsInScreen(videoRect)
            var removeIndex = -1
            for (i in 0 until targetNodeArray.size) {
                var textRect = Rect()
                targetNodeArray[i].getBoundsInScreen(textRect)
                if (abs(videoRect.top - textRect.top) < 10) {
                    removeIndex = i
                }
            }
            if (removeIndex != -1) {
                Log.d(TAG, "readPaperTask remove　视频　" + targetNodeArray[removeIndex].text)
                targetNodeArray.removeAt(removeIndex)
            }
        }

        for (zhiDingItem in zhidingArray) {
            val zhiDingRect = Rect()
            zhiDingItem.getBoundsInScreen(zhiDingRect)
            var removeIndex = -1
            for (i in 0 until targetNodeArray.size) {
                var textRect = Rect()
                targetNodeArray[i].getBoundsInScreen(textRect)
                if (abs(zhiDingRect.top - textRect.top) < 10) {
                    removeIndex = i
                }
            }
            if (removeIndex != -1) {
                Log.d(TAG, "readPaperTask remove　置顶　" + targetNodeArray[removeIndex].text)
                targetNodeArray.removeAt(removeIndex)
            }
        }
    }

    //领取时段奖励
    private fun timeReward(rootInActiveWindow: AccessibilityNodeInfo?) {
        AccessibilityUtil.clickByText(rootInActiveWindow, "领取")
        AccessibilityUtil.clickByText(rootInActiveWindow, "我知道了")
    }

    private fun closeAdDialog(rootInActiveWindow: AccessibilityNodeInfo?) {
        AccessibilityUtil.clickById(rootInActiveWindow, mAdCloseBtnId)
    }
}