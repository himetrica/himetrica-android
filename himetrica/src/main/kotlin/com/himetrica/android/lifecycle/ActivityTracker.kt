package com.himetrica.android.lifecycle

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.himetrica.android.Himetrica

internal class ActivityTracker(
    private val himetrica: Himetrica,
) : Application.ActivityLifecycleCallbacks {

    private var currentActivity: String? = null

    override fun onActivityResumed(activity: Activity) {
        val name = getScreenName(activity)
        if (name != currentActivity) {
            currentActivity = name
            himetrica.trackScreen(name)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}

    private fun getScreenName(activity: Activity): String {
        // Use Activity title if set, otherwise use class name (strip "Activity" suffix)
        val title = activity.title?.toString()
        if (!title.isNullOrBlank() && title != activity.packageName) {
            return title
        }
        return activity.javaClass.simpleName.removeSuffix("Activity")
    }
}
