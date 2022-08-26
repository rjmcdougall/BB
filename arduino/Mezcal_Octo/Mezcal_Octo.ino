
#include <OctoWS2811.h>
#include <CmdMessenger.h>

#define RGB_MAX 100

bool inited = false;


char field_separator   = ',';
char command_separator = ';';
char escape_separator  = '\\';


// Attach a new CmdMessenger object to the default SerialUSB port
CmdMessenger cmdMessenger = CmdMessenger(Serial, field_separator, command_separator, escape_separator);

// This is the list of recognized BB commands. 
enum
{
  BBacknowledge,
  BBCommandList,
  BBsetled,
  BBsetheadlight,
  BBsetmode,
  BBClearScreen,
  BBUpdate,
  BBShowBattery,
  BBGetBatteryLevel,
  BBFillScreen,
  BBSetStrip,
  BBPingStrip,
  BBEchoStrip,
  BBerror
};



// Any group of digital pins may be used
const int numStrips = 32;
/*
  byte pinList[numStrips] = {0,1,2,3,4,5,6,7,
                          8,9,10,11,12,24,25,26,
                          27,28,29,30,31,32,33,34,
                          35,36,37,38,39,40,41,13};
*/
byte pinList[numStrips] = {3,2,1,0,
                          7,6,5,4,
                          11, 10, 9, 8, 
                          26, 25, 24, 12,
                          30, 29, 28, 27,
                          34, 33, 32, 31,
                          39, 40, 41, 13,
                          38, 37, 36, 35};
                          //13, 41, 40, 39};


const int ledsPerStrip = 550;


// These buffers need to be large enough for all the pixels.
// The total number of pixels is "ledsPerStrip * numStrips".
// Each pixel needs 3 bytes, so multiply by 3.  An "int" is
// 4 bytes, so divide by 4.  The array is created using "int"
// so the compiler will align it to 32 bit memory.
const int bytesPerLED = 3;  // change to 4 if using RGBW
DMAMEM int displayMemory[ledsPerStrip * numStrips * bytesPerLED / 4];
int drawingMemory[ledsPerStrip * numStrips * bytesPerLED / 4];

const int config = WS2811_GRB | WS2811_800kHz;

OctoWS2811 leds(ledsPerStrip, displayMemory, drawingMemory, config, numStrips, pinList);

void setup() {
  leds.begin();
  BBattachCommandCallbacks();
  cmdMessenger.sendCmd(BBacknowledge,"BB Ready\n");
  ShowCommands();
  clear();
  leds.show();
}

#define OFF 0x030000

struct TranslationMap {
  int y;
  int startX;
  int endX;
  int stripDirection;
  int stripNumber;
  int stripOffset;
};

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
  162, 64, 6, -1, 20, 115, // 54
  163, 63, 7, -1, 20, 174, // 54
  164, 63, 7, -1, 20, 230, // 52
  165, 63, 7, -1, 20, 288, // 52
  166, 63, 7, -1, 20, 343, // 52
  167, 62, 8, -1, 20, 399, // 52
  168, 62, 8, -1, 20, 454, // 50

  
  169, 62, 8, -1, 21, 0, // 50
  170, 62, 8, -1, 21, 54, // 50
  171, 62, 8, -1, 21, 109, // 50
  172, 62, 8, -1, 21, 162, // 48
  173, 62, 8, -1, 21, 214, // 48
  174, 61, 9, -1, 21, 266, // 48
  175, 61, 9, -1, 21, 317, // 46
  176, 61, 9, -1, 21, 368, // 46
  177, 61, 9, -1, 21, 417, // 46
  
  178, 58, 12, -1, 22, 2, // 44
  179, 58, 12, -1, 22, 51, // 44
  180, 58, 12, -1, 22, 100, // 44
  181, 58, 12, -1, 22, 148, // 44
  182, 58, 12, -1, 22, 195, // 42
  183, 56, 14, -1, 22, 244, // 42
  184, 56, 14, -1, 22, 291, // 40
  185, 56, 14, -1, 22, 336, // 40
  186, 56, 14, -1, 22, 381, // 38
  187, 56, 14, -1, 22, 425, // 38
  188, 56, 14, -1, 22, 468, // 36
 
  189, 55, 15, -1, 23, 1, // 36
  190, 55, 15, -1, 23, 42, // 34
  191, 55, 15, -1, 23, 83, // 34
  192, 55, 15, -1, 23, 123, // 34
  193, 55, 15, -1, 23, 163, // 32
  194, 54, 16, -1, 23, 203, // 32
  195, 54, 16, -1, 23, 241, // 32
  196, 54, 16, -1, 23, 278, // 32
  197, 54, 16, -1, 23, 313, // 30
  198, 54, 16, -1, 23, 348, // 30
  199, 54, 16, -1, 23, 382, // 30
  200, 54, 16, -1, 23, 414, // 28
  201, 54, 16, -1, 23, 446, // 28

  
  202, 51, 19, -1, 24, 0, // 28
  203, 51, 19, -1, 24, 29, // 26
  204, 51, 19, -1, 24, 57, // 26
  205, 48, 22, -1, 24, 87, // 26
  206, 48, 22, -1, 24, 111, // 24
  207, 48, 22, -1, 24, 132, // 24
  208, 53, 17, -1, 24, 154

};

