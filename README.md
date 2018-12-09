BlitzMail
=========
Share content via email with just one click!

![BlitzMail Logo](/res/drawable-xhdpi/ic_launcher.png)

BlitzMail is an Android app that allows you to set up your email account once
and then use it to send emails or quick notes to an address of your choice.
This comes in handy when you need to send a lot of things via email,
because you are in a low connectivity area (e.g. subway)
and want to remember things to look at later.

It also works when you are offline.
BlitzMail then queues your emails
and allows you to send them later when you have connectivity again.

The SMTP password is stored encrypted with a built-in key and salted with your device ID.
This is not fully secure, but should provide reasonable protection for your password.

[![Flattr me](https://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/thing/1217295)
[![Follow @BlitzMailApp](artwork/twitter.png)](https://twitter.com/BlitzMailApp)

Get BlitzMail
-------------

[<img src="https://f-droid.org/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/de.grobox.blitzmail/)
[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
     alt="Get it on Google Play"
     height="80">](https://play.google.com/store/apps/details?id=de.grobox.blitzmail.pro)

Screenshots
-----------

[<img src="/artwork/screen_ShareVia.png" width="200">](/artwork/screen_ShareVia.png)
[<img src="/artwork/screen_SendMailActivity.png" width="200">](/artwork/screen_SendMailActivity.png)
[<img src="/artwork/screen_NoteActivity.png" width="200">](/artwork/screen_NoteActivity.png)
[<img src="/artwork/screen_MainActivity.png" width="200">](/artwork/screen_MainActivity.png)

Contributing
------------

Contributions are both encouraged and appreciated. If you want to do more than fixing bugs, please talk to me *before* doing any major work so that we can coordinate, prevent duplicated work and discuss the best approach for your undertaking.

Missing features are documented in the [issue tracker](https://github.com/grote/BlitzMail/issues?labels=enhancement&state=open). Feel free to look there for ideas or to add your own.

### Translate BlitzMail Into Your Language ###

Translating BlitzMail is very easy. Just sign up for an account at [Transifex](https://www.transifex.com). After you are logged in, go to the [BlitzMail translation project](https://www.transifex.com/projects/p/blitzmail/). There you can click the "Request language" button if your language does not exist, or if you want to improve existing translations, apply for the respective language team.

What you translate there will then be included in the next release of BlitzMail.

Please do not send translations as a pull request in GitHub since this not only causes more work, but also might overwrite other people's translations in Transifex.

### Building from Source ###

Before building BlitzMail from source, make sure to run the following command.

```bash
cp src/de/grobox/blitzmail/PrivateConstants.sample_java src/de/grobox/blitzmail/PrivateConstants.java
```

Then edit `src/de/grobox/blitzmail/PrivateConstants.java` and replace `[Please insert your encryption key here]` with the encryption key of your choice.

License
-------

![GNU AGPLv3 Image](https://www.gnu.org/graphics/agplv3-88x31.png)

This program is Free Software: You can use, study share and improve it at your
will. Specifically you can redistribute and/or modify it under the terms of the
[GNU Affero General Public License](https://www.gnu.org/licenses/agpl.html) as
published by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
