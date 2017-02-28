#include <SPI.h>
#include <Adafruit_GFX.h>
#define TEENSY 1
#include <Board_WS2801.h>
#include "Print.h"
#include <CmdMessenger.h>


/*****************************************************************************
  Burner Board LED and Audio Code

  Richard McDougall
  June 2013

  Uses modified Adafruit WS2801 library, adapted for virtual rectangle matrix
  with holes where the board is missing the corners

  Some code from riginal matrix example Written by 
  David Kavanagh (dkavanagh@gmail.com).  
  BSD license, all text above must be included in any redistribution

 *****************************************************************************/

#if defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
#define MEGA 1
#else
//#define DUE 1
#define TEENSY 1
#endif


#define NSIDELIGHTS 79
// smallboard has 57
//#define NSIDELIGHTS 57

// PINS
// - SPI: 50 (MISO), 51 (MOSI), 52 (SCK), 53 (SS)
//- PWM: 2 to 13 and 44 to 46
//- 36, 42: relays
//- ID Pins 22, 23, 24, 25
//  connect inverted - ground the 1's
//  - 0, rmc - 0,0,0,0 - none
//  - 1, rmc - 0,0,0,1 - 25
//  - 2, woodson - 0,0,1,0 - 24
//  - 3, ric - 0,0,1,1 - 24, 25
//  - 4, steve - 0,1,0,0 - 23
//  - 5, steve - 0,1,0,1 - 23, 25
//  - 6, joon x - 0,1,1,0 - 23, 24
//  - 7, james x - 0,1,1,1 - 23, 24
//  - 8, richard x - 1,0,0,0 - 22
//  - 9, jonathan x - 1,0,0,1 - 22,25
  

uint16_t boardId = 0;
const long intensity = 300;
int8_t board_mode = 1;
bool ledsOn = false;
uint8_t wheel_color;


// Set the first variable to the NUMBER of pixels in a row and
// the second value to number of pixels in a column.
Board_WS2801 *strip;

uint8_t led[8];
uint8_t ledo[8];
uint8_t ledn[8];

#define RGB_MAX 255
#define RGB_DIM 80  

#define BATTERY_PIN A0
#define MOT_PIN A1
//#ifdef MEGA
#define REMOTE_PIN A10
//#define LRELAY_PIN 36
#define LRELAY_PIN 13
#define SRELAY_PIN 42
#define SRELAY_PIN 42

#define AUDIO_PIN 10

/* Rotary encoder read example */
#define ENC_A A6
#define ENC_B A7
#define ENC_PORT PINF
 
#define ID_0 25
#define ID_1 24 
#define ID_2 23
#define ID_3 22

//#else
//#define REMOTE_PIN A2
//#define LRELAY_PIN 3
//#define SRELAY_PIN 4
//#endif

bool headLightOn = false;

char *boards[] = {
  "PROTO",
  "PROTO",
  "AKULA",  
  "BOADIE", 
  "GOOFY", 
  "STEVE", 
  "JOON",
  "ARTEMIS",
  "BISCUIT",
  "SQUEEZE"};
  
char *names[] = {
  "RICHARD",
  "RICHARD",
  "WOODSON",  
  "RIC", 
  "STEVE", 
  "STEVE", 
  "JOON",
  "JAMES",
  "RICHARD",
  "JONATHAN"};


char field_separator   = ',';
char command_separator = ';';
char escape_separator  = '\\';

#ifdef DUE
// Attach a new CmdMessenger object to the default Serial port
CmdMessenger cmdMessenger = CmdMessenger(SerialUSB, field_separator, command_separator, escape_separator);
CmdMessenger cmdMessengerCons = CmdMessenger(Serial);
#endif

#ifdef TEENSY
// Attach a new CmdMessenger object to the default Serial port
CmdMessenger cmdMessenger = CmdMessenger(Serial, field_separator, command_separator, escape_separator);
CmdMessenger cmdMessengerCons = CmdMessenger(Serial1);
#endif

// This is the list of recognized BB commands. 
enum
{
  BBacknowledge,
  BBCommandList,
  BBsetled,
  BBsetheadlight,
  BBsetmode,
  BBClearScreen,
  BBScroll,
  BBFade,
  BBUpdate,
  BBShowBattery,
  BBGetVoltage,
  BBGetBoardID,
  BBGetMode,
  BBFillScreen,
  BBVuMeterV,
  BBVuMeterH,
  BBSetRow,
  BBEsperanto,
  BBDiscoPlain,
  BBerror
};

// Callbacks define on which received commands we take action
void BBattachCommandCallbacks()
{
  // Attach callback methods
  cmdMessenger.attach(BBCommandList, ShowCommands);     // 0
  cmdMessenger.attach(BBCmdOne);                        // 1
  cmdMessenger.attach(BBsetled, Onsetled);              // 2
  cmdMessenger.attach(BBsetheadlight, Onsetheadlight);  // 3
  cmdMessenger.attach(BBsetmode, Onsetmode);            // 4
  cmdMessenger.attach(BBClearScreen, OnClearScreen);    // 5
  cmdMessenger.attach(BBScroll, OnScroll);              // 6
  cmdMessenger.attach(BBFade, OnFade);                  // 7
  cmdMessenger.attach(BBUpdate, OnUpdate);              // 8
  cmdMessenger.attach(BBShowBattery, OnShowBattery);    // 9
  cmdMessenger.attach(BBGetVoltage, OnGetVoltage);      // 10
  cmdMessenger.attach(BBGetBoardID, OnGetBoardID);      // 11
  cmdMessenger.attach(BBGetMode, OnGetMode);            // 12
  cmdMessenger.attach(BBFillScreen, OnFillScreen);      // 13
  cmdMessenger.attach(BBVuMeterV, OnVuMeterV);          // 14
  cmdMessenger.attach(BBVuMeterH, OnVuMeterH);          // 15
  cmdMessenger.attach(BBSetRow, OnSetRow);              // 16
  cmdMessenger.attach(BBEsperanto, OnEsperanto);        // 17
  cmdMessenger.attach(BBDiscoPlain, OnDiscoPlain);      // 18

}

