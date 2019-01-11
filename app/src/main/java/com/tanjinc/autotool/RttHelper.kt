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
 */
object RttHelper  {

    //com.expflow.reading/com.expflow.reading.activity.PopMessageActivity

    const val TAG = "RttHelper"
    const val SCROLL_DELAY = 5 * 1000L
    private var mIsInDetail = false
    private var mIsPaperTask = false
    private var mRefresh = false
    //    private var mTaskStack: Stack<AccessibilityNodeInfo> = Stack()
    private var mTargetNodeArray = mutableListOf<AccessibilityNodeInfo>()
    private var mCurrentIndex = 0

    fun autoWork(service: AutoClickService, rootNode: AccessibilityNodeInfo?, event: AccessibilityEvent) {


        if (!mIsInDetail && AccessibilityUtil.findByClassName(rootNode, "android.webkit.WebView") != null) {
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

//            mTaskStack.clear()
            mTargetNodeArray.clear()

            if (mRefresh) {

                val listArray = rootInActiveWindow?.findAccessibilityNodeInfosByViewId("android:id/list")
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

//            var paperArray = mutableListOf<AccessibilityNodeInfo>()
            AccessibilityUtil.findTextArray(rootInActiveWindow, Regex("天[0-9][0-9]:[0-9][0-9]"), mTargetNodeArray)

            Log.d(TAG, "mTargetNodeArray size = ${mTargetNodeArray.size}")
            for (item in mTargetNodeArray) {
                Log.d(TAG, "readPaperTask " + item.text)
//                mTaskStack.push(item)
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
                delay(SCROLL_DELAY)
                val webViewNode = AccessibilityUtil.findByClassName(rootNode, "android.webkit.WebView")
                webViewNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                Log.d(TAG, "detailLoop detail scroll... $i")

            }
            delay(2 * 1000)
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            Log.d(TAG, "detailLoop GLOBAL_ACTION_BACK ....  ")


            launch {
                delay(2 * 1000)
                Log.d(TAG, "detailLoop reset ....  ")
//                launch(UI){
//                    Toast.makeText(MyApplication.getApplication(), "返回首页！", Toast.LENGTH_SHORT).show()
//                }
                mIsInDetail = false
                mIsPaperTask = false
            }
        }
    }

}