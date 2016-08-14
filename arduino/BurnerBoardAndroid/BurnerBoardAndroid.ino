#include "SPI.h"
#include <Adafruit_GFX.h>
#include "Board_WS2801.h"
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
#define DUE 1
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
//  - 1, woodson - 0,0,0, 1 - 25
//  - 2, ric - 0,0,1,0 - 24
//  - 3, james - 0,0,1,1 - 24, 25
//  - 4, steve - 0,1,0,0 - 23
//  - 5, joon - 0,1,0,1 - 23, 25
//  - 6, steve x - 0,1,1,0 - 23, 24
  

uint16_t boardId = 0;
const long intensity = 300;
int8_t board_mode = 0;
bool ledsOn = false;


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
#ifdef MEGA
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

#else
#define REMOTE_PIN A2
#define LRELAY_PIN 3
#define SRELAY_PIN 4
#endif

bool headLightOn = false;


// Attach a new CmdMessenger object to the default Serial port
CmdMessenger cmdMessenger = CmdMessenger(Serial);

// This is the list of recognized BB commands. 
enum
{
  BBCommandList,
  BBacknowledge,
  BBerror,
  BBsetheadlight,
  BBsetled,
  BBsetmode
};

// Callbacks define on which received commands we take action
void BBattachCommandCallbacks()
{
  // Attach callback methods
  cmdMessenger.attach(BBCommandList, ShowCommands);
  cmdMessenger.attach(OnUnknownCommand);
  cmdMessenger.attach(BBsetled, Onsetled);
  cmdMessenger.attach(BBsetheadlight, Onsetheadlight);
  cmdMessenger.attach(BBsetmode, Onsetmode);
}

// Show available commands
void ShowCommands() 
{
  Serial.println("\nAvailable commands");
  Serial.println(" 0;                       - This command list");
  Serial.println(" 3,<headlight state>;     - Set headlight.");
  Serial.println(" 4,<led state>;           - Set led. 0 = off, 1 = on"); 
  Serial.println(" 5,<mode>;                - Set mode n"); 
}

// Called when a received command has no attached function
void OnUnknownCommand()
{
  cmdMessenger.sendCmd(BBerror,"Command without attached callback\n");
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
  // Read led state argument, interpret string as boolean
  mode = cmdMessenger.readInt32Arg();
  if (mode == -1) 
    board_mode++;
  else if (mode == -2) 
    board_mode--;
  else 
    board_mode = mode;
  if (board_mode < 0)
        board_mode = 0;
  if (board_mode > 9)
        board_mode = 9;
  cmdMessenger.sendCmd(BBacknowledge,board_mode);
}


uint8_t row;

uint8_t wheel_color;

char *boards[] = {
  "PROTO",
  "PROTO",
  "AKULA",  
  "BOADIE", 
  "GOOFY", 
  "STEVE", 
  "JOON",
  "ARTEMIS"};
  
char *names[] = {
  "RICHARD",
  "RICHARD",
  "WOODSON",  
  "RIC", 
  "STEVE", 
  "STEVE", 
  "JOON",
  "JAMES"};


int32_t audio_level = 0;

