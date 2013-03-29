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

import java.util.Properties;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class SendActivity extends Activity {
	protected ProgressDialog progressDialog;
	
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// before doing anything show progress dialog to user
		progressDialog = ProgressDialog.show(this, getString(R.string.sending_mail), getString(R.string.please_wait), true);
        progressDialog.setCancelable(false);
				
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    
	    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

	    if(Intent.ACTION_SEND.equals(action)) {
	    	String text    = intent.getStringExtra(Intent.EXTRA_TEXT);
//	    	String email   = intent.getStringExtra(Intent.EXTRA_EMAIL);
	    	String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
	    	String cc      = intent.getStringExtra(Intent.EXTRA_CC);
	    	String bcc     = intent.getStringExtra(Intent.EXTRA_BCC);

	    	// Check for empty content
	    	if(subject == null && text != null) {
	    		// cut all characters from subject after the 128th
	    		subject = text.substring(0, (text.length() < 128) ? text.length() : 128);
	    	} else if(subject != null && text == null) {
	    		text = subject;
	    	} else if(subject == null && text == null) {
	    		Log.e("Instant Mail", "Did not send mail, because subject and body empty.");
	    		showError(getString(R.string.error_no_body_no_subject));
	    		return;
	    	}
	    	
	    	Properties prefs;
	    	try {
	    		prefs = getPrefs();
	    	}
	    	catch(Exception e) {
	    		Log.i("SendActivity", "ERROR: " + e.getMessage(), e);
	    		showError(e.getMessage());
	    		return;
	    	}
	    	
	    	// Start Mail Task
	    	final AsyncMailTask mail = new AsyncMailTask(this, prefs);
	    	
	    	mail.body    = text;
	    	mail.subject = subject;
	    	mail.cc      = cc;
	    	mail.bcc     = bcc;

	    	mail.execute();
	    }	    
	}

	private Properties getPrefs() {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		Crypto crypto = new Crypto(this);

		String recipients = pref.getString("pref_recipient", null);
		String sender = pref.getString("pref_sender", null);
		
		String server = pref.getString("pref_smtp_server", null);
		String port = pref.getString("pref_smtp_port", null);
		Boolean auth = pref.getBoolean("pref_smtp_auth", false);
		String user = pref.getString("pref_smtp_user", null);
		String password = pref.getString("pref_smtp_pass", null);
		
		if(recipients == null) throw new RuntimeException(getString(R.string.error_option_not_set)+" "+getString(R.string.pref_recipient));
		if(sender == null)     throw new RuntimeException(getString(R.string.error_option_not_set)+" "+getString(R.string.pref_sender));
		if(server == null)     throw new RuntimeException(getString(R.string.error_option_not_set)+" "+getString(R.string.pref_smtp_server));
		if(port == null)       throw new RuntimeException(getString(R.string.error_option_not_set)+" "+getString(R.string.pref_smtp_port));
		if(auth) {
			if(user == null)     throw new RuntimeException(getString(R.string.error_option_not_set)+" "+getString(R.string.pref_smtp_user));
			if(password == null) throw new RuntimeException(getString(R.string.error_option_not_set)+" "+getString(R.string.pref_smtp_pass));
			
			// Decrypt password
			password = crypto.decrypt(password);
		}

		Properties props = new Properties();
		props.setProperty("mail.transport.protocol", "smtp");
		props.setProperty("mail.host", server);
		props.setProperty("mail.smtp.auth", String.valueOf(auth));
		props.setProperty("mail.smtp.port", port);
		props.setProperty("mail.smtp.sender", sender);
		props.setProperty("mail.smtp.recipients", recipients);
		props.setProperty("mail.smtp.quitwait", "false");
		
		if(auth) {
			// set username and password
			props.setProperty("mail.smtp.user", user);
			props.setProperty("mail.smtp.pass", password);

			// set encryption properties
			if(pref.getString("pref_smtp_encryption", "").equals("ssl")) {
				Log.i("SendActivity", "Using SSL Encryption...");
				props.setProperty("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
				props.setProperty("mail.smtp.socketFactory.port", port);
				props.setProperty("mail.smtp.socketFactory.fallback", "false");
			} else if(pref.getString("pref_smtp_encryption", "").equals("tls")) {
				Log.i("SendActivity", "Using TLS Encryption...");
				props.setProperty("mail.smtp.starttls.enable", "true");
			}
		} else {
			// set some hostname for proper HELO greeting
			props.setProperty("mail.smtp.localhost",  "android.com");
		}
		
		return props;
	}
	
	private void showError(String text) {
		// close progress dialog first
		progressDialog.cancel();
				
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setTitle(getString(R.string.error));
	    builder.setMessage(text);
	    builder.setIcon(android.R.drawable.ic_dialog_alert);
	    
	    // Add the buttons
	    builder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
	    	public void onClick(DialogInterface dialog, int id) {
	    		// User clicked OK button, close this Activity
	    		SendActivity.this.finish();
	    	}
	    });
	    // Create and show the AlertDialog
	    AlertDialog dialog = builder.create();
	    dialog.setCanceledOnTouchOutside(false);
	    dialog.show();
	}
}
