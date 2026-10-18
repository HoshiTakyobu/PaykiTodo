package com.example.todoalarm.sync

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import com.example.todoalarm.TodoApplication
import kotlinx.coroutines.*

class RemoteSyncJobService : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            val app = application as TodoApplication
            val retry = try {
                app.remoteSyncCoordinator.syncNow() is RemoteSyncCoordinator.RemoteSyncResult.Failed ||
                    (app.settingsStore.hasValidRemoteSession() && app.settingsStore.currentSettings().remoteSyncDirty &&
                        app.settingsStore.currentSettings().remoteSyncConflictRevision == 0L)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { true }
            jobFinished(params, retry)
        }
        return true
    }
    override fun onStopJob(params: JobParameters): Boolean { scope.coroutineContext.cancelChildren(); return true }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        private const val WRITE_JOB = 19002
        private const val PERIODIC_JOB = 19003
        fun schedule(context: Context, changed: Boolean = false) {
            val scheduler = context.getSystemService(JobScheduler::class.java)
            val id = if (changed) WRITE_JOB else PERIODIC_JOB
            if (scheduler.getPendingJob(id) != null) return
            val builder = JobInfo.Builder(id, ComponentName(context, RemoteSyncJobService::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true)
                .setBackoffCriteria(30_000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
            if (changed) builder.setMinimumLatency(1_000L) else builder.setPeriodic(15 * 60_000L)
            scheduler.schedule(builder.build())
        }
        fun cancel(context: Context) {
            context.getSystemService(JobScheduler::class.java).apply { cancel(WRITE_JOB); cancel(PERIODIC_JOB) }
        }
    }
}
