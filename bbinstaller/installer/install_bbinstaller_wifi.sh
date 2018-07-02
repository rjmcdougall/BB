#!/bin/bash
#set -xe

#############################################################################
###
### Usage: ./installer_bbinstaller_wifh.sh [path/to/package.apk]
###
### * First, connect to your board or RPI using adb
### * This will install the latest BB Installer (by default, $PACKAGE below)
###   or the package you explicitly provide.
### * Lastly, this will reboot your board / RPI
###
#############################################################################

PATH="${PATH}:~/Library/Android/sdk/platform-tools"
ADB=`which adb`

### Figure out what package to install
PACKAGE=release/bbinstaller-release.apk
if [ ! -z "$1" ]
then
    PACKAGE=$1
fi

### Does the package exist?
if [ ! -e "$PACKAGE" ]
then
    echo "No such file: $PACKAGE"
    exit 1
fi

### Find the device
DEVICE=`$ADB devices |grep 5555|cut -d':' -f1`

if [ -z "$DEVICE" ]
then
	echo "No android device connected, use adb connect <ip>"
	exit 1
fi

### Proceed to install
echo "Installing $PACKAGE to device on $DEVICE using $ADB"

echo "Configuring $DEVICE for root access..."
$ADB root
sleep 3

echo "Reconnecting to $DEVICE..."
$ADB connect $DEVICE

echo "Configuring $DEVICE for writeable boot filesystem..."
$ADB remount

echo "Deploying BB Installer to $DEVICE..."
$ADB install -r $PACKAGE
$ADB shell cp -rp  /data/app/com.richardmcdougall.bbinstaller* /system/priv-app
$ADB shell pm uninstall com.richardmcdougall.bbinstaller

echo "Rebooting $DEVICE..."
$ADB reboot

#~/Library/Android/sdk/platform-tools/adb shell am startservice com.richardmcdougall.bbinstaller/.Installer


