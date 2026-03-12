package com.himetrica.tracker

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import java.util.Locale

internal class DeviceInfo(context: Context) {
    val screenWidth: Int
    val screenHeight: Int
    val appVersion: String
    val osVersion: String = Build.VERSION.RELEASE
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}"
    val locale: String = Locale.getDefault().toString()

    init {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = wm.currentWindowMetrics
            val bounds = windowMetrics.bounds
            screenWidth = bounds.width()
            screenHeight = bounds.height()
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
        }

        appVersion = try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.versionName ?: "1.0.0"
        } catch (_: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }
}
