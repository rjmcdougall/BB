#!/bin/sh

echo "IP Address?"
read ip

~/Library/Android/sdk/platform-tools/adb connect ${ip}

