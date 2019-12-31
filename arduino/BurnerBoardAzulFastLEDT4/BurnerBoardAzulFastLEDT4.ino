

#include <FastLED.h>
#include <CmdMessenger.h>

boolean do_lowbattery_actions = false;
boolean kDisableBatteryMeter = false;

//#define DEBUG_PIXELS 1

#define RGB_MAX 255

#define NUM_LEDS_PER_STRIP 552  //552
#define NUM_STRIPS 8
#define NUM_LEDS (NUM_STRIPS * NUM_LEDS_PER_STRIP)

CRGB leds[NUM_STRIPS * NUM_LEDS_PER_STRIP];

// Pin layouts on the teensy 3/3.1:
// WS2811_PORTD: 2,14,7,8,6,20,21,5 (BB Azul Adapter)
// WS2811_PORTC: 15,22,23,9,10,13,11,12,28,27,29,30 (these last 4 are pads on the bottom of the teensy)
// WS2811_PORTDC: 2,14,7,8,6,20,21,5,15,22,23,9,10,13,11,12 - 16 way parallel
//
// The new Teensy 4 also supports parallel output, but it's done slightly differently from the above platforms. 
// First off, there are three sets of possible pins that can be used
// Each of the three set of pins, in order:
// First: 1,0,24,25,19,18,14,15,17,16,22,23,20,21,26,27
// Second: 10,12,11,13,6,9,32,8,7
// Third: 37, 36, 35, 34, 39, 38, 28, 31, 30

struct TranslationMap {
                int y;
                int startX;
                int endX;
                int stripDirection;
                int stripNumber;
                int stripOffset;
                };

// Standard
struct TranslationMap boardMap[] = {
  0,23,22,-1,8,452,
  1,20,25,1,8,446,
  2,27,18,-1,8,436,
  3,16,29,1,8,422,
  4,30,15,-1,8,406,
  5,14,31,1,8,388,
  6,33,12,-1,8,366,
  7,11,34,1,8,342,
  8,31,14,-1,8,324,
  9,14,31,1,8,306,
  10,32,13,-1,8,286,
  11,12,33,1,8,264,
  12,34,11,-1,8,240,
  13,11,34,1,8,216,
  14,35,10,-1,8,190,
  15,9,36,1,8,162,
  16,37,8,-1,8,132,
  17,8,37,1,8,102,
  18,38,7,-1,8,70,
  19,6,39,1,8,36,
  20,40,5,-1,8,0,
  21,40,5,-1,7,0,
  22,7,38,1,7,36,
  23,38,7,-1,7,68,
  24,6,39,1,7,100,
  25,39,6,-1,7,134,
  26,5,40,1,7,168,
  27,40,5,-1,7,204,
  28,4,41,1,7,240,
  29,41,4,-1,7,278,
  30,4,41,1,7,316,
  31,42,3,-1,7,354,
  32,3,42,1,7,394,
  33,42,3,-1,7,434,
  34,2,43,1,7,474,
  35,43,2,-1,6,0,
  36,2,43,1,6,42,
  37,44,1,-1,6,84,
  38,1,44,1,6,128,
  39,44,1,-1,6,172,
  40,1,44,1,6,216,
  41,44,1,-1,6,260,
  42,1,44,1,6,304,
  43,44,1,-1,6,348,
  44,0,45,1,6,392,
  45,45,0,-1,6,438,
  46,0,45,1,6,484,
  47,45,0,-1,5,0,
  48,0,45,1,5,46,
  49,45,0,-1,5,92,
  50,0,45,1,5,138,
  51,45,0,-1,5,184,
  52,0,45,1,5,230,
  53,45,0,-1,5,276,
  54,0,45,1,5,322,
  55,45,0,-1,5,368,
  56,0,45,1,5,414,
  57,45,0,-1,5,460,
  58,0,45,1,5,506,
  59,45,0,-1,4,0,
  60,0,45,1,4,46,
  61,45,0,-1,4,92,
  62,0,45,1,4,138,
  63,45,0,-1,4,184,
  64,0,45,1,4,230,
  65,45,0,-1,4,276,
  66,0,45,1,4,322,
  67,45,0,-1,4,368,
  68,0,45,1,4,414,
  69,45,0,-1,4,460,
  70,1,44,1,4,506,
  71,44,1,-1,3,0,
  72,1,44,1,3,44,
  73,44,1,-1,3,88,
  74,1,44,1,3,132,
  75,44,1,-1,3,176,
  76,1,44,1,3,220,
  77,44,1,-1,3,264,
  78,1,44,1,3,308,
  79,43,2,-1,3,352,
  80,2,43,1,3,394,
  81,43,2,-1,3,436,
  82,2,43,1,3,478,
  83,42,3,-1,2,0,
  84,3,42,1,2,40,
  85,42,3,-1,2,80,
  86,3,42,1,2,120,
  87,42,3,-1,2,160,
  88,4,41,1,2,200,
  89,41,4,-1,2,238,
  90,4,41,1,2,276,
  91,40,5,-1,2,314,
  92,5,40,1,2,350,
  93,40,5,-1,2,386,
  94,6,39,1,2,422,
  95,39,6,-1,2,456,
  96,6,39,1,2,490,
  97,39,6,-1,1,0,
  98,7,38,1,1,34,
  99,38,7,-1,1,66,
  100,8,37,1,1,98,
  101,37,8,-1,1,128,
  102,9,36,1,1,158,
  103,36,9,-1,1,186,
  104,10,35,1,1,214,
  105,38,7,-1,1,240,
  106,8,37,1,1,272,
  107,36,9,-1,1,302,
  108,9,36,1,1,330,
  109,35,10,-1,1,358,
  110,10,35,1,1,384,
  111,34,11,-1,1,410,
  112,12,33,1,1,434,
  113,32,13,-1,1,456,
  114,14,31,1,1,476,
  115,34,11,-1,1,494,
  116,14,31,1,1,518,
  117,26,19,-1,1,536
};


