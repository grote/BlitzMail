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

[![Available on F-Droid](https://f-droid.org/wiki/images/c/c4/F-Droid-button_available-on.png)](https://f-droid.org/repository/browse/?fdid=de.grobox.blitzmail)
[![Available on Google Play](https://developer.android.com/images/brand/en_app_rgb_wo_45.png)](https://play.google.com/store/apps/details?id=de.grobox.blitzmail.pro)

Screenshots
-----------

[<img src="/artwork/screen_ShareVia.png" width="200">](/artwork/screen_ShareVia.png)
[<img src="/artwork/screen_SendMailActivity.png" width="200">](/artwork/screen_SendMailActivity.png)
[<img src="/artwork/screen_NoteActivity.png" width="200">](/artwork/screen_NoteActivity.png)
[<img src="/artwork/screen_MainActivity.png" width="200">](/artwork/screen_MainActivity.png)

Building from Source
--------------------
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
