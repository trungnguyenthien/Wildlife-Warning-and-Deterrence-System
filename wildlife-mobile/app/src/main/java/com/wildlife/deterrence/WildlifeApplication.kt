package com.wildlife.deterrence

import android.app.Application
import android.util.Log

class WildlifeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createChannels(this)
        Log.d("NotifChannel", "Channels created")
    }
}
