package com.tanjinc.autotool

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.tanjinc.autotool.utils.AccessibilityUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Author by tanjincheng, Date on 19-1-10.
 * com.zhangku.qukandian/com.zhangku.qukandian.activitys.MainActivity 趣看点
 */
class MyttHelper : BaseHelper() {

    private var mIsInDetail = false
    private var mIsPaperTask = false
    private var mRefresh = false
    private var mTargetNodeArray = mutableListOf<AccessibilityNodeInfo>()
    private var mCurrentIndex = 0


    companion object {
        const val TAG = "MyttHelper"
        const val SCROLL_DELAY = 5 * 1000L
    }

    private val sListView = "com.zhangku.qukandian:id/base_refresh_recyclerview"
    private val sDetailRoot= "android.support.v7.widget.RecyclerView"

    override fun getPacketName(): String? {
        return "com.zhangku.qukandian"
    }

    override fun isInDetail(rootNode: AccessibilityNodeInfo?): Boolean {
        return AccessibilityUtil.findByClassName(rootNode, sDetailRoot) != null
                && AccessibilityUtil.findByText(rootNode, "字体调节", strict = true) != null
    }

    override fun getFilterRegex(): Regex {
        return Regex("[0-9]阅读")
    }

    override fun autoWork(service: AutoClickService, rootNode: AccessibilityNodeInfo?, event: AccessibilityEvent) {

        if (!mIsInDetail && isInDetail(rootNode)) {
            detailLoop(service, rootNode)
        } else {
            if (!mIsPaperTask) {
                mainPage(rootNode)
            }
        }
    }


    private fun mainPage(rootInActiveWindow: AccessibilityNodeInfo?) {
        Log.d(TAG, "mIsInDetail=$mIsInDetail mCurrentIndex=$mCurrentIndex size=${mTargetNodeArray.size}")
        if (!mIsInDetail) {
            mTargetNodeArray.clear()
            if (mRefresh) {
                val listArray = rootInActiveWindow?.findAccessibilityNodeInfosByViewId(sListView)
                if (listArray != null) {
                    for (listNode in listArray) {
                        if (listNode != null && listNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)) {
                            Log.d(TAG, "scroll success")
                            //等待2秒
                            mCurrentIndex = 0
                            mRefresh = false
                            mIsPaperTask = true
                            GlobalScope.launch {
                                delay(3 * 1000)
                                mIsPaperTask = false
                            }
                            return
                        } else {
                            Log.d(TAG, "scroll fail")
                            mRefresh = true
                        }
                    }
                }
            }
            Log.d(TAG, "mainPage ..." )
            AccessibilityUtil.findTextArray(rootInActiveWindow, getFilterRegex(), mTargetNodeArray)

            Log.d(TAG, "mTargetNodeArray size = ${mTargetNodeArray.size}")
            for (item in mTargetNodeArray) {
                Log.d(TAG, "readPaperTask " + item.text)
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
                    Toast.makeText(MyApplication.getApplication(), "进入详情页：${taskItem.text}", Toast.LENGTH_SHORT).show()
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
                delay(Companion.SCROLL_DELAY)
                val webViewNode = AccessibilityUtil.findByClassName(rootNode, sDetailRoot)
                webViewNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                Log.d(TAG, "detailLoop detail scroll... $i")
                AccessibilityUtil.clickByText(rootNode, "查看全文", true)

            }
            delay(2 * 1000)
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            Log.d(TAG, "detailLoop GLOBAL_ACTION_BACK ....  ")

            launch {
                delay(2 * 1000)
                Log.d(TAG, "detailLoop reset ....  ")
                mIsInDetail = false
                mIsPaperTask = false
            }
        }
    }
}