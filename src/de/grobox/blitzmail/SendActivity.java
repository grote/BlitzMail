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

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.graphics.BitmapFactory.decodeResource;
import static android.os.Build.VERSION.SDK_INT;

public class SendActivity extends AppCompatActivity {
	// define variables to be used in AsyncMailTask
	protected NotificationManager mNotifyManager;
	protected NotificationCompat.Builder mBuilder;
	private int mailId;
	protected Intent notifyIntent;
	private Properties prefs;
	private boolean error = false, requestingPermission = false;
	private ArrayList<Uri> uris;

	private static final String NOTIFICATION_CHANNEL_ID = "BlitzMail";
	private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 42;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// setup notification channels
		mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		assert mNotifyManager != null;
		if (SDK_INT >= 26) {
			NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.app_name), IMPORTANCE_LOW);
			mNotifyManager.createNotificationChannel(channel);
		}

		// generate mail id from current time
		mailId = (int) ((new Date().getTime() / 100) % 1000000000);

		// before doing anything show notification about sending process
		mBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
		showNotification();

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
		String type = intent.getType();

		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		if(Intent.ACTION_SEND.equals(action) && type != null) {
			if("text/plain".equals(type)) {
				handleSendText(intent);
			}
			else  {
				handleSendAttachment(intent);
			}
		}
		else if(Intent.ACTION_SEND_MULTIPLE.equals(action)) {
			handleSendMultipleAttachment(intent);
		}
		else if("BlitzMailReSend".equals(action)) {
			JSONObject jMail;
			try {
				jMail = new JSONObject(intent.getStringExtra("mail"));

				// Dirty Hack: Cancel the notification with the wrong ID
				mNotifyManager.cancel(mailId);

				// get proper ID from saved mail
				mailId = jMail.optInt("id");

				// issue new notification
				mNotifyManager.notify(mailId, mBuilder.build());
			} catch (JSONException e) {
				e.printStackTrace();
				return;
			}

			// Start Mail Task
			sendMail(jMail, false);
		} else {
			showError(getString(R.string.error_noaction));
		}

		if(error || requestingPermission) {
			mNotifyManager.cancel(mailId);
		} else {
			finish();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		if(requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
			requestingPermission = false;
			if(grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
				showNotification();
				sendAttachment(getMailWithAttachments(uris));
				finish();
			} else {
				showError(getString(R.string.error_permission_denied));
			}
		}
	}

	private void showNotification() {
		mBuilder.setContentTitle(getString(R.string.sending_mail))
				.setContentText(getString(R.string.please_wait))
				.setSmallIcon(R.drawable.notification_icon)
				.setLargeIcon(decodeResource(getResources(), R.drawable.ic_launcher))
				.setOngoing(true)
				.setProgress(0, 0, true)
				.setSound(null);

		// Create Pending Intent
		notifyIntent = new Intent(this, NotificationHandlerActivity.class);
		notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		mBuilder.setContentIntent(pendingIntent);

		// TODO allow sending to be cancelled, might need a service with broadcast receiver
//		Intent dismissIntent = new Intent(this, NotificationHandlerActivity.class);
//		dismissIntent.setAction("test");
//		PendingIntent piCancel = PendingIntent.getActivity(this, 0, dismissIntent, 0);

//		mBuilder.addAction(R.drawable.ic_action_cancel, getString(android.R.string.cancel), piCancel);

		// Issues the notification
		mNotifyManager.notify(mailId, mBuilder.build());
	}

	void handleSendText(Intent intent) {
		String text = intent.getStringExtra(Intent.EXTRA_TEXT);

		if(text != null) {
			//String email   = intent.getStringExtra(Intent.EXTRA_EMAIL);
			String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
			String cc = intent.getStringExtra(Intent.EXTRA_CC);

			// Check for empty content
			if(subject == null) {
				// cut all characters from subject after the 128th
				subject = text.substring(0, (text.length() < 128) ? text.length() : 128);
				// remove line breaks from subject
				subject = subject.replace("\n", " ").replace("\r", " ");
			}

			// create JSON object with mail information
			JSONObject jMail = new JSONObject();
			try {
				jMail.put("id", mailId);
				jMail.put("body", text);
				jMail.put("subject", subject);
				jMail.put("cc", cc);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			// Start Mail Task
			sendMail(jMail);
		} else {
			showError(getString(R.string.error_no_body_no_subject));
		}
	}

	private void handleSendAttachment(Intent intent) {
		Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);

		ArrayList<Uri> list = new ArrayList<>(1);
		list.add(uri);

		sendAttachment(getMailWithAttachments(list));
	}

	private void handleSendMultipleAttachment(Intent intent) {
		ArrayList<Uri> attachmentUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

		sendAttachment(getMailWithAttachments(attachmentUris));
	}

	private void sendAttachment(@Nullable JSONObject jMail) {
		if (jMail == null && requestingPermission) {
			return;
		} else if(jMail == null) {
			showError(getString(R.string.error_attachment));
			return;
		}

		try {
			String num = String.valueOf(jMail.getJSONArray("attachments").length());
			jMail.put("subject", num + " " + getString(R.string.files_shared) + " " + getString(R.string.app_name));
		} catch(JSONException e) {
			e.printStackTrace();
		}

		// Start Mail Task
		sendMail(jMail);
	}

	@Nullable
	private JSONObject getMailWithAttachments(ArrayList<Uri> attachmentUris) {
		if(attachmentUris != null) {
			// check if permission is needed for an URI and request if so
			for(Uri uri : attachmentUris) {
				if("file".equals(uri.getScheme()) && SDK_INT >= 23) requestPermission();
				if (requestingPermission) {
					uris = attachmentUris;
					return null;
				}
			}

			// create JSON object with mail information
			try {
				JSONObject jMail = new JSONObject();
				jMail.put("id", mailId);
				JSONArray attachments = new JSONArray();

				for(Uri uri : attachmentUris) {
					JSONObject attachment = new JSONObject();

					// get file name
					String filename;
					Cursor cursor = getContentResolver().query(uri, null, null, null, null);
					if(cursor != null) {
						int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
						cursor.moveToFirst();
						filename = cursor.getString(nameIndex);
						cursor.close();
					}
					else {
						filename = uri.getLastPathSegment();
					}
					attachment.put("filename", filename);

					// copy file into a temporary file
					try {
						File file = File.createTempFile(filename, null);
						FileOutputStream fos = new FileOutputStream(file);

						FileDescriptor fd = getContentResolver().openFileDescriptor(uri, "r").getFileDescriptor();
						FileInputStream fis = new FileInputStream(fd);

						copyLarge(fis, fos);

						attachment.put("path", file.getAbsolutePath());
					}
					catch(FileNotFoundException e) {
						showError(getString(R.string.error_file_not_found));
						return null;
					}
					catch(IOException e) {
						e.printStackTrace();
					}

					// get mime type
					String mimeType = getContentResolver().getType(uri);
					if(mimeType == null) {
						// guess mime type
						if(filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
							mimeType = " image/jpeg";
						} else {
							mimeType = "application/octet-stream";
						}
					}
					attachment.put("mimeType", mimeType);

					attachments.put(attachment);
				}
				jMail.put("attachments", attachments);

				return jMail;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private void sendMail(JSONObject jMail, boolean save) {
		try {
			mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(jMail.getString("subject")));
		} catch(JSONException e) {
			e.printStackTrace();
		}

		if(save) {
			// remember mail for later
			MailStorage.saveMail(this, jMail);
		}

		// pass mail on to notification dialog class
		notifyIntent.putExtra("mail", jMail.toString());

		final AsyncMailTask mail = new AsyncMailTask(this, prefs, jMail);
		mail.execute();
	}

	private void sendMail(JSONObject jMail) {
		sendMail(jMail, true);
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
		String from = pref.getString("pref_sender_name", getString(R.string.app_name)) + " <" + sender + ">";
		props.setProperty("mail.user", from);
		props.setProperty("mail.from", from);
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
				props.setProperty("mail.smtp.ssl.enable", "true");
			} else if(pref.getString("pref_smtp_encryption", "").equals("tls")) {
				Log.i("SendActivity", "Using TLS Encryption...");
				props.setProperty("mail.smtp.starttls.enable", "true");
			}
			props.setProperty("mail.smtp.ssl.checkserveridentity", "true");
		} else {
			// set some hostname for proper HELO greeting
			props.setProperty("mail.smtp.localhost",  "android.com");
		}

		return props;
	}

	public int getMailId() {
		return mailId;
	}

	@RequiresApi(api = 16)
	private void requestPermission() {
		if(ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE)) {
				Toast.makeText(this, R.string.error_no_permission, Toast.LENGTH_LONG).show();
			} else {
				requestingPermission = true;
				ActivityCompat.requestPermissions(this, new String[]{READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
			}
		}
	}

	private void showError(String text) {
		error = true;
		// close notification first
		mNotifyManager.cancel(mailId);

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);

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

	public static long copyLarge(InputStream input, OutputStream output) throws IOException	{
		byte[] buffer = new byte[4096];
		long count = 0L;
		int n;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}
}
