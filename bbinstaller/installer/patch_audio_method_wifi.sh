#!/bin/sh

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

adb root
sleep 10

# Get ready to copy some config files
adb remount
sleep 10

# Set Audio offload disable to enable variable rate player
~/Library/Android/sdk/platform-tools/adb push init.qcom.post_boot.sh  /etc


echo "Rebooting $DEVICE..."
~/Library/Android/sdk/platform-tools/adb reboot