// Callbacks define on which received commands we take action
void BBattachConsCommandCallbacks()
{
  // Attach callback methods
  cmdMessengerCons.attach(BBCommandList, ShowCommands);     // 0
  cmdMessengerCons.attach(BBCmdOne);                        // 1
  cmdMessengerCons.attach(BBsetled, Onsetled);              // 2
  cmdMessengerCons.attach(BBsetheadlight, Onsetheadlight);  // 3
  cmdMessengerCons.attach(BBsetmode, Onsetmode);            // 4
  cmdMessengerCons.attach(BBClearScreen, OnClearScreen);    // 5
  cmdMessengerCons.attach(BBScroll, OnScroll);              // 6
  cmdMessengerCons.attach(BBFade, OnFade);                  // 7
  cmdMessengerCons.attach(BBUpdate, OnUpdate);              // 8
  cmdMessengerCons.attach(BBShowBattery, OnShowBattery);    // 9
  cmdMessengerCons.attach(BBGetVoltage, OnGetVoltage);      // 10
  cmdMessengerCons.attach(BBGetBoardID, OnGetBoardID);      // 11
  cmdMessengerCons.attach(BBGetMode, OnGetMode);            // 12
  cmdMessengerCons.attach(BBFillScreen, OnFillScreen);      // 13
  cmdMessengerCons.attach(BBVuMeterV, OnVuMeterV);          // 14
  cmdMessengerCons.attach(BBVuMeterH, OnVuMeterH);          // 15
  cmdMessengerCons.attach(BBEsperanto, OnEsperanto);        // 16
  cmdMessengerCons.attach(BBDiscoPlain, OnDiscoPlain);      // 17
}

#define SHOW_COMMANDS 1
// Show available commands
void ShowCommands() 
{
#ifdef SHOW_COMMANDS
  Serial1.println("\nAvailable commands");
  Serial1.println(" 1;                       - This command list");
  Serial1.println(" 2,<headlight state>;     - Set headlight.");
  Serial1.println(" 3,<led state>;           - Set led. 0 = off, 1 = on"); 
  Serial1.println(" 4,<mode>;                - Set mode n"); 
  Serial1.println(" 5;                       - Clear Screen"); 
  Serial1.println(" 6,<direction>;           - Scroll up or down"); 
  Serial1.println(" 7,<amount>;              - Fade screen by n levels"); 
  Serial1.println(" 8;                       - Render and display screen"); 
  Serial1.println(" 9;                       - Show Battery for 2 seconds"); 
  Serial1.println(" 10;                      - Get battery voltage"); 
  Serial1.println(" 11;                      - Get Board ID"); 
  Serial1.println(" 12;                      - Get mode"); 
  Serial1.println(" 13,r,g,b;                - Fill Screen"); 
  Serial1.println(" 14,L1,L2,L3,...,L8;      - Render Vu Meter values 0-9"); 
  Serial1.println(" 15,L1,L2,L3,...,L8;      - Render Vu Meter values 0-9"); 
  Serial1.println(" 16,row,binary;           - Fill row of pixels"); 
  Serial1.println(" 17,L1,L2,L3,...,L8;      - Render Vu Meter values 0-9"); 
#endif
}

// Called when a received command has no attached function
void BBCmdOne()
{
  cmdMessenger.sendCmd(BBerror,"BB Cmd One\n");
}

// Callback function that sets headlight on or off
void Onsetheadlight()
{
  // Read led state argument, interpret string as boolean
  headLightOn = cmdMessenger.readBoolArg();
  cmdMessenger.sendCmd(BBacknowledge,headLightOn);
  if (headLightOn == true)
      digitalWrite(LRELAY_PIN, HIGH);
  else 
      digitalWrite(LRELAY_PIN, LOW);

}

// Callback function that sets deck lights on or off
void Onsetled()
{
  // Read led state argument, interpret string as boolean
  ledsOn = cmdMessenger.readBoolArg();
  cmdMessenger.sendCmd(BBacknowledge,ledsOn);
}


// Callback function that board mode
void Onsetmode()
{
  int mode;
  char strmode[10];

  // Read led state argument, interpret string as boolean
  mode = cmdMessenger.readInt32Arg();
  if (mode == 99) 
    board_mode++;
  else if (mode == 98) 
    board_mode--;
  else 
    board_mode = mode;
  if (board_mode < 0)
        board_mode = 0;
  if (board_mode > 20)
        board_mode = 0;
  cmdMessenger.sendCmd(BBacknowledge);
  cmdMessenger.sendCmd(BBsetmode,board_mode);
  Serial1.print("setmode: ");
  Serial1.print(mode);
  Serial1.print(":");
  Serial1.println(board_mode);
  sprintf(strmode, "%d", board_mode);
  strip->print(strmode, 35, 1, 1);
  strip->show();
  mydelay(300);
  clearScreen();
}

