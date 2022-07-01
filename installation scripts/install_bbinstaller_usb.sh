
#allow system files to be edited.   https://wiki.friendlyelec.com/wiki/index.php/NanoPi_M4B
adb disconnect
adb root
adb disable-verity
adb reboot
sleep 20

#remove any previous installations. This will error if a first time install.
adb root
adb remount
adb shell rm -r /system/priv-app/com.richardmcdougall.bb*
adb shell pm uninstall com.richardmcdougall.bb
adb reboot
sleep 20

#disable the android volume limit.  Untested as a script. Instructions here:
#https://docs.google.com/document/d/1AupwDhrRKcfs7SzOsjg5rXeNmyATDJ-DuuIfkldzVwE/edit
adb root
adb pull /system/build.prop
echo 'VNCSERVERS="1:root"' >> build.prop
adb shell
su
mount -o rw,remount /system
exit
exit
adb push build.prop /system/
adb remount
adb reboot

#install bbinstaller -- note that bbinstaller requires system permissions that are
#required to install a privaledged app.
adb root
adb remount
adb install -r ../bbinstaller/build/outputs/apk/release/bbinstaller-release.apk
adb push privapp-permissions-com.richardmcdougall.bbinstaller.xml /etc/permissions
adb push com.richardmcdougall.bb.xml /etc/default-permissions/com.richardmcdouall.bb.xml
adb shell cp -rp  /data/app/com.richardmcdougall.bbinstaller* /system/priv-app

#after making this a privaledged app, uninstall.
adb shell pm uninstall --user 0 com.richardmcdougall.bbinstaller
adb reboot
sleep 20
adb disconnect

