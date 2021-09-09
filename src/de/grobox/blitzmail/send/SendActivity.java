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

package de.grobox.blitzmail.send;

import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import de.grobox.blitzmail.R;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.content.Intent.ACTION_SEND;
import static android.content.Intent.ACTION_SEND_MULTIPLE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static android.os.Build.VERSION.SDK_INT;
import static android.widget.Toast.LENGTH_LONG;
import static de.grobox.blitzmail.send.SenderServiceKt.MAIL;
import static de.grobox.blitzmail.send.SenderServiceKt.MAIL_ATTACHMENTS;
import static de.grobox.blitzmail.send.SenderServiceKt.MAIL_BODY;
import static de.grobox.blitzmail.send.SenderServiceKt.MAIL_CC;
import static de.grobox.blitzmail.send.SenderServiceKt.MAIL_ID;
import static de.grobox.blitzmail.send.SenderServiceKt.MAIL_SUBJECT;
import static de.grobox.blitzmail.send.SenderServiceKt.MAIL_DATE;

public class SendActivity extends AppCompatActivity {
	// define variables to be used in AsyncMailTask
	protected NotificationManager mNotifyManager;
	private int mailId;
	private boolean error = false, requestingPermission = false;
	private ArrayList<Uri> uris;

	public static final String ACTION_RESEND = "BlitzMailReSend";
	private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 42;
	private static final int MAX_SUBJECT_LENGTH = 128;
	private static final int MAX_FILENAME_LENGTH = 64;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// setup notification channels
		mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// generate mail id from current time
		mailId = (int) (System.currentTimeMillis() - 1000000) / 100;

		// get and handle Intent
		Intent intent = getIntent();
		String action = intent.getAction();
		String type = intent.getType();

		intent.addFlags(FLAG_ACTIVITY_NEW_TASK);

		if(ACTION_SEND.equals(action) && type != null) {
			if("text/plain".equals(type)) {
				handleSendText(intent);
			}
			else  {
				handleSendAttachment(intent);
			}
		} else if(ACTION_SEND_MULTIPLE.equals(action)) {
			handleSendMultipleAttachment(intent);
		} else if(ACTION_RESEND.equals(action)) {
			sendMail(null);
		} else {
			showError(getString(R.string.error_noaction));
		}

		if(!error && !requestingPermission) {
			finish();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
		if(requestCode == MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE) {
			requestingPermission = false;
			if(grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
				sendAttachment(getMailWithAttachments(uris));
				finish();
			} else {
				showError(getString(R.string.error_permission_denied));
			}
		}
	}

	void handleSendText(Intent intent) {
		String text = intent.getStringExtra(Intent.EXTRA_TEXT);

		if(text != null) {
			//String email   = intent.getStringExtra(Intent.EXTRA_EMAIL);
			String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
			String cc = intent.getStringExtra(Intent.EXTRA_CC);

			// Check for empty content
			if(subject == null || subject.isEmpty()) {
				// cut all characters from subject after the 128th
				subject = text.substring(0, Math.min(text.length(), MAX_SUBJECT_LENGTH));
				// remove line breaks from subject
				subject = subject.replace("\n", " ").replace("\r", " ");
			}

			// create JSON object with mail information
			JSONObject jMail = new JSONObject();
			try {
				jMail.put(MAIL_ID, mailId);
				jMail.put(MAIL_BODY, text);
				jMail.put(MAIL_SUBJECT, subject);
				jMail.put(MAIL_CC, cc);
				jMail.put(MAIL_DATE, new Date().getTime());
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
		if (jMail == null || !jMail.has(MAIL_ATTACHMENTS)) {
			if (!requestingPermission) {
				showError(getString(R.string.error_attachment));
			}
			return;
		}

		try {
			JSONArray files = jMail.getJSONArray(MAIL_ATTACHMENTS);
			if (files.length() == 0) {
				showError(getString(R.string.warning_nothing_to_send));
				return;
			}

			String filename = files.getJSONObject(0).getString("filename");
			if (filename.length() > MAX_FILENAME_LENGTH) {
				filename = filename.substring(0, MAX_FILENAME_LENGTH - 2) + "…";
			}

			if (files.length() == 1) {
				jMail.put(MAIL_SUBJECT, getString(R.string.subject_single_file, filename));
			} else {
				int over_one = files.length() - 1;
				jMail.put(MAIL_SUBJECT, getResources().getQuantityString(
						R.plurals.subject_multiple_files, over_one, filename, over_one));
			}
		} catch(JSONException e) {
			e.printStackTrace();
		}

		// Start Mail Task
		sendMail(jMail);
	}

	@Nullable
	private JSONObject getMailWithAttachments(ArrayList<Uri> attachmentUris) {
		if(attachmentUris == null) {
			return null;
		}

		// check if permission is needed for an URI and request if so
		for(Uri uri : attachmentUris) {
			if("file".equals(uri.getScheme()) && SDK_INT >= 23) requestPermission();
			if (requestingPermission) {
				uris = attachmentUris;
				return null;
			}
		}

		// create JSON object with mail information
		JSONArray attachments = new JSONArray();

		for(Uri uri : attachmentUris) {
			try {
				// get file name
				String filename;
				Cursor cursor = getContentResolver().query(uri, null, null, null, null);
				if(cursor != null) {
					int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
					cursor.moveToFirst();
					filename = cursor.getString(nameIndex);
					cursor.close();
				} else {
					filename = uri.getLastPathSegment();
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

				// copy file into a temporary file
				File file = File.createTempFile(filename, null);
				FileOutputStream fos = new FileOutputStream(file);
				FileDescriptor fd = getContentResolver().openFileDescriptor(uri, "r").getFileDescriptor();
				FileInputStream fis = new FileInputStream(fd);

				copyLarge(fis, fos);

				// add file to mail attachments
				JSONObject attachment = new JSONObject();
				attachment.put("filename", filename);
				attachment.put("mimeType", mimeType);
				attachment.put("path", file.getAbsolutePath());
				attachments.put(attachment);
			}
			catch(FileNotFoundException e) {
				showError(getString(R.string.error_file_not_found));
			}
			catch(IOException e) {
				e.printStackTrace();
			}
			catch(JSONException e) {
				e.printStackTrace();
			}
		}

		try {
			JSONObject jMail = new JSONObject();
			jMail.put(MAIL_ID, mailId);
			// jMail.put(MAIL_CC, cc);
			jMail.put(MAIL_DATE, new Date().getTime());
			jMail.put(MAIL_ATTACHMENTS, attachments);
			return jMail;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void sendMail(@Nullable JSONObject jMail) {
		Intent intent = new Intent(this, SenderService.class);
		intent.putExtra(MAIL, jMail == null ? null : jMail.toString());
		ContextCompat.startForegroundService(this, intent);
	}

	@RequiresApi(api = 16)
	private void requestPermission() {
		if(ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
			// Should we show an explanation?
			if (ActivityCompat.shouldShowRequestPermissionRationale(this, READ_EXTERNAL_STORAGE)) {
				Toast.makeText(this, R.string.error_no_permission, LENGTH_LONG).show();
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

	private static void copyLarge(InputStream input, OutputStream output) throws IOException	{
		byte[] buffer = new byte[4096];
		int n;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
	}

}