void OnClearScreen()
{
  clearScreen();
}

void OnScroll()
{
  scrollDown();
}


void OnFade() {
  int amount;

  amount =  cmdMessenger.readInt32Arg();
  if (amount == 0)
    amount = 10;

  fadeBoard(amount);
}

void OnUpdate() {
  strip->show();
}


void OnShowBattery() {
  drawBattery();
}

void OnGetVoltage() {
  
}

void OnGetBoardID() {
  //cmdMessenger.sendCmd(BBGetBoardID,boards[boardId]);
  cmdMessenger.sendCmdStart(BBGetBoardID);
  cmdMessenger.sendCmdArg(boards[boardId]);
  cmdMessenger.sendCmdEnd();
}

void OnGetMode() {
  cmdMessenger.sendCmdStart(BBsetmode);
  cmdMessenger.sendCmdArg(board_mode);
  cmdMessenger.sendCmdEnd();
}

void OnFillScreen() {
  char color[4];
  int32_t *c = (int32_t *)&color[0];

  color[2] =  cmdMessenger.readInt32Arg();
  color[1] =  cmdMessenger.readInt32Arg();
  color[0] =  cmdMessenger.readInt32Arg();

  fillScreen(*c);
  
}


struct rowType { char row[36]; };
typedef rowType rowType;

// Set a row of 12 r,g,b pixels from binary data
// The first an last pixel are for the side lights
void OnSetRow() {
  rowType response;
  char *pixels;
  uint32_t row;
  int i;
  int nPixels = sizeof(rowType);
  int r, g, b;
  int x, y;
  
  row = cmdMessenger.readInt32Arg();
  response =  cmdMessenger.readBinArg<rowType>();
  pixels = (char *)&response;
  //cmdMessenger.unescape(pixels);
  /*
  Serial1.print("OnSetRow ");
  Serial1.print(row);
  Serial1.print(" = ");
  Serial1.print(nPixels);
  Serial1.print("<");
  */
  for (i = 0; i < nPixels; i += 3) {
    x = i / 3;
    r = pixels[i];
    b = pixels[i+1];
    g = pixels[i+2];
    /*
    Serial1.print("setPixel(");
    Serial1.print(x);
    Serial1.print(",");
    Serial1.print(row);
    Serial1.print(",");
    Serial1.print(r);
    Serial1.print(",");
    Serial1.print(g);
    Serial1.print(",");
    Serial1.print(b);
    */
    if (x == 0) {
        setSideLight(0, row, rgbTo24BitColor(r, g, b));
    } else if (x < 11) {
        strip->setPixelColor(x - 1, row, r, g, b);
    } else if (x == 11) {        
        setSideLight(1, row, rgbTo24BitColor(r, g, b));
    }
    //Serial1.println(")");
  }
}

// Pick classic VU meter colors based on volume
int32_t vuColor(int amount) {
  if (amount < 11)
      return (rgbTo24BitColor(0,255,0));
  else if (amount < 21)
      return (rgbTo24BitColor(255,165,0));
  else
       return (rgbTo24BitColor(255,0,0));
}

void OnVuMeterV() {
  int level;
  int i;
  int y;

  fadeBoard(50);
  level =  cmdMessenger.readInt32Arg();
  level =  cmdMessenger.readInt32Arg();
  level =  cmdMessenger.readInt32Arg();
  for (i = 3; i < 15; i += 2) {
    level =  cmdMessenger.readInt32Arg();
    level *= 5;
    if (level > 35)
      level = 35;
    for (y = 0; y < level; y++) {
      if (i == 3) {
          setSideLight(0, 39 + y, vuColor(y));
          setSideLight(0, 38 - y, vuColor(y));
          setSideLight(1, 39 + y, vuColor(y));
          setSideLight(1, 38 - y, vuColor(y));
          } else {
          strip->setPixelColor(((i / 2) - 2) , 35 + y, vuColor(y));         
          strip->setPixelColor(((i / 2) - 2) , 34 - y, vuColor(y));               
          strip->setPixelColor(9 - ((i / 2) - 2), 35 + y, vuColor(y));         
          strip->setPixelColor(9 - ((i / 2) - 2), 34 - y, vuColor(y)); 
          }
    }
  }
  strip->show();
}

void OnVuMeterH() {
  int level;
  int i;
  int x;
  int m;
  int y;

  unsigned long ts = micros();
  
  // Throw away first low? frequency bin
  level =  cmdMessenger.readInt32Arg();
  
  fadeBoard(20);
  for (i = 0; i < 6; i++) {
    level =  cmdMessenger.readInt32Arg();
    //level /= 2;
    level --;
    if (level > 6)
      level = 6;
    if (i == 2 && level > 3) {
      wheel_color+= 1;
      fillSideLights(wheel(wheel_color));
    }
    for (x = 1; x < level; x++) {
      for (m = 0; m < 6; m++) {
          y = min(69, 35 + (i * 6) + m);
          strip->setPixelColor(5 + (x - 1), y, wheel(wheel_color));        
          strip->setPixelColor(4 - (x - 1), y, wheel(wheel_color));         
          y = max(0, 34 - ((i * 6) + m));
          strip->setPixelColor(5 + (x - 1), y, wheel(wheel_color));
          strip->setPixelColor(4 - (x - 1), y, wheel(wheel_color)); 
          wheel_color+= 15;
      }
    }
  }
  strip->show();
  strip->updates++;
  //Serial1.print("OnVuMeterH: ");
  //Serial1.println(micros() - ts);
}


