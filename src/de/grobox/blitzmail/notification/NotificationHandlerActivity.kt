/*    BlitzMail
 *    Copyright (C) 2013 Torsten Grote
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as
 *    published by the Free Software Foundation, either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.
 *
 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.grobox.blitzmail.notification

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import de.grobox.blitzmail.BuildConfig
import de.grobox.blitzmail.MailStorage
import de.grobox.blitzmail.R
import de.grobox.blitzmail.send.SendActivity
import de.grobox.blitzmail.send.SendActivity.ACTION_RESEND
import de.grobox.blitzmail.send.scheduleSending

class NotificationHandlerActivity : Activity() {

    companion object {
        const val ACTION_DIALOG = "de.grobox.blitzmail.action.DIALOG"
        const val ACTION_FINISH = "de.grobox.blitzmail.action.FINISH"
        const val ACTION_SEND_LATER = "de.grobox.blitzmail.action.SEND_LATER"
        const val ACTION_TRY_AGAIN = "de.grobox.blitzmail.action.TRY_AGAIN"
        const val EXTRA_MAIL_ID = "de.grobox.blitzmail.extra.mailId"
    }

    private lateinit var mailNotificationManager: MailNotificationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mailNotificationManager = getMailNotificationManager(applicationContext)

        if (intent != null) {
            onNewIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        // show dialog for server errors
        when (intent.action) {
            ACTION_DIALOG -> {
                val mailId = intent.getStringExtra(EXTRA_MAIL_ID)
                val builder = AlertDialog.Builder(this, R.style.DialogTheme)
                        .setTitle(intent.getStringExtra("ContentTitle"))
                        .setMessage(intent.getStringExtra("ContentText"))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        // Add the buttons
                        .setNegativeButton(resources.getString(R.string.delete)) { _, _ -> deleteMail(mailId) }
                        .setNeutralButton(resources.getString(R.string.send_later)) { _, _ -> sendLater() }
                        .setPositiveButton(resources.getString(R.string.try_again)) { _, _ -> tryAgain() }

                // Create and show the AlertDialog
                val dialog = builder.create()
                dialog.setCanceledOnTouchOutside(false)
                dialog.show()
            }
            ACTION_SEND_LATER -> sendLater()
            ACTION_TRY_AGAIN -> tryAgain()
            ACTION_FINISH -> killNotificationAndFinish()
            else -> finish()
        }
    }

    private fun sendLater() {
        // User clicked Cancel button
        if (BuildConfig.PRO) {
            scheduleSending()
            killNotificationAndFinish()
        } else {
            val builder = AlertDialog.Builder(this, R.style.DialogTheme)

            builder.setTitle(getString(R.string.app_name))
            builder.setMessage(getString(R.string.error_lite_version))
            builder.setIcon(android.R.drawable.ic_dialog_info)

            // Add the buttons
            builder.setPositiveButton(android.R.string.ok) { lite_dialog, _ ->
                val uri = Uri.parse("https://play.google.com/store/apps/details?id=de.grobox.blitzmail.pro")
                val intent = Intent(Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                }
                lite_dialog.dismiss()
                finish()
            }
            builder.setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                finish()
            }

            // Create and show the AlertDialog
            val dialog = builder.create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }
    }

    private fun tryAgain() {
        // Prepare start of new activity
        val intent = Intent(this, SendActivity::class.java)
        intent.action = ACTION_RESEND

        killNotificationAndFinish()

        startActivity(intent)
    }

    private fun deleteMail(mailId: String) {
        MailStorage.deleteMail(applicationContext, mailId)
        finish()
    }

    private fun killNotificationAndFinish() {
        mailNotificationManager.cancelErrorNotification()
        finish()
    }
}
