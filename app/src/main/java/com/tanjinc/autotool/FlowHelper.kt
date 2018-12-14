package com.tanjinc.autotool

import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.tanjinc.autotool.utils.AccessibilityUtil
import android.os.Bundle
import android.util.Log
import com.tanjinc.autotool.utils.SharePreferenceUtil
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch


/**
 * Author by tanjincheng, Date on 18-12-13.
 */
object FlowHelper {
    const val TAG = "FlowHelper"
    val tag = "tag_test"
    private var mSearchSuccess = false
    private var mFlowSuccess = false
    private var mEditNode:AccessibilityNodeInfo ?= null
    private var mSearchNode: AccessibilityNodeInfo ?= null

    fun autoFlow(service: AutoClickService, rootNode: AccessibilityNodeInfo?, event: AccessibilityEvent) {
        if (!mSearchSuccess || !mFlowSuccess) {

//            AccessibilityUtil.clickByText(rootNode, "搜索你感兴趣的内容", true)

            mEditNode = AccessibilityUtil.findByClassName(rootNode, "android.widget.EditText")
            mSearchNode = AccessibilityUtil.findByText(rootNode, "搜索", strict = true)

            Log.d(TAG, "mSearchSuccess = $mSearchSuccess mFlowSuccess= $mFlowSuccess")
            val arguments = Bundle()
            if (mSearchNode != null && mEditNode != null && !mSearchSuccess) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,"养生老谭")
                    mEditNode?.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                    mSearchSuccess = mSearchNode?.performAction(AccessibilityNodeInfo.ACTION_CLICK)!!
                    return
                }
            }

            //关注
            if (!mFlowSuccess) {
                var haveFlow = SharePreferenceUtil.getBoolean(Constants.FLOW_SUCCESS)
                var flowNodeArray = mutableListOf<AccessibilityNodeInfo>()
                AccessibilityUtil.findTextArray(rootNode, Regex("关注"), flowNodeArray, true)
                Log.d(TAG, "find 关注 " + flowNodeArray.size)
                if (flowNodeArray.size > 0) {
                    mFlowSuccess = AccessibilityUtil.clickByNode(rootNode, flowNodeArray[0])
                    SharePreferenceUtil.putBoolean(Constants.FLOW_SUCCESS, mFlowSuccess)
                    Log.d(TAG, "mFlowSuccess= $mFlowSuccess")
                }
                AccessibilityUtil.clickByText(rootNode, "养生老谭", true)
            }
        }
    }
}