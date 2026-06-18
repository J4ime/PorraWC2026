package com.porrawc2026.app.util

import com.porrawc2026.app.ui.screens.home.GoalEvent
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveMatchStore @Inject constructor() {
    val liveMinutes = ConcurrentHashMap<Int, String>()
    val goalScorers = ConcurrentHashMap<Int, Pair<List<GoalEvent>, List<GoalEvent>>>()
}
