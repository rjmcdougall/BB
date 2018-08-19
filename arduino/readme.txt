# install the arduino ide
https://www.arduino.cc/en/Main/Software

# install feather board manager:
https://learn.adafruit.com/adafruit-feather-m0-basic-proto/using-with-arduino-ide
download the SAMD version

# get the radiohead library
# install in your arduino libraries dir
wget http://www.airspayce.com/mikem/arduino/RadioHead/RadioHead-1.85.zip

# get the cmdmessenger library
available via 'library manager'

# get the tinygps library
# install in your arduino libraries dir
wget https://github.com/mikalhart/TinyGPS/archive/v13.zip

# figure out what port the arduino is on. Go to the 'port' menu
# if there is only one USB entry, that's the on. Otherwise, look at 
# the output of:
ls /dev/tty.usb*


