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

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationHandlerActivity extends Activity {
	private JSONObject mMail;

	public final static String ACTION_DIALOG = "de.grobox.blitzmail.action.DIALOG";
	public final static String ACTION_FINISH = "de.grobox.blitzmail.action.FINISH";
	public final static String ACTION_SEND_LATER = "de.grobox.blitzmail.action.SEND_LATER";
	public final static String ACTION_TRY_AGAIN = "de.grobox.blitzmail.action.TRY_AGAIN";

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();

		if(intent != null) {
			String mail = intent.getStringExtra("mail");

			try {
				if(mail != null) {
					mMail = new JSONObject(mail);
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}

			onNewIntent(intent);
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		String action = intent.getAction();

		if(action == null) {
			finish();
			return;
		}

		// show dialog for server errors
		if(action.equals(ACTION_DIALOG)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.InvisibleTheme);

			builder.setTitle(intent.getStringExtra("ContentTitle"))
					.setMessage(intent.getStringExtra("ContentText"))
					.setIcon(android.R.drawable.ic_dialog_alert)
					// Add the buttons
					.setNegativeButton(getResources().getString(R.string.dismiss), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							deleteMail();
							// User clicked Cancel button, close this Activity
							finish();
						}
					})
					.setNeutralButton(getResources().getString(R.string.send_later), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							sendLater();
						}
					})
					.setPositiveButton(getResources().getString(R.string.try_again), new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							tryAgain();
						}
					});

			// Create and show the AlertDialog
			AlertDialog dialog = builder.create();
			dialog.setCanceledOnTouchOutside(false);
			dialog.show();
		}
		else if(action.equals(ACTION_SEND_LATER)) {
			sendLater();
		}
		else if(action.equals(ACTION_TRY_AGAIN)) {
			tryAgain();
		}
		else if(action.equals(ACTION_FINISH)) {
			killNotificationAndFinish();
		}
		else {
			// simply close activity
			finish();
		}
	}

	private void deleteMail() {
		if(mMail != null) {
			MailStorage.deleteMail(this, mMail.optString("id"));
		}
	}

	private void sendLater() {
		// User clicked Cancel button
		if(BuildConfig.PRO) {
			// start listening for network connectivity changes
			ComponentName receiver = new ComponentName(this, NetworkChangeReceiver.class);

			PackageManager pm = getPackageManager();
			pm.setComponentEnabledSetting(receiver,
					PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
					PackageManager.DONT_KILL_APP);

			// close this Activity
			killNotificationAndFinish();
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(NotificationHandlerActivity.this, R.style.InvisibleTheme);

			builder.setTitle(getString(R.string.app_name));
			builder.setMessage(getString(R.string.error_lite_version));
			builder.setIcon(android.R.drawable.ic_dialog_info);

			// Add the buttons
			builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface lite_dialog, int id) {
					Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=de.grobox.blitzmail.pro");
					Intent intent = new Intent(Intent.ACTION_VIEW, uri);
					if (intent.resolveActivity(getPackageManager()) != null) {
						startActivity(intent);
					}
					lite_dialog.dismiss();
					finish();
				}
			});
			builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface lite_dialog, int id) {
					lite_dialog.dismiss();
					finish();
				}
			});

			// Create and show the AlertDialog
			AlertDialog lite_dialog = builder.create();
			lite_dialog.setCanceledOnTouchOutside(false);
			lite_dialog.show();
		}
	}

	private void tryAgain() {
		if(mMail == null) return;

		// Prepare start of new activity
		Intent intent = new Intent(NotificationHandlerActivity.this, SendActivity.class);
		intent.setAction("BlitzMailReSend");
		intent.putExtra("mail", mMail.toString());

		killNotificationAndFinish();

		startActivity(intent);
	}

	private void killNotificationAndFinish() {
		NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		nm.cancel(mMail.optInt("id"));

		finish();
	}
}
