package com.porrawc2026.app

import android.app.Application
import com.porrawc2026.app.util.GoalNotifier
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PorraWCApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GoalNotifier.init(this)
    }
}
