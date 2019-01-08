package com.tanjinc.autotool

import android.accessibilityservice.AccessibilityService
import android.nfc.Tag
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.tanjinc.autotool.utils.AccessibilityUtil
import com.tanjinc.autotool.utils.SharePreferenceUtil
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.*

object QttHelper {

    const val TAG = "QttHelper"
    private var mIsComment:Boolean = false //评论
    private var mIsCollect:Boolean = false //收藏
    private var mIsShare:Boolean = false //分享
    private var mIsShareSuccess = false //分享成功

    private var randomText = mutableListOf<String>(
            "很好，试试看",
            "感谢分享",
            "谢谢",
            "是真的吗？",
            "很好")

    fun autoWork(service: AutoClickService, rootNode: AccessibilityNodeInfo?, event: AccessibilityEvent) {

        if (SharePreferenceUtil.getBoolean(Constants.FLOW_TASK)){



            if (!mIsComment) {
                //评论
                AccessibilityUtil.clickByText(rootNode, "我来说两句...")
                val editNode = AccessibilityUtil.findByClassName(rootNode, "android.widget.EditText", false)
                AccessibilityUtil.inputText(editNode, commentText)
                //防止评论过快
                if (System.currentTimeMillis() - SharePreferenceUtil.getLong(Constants.LAST_COMMENT_TIME) < 15 * 1000) {
                    return
                }
                mIsComment = AccessibilityUtil.clickByText(rootNode, "发送")
                SharePreferenceUtil.putLong(Constants.LAST_COMMENT_TIME, System.currentTimeMillis())
            }

            if (mIsComment) {
                var images = mutableListOf<AccessibilityNodeInfo>()
                AccessibilityUtil.findByClassName(rootNode, "android.widget.ImageView", images)

                var collectNode:AccessibilityNodeInfo ?= null
                var shareNode: AccessibilityNodeInfo ?= null
                if (images.size == 4) {
                    collectNode = images[1]
                    shareNode = images[2]
                }

                //收藏
                if (!mIsCollect && collectNode != null) {
                    mIsCollect = AccessibilityUtil.clickByNode(rootNode, collectNode)
                }

                //分享
                if (!mIsShare && shareNode != null) {
                    mIsShare = AccessibilityUtil.clickByNode(rootNode, shareNode)
                }
                if (!mIsShareSuccess) {
                    mIsShareSuccess = AccessibilityUtil.clickByText(rootNode, "复制链接", true)
                }
            }

            if (mIsComment && mIsCollect && mIsShare) {
                if (isInDetail(rootNode)) {
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    launch {
                        delay(2*1000)
                        mIsComment = false
                        mIsCollect = false
                        mIsShare = false
                        mIsShareSuccess = false
                    }
                }
            }
        }
    }

    private fun isInDetail(rootNode: AccessibilityNodeInfo?):Boolean {
        return AccessibilityUtil.findByClassName(rootNode, "android.webkit.WebView") != null
    }
    private val commentText: String
        get() = randomText[Random().nextInt(randomText.size)]
}