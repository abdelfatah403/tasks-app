package com.taskflow.app

import android.app.Application
import com.taskflow.app.data.local.TaskFlowDatabase

class TaskFlowApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Warm up local Room database on startup
        TaskFlowDatabase.getInstance(this)
    }
}
