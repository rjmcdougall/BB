adb disconnect
adb root
adb disable-verity
adb remount
adb shell rm -r /system/priv-app/com.richardmcdougall.bb*
adb shell pm uninstall com.richardmcdougall.bb
adb reboot
sleep 20
adb disconnect
adb install -r ../bbinstaller/build/outputs/apk/release/bbinstaller-release.apk
adb root
adb remount
adb push privapp-permissions-com.richardmcdougall.bbinstaller.xml /etc/permissions
adb shell cp -rp  /data/app/com.richardmcdougall.bbinstaller* /system/priv-app
adb shell pm uninstall com.richardmcdougall.bbinstaller
adb reboot
sleep 20
adb disconnect
#sleep 30
#adb shell am startservice com.richardmcdougall.bbinstaller/.Installer

