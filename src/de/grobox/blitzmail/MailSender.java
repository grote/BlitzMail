package de.grobox.blitzmail;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.provider.JSSEProvider;

import android.util.Log;

import java.io.ByteArrayInputStream;   
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Security;
import java.util.Properties;

public class MailSender extends javax.mail.Authenticator {
	private String sender;
	private String recipients;
	private Properties props;
	private Session session;

	static {
		Security.addProvider(new JSSEProvider());   
	}  

	public MailSender(Properties props) {		
		this.props = props;
		this.sender = props.getProperty("mail.smtp.sender", "");
		this.recipients = props.getProperty("mail.smtp.recipients", "");
		
		// get a new mail session, don't use the default one to ensure fresh sessions
		session = Session.getInstance(props, this);
	}

	protected PasswordAuthentication getPasswordAuthentication() {   
		return new PasswordAuthentication(props.getProperty("mail.smtp.user", ""), props.getProperty("mail.smtp.pass", ""));   
	}   

	public synchronized void sendMail(String subject, String body,  String cc, String bcc) throws Exception {
		try {
			MimeMessage message = new MimeMessage(session);   
			DataHandler handler = new DataHandler(new ByteArrayDataSource(body.getBytes(), "text/plain"));   
			message.setSender(new InternetAddress(sender));
			message.setSubject(subject);
			message.setDataHandler(handler);
						
			if(recipients.indexOf(',') > 0) {   
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipients));
			} else {  
				message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipients));
			}
			
			if(cc != null) {
				if(cc.indexOf(',') > 0) {   
					message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
				} else {  
					message.setRecipient(Message.RecipientType.CC, new InternetAddress(cc));
				}
			}

			if(bcc != null) {
				if(bcc.indexOf(',') > 0) {   
					message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
				} else {  
					message.setRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));
				}
			}
			
			Transport.send(message);
		}
		catch(Exception e) {
			Log.d("MailSender", "ERROR: " + e.getMessage(), e);
			throw e;
		}
	}   

	public class ByteArrayDataSource implements DataSource {   
		private byte[] data;   
		private String type;   

		public ByteArrayDataSource(byte[] data, String type) {   
			super();   
			this.data = data;   
			this.type = type;   
		}   

		public ByteArrayDataSource(byte[] data) {   
			super();   
			this.data = data;   
		}   

		public void setType(String type) {   
			this.type = type;   
		}   

		public String getContentType() {   
			if (type == null)   
				return "application/octet-stream";   
			else  
				return type;   
		}   

		public InputStream getInputStream() throws IOException {   
			return new ByteArrayInputStream(data);   
		}   

		public String getName() {   
			return "ByteArrayDataSource";   
		}   

		public OutputStream getOutputStream() throws IOException {   
			throw new IOException("Not Supported");   
		}   
	}   
}  