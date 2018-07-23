#include <SPI.h>
#include <RH_RF95.h>

#define REPEATER_NUMBER 3

#define REPEATER_BIT(rbit) (1 << rbit)
#define REPEATED_BY(rpt, by) ((rpt & REPEATER_BIT(by)) > 0)
#define REPEATED_BY_ME(rptme) REPEATED_BY(rptme, REPEATER_NUMBER)
#define SET_REPEATED_BY(rpt, by) (rpt |= REPEATER_BIT(by))
#define SET_REPEATED_BY_ME(rptme) (SET_REPEATED_BY(rptme, REPEATER_NUMBER))

int16_t myAddress = 32768 + (REPEATER_BIT(REPEATER_NUMBER));

#define LED 13

//Teensy pin 7, Feather pin 9
//#define VBATPIN A7
#define VBATPIN 9
   
#define PRINT 1

/* for teensy 

#define RFM95_CS 10
#define RFM95_RST 4
#define RFM95_INT 3
*/

/* for feather m0 */

#define RFM95_CS 8
#define RFM95_RST 4
#define RFM95_INT 3


// Change to 434.0 or other frequency, must match RX's freq!
#define RF95_FREQ 915.0

// Singleton instance of the radio driver
RH_RF95 rf95(RFM95_CS, RFM95_INT);
//RH_RF95 rf95; //moteino

long startTime = micros();

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

  //rf95.setModemConfig(RH_RF95::Bw125Cr48Sf4096); 
  //RH_RF95::ModemConfig modem_config = {
  //0x78, // Reg 0x1D: BW=125kHz, Coding=4/8, Header=explicit
  //0xC4, // Reg 0x1E: Spread=4096chips/symbol, CRC=enable
  //0x08  // Reg 0x26: LowDataRate=On, Agc=Off
  //};
  //rf95.setModemRegisters(&modem_config);

  //rf95.setModemConfig(MODEM_CONFIG_TABLE[Bw125Cr48Sf4096]);

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

  Serial.print("Repeater ");
  Serial.print(REPEATER_NUMBER);
  Serial.print(", address ");
  Serial.println(myAddress);
  
}

// timing
#define TRANSMIT_INTERVAL 5000      // interval between sending updates
unsigned long lastStats; 

#define MAGIC_NUMBER_LEN 2
uint8_t MAGIC_NUMBER[MAGIC_NUMBER_LEN] = {0xbb, 0x01}; // BB Location
uint8_t MAGIC_NUMBER_REPEATER[MAGIC_NUMBER_LEN] = {0xbb, 0x02}; // Repeater stats

uint8_t buf[RH_RF95_MAX_MESSAGE_LEN];
int lastRSSI;

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
float theirElev;  
bool areTheyAccurate;
int8_t myTtl = 1;
int32_t packetsForwarded = 0;
int32_t packetsIgnored = 0;
uint8_t myBattery = 0;


void PrintHex8(uint8_t *data, uint8_t length) // prints 8-bit data in hex with leading zeroes
{
       for (int i=0; i<length; i++) { 
         if (data[i]<0x10) {Serial.print("0");} 
         Serial.print(data[i],HEX); 
         //Serial.print(" "); 
       }
}

boolean processRecv(int len) {
  for (int i = 0; i < MAGIC_NUMBER_LEN; i++) {
    if (MAGIC_NUMBER[i] != buf[i]) {
      return false;
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
  theirElev = *(int32_t*)p;
  p = (int32_t*)p + 1;
  areTheyAccurate = *(uint8_t*)p;


#ifdef PRINT
  Serial.print("Rec packet len (");
  Serial.print(len);
  Serial.print(" addr:");
  Serial.print(theirAddress);
  Serial.print(": ");
  Serial.print(" RSSI:");
  Serial.print(lastRSSI);
  printRepeatedBy(repeatedBy);
  Serial.print(": ");
  for (int i = 0; i < len; i++) {
    Serial.print(" ");
    PrintHex8(&buf[i], 1);
  }
  Serial.println("");
#endif

  // Repeat this packet?
  if (REPEATED_BY_ME(repeatedBy)) {
    return false;
  } else {
    return true;
  }

}

void printRepeatedBy(int8_t rpt) {
  Serial.print(" Repeated by: ");
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

void repeatPacket(uint8_t len) {
#ifdef PRINT
  Serial.println("Forwaring...");
#endif
  // Set repeated-by bit
  void* p = buf + MAGIC_NUMBER_LEN + sizeof(int16_t);
  uint8_t rptBy = repeatedBy;
  SET_REPEATED_BY_ME(rptBy);
  *(int8_t*)p = rptBy;
  rf95.send((uint8_t *)buf, len);
  rf95.waitPacketSent();
}

int batteryLevel() {
    float measuredvbat = analogRead(VBATPIN);
    measuredvbat *= 2;    // we divided by 2, so multiply back
    measuredvbat *= 3.3;  // Multiply by 3.3V, our reference voltage
    measuredvbat /= 1024; // convert to voltage
    int batteryPct = min(100, (measuredvbat - 3.6) * 166); // 3.6v - 4.2v
    Serial.print("VBat: " ); Serial.println(measuredvbat);
    Serial.print("VBatHex: " ); Serial.println(myBattery);
    Serial.print("VBatPct: "); Serial.println(batteryPct);
    //myBattery = (measuredvbat - 3) * 100;
    return(batteryPct);

}

void transmitStats() {
  
  uint8_t len = MAGIC_NUMBER_LEN + 3 * sizeof(int32_t) + sizeof(int8_t) + 1;
  uint8_t radiopacket[len];
  for (int i = 0; i < MAGIC_NUMBER_LEN; i++) {
    radiopacket[i] = MAGIC_NUMBER_REPEATER[i];
  }
  void* p = radiopacket + MAGIC_NUMBER_LEN;
  *(int16_t*)p = myAddress;
  p = (int16_t*)p + 1;
  *(int8_t*)p = myBattery;
  p = (int8_t*)p + 1;
  *(int32_t*)p = packetsForwarded;
  p = (int32_t*)p + 1;
  *(int32_t*)p = packetsIgnored;
  radiopacket[len - 1] = '\0';

  rf95.send((uint8_t *)radiopacket, len);
  rf95.waitPacketSent();
  lastStats = millis();
}

void loop() {

  if (rf95.available()) {
    digitalWrite(LED, HIGH);
    uint8_t len = sizeof(buf);
    if (rf95.recv(buf, &len)) {
      lastRSSI = rf95.lastRssi();
      // Repeat packet if it's a location service packet
      if (processRecv(len)) {
          // random delay up to 1 second to avoid stepping on each other
          delay(random(1000));
          repeatPacket(len);
          packetsForwarded++;
      } else {
        packetsIgnored++;
      }
    }
    digitalWrite(LED, LOW);
  }

  long sinceLastTransmit = millis() - lastStats;
  if (sinceLastTransmit < 0 || sinceLastTransmit > TRANSMIT_INTERVAL) {

    myBattery = batteryLevel();
    transmitStats();
  }

  
}





