package com.himetrica.android.lifecycle

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.himetrica.android.Himetrica

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
