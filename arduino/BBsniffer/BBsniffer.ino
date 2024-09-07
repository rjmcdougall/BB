
#include <SPI.h>
#include <RH_RF95.h>


#define REPEATER_BIT(rbit) (1 << rbit)
#define REPEATED_BY(rpt, by) ((rpt & REPEATER_BIT(by)) > 0)


/* for moteino 

#define LED 9
#define RFM95_CS 8
#define RFM95_RST 4
#define RFM95_INT 7
#define LED 9
 */


/* for teensy 

#define LED 7
#define RFM95_CS 10
#define RFM95_RST 4
#define RFM95_INT 3
*/


/* for feather m0 
*/
#define LED 13
#define RFM95_CS 8
#define RFM95_RST 4
#define RFM95_INT 3

// Pin definetion of WIFI LoRa 32
// HelTec AutoMation 2017 support@heltec.cn
/*
#define SCK     5    // GPIO5  -- SX127x's SCK
#define MISO    19   // GPIO19 -- SX127x's MISO
#define MOSI    27   // GPIO27 -- SX127x's MOSI
#define RFM95_CS      18   // GPIO18 -- SX127x's CS
#define RFM95_RST     14   // GPIO14 -- SX127x's RESET
#define RFM95_INT     26   // GPIO26 -- SX127x's IRQ(Interrupt Request)
#define LED 22
*/

// Change to 434.0 or other frequency, must match RX's freq!
#define RF95_FREQ 915.0

// Singleton instance of the radio driver
RH_RF95 rf95(RFM95_CS, RFM95_INT);  // Teensy and m0
//RH_RF95 rf95; //moteino


// timing
#define TRANSMIT_INTERVAL 10000  // interval between sending updates
#define DISPLAY_INTERVAL 5000    //150      // interval between updating display
#define MAX_FIX_AGE 5000         // Ignore data from GPS if older than this
unsigned long lastSend, lastDisplay, lastFix, lastRecv;
bool sending = false;

// 95% error radius at HDOP=1
#define GPS_BASE_ACCURACY 6.2  // m

#define ACCURACY_THRESHOLD 30  // m

// tinyGPS
#include <TinyGPS.h>

TinyGPS gps;

#define MAX_PAIRS 100

struct NameValueAddress {
  char name[50];
  int address;
};

int numAddrs = 0;

struct NameValueAddress names[MAX_PAIRS] = {
  { "unknown", 0 },
  { "DigitalMast", 16 },
  { "akula", 33 },
  { "artemis", 51 },
  { "athena", 32 },
  { "bbboomi", 60 },
  { "biscuit", 52 },
  { "boadie", 45 },
  { "boomi", 18 },
  { "candy", 50 },
  { "cranky", 4 },
  { "funky", 25 },
  { "goofy", 54 },
  { "honey", 58 },
  { "intrepid", 43 },
  { "joon", 44 },
  { "jupiter", 14 },
  { "kronos", 7 },
  { "ltlwing", 27 },
  { "mermaid", 64 },
  { "monaco", 41 },
  { "monitor", 59 },
  { "pegasus", 62 },
  { "ratchet", 30 },
  { "squeeze", 55 },
  { "supersexy", 63 },
  { "thymol", 19 },
  { "vega", 22 }
};

char * lookupName(struct NameValueAddress names[], int address) {
  for (int i = 0; i < numAddrs; i++) {
    if (names[i].address == address) {
      return names[i].name;
    }
  }
  return names[0].name; // unknown
}



void PrintHex8(uint8_t* data, uint8_t length)  // prints 8-bit data in hex with leading zeroes
{
  for (int i = 0; i < length; i++) {
    if (data[i] < 0x10) { Serial.print("0"); }
    Serial.print(data[i], HEX);
    //Serial.print(" ");
  }
}


void PrintAddr(char * addr)  // prints 8-bit data in hex with leading zeroes
{
  char format[128];
  sprintf(format, "%16s", addr);
  Serial.print(format);
}

void PrintDec8(int num, uint8_t precision) {
  char tmp[16];
  char format[128];

  sprintf(format, "%%0%dd", precision);
  sprintf(tmp, format, num);
  Serial.print(tmp);
}

void PrintTime(long time, uint8_t precision) {
  char tmp[16];
  char format[128];

  sprintf(format, "%%0%dd", precision);
  sprintf(tmp, format, time);
  Serial.print(tmp);
}

void printRepeatedBy(int8_t rpt) {
  Serial.print("                                              Repeated by: ");
  bool first = true;
  for (int i = 0; i < 8; i++) {
    if (REPEATED_BY(rpt, i)) {
      if (!first) {
        Serial.print(",");
      }
      first = false;
      Serial.print(i);
    }
  }
}

