// Libraries for GPS
#include <TinyGPS++.h>

#include <RadioLib.h>

#define REPEATER_BIT(rbit) (1 << rbit)
#define REPEATED_BY(rpt, by) ((rpt & REPEATER_BIT(by)) > 0)

// Libraries for E-paper Display
#include <GxEPD.h>
#include <GxDEPG0150BN/GxDEPG0150BN.h> // 1.54" b/w
#include GxEPD_BitmapExamples
#include <Fonts/FreeMonoBold9pt7b.h>
#include <Fonts/FreeMonoBold12pt7b.h>
#include <Fonts/FreeMonoBold18pt7b.h>
#include <Fonts/FreeMonoBold24pt7b.h>
#include <GxIO/GxIO_SPI/GxIO_SPI.h>
#include <GxIO/GxIO.h>

TinyGPSPlus MyGPS;

byte buf[256];

void processRecv(int len);
void PrintHex8(uint8_t *data, uint8_t length);
void processRepeaterStats(char *name);
void processGPS(char *name);
int lastRSSI;

int32_t theirLat = 99;
int32_t theirLon = 99;
int16_t theirAddress;
int8_t repeatedBy;
int8_t theirBatt;
float theirElev;
bool areTheyAccurate;

// General setting
int TX_OUTPUT_POWER = 20; // Power maxi 20 dBm

#ifndef _PINNUM
#define _PINNUM(port, pin) ((port) * 32 + (pin))
#endif

// Pinout ePaper Display
#define ePaper_Miso _PINNUM(1, 6)
#define ePaper_Mosi _PINNUM(0, 29)
#define ePaper_Sclk _PINNUM(0, 31)
#define ePaper_Cs _PINNUM(0, 30)
#define ePaper_Dc _PINNUM(0, 28)
#define ePaper_Rst _PINNUM(0, 2)
#define ePaper_Busy _PINNUM(0, 3)
#define ePaper_Backlight _PINNUM(1, 11)

// Pinout Lora Radio Module
#define RADIO_MISO_PIN _PINNUM(0, 23)
#define RADIO_MOSI_PIN _PINNUM(0, 22)
#define RADIO_SCLK_PIN _PINNUM(0, 19)
#define RADIO_CS_PIN _PINNUM(0, 24)
#define RADIO_RST_PIN _PINNUM(0, 25)
#define RADIO_DI0_PIN _PINNUM(0, 22)
#define RADIO_DIO1_PIN _PINNUM(0, 20)
#define RADIO_DIO2_PIN //_PINNUM(0,3)
#define RADIO_DIO3_PIN _PINNUM(0, 21)
#define RADIO_DIO4_PIN //_PINNUM(0,3)
#define RADIO_DIO5_PIN //_PINNUM(0,3)
#define RADIO_BUSY_PIN _PINNUM(0, 17)

// Pinout Flash Chip
#define Flash_Cs _PINNUM(1, 15)
#define Flash_Miso _PINNUM(1, 13)
#define Flash_Mosi _PINNUM(1, 12)
#define Flash_Sclk _PINNUM(1, 14)

#define Touch_Pin _PINNUM(0, 11)
#define Adc_Pin _PINNUM(0, 4)

#define I2C_SDA _PINNUM(0, 26)
#define I2C_SCL _PINNUM(0, 27)

#define RTC_Int_Pin _PINNUM(0, 16)

// Pinout GPS Module
#define GPS_RX_PIN _PINNUM(1, 9)
#define GPS_TX_PIN _PINNUM(1, 8)
#define Gps_Wakeup_Pin _PINNUM(1, 2)
#define Gps_Reset_Pin _PINNUM(1, 5)
#define Gps_pps_Pin _PINNUM(1, 4)

#define UserButton_Pin _PINNUM(1, 10)

#define Power_Enable_Pin _PINNUM(0, 13)
#define Power_On_Pin _PINNUM(0, 12)