int32_t read_audio()
{
  int32_t audio;

  audio = analogRead(AUDIO_PIN);

  Serial.print("Audio ");
  Serial.println(audio);

  return(audio_level);
 
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



void mydelay(uint32_t del) {
  int i;
  int8_t enc;
  char mode[10];
  boolean newmode = false;
  
  for (i = 0; i < del; i++) {
    delay(1);
    enc = read_encoder();
    if (enc) {
      Serial.print("Counter value: ");
      Serial.println(board_mode, DEC);
      board_mode += enc;
      if (board_mode < 0)
        board_mode = 0;
      if (board_mode > 9)
        board_mode = 9;
      clearScreen();
      sprintf(mode, "%d", board_mode);
      // Tell android of new mode
      cmdMessenger.sendCmdStart(BBsetmode);
      cmdMessenger.sendCmdArg(mode);
      cmdMessenger.sendCmdEnd();
      strip->print(mode, 35, 1, 1);
      strip->show();
      del = 300;
      newmode = true;
    }
  }
  if (newmode == true)
    clearScreen();
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
      strip->setPixelColor(i, wheel( ((i * 256 / strip->numPixels()) + j) % 256) );
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

#define FADER 50
// Fade the board
void fadeBoard() {
  int i;
  uint32_t color;
  uint8_t r, g, b;
  
    for (i=0; i < strip->numPixels() + (NSIDELIGHTS * 2); i++) {
      color = strip->getPixelColor(i);

      r = (color & 0x00ff0000) >> 16;
      g = (color & 0x0000ff00) >> 8;
      b = (color & 0x000000ff);
      if (r)
        r-=FADER;
      if (b)
        b-=FADER;
      if (g)
        g-=FADER;
        
      if (r < FADER)
        r = 0;
      if (g < FADER)
        g = 0;        
      if (b < FADER)
        b = 0;        
        /*
      Serial.print("color  ");
      Serial.print(color, HEX);
      Serial.print("  ");
      Serial.print(r, HEX);
      Serial.print("  ");
      Serial.print(g, HEX);
      Serial.print("  ");
      Serial.print(b, HEX);
      Serial.println("");
      */
      strip->setPixelColor(i, r, g, b);
    }  
    strip->show();   // write all the pixels out  
}

//Shift Matrixrow down
void shiftMatrixLines()
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
    shiftMatrixLines();
    strip->show();
  }

  // Red and White with solid blue
  for (x = 0; x < 4; x++) {
    strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, 0, 0));
    x++;
    strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, RGB_DIM, RGB_DIM));
  }
  shiftMatrixLines();
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
  shiftMatrixLines();
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
    shiftMatrixLines();
    strip->show();  
    // Blue/white
    for (x = 4; x < 10; x++) {
      strip->setPixelColor(x, 69, rgbTo24BitColor(0, 0, RGB_DIM));
      x++;
      strip->setPixelColor(x, 69, rgbTo24BitColor(RGB_DIM, RGB_DIM, RGB_DIM));
    }
    shiftMatrixLines();
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
  shiftMatrixLines();


  // 10 lines of blank
  for (x = 0; x < 10; x++) {
    strip->setPixelColor(x, 69, rgbTo24BitColor(0, 0, 0));
  }
  for (row = 0; row < 10; row++) {
    shiftMatrixLines();
    strip->show();
  }

}




