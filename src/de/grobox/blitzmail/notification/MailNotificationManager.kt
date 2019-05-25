package de.grobox.blitzmail.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.graphics.BitmapFactory
import android.os.Build
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import de.grobox.blitzmail.R
import de.grobox.blitzmail.notification.NotificationHandlerActivity.Companion.ACTION_DIALOG
import de.grobox.blitzmail.notification.NotificationHandlerActivity.Companion.ACTION_FINISH
import de.grobox.blitzmail.notification.NotificationHandlerActivity.Companion.ACTION_SEND_LATER
import de.grobox.blitzmail.notification.NotificationHandlerActivity.Companion.EXTRA_MAIL_ID
import java.security.cert.CertificateException
import javax.crypto.BadPaddingException
import javax.mail.AuthenticationFailedException
import javax.mail.MessagingException
import javax.net.ssl.SSLException


@SuppressLint("StaticFieldLeak")
private var mailNotificationManager: MailNotificationManager? = null

/**
 * @param context pass only ApplicationContext here
 */
fun getMailNotificationManager(context: Context): MailNotificationManager {
    return mailNotificationManager ?: MailNotificationManager(context)
}

const val NOTIFICATION_ID_SENDING = 1
const val NOTIFICATION_ID_ERROR = 2
private const val NOTIFICATION_CHANNEL_ID = "BlitzMail"

class MailNotificationManager internal constructor(val c: Context) {

    private val notificationManager = c.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val notificationBuilder = NotificationCompat.Builder(c, NOTIFICATION_CHANNEL_ID)

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, c.getString(R.string.app_name), IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getForegroundNotification(): Notification {
        val notifyIntent = Intent(c, NotificationHandlerActivity::class.java)
        notifyIntent.flags = FLAG_ACTIVITY_NEW_TASK
        val pendingIntent = PendingIntent.getActivity(c, 0, notifyIntent, FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(c, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(c.getString(R.string.sending_mail))
                .setContentText(c.getString(R.string.please_wait))
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(c.resources, R.drawable.ic_launcher))
//                .setStyle(NotificationCompat.BigTextStyle().bigText(subject))
                .setOngoing(true)
                .setProgress(0, 0, true)
                .setContentIntent(pendingIntent)
                .build()
    }

    fun showSuccessNotification(mailId: Int, subject: String?) {
        // set progress notification to finished
        notificationBuilder.setProgress(0, 0, false)
        notificationBuilder.setOngoing(false)

        // set dialog to auto close when clicked
        notificationBuilder.setAutoCancel(true)

        // check to see if there should be a success notification
        if (!PreferenceManager.getDefaultSharedPreferences(c).getBoolean("pref_success_notification", true)) {
            // don't show success notification
            notificationManager.cancel(mailId)
            return
        }

        val msg = subject ?: ""

        // show success notification
        notificationBuilder.setContentTitle(c.getString(R.string.sent_mail))
                .setLargeIcon(BitmapFactory.decodeResource(c.resources, R.drawable.ic_launcher))
                .setSmallIcon(R.drawable.ic_stat_notify)
        val notifyIntent = Intent(c, NotificationHandlerActivity::class.java)
        notifyIntent.flags = FLAG_ACTIVITY_NEW_TASK
        notifyIntent.action = ACTION_FINISH

        // Quick Action: Dismiss
        val piDismiss = PendingIntent.getActivity(c, 0, notifyIntent, FLAG_UPDATE_CURRENT)
        notificationBuilder.addAction(R.drawable.ic_action_cancel, c.getString(R.string.dismiss), piDismiss)

        // Update the notification
        notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(msg))
                .setContentText(msg.substring(0, if (msg.length <= 32) msg.length else 32))
                .setContentIntent(PendingIntent.getActivity(c, 0, notifyIntent, FLAG_UPDATE_CURRENT))
        notificationManager.notify(mailId, notificationBuilder.build())
    }

    fun showErrorNotification(e: Exception, mailId: String) {
        // set progress notification to finished
        notificationBuilder.setProgress(0, 0, false)
        notificationBuilder.setOngoing(false)

        // set notification to auto close when clicked
        notificationBuilder.setAutoCancel(true)

        // show error notification
        notificationBuilder.setContentTitle(c.getString(R.string.app_name) + " - " + c.getString(R.string.error))
                .setLargeIcon(BitmapFactory.decodeResource(c.resources, R.drawable.ic_launcher))
                .setSmallIcon(android.R.drawable.ic_dialog_alert)

        // Quick Action: Try Again
        val tryAgainIntent = Intent(c, NotificationHandlerActivity::class.java)
        tryAgainIntent.action = NotificationHandlerActivity.ACTION_TRY_AGAIN
        tryAgainIntent.flags = FLAG_ACTIVITY_NEW_TASK
        val piTryAgain = PendingIntent.getActivity(c, 0, tryAgainIntent, FLAG_UPDATE_CURRENT)
        notificationBuilder.addAction(R.drawable.ic_action_try_again, c.getString(R.string.try_again), piTryAgain)

        // Quick Action: Send Later
        val sendLaterIntent = Intent(c, NotificationHandlerActivity::class.java)
        sendLaterIntent.action = ACTION_SEND_LATER
        tryAgainIntent.flags = FLAG_ACTIVITY_NEW_TASK
        val piSendLater = PendingIntent.getActivity(c, 0, sendLaterIntent, FLAG_UPDATE_CURRENT)
        notificationBuilder.addAction(R.drawable.ic_action_send_later, c.getString(R.string.send_later), piSendLater)

        e.printStackTrace()

        var msg = if (e is AuthenticationFailedException) {
            c.getString(R.string.error_auth_failed)
        } else if (e is MessagingException && e.cause is SSLException && (e.cause as Exception).cause is CertificateException) {
            c.getString(R.string.error_sslcert_invalid)
        } else if (e is RuntimeException && e.cause is BadPaddingException) {
            c.getString(R.string.error_decrypt)
        } else {
            // TODO improve showing the error here
            c.getString(R.string.error_smtp) + '\n'.toString() + e.localizedMessage
        }

        // get and show the cause for the exception if it exists
        if (e.cause is Exception) {
            val cause = e.cause as Exception
            msg += "\nCause: " + cause.localizedMessage
        }

        val notifyIntent = Intent(c, NotificationHandlerActivity::class.java)
        notifyIntent.flags = FLAG_ACTIVITY_NEW_TASK
        notifyIntent.action = ACTION_DIALOG
        notifyIntent.putExtra("ContentTitle", c.getString(R.string.error))
        notifyIntent.putExtra("ContentText", msg)
        notifyIntent.putExtra(EXTRA_MAIL_ID, mailId)

        // Update the notification
        notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(msg))
                .setContentText(msg.substring(0, if (msg.length <= 32) msg.length else 32))
                .setContentIntent(PendingIntent.getActivity(c, 0, notifyIntent, FLAG_UPDATE_CURRENT))
        notificationManager.notify(NOTIFICATION_ID_ERROR, notificationBuilder.build())
    }

    fun cancelErrorNotification() {
        notificationManager.cancel(NOTIFICATION_ID_ERROR)
    }

}
