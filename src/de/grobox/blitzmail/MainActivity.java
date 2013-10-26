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

import java.util.Iterator;

import org.json.JSONObject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;

@SuppressWarnings("deprecation")
public class MainActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		addSendNowPref();

		setPrefState();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("pref_smtp_auth")) {
			setPrefState();
		}
	}


	private void setPrefState() {
		CheckBoxPreference authPref = (CheckBoxPreference) findPreference("pref_smtp_auth");
		if(authPref.isChecked()) {
			findPreference("pref_smtp_encryption").setEnabled(true);
			findPreference("pref_smtp_user").setEnabled(true);
			findPreference("pref_smtp_pass").setEnabled(true);
		} else {
			findPreference("pref_smtp_encryption").setEnabled(false);
			findPreference("pref_smtp_user").setEnabled(false);
			findPreference("pref_smtp_pass").setEnabled(false);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

		// recreate send now preference
		PreferenceCategory cat = (PreferenceCategory) findPreference("pref_sending");
		if(cat.findPreference("pref_send_now") != null) {
			cat.removePreference(findPreference("pref_send_now"));
		}
		addSendNowPref();
	}

	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	private void addSendNowPref() {
		JSONObject mails = MailStorage.getMails(this);

		if(mails != null && mails.length() > 0) {
			PreferenceCategory targetCategory = (PreferenceCategory) findPreference("pref_sending");

			Preference pref = new Preference(this);
			pref.setKey("pref_send_now");
			pref.setTitle(R.string.pref_send_now);
			pref.setSummary(String.format(getResources().getString(R.string.pref_send_now_summary), mails.length()));

			pref.setOnPreferenceClickListener(new OnPreferenceClickListener(){
				public boolean onPreferenceClick(Preference preference) {
					sendNow();

					return true;
				}
			});

			targetCategory.addPreference(pref);
		}
	}

	private void sendNow() {
		JSONObject mails = MailStorage.getMails(this);

		Iterator<?> i = mails.keys();

		while(i.hasNext()) {
			String mail = mails.opt((String) i.next()).toString();

			Intent intent = new Intent(this, SendActivity.class);
			intent.setAction("BlitzMailReSend");
			intent.putExtra("mail", mail);
			startActivity(intent);
		}

		((PreferenceCategory) findPreference("pref_sending")).removePreference(findPreference("pref_send_now"));
	}

}
