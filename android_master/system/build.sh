#!/bin/sh

cp  ../../bb/build/outputs/apk/debug/bb-debug.apk  .
java -jar ../signapk.jar ../keys/platform.x509.pem ../keys/platform.pk8 bb-debug.apk  bb-signed.apk
/Users/rmc/Library/Android/sdk//build-tools/22.0.1/zipalign -f -v 4 bb-signed.apk bb-aligned.apk
~/Library/Android/sdk/platform-tools/adb install -r bb-signed.apk
~/Library/Android/sdk/platform-tools/adb shell am start com.richardmcdougall.bb
