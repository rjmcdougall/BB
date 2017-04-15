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
int8_t board_mode = 0;
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


/* Rotary encoder read example */
#define ENC_A A6
#define ENC_B A7
#define ENC_PORT PINF
 
#define ID_0 25
#define ID_1 24 
#define ID_2 23
#define ID_3 22

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
  BBSetRow,
  BBSetOtherlight,
  BBPingRow,
  BBEchoRow,
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
  cmdMessenger.attach(BBSetRow, OnSetRow);              // 14
  cmdMessenger.attach(BBSetOtherlight, OnSetOtherlight);// 15
  cmdMessenger.attach(BBPingRow, OnPingRow);            // 16
  cmdMessenger.attach(BBEchoRow, OnEchoRow);            // 17
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
  Serial1.println(" 14,row,binary;           - Fill row of pixels"); 
#endif
}

// Called when a received command has no attached function
void BBCmdOne()
{
  cmdMessenger.sendCmd(BBerror,"BB Error\n");
  Serial1.print("BB Error ");

}

// Callback function that sets headlight on or off
void Onsetheadlight()
{


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

// row is sized larger because some chars might be escaped
struct rowType { char row[256]; };
typedef rowType rowType;

// Set a row of 10 r,g,b pixels from binary data
// The first an last pixel are for the side lights
void OnSetRow() {
  rowType response;
  char *pixels;
  uint32_t row;
  int i;
  int nPixels = 30; //sizeof(rowType);
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
  Serial1.print(nPixels);
  Serial1.print("<");
#endif
  
  for (i = 0; i < nPixels; i += 3) {
    x = i / 3;
    r = pixels[i];
    g = pixels[i+1];
    b = pixels[i+2];
    
#ifdef DEBUG_PIXELS
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
#endif       
    strip->setPixelColor(x, row, r, g, b);
#ifdef DEBUG_PIXELS
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

// Set a row of 79 r,g,b pixels from binary data
// The first an last pixel are for the side lights
void OnSetOtherlight() {
  rowType response;
  char *pixels;
  uint32_t other;
  int pixel;
  int nPixels = 79; //
  int r, g, b;
  int x, y;
  
  other = cmdMessenger.readInt32Arg();
  response =  cmdMessenger.readBinArg<rowType>();
  pixels = (char *)&response;
  cmdMessenger.unescape(pixels, nPixels * 3);

#ifdef DEBUG_PIXELS
  Serial1.print("OnSetOtherlight ");
  Serial1.print(pixel);
  Serial1.print(" = ");
  Serial1.print(nPixels);
  Serial1.print("<");
#endif
  
  for (pixel = 0; pixel < nPixels * 3; pixel += 3) {
    x = pixel / 3;
    r = pixels[pixel];
    g = pixels[pixel+1];
    b = pixels[pixel+2];
    
#ifdef DEBUG_PIXELS
    Serial1.print("setPixel(");
    Serial1.print(other);
    Serial1.print(",");
    Serial1.print(x);
    Serial1.print(",");
    Serial1.print(r);
    Serial1.print(",");
    Serial1.print(g);
    Serial1.print(",");
    Serial1.print(b);
#endif    
    
    setSideLight(other, x, rgbTo24BitColor(r, g, b));

#ifdef DEBUG_PIXELS
    Serial1.println(")");
#endif
  }
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
void setSideLight(int lr, int x,  uint32_t color)
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
#define BATTERY_PIN A0
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






void setup() {

  uint16_t i;

  // Console for debugging
  Serial1.begin(115200);
  SerialUSB.begin(115200); // Initialize Serial Monitor USB
  Serial1.println("Goodnight moon!");

  // Set battery level analogue reference
#ifdef MEGA
  analogReference(INTERNAL1V1);
#else
  //analogReference(INTERNAL);
#endif

  
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



void loop_matrixfast()
{
  drawHeader(true);
  scrollDown();
  mydelay(10);
}


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
       loop_matrixfast();
       mydelay(0);
      break;

      case 50:
       mydelay(1);
       break;      
              
     default:
       mydelay(1);
       break;
   }  

}


void screenHook() {
//  checkButton();
}


