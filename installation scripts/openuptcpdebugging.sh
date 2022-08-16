$ adb shell
hikey960:/ $ su
hikey960:/ # setprop persist.adb.tcp.port 5555
hikey960:/ # exit
hikey960:/ $ exit
$ adb reboot
$ adb connect 192.168.196.77:5555
connected to 192.168.196.77:5555