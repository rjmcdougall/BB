

#include <OctoWS2811.h>

// Any group of digital pins may be used
const int numPins = 32;
/*
  byte pinList[numPins] = {0,1,2,3,4,5,6,7,
                          8,9,10,11,12,24,25,26,
                          27,28,29,30,31,32,33,34,
                          35,36,37,38,39,40,41,13};
*/
byte pinList[numPins] = {3, 2, 1, 0,
                         7, 6, 5, 4,
                         11, 10, 9, 8,
                         26, 25, 24, 12,
                         30, 29, 28, 27,
                         34, 33, 32, 31,
                         38, 37, 36, 35,
                         13, 41, 40, 39
                        };


const int ledsPerStrip = 550;


struct TranslationMap {
  int y;
  int startX;
  int endX;
  int stripDirection;
  int stripNumber;
  int stripOffset;
};

#define OFF 0x030000

#define yRows 205
#define xRows 70

// y,startX,endX,stripDirection,stripNumber,stripOffset,
struct TranslationMap boardMap[] = {
  0, 37, 35, -1, 1, 0,// 2
  1, 38, 34, -1, 1, 4, // 4
  2, 39, 33, -1, 1, 12, // 6
  3, 40, 32, -1, 1, 23, // 8
  4, 41, 31, -1, 1, 36, // 10
  5, 42, 30, -1, 1, 51, // 12
  6, 43, 29, -1, 1, 69, // 14
  7, 44, 28, -1, 1, 86, // 16
  8, 45, 27, -1, 1, 106, // 18
  9, 46, 26, -1, 1, 128, // 20
  10, 47, 25, -1, 1, 150, // 22
  11, 48, 24, -1, 1, 175, // 24
  12, 48, 24, -1, 1, 201, // 24
  13, 49, 23, -1, 1, 227, // 26
  14, 49, 23, -1, 1, 256, // 26
  15, 50, 22, -1, 1, 284, // 28
  16, 51, 21, -1, 1, 315, // 30
  17, 51, 21, -1, 1, 347, // 30
  18, 52, 20, -1, 1, 379, // 32
  19, 52, 20, -1, 1, 413, // 32
  20, 53, 19, -1, 1, 447, // 34


  21, 53, 19, -1, 2, 0 , // 36
  22, 54, 18, -1, 2, 36, // 36
  23, 55, 17, -1, 2, 72, // 38
  24, 55, 17, -1, 2, 111 , // 38
  25, 56, 16, -1, 2, 150 , // 40
  26, 56, 16, -1, 2, 190 , // 40
  27, 57, 15, -1, 2, 230 , // 42
  28, 57, 15, -1, 2, 271 , // 42
  29, 58, 14, -1, 2, 314 , // 44
  30, 58, 14, -1, 2, 358 , // 44
  31, 58, 14, -1, 2, 404 , // 44
  32, 58, 14, -1, 2, 449 , // 46


  33, 58, 14, -1, 3, 1 , // 46
  34, 59, 13, -1, 3, 47, // 46
  35, 60, 12, -1, 3, 94, // 48
  36, 60, 12, -1, 3, 142 , // 48
  37, 60, 12, -1, 3, 191 , // 48
  38, 60, 12, -1, 3, 240 , // 48
  39, 61, 11, -1, 3, 290 , // 50
  40, 61, 11, -1, 3, 341 , // 50
  41, 61, 11, -1, 3, 392 , // 50
  42, 61, 10, -1, 3, 444 , // 52

  43, 61, 10, -1, 4, 0 , // 52
  44, 62, 10, -1, 4, 52, // 52
  45, 62, 10, -1, 4, 106, // 54
  46, 62, 10, -1, 4, 160, // 54
  47, 63, 10, -1, 4, 214, // 54
  48, 63, 9, -1, 4, 269, // 54
  49, 63, 8, -1, 4, 324, // 56
  50, 63, 8, -1, 4, 380, // 56
  51, 63, 8, -1, 4, 438, // 56


  52, 63, 8, -1, 5, 0, // 56
  53, 63, 7, -1, 5, 58 , // 58
  54, 63, 7, -1, 5, 116, // 58
  55, 65, 7, -1, 5, 174, // 58
  56, 65, 7, -1, 5, 233, // 58
  57, 65, 7, -1, 5, 293, // 58
  58, 66, 6, -1, 5, 352, // 60
  59, 66, 6, -1, 5, 412, // 60