int myBrightness = 255;

void OnEsperanto() {
  int level;
  int i;
  int x;
  int m;
  int y;
  int lastLevel = 0;

  unsigned long ts = micros();
  
  // Throw away first low? frequency bin
  level =  cmdMessenger.readInt32Arg();
  
  for (i = 0; i < 6; i++) {
    level =  cmdMessenger.readInt32Arg();
    //level /= 2;
    level --;
    if (level > 7)
      level = 7;
    if (i == 2) {
      if (((level - lastLevel) > 4)) {
        myBrightness = 255;   
      } else {
        myBrightness -= 20;
      }
      lastLevel = level;
    }
  }

  strip->setBrightness(myBrightness);
}

void OnDiscoPlain() {
  int level;
  int lastlevel = 0;
  int i;
  int x;
  int m;
  int y;
  uint32_t color;
  int total_level = 0;

  unsigned long ts = micros();
  
  fadeBoard(20);

  for (i = 0; i < 7; i++) {
    level =  cmdMessenger.readInt32Arg();
    if ((level - lastlevel) > 0) {
      total_level += (level - lastlevel);
    }
    
    if (total_level > 30) {
        color = wheel(wheel_color);
        fillScreen(color);  
        fillSideLights(color);
        wheel_color+= 15;
    } 
  }
  strip->show();
}

/* Helper functions */
/* returns change in encoder state (-1,0,1) */
int8_t read_encoder()
{
  static int8_t enc_states[] = {0,-1,1,0,1,0,0,-1,-1,0,0,1,0,1,-1,0};
  static uint8_t old_AB = 0;
  /**/
  old_AB <<= 2;                   //remember previous state
//  old_AB |= ( ENC_PORT & 0x03 );  //add current state
  old_AB |= (digitalRead(ENC_B) | (digitalRead(ENC_A) <<1));
  return ( enc_states[( old_AB & 0x0f )]);
}

long nDelays;
long lastUpdates;
unsigned long lastTick = 0;

// My Delay includes a check for the input control dial
void mydelay(uint32_t del) {
  int i;
  int8_t enc;
  char mode[10];
  boolean newmode = false;
  unsigned long currentTime = millis();
  
  for (i = 0; i < del; i++) {
    nDelays++;
    delay(1);
    enc = read_encoder();
    if (enc) {
//     Serial1.print("Counter value: ");
//    Serial1.println(board_mode, DEC);
      board_mode += enc;
      if (board_mode < 0)
        board_mode = 0;
      if (board_mode > 20)
        board_mode = 20;
      clearScreen();
      sprintf(mode, "%d", board_mode);
      // Tell android of new mode
      cmdMessenger.sendCmdStart(BBsetmode);
      cmdMessenger.sendCmdArg(board_mode);
      cmdMessenger.sendCmdEnd();
      strip->print(mode, 35, 1, 1);
      strip->show();
      del = 300;
      newmode = true;
      // Process incoming serial data, and perform callbacks
      cmdMessenger.feedinSerialData();
      cmdMessengerCons.feedinSerialData();
    }
  }
  if (newmode == true)
    clearScreen();
  if ((currentTime - lastTick) > 1000) {
    lastTick = currentTime;
    Serial1.print("tick ndelays = ");
    Serial1.print(nDelays, DEC);
    Serial1.print(" updates = ");
    Serial1.println(strip->updates - lastUpdates, DEC);
    lastUpdates = strip->updates;
    nDelays = 0;
  }
}