long startTime = micros();


void setup() {
  numAddrs = sizeof(names) / sizeof(NameValueAddress);

  Serial.begin(115200);
  Serial.println("Sniffer booting...");


  pinMode(RFM95_RST, OUTPUT);
  digitalWrite(RFM95_RST, HIGH);

  pinMode(LED, OUTPUT);


  // manual reset
  digitalWrite(RFM95_RST, LOW);
  delay(10);
  digitalWrite(RFM95_RST, HIGH);
  delay(10);

  // for ttgo
  //SPI.begin(SCK,MISO,MOSI,SS);

  while (!rf95.init()) {
    Serial.println("LoRa radio init failed");
    while (1)
      ;
  }

  // Defaults after init are 434.0MHz, modulation GFSK_Rb250Fd250, +13dbM
  if (!rf95.setFrequency(RF95_FREQ)) {
    Serial.println("setFrequency failed");
    while (1)
      ;
  }

  //rf95.setModemConfig(RH_RF95::Bw125Cr48Sf4096);
  RH_RF95::ModemConfig modem_config = {
    0x78,  // Reg 0x1D: BW=125kHz, Coding=4/8, Header=explicit
    0xC4,  // Reg 0x1E: Spread=4096chips/symbol, CRC=enable
    0x08   // Reg 0x26: LowDataRate=On, Agc=Off
  };
  //rf95.setModemRegisters(&modem_config);
  rf95.printRegisters();


  // Defaults after init are 434.0MHz, 13dBm, Bw = 125 kHz, Cr = 4/5, Sf = 128chips/symbol, CRC on

  // The default transmitter power is 13dBm, using PA_BOOST.
  // If you are using RFM95/96/97/98 modules which uses the PA_BOOST transmitter pin, then
  // you can set transmitter powers from 5 to 23 dBm:
  rf95.setTxPower(23, false);


  Serial.println("Sniffer started...");
}

#define MAGIC_NUMBER_LEN 2
uint8_t MAGIC_NUMBER[MAGIC_NUMBER_LEN] = { 0xbb, 0x01 };
uint8_t MAGIC_NUMBER_REP[MAGIC_NUMBER_LEN] = { 0xbb, 0x02 };
uint8_t MAGIC_NUMBER_SYNC_CLIENT[MAGIC_NUMBER_LEN] = { 0xbb, 0x03 };
uint8_t MAGIC_NUMBER_SYNC_SERVER[MAGIC_NUMBER_LEN] = { 0xbb, 0x04 };
uint8_t MAGIC_NUMBER_TRACKER[MAGIC_NUMBER_LEN] = { 0x02, 0xcb };

//String timeStr = "";
//uint8_t buf[RH_RF95_MAX_MESSAGE_LEN];
uint8_t buf[64];
int lastRSSI;

// production - burning man
#define MAN_LAT 40786590
#define MAN_LON -119206562

#define PLAYA_ELEV 1190.  // m
#define SCALE 1.


// lat/lon are stored as signed 32-bit ints as millionths of a degree (-123.45678 => -123,456,780)
int32_t myLat;
int32_t myLon;
float myElev = 0;
float myHAcc;
bool amIAccurate;
int32_t theirLat = 99;
int32_t theirLon = 99;
int16_t theirAddress;
int8_t repeatedBy;
int8_t theirBatt;
float theirElev;
bool areTheyAccurate;
int8_t myTtl = 1;
static int16_t myAddress = 300;
static String unknown = {"unknown"};

void processRecv(int len) {

  int addr = 0;
  PrintTime((micros() - startTime) / 1000, 10);
  startTime = micros();
  if (len > 3) {
    addr = buf[2];
  }

  char * name = lookupName(names, addr);
  PrintAddr(name);
  
  Serial.print(" len ");
  PrintDec8(len, 3);
  Serial.print(" RSSI:");
  PrintDec8(lastRSSI, 3);
  Serial.print(": ");
  for (int i = 0; i < len; i++) {
    Serial.print(" ");
    PrintHex8(&buf[i], 1);
  }
  Serial.println("");
  processGPS();
  processRepeaterStats();
}

