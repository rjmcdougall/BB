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
~/Library/Android/sdk/platform-tools/adb reboot





