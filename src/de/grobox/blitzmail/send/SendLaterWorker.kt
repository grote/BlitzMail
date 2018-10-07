package de.grobox.blitzmail.send

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.*
import androidx.work.Worker.Result.SUCCESS
import de.grobox.blitzmail.send.SendActivity.ACTION_RESEND
import java.util.concurrent.TimeUnit

class SendLaterWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val intent = Intent(applicationContext, SenderService::class.java)
        ContextCompat.startForegroundService(applicationContext, intent)
        return SUCCESS
    }

}

fun scheduleSending() {
    val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
    val worker = OneTimeWorkRequestBuilder<SendLaterWorker>()
            .setInitialDelay(5, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
    WorkManager.getInstance().enqueue(worker)
}

fun sendQueuedMails(context: Context) {
    val intent = Intent(context, SendActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    intent.action = ACTION_RESEND
    context.startActivity(intent)
}