// Candy
struct TranslationMap boardMapCandy[] = {
  0,23,22,-1,8,452,
  1,20,25,1,8,446,
  2,27,18,-1,8,436,
  3,16,29,1,8,422,
  4,30,15,-1,8,406,
  5,14,31,1,8,388,
  6,33,12,-1,8,366,
  7,11,34,1,8,342,
  8,31,14,-1,8,324,
  9,14,31,1,8,306,
  10,32,13,-1,8,286,
  11,12,33,1,8,264,
  12,34,11,-1,8,240,
  13,11,34,1,8,216,
  14,35,10,-1,8,190,
  15,9,36,1,8,162,
  16,37,8,-1,8,132,
  17,8,37,1,8,102,
  18,38,7,-1,8,70,
  19,6,39,1,8,36,
  20,40,5,-1,8,0,
  21,40,5,-1,7,0,
  22,7,38,1,7,36,
  23,38,7,-1,7,68,
  24,6,39,1,7,100,
  25,39,6,-1,7,134,
  26,5,40,1,7,168,
  27,40,5,-1,7,204,
  28,4,40,1,7,240,
  29,41,4,-1,7,277,
  30,4,41,1,7,315,
  31,42,3,-1,7,353,
  32,3,42,1,7,393,
  33,42,3,-1,7,433,
  34,2,43,1,7,473,
  35,43,2,-1,6,0,
  36,2,43,1,6,42,
  37,44,1,-1,6,84,
  38,1,44,1,6,128,
  39,45,0,-1,6,172,
  40,1,44,1,6,216,
  41,44,1,-1,6,260,
  42,1,44,1,6,304,
  43,44,1,-1,6,348,
  44,0,45,1,6,392,
  45,45,0,-1,6,438,
  46,0,45,1,6,484,
  47,45,0,-1,5,0,
  48,0,45,1,5,46,
  49,45,0,-1,5,92,
  50,0,45,1,5,138,
  51,45,0,-1,5,184,
  52,0,45,1,5,230,
  53,45,0,-1,5,276,
  54,0,45,1,5,322,
  55,45,0,-1,5,368,
  56,0,45,1,5,414,
  57,45,0,-1,5,460,
  58,0,45,1,5,506,
  59,45,0,-1,4,0,
  60,0,45,1,4,46,
  61,45,0,-1,4,92,
  62,0,45,1,4,138,
  63,45,0,-1,4,184,
  64,0,45,1,4,230,
  65,45,0,-1,4,276,
  66,0,45,1,4,322,
  67,45,0,-1,4,368,
  68,0,45,1,4,414,
  69,45,0,-1,4,460,
  70,1,44,1,4,506,
  71,44,1,-1,3,0,
  72,1,44,1,3,44,
  73,44,1,-1,3,88,
  74,1,44,1,3,132,
  75,44,1,-1,3,176,
  76,1,44,1,3,220,
  77,44,1,-1,3,264,
  78,1,44,1,3,308,
  79,43,2,-1,3,352,
  80,2,43,1,3,394,
  81,43,2,-1,3,436,
  82,2,43,1,3,478,
  83,42,3,-1,2,0,
  84,3,42,1,2,40,
  85,42,3,-1,2,80,
  86,3,42,1,2,120,
  87,42,3,-1,2,160,
  88,4,41,1,2,200,
  89,41,4,-1,2,238,
  90,4,41,1,2,276,
  91,40,5,-1,2,314,
  92,5,40,1,2,350,
  93,40,5,-1,2,386,
  94,6,39,1,2,422,
  95,39,6,-1,2,456,
  96,6,39,1,2,490,
  97,39,6,-1,1,0,
  98,7,38,1,1,34,
  99,38,7,-1,1,66,
  100,8,37,1,1,98,
  101,37,8,-1,1,128,
  102,9,36,1,1,158,
  103,36,9,-1,1,186,
  104,10,35,1,1,214,
  105,38,7,-1,1,240,
  106,8,37,1,1,272,
  107,36,9,-1,1,302,
  108,9,36,1,1,330,
  109,35,10,-1,1,358,
  110,10,35,1,1,384,
  111,34,11,-1,1,410,
  112,12,33,1,1,434,
  113,32,13,-1,1,456,
  114,14,31,1,1,476,
  115,34,11,-1,1,494,
  116,14,31,1,1,518,
  117,26,19,-1,1,536};