  60, 66, 6, -1, 6, 0, // 60
  61, 66, 6, -1, 6, 61 , // 60
  62, 66, 6, -1, 6, 122, // 60
  63, 66, 6, -1, 6, 184, // 62
  64, 66, 6, -1, 6, 247, // 62
  65, 66, 6, -1, 6, 309, // 62
  66, 66, 6, -1, 6, 373, // 62
  67, 66, 6, -1, 6, 436, // 62



  68, 66, 6, -1, 7, 0, // 64
  69, 66, 6, -1, 7, 64 , // 64
  70, 66, 6, -1, 7, 129, // 64
  71, 66, 6, -1, 7, 193, // 64
  72, 66, 6, -1, 7, 257, // 64
  73, 67, 5, -1, 7, 321, // 66
  74, 68, 4, -1, 7, 385, // 66


  75, 68, 4, -1, 8, 0, // 66
  76, 68, 4, -1, 8, 65 , // 66
  77, 68, 4, -1, 8, 130, // 66
  78, 68, 4, -1, 8, 197, // 68
  79, 68, 4, -1, 8, 263, // 68
  80, 68, 4, -1, 8, 329, // 68
  81, 68, 4, -1, 8, 496, // 68
  82, 68, 4, -1, 8, 464, // 68


  83, 68, 4, -1, 9, 0, // 68
  84, 69, 1, -1, 9, 66 , // 70
  85, 69, 1, -1, 9, 133, // 70
  86, 69, 1, -1, 9, 199, // 70
  87, 69, 1, -1, 9, 266, // 70
  88, 69, 1, -1, 9, 334, // 70
  89, 69, 1, -1, 9, 401, // 70


  90, 69, 1, -1, 10, 0 , // 72
  91, 69, 1, -1, 10, 67, // 72
  92, 69, 1, -1, 10, 135 , // 72
  93, 69, 1, -1, 10, 203 , // 72
  94, 69, 1, -1, 10, 271 , // 72
  95, 69, 1, -1, 10, 339 , // 72
  96, 69, 1, -1, 10, 407 , // 72


  97, 69, 1, -1, 11, 0 , // 72
  98, 69, 1, -1, 11, 68, // 72
  99, 69, 1, -1, 11, 136 , // 72
  100, 69, 1, -1, 11, 204, // 72
  101, 69, 1, -1, 11, 272, // 72
  102, 69, 1, -1, 11, 341, // 72
  103, 69, 1, -1, 11, 410, // 72

  104, 69, 1, -1, 12, 0, // 72
  105, 69, 1, -1, 12, 68, // 72
  106, 69, 1, -1, 12, 136, // 72
  107, 69, 1, -1, 12, 204, // 72

  108, 69, 1, -1, 13, 0, // 72
  109, 69, 1, -1, 13, 68, // 72
  110, 69, 1, -1, 13, 136, // 72
  111, 69, 1, -1, 13, 204, // 72
  112, 69, 1, -1, 13, 272, // 72
  113, 69, 1, -1, 13, 340, // 72
  114, 69, 1, -1, 13, 408, // 72


  115, 68, 2, -1, 14, 0, // 72
  116, 68, 2, -1, 14, 68, // 72
  117, 68, 2, -1, 14, 136, // 72
  118, 68, 2, -1, 14, 203, // 72
  119, 68, 2, -1, 14, 270, // 72
  120, 68, 2, -1, 14, 338, // 72
  121, 68, 2, -1, 14, 406, // 70


  122, 68, 2, -1, 15, 0, // 70
  123, 68, 2, -1, 15, 68, // 70
  124, 68, 2, -1, 15, 135, // 70
  125, 68, 2, -1, 15, 202, // 70
  126, 68, 2, -1, 15, 269, // 70
  127, 68, 2, -1, 15, 336, // 68


  128, 67, 3, -1, 16, 0, // 68
  129, 67, 3, -1, 16, 67, // 68
  130, 67, 3, -1, 16, 134, // 68
  131, 67, 3, -1, 16, 200, // 68
  132, 67, 3, -1, 16, 266, // 66
  133, 67, 3, -1, 16, 332, // 66
  134, 67, 3, -1, 16, 398, // 66
  135, 67, 3, -1, 16, 463, // 66
  136, 67, 3, -1, 16, 522, // 64