// Set sidelight left/right 
void setSideLight(uint16_t lr, uint16_t x,  uint32_t color)
{
        uint16_t pixel = lr * NSIDELIGHTS + x;
        strip->setPixelColor(700 + pixel, color);
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

// Slightly different, this one makes the rainbow wheel equally distributed 
// along the chain
void rainbowCycle(uint8_t wait) {
  int i, j;
  for (j=0; j < 256 * 5; j++) {     // 5 cycles of all 25 colors in the wheel
    for (i=0; i < strip->numPixels(); i++) {
      // tricky math! we use each pixel as a fraction of the full 96-color wheel
      // (thats the i / strip->numPixels() part)
      // Then add in j which makes the colors go around per pixel
      // the % 96 is to make the wheel cycle around
      strip->setPixelColor(i, wheel(((i * 256 / strip->numPixels()) + j) % 256) );
    }  
    strip->show();   // write all the pixels out
    mydelay(wait);
  }
}

// Slightly different, this one makes the rainbow wheel equally distributed 
// along the chain
void rainbowCycle2() {
  int i;
  uint32_t color;
  uint8_t r, g, b;
  
    for (i=0; i < strip->numPixels(); i++) {
      strip->setPixelColor(i, wheel(wheel_color));
      wheel_color += random(255);
    }  
    strip->show();   // write all the pixels out  
}

#define FADER 10
// Fade the board
void fadeBoard(int amount) {
  int i;
  uint32_t color;
  uint8_t r, g, b;
  
    for (i=0; i < strip->numPixels() + (NSIDELIGHTS * 2); i++) {
      color = strip->getPixelColor(i);

      r = (color & 0x00ff0000) >> 16;
      g = (color & 0x0000ff00) >> 8;
      b = (color & 0x000000ff);
      if (r)
        r-= amount;
      if (b)
        b-= amount;
      if (g)
        g-= amount;
        
      if (r < amount)
        r = 0;
      if (g < amount)
        g = 0;        
      if (b < amount)
        b = 0;        
        /*
      Serial1.print("color  ");
      Serial1.print(color, HEX);
      Serial1.print("  ");
      Serial1.print(r, HEX);
      Serial1.print("  ");
      Serial1.print(g, HEX);
      Serial1.print("  ");
      Serial1.print(b, HEX);
      Serial1.println("");
      */
      strip->setPixelColor(i, r, g, b);
    }  
    //strip->show();   // write all the pixels out  
}

//Shift Matrixrow up
void scrollUp()
{
  uint16_t x, y;

  for(y = 68; y >= 0; y--)
  {
    for(byte x = 0; x < 10;x++)
    {
      strip->setPixelColor(x, y + 1, strip->getPixelColor(x, y));
    }
  }
  // Hardcoded (NSIDELIGHTS * 2) side LEDS for now
  for(y = (NSIDELIGHTS  - 1); y > 0; y--)
  {
      strip->setPixelColor(700 + y, strip->getPixelColor(700 + y + 1));
      strip->setPixelColor(700 + NSIDELIGHTS + y, strip->getPixelColor(700 + NSIDELIGHTS + y + 1));
  }
}


//Shift Matrixrow down
void scrollDown()
{
  uint16_t x, y;

  for(y = 0; y < 69; y++)
  {
    for(byte x = 0; x < 10;x++)
    {
      strip->setPixelColor(x, y, strip->getPixelColor(x, y + 1));
    }
  }
  // Hardcoded (NSIDELIGHTS * 2) side LEDS for now
    for(y = 0; y < (NSIDELIGHTS  - 1); y++)
  {
      strip->setPixelColor(700 + y, strip->getPixelColor(700 + y + 1));
      strip->setPixelColor(700 + NSIDELIGHTS + y, strip->getPixelColor(700 + NSIDELIGHTS + y + 1));
  }

}


// I see blondes, brunets, redheads...
void shiftMatrixCircles()
{
  uint16_t x;

  for(x = 35; x < 69; x++)
  {
      strip->drawCircle(69 - x, 5, x - 35, strip->getPixelColor(5, x + 1));
  }
  mydelay(50);
}

// Clear Screen
void clearScreen()
{
  uint16_t x, y;

  for(y = 0; y < 70; y++)
  {
    for(byte x = 0; x < 10;x++)
    {
      strip->setPixelColor(x, y, rgbTo24BitColor(0, 0, 0));         
    }
  }
  // Hardcoded (NSIDELIGHTS * 2) side LEDS for now
  for(y = 0; y < NSIDELIGHTS; y++)
  {
      strip->setPixelColor(700 + y, rgbTo24BitColor(0, 0, 0));
      strip->setPixelColor(700 + NSIDELIGHTS + y, rgbTo24BitColor(0, 0, 0));
  }

}

// Fill Sidelights
void fillSideLights(uint32_t color)
{
  uint16_t y;
    // Hardcoded (NSIDELIGHTS * 2) side LEDS for now
  for(y = 0; y < NSIDELIGHTS; y++)
  {
      strip->setPixelColor(700 + y, color);
      strip->setPixelColor(700 + NSIDELIGHTS + y, color);
  }
}

// Clear Screen
void fillScreen(uint32_t color)
{
  uint16_t x, y;

  for(y = 0; y < 70; y++)
  {
    for(byte x = 0; x < 10;x++)
    {
      strip->setPixelColor(x, y, color);         
    }
  }
    // Hardcoded (NSIDELIGHTS * 2) side LEDS for now
  for(y = 0; y < NSIDELIGHTS; y++)
  {
      strip->setPixelColor(700 + y, color);
      strip->setPixelColor(700 + NSIDELIGHTS + y, color);
  }
}

void lines(uint8_t wait) {
  uint16_t x, y;
  uint32_t j = 0;

  for (x = 0; x < 10; x++) {
    for(y = 0; y < 70; y++) {
      strip->setPixelColor(x, y, wheel(j));
      strip->show();
      mydelay(wait);
    }
    j += 50;
  }
}

void drawzagX(uint8_t w, uint8_t h, uint8_t wait) {
  uint16_t x, y;
  for (x=0; x<w; x++) {
    strip->setPixelColor(x, x, 255, 0, 0);
    strip->show();
    mydelay(wait);
  }
  for (y=0; y<h; y++) {
    strip->setPixelColor(w-1-y, y, 0, 0, 255);
    strip->show();
    mydelay(wait);
  }
}

void drawY(uint8_t startx, uint8_t starty, uint8_t length, uint32_t color) {
  uint16_t x, y;

  for (y = starty; y < starty + length; y++) {
    strip->setPixelColor(x, y, color);
  }
}

void drawRect(uint8_t startx, uint8_t starty, uint8_t length, uint32_t color) {
  uint16_t x, y;

  for (y = starty; y < starty + length; y++) {
    strip->setPixelColor(x, y, color);
  }
}


/* Helper macros */
#define HEX__(n) 0x##n##LU
#define B8__(x) ((x&0x0000000FLU)?1:0) \
  +((x&0x000000F0LU)?2:0) \
+((x&0x00000F00LU)?4:0) \
+((x&0x0000F000LU)?8:0) \
+((x&0x000F0000LU)?16:0) \
+((x&0x00F00000LU)?32:0) \
+((x&0x0F000000LU)?64:0) \
+((x&0xF0000000LU)?128:0)

/* User macros */
#define B8(d) ((unsigned char)B8__(HEX__(d)))
#define B16(dmsb,dlsb) (((unsigned short)B8(dmsb)<<8) \
    + B8(dlsb))
