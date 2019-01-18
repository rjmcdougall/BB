

#include <CmdMessenger.h>
#include <i2c_t3.h>

#include <SmartLEDShieldV4.h>  // uncomment this line for SmartLED Shield V4 (needs to be before #include <SmartMatrix3.h>)
#include <SmartMatrix3.h>


#define COLOR_DEPTH 24                  // known working: 24, 48 - If the sketch uses type `rgb24` directly, COLOR_DEPTH must be 24
const uint8_t kMatrixWidth = 64;        // known working: 32, 64, 96, 128
const uint8_t kMatrixHeight = 32;       // known working: 16, 32, 48, 64
const uint8_t kRefreshDepth = 36;       // known working: 24, 36, 48
const uint8_t kDmaBufferRows = 4;       // known working: 2-4, use 2 to save memory, more to keep from dropping frames and automatically lowering refresh rate
const uint8_t kPanelType = SMARTMATRIX_HUB75_32ROW_MOD16SCAN;   // use SMARTMATRIX_HUB75_16ROW_MOD8SCAN for common 16x32 panels
const uint8_t kMatrixOptions = (SMARTMATRIX_OPTIONS_NONE);      // see http://docs.pixelmatix.com/SmartMatrix for options
const uint8_t kBackgroundLayerOptions = (SM_BACKGROUND_OPTIONS_NONE);
const uint8_t kScrollingLayerOptions = (SM_SCROLLING_OPTIONS_NONE);
const uint8_t kIndexedLayerOptions = (SM_INDEXED_OPTIONS_NONE);

SMARTMATRIX_ALLOCATE_BUFFERS(matrix, kMatrixWidth, kMatrixHeight, kRefreshDepth, kDmaBufferRows, kPanelType, kMatrixOptions);
SMARTMATRIX_ALLOCATE_BACKGROUND_LAYER(backgroundLayer, kMatrixWidth, kMatrixHeight, COLOR_DEPTH, kBackgroundLayerOptions);
SMARTMATRIX_ALLOCATE_SCROLLING_LAYER(scrollingLayer, kMatrixWidth, kMatrixHeight, COLOR_DEPTH, kScrollingLayerOptions);
SMARTMATRIX_ALLOCATE_INDEXED_LAYER(indexedLayer, kMatrixWidth, kMatrixHeight, COLOR_DEPTH, kIndexedLayerOptions);

const int defaultBrightness = 100*(255/100);    // full brightness
//const int defaultBrightness = 15*(255/100);    // dim: 15% brightness
const int defaultScrollOffset = 6;
const rgb24 defaultBackgroundColor = {0x40, 0, 0};

// Teensy 3.0 has the LED on pin 13
const int ledPin = 13;

boolean do_lowbattery_actions = false;

//#define DEBUG_PIXELS 1

// Burner Boards have 2 relay outputs, speed-pin control and a spare aux relay
#define SPEED_PIN 23
#define AUX_PIN 22

#define RGB_MAX 255

const int ledsPerStrip = 552;
const int NUM_LEDS = 4416;


bool inited = false;


char field_separator   = ',';
char command_separator = ';';
char escape_separator  = '\\';


boolean batteryCritical = false;
boolean batteryLow = false;

uint32_t batteryControl = 0;        
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


