

#include <OctoWS2811.h>
#include <CmdMessenger.h>


//#define DEBUG_PIXELS 1

#define RGB_MAX 255
#define RGB_MAX_INITIAL 32

#define NUM_LEDS_PER_STRIP 550
//552
#define NUM_STRIPS 16
#define NUM_LEDS (NUM_STRIPS * NUM_LEDS_PER_STRIP)

const int numPins = NUM_STRIPS;
byte pinList[numPins] = {1, 0, 24, 25, 19, 18, 14, 15, 17, 16, 22, 23, 20, 21, 26, 27};
const int ledsPerStrip = NUM_LEDS_PER_STRIP;

                         // These buffers need to be large enough for all the pixels.
                         // The total number of pixels is "ledsPerStrip * numPins".
                         // Each pixel needs 3 bytes, so multiply by 3.  An "int" is
                         // 4 bytes, so divide by 4.  The array is created using "int"
                         // so the compiler will align it to 32 bit memory.
const int bytesPerLED = 3;  // change to 4 if using RGBW
DMAMEM int displayMemory[ledsPerStrip * numPins * bytesPerLED / 4];
int drawingMemory[ledsPerStrip * numPins * bytesPerLED / 4];

#define RGB(r, g, b) (((r) << 16) | ((g) << 8) | (b))


const int config = WS2811_GRB | WS2811_800kHz;

OctoWS2811 leds(ledsPerStrip, displayMemory, drawingMemory, config, numPins, pinList);

bool inited = false;

char field_separator   = ',';
char command_separator = ';';
char escape_separator  = '\\';


#define PIXEL_RED  0;
#define  PIXEL_GREEN 1;
#define  PIXEL_BLUE 2;

// Convert HSL (Hue, Saturation, Lightness) to RGB (Red, Green, Blue)
//
//   hue:        0 to 359 - position on the color wheel, 0=red, 60=orange,
//                            120=yellow, 180=green, 240=blue, 300=violet
//
//   saturation: 0 to 100 - how bright or dull the color, 100=full, 0=gray
//
//   lightness:  0 to 100 - how light the color is, 100=white, 50=color, 0=black
//
int makeColor(unsigned int hue, unsigned int saturation, unsigned int lightness)
{
  unsigned int red, green, blue;
  unsigned int var1, var2;

  if (hue > 359) hue = hue % 360;
  if (saturation > 100) saturation = 100;
  if (lightness > 100) lightness = 100;

  // algorithm from: http://www.easyrgb.com/index.php?X=MATH&H=19#text19
  if (saturation == 0) {
    red = green = blue = lightness * 255 / 100;
  } else {
    if (lightness < 50) {
      var2 = lightness * (100 + saturation);
    } else {
      var2 = ((lightness + saturation) * 100) - (saturation * lightness);
    }
    var1 = lightness * 200 - var2;
    red = h2rgb(var1, var2, (hue < 240) ? hue + 120 : hue - 240) * 255 / 600000;
    green = h2rgb(var1, var2, hue) * 255 / 600000;
    blue = h2rgb(var1, var2, (hue >= 120) ? hue - 120 : hue + 240) * 255 / 600000;
  }
  return (red << 16) | (green << 8) | blue;
}

unsigned int h2rgb(unsigned int v1, unsigned int v2, unsigned int hue)
{
  if (hue < 60) return v1 * 60 + (v2 - v1) * hue;
  if (hue < 180) return v2 * 60;
  if (hue < 240) return v1 * 60 + (v2 - v1) * (240 - hue);
  return v1 * 60;
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
  showingBattery = 30;
}

void OnGetBatteryLevel() {

}


// row is sized larger because some chars might be escaped
struct rowType {
  char row[NUM_LEDS_PER_STRIP * 3 * 2];
};
typedef rowType rowType;

// Set a row of 10 r,g,b pixels from binary data
// The first an last pixel are for the side lights
void OnSetRow() {
  rowType response;
  char *pixels;
  uint32_t row;
  int i;
  int nPixels = NUM_LEDS_PER_STRIP * 3; //sizeof(rowType);
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
  if (row > (NUM_STRIPS - 1)) {
    row = (NUM_STRIPS - 1);
  }

  // NanoPi/Teensy4 board is reversed
  // Strip 1 is at front of board
  //row = 16 - row;

  int startPixel = row * NUM_LEDS_PER_STRIP;

  for (i = 0; i < NUM_LEDS_PER_STRIP * 3; i += 3) {
    r = pixels[i];
    g = pixels[i + 1];
    b = pixels[i + 2];

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
  return RGB(r, g, b);
}

// Clear Screen
void clearScreen()
{
  int i;
  for (i = 0; i < NUM_LEDS; i++) {
    leds.setPixel(i, 0);
  }
  leds.show();
}

// Fill screen
void fillScreen(uint32_t color) {
  register int i;
  for (i = 0; i < NUM_LEDS; i++) {
    leds.setPixel(i, color);
  }
}



void setup() {
  char batt[16];
  delay(1000);
  // Console for debugging
  //Serial.begin(115200);
  //Serial.println("Goodnight moon!");



  BBattachCommandCallbacks();
  leds.begin();
}

uint8_t c = 0;

#define RED    0x160000
#define GREEN  0x001600
#define BLUE   0x000016
#define YELLOW 0x101400
#define PINK   0x120009
#define ORANGE 0x100400
#define WHITE  0x202020


static uint8_t hue = 0;

// Counter to see if we check battery level
unsigned long lastBatteryCheck = micros() - (20 * 1000000);;



void loop() {

  // Process incoming serial data, and perform callbacks

  // Pulse the screen until connected to android
  if (inited == false) {
    for (int i = 0; i < NUM_STRIPS; i++) {
      for (int j = 0; j < NUM_LEDS_PER_STRIP; j++) {

        int color;

        if ((j % 3) == 0) {
          color = makeColor((32 * i) + hue + j, 75, 100);
        } else {
          color = makeColor(0, 0, 0);
        }

        leds.setPixel((i * NUM_LEDS_PER_STRIP) + j, color);
      }
    }

    // Set the first n leds on each strip to show which strip it is
    for (int i = 0; i < NUM_STRIPS; i++) {
      for (int j = 0; j <= i; j++) {
        leds.setPixel((i * NUM_LEDS_PER_STRIP) + j, 255, 0, 0);
      }
    }
    hue++;
    leds.show();
  }
  cmdMessenger.feedinSerialData();

}