bool inited = false;

char field_separator   = ',';
char command_separator = ';';
char escape_separator  = '\\';

boolean batteryCritical = false;
boolean batteryLow = false;

uint32_t batteryControl = -1;        
int  batteryStateOfCharge = -1;     // %
int  batteryMaxError = -1;          // %
int batteryRemainingCapacity = -1;  // mAh
int batteryFullChargeCapacity = -1; // mAh
int batteryVoltage = -1;            // mV
int batteryAverageCurrent = -1;     // mA
int batteryTemperature = -1;        // 0.1K
int batteryFlags = -1;          
int batteryCurrent = -1;            // mA
int batteryFlagsB = -1;

#define PIXEL_RED  0;
#define  PIXEL_GREEN 1;
#define  PIXEL_BLUE 2;

void testPattern() {
  int row;
  int endRow = 118;
  for (row = 0; row < endRow; row++) {
    translatePixel(22, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
    translatePixel(23, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  }
  
}

void translatePixel(int x, int y, int color) {
  int flipY = 117 - y;
  int startX = boardMap[flipY].startX;
  int endX = boardMap[flipY].endX;
  int stripOffset = boardMap[flipY].stripOffset;
  int stripNumber = boardMap[flipY].stripNumber - 1;
  int stripDirection = boardMap[flipY].stripDirection;

  int pixelOffset;

  // Check it off screen
  if (x < min (startX, endX)  && x > max(startX, endX))
    return;

  // Calculate strip number and offset first
  if (stripDirection == 1) {
    pixelOffset = stripOffset + x - startX ;
    leds[stripNumber * NUM_LEDS_PER_STRIP + pixelOffset] = color;
  } else {
    pixelOffset = stripOffset + startX - x;
    leds[stripNumber * NUM_LEDS_PER_STRIP + pixelOffset] = color;
  }
} 


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
  BBSetRow,
  BBPingRow,
  BBEchoRow,
  BBerror
};

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
  cmdMessenger.attach(BBSetRow, OnSetRow);              // 10
  cmdMessenger.attach(BBPingRow, OnPingRow);            // 11
  cmdMessenger.attach(BBEchoRow, OnEchoRow);            // 12
}

