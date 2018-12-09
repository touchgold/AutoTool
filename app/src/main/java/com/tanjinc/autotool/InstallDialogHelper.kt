package com.tanjinc.autotool

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.tanjinc.autotool.utils.AccessibilityUtil
import com.tanjinc.autotool.utils.ProcessUtils
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.lang.Exception

/**
 * Author by tanjincheng, Date on 18-12-7.
 */

data class TaskBean(
        val taskName:String,
        val packName:String,
        val clickBtn:String,
        var finished:Boolean = false,
        var delay:Int = 0)

class InstallDialogHelper {
    companion object {
        val TAG = "InstallDialogHelper"

        var mTaskList = mutableListOf<TaskBean>()

        fun reset() {
            mTaskList.clear()
            mTaskList.add(TaskBean("1", "com.jifen.qukan", "立即试玩/打开试玩/打开注册并试玩/打开使用", false, 3 * 60 * 1000))
            mTaskList.add(TaskBean("2", "com.android.packageinstaller", "继续", false, 3*1000))
            mTaskList.add(TaskBean("2", "com.android.packageinstaller", "完成"))
//            mTaskList.add(TaskBean("install_success", "com.jifen.qukan", "打开试玩/打开注册并试玩"))
            mTaskList.add(TaskBean("4", "com.jifen.qukan", "领取奖励"))

        }
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

        var allFinished = false
        fun autoInstall(service: AutoClickService, rootNode: AccessibilityNodeInfo?, event: AccessibilityEvent, haveInstalled:Boolean = false) {
            try {

                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

                    if (mTaskList.size == 0) {
                        reset()
                    }
                    for (item in mTaskList) {
                        if (!item.finished && event.packageName == item.packName) {

                            if (item.clickBtn.contains("/")) {
                                val clickBtns = item.clickBtn.split("/")
                                for (btn in clickBtns) {
                                    item.finished = AccessibilityUtil.clickByText(rootNode, btn, true)
                                    if (item.finished) {
                                        Log.d(TAG, "click success ${item.clickBtn} --> $btn")
                                        launch {
                                            delay(1*1000)
                                            val intent = Intent()
                                            intent.action = MainActivity.MSG_START_QUTOUTIAO
                                            MyApplication.getApplication().sendBroadcast(intent)
                                            Log.d(TAG, " start main activity ... ")
                                        }
                                        return
                                    }
                                }
                            } else {
                                item.finished = AccessibilityUtil.clickByText(rootNode, item.clickBtn, true)
                                Log.d(TAG, "click ${item.clickBtn} ${item.finished}")
                            }

                            if (item.packName == "com.android.packageinstaller") {
                                launch {
                                    delay(item.delay)
                                    item.finished = AccessibilityUtil.clickByText(rootNode, item.clickBtn, true)
                                }
                            }


                            break
                        }
                    }
                    allFinished = true
                    for (item in mTaskList) {
                        if (!item.finished) {
                            allFinished = false
                        }
                    }
                    if (allFinished) {
                        reset()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }

        }
    }
}