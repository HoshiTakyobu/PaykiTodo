package com.example.todoalarm.alarm

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.example.todoalarm.TodoApplication
import com.example.todoalarm.data.AiReportPolicy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/** OS-backed cleanup when the process is absent; reads enforce expiry even if a job is delayed. */
class AiReportExpiryJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            val app = application as TodoApplication
            val retry = try {
                app.repository.purgeAiReportsOlderThan(app.settingsStore.currentSettings().aiReportRetention)
                false
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { true }
            jobFinished(params, retry)
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { scope.coroutineContext.cancelChildren(); return true }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        private const val JOB_ID = 19004
        suspend fun monitor(app: TodoApplication) {
            app.repository.purgeAiReportsOlderThan(app.settingsStore.currentSettings().aiReportRetention)
            app.repository.observeOldestDailyReportTime().collectLatest { generated ->
                val scheduler = app.getSystemService(JobScheduler::class.java)
                scheduler.cancel(JOB_ID)
                if (generated == null) return@collectLatest
                val remaining = (generated - AiReportPolicy.dailyCutoff()).coerceAtLeast(0)
                scheduler.schedule(JobInfo.Builder(JOB_ID, ComponentName(app, AiReportExpiryJobService::class.java))
                    .setMinimumLatency(remaining).setOverrideDeadline(remaining + 60_000)
                    .setPersisted(true).build())
                while (true) {
                    val wait = generated - AiReportPolicy.dailyCutoff()
                    if (wait <= 0) {
                        app.repository.purgeExpiredDailyReports()
                        break
                    }
                    delay(wait.coerceAtMost(60_000))
                }
            }
        }
    }
}
