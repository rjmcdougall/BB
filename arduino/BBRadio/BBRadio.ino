#include <SPI.h>
#include <RH_RF95.h>
#include <CmdMessenger.h>

#include <Wire.h>


#define LED 13

/* for feather m0 */
#define RFM95_CS 8
#define RFM95_RST 4
#define RFM95_INT 3

// Change to 434.0 or other frequency, must match RX's freq!
#define RF95_FREQ 915.0

// Singleton instance of the radio driver
RH_RF95 rf95(RFM95_CS, RFM95_INT);


// timing
#define TRANSMIT_INTERVAL 1000      // interval between sending updates
#define DISPLAY_INTERVAL 1000//150      // interval between updating display
#define MAX_FIX_AGE 5000   // Ignore data from GPS if older than this
unsigned long lastSend, lastDisplay, lastFix, lastRecv;
bool sending = false;

// 95% error radius at HDOP=1
#define GPS_BASE_ACCURACY 6.2  // m

#define ACCURACY_THRESHOLD 30  // m

// tinyGPS
#include <TinyGPS.h>

TinyGPS gps;

// Attach a new CmdMessenger object to the default SerialUSB port
char field_separator   = ',';
char command_separator = ';';
char escape_separator  = '\\';
CmdMessenger cmdMessenger = CmdMessenger(Serial, field_separator, command_separator, escape_separator);

// This is the list of recognized BB commands. 
enum
{
  BBRacknowledge,
  BBRCommandList,
  BBRGPS,
  BBRTime,
  BBRReceive,
  BBRBroadcast
};

// Callbacks define on which received commands we take action
void BBattachCommandCallbacks()
{
  // Attach callback methods
  cmdMessenger.attach(BBRCommandList, ShowCommands);    // 1
  cmdMessenger.attach(BBRGPS, OnGps);                   // 2
  cmdMessenger.attach(BBRTime, OnTime);                 // 3
  cmdMessenger.attach(BBRTime, OnReceive);              // 4
  cmdMessenger.attach(BBRBroadcast, OnBroadcast);       // 5
}

// Show available commands
void ShowCommands() 
{
  Serial.println("\nAvailable commands");
  Serial.println(" 1;                       - This command list");
  Serial.println(" 5,n,n,n,n,n;             - Send bytes");
};

void OnGps() {

}

void EmitGps(char * str) {
  cmdMessenger.sendCmdStart(BBRGPS);
  cmdMessenger.sendCmdArg(str);
  cmdMessenger.sendCmdEnd();  
}

void OnTime() {
  
}

void OnReceive() {
  
}

void EmitReceive(uint32_t signalStrength, uint8_t *bytes, int len) {
  cmdMessenger.sendCmdStart(BBRReceive);
  cmdMessenger.sendCmdArg(signalStrength);
  for (int i = 0; i < len; i++) {
    cmdMessenger.sendCmdArg(bytes[i]);
  }
  cmdMessenger.sendCmdEnd();  
}



uint8_t radiopacket[256]; // Max RF95 send size is ~230 bytes

void OnBroadcast() {
  int32_t len = cmdMessenger.readInt32Arg();
  if (len > 0) {
    for (int i = 0; (i < len) && (i < 240); i++ ) {
        radiopacket[i] = cmdMessenger.readInt32Arg();
    }
  }
  rf95.send((uint8_t *)radiopacket, len);
  rf95.waitPacketSent();
}


void setup() {
  pinMode(RFM95_RST, OUTPUT);
  digitalWrite(RFM95_RST, HIGH);

  pinMode(LED, OUTPUT);


  // manual reset
  digitalWrite(RFM95_RST, LOW);
  delay(10);
  digitalWrite(RFM95_RST, HIGH);
  delay(10);

  while (!rf95.init()) {
    Serial.println("LoRa radio init failed");
    while (1);
  }

  // Defaults after init are 434.0MHz, modulation GFSK_Rb250Fd250, +13dbM
  if (!rf95.setFrequency(RF95_FREQ)) {
    Serial.println("setFrequency failed");
    while (1);
  }

  // Defaults after init are 434.0MHz, 13dBm, Bw = 125 kHz, Cr = 4/5, Sf = 128chips/symbol, CRC on

  // The default transmitter power is 13dBm, using PA_BOOST.
  // If you are using RFM95/96/97/98 modules which uses the PA_BOOST transmitter pin, then
  // you can set transmitter powers from 5 to 23 dBm:
  rf95.setTxPower(23, false);

  Serial.begin(9600);
  Serial1.begin(9600);
}

//String timeStr = "";
uint8_t buf[RH_RF95_MAX_MESSAGE_LEN];
int lastRSSI;


// String to collect GPS responses
char gpsBlurb[1024] = "";
char *gpsBlurbPtr = &gpsBlurb[0];

void processGps(char c) {
    if (c == 13) { // CR at end of emission
      *gpsBlurbPtr = 13;
      *gpsBlurbPtr++;
      *gpsBlurbPtr = 10;
      *gpsBlurbPtr++;
      *gpsBlurbPtr = 0;
      EmitGps(gpsBlurb);
      gpsBlurbPtr = &gpsBlurb[0]; 
    } else {
      if (c != 10) { //ignore LF
        if (c == 44) {
          c = 95;
        }
        *gpsBlurbPtr = c;
        gpsBlurbPtr++;
        *gpsBlurbPtr = 0;
      }
    }  
}


void loop() {
  if (Serial1.available()) {
    char c = Serial1.read();
    processGps(c);
  }

  if (rf95.available()) {
    //Serial.println("Received packet");
    uint8_t len = sizeof(buf);
    if (rf95.recv(buf, &len)) {
      lastRSSI = rf95.lastRssi();
      digitalWrite(LED, HIGH);
      EmitReceive(lastRSSI, buf, len);
      digitalWrite(LED, LOW);
    }
  }
}



