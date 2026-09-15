package com.martinsterentjevs.cacheit.di.controllers

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class NoteAutosaveController(
    private val scope: CoroutineScope,
    private val getInactivityDurationMs: suspend () -> Long,
    private val onAutosave: suspend () -> Unit
) {
    private var timerJob: Job? = null

    fun onEditInteraction() = rescheduleTimer()
    fun onManualSave() = rescheduleTimer()

    fun onScreenOff() {
        timerJob?.cancel()
        scope.launch { onAutosave() }
    }

    fun dispose() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun rescheduleTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            delay(getInactivityDurationMs().milliseconds)
            onAutosave()
        }
    }
}