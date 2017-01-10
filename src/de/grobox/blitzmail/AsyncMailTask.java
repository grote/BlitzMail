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

package de.grobox.blitzmail;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import org.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

class AsyncMailTask extends AsyncTask<Void, Void, Boolean> {
	private SendActivity activity;
	private Properties props;
	private Exception e;

	private JSONObject mail;

	AsyncMailTask(SendActivity activity, Properties props, JSONObject mail) {
		this.activity = activity;
		this.props = props;
		this.mail = mail;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		// try to get proper hostname and set fake one if failed
		if(props.getProperty("mail.smtp.localhost", "").equals("android.com")) {
			String hostname = "";
			try {
				hostname = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				// do nothing
			}
			if(!hostname.equals("localhost")) {
				props.setProperty("mail.smtp.localhost",  hostname);
			}
		}

		MailSender sender = new MailSender(activity, props, mail);

		try {
			sender.sendMail();
		} catch(Exception e) {
			Log.d("AsyncMailTask", "ERROR: " + e.getLocalizedMessage());

			// remember exception for when task is finished
			this.e = e;

			return false;
		}
		return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		String msg;

		// set progress notification to finished
		activity.mBuilder.setProgress(0, 0, false);
		activity.mBuilder.setOngoing(false);

		// set dialog to auto close when clicked
		activity.mBuilder.setAutoCancel(true);

		if(result) {
			// Everything went fine, so delete mail from local storage
			MailStorage.deleteMail(activity, mail.optString("id"));

			// check to see if there should be a success notification
			if(!PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("pref_success_notification", true)) {
				// don't show success notification
				activity.mNotifyManager.cancel(activity.getMailId());
				return;
			}

			msg = mail.optString("subject");

			// show success notification
			activity.mBuilder.setContentTitle(activity.getString(R.string.sent_mail))
					.setLargeIcon(BitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_launcher))
					.setSmallIcon(R.drawable.ic_stat_notify);
			activity.notifyIntent.setAction(NotificationHandlerActivity.ACTION_FINISH);

			// Quick Action: Dismiss
			PendingIntent piDismiss = PendingIntent.getActivity(activity, 0, activity.notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			activity.mBuilder.addAction(R.drawable.ic_action_cancel, activity.getString(R.string.dismiss), piDismiss);
		} else {
			// show error notification
			activity.mBuilder.setContentTitle(activity.getString(R.string.app_name) + " - " + activity.getString(R.string.error))
					.setLargeIcon(BitmapFactory.decodeResource(activity.getResources(), R.drawable.ic_launcher))
					.setSmallIcon(android.R.drawable.ic_dialog_alert);

			// Quick Action: Try Again
			Intent tryAgainIntent = new Intent(activity, NotificationHandlerActivity.class);
			tryAgainIntent.setAction(NotificationHandlerActivity.ACTION_TRY_AGAIN);
			tryAgainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			tryAgainIntent.putExtra("mail", mail.toString());
			PendingIntent piTryAgain = PendingIntent.getActivity(activity, 0, tryAgainIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			activity.mBuilder.addAction(R.drawable.ic_action_try_again, activity.getString(R.string.try_again), piTryAgain);

			// Quick Action: Send Later
			Intent sendLaterIntent = new Intent(activity, NotificationHandlerActivity.class);
			sendLaterIntent.setAction(NotificationHandlerActivity.ACTION_SEND_LATER);
			tryAgainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			sendLaterIntent.putExtra("mail", mail.toString());
			PendingIntent piSendLater = PendingIntent.getActivity(activity, 0, sendLaterIntent, PendingIntent.FLAG_UPDATE_CURRENT);
			activity.mBuilder.addAction(R.drawable.ic_action_send_later, activity.getString(R.string.send_later), piSendLater);

			e.printStackTrace();

			if(e.getClass().getCanonicalName().equals("javax.mail.AuthenticationFailedException")) {
				msg = activity.getString(R.string.error_auth_failed);
			}
			else if(e.getClass().getCanonicalName().equals("javax.mail.MessagingException") &&
					e.getCause() != null &&
					e.getCause().getClass().getCanonicalName().equals("javax.net.ssl.SSLException") &&
					e.getCause().getCause() != null &&
					e.getCause().getCause().getClass().getCanonicalName().equals("java.security.cert.CertificateException")) {
				// TODO use MemorizingTrustManager instead, issue #1
				msg = activity.getString(R.string.error_sslcert_invalid);
			}
			else {
				// TODO improve showing the error here
				msg = activity.getString(R.string.error_smtp) + '\n' + e.getLocalizedMessage();
			}

			// get and show the cause for the exception if it exists
			if(e.getCause() != null) {
				Throwable ecause = e.getCause();
				Log.d("AsyncMailTask", ecause.getClass().getCanonicalName());
				msg += "\nCause: " + ecause.getLocalizedMessage();
			}

			activity.notifyIntent.setAction(NotificationHandlerActivity.ACTION_DIALOG);
			activity.notifyIntent.putExtra("ContentTitle", activity.getString(R.string.error));
			activity.notifyIntent.putExtra("ContentText", msg);
		}

		// Update the notification
		activity.mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
				.setContentText(msg.substring(0, msg.length() <= 32 ? msg.length() : 32))
				.setContentIntent(PendingIntent.getActivity(activity, 0, activity.notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT));
		activity.mNotifyManager.notify(activity.getMailId(), activity.mBuilder.build());
	}
}