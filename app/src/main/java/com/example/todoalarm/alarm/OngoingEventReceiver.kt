package com.example.todoalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoalarm.TodoApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class OngoingEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getLongExtra(OngoingEventNotifier.EXTRA_EVENT_ID, -1L)
        if (eventId <= 0L) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as? TodoApplication
                val event = app?.repository?.getTodo(eventId)
                when (intent.action) {
                    OngoingEventNotifier.ACTION_START -> {
                        if (event == null) {
                            OngoingEventNotifier.cancelAll(context.applicationContext, eventId)
                        } else {
                            OngoingEventNotifier.handleStart(context.applicationContext, event)
                        }
                    }
                    OngoingEventNotifier.ACTION_END -> {
                        OngoingEventNotifier.cancelAll(context.applicationContext, eventId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
