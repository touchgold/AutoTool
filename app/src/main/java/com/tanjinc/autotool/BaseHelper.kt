package com.tanjinc.autotool

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Author by tanjincheng, Date on 19-1-10.
 */
abstract class BaseHelper {

    abstract fun getPacketName():String?
    abstract fun autoWork(service: AutoClickService, rootNode: AccessibilityNodeInfo?, event: AccessibilityEvent)

    //判断是否在详情页
    abstract fun isInDetail(rootNode: AccessibilityNodeInfo?):Boolean

    //首页匹配规则
    abstract fun getFilterRegex():Regex
}