void testPattern() {
  int row;
  int endRow = 208;
  for (row = 0; row < endRow; row++) {
    translatePixel(35, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
    translatePixel(36, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  }

}

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


void loop() {
  if (!inited) {
     testPattern();
  }
  cmdMessenger.feedinSerialData();
  leds.show();
}


void flashStripNo() {
  for (int strip = 0; strip < numStrips; strip++) {
    flashStrip(strip, 100000, 0x00ff00);
  }
}

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


void flashStrip(int strip, int del, int color)
{
  int offset = strip * ledsPerStrip;
  for (int j = 0; j < strip + 2; j++) {
    for (int i = offset; i < offset + ledsPerStrip; i++) {
      leds.setPixel(i, OFF);
    }
    leds.show();
    delayMicroseconds(del);

    for (int i = offset; i < offset + ledsPerStrip; i++) {
      leds.setPixel(i, color);
    }
    leds.show();
    delayMicroseconds(del);



  }
  for (int i = offset; i < offset + ledsPerStrip; i++) {
    leds.setPixel(i, OFF);
  }
  leds.show();
  delayMicroseconds(del * 3);
}

// Create a 24 bit color value from G, R, B
uint32_t rgbTo24BitColor(byte r, byte g, byte b)
{
  uint32_t c;
  c = b;
  c <<= 8;
  c |= r;
  c <<= 8;
  c |= g;
  return c;
}




// Callbacks define on which received commands we take action
void BBattachCommandCallbacks()
{
  // Attach callback methods
  cmdMessenger.attach(BBCommandList, ShowCommands);     // 1
  cmdMessenger.attach(BBCmdOne);                        // 2
  cmdMessenger.attach(BBsetled, Onsetled);              // 3
  cmdMessenger.attach(BBsetheadlight, Onsetheadlight);  // 4
  cmdMessenger.attach(BBClearScreen, OnClearScreen);    // 5
  cmdMessenger.attach(BBUpdate, OnUpdate);              // 6
  cmdMessenger.attach(BBShowBattery, OnShowBattery);    // 7
  cmdMessenger.attach(BBGetBatteryLevel, OnGetBatteryLevel);      // 8
  cmdMessenger.attach(BBFillScreen, OnFillScreen);      // 9
  cmdMessenger.attach(BBSetStrip, OnSetStrip);              // 10
  cmdMessenger.attach(BBPingStrip, OnPingStrip);            // 11
  cmdMessenger.attach(BBEchoStrip, OnEchoStrip);            // 12
}

// Show available commands
void ShowCommands() 
{
}

// Called when a received command has no attached function
void BBCmdOne()
{
}

// Callback function that sets headlight on or off
void Onsetheadlight()
{
}

bool ledsOn = false;
// Callback function that sets deck lights on or off
void Onsetled()
{
  // Read led state argument, interpret string as boolean
  ledsOn = cmdMessenger.readBoolArg();
  //cmdMessenger.sendCmd(BBacknowledge,ledsOn);
}

void OnClearScreen()
{
  clear();
}

void OnUpdate() {
  leds.show();
  inited = true;
}

void OnFillScreen() {
}

void OnShowBattery() {

}

void OnGetBatteryLevel() {

}




// strip is sized larger because some chars might be escaped (r, g, b + escapes)
struct rowType { char strip[2048]; };
typedef rowType rowType;

// Set a strip of r,g,b pixels from binary data
// The first an last pixel are for the side lights
void OnSetStrip() {
  rowType response;
  char *pixels;
  uint32_t strip;
  int i;
  int nPixels = 552 * 3; //sizeof(rowType);
  int r, g, b;
  int x, y;
  
  strip = cmdMessenger.readInt32Arg();
  response =  cmdMessenger.readBinArg<rowType>();
  pixels = (char *)&response;
  cmdMessenger.unescape(pixels, nPixels);

#ifdef DEBUG_PIXELS
  Serial1.print("OnSetStrip ");
  Serial1.print(strip);
  Serial1.print(" = ");
  Serial1.println(nPixels);
  //Serial1.print("<");
#endif

  // Catch error case
  if (strip > numStrips) {
    strip = numStrips;
  }

  int startPixel = strip * ledsPerStrip;
  
  for (i = 0; i < ledsPerStrip * 3; i += 3) {
    r = pixels[i];
    g = pixels[i+1];
    b = pixels[i+2];
    
#ifdef DEBUG_PIXELS2
    Serial1.print("setPixel(");
    Serial1.print(i);
    Serial1.print(",");
    Serial1.print(strip);
    Serial1.print(",");
    Serial1.print(r);
    Serial1.print(",");
    Serial1.print(g);
    Serial1.print(",");
    Serial1.print(b);
#endif       
    leds.setPixel(startPixel + (i / 3), r, g, b);
#ifdef DEBUG_PIXELS2
    Serial1.println(")");
#endif
  }
}


// Echo a test strip of 16 values
void OnEchoStrip() {
  rowType response;
  char *pixels;
  String echoReply;
  int b;
  
  response =  cmdMessenger.readBinArg<rowType>();
  pixels = (char *)&response;
  cmdMessenger.unescape(pixels, 16);
  for (int i = 0; i < 16; i++) {
    b = pixels[i];
    echoReply = echoReply + " " + b;
  }

  cmdMessenger.sendCmdStart(BBEchoStrip);
  cmdMessenger.sendCmdArg(echoReply);
  cmdMessenger.sendCmdEnd();
  
}

// For benchmarks
void OnPingStrip() {
  rowType response;
  int strip;

  strip = cmdMessenger.readInt32Arg();
  response =  cmdMessenger.readBinArg<rowType>();
}
