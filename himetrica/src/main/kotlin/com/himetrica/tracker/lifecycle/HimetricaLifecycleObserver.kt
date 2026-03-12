package com.himetrica.tracker.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.himetrica.tracker.Himetrica

internal class HimetricaLifecycleObserver(
    private val himetrica: Himetrica,
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        himetrica.onAppForeground()
    }

    override fun onStop(owner: LifecycleOwner) {
        himetrica.onAppBackground()
    }
}