#define B32(dmsb,db2,db3,dlsb) (((unsigned long)B8(dmsb)<<24) \
    + ((unsigned long)B8(db2)<<16) \
    + ((unsigned long)B8(db3)<<8) \
    + B8(dlsb))



// US flag 
void drawUSflag() {

  //drawY(0, 70, rgbTo24BitColor(255, 0, 0));
  //drawY(1, 70, rgbTo24BitColor(255, 255, 255));

  uint32_t color;
  uint16_t x;


  // Red and White for 20 rows
  for (row = 0; row < 20; row++) {

    for (x = 0; x < 10; x++) {
      strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, 0, 0));
      x++;
      strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, RGB_DIM, RGB_DIM));
    }
    scrollDown();
    strip->show();
  }

  // Red and White with solid blue
  for (x = 0; x < 4; x++) {
    strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, 0, 0));
    x++;
    strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, RGB_DIM, RGB_DIM));
  }
  scrollDown();
  strip->show();
  row++;


  // Red/white
  for (x = 0; x < 4; x++) {
    strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, 0, 0));
    x++;
    strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, RGB_DIM, RGB_DIM));
  }
  // Solid Blue line
  for (; x < 10; x++) {
    strip->setPixelColor(x, 69, rgbTo24BitColor(0, 0, RGB_DIM));
  }
  scrollDown();
  strip->show();

  for (row = 0; row < 20; row++) {
    // Red/white
    for (x = 0; x < 4; x++) {
      strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, 0, 0));
      x++;
      strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, RGB_DIM, RGB_DIM));
    }
    // White/Blue
    for (x = 4; x < 10; x++) {
      strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, RGB_DIM, RGB_DIM));
      x++;
      strip->setPixelColor(x, 69, rgbTo24BitColor(0, 0, RGB_DIM));
    }    
    scrollDown();
    strip->show();  
    // Blue/white
    for (x = 4; x < 10; x++) {
      strip->setPixelColor(x, 69, rgbTo24BitColor(0, 0, RGB_DIM));
      x++;
      strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, RGB_DIM, RGB_DIM));
    }
    scrollDown();
    strip->show();
    row++;

  }

  // Red/white
  for (x = 0; x < 4; x++) {
    strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, 0, 0));
    x++;
    strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, RGB_DIM, RGB_DIM));
  }
  // Blue line
  for (; x < 10; x++) {
    strip->setPixelColor(x, 69, rgbTo24BitColor(0, 0, RGB_DIM));
  }
  scrollDown();


  // 10 lines of blank
  for (x = 0; x < 10; x++) {
    strip->setPixelColor(x, 69, rgbTo24BitColor(0, 0, 0));
  }
  for (row = 0; row < 10; row++) {
    scrollDown();
    strip->show();
  }

}


void drawHeader(bool isTop) {
  uint32_t color;
  uint16_t x;
  uint16_t y;
  uint16_t sideLight;

  if (isTop) {
    y = 69;
    sideLight = NSIDELIGHTS  - 1;
  } else { 
    y = 0;
    sideLight = 0;
  }

  for (x = 0; x < 10; x++) {
    //   color = random(2,4)%2 == 0 ? rgbTo24BitColor(0,0,0) : rgbTo24BitColor(0, 255, 0); //Chance of 1/3rd 
    color = random(2,4)%2 == 0 ? rgbTo24BitColor(0, 0, 0): wheel(wheel_color); //Chance of 1/3rd 
    //   color = random(2,4)%2 == 0 ? rgbTo24BitColor(0, 0, 0): rgbTo24BitColor(255, 255, 255); //Chance of 1/3rd 
    //   color =  rgbTo24BitColor(255, 255, 255); //Chance of 1/3rd 
    strip->setPixelColor(x, y, color);
    // Ripple down the side lights with the same color as the edges
    if (x == 0) {
        setSideLight(0, sideLight, color);
    }
    if (x == 9) {
        setSideLight(1, sideLight, color);
    }
    wheel_color++;
  }
  strip->show();
}

void drawHeaderLunarian() {
  uint32_t color;
  uint16_t x;

  for (x = 0; x < 10; x++) {
    //   color = random(2,4)%2 == 0 ? rgbTo24BitColor(0,0,0) : rgbTo24BitColor(0, 255, 0); //Chance of 1/3rd 
    color = random(2,4)%2 == 0 ? rgbTo24BitColor(0, 0, 0): rgbTo24BitColor(128, 128, 128); //Chance of 1/3rd 
    //   color = random(2,4)%2 == 0 ? rgbTo24BitColor(0, 0, 0): rgbTo24BitColor(255, 255, 255); //Chance of 1/3rd 
    //   color =  rgbTo24BitColor(255, 255, 255); //Chance of 1/3rd 
    strip->setPixelColor(x, 69, color);
    // Ripple down the side lights with the same color as the edges
    if (x == 0) {
        setSideLight(0, (NSIDELIGHTS  - 1), color);
    }
    if (x == 9) {
        setSideLight(1, (NSIDELIGHTS  - 1), color);
    }
    wheel_color++;
  }
  strip->show();
}







