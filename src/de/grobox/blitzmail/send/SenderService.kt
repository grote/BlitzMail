package de.grobox.blitzmail.send

import android.app.IntentService
import android.content.Intent
import android.util.Log
import de.grobox.blitzmail.MailStorage
import de.grobox.blitzmail.notification.MailNotificationManager
import de.grobox.blitzmail.notification.getMailNotificationManager
import de.grobox.blitzmail.preferences.getProperties
import org.json.JSONObject

const val MAIL = "mail"
const val MAIL_ID = "id"
const val MAIL_BODY = "body"
const val MAIL_SUBJECT = "subject"
const val MAIL_CC = "cc"
const val MAIL_ATTACHMENTS = "attachments"

class SenderService : IntentService("SenderService") {

    private lateinit var mailNotificationManager: MailNotificationManager

    override fun onCreate() {
        super.onCreate()
        mailNotificationManager = getMailNotificationManager(applicationContext)
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        val mail = JSONObject(intent.getStringExtra(MAIL))
        val mailId = mail.getInt(MAIL_ID)
        val subject = mail.getString(MAIL_SUBJECT)

        mailNotificationManager.createNotificationChannel();
        startForeground(mailId, mailNotificationManager.getForegroundNotification());

        try {
            // save mail, will be removed by sender on
            MailStorage.saveMail(this, mail)

            MailSender(getProperties(applicationContext), mail).sendMail()

            mailNotificationManager.showSuccessNotification(mailId, subject)

            // Everything went fine, so delete mail from local storage
            MailStorage.deleteMail(applicationContext, mailId.toString())
        } catch (e: Exception) {
            Log.d("AsyncMailTask", "ERROR: " + e.localizedMessage)

            mailNotificationManager.showErrorNotification(mailId, mail, e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }

}
