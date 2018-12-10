package com.tanjinc.autotool

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.clickById
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.clickByNode
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.clickByText
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.findByText
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.findByViewName
import com.tanjinc.autotool.utils.AccessibilityUtil.Companion.findTextArray
/**
 * Author by tanjincheng, Date on 18-12-3.
 * 魅族新闻资讯
 */
class NewsHelper  {
    companion object {
        const val TAG  = "NewsHelper"
        var mWebViewNodeInfo : AccessibilityNodeInfo ?= null
        var mIsScrolling = false
        fun autoWork(rootNodeInfo: AccessibilityNodeInfo) {
            val webViewNode = findByViewName(rootNodeInfo, "android.webkit.WebView")

            if (mWebViewNodeInfo == null) {
                mWebViewNodeInfo = webViewNode
            } else {
                if (mWebViewNodeInfo == webViewNode) {
                    return
                }
            }

            if (webViewNode != null && webViewNode.isScrollable && !mIsScrolling) {
                Thread {
                    synchronized(mIsScrolling) {
                        mIsScrolling = true
                        for (i in 0..6) {
                            Thread.sleep(5 * 1000)
                            webViewNode?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                            Log.d(TAG, "scroll ..." + Thread.currentThread().name)
                        }
                        mIsScrolling = false
                    }
                }.start()
            }

            val relateNode = findByText(rootNodeInfo, "相关推荐")

        }
    }
}