void drawCenter() {
  uint32_t color;
  uint16_t x;

    color = random(2,4)%2 == 0 ? rgbTo24BitColor(0, 0, 0): wheel(wheel_color); //Chance of 1/3rd 
//    color = rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX);
    strip->fillCircle(35, 5, 1, color);
    wheel_color++;
    strip->show();
}


uint16_t readID() {
 uint16_t bit;
 uint16_t id;
 
 bit = digitalRead(ID_0);
 Serial1.print(bit, BIN);
 id = !bit;
 bit = digitalRead(ID_1);
 Serial1.print(bit, BIN);
 id |= !bit << 1;
 bit = digitalRead(ID_2);
 Serial1.print(bit, BIN);
 id |= !bit << 2; 
 bit = digitalRead(ID_3);
 Serial1.print(bit, BIN);
 id |= !bit << 3;
 
 Serial1.print("Board ID  ");
 Serial1.print(id, DEC);
 Serial1.println("");

 return(id);

}


// Working on proto, but low end is over
// = 90% = 30350
// 38.1 = 100% = 102400
// 36v = 40% = 96900
// 10% = 91800
//#define LEVEL_EMPTY 91800
//#define LEVEL_FULL  102300

// New settings, 8/17/2013
// 0 = 92900
// 100 = 102300
//#define LEVEL_EMPTY 92900
//#define LEVEL_FULL  102300
// Lifep04 batteries range is 39.1 -> 36.0v
#define LEVEL_EMPTY 93700
#define LEVEL_FULL  102300

// = 90% = 30350
// 38.1 = 100% = 102400
// 36v = 40% = 96900
// 10% = 91800

// Battery Level Meter
// This is a simple starting point
// Todo: Sample the battery level continously and maintain a rolling average
//       This will help with the varying voltages as motor load changes, which
//       will result in varing results depending on load with this current code
//

int getBattery() {
  int32_t level = 0;
  uint16_t i;
  uint16_t sample;

  // Read Battery Level
  // 18 * 1.75v = 31.5v for low voltage
  // Set zero to 30v
  // Set 100% to 38v

  // Convert to level 0-28
  for (i = 0; i < 100; i++) {
    level += sample = analogRead(BATTERY_PIN);
    //Serial1.print("Battery sample ");
    //Serial1.print(sample, DEC);
    //Serial1.println(" ");
  }
  //Serial1.print("Battery Level ");
  //Serial1.print(level, DEC);
  //Serial1.println(" ");

  if (level > LEVEL_FULL) {
    level = LEVEL_FULL;
  }

  // Sometimes noise makes level just below zero
  if (level > LEVEL_EMPTY) {
    level -= LEVEL_EMPTY;
  } else {
    level = 0;
  }


  level *= 29;

  level = level / (LEVEL_FULL - LEVEL_EMPTY);

  //Serial1.print("Adjusted Level ");
  //Serial1.print(level, DEC);
  //Serial1.println(" ");
  return(level);
  
}


