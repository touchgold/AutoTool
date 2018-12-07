package com.tanjinc.autotool

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.tanjinc.autotool.utils.AccessibilityUtil

/**
 * Author by tanjincheng, Date on 18-12-7.
 */
class InstallDialogHelper {
    companion object {
        val TAG = "InstallDialogHelper"


        /**
         * packageName:com.jifen.qukan
            event type:TYPE_WINDOW_STATE_CHANGED
            text:+1080金币
            text:跳跳跳方块
            text:首次下载安装
            text:返回趣头条,点击打开试玩3分钟
            text:再次返回趣头条,领取金币
            text:立即试玩
            text:领取奖励
         */
        fun isInstallDialog(event: AccessibilityEvent):Boolean {
            if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                return false
            }

            if (event.text.size == 7 && event.text[event.text.size-1] == "领取奖励") {
                return true
            }

            return false
        }

        fun autoInstall(rootNode: AccessibilityNodeInfo) {
            AccessibilityUtil.clickByText(rootNode, "立即试玩")
        }
    }
}