#define GreenLed_Pin _PINNUM(1, 1)
#define RedLed_Pin _PINNUM(1, 3)
#define BlueLed_Pin _PINNUM(0, 14)

#define SerialMon Serial
#define SerialGPS Serial2

#define MONITOR_SPEED 115200
#define GPS_BAUD_RATE 9600

#define FREQ 915000000

uint32_t blinkMillis = 0;
uint32_t last = 0;

SPIClass dispPort(NRF_SPIM2, ePaper_Miso, ePaper_Sclk, ePaper_Mosi);
SPIClass rfPort(NRF_SPIM3, RADIO_MISO_PIN, RADIO_SCLK_PIN, RADIO_MOSI_PIN);
SPISettings spiSettings;
GxIO_Class io(dispPort, ePaper_Cs, ePaper_Dc, ePaper_Rst);
GxEPD_Class display(io, ePaper_Rst, ePaper_Busy);

SX1262 radio = nullptr;

void setupLora();
void setupDisplay();
bool setupGPS();
void loopGPS();
void boardInit();
void LilyGo_logo();
void Veille_logo();
void enableBacklight(bool en);

bool operationDone = true;

#define MAX_PAIRS 100

struct NameValueAddress
{
    char name[50];
    int address;
};

int numAddrs = 0;

struct NameValueAddress names[MAX_PAIRS] = {
    {"unknown", 0},
    {"DigitalMast", 16},
    {"akula", 33},
    {"artemis", 51},
    {"athena", 32},
    {"bbboomi", 60},
    {"biscuit", 52},
    {"boadie", 45},
    {"boomi", 18},
    {"candy", 50},
    {"cranky", 4},
    {"funky", 25},
    {"goofy", 54},
    {"honey", 58},
    {"intrepid", 43},
    {"joon", 44},
    {"jupiter", 14},
    {"kronos", 7},
    {"ltlwing", 27},
    {"mermaid", 64},
    {"monaco", 41},
    {"monitor", 59},
    {"pegasus", 62},
    {"ratchet", 30},
    {"squeeze", 55},
    {"supersexy", 63},
    {"thymol", 19},
    {"vega", 22}};

char addrname[16];
char *lookupName(struct NameValueAddress names[], int address)
{
    for (int i = 0; i < numAddrs; i++)
    {
        if (names[i].address == address)
        {
            return names[i].name;
        }
    }
    sprintf(addrname, "%03d ", address);
    // return names[0].name; // unknown
    return addrname; // unknown
}

void setFlag(void)
{
    // we sent or received a packet, set the flag
    operationDone = true;
}

void setup()
{
    numAddrs = sizeof(names) / sizeof(NameValueAddress);
    Serial.begin(115200);
    delay(200);
    boardInit();
    delay(200);
    enableBacklight(true);

    delay(2000);
    enableBacklight(false);
    display.fillScreen(GxEPD_WHITE);
    display.setRotation(3);

    display.setCursor(0, 15);
    //display.print("burnerboard");
    display.update();
    //display.setCursor(0, 15);
}

bool disp;
bool Pwr_on;
bool Pwr_en;

byte packet[128];