// Show available commands
void ShowCommands() 
{
  /*
  Serial.println("\nAvailable commands");
  Serial.println(" 1;                       - This command list");
  Serial.println(" 2,<headlight state>;     - Set headlight.");
  Serial.println(" 3,<led state>;           - Set led. 0 = off, 1 = on"); 
  Serial.println(" 4;                       - Clear Screen"); 
  Serial.println(" 5;                       - Render and display screen"); 
  Serial.println(" 6;                       - Show Battery for 2 seconds"); 
  Serial.println(" 7;                      - Get battery level"); 
  Serial.println(" 8,r,g,b;                - Fill Screen"); 
  Serial.println(" 9,row,binary;           - Fill row of pixels"); 
  Serial.println(" 10,row,binary;           - ping round trip test of row"); 
  Serial.println(" 11,binary;               - Echo back row of pixels"); 
  */
}

// Called when a received command has no attached function
void BBCmdOne()
{
  //cmdMessenger.sendCmd(BBerror,"BB Error\n");
  //Serial.print("BB Error ");

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
  clearScreen();
}

// For show battery on middle press
int showingBattery = 0;

void OnUpdate() {
  LEDS.show();
  inited = true;
}

void OnFillScreen() {
  char color[4];
  int32_t *c = (int32_t *)&color[0];

  color[2] =  cmdMessenger.readInt32Arg();
  color[1] =  cmdMessenger.readInt32Arg();
  color[0] =  cmdMessenger.readInt32Arg();

  fillScreen(*c);
  
}

void OnShowBattery() {
  showingBattery = 30;
}

void OnGetBatteryLevel() {
  cmdMessenger.sendCmdStart(BBGetBatteryLevel);
  cmdMessenger.sendCmdArg(batteryControl);
  cmdMessenger.sendCmdArg(batteryStateOfCharge);
  cmdMessenger.sendCmdArg(batteryMaxError);
  cmdMessenger.sendCmdArg(batteryRemainingCapacity);
  cmdMessenger.sendCmdArg(batteryFullChargeCapacity);
  cmdMessenger.sendCmdArg(batteryVoltage);
  cmdMessenger.sendCmdArg(batteryAverageCurrent);
  cmdMessenger.sendCmdArg(batteryTemperature);
  cmdMessenger.sendCmdArg(batteryFlags);
  cmdMessenger.sendCmdArg(batteryCurrent);
  cmdMessenger.sendCmdArg(batteryFlagsB);
  cmdMessenger.sendCmdEnd();
}


// row is sized larger because some chars might be escaped
struct rowType { char row[552 * 3 * 2]; };
typedef rowType rowType;

