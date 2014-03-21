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

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class NotificationHandlerActivity extends Activity {
	private JSONObject mMail;
	private Context context = this;

	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		onNewIntent(intent);

		try {
			if(intent != null) {
				mMail = new JSONObject(intent.getStringExtra("mail"));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Bundle extras = intent.getExtras(); 

		// show dialog for server errors
		if(extras != null && extras.getString("ContentTitle").equals(getString(R.string.error))) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			builder.setTitle(getString(R.string.app_name) + " - " + getString(R.string.error));
			builder.setMessage(extras.getString("ContentText"));
			builder.setIcon(android.R.drawable.ic_dialog_alert);

			// Add the buttons
			builder.setNegativeButton(getResources().getString(R.string.dismiss), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					deleteMail();
					// User clicked Cancel button, close this Activity
					finish();
				}
			});
			builder.setNeutralButton(getResources().getString(R.string.send_later), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				// User clicked Cancel button, close this Activity
				finish();
			}
		});
			builder.setPositiveButton(getResources().getString(R.string.try_again), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// Prepare start of new activity
					Intent intent = new Intent(context, SendActivity.class);
					intent.setAction("BlitzMailReSend");
					intent.putExtra("mail", mMail.toString());
					finish();

					startActivity(intent);
				}
			});

			// Create and show the AlertDialog
			AlertDialog dialog = builder.create();
			dialog.setCanceledOnTouchOutside(false);
			dialog.show();
		} else {
			// close activity
			finish();
		}
	}

	private void deleteMail() {
		if(mMail != null) {
			MailStorage.deleteMail(this, mMail.optString("id"));
		}
	}
}
