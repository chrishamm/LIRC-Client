LIRC-Client app for Android
==============

Summary
--------------

This application is a sending client for LIRC (http://www.lirc.org).
It can be used to query device information from a running LIRC server that has been started with the "--listen" parameter
and to organize custom activities in order to start and stop multiple devices at once.

License
--------------

The core application is published under the terms of the GNU Public License v3. You can find a copy of it in "licenses/gpl-3.0.txt".
In addition, this app uses an LIRC library to communicate with LIRC servers in "src/com/chham/lirc", which is licensed
under the terms of the Apache Open Source License v2. You may use this particular library in your own applications.
You can find a copy of the Apache Open Source License v2 in "licenses/LICENSE-2.0.txt".

Notes
--------------

You may modify and/or redistribute contents of this repository, but I'd be happy to receive pull requests if you decide
to implement additional features, so I can update the appropriate binary on the Play Store.

There are some features I have not implemented yet, so if you feel like coding a bit, please take a look at "TODO.txt".