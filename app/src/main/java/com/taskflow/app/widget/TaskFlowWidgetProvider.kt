package com.taskflow.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.taskflow.app.MainActivity
import com.taskflow.app.R
import com.taskflow.app.data.local.TaskFlowDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TaskFlowWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            updateSingleWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TaskFlowWidgetProvider::class.java))
            onUpdate(context, manager, ids)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.taskflow.app.ACTION_REFRESH_WIDGET"
        const val ACTION_QUICK_ADD = "com.taskflow.app.ACTION_QUICK_ADD"
        const val ACTION_VOICE_ADD = "com.taskflow.app.ACTION_VOICE_ADD"

        fun requestWidgetUpdate(context: Context) {
            val intent = Intent(context, TaskFlowWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            context.sendBroadcast(intent)
        }

        private fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val pendingResult = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            pendingResult.launch {
                val dao = TaskFlowDatabase.getInstance(context).taskFlowDao()
                val topTasks = dao.getTopActiveTasksForWidget()

                val views = RemoteViews(context.packageName, R.layout.widget_taskflow)
                views.setTextViewText(
                    R.id.widget_subtitle,
                    if (topTasks.isEmpty()) "All tasks completed for today! 🎉"
                    else "${topTasks.size} active priority tasks"
                )

                val formattedList = if (topTasks.isEmpty()) {
                    "Tap '+ Quick Add' or '🎤 Voice' to capture a new task."
                } else {
                    topTasks.take(5).joinToString("\n") { t ->
                        val star = if (t.isStarred) "★ " else ""
                        val time = t.formattedTime?.let { " ($it)" } ?: ""
                        "• [${t.priority.shortTag}] $star${t.title}$time"
                    }
                }
                views.setTextViewText(R.id.widget_task_list, formattedList)

                // Open App Intent
                val openIntent = Intent(context, MainActivity::class.java)
                val openPending = PendingIntent.getActivity(
                    context,
                    100,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openPending)

                // Quick Add Intent
                val quickAddIntent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_QUICK_ADD
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val quickAddPending = PendingIntent.getActivity(
                    context,
                    101,
                    quickAddIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_add, quickAddPending)

                // Voice Quick Add Intent
                val voiceIntent = Intent(context, MainActivity::class.java).apply {
                    action = ACTION_VOICE_ADD
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val voicePending = PendingIntent.getActivity(
                    context,
                    102,
                    voiceIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_btn_voice, voicePending)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }
}
