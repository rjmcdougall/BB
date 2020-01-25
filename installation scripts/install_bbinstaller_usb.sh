

~/Library/Android/sdk/platform-tools/adb root
~/Library/Android/sdk/platform-tools/adb remount
~/Library/Android/sdk/platform-tools/adb shell rm -r /system/priv-app/com.richardmcdougall.bb*
~/Library/Android/sdk/platform-tools/adb reboot
sleep 40
~/Library/Android/sdk/platform-tools/adb install -rg release/bbinstaller-release.apk
~/Library/Android/sdk/platform-tools/adb root
~/Library/Android/sdk/platform-tools/adb remount
~/Library/Android/sdk/platform-tools/adb shell cp -rp  /data/app/com.richardmcdougall.bbinstaller* /system/priv-app
~/Library/Android/sdk/platform-tools/adb shell pm uninstall com.richardmcdougall.bbinstaller
~/Library/Android/sdk/platform-tools/adb reboot
#sleep 30
#~/Library/Android/sdk/platform-tools/adb shell am startservice com.richardmcdougall.bbinstaller/.Installer