  137, 67, 3, -1, 17, 1, // 64
  138, 67, 3, -1, 17, 66, // 64
  139, 67, 3, -1, 17, 131, // 62
  140, 67, 3, -1, 17, 196, // 62
  141, 67, 3, -1, 17, 260, // 62
  142, 67, 3, -1, 17, 324, // 62
  143, 67, 3, -1, 17, 387, // 62

  144, 67, 3, -1, 18, 0, // 60
  145, 67, 3, -1, 18, 63, // 60
  146, 67, 3, -1, 18, 126, // 60
  147, 67, 3, -1, 18, 188, // 60
  148, 67, 3, -1, 18, 251, // 60
  149, 67, 3, -1, 18, 313, // 58
  150, 67, 3, -1, 18, 375, // 58
  151, 67, 3, -1, 18, 436, // 58

  152, 66, 4, -1, 19, 0, // 58
  153, 66, 4, -1, 19, 60, // 58
  154, 66, 4, -1, 19, 121, // 56
  155, 66, 4, -1, 19, 181, // 56
  156, 66, 4, -1, 19, 240, // 56
  157, 66, 4, -1, 19, 299, // 56
  158, 66, 4, -1, 19, 358, // 56
  159, 66, 4, -1, 19, 417, // 54

  160, 64, 6, -1, 20, 0, // 54
  161, 64, 6, -1, 20, 58, // 54
  161, 64, 6, -1, 20, 115, // 54
  162, 63, 7, -1, 20, 174, // 54
  163, 63, 7, -1, 20, 230, // 52
  164, 63, 7, -1, 20, 288, // 52
  165, 63, 7, -1, 20, 343, // 52
  166, 62, 8, -1, 20, 399, // 52
  167, 62, 8, -1, 20, 454, // 50


  168, 62, 8, -1, 21, 0, // 50
  169, 62, 8, -1, 21, 54, // 50
  170, 62, 8, -1, 21, 109, // 50
  171, 62, 8, -1, 21, 162, // 48
  172, 62, 8, -1, 21, 214, // 48
  173, 61, 9, -1, 21, 266, // 48
  174, 61, 9, -1, 21, 317, // 46
  175, 61, 9, -1, 21, 368, // 46
  176, 61, 9, -1, 21, 417, // 46

  177, 58, 12, -1, 22, 2, // 44
  178, 58, 12, -1, 22, 51, // 44
  179, 58, 12, -1, 22, 100, // 44
  180, 58, 12, -1, 22, 148, // 44
  181, 58, 12, -1, 22, 195, // 42
  182, 56, 14, -1, 22, 244, // 42
  183, 56, 14, -1, 22, 291, // 40
  184, 56, 14, -1, 22, 336, // 40
  185, 56, 14, -1, 22, 381, // 38
  186, 56, 14, -1, 22, 425, // 38
  187, 56, 14, -1, 22, 468, // 36

  188, 55, 15, -1, 23, 1, // 36
  189, 55, 15, -1, 23, 42, // 34
  190, 55, 15, -1, 23, 83, // 34
  191, 55, 15, -1, 23, 123, // 34
  192, 55, 15, -1, 23, 163, // 32
  193, 54, 16, -1, 23, 203, // 32
  194, 54, 16, -1, 23, 241, // 32
  195, 54, 16, -1, 23, 278, // 32
  196, 54, 16, -1, 23, 313, // 30
  197, 54, 16, -1, 23, 348, // 30
  198, 54, 16, -1, 23, 382, // 30
  199, 54, 16, -1, 23, 414, // 28
  200, 54, 16, -1, 23, 446, // 28


  201, 51, 19, -1, 24, 0, // 28
  202, 51, 19, -1, 24, 29, // 26
  203, 51, 19, -1, 24, 57, // 26
  204, 48, 22, -1, 24, 87, // 26
  205, 48, 22, -1, 24, 111, // 24
  206, 48, 22, -1, 24, 132, // 24
  207, 53, 17, -1, 24, 154

};


