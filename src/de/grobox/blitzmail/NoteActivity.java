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
import android.view.View;
import android.widget.TextView;

public class NoteActivity extends Activity {
	private View mView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mView = getLayoutInflater().inflate(R.layout.activity_note, null);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(mView)
		.setIcon(R.drawable.ic_launcher_note)
		.setTitle(R.string.note_name)
		.setCancelable(false)
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				finish();
			}
		})
		.setPositiveButton(R.string.send_mail, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				sendMail(((TextView) mView.findViewById(R.id.text)).getText().toString());

				finish();
			}
		});
		// Create the AlertDialog object and show it
		builder.create().show();
	}

	private void sendMail(CharSequence text) {
		Intent intent = new Intent(this, SendActivity.class);
		intent.setAction(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_TEXT, text);

		startActivity(intent);
	}

}
