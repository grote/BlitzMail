package de.grobox.blitzmail;

import android.content.Context;

import com.provider.JSSEProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.Security;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class MailSender extends javax.mail.Authenticator {
	private Context context;
	private Properties props;
	private JSONObject mail;
	private Session session;

	static {
		Security.addProvider(new JSSEProvider());
	}

	public MailSender(Context context, Properties props, JSONObject mail) {
		this.context = context;
		this.props = props;
		this.mail = mail;

		// get a new mail session, don't use the default one to ensure fresh sessions
		session = Session.getInstance(props, this);
	}

	protected PasswordAuthentication getPasswordAuthentication() {
		return new PasswordAuthentication(props.getProperty("mail.smtp.user", ""), props.getProperty("mail.smtp.pass", ""));
	}

	public synchronized void sendMail() throws Exception {

		if(mail.has("body")) {
			MimeMessage message = getMessage();
			message.setText(mail.optString("body"));
			message.setSubject(mail.optString("subject"));

			String cc = mail.optString("cc", null);
			if(cc != null) {
				if(cc.indexOf(',') > 0) {
					message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
				} else {
					message.setRecipient(Message.RecipientType.CC, new InternetAddress(cc));
				}
			}

			Transport.send(message);
		}
		else if(mail.has("attachments")) {
			JSONArray attachments = mail.getJSONArray("attachments");

			Multipart mp = new MimeMultipart();

			for(int i = 0; i < attachments.length(); i++) {
				JSONObject attachment = attachments.getJSONObject(i);

				MimeBodyPart mbp = getMimeBodyPart(attachment);

				if(mbp != null) {
					mp.addBodyPart(mbp);
				}
			}

			if(mp.getCount() == 0) {
				throw new RuntimeException(context.getString(R.string.error_attachment));
			}

			// actually send message
			MimeMessage message = getMessage();
			message.setContent(mp);
			message.setSubject(mail.optString("subject"));
			Transport.send(message);
		}
	}

	private MimeBodyPart getMimeBodyPart(JSONObject attachment) throws IOException, MessagingException {
		MimeBodyPart mbp = new MimeBodyPart();

		try {
			mbp.attachFile(attachment.getString("path"));
			mbp.setFileName(attachment.getString("filename"));
			mbp.setHeader("Content-Type", attachment.getString("mimeType") + "; name=" + mbp.getFileName());
		} catch(JSONException e) {
			e.printStackTrace();
		}

		return mbp;
	}

	private MimeMessage getMessage() throws Exception {
		MimeMessage message = new MimeMessage(session);
		message.setFrom(); // uses mail.user property
		message.setSentDate(new Date());

		String recipients = props.getProperty("mail.smtp.recipients", "");

		if(recipients.indexOf(',') > 0) {
			message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
		} else {
			message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));
		}

		return message;
	}
}