// These buffers need to be large enough for all the pixels.
// The total number of pixels is "ledsPerStrip * numPins".
// Each pixel needs 3 bytes, so multiply by 3.  An "int" is
// 4 bytes, so divide by 4.  The array is created using "int"
// so the compiler will align it to 32 bit memory.
const int bytesPerLED = 3;  // change to 4 if using RGBW
DMAMEM int displayMemory[ledsPerStrip * numPins * bytesPerLED / 4];
int drawingMemory[ledsPerStrip * numPins * bytesPerLED / 4];

int pixelMemory[xRows * yRows];

const int config = WS2811_GRB | WS2811_800kHz;

OctoWS2811 leds(ledsPerStrip, displayMemory, drawingMemory, config, numPins, pinList);


void clear()
{
  for (int i = 0; i < leds.numPixels(); i++) {
    leds.setPixel(i, 0);
  }
  leds.show();
  for (int i = 0; i < leds.numPixels(); i++) {
    leds.setPixel(i, OFF);
  }
  leds.show();
}


void setup() {
  leds.begin();
  clear();
  leds.show();
}

#define OFF 0x030000


int brightness = 50;

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

int strip_colors[24];

void translatePixel(int x, int y, int color) {
  int startX = boardMap[y].startX;
  int endX = boardMap[y].endX;
  int stripOffset = boardMap[y].stripOffset;
  int stripNumber = boardMap[y].stripNumber - 1;
  int stripDirection = boardMap[y].stripDirection;

  int pixelOffset;

  // Check it off screen
  if (x < min (startX, endX)  && x > max(startX, endX))
    return;

  // Calculate strip number and offset first
  if (stripDirection == 1) {
    pixelOffset = stripOffset + x - startX ;
    leds.setPixel(stripNumber * ledsPerStrip + pixelOffset, color);
  } else {
    pixelOffset = stripOffset + startX - x;
    leds.setPixel(stripNumber * ledsPerStrip + pixelOffset, color);
  }
}

int getTranslatePixel(int x, int y) {
  int startX = boardMap[y].startX;
  int endX = boardMap[y].endX;
  int stripOffset = boardMap[y].stripOffset;
  int stripNumber = boardMap[y].stripNumber - 1;
  int stripDirection = boardMap[y].stripDirection;

  int pixelOffset;

  // Check it off screen
  if (x < min (startX, endX)  && x > max(startX, endX))
    return;

  // Calculate strip number and offset first
  if (stripDirection == 1) {
    pixelOffset = stripOffset + x - startX ;
    return (leds.getPixel(stripNumber * ledsPerStrip + pixelOffset));
  } else {
    pixelOffset = stripOffset + startX - x;
    return (leds.getPixel(stripNumber * ledsPerStrip + pixelOffset));
  }
}

void scrollUp()
{
  int x, y;

  for (y = yRows - 1; y > 0; y--)
  {
    for (int x = 0; x < xRows; x++)
    {
      setPixel(x, y, getPixel(x, y - 1));
    }
  }
}

void setPixel(int x, int y, int color) {
  pixelMemory[x + y * xRows] = color;
}

int getPixel(int x, int y) {
  return pixelMemory[x + y * xRows];
}

void drawHeader() {
  int color;
  int x;
  int y;

  for (x = 0; x < xRows; x+=3) {
    color = random(2, 10) % 3 != 0 ? rgbTo24BitColor(0, 0, 0) : wheel(wheel_color); //Chance of 1/3rd
    setPixel(x, 0, color);
    setPixel(x + 1, 0, color);
    setPixel(x + 2, 0, color);
    wheel_color++;
  }
}

void flushPixels() {
  for (int x = 0; x < xRows; x++) {
    for (int y = 0; y < yRows; y++) {
      translatePixel(x, y, getPixel(x, y));
    }
  }
}

void loop() {

  uint32_t color;

  scrollUp();
  //leds.show();
  scrollUp();
  leds.show();
  scrollUp();
  leds.show();
  drawHeader();
  flushPixels();
}



void setStrip(int strip, int del, int color)
{
  int offset = strip * ledsPerStrip;
  //for (int i=0; i < leds.numPixels(); i++) {
  //  leds.setPixel(i, OFF);
  // }
  for (int i = offset; i < offset + ledsPerStrip; i++) {
    leds.setPixel(i, color);
  }
}