void processGPS() {
  for (int i = 0; i < MAGIC_NUMBER_LEN; i++) {
    if (MAGIC_NUMBER[i] != buf[i]) {
      return;
    }
  }

  void* p = buf + MAGIC_NUMBER_LEN;
  theirAddress = *(int16_t*)p;
  p = (int16_t*)p + 1;
  repeatedBy = *(int8_t*)p;
  p = (int8_t*)p + 1;
  theirLat = *(int32_t*)p;
  p = (int32_t*)p + 1;
  theirLon = *(int32_t*)p;
  p = (int32_t*)p + 1;
  theirBatt = *(int8_t*)p;
  p = (int32_t*)p + 1;
  areTheyAccurate = *(uint8_t*)p;
  lastRecv = millis();

  printRepeatedBy(repeatedBy);
  Serial.print("    RSSI ");
  Serial.print(lastRSSI);
  Serial.print(", addr ");
  Serial.print(theirAddress);
  Serial.print(", batt ");
  Serial.print(theirBatt);
  Serial.print(", lat ");
  Serial.print(theirLat);
  Serial.print("");
  Serial.print(", lon ");
  Serial.print(theirLon);
  Serial.print("");
  //Serial.print("    Rec packet playa  ");
  //Serial.print(fmtPlayaStr(theirLat, theirLon, true));
  Serial.println("");
}

void processGPSTracker() {
  for (int i = 0; i < MAGIC_NUMBER_LEN; i++) {
    if (MAGIC_NUMBER_TRACKER[i] != buf[i]) {
      return;
    }
  }

  void* p = buf + MAGIC_NUMBER_LEN;
  theirLat = *(int32_t*)p;
  p = (int32_t*)p + 1;
  theirLon = *(int32_t*)p;
  p = (int32_t*)p + 1;
  areTheyAccurate = *(uint8_t*)p;
  lastRecv = millis();


  Serial.println();
  Serial.print("    Tracker RSSI ");
  Serial.print(lastRSSI);
  Serial.print(", lat ");
  Serial.print(theirLat);
  Serial.print(", lon ");
  Serial.print(theirLon);
  Serial.print(", playa  ");
  Serial.print(fmtPlayaStr(theirLat, theirLon, true));
  Serial.println("");
}

void processRepeaterStats() {
  for (int i = 0; i < MAGIC_NUMBER_LEN; i++) {
    if (MAGIC_NUMBER_REP[i] != buf[i]) {
      return;
    }
  }

  void* p = buf + MAGIC_NUMBER_LEN;
  int address = *(uint16_t*)p - 32768;
  p = (int16_t*)p + 1;
  int battery3 = *(int8_t*)p;
  p = (int8_t*)p + 1;
  int packetsForwarded = *(uint32_t*)p;
  p = (int32_t*)p + 1;
  int packetsIgnored = *(uint32_t*)p;


  Serial.print("    Repeater Address ");
  Serial.print(address);
  Serial.print(", Battery Level ");
  //Serial.print(3 + ((float)battery3 / 100) );
  Serial.print(battery3);
  Serial.print(", Repeater Packets Forwarded ");
  Serial.print(packetsForwarded);
  Serial.print(", Repeater Packets Ignored ");
  Serial.print(packetsIgnored);
  Serial.println();
}


String fmtPlayaStr(int32_t lat, int32_t lon, bool accurate) {
  if (lat == 0 && lon == 0) {
    return "404 cosmos not found";
  } else {
    return playaStr(lat, lon, accurate);
  }
}

//void setFixTime() {
//  int year;
//  byte month, day, hour, minute, second, hundredths;
//  unsigned long age;
//  gps.crack_datetime(&year, &month, &day, &hour, &minute, &second, &hundredths, &age);
//  timeStr =  String(hour) + ":" + String(minute) + ":" +  String(second) + "/" + String(age) + "ms";
//}

///// PLAYA COORDINATES CODE /////

#define DEG_PER_RAD (180. / 3.1415926535)
#define CLOCK_MINUTES (12 * 60)
#define METERS_PER_DEGREE (40030230. / 360.)
// Direction of north in clock units
#define NORTH 10.5                       // hours
#define NUM_RINGS 13                     // Esplanade through L
#define ESPLANADE_RADIUS (2500 * .3048)  // m
#define FIRST_BLOCK_DEPTH (440 * .3048)  // m
#define BLOCK_DEPTH (240 * .3048)        // m
// How far in from Esplanade to show distance relative to Esplanade rather than the man
#define ESPLANADE_INNER_BUFFER (250 * .3048)  // m
// Radial size on either side of 12 w/ no city streets
#define RADIAL_GAP 2.  // hours
// How far radially from edge of city to show distance relative to city streets
#define RADIAL_BUFFER .25  // hours

//// overrides for afrikaburn
//#define NORTH 3.3333  // make 6ish approx line up with bearing 80 deg
//#define NUM_RINGS 0  // only give distance relative to clan


