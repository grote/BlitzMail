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

import java.util.Date;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class SendActivity extends Activity {
	// define variables to be used in AsyncMailTask
	protected NotificationManager mNotifyManager;
	protected NotificationCompat.Builder mBuilder;
	protected Intent notifyIntent;
	private JSONObject mMail;

	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// before doing anything show notification about sending process
		mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		mBuilder = new NotificationCompat.Builder(this);
		mBuilder.setContentTitle(getString(R.string.sending_mail))
			.setContentText(getString(R.string.please_wait))
			.setSmallIcon(R.drawable.notification_icon)
			.setOngoing(true);
		// Sets an activity indicator for an operation of indeterminate length
		mBuilder.setProgress(0, 0, true);
		// Create Pending Intent
		notifyIntent = new Intent(this, NotificationHandlerActivity.class);
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(pendingIntent);
		// Issues the notification
		mNotifyManager.notify(0, mBuilder.build());

		Properties prefs;
		try {
			prefs = getPrefs();
		}
		catch(Exception e) {
			String msg = e.getMessage();

			Log.i("SendActivity", "ERROR: " + msg, e);

			if(e.getClass().getCanonicalName().equals("java.lang.RuntimeException") &&
					e.getCause() != null &&
					e.getCause().getClass().getCanonicalName().equals("javax.crypto.BadPaddingException")) {
				msg = getString(R.string.error_decrypt);
			}

			showError(msg);
			return;
		}

		// get and handle Intent
		Intent intent = getIntent();
		String action = intent.getAction();

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		if(action.equals(Intent.ACTION_SEND)) {
			String text    = intent.getStringExtra(Intent.EXTRA_TEXT);
			//String email   = intent.getStringExtra(Intent.EXTRA_EMAIL);
			String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
			String cc      = intent.getStringExtra(Intent.EXTRA_CC);
			String bcc     = intent.getStringExtra(Intent.EXTRA_BCC);

			// Check for empty content
			if(subject == null && text != null) {
				// cut all characters from subject after the 128th
				subject = text.substring(0, (text.length() < 128) ? text.length() : 128);
				// remove line breaks from subject
				subject = subject.replace("\n", " ").replace("\r", " ");
			} else if(subject != null && text == null) {
				text = subject;
			} else if(subject == null && text == null) {
				Log.e("Instant Mail", "Did not send mail, because subject and body empty.");
				showError(getString(R.string.error_no_body_no_subject));
				return;
			}

			// create JSON object with mail information
			mMail = new JSONObject();
			try {
				mMail.put("id", String.valueOf(new Date().getTime()));
				mMail.put("body", text);
				mMail.put("subject", subject);
				mMail.put("cc", cc);
				mMail.put("bcc", bcc);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			// remember mail for later
			MailStorage.saveMail(this, mMail);

			// pass mail on to notification dialog class
			notifyIntent.putExtra("mail", mMail.toString());

			// Start Mail Task
			AsyncMailTask mail = new AsyncMailTask(this, prefs, mMail);
			mail.execute();
		}
		else if(action.equals("BlitzMailReSend")) {
			try {
				mMail = new JSONObject(intent.getStringExtra("mail"));
			} catch (JSONException e) {
				e.printStackTrace();
			}

			// pass mail on to notification dialog class
			notifyIntent.putExtra("mail", mMail.toString());

			// Start Mail Task
			AsyncMailTask mail = new AsyncMailTask(this, prefs, mMail);
			mail.execute();
		}
		finish();
	}

	private Properties getPrefs() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		Crypto crypto = new Crypto(this);

		String recipients = pref.getString("pref_recipient", null);
		String sender = pref.getString("pref_sender", null);

		String server = pref.getString("pref_smtp_server", null);
		String port = pref.getString("pref_smtp_port", null);
		Boolean auth = pref.getBoolean("pref_smtp_auth", false);
		String user = pref.getString("pref_smtp_user", null);
		String password = pref.getString("pref_smtp_pass", null);

		if(recipients == null) throw new RuntimeException(getString(R.string.error_option_not_set)+" "+getString(R.string.pref_recipient));
		if(sender == null)     throw new RuntimeException(getString(R.string.error_option_not_set)+" "+getString(R.string.pref_sender));
		if(server == null)     throw new RuntimeException(getString(R.string.error_option_not_set)+" "+getString(R.string.pref_smtp_server));
		if(port == null)       throw new RuntimeException(getString(R.string.error_option_not_set)+" "+getString(R.string.pref_smtp_port));
		if(auth) {
			if(user == null)     throw new RuntimeException(getString(R.string.error_option_not_set)+" "+getString(R.string.pref_smtp_user));
			if(password == null) throw new RuntimeException(getString(R.string.error_option_not_set)+" "+getString(R.string.pref_smtp_pass));

			// Decrypt password
			password = crypto.decrypt(password);
		}

		Properties props = new Properties();
		props.setProperty("mail.transport.protocol", "smtp");
		props.setProperty("mail.host", server);
		props.setProperty("mail.user", pref.getString("pref_sender_name", getString(R.string.app_name)) + " <" + sender + ">");
		props.setProperty("mail.smtp.auth", String.valueOf(auth));
		props.setProperty("mail.smtp.port", port);
		props.setProperty("mail.smtp.recipients", recipients);
		props.setProperty("mail.smtp.quitwait", "false");

		if(auth) {
			// set username and password
			props.setProperty("mail.smtp.user", user);
			props.setProperty("mail.smtp.pass", password);

			// set encryption properties
			if(pref.getString("pref_smtp_encryption", "").equals("ssl")) {
				Log.i("SendActivity", "Using SSL Encryption...");
				props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
				props.setProperty("mail.smtp.socketFactory.port", port);
				props.setProperty("mail.smtp.socketFactory.fallback", "false");
				props.setProperty("mail.smtp.ssl.checkserveridentity", "true");
			} else if(pref.getString("pref_smtp_encryption", "").equals("tls")) {
				Log.i("SendActivity", "Using TLS Encryption...");
				props.setProperty("mail.smtp.starttls.enable", "true");
			}
		} else {
			// set some hostname for proper HELO greeting
			props.setProperty("mail.smtp.localhost",  "android.com");
		}

		return props;
	}

	private void showError(String text) {
		// close notification first
		mNotifyManager.cancel(0);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle(getString(R.string.app_name) + " - " + getString(R.string.error));
		builder.setMessage(text);
		builder.setIcon(android.R.drawable.ic_dialog_alert);

		// Add the buttons
		builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// User clicked Cancel button, close this Activity
				finish();
			}
		});
		// Create and show the AlertDialog
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
	}
}
