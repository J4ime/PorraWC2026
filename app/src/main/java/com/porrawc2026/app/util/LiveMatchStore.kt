package com.porrawc2026.app.util

import com.porrawc2026.app.ui.screens.home.GoalEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveMatchStore @Inject constructor() {
    val liveMinutes = mutableMapOf<Int, String>()
    val goalScorers = mutableMapOf<Int, Pair<List<GoalEvent>, List<GoalEvent>>>()
}
