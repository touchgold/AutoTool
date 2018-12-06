package com.tanjinc.autotool.utils

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.tanjinc.autotool.AutoClickService
import java.lang.Exception

/**
 * Author by tanjincheng, Date on 18-12-4.
 */
class AccessibilityUtil {
    companion object {
        val TAG = "AccessibilityUtil"
         fun clickByNode(rootNodeInfo: AccessibilityNodeInfo?, nodeInfo: AccessibilityNodeInfo): Boolean {
             try {
                 if (nodeInfo.isClickable) {
                     return nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                 } else if (nodeInfo.parent != null){
                     return nodeInfo.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                 }
             } catch (e: Exception) {
                 Log.e(TAG, e.toString())
             }
            return false
        }
        fun clickByText(rootNodeInfo: AccessibilityNodeInfo?, text:String) :Boolean{
            if (rootNodeInfo == null) {
                return false
            }
            try {
                val targetNodeInfo = rootNodeInfo?.findAccessibilityNodeInfosByText(text)
                var clicked = false
                if(targetNodeInfo != null && targetNodeInfo.size> 0 ) {
                    for (i in 0 until targetNodeInfo.size) {
                        if (targetNodeInfo[i]?.text == text) {
                            if (targetNodeInfo[i].isClickable) {
                                clicked = targetNodeInfo[i].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            }
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
            return false
        }

         fun clickById(rootNodeInfo: AccessibilityNodeInfo?, id:String?) :Boolean{
            if (rootNodeInfo == null) {
                return false
            }
            var clicked = false
            try {

                val targetNodeInfo:MutableList<AccessibilityNodeInfo> ?= rootNodeInfo.findAccessibilityNodeInfosByViewId(id)
                if (targetNodeInfo?.size!! > 0) {
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

         fun clickByRule(rootNodeInfo: AccessibilityNodeInfo?) :Boolean{
            var targetNode = findByText(rootNodeInfo, "人试玩", "")
            if (targetNode != null) {
                targetNode.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            return false
        }


         fun findByViewName(rootNodeInfo: AccessibilityNodeInfo?, text: String) : AccessibilityNodeInfo?{
            if (rootNodeInfo == null) {
                return null
            }
            var targetNodeInf:AccessibilityNodeInfo ?= null
            try {

                for (i in 0 until rootNodeInfo.childCount) {
                    var nodeI = rootNodeInfo.getChild(i)
                    if (nodeI != null) {
                        if (nodeI.className != null && nodeI.className.contains(text) && nodeI.isScrollable) {
                            Log.d(TAG, " findByViewName className= " + nodeI.className)
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


         fun findTextArray(rootNodeInfo: AccessibilityNodeInfo?, text: String, textArray: MutableList<AccessibilityNodeInfo>, end:Boolean = false){
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
         fun findByText(rootNodeInfo: AccessibilityNodeInfo?, text: String, excText:String = "null", end:Boolean = false) : AccessibilityNodeInfo?{
            if (rootNodeInfo == null) {
                return null
            }
            var targetNodeInf:AccessibilityNodeInfo ?= null
            try {

                for (i in 0 until rootNodeInfo.childCount) {
                    var nodeI = rootNodeInfo.getChild(if(!end) i else rootNodeInfo.childCount - 1- i)
                    if (nodeI != null) {
                        if (nodeI.text != null && nodeI.text.contains(text) && !nodeI.text.contains(excText)) {
//                            Log.d(TAG, " findByText success text= " + nodeI.text)
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

    }
}