void drawBattery() {
  uint32_t color;
  uint16_t x;
  uint8_t row;
  int level;

  // Clear screen and measure voltage, since screen load varies it!
  clearScreen();
  strip->show();
  mydelay(1000);
  
  level = getBattery();

  row = 20;

  // White Battery Shell with Green level

  // Battery Bottom
  for (x = 0; x < 10; x++) {
    strip->setPixelColor(x, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  }
  row++;

  // Battery Sides
  for (; row < 49; row++) {
    strip->setPixelColor(0, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
    strip->setPixelColor(9, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  }

  // Battery Top
  for (x = 0; x < 10; x++) {
    strip->setPixelColor(x, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  }
  row++;

  // Battery button
  for (x = 3; x < 7; x++) {
    strip->setPixelColor(x, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
    strip->setPixelColor(x, row+1, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  }
  row+=2;

  // Battery Level
  for (row = 21; row < 21 + level; row++) {
    for (x = 1; x < 9; x++) {
      strip->setPixelColor(x, row, rgbTo24BitColor(0, RGB_DIM, 0));
    }
  }

  strip->show();
}

void drawTheMan(uint32_t color) {
  uint16_t x;
  uint16_t row;

  uint16_t the_man[] = {
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(10,00000001),
    B16(11,00000011),
    B16(11,00000011),
    B16(01,10000110),
    B16(01,10000110),
    B16(01,10000110),
    B16(01,10000110),
    B16(00,11001100),
    B16(00,11001100),
    B16(00,11001100),
    B16(00,11001100),
    B16(00,11001100),
    B16(00,11001100),
    B16(00,01001000),
    B16(00,01001000),
    B16(00,01001000),
    B16(00,01001000),
    B16(00,01001000),
    B16(00,01001000),
    B16(00,11001100),
    B16(00,11001100),
    B16(00,11001100),
    B16(00,11001100),
    B16(00,11001100),
    B16(00,10000100),
    B16(01,10000110),
    B16(01,10000110),
    B16(01,10110110),
    B16(11,00110011),
    B16(11,01111011),
    B16(10,01111001),
    B16(00,00110000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000),
    B16(00,00000000)};

  clearScreen();
  for (row = 0; row < sizeof(the_man) / sizeof(uint16_t); row++) {
    for (x = 0; x < 10; x++) {
      strip->setPixelColor(x,  row, the_man[row] & (1<<(x))? color: rgbTo24BitColor(0, 0, 0));
    }
  }
  strip->show();
}

void cycleTheMan(){
  uint32_t color;
  uint8_t the_red;
  uint8_t the_green;
  uint8_t the_blue;
  uint8_t the_cycle;
  the_red = 100;
  the_green = 100;
  the_blue = 100;
  wheel_color = 255;
  for (the_cycle = 0; (the_cycle < 100) && board_mode == 10; the_cycle++) {
        the_red = random(2,4)%2 == 0 ? rgbTo24BitColor(80, 80, 80): wheel(wheel_color); //Chance of 1/3rd
        the_green = random(2,4)%2 == 0 ? rgbTo24BitColor(80, 80, 80): wheel(wheel_color); //Chance of 1/3rd
        the_blue = random(2,4)%2 == 0 ? rgbTo24BitColor(80, 80, 80): wheel(wheel_color); //Chance of 1/3rd
        drawTheMan(rgbTo24BitColor(the_red, the_green, the_blue));
        mydelay(20);
  }
}

void setup() {

  uint16_t i;

  // Console for debugging
  Serial.begin(115200);
  SerialUSB.begin(115200); // Initialize Serial Monitor USB
  Serial1.println("Goodnight moon!");

  // Set battery level analogue reference
#ifdef MEGA
  analogReference(INTERNAL1V1);
#else
  //analogReference(INTERNAL);
#endif
  pinMode(AUDIO_PIN, INPUT);
  pinMode(BATTERY_PIN, INPUT);
  pinMode(MOT_PIN, INPUT);
  pinMode(REMOTE_PIN, INPUT);
  digitalWrite(REMOTE_PIN, LOW);
  pinMode(SRELAY_PIN, OUTPUT);
  pinMode(LRELAY_PIN, OUTPUT);
  digitalWrite(SRELAY_PIN, HIGH);
  digitalWrite(LRELAY_PIN, HIGH);
  
// ID Pins  
  pinMode(ID_0, INPUT);
  digitalWrite(ID_0, HIGH);
  pinMode(ID_1, INPUT);
  digitalWrite(ID_1, HIGH);
  pinMode(ID_2, INPUT);
  digitalWrite(ID_2, HIGH);
  pinMode(ID_3, INPUT);
  digitalWrite(ID_3, HIGH);
  
  // Encoder Pins
  pinMode(ENC_A, INPUT);
  digitalWrite(ENC_A, HIGH);
  pinMode(ENC_B, INPUT);
  digitalWrite(ENC_B, HIGH);

  boardId = readID();



  strip = new Board_WS2801((uint16_t)10, (uint16_t)70, WS2801_RGB, (boolean)true);

  strip->begin();
  
  // Update LED contents, to start they are all 'off'
  clearScreen();
  strip->show();
  
  strip->print(boards[boardId], 15, 1, 1);
  strip->show();
  mydelay(1000);

  clearScreen();
  strip->print(names[boardId], 15, 1, 1);
  strip->show();
  mydelay(1000);

  drawBattery();
  mydelay(1000);

  clearScreen();

  BBattachCommandCallbacks();
  cmdMessenger.sendCmd(BBacknowledge,"BB Ready\n");
  ShowCommands();

}


void loop_matrix()
{
  drawHeader(true);
  scrollDown();
  mydelay(50);
}


void loop_matrixfast()
{
  drawHeader(true);
  scrollDown();
  mydelay(10);
}


void loop_lunarian()
{
  drawHeaderLunarian();
  scrollDown(); 
  mydelay(1);
}


void loop_battery()
{
  drawBattery();
  mydelay(1000);
  clearScreen();
}


void loop_blank() {
  mydelay(5);
}

void loop_theman() {
// The Man
        for (row = 0; row < 10; row++) {
          strip->setPixelColor(row, 69, 0);
        }
        strip->show();
        for (row = 0; row < 70; row++) {
          scrollDown();
          strip->show();
        }
 
        cycleTheMan();
}


int16_t loopcnt = 0;

int16_t state = 0;
// state = 0: lines
// state = 1: flag


void loop() {
  int i;
  unsigned long ts;
  
  // Process incoming serial data, and perform callbacks
  //ts = micros();
  cmdMessenger.feedinSerialData();
  //Serial1.print("cmdMessenger.feedinSerialData:");
  //Serial1.println(micros() - ts);
  cmdMessengerCons.feedinSerialData();
   
   /*
   Serial1.print("Encoder sample ");
   Serial1.print(board_mode, DEC);
   Serial1.println(" ");
   */
   
   switch (board_mode) {
     
     case 1:
       loop_matrix();
       break;
 
     case 2:
       loop_matrixfast();
       break;
       
     case 3:
       loop_lunarian();
       break;

     case 4:
       loop_matrixfast();
       break;
              
     default:
       mydelay(1);
       break;
   }  

}


void screenHook() {
//  checkButton();
}


