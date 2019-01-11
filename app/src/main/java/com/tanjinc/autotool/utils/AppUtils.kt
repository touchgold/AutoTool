package com.tanjinc.autotool.utils

import android.content.Context
import java.nio.file.Files.size
import android.content.pm.PackageManager



/**
 * Author by tanjincheng, Date on 19-1-11.
 */
object AppUtils {
    fun isAvilible(context: Context, packageName: String): Boolean {
        val packageManager = context.packageManager//获取packagemanager
        val pinfo = packageManager.getInstalledPackages(0)//获取所有已安装程序的包信息
        val pName = ArrayList<String>()//用于存储所有已安装程序的包名
        //从pinfo中将包名字逐一取出，压入pName list中
        if (pinfo != null) {
            for (i in pinfo.indices) {
                val pn = pinfo[i].packageName
                pName.add(pn)
            }
        }
        return pName.contains(packageName)//判断pName中是否有目标程序的包名，有TRUE，没有FALSE
    }
}