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

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import de.grobox.blitzmail.send.SendActivity;

import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

public class NoteActivity extends AppCompatActivity {

	private EditText textView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		View mView = getLayoutInflater().inflate(R.layout.activity_note, null);
		textView = (EditText) mView.findViewById(R.id.text);

		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme)
		.setView(mView)
		.setTitle(R.string.note_name)
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				textView.setText(null);
				saveText(null);

				finish();
			}
		})
		.setNeutralButton(R.string.save, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				finish();
			}
		})
		.setPositiveButton(R.string.send_mail, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int id) {
				String msg = textView.getText().toString();

				if(msg.length() < 1) {
					Toast.makeText(NoteActivity.this, R.string.warning_nothing_to_send, Toast.LENGTH_SHORT).show();
				} else {
					sendMail(msg);

					saveText("");
					textView.setText(null);
				}
				finish();
			}
		})
		.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				finish();
			}
		});

		// Create the AlertDialog object and show it
		Dialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();

		 ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
		 ClipData cd = cm.getPrimaryClip();
		 if (cd != null) {
			 CharSequence text = cd.getItemAt(0).getText();
			 if (text != null && text.length() > 0) {
				textView.setText(text.toString());
				textView.setSelection(0, text.length());
			 }
		 }

		// stretch horizontally across screen
		Window window = dialog.getWindow();
		window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		// Open keyboard
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	}

	@Override
	protected void onResume() {
		super.onResume();

		String text = getPreferences(MODE_PRIVATE).getString("note", null);

		// restore note if there is one to restore
		if(text != null) {
			textView.setText(text);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		CharSequence text = textView.getText();

		// save note in case app gets killed
		if(text.length() > 0) {
			saveText(text.toString());
		}
	}

	private void saveText(String text) {
		getPreferences(MODE_PRIVATE)
				.edit()
				.putString("note", text)
				.apply();
	}

	private void sendMail(CharSequence text) {
		Intent intent = new Intent(this, SendActivity.class);
		intent.setAction(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, text);

		startActivity(intent);
	}

}
