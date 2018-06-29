
DEVICE=`~/Library/Android/sdk/platform-tools/adb devices |grep 5555|cut -d':' -f1`

if [ -z "$DEVICE" ]
then
	echo "No android device connected, use adb connect"
	exit
fi

echo "Installing to device on $DEVICE"

echo "Configuring $DEVICE for root access..."
~/Library/Android/sdk/platform-tools/adb root
sleep 3
echo "Reconnecting to $DEVICE..."
~/Library/Android/sdk/platform-tools/adb connect $DEVICE
echo "Configuring $DEVICE for writeable boot filesystem..."
~/Library/Android/sdk/platform-tools/adb remount
echo "Deploying BB Installer to $DEVICE..."
~/Library/Android/sdk/platform-tools/adb shell rm -r /system/priv-app/com.richardmcdougall.bb*
#~/Library/Android/sdk/platform-tools/adb install -rg release/bbinstaller-release.apk
~/Library/Android/sdk/platform-tools/adb install -g release/bbinstaller-release.apk
~/Library/Android/sdk/platform-tools/adb shell cp -rp  /data/app/com.richardmcdougall.bbinstaller* /system/priv-app
~/Library/Android/sdk/platform-tools/adb shell pm uninstall com.richardmcdougall.bbinstaller
echo "Rebooting $DEVICE..."
~/Library/Android/sdk/platform-tools/adb reboot

#~/Library/Android/sdk/platform-tools/adb shell am startservice com.richardmcdougall.bbinstaller/.Installer
