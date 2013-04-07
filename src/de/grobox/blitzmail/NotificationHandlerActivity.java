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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

public class NotificationHandlerActivity extends Activity {
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		onNewIntent(intent);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Bundle extras = intent.getExtras(); 

		// show dialog for server errors
		if(extras != null && extras.getString("ContentTitle").equals(getString(R.string.error))) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);

			builder.setTitle(getString(R.string.app_name) + " - " + getString(R.string.error));
			builder.setMessage(getString(R.string.error_smtp) + '\n' + extras.getString("ContentText"));
			builder.setIcon(android.R.drawable.ic_dialog_alert);

			// Add the buttons
			builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// User clicked OK button, close this Activity
					finish();
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
}
