# ledger-wallet-android
-----------------------

Ledger Wallet Android application


## Prerequisites:

Install Java 1.7 or later and set JAVA_HOME

Install Gradle and set GRADLE_HOME

Install Android SDK for API 22 (Lollipop 5.1.1) and set ANDROID_HOME

(You can omit all Atom images, Android Wear and Android TV stuff)


Through Android SDK manager:

Install Android support repository

Install Google Play Services

Install Google repository

Install Google USB driver


## Building:

At the command prompt, type `gradlew.bat build` . If using Git Gui use `./gradlew build`

If you get an unexpected warning related to gradle_daemon and not being able to reserve enough memory space, edit gradle.properties and change `-Xmx2048m` to `-Xmx1024m` . This happens in 32-bit Windows due to an OS limit of 3GB total physical memory.

