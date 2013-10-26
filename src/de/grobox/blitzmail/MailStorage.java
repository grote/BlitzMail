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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class MailStorage {

	static public JSONObject getMails(Context context) {
		SharedPreferences sharedPref = context.getSharedPreferences("BlitzMail", Context.MODE_PRIVATE);
		String mails_str = sharedPref.getString("mails", null);

		JSONObject mails = new JSONObject();

		if(mails_str != null) {
			try {
				mails = new JSONObject(mails_str);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		return mails;
	}

	static public void saveMail(Context context, JSONObject mail) {
		SharedPreferences sharedPref = context.getSharedPreferences("BlitzMail", Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = sharedPref.edit();

		JSONObject mails = getMails(context);

		try {
			mails.put(mail.getString("id"), mail);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		String mails_str = null;
		try {
			mails_str = mails.toString(4);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		prefEditor.putString("mails", mails_str);
		prefEditor.commit();
	}

	static public void deleteMail(Context context, String id) {
		SharedPreferences sharedPref = context.getSharedPreferences("BlitzMail", Context.MODE_PRIVATE);
		SharedPreferences.Editor prefEditor = sharedPref.edit();

		JSONObject mails = getMails(context);

		mails.remove(id);

		String mails_str = null;
		try {
			mails_str = mails.toString(4);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		Log.d("MailStorage", "Removing mail with id " + id);

		prefEditor.putString("mails", mails_str);
		prefEditor.commit();
	}
}
