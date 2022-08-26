/*  OctoWS2811 Teensy4_PinList.ino - Demonstrate use of any pins
    http://www.pjrc.com/teensy/td_libs_OctoWS2811.html

  With Teensy 4.x, you can use any group of pins.  You are not limited
  to only 8 fixed pins as with Teensy 3.x.  This example shows how to
  use only 4 pins, which are the 4 pins of the Octo28 shield which do
  not conflict with the Audio shield.

  Required Connections
  --------------------
    pin 2:  LED Strip #1
    pin 14: LED strip #2
    pin 6:  LED strip #5
    pin 5:  LED strip #8
*/

#include <OctoWS2811.h>

// Any group of digital pins may be used
const int numPins = 32;
/*
byte pinList[numPins] = {0,1,2,3,4,5,6,7,
                          8,9,10,11,12,24,25,26,
                          27,28,29,30,31,32,33,34,
                          35,36,37,38,39,40,41,13};
*/

/* Mezal Kronos */
byte pinList[numPins] = {3,2,1,0,
                          7,6,5,4,
                          11, 10, 9, 8, 
                          26, 25, 24, 12,
                          30, 29, 28, 27,
                          34, 33, 32, 31,
                          39, 40, 41, 13,
                          38, 37, 36, 35};
                          //13, 41, 40, 39};



/*
 * test teensy3
byte pinList[numPins] = {2, 14, 7, 8,
                         8, 9, 20, 21
                        };
 */

//byte pinList[numPins] = {1,0,24,25,19,18,14,15};

const int ledsPerStrip = 550;


// These buffers need to be large enough for all the pixels.
// The total number of pixels is "ledsPerStrip * numPins".
// Each pixel needs 3 bytes, so multiply by 3.  An "int" is
// 4 bytes, so divide by 4.  The array is created using "int"
// so the compiler will align it to 32 bit memory.
const int bytesPerLED = 3;  // change to 4 if using RGBW
DMAMEM int displayMemory[ledsPerStrip * numPins * bytesPerLED / 4];
int drawingMemory[ledsPerStrip * numPins * bytesPerLED / 4];

const int config = WS2811_GRB | WS2811_800kHz;

OctoWS2811 leds(ledsPerStrip, displayMemory, drawingMemory, config, numPins, pinList);
//OctoWS2811 leds(ledsPerStrip, displayMemory, drawingMemory, config);

void setup() {
  leds.begin();
  clear();
  leds.show();
}

#define OFF 0x030000

#define RED    0xFF0000
#define GREEN  0x00FF00
#define BLUE   0x0000FF
#define YELLOW 0xFFFF00
#define PINK   0xFF1088
#define ORANGE 0xE05800
#define WHITE  0xFFFFFF

// Less intense...
/*
#define RED    0x160000
#define GREEN  0x001600
#define BLUE   0x000016
#define YELLOW 0x101400
#define PINK   0x120009
#define ORANGE 0x100400
#define WHITE  0x101010
*/

int brightness = 15;

// Create a 24 bit color value from R,G,B
uint32_t rgbTo24BitColor(byte r, byte g, byte b)
{
        uint32_t c;
        c = (b * brightness) / 100;
        c <<= 8;
        c |= (r * brightness) / 100;
        c <<= 8;
        c |=  (g * brightness) / 100;
        return c;
}

uint8_t row;

//Input a value 0 to 255 to get a color value.
//The colours are a transition r - g -b - back to r
uint32_t wheel(byte WheelPos)
{
        if (WheelPos < 85) {
                return rgbTo24BitColor(WheelPos * 3, 255 - WheelPos * 3, 0);
        } else if (WheelPos < 170) {
                WheelPos -= 85;
                return rgbTo24BitColor(255 - WheelPos * 3, 0, WheelPos * 3);
        } else {
          WheelPos -= 170; 
          return rgbTo24BitColor(0, WheelPos * 3, 255 - WheelPos * 3);
        }
}

uint8_t wheel_color;
int strip_lit = 0;

#define TEST 1

int strip_colors[33];

void loop() {

  uint32_t color;

#ifndef TEST
  for (int i = numPins; i > 0; i--) {
    strip_colors[i] = strip_colors[i - 1];
  }
  color = random(2,4)%2 == 0 ? rgbTo24BitColor(0, 0, 0): wheel(wheel_color); //Chance of 1/3rd
  wheel_color += 9;
  strip_colors[0] = color;
    
  for (int strip = 0; strip < numPins; strip++) {
    setStrip(strip, 100000, strip_colors[strip]);
  }
#else
  for (int strip = 0; strip < numPins; strip++) {
    setStrip(strip, 100000, strip == strip_lit ? 0x001000 : 0x010000);
  }
  strip_lit++;
  if (strip_lit >= numPins) {
    strip_lit = 0;
  }
   
#endif
  leds.show();
  //delay(60);
  delay(300);
}


void clear()
{
  for (int i=0; i < leds.numPixels(); i++) {
    leds.setPixel(i, 0);
  }
  leds.show();
  for (int i=0; i < leds.numPixels(); i++) {
    leds.setPixel(i, OFF);
  }
  leds.show();
}


void setStrip(int strip, int del, int color)
{
  int offset = strip * ledsPerStrip;
  //for (int i=0; i < leds.numPixels(); i++) {
  //  leds.setPixel(i, OFF);
  // }
  for (int i=offset; i < offset + ledsPerStrip; i++) {
    leds.setPixel(i, color);
  }
}
