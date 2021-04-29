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
import android.content.SharedPreferences;
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
				dialog.cancel();
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

				if (msg.trim().isEmpty()) {
					Toast.makeText(NoteActivity.this, R.string.warning_nothing_to_send, Toast.LENGTH_SHORT).show();
				} else {
					sendMail(msg);
					textView.setText("");
				}
				finish();
			}
		})
		.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				textView.setText("");
				finish();
			}
		});

		// Create the AlertDialog object and show it
		Dialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();

		// stretch horizontally across screen
		Window window = dialog.getWindow();
		window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

		// Open keyboard
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// restore note if there is one to restore
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		String savedNote = prefs.getString("note", null);

		if (savedNote != null && !savedNote.isEmpty()) {
			// pre-fill with saved note
			textView.setText(savedNote);
			textView.setSelection(prefs.getInt("selStart", 0), prefs.getInt("selEnd", 0));
		} else {
			// pre-fill with clipboard content note
			ClipData clip = ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).getPrimaryClip();
			if (clip != null) {
				String text = clip.getItemAt(0).getText().toString();
				if (!text.isEmpty()) {
					// only pre-fill a particular clipboard content once
					int lastClip = text.hashCode();
					if (lastClip != prefs.getInt("lastClip", 0)) {
						textView.setText(text);
						textView.selectAll();
						// we use the hashcode to avoid storing sensitive data
						prefs.edit().putInt("lastClip", lastClip).apply();
					}
				}
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		// save note in case app gets killed
		SharedPreferences.Editor prefs = getPreferences(MODE_PRIVATE).edit();
		prefs.putString("note", textView.getText().toString());
		prefs.putInt("selStart", textView.getSelectionStart());
		prefs.putInt("selEnd", textView.getSelectionEnd());
		prefs.apply();
	}

	private void sendMail(CharSequence text) {
		Intent intent = new Intent(this, SendActivity.class);
		intent.setAction(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_TEXT, text);

		startActivity(intent);
	}

}
