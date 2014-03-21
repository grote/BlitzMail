package de.grobox.blitzmail;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class EncryptedEditTextPreference extends EditTextPreference {

	Crypto crypto;

	public EncryptedEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		crypto = new Crypto(context);
	}

	public EncryptedEditTextPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		crypto = new Crypto(context);
	}

	public EncryptedEditTextPreference(Context context) {
		super(context);
		crypto = new Crypto(context);
	}

	@Override
	public String getText() {		
		String value = super.getText();
		if(value == null) {
			return null;
		} else {
			try {
				return crypto.decrypt(value);
			}
			catch(Exception e) {
				return null;
			}
		}
	}

	@Override
	protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
		super.setText(restoreValue ? getPersistedString(null) : (String) defaultValue);
	}

	@Override
	public void setText(String text) {
		if(text == null) {
			super.setText(null);
			return;
		}
		super.setText(crypto.encrypt(text));
	}
}