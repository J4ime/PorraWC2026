package com.porrawc2026.app.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalEventBus @Inject constructor() {
    private val _goalScored = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val goalScored: SharedFlow<Unit> = _goalScored.asSharedFlow()

    fun notifyGoal() {
        val emitted = _goalScored.tryEmit(Unit)
        if (!emitted) {
            LogManager.log("GoalEventBus", "Failed to emit goal event - buffer full or no subscribers")
        }
    }
}
