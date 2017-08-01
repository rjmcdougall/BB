

#include <OctoWS2811.h>
#include <CmdMessenger.h>


//#define DEBUG_PIXELS 1

const int ledsPerStrip = 552;
const int NUM_LEDS = 4416;

DMAMEM int displayMemory[ledsPerStrip*6];
int drawingMemory[ledsPerStrip*6];

const int config = WS2811_GRB | WS2811_800kHz;
OctoWS2811 leds(ledsPerStrip, displayMemory, drawingMemory, config);
bool inited = false;


char field_separator   = ',';
char command_separator = ';';
char escape_separator  = '\\';


boolean batteryCritical = false;
boolean batteryLow = false;
int  batteryLevel = -1;
int batteryCapacityRemaining = -1;
int batteryCapacityMax = -1;
int batteryVoltage = -1;
int batteryCurrent = -1;

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
}

// Called when a received command has no attached function
void BBCmdOne()
{
  cmdMessenger.sendCmd(BBerror,"BB Error\n");
  Serial.print("BB Error ");

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
  cmdMessenger.sendCmd(BBacknowledge,ledsOn);
}

void OnClearScreen()
{
  clearScreen();
}

void OnUpdate() {

  if (batteryCritical) {
    fillScreen(rgbTo24BitColor(255,0,0));
  }
  // skip if charging
  if (0 && batteryCurrent < 32000) {
    return;
  }
  
  if (batteryLow) {
    clearScreen();
    //drawBattery();
  }
  leds.show();
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
  clearScreen();
  drawBattery();
  leds.show();
  delay(3000);
  batteryStats();
}

void OnGetBatteryLevel() {
  cmdMessenger.sendCmdStart(BBGetBatteryLevel);
  cmdMessenger.sendCmdArg(batteryLevel);
  cmdMessenger.sendCmdArg(batteryCapacityRemaining);
  cmdMessenger.sendCmdArg(batteryCapacityMax);
  cmdMessenger.sendCmdArg(batteryVoltage);
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

  int startPixel = row * ledsPerStrip;
  
  for (i = 0; i < ledsPerStrip * 3; i += 3) {
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
    leds.setPixel(startPixel + (i / 3), r, g, b);
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
    leds.setPixel(i, rgbTo24BitColor(0,0,0));
  }
  leds.show();
}

// Fill screen
void fillScreen(uint32_t color) {
  register int i;
  for (i=0; i < NUM_LEDS; i++) {
    leds.setPixel(i, color);
  }
  leds.show();
}

void batteryStats() {
  char batt[16];
  
  clearScreen();
  drawBattery();
  leds.show();
  delay(5000);

  clearScreen();
  sprintf(batt, "%d v", batteryVoltage);
  //leds.print(batt, 12, 1, 1);
  //leds.show();
  delay(5000);

  clearScreen();
  sprintf(batt, "%d C", batteryCapacityRemaining);
  //leds.print(batt, 12, 1, 1);
  //leds.show();
  delay(5000);

  clearScreen();
  sprintf(batt, "%d F", batteryCapacityMax);
  //leds.print(batt, 12, 1, 1);
  //leds.show();
  delay(5000);

  clearScreen();
  sprintf(batt, "%d A", batteryCurrent);
  //leds.print(batt, 12, 1, 1);
  leds.show();
  delay(5000);
  
}

#include <Wire.h>  // Wire library for communicating over I2C
#define BQ34Z100 0x55

unsigned int nonBlockingRead() {
  for (int i = 0; (i < 3) &&  (!Wire.available()); i++) {
    delay(1);
  }
  if (Wire.available()) {
    return(Wire.read());
  } else {
    return (-1);
  }
}

int getBattery8(int reg) {
  Wire.beginTransmission(BQ34Z100);
  Wire.write(reg);
  Wire.endTransmission();
  Wire.requestFrom(BQ34Z100,1);
  return (nonBlockingRead());
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

int getBatteryAll() {

  int value;

  // Capacity in %age
  value = getBattery8(0x02);
  if (value > 100) {
    value = 100;
  }
  if (value >= 0) {
    batteryLevel = value;
  }

  // Capacity in mAH
  value = getBattery16(0x04, 0x05);
  if (value != -1) {
    batteryCapacityRemaining = value;
  }
  
  // Learned Capacity 
  value = getBattery16(0x06, 0x07);
  if (value != -1) {
    batteryCapacityMax = value;
  }

  // Voltage
  value = getBattery16(0x08, 0x09);
  if (value != -1) {
    batteryVoltage = value;
  }

  // Current
  value = getBattery16(0xa, 0xb);
  if (value != -1) {
    batteryCurrent = value;
  }
  
  return(batteryLevel);
}

int getBatteryLevel() {

  int value;

  // Capacity in %age
  value = getBattery8(0x02);
  if (value > 100) {
    value = 100;
  }
  if (value >= 0) {
    batteryLevel = value;
  }
  
  return(batteryLevel);
}


void drawBattery() {
}


void setup() {
  char batt[16];
  
  // Console for debugging
  Serial.begin(115200);
  Serial1.begin(115200); // Initialize Serial Monitor USB
  Serial.println("Goodnight moon!");
  Serial1.println("Goodnight moon!");
  BBattachCommandCallbacks();
  cmdMessenger.sendCmd(BBacknowledge,"BB Ready\n");
  ShowCommands();
  
  leds.begin();
  //fillScreen(rgbTo24BitColor(13,30,96));
  //leds.show();

  for (int i = 0; i < 100; i++) {
    leds.setPixel((0 * 552) + i,0,0,40);
    leds.show();
  }

}


uint8_t c = 0;

void loop() {
  if (inited == false) {
    fillScreen(rgbTo24BitColor(c,c + 10,48));
    leds.show();
    c++;
    if (c > 64)
      c = 0;
  }
  cmdMessenger.feedinSerialData();

}


