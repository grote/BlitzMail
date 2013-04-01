BlitzMail
=========
Share content via email with just one click!

BlitzMail is an Android app that allows you to set up your email account once
and then use it to send emails to an address of your choice. This comes in handy when you need to send a lot of things via email, because you are in a low connectivity area (e.g. subway) and want to remember things to look at later.

It is available as installable app binary at the
* [F-Droid repository](http://f-droid.org/repository/browse/?fdid=de.grobox.blitzmail) at no cost, but please [Flattr me](https://flattr.com/thing/1217295) if you like it!
* [Google Play Store](https://play.google.com/store/apps/details?id=de.grobox.blitzmail) for a nominal fee.

The SMTP password is stored encrypted with a built-in key and salted with your device ID.
This is not fully secure, but should provide reasonable protection for your password.

[![Flattr me](https://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/thing/1217295)

Screenshots
-----------

![BlitzMail Settings](/artwork/MainActivity.png)
![BlitzMail Share Via](/artwork/ShareVia.png)
![BlitzMail Sending Mail](/artwork/SendMailActivity.png)

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
