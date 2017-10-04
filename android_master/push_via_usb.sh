#!/bin/sh

echo "THis pushes the boot.prop file and other config files via USB"
echo "Hope you remembered to set the name and type in boot.prop"

~/Library/Android/sdk/platform-tools/adb install -r -g bb.apk
~/Library/Android/sdk/platform-tools/adb root
sleep 10
~/Library/Android/sdk/platform-tools/adb shell setprop persist.adb.tcp.port 5555
~/Library/Android/sdk/platform-tools/adb remount
sleep 10
~/Library/Android/sdk/platform-tools/adb push build.prop  /system/build.prop 
~/Library/Android/sdk/platform-tools/adb push audio_policy.conf  /system/etc/audio_policy.conf  

# Set so no internet connection is required to bring up WIFI
# https://github.com/LineageOS/android_frameworks_base/blob/cm-14.1/core/java/android/provider/Settings.java#L8273
#
#public static final int CAPTIVE_PORTAL_MODE_IGNORE = 0;
#public static final int CAPTIVE_PORTAL_MODE_PROMPT = 1;
#public static final int CAPTIVE_PORTAL_MODE_AVOID = 2;

~/Library/Android/sdk/platform-tools/adb shell settings put global settings put global captive_portal_mode 0

~/Library/Android/sdk/platform-tools/adb reboot