void drawHeader() {
  uint32_t color;
  uint16_t x;

  for (x = 0; x < 10; x++) {
    //   color = random(2,4)%2 == 0 ? rgbTo24BitColor(0,0,0) : rgbTo24BitColor(0, 255, 0); //Chance of 1/3rd 
    color = random(2,4)%2 == 0 ? rgbTo24BitColor(0, 0, 0): wheel(wheel_color); //Chance of 1/3rd 
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

void drawHeaderXmas() {
  uint32_t color;
  uint16_t x;

  for (x = 0; x < 10; x++) {
    //   color = random(2,4)%2 == 0 ? rgbTo24BitColor(0,0,0) : rgbTo24BitColor(0, 255, 0); //Chance of 1/3rd 
    color = random(2,8)%2 == 0 ? rgbTo24BitColor(10, 10, 10): rgbTo24BitColor(128, 0, 0); //Chance of 1/3rd 
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

static int pd_x;
static int pd_y;
static int pd_side;

void drawPixelDust() {
  uint32_t color;
  uint16_t x, y;

  x = random(9);
  y = random(69);
  color = wheel(random(255));
  strip->setPixelColor(pd_x, pd_y, rgbTo24BitColor(0, 0, 0));
  strip->setPixelColor(pd_x+1, pd_y, rgbTo24BitColor(0, 0, 0));
  strip->setPixelColor(pd_x, pd_y+1, rgbTo24BitColor(0, 0, 0));
  strip->setPixelColor(pd_x+1, pd_y+1, rgbTo24BitColor(0, 0, 0));
  strip->setPixelColor(x, y, color);
  strip->setPixelColor(x+1, y, color);
  strip->setPixelColor(x, y+1, color);
  strip->setPixelColor(x+1, y+1, color);
  pd_x = x;
  pd_y = y;
  x = random(158);
  strip->setPixelColor(700 + pd_side, rgbTo24BitColor(0, 0, 0));
  strip->setPixelColor(700 + x, color);
  pd_side = x;
  strip->show();
}

void drawPixelDust2() {
  uint32_t color;
  uint16_t x, y;

  x = random(10);
  y = random(70);
  color = wheel(random(255));
  strip->setPixelColor(x, y, color);
  pd_x = x;
  pd_y = y;
  x = random(158);
  strip->setPixelColor(700 + x, color);
}

void drawStatic() {
  uint32_t color;
  uint16_t x, y;

  x = random(10);
  y = random(70);
  color = rgbTo24BitColor(200, 200, 200);
  strip->setPixelColor(x, y, color);
  pd_x = x;
  pd_y = y;
}


/* 
 *      |
 *     -#-
 *      |
*/
static int flake_row = 0;
static int flake_col = 0;
  
void drawSnowFlakes() {
  int x;
  uint32_t color;


       // Blue Background
       for (x = 0; x < 10; x++) {
    strip->setPixelColor(x, 69, rgbTo24BitColor(0, 0, 20));
       }
       setSideLight(0, (NSIDELIGHTS  - 1), rgbTo24BitColor(0, 0, 20));
       setSideLight(1, (NSIDELIGHTS  - 1), rgbTo24BitColor(0, 0, 20));

       switch(flake_row) {
    case 0:
    flake_col = random() % 8 + 1;
    strip->setPixelColor(flake_col, 69, rgbTo24BitColor(64, 64, 64));
    break;
    
    case 1:
    strip->setPixelColor(flake_col - 1, 69, rgbTo24BitColor(64, 64, 64));
    strip->setPixelColor(flake_col, 69, rgbTo24BitColor(255, 255, 255));
    strip->setPixelColor(flake_col + 1, 69, rgbTo24BitColor(64, 64, 64));   
    break;
    
    case 2:
    strip->setPixelColor(flake_col, 69, rgbTo24BitColor(64, 64, 64));
    break;
    
    case 3:
    break;

                default:
    break;
  }
  
  flake_row++;
  if (flake_row > 4) {
    flake_row = 0;
  }
        color = random(2,8)%2 == 0 ? rgbTo24BitColor(0, 0, 50): rgbTo24BitColor(128, 128, 128); //Chance of 1/3rd 

        // Ripple down the side lights with the same color as the edges
        setSideLight(0, (NSIDELIGHTS  - 1), color);
        setSideLight(1, (NSIDELIGHTS  - 1), color);
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
 Serial.print(bit, BIN);
 id = !bit;
 bit = digitalRead(ID_1);
 Serial.print(bit, BIN);
 id |= !bit << 1;
 bit = digitalRead(ID_2);
 Serial.print(bit, BIN);
 id |= !bit << 2; 
 bit = digitalRead(ID_3);
 Serial.print(bit, BIN);
 id |= !bit << 3;
 
 Serial.print("Board ID  ");
 Serial.print(id, DEC);
 Serial.println("");

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
void drawBattery() {
  uint32_t color;
  uint16_t x;
  uint8_t row;
  int32_t level = 0;
  uint16_t i;
  uint16_t sample;

  // Read Battery Level
  // 18 * 1.75v = 31.5v for low voltage
  // Set zero to 30v
  // Set 100% to 38v

  // Clear screen and measure voltage, since screen load varies it!
  clearScreen();
  strip->show();
  mydelay(1000);

  // Convert to level 0-28
  for (i = 0; i < 100; i++) {
    level += sample = analogRead(BATTERY_PIN);
    Serial.print("Battery sample ");
    Serial.print(sample, DEC);
    Serial.println(" ");
  }
  Serial.print("Battery Level ");
  Serial.print(level, DEC);
  Serial.println(" ");

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

  Serial.print("Adjusted Level ");
  Serial.print(level, DEC);
  Serial.println(" ");


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
  Serial.begin(115200);
  Serial.println("Goodnight moon!");

  // Set battery level analogue reference
#ifdef MEGA
  analogReference(INTERNAL1V1);
#else
  //  analogReference(INTERNAL);
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

  /*
     for (uint16_t i = 0; i < 544; i++) {
     Serial.print("Strip pixel ");
     Serial.print(i, DEC);
     Serial.print(" = virt pixel ");
     Serial.print(strip->pixel_translate[i], DEC);
     Serial.println(" ");
     }
   */

  boardId = readID();
  /*
  if (boardId == 0 || boardId == 1 || boardId == 2 || boardId == 3 || boardId == 4 || boardId == 7) {
    strip = new Board_WS2801((uint16_t)10, (uint16_t)70, WS2801_RGB, (boolean)true);
  } else {
    strip = new Board_WS2801((uint16_t)10, (uint16_t)70, WS2801_RGB, (boolean)false);
  }
  */
  strip = new Board_WS2801((uint16_t)10, (uint16_t)70, WS2801_RGB, (boolean)true);

  strip->begin();


  // test zone
/*
  clearScreen();
  int x;
  for (x = 0; x < 858; x++) {
     strip->setPixelColor(x, rgbTo24BitColor(50,50,50));
     strip->show();
    mydelay(50);
  }
       strip->setPixelColor(700, rgbTo24BitColor(0,0,50));
       strip->setPixelColor(779, rgbTo24BitColor(0,0,50));
     strip->show();

  mydelay(15000);
  */
  
  
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


//  strip->fillCircle(35, 5, 3, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  //strip->circles(15, 5, 5);
//  strip->show();
//  mydelay(5000);
  
//  for (i = 0; i < 500; i++) {
//    rainbowCycle();
//  }

  drawBattery();
  mydelay(1000);

  clearScreen();

  BBattachCommandCallbacks();
  cmdMessenger.sendCmd(BBacknowledge,"BB Ready\n");
  ShowCommands();

}


void loop_matrix()
{
  drawHeader();
  shiftMatrixLines();
  mydelay(50);
}

void loop_snowflakes()
{
  drawSnowFlakes();
  shiftMatrixLines();
  mydelay(50);
}

void loop_matrixfast()
{
  drawHeader();
  shiftMatrixLines();
  mydelay(1);
}


void loop_lunarian()
{
  drawHeaderLunarian();
  shiftMatrixLines(); 
  mydelay(1);
}

void loop_xmas()
{
  drawHeaderXmas();
  shiftMatrixLines(); 
  mydelay(1);
}

void loop_battery()
{
  drawBattery();
  mydelay(1000);
  clearScreen();
}


void loop_distrikt()
{
  //drawDistrikt();
  mydelay(10);
}

void loop_stanford(uint8_t enc)
{
  int i;
  
      for (i = 0; i < 20  && board_mode == enc; i++) {
        //drawStanford();
        strip->show();
        mydelay(300);
        fillScreen(rgbTo24BitColor(14, 2, 2u));
        strip->show();
        mydelay(300);
      }
}
  
void loop_pixeldust() {
  drawPixelDust();
  mydelay(5);

}

void loop_pixeldust2() {
  drawPixelDust2();
  fadeBoard();
  strip->show();
  mydelay(1);

}

void loop_static() {
  drawStatic();
  fadeBoard();
  strip->show();
  mydelay(1);

}


void loop_rainbow() {
  rainbowCycle2();
  mydelay(5);
}



int16_t loopcnt = 0;

int16_t state = 0;
// state = 0: lines
// state = 1: flag


void loop() {
  int i;
  
  // Process incoming serial data, and perform callbacks
  cmdMessenger.feedinSerialData();
   
   /*
   Serial.print("Encoder sample ");
   Serial.print(board_mode, DEC);
   Serial.println(" ");
   */
   
   switch (board_mode) {
     
     case 0:
       loop_matrix();
       break;
 
      case 1:
       loop_matrixfast();
       break;
       
     case 2:
       loop_lunarian();
       break;
       
     case 3:
       loop_snowflakes();
       break;
       
     case 4:
       loop_pixeldust();
       break;
       
     case 5:
       loop_pixeldust2();
       break;
       
     case 6:
       loop_rainbow();
       break;

     case 7:
       loop_xmas();
//       loop_stanford(7);
       break;
       
     case 8:
       loop_battery();
       break;

     case 9:
       loop_static();
       board_mode = 9;     
       
     default:
       mydelay(1);
       break;
   }  

}


void screenHook() {
//  checkButton();
}

#ifdef LIGHTCONTROL


uint16_t buttonPress = 0;
bool lightState = true;

#define HOLD_COUNT 30

void checkButton() {
  uint16_t remotePosition;

#ifdef MEGA
  remotePosition = analogRead(REMOTE_PIN);
  //  Serial.print("Remote position ");
  //  Serial.println(remotePosition);

  //  Serial.print("Button position ");
  //  Serial.println(buttonPress);

  if (remotePosition > 100) {
    ledsOn = true;
  } else {
    ledsOn = false;
  }

  if (buttonPress == HOLD_COUNT) {
    if (lightState == false) {
      lightState = true;
      digitalWrite(LRELAY_PIN, HIGH);
    } else {
      if (lightState == true) {
        lightState = false;
        digitalWrite(LRELAY_PIN, LOW);
      }  
    }
    buttonPress = 0;
  }


  if (remotePosition < 400 && remotePosition > 100) {
    buttonPress++;
    if (buttonPress > HOLD_COUNT) {
      buttonPress = HOLD_COUNT;
    }
  } else {
    if (buttonPress > 0) {
      buttonPress--;
    }
  } 
#else
  ledsOn = true;
#endif
}


#endif

#ifdef CRAP


   if (board_mode == 2) {
      drawUSflag();
      loopcnt += 50;
   }

#ifdef FOO

   if (ledsOn) {

    if (loopcnt > 3000) {
      loopcnt = 0;
      state++;
    }

    if (board_mode == 1) {
 /*
      for (i = 0; i < 5; i++) {
        drawVMW();
        strip->show();
        mydelay(2000);
        clearScreen();
        strip->show();
        mydelay(2000);
      }
 */
    
      drawDistrikt();
      state = 1;    

/*
      for (i = 0; i < 20; i++) {
        drawStanford();
        strip->show();
        mydelay(300);
        fillScreen(rgbTo24BitColor(14, 2, 2u));
        strip->show();
        mydelay(300);
      }

      state = 1;    
      drawSnowFlakes();
      shiftMatrixLines();

*/
    }



//    if (state == 0) {
//      drawCenter();
//      shiftMatrixCircles();
//    }  

    if (board_mode == 0) {
//      state = 2;
      drawHeader();
      shiftMatrixLines();
    }  

    if (board_mode == 2) {
      drawUSflag();
      loopcnt += 50;
    }


    if (board_mode == 3) {
      drawStanfordLogo();
      drawStanfordTree();
      mydelay(10000);
      state = 3;    
    }



// The Man
    if (board_mode == 4) {
        for (row = 0; row < 10; row++) {
          strip->setPixelColor(row, 69, 0);
        }
        strip->show();
        for (row = 0; row < 70; row++) {
          shiftMatrixLines();
          strip->show();
        }
 
        cycleTheMan();
        state = 3;    
    }
 

    if (board_mode == 5) {
      loopcnt = 0;
      state = 0;
      drawBattery();
      mydelay(1000);
      clearScreen();
    }


  } else {
    clearScreen();
    strip->show();
  }

  loopcnt++;
  
#endif
#endif

