
adb disconnect
adb root
adb disable-verity
adb remount
adb shell rm -r /system/priv-app/com.richardmcdougall.bb*
adb shell pm uninstall com.richardmcdougall.bbinstaller
adb shell pm uninstall com.richardmcdougall.bb
adb reboot
sleep 20
adb disconnect