void loop()
// void newPacket()
{
     if (true)
    //if (operationDone)
    {
        // reset flag
        operationDone = false;

        //String packet = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
        //packet.reserve(128);
        //int state = radio.readData(packet);
      int state = radio.receive(&packet[0], (size_t)32);
        // delay(10);

        if (state == RADIOLIB_ERR_NONE)
        {
            int len = radio.getPacketLength();
            // packet.toCharArray((char *)&buf[0], 64, 0);
            memcpy(&buf[0], &packet[4], len);

            // Packet received successfully
            Serial.print("Received packet! RSSI: ");
            Serial.print((lastRSSI = radio.getRSSI()));
            Serial.print(" dBm, SNR: ");
            Serial.print(radio.getSNR());
            Serial.print(" dB, Length: ");
            Serial.print(len);
            // print frequency error
            // of the last received packet
            Serial.print(F(" Frequency error:\t"));
            Serial.print(radio.getFrequencyError());
            Serial.print(F(" Hz"));

            Serial.println();

            // Print the packet data
            // Serial.print("Data:");
            int i;
            // for (i = 0; (i < 40) && radio.available(); i++)
            // for (int i = 0; i < len; i++)
            //{
            //     buf[i] = radio.read(false);
            // }
            // Serial.print(i);
            // Serial.print(" ");

            // radio.readData(str);
            //Serial.print("Data: ");
            // Serial.print(packet.length());
            // Serial.print(" ");
            // Serial.print(len);
            // Serial.print(" : ");
            //  Serial.println(str);
            //  memcpy(buf, (uint8_t *)packet.c_str() + 20, len);

            // memcpy(&buf[0], (uint8_t *)packet.c_str(), len);
            // packet.toCharArray((char *)&buf[0], 64, 0);
            // for (i = 0; i < len; i++) {
            //     buf[i] = packet[i];
            // }
            //for (i = 0; i < len + 1; i++)
            //{
                //Serial.print(" ");
                //PrintHex8(&buf[i], 1);
            //}
            //wwSerial.println("");

            processRecv(len);
        }
        else
        {
            // Handle reception errors...
        }
        // Serial.println();
        //  display.update();
    }

#ifdef NONE
    if (millis() - blinkMillis > 2500)
    {
        blinkMillis = millis();
        disp = true;
    }

    if ((digitalRead(UserButton_Pin) == LOW) && (Pwr_on == false))
    {
        // Alimentation périphériques
        digitalWrite(Power_On_Pin, HIGH);
        enableBacklight(true);
        display.fillScreen(GxEPD_WHITE);
        display.updateWindow(0, 0, GxEPD_WIDTH, GxEPD_HEIGHT, false);
        enableBacklight(false);
        // digitalWrite(GreenLed_Pin,HIGH);
        // digitalWrite(RedLed_Pin,LOW);
        Pwr_on = true;
        SerialMon.println("PWR ON");
        while (digitalRead(UserButton_Pin) == LOW)
            ;
    }
    else if ((digitalRead(UserButton_Pin) == LOW) && (Pwr_on == true))
    {
        digitalWrite(Power_On_Pin, LOW);
        // digitalWrite(GreenLed_Pin,LOW);
        // digitalWrite(RedLed_Pin,HIGH);
        Pwr_on = false;
        SerialMon.println("PWR OFF");
        while (digitalRead(UserButton_Pin) == LOW)
            ;
    }

#endif
}

void setupDisplay()
{
    dispPort.begin();
    display.init(/*115200*/); // enable diagnostic output on Serial
    display.setRotation(0);
    display.fillScreen(GxEPD_WHITE);
    display.setTextColor(GxEPD_BLACK);
    display.setFont(&FreeMonoBold9pt7b);
}

bool setupGPS()
{
    SerialMon.println("[GPS] Initializing ... ");
    SerialGPS.setPins(GPS_RX_PIN, GPS_TX_PIN);
    SerialMon.println("Pins ... OK");
    SerialGPS.begin(GPS_BAUD_RATE);
    SerialMon.println("Serial Port2  ... OK");

    pinMode(Gps_pps_Pin, INPUT);
    pinMode(Gps_Wakeup_Pin, OUTPUT);
    digitalWrite(Gps_Wakeup_Pin, HIGH);

    delay(10);
    pinMode(Gps_Reset_Pin, OUTPUT);
    digitalWrite(Gps_Reset_Pin, HIGH);
    delay(10);
    digitalWrite(Gps_Reset_Pin, LOW);
    delay(10);
    digitalWrite(Gps_Reset_Pin, HIGH);

    SerialMon.println("GPS reset ..... OK");

    SerialMon.println("GPS Started");
    return true;
}

