package com.example.todoalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.todoalarm.TodoApplication
import kotlinx.coroutines.launch

/**
 * 处理正向计时通知栏的动作按钮：暂停、继续、结束。
 */
class ActivityTimerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val app = context.applicationContext as? TodoApplication ?: return

        app.applicationScope.launch {
            when (action) {
                ACTION_PAUSE_TIMER -> {
                    app.repository.pauseActivityTimer()
                    ActivityTimerService.refresh(context)
                }
                ACTION_RESUME_TIMER -> {
                    app.repository.resumeActivityTimer()
                    ActivityTimerService.refresh(context)
                }
                ACTION_FINISH_TIMER -> {
                    app.repository.finishActivityTimer()
                    ActivityTimerService.stop(context)
                }
            }
        }
    }

    companion object {
        const val ACTION_PAUSE_TIMER = "com.example.todoalarm.ACTION_PAUSE_TIMER"
        const val ACTION_RESUME_TIMER = "com.example.todoalarm.ACTION_RESUME_TIMER"
        const val ACTION_FINISH_TIMER = "com.example.todoalarm.ACTION_FINISH_TIMER"
    }
}
