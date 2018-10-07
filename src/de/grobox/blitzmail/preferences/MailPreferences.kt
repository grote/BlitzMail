package de.grobox.blitzmail.preferences

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import de.grobox.blitzmail.crypto.Crypto
import de.grobox.blitzmail.R
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.*


fun getProperties(c: Context): Properties {
    val pref = PreferenceManager.getDefaultSharedPreferences(c)
    val crypto = Crypto(c)

    val recipients = pref.getString("pref_recipient", null)
    val sender = pref.getString("pref_sender", null)

    val server = pref.getString("pref_smtp_server", null)
    val port = pref.getString("pref_smtp_port", null)
    val auth = pref.getBoolean("pref_smtp_auth", false)
    val user = pref.getString("pref_smtp_user", null)
    var password = pref.getString("pref_smtp_pass", null)

    if (recipients == null) throw RuntimeException(c.getString(R.string.error_option_not_set) + " " + c.getString(R.string.pref_recipient))
    if (sender == null) throw RuntimeException(c.getString(R.string.error_option_not_set) + " " + c.getString(R.string.pref_sender))
    if (server == null) throw RuntimeException(c.getString(R.string.error_option_not_set) + " " + c.getString(R.string.pref_smtp_server))
    if (port == null) throw RuntimeException(c.getString(R.string.error_option_not_set) + " " + c.getString(R.string.pref_smtp_port))
    if (auth) {
        if (user == null) throw RuntimeException(c.getString(R.string.error_option_not_set) + " " + c.getString(R.string.pref_smtp_user))
        if (password == null) throw RuntimeException(c.getString(R.string.error_option_not_set) + " " + c.getString(R.string.pref_smtp_pass))

        // Decrypt password
        password = crypto.decrypt(password)
    }

    val props = Properties()
    props.setProperty("mail.transport.protocol", "smtp")
    props.setProperty("mail.host", server)
    val appName = c.getString(R.string.app_name)
    val from = (pref.getString("pref_sender_name", appName) ?: appName) + " <" + sender + ">"
    props.setProperty("mail.user", from)
    props.setProperty("mail.from", from)
    props.setProperty("mail.smtp.auth", auth.toString())
    props.setProperty("mail.smtp.port", port)
    props.setProperty("mail.smtp.recipients", recipients)
    props.setProperty("mail.smtp.quitwait", "false")

    if (auth) {
        // set username and password
        props.setProperty("mail.smtp.user", user)
        props.setProperty("mail.smtp.pass", password)

        // set encryption properties
        if (pref.getString("pref_smtp_encryption", "") == "ssl") {
            Log.i("SendActivity", "Using SSL Encryption...")
            props.setProperty("mail.smtp.ssl.enable", "true")
        } else if (pref.getString("pref_smtp_encryption", "") == "tls") {
            Log.i("SendActivity", "Using TLS Encryption...")
            props.setProperty("mail.smtp.starttls.enable", "true")
        }
        props.setProperty("mail.smtp.ssl.checkserveridentity", "true")
    } else {
        // set some hostname for proper HELO greeting
        props.setProperty("mail.smtp.localhost", "android.com")
    }

    // try to get proper hostname and set fake one if failed
    if (props.getProperty("mail.smtp.localhost", "") == "android.com") {
        var hostname = ""
        try {
            hostname = InetAddress.getLocalHost().hostName
        } catch (e: UnknownHostException) {
            // do nothing
        }

        if (hostname != "localhost") {
            props.setProperty("mail.smtp.localhost", hostname)
        }
    }

    return props
}