void setupLora()
{
    SerialMon.println("[LoRa] Initializing ... ");
    rfPort.begin();
    radio = new Module(RADIO_CS_PIN, RADIO_DIO1_PIN, RADIO_RST_PIN, RADIO_BUSY_PIN, rfPort, spiSettings);

    delay(200);

    if (!radio.begin(915.0, 125.0, 7, 5, 0x12, TX_OUTPUT_POWER))
    {
        SerialMon.println(".......... OK");
        display.print("lora ok");
        display.update();
        delay(100);
    }
    else
    {
        SerialMon.println(".......... KO");
        display.print("lora bad");
        display.update();
        delay(1000);
    }
    // radio.setSpreadingFactor(4096);
    // radio.setCRC(true);
    // radio.setDio1Action(setFlag);
    //radio.setPacketReceivedAction(setFlag);

    // radio.setDio1Action(newPacket);

    int state = radio.startReceive();
    if (state == RADIOLIB_ERR_NONE)
    {
        Serial.println(F("success!"));
    }
    else
    {
        Serial.print(F("failed, code "));
        Serial.println(state);
        while (true)
        {
            delay(10);
        }
    }

    SerialMon.println("LoRa Initializing OK!");
}

void boardInit()
{

    SerialMon.begin(MONITOR_SPEED);
    SerialMon.println("Start\n");

    uint32_t reset_reason;
    sd_power_reset_reason_get(&reset_reason);
    SerialMon.print("sd_power_reset_reason_get:");
    SerialMon.println(reset_reason, HEX);

    pinMode(Power_Enable_Pin, OUTPUT);
    digitalWrite(Power_Enable_Pin, HIGH);
    Pwr_en = true;

    pinMode(Power_On_Pin, OUTPUT);
    digitalWrite(Power_On_Pin, HIGH);
    Pwr_on = true;

    pinMode(ePaper_Backlight, OUTPUT);

    pinMode(GreenLed_Pin, OUTPUT);
    pinMode(RedLed_Pin, OUTPUT);
    pinMode(BlueLed_Pin, OUTPUT);

    pinMode(UserButton_Pin, INPUT_PULLUP);
    pinMode(Touch_Pin, INPUT_PULLUP);

    setupDisplay();
    setupGPS();
    setupLora();

    display.update();
    delay(500);
}

void enableBacklight(bool en)
{
    digitalWrite(ePaper_Backlight, en);
}

#define MAGIC_NUMBER_LEN 2
uint8_t MAGIC_NUMBER[MAGIC_NUMBER_LEN] = {0xbb, 0x01};
uint8_t MAGIC_NUMBER_REP[MAGIC_NUMBER_LEN] = {0xbb, 0x02};
uint8_t MAGIC_NUMBER_SYNC_CLIENT[MAGIC_NUMBER_LEN] = {0xbb, 0x03};
uint8_t MAGIC_NUMBER_SYNC_SERVER[MAGIC_NUMBER_LEN] = {0xbb, 0x04};
uint8_t MAGIC_NUMBER_TRACKER[MAGIC_NUMBER_LEN] = {0x02, 0xcb};

// String timeStr = "";
// uint8_t buf[RH_RF95_MAX_MESSAGE_LEN];

void PrintHex8(uint8_t *data, uint8_t length) // prints 8-bit data in hex with leading zeroes
{
    for (int i = 0; i < length; i++)
    {
        if (data[i] < 0x10)
        {
            Serial.print("0");
        }
        Serial.print(data[i], HEX);
        // Serial.print(" ");
    }
}

void PrintAddr(char *addr) // prints 8-bit data in hex with leading zeroes
{
    char s[128];
    sprintf(s, "%16s", addr);
    Serial.print(s);
}

void PrintAddrDisp(char *addr) // prints 8-bit data in hex with leading zeroes
{
    char s[128];
    sprintf(s, "%8s ", addr);
    display.print(s);
}

