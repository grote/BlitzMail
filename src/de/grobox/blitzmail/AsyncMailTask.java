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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import android.app.PendingIntent;
import android.os.AsyncTask;
import android.util.Log;

public class AsyncMailTask extends AsyncTask<Void, Void, Boolean> {
	private SendActivity activity;
	private Properties props;
	private Exception e;

	public String body;
	public String subject;
	public String email;
	public String cc;
	public String bcc;

	public AsyncMailTask(SendActivity activity, Properties props) {
		this.activity = activity;
		this.props = props;
	}

	@Override
	protected Boolean doInBackground(Void... params) {
		// try to get proper hostname and set fake one if failed
		if(props.getProperty("mail.smtp.localhost", "").equals("android.com")) {
			String hostname = "";
			try {
				hostname = InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				// do nothing
			}
			if(!hostname.equals("localhost")) {
				props.setProperty("mail.smtp.localhost",  hostname);
			}
		}

		MailSender sender = new MailSender(props);

		try {
			sender.sendMail(subject, body, cc, bcc);
		} catch(Exception e) {
			Log.d("AsyncMailTask", "ERROR: " + e.getMessage());

			// remember exception for when task is finished
			this.e = e;

			return false;
		}		
		return true;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		String msg;

		// set progress notification to finished
		activity.mBuilder.setProgress(0, 0, false);
		activity.mBuilder.setOngoing(false);

		// set dialog to auto close when clicked
		activity.mBuilder.setAutoCancel(true);

		if(result) {
			// Everything went fine
			activity.mBuilder.setSmallIcon(R.drawable.ic_launcher);
			activity.mBuilder.setContentTitle(activity.getString(R.string.sent_mail));
			activity.notifyIntent.putExtra("ContentTitle", activity.getString(R.string.sent_mail));
			msg = subject;
		} else {
			activity.mBuilder.setContentTitle(activity.getString(R.string.error));
			activity.notifyIntent.putExtra("ContentTitle", activity.getString(R.string.error));
			activity.mBuilder.setSmallIcon(android.R.drawable.ic_dialog_alert);

			Log.d("AsyncMailTask", e.getClass().getCanonicalName());

			if(e.getClass().getCanonicalName().equals("javax.mail.AuthenticationFailedException")) {
				msg = activity.getString(R.string.error_auth_failed);
			} else {
				msg = e.getMessage();
			}
			// get and show the cause for the exception if it exists
			Throwable ecause = e.getCause(); 
			if(ecause != null) {
				Log.d("AsyncMailTask", ecause.getClass().getCanonicalName());
				msg += "\n" + ecause.getMessage();
			}
		}

		// Update the notification
		activity.mBuilder.setContentText(msg);
		activity.notifyIntent.putExtra("ContentText", msg);
		activity.mBuilder.setContentIntent(PendingIntent.getActivity(activity, 0, activity.notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT));
		activity.mNotifyManager.notify(0, activity.mBuilder.build());

		activity.finish();
	}
}