// 0=man, 1=espl, 2=A, 3=B, ...
float ringRadius(int n) {
  if (n == 0) {
    return 0;
  } else if (n == 1) {
    return ESPLANADE_RADIUS;
  } else if (n == 2) {
    return ESPLANADE_RADIUS + FIRST_BLOCK_DEPTH;
  } else {
    return ESPLANADE_RADIUS + FIRST_BLOCK_DEPTH + (n - 2) * BLOCK_DEPTH;
  }
}

// Distance inward from ring 'n' to show distance relative to n vs. n-1
float ringInnerBuffer(int n) {
  if (n == 0) {
    return 0;
  } else if (n == 1) {
    return ESPLANADE_INNER_BUFFER;
  } else if (n == 2) {
    return .5 * FIRST_BLOCK_DEPTH;
  } else {
    return .5 * BLOCK_DEPTH;
  }
}

int getReferenceRing(float dist) {
  for (int n = NUM_RINGS; n > 0; n--) {
    Serial.println(n + ":" + String(ringRadius(n)) + " " + String(ringInnerBuffer(n)));
    if (ringRadius(n) - ringInnerBuffer(n) <= dist) {

      return n;
    }
  }
  return 0;
}

String getRefDisp(int n) {
  if (n == 0) {
    return ")(";
  } else if (n == 1) {
    return "Espl";
  } else {
    return String(char(int('A') + n - 2));
  }
}

String playaStr(int32_t lat, int32_t lon, bool accurate) {
  // Safe conversion to float w/o precision loss.
  float dlat = 1e-6 * (lat - MAN_LAT);
  float dlon = 1e-6 * (lon - MAN_LON);

  float m_dx = dlon * METERS_PER_DEGREE * cos(1e-6 * MAN_LAT / DEG_PER_RAD);
  float m_dy = dlat * METERS_PER_DEGREE;

  float dist = SCALE * sqrt(m_dx * m_dx + m_dy * m_dy);
  float bearing = DEG_PER_RAD * atan2(m_dx, m_dy);

  float clock_hours = (bearing / 360. * 12. + NORTH);
  int clock_minutes = (int)(clock_hours * 60 + .5);
  // Force into the range [0, CLOCK_MINUTES)
  clock_minutes = ((clock_minutes % CLOCK_MINUTES) + CLOCK_MINUTES) % CLOCK_MINUTES;

  int hour = clock_minutes / 60;
  int minute = clock_minutes % 60;
  String clock_disp = String(hour) + ":" + (minute < 10 ? "0" : "") + String(minute);

  int refRing;
  if (6 - abs(clock_minutes / 60. - 6) < RADIAL_GAP - RADIAL_BUFFER) {
    refRing = 0;
  } else {
    refRing = getReferenceRing(dist);
  }
  float refDelta = dist - ringRadius(refRing);
  long refDeltaRounded = (long)(refDelta + .5);

  return clock_disp + " & " + getRefDisp(refRing) + (refDeltaRounded >= 0 ? "+" : "-") + String(refDeltaRounded < 0 ? -refDeltaRounded : refDeltaRounded) + "m" + (accurate ? "" : "-ish");
}

static void print_date(TinyGPS& gps) {
  int year;
  byte month, day, hour, minute, second, hundredths;
  unsigned long age;
  gps.crack_datetime(&year, &month, &day, &hour, &minute, &second, &hundredths, &age);
  if (age == TinyGPS::GPS_INVALID_AGE)
    Serial.print("********** ******** ");
  else {
    char sz[32];
    sprintf(sz, "%02d/%02d/%02d %02d:%02d:%02d ",
            month, day, year, hour, minute, second);
    Serial.print(sz);
  }
  print_int(age, TinyGPS::GPS_INVALID_AGE, 5);
}

static void print_int(unsigned long val, unsigned long invalid, int len) {
  char sz[32];
  if (val == invalid)
    strcpy(sz, "*******");
  else
    sprintf(sz, "%ld", val);
  sz[len] = 0;
  for (int i = strlen(sz); i < len; ++i)
    sz[i] = ' ';
  if (len > 0)
    sz[len - 1] = ' ';
  Serial.print(sz);
}

void loop() {
  if (rf95.available()) {
    digitalWrite(LED, HIGH);
    //Serial.println("Received packet RSSI");
    uint8_t len = sizeof(buf);
    if (rf95.recv(buf, &len)) {
      lastRSSI = rf95.lastRssi();
      processRecv(len);
    }
    digitalWrite(LED, LOW);
  }
}