void PrintDec8(int num, uint8_t precision)
{
    char tmp[16];
    char format[128];

    sprintf(format, "%%0%dd", precision);
    sprintf(tmp, format, num);
    Serial.print(tmp);
    //display.print(tmp);
}

void PrintDec8Disp(int num, uint8_t precision)
{
    char tmp[16];
    char format[128];

    sprintf(format, "%%0%dd", precision);
    sprintf(tmp, format, num);
    display.print(tmp);
}

void PrintTime(long time, uint8_t precision)
{
    char tmp[16];
    char format[128];

    sprintf(format, "%%0%dd", precision);
    sprintf(tmp, format, time);
    Serial.print(tmp);
}

void printRepeatedBy(int8_t rpt)
{
    Serial.print("                                              Repeated by: ");
    bool first = true;
    for (int i = 0; i < 8; i++)
    {
        if (REPEATED_BY(rpt, i))
        {
            if (!first)
            {
                Serial.print(",");
            }
            first = false;
            Serial.print(i);
        }
    }
}

void processRecv(int len)
{

    int addr = 0;
    if (len > 3)
    {
        addr = buf[2];
    }

    char *name = lookupName(names, addr);
    PrintAddr(name);

    Serial.print(" len ");
    PrintDec8(len, 3);
    Serial.print(" RSSI:");
    PrintDec8(lastRSSI, 3);
    Serial.print(": ");
    for (int i = 0; i < len; i++)
    {
         Serial.print(" ");
         PrintHex8(&buf[i], 1);
     }
    Serial.println("");
    processGPS(name);
    processRepeaterStats(name);
}

void processRepeaterStats(char *name)
{
    for (int i = 0; i < MAGIC_NUMBER_LEN; i++)
    {
        if (MAGIC_NUMBER_REP[i] != buf[i])
        {
            return;
        }
    }

    void *p = &buf[0] + MAGIC_NUMBER_LEN;
    int address = *(uint16_t *)p - 32768;
    p = (int16_t *)p + 1;
    int battery3 = *(int8_t *)p;
    p = (int8_t *)p + 1;
    int packetsForwarded = *(uint32_t *)p;
    p = (int32_t *)p + 1;
    int packetsIgnored = *(uint32_t *)p;

    char rptname[32];
    sprintf(rptname, "rpt-%d", address);

    Serial.print("    Repeater Address ");
    Serial.print(rptname);
    Serial.print(", Battery Level ");
    // Serial.print(3 + ((float)battery3 / 100) );
    Serial.print(battery3);
    Serial.print(", Repeater Packets Forwarded ");
    Serial.print(packetsForwarded);
    Serial.print(", Repeater Packets Ignored ");
    Serial.print(packetsIgnored);
    Serial.println();

    PrintAddrDisp(rptname);
    display.print(lastRSSI);
    display.print("db ");
    display.print(battery3);
    display.println("%");
    display.update();
}

void processGPS(char *name)
{
    for (int i = 0; i < MAGIC_NUMBER_LEN; i++)
    {
        if (MAGIC_NUMBER[i] != buf[i])
        {
            return;
        }
    }

    void *p = &buf[0] + MAGIC_NUMBER_LEN;
    theirAddress = *(int16_t *)p;
    p = (int16_t *)p + 1;
    repeatedBy = *(int8_t *)p;
    p = (int8_t *)p + 1;
    theirLat = *(int32_t *)p;
    p = (int32_t *)p + 1;
    theirLon = *(int32_t *)p;
    p = (int32_t *)p + 1;
    theirBatt = *(int8_t *)p;
    if (theirBatt < 0)
    {
        theirBatt = 0;
    }
    p = (int32_t *)p + 1;
    areTheyAccurate = *(uint8_t *)p;

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
    // Serial.print("    Rec packet playa  ");
    // Serial.print(fmtPlayaStr(theirLat, theirLon, true));
    Serial.println("");

    PrintAddrDisp(name);
    display.print(lastRSSI);
    display.print("db ");
    display.print(theirBatt);
    display.println("%");
    display.update();
}