// Local pixels x,y,color
void translatePixel(int x, int y, int32_t color) {
    rgb24 c = { (color & 0xFFFFFF >> 16), (color & 0xFFFF) >> 8, color & 0xff};
  // 23 - 16 = 7
  // 23 + 16 = 39
  // 59 - 32 = 27
  // 59 + 32 = 91
  if ((x > 7) && (x < 39) && (y > 27) && (y < 91)) { 
    backgroundLayer.drawPixel(y - 27, x - 7, c);
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
  BBDisplayText,
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
  cmdMessenger.attach(BBDisplayText, OnDisplayText);    // 13
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

  if (do_lowbattery_actions && batteryCritical) {
    fillScreen(rgbTo24BitColor(40,0,0));
    drawBattery();
    drawBatteryTop();
  }

  if (showingBattery > 0) {
    drawBatteryTop();
    showingBattery--;
  }
  
  // skip if charging
  if (0 && batteryAverageCurrent < 32000) {
    return;
  }
  
  if (batteryLow) {
    //clearScreen();
    drawBatteryTop();
  }
  backgroundLayer.swapBuffers();
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
  //clearScreen();
  //drawBattery();
  //leds.show();
  //delay(3000);
  //batteryStats();
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
struct rowType { char row[2048]; };
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
  char text[32];
  
  row = cmdMessenger.readInt32Arg();
  response =  cmdMessenger.readBinArg<rowType>();
  pixels = (char *)&response;
  cmdMessenger.unescape(pixels, nPixels);

  //sprintf(text, "row %3ld", row);
  //backgroundLayer.fillScreen({0,0,0});
  //backgroundLayer.drawString(0, kMatrixHeight / 2 - 6, {255,0,0}, text);
  //backgroundLayer.swapBuffers();
#ifdef DEBUG_PIXELS
  Serial1.print("Onw ");
  Serial1.print(row);
  Serial1.print(" = ");
  Serial1.println(nPixels);
  //Serial1.print("<");
#endif

  // Catch error case
  if (row > 63) {
    row = 63;
  }


  int startPixel = row * ledsPerStrip;
  
  for (i = 0; i < ledsPerStrip * 3; i += 3) {
    r = pixels[i];
    g = pixels[i+1];
    b = pixels[i+2];
    backgroundLayer.drawPixel(row, i / 3, {r, g, b});
    //translatePixel(i / 3, row, rgbTo24BitColor(r, g, b));

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
    backgroundLayer.fillScreen({0,0,0});
    //backgroundLayer.swapBuffers();
}

// Fill screen
void fillScreen(uint32_t color) {
    rgb24 c = { (color & 0xFFFFFF >> 16), (color & 0xFFFF) >> 8, color & 0xff};
    backgroundLayer.fillScreen(c);
    //backgroundLayer.swapBuffers();
}

void batteryStats() {
  char batt[16];
  
  //clearScreen();a
  //drawBattery();
  //leds.show();
  //delay(5000);

  //clearScreen();
  sprintf(batt, "%d v", batteryVoltage);
  //leds.print(batt, 12, 1, 1);
  //leds.show();
  Serial.println(batt);
  //delay(5000);

  //clearScreen();
  sprintf(batt, "%d C", batteryRemainingCapacity);
  //leds.print(batt, 12, 1, 1);
  //leds.show();
  Serial.println(batt);
  //delay(5000);

  //clearScreen();
  sprintf(batt, "%d F", batteryFullChargeCapacity);
  //leds.print(batt, 12, 1, 1);
  //leds.show();
  Serial.println(batt);
  //delay(5000);

  //clearScreen();
  sprintf(batt, "%d A", batteryAverageCurrent);
  //leds.print(batt, 12, 1, 1);
  //leds.show();
  Serial.println(batt);
  //delay(5000);
  
}

//#include <Wire.h>  // Wire library for communicating over I2C
#define BQ34Z100 0x55

int getBattery8(int reg) {
  Wire.beginTransmission(BQ34Z100);
  Wire.write(reg);
  Wire.endTransmission();
  Wire.requestFrom(BQ34Z100,1);
  return (Wire.read());
}

int getBattery16(int register1, int register2) {
  unsigned int low;
  unsigned int high;
  unsigned int high1;
  int value = -1;
  
  if ((value = getBattery8(register1)) != -1) {
    low = value;
    if ((value = getBattery8(register2)) != -1) {
      high1 = value << 8;
      high1 = high1 + low;
      return(high1);
    }
  }
  return(-1);
}

/* 
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
 */
int getBatteryAll() {

  int value;

  batteryStateOfCharge = 50;
  batteryCurrent = -100;
  batteryAverageCurrent = -100;
  return 50;

  // batteryControl
  getBattery16(0x00, 0x01);
  value = getBattery16(0x00, 0x00);
  if (value >= 0) {
    batteryControl = value;
  }
  
  // Capacity in %age
  value = getBattery8(0x02);
  if (value > 100) {
    value = 100;
  }
  if (value >= 0) {
    batteryStateOfCharge = value;
  }

  // Max Error %age
  value = getBattery8(0x03);
  if (value > 100) {
    value = 100;
  }
  if (value >= 0) {
    batteryMaxError = value;
  }

  // Capacity in mAH
  value = getBattery16(0x04, 0x05);
  if (value != -1) {
    batteryRemainingCapacity = value;
  }
  
  // Learned Capacity 
  value = getBattery16(0x06, 0x07);
  if (value != -1) {
    batteryFullChargeCapacity = value;
  }

  // Voltage
  value = getBattery16(0x08, 0x09);
  if (value != -1) {
    batteryVoltage = value;
  }

  // Avg Current
  value = getBattery16(0xa, 0xb);
  if (value != -1) {
    batteryAverageCurrent = value;
  }

  // Temp
  value = getBattery16(0xc, 0xd);
  if (value != -1) {
    batteryTemperature = value;
  }

  // batteryFlags
  value = getBattery16(0xe, 0xf);
  if (value != -1) {
    batteryFlags = value;
  }

  // Instant Current
  value = getBattery16(0x10, 0x11);
  if (value != -1) {
    batteryCurrent = value;
  }

  // batteryFlagsB
  value = getBattery16(0x12, 0x13);
  if (value != -1) {
    batteryFlagsB = value;
  }

  return(batteryStateOfCharge);
}

int getBatteryLevel() {

  return 14;
  int value;

  // Capacity in %age
  value = getBattery8(0x02);
  if (value > 100) {
    value = 100;
  }
  if (value >= 0) {
    batteryStateOfCharge = value;
  }
  
  return(batteryStateOfCharge);
}


void drawBattery() {
  uint32_t color;
  uint16_t x;
  int level;
  int battWidth = 20;
  int battLeft = 23 - battWidth / 2;
  int startRow = 34;
  int row;
  int endRow = 84;
  
  level = getBatteryLevel() / 2; // convert 100 to max of 50

  row = startRow;
  
  // White Battery Shell with Green level

  // Battery Bottom
  for (x = 0; x < battWidth; x++) {
    translatePixel(x + battLeft, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  }
  row++;

  // Battery Sides
  for (; row <= endRow; row++) {
    translatePixel(battLeft, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
    translatePixel(battLeft + battWidth - 1, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  }

  // Battery Top
  for (x = 0; x < battWidth; x++) {
    translatePixel(x + battLeft, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  }
  row++;

  // Battery button
  for (x = battWidth / 2 - 6; x < battWidth / 2 + 6; x++) {
    translatePixel(x + battLeft, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
    translatePixel(x + battLeft, row+1, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
    translatePixel(x + battLeft, row+2, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
    translatePixel(x + battLeft, row+3, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  }

  // Get level failed
  if (level < 0) {
    // RED
    for (row = startRow + 1; row < endRow ; row++) {
      for (x = battLeft; x < battLeft + battWidth; x++) {
        translatePixel(x, row, rgbTo24BitColor(255, 0, 0));
      }
    }
  } else {
    // Battery Level
    uint32_t batteryColor;
    if (batteryCritical) {
      batteryColor = rgbTo24BitColor(255, 0, 0);
    } else if (batteryLow) {
      batteryColor = rgbTo24BitColor(255, 165, 0);
    }  else {
      batteryColor = rgbTo24BitColor(0, 255, 0);
    }
    for (row = startRow + 1; row < startRow + level ; row++) {
      for (x = battLeft  + 1; x < battLeft + battWidth - 1; x++) {
        translatePixel(x, row, batteryColor);
      }
    }
    for (; row <= endRow ; row++) {
      for (x = battLeft + 1; x < battLeft + battWidth - 1; x++) {
        translatePixel(x, row, 0);
      }
    }
  }
}

void drawBatteryTop() {
  uint32_t color;
  uint16_t x;
  uint8_t row;
  int level;

  // Little battery at top
  // row 105-114
  // 105-112 is battery
  // 113-114 is button
  // inside is 106-111
  
  level = 1 + getBatteryLevel() / 19; // convert 100 to max of 7

  row = 105;

  // White Battery Shell with Green level

  // Battery Bottom
  for (x = 20; x < 26; x++) {
    translatePixel(x, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  }
  row++;

  // Battery Sides
  for (; row < 112; row++) {
    translatePixel(20, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
    translatePixel(25, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  }

  // Battery Top
  for (x = 20; x < 26; x++) {
    translatePixel(x, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  }
  row++;

  // Battery button
  for (x = 22; x < 24; x++) {
    translatePixel(x, row, rgbTo24BitColor(RGB_MAX, RGB_MAX, RGB_MAX));
  }
 

  // Get level failed
  if (level < 0) {
    // RED
    for (row = 106; row < 112; row++) {
      for (x = 21; x < 25; x++) {
        translatePixel(x, row, rgbTo24BitColor(255, 0, 0));
      }
    }
  } else {
    // Battery Level
    uint32_t batteryColor;
    if (batteryCritical) {
      batteryColor = rgbTo24BitColor(255, 0, 0);
    } else if (batteryLow) {
      batteryColor = rgbTo24BitColor(255, 165, 0);
    }  else {
      batteryColor = rgbTo24BitColor(0, 255, 0);
    }
    for (row = 106; row < 106 + level; row++) {
      for (x = 21; x < 25; x++) {
        translatePixel(x, row, batteryColor);
      }
    }
    for (; row < 112; row++) {
      for (x = 21; x < 25; x++) {
        translatePixel(x, row, 0);
      }
    }
  }
}

void setup() {
  char batt[16];
  
  // Console for debugging
  Serial.begin(115200);
  Serial1.begin(115200); // Initialize Serial Monitor USB
  Serial.println("Goodnight moon!");
  Serial1.println("Goodnight moon!");

  // Setup motor control speed limiter to no-limit
  pinMode(AUX_PIN, OUTPUT);
  pinMode(SPEED_PIN, OUTPUT);
  //limitBoardSpeed(false);
  
  BBattachCommandCallbacks();
  cmdMessenger.sendCmd(BBacknowledge,"BB Ready\n");
  ShowCommands();

  // Setup I2C for battery monitor
  /*
  // Default Wire library
  Wire.setClock(10000);
  Wire.begin();
  */
  // Setup for Master mode, pins 18/19, internal pullups, 100kHz, 200ms default timeout
  Wire.begin(I2C_MASTER, 0x00, I2C_PINS_18_19, I2C_PULLUP_EXT, 100000);
  Wire.setDefaultTimeout(200000); // 200ms

  // setup matrix
  matrix.addLayer(&backgroundLayer); 
  matrix.begin();

  /* I2C Changes Needed for SmartMatrix Shield */
  // switch pins to use 16/17 for I2C instead of 18/19, after calling matrix.begin()//
  pinMode(18, INPUT);
  pinMode(19, INPUT);
  CORE_PIN16_CONFIG = (PORT_PCR_MUX(2) | PORT_PCR_PE | PORT_PCR_PS);
  CORE_PIN17_CONFIG = (PORT_PCR_MUX(2) | PORT_PCR_PE | PORT_PCR_PS);

  // display a simple message - will stay on the screen if calls to the RTC library fail later
  backgroundLayer.fillScreen({0,0,0});
  backgroundLayer.setFont(gohufont11b);
  backgroundLayer.drawString(0, kMatrixHeight / 2 - 6, {0,255,0}, "Burner Board");
  backgroundLayer.swapBuffers(false);

  matrix.setBrightness(defaultBrightness);
}

uint8_t c = 0;

#define RED    0x160000
#define GREEN  0x001600
#define BLUE   0x000016
#define YELLOW 0x101400
#define PINK   0x120009
#define ORANGE 0x100400
#define WHITE  0x202020

// Counter to see if we check battery level
unsigned long lastBatteryCheck = micros() - (20 * 1000000);;

void loop() {

  int i;
  unsigned long ts;
  int batteryStateOfCharge = -1;
  char text[32];
  
  // Process incoming serial data, and perform callbacks
  ts = micros();
  // Every 10 seconds check batter level from battery computer
  if ((ts - lastBatteryCheck) > 10000000) {
    lastBatteryCheck = ts;
    batteryStateOfCharge = 50;getBatteryAll();
    if (batteryStateOfCharge >= 0) {
      // valid reading
      if (batteryStateOfCharge <= 25) {
        batteryLow = true;
      } else {
        batteryLow = false;
      }
    }
    if ((batteryVoltage > 10000) && (batteryVoltage < 35400)) {
        if (do_lowbattery_actions) {
          //limitBoardSpeed(true);
        }
        batteryCritical = true;
    } else {
        //limitBoardSpeed(false);
        batteryCritical = false;
    }
    // Send upstrean to Android
    OnGetBatteryLevel();
  }
  
  // Uh oh, battery is almost dead
  if (0 & batteryCritical) {
      fillScreen(rgbTo24BitColor(40,0,0));
      drawBattery();
      drawBatteryTop();
      backgroundLayer.swapBuffers();
  } else {

    // display battery if charging, currrent > 1000mah (10 divider)
    if (0 && batteryAverageCurrent < 32000) {
      clearScreen();
      drawBattery();
      backgroundLayer.swapBuffers();
      delay(2000);
      clearScreen();
      backgroundLayer.swapBuffers();
      delay(2000);
    }

    // Pulse the screen until connected to android
    if (inited == false) {
      fillScreen(rgbTo24BitColor(c,c + 10,40));
      //backgroundLayer.fillScreen({c,c+10,40});

      batteryStateOfCharge = getBatteryLevel();
      drawBattery();
      backgroundLayer.swapBuffers();
      c++;
      if (c > 25)
        c = 0;
    }
  }

  //sprintf(text, "%ld", ts/1000);
  //backgroundLayer.fillScreen({0,0,0});
  //backgroundLayer.drawString(0, kMatrixHeight / 2 - 6, {255,0,0}, text);
  //backgroundLayer.swapBuffers();
  
  cmdMessenger.feedinSerialData();

}

// Enable the speed pin -- true means make the board speed limited
void limitBoardSpeed(bool limit) {
    if (limit) {
      digitalWrite(SPEED_PIN, LOW);
    } else {
      digitalWrite(SPEED_PIN, HIGH);
    }
}



void OnDisplayText() {
    int x = kMatrixWidth/2-15;

    char * msg = cmdMessenger.readStringArg();
    
    backgroundLayer.fillScreen({0,0,0});
    backgroundLayer.setFont(gohufont11b);
    backgroundLayer.drawString(0, kMatrixHeight / 2 - 6, {0,255,0}, msg);
    backgroundLayer.swapBuffers();
}


void print2digits(int number) {
  if (number >= 0 && number < 10) {
    Serial.write('0');
  }
  Serial.print(number);
}



