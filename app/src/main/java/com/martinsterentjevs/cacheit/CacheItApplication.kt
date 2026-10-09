package com.martinsterentjevs.cacheit

import android.app.Application
import com.martinsterentjevs.cacheit.services.websockets.NudgeHandler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class CacheItApplication : Application() {

    @Inject lateinit var nudgeHandler: NudgeHandler

    override fun onCreate() {
        super.onCreate()
        // Safe to start unconditionally regardless of login state — it just collects
        // from wsClient.nudges, which only ever emits once a connection is Ready.
        nudgeHandler.start()
    }
}