// Set a row of 10 r,g,b pixels from binary data
// The first an last pixel are for the side lights
void OnSetRow() {
  rowType response;
  char *pixels;
  uint32_t row;
  int i;
  int nPixels = 552 * 3; //sizeof(rowType);
  int r, g, b;
  int x, y;
  
  row = cmdMessenger.readInt32Arg();
  response =  cmdMessenger.readBinArg<rowType>();
  pixels = (char *)&response;
  cmdMessenger.unescape(pixels, nPixels);

#ifdef DEBUG_PIXELS
  Serial1.print("OnSetRow ");
  Serial1.print(row);
  Serial1.print(" = ");
  Serial1.println(nPixels);
  //Serial1.print("<");
#endif

  // Catch error case
  if (row > 7) {
    row = 7;
  }

  int startPixel = row * NUM_LEDS_PER_STRIP;
  
  for (i = 0; i < NUM_LEDS_PER_STRIP * 3; i += 3) {
    r = pixels[i];
    g = pixels[i+1];
    b = pixels[i+2];
    
#ifdef DEBUG_PIXELS2
    Serial1.print("setPixel(");
    Serial1.print(i);
    Serial1.print(",");
    Serial1.print(row);
    Serial1.print(",");
    Serial1.print(r);
    Serial1.print(",");
    Serial1.print(g);
    Serial1.print(",");
    Serial1.print(b);
#endif       
    leds[startPixel + (i / 3)] =  CRGB(r, g, b);
#ifdef DEBUG_PIXELS2
    Serial1.println(")");
#endif
  }
}


// Echo a test row of 16 values
void OnEchoRow() {
  rowType response;
  char *pixels;
  uint32_t row;
  String echoReply;
  int b;
  
  response =  cmdMessenger.readBinArg<rowType>();
  pixels = (char *)&response;
  cmdMessenger.unescape(pixels, 16);
  for (int i = 0; i < 16; i++) {
    b = pixels[i];
    echoReply = echoReply + " " + b;
  }

  cmdMessenger.sendCmdStart(BBEchoRow);
  cmdMessenger.sendCmdArg(echoReply);
  cmdMessenger.sendCmdEnd();
  
}

// For benchmarks
void OnPingRow() {
  rowType response;
  int row;

  row = cmdMessenger.readInt32Arg();
  response =  cmdMessenger.readBinArg<rowType>();
}

// Create a 24 bit color value from R,G,B
uint32_t rgbTo24BitColor(byte r, byte g, byte b)
{
        uint32_t c;
        c = r;
        c <<= 8;
        c |= g;
        c <<= 8;
        c |= b;
        return c;
}

// Clear Screen
void clearScreen()
{
  int i;
  for (i = 0; i < NUM_LEDS; i++) {
    leds[i] = 0;
  }
  LEDS.show();
}

// Fill screen
void fillScreen(uint32_t color) {
  register int i;
  for (i=0; i < NUM_LEDS; i++) {
    leds[i] = color;
  }
}



void setup() {
  delay(1000);  
  // Console for debugging
  Serial.begin(115200);
  Serial.println("Goodnight moon!");

  BBattachCommandCallbacks();
  cmdMessenger.sendCmd(BBacknowledge,"BB Ready\n");
  ShowCommands();


  // Teensy 3.x
  // LEDS.addLeds<WS2811_PORTA,NUM_STRIPS>(leds, NUM_LEDS_PER_STRIP);
  // LEDS.addLeds<WS2811_PORTB,NUM_STRIPS>(leds, NUM_LEDS_PER_STRIP);
  //LEDS.addLeds<WS2811_PORTD,NUM_STRIPS>(leds, NUM_LEDS_PER_STRIP).setCorrection(TypicalLEDStrip);
  // LEDS.addLeds<WS2811_PORTDC,NUM_STRIPS>(leds, NUM_LEDS_PER_STRIP);
  
  // Teensy 4 parallel output example
  LEDS.addLeds<NUM_STRIPS, WS2811, 10>(leds,NUM_LEDS_PER_STRIP);
  LEDS.setBrightness(RGB_MAX);
}

uint8_t c = 0;

#define RED    0x160000
#define GREEN  0x001600
#define BLUE   0x000016
#define YELLOW 0x101400
#define PINK   0x120009
#define ORANGE 0x100400
#define WHITE  0x202020



void loop() {
  // Pulse the screen until connected to android
  if (inited == false) {
    fillScreen(rgbTo24BitColor(c,c + 10,40));
    c++;
    if (c > 25)
      c = 0;
    LEDS.show();
    LEDS.delay(100);
  }
  cmdMessenger.feedinSerialData();
}
