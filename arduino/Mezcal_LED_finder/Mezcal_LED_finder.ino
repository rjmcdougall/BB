
#include <OctoWS2811.h>


const int ledsPerStrip = 600;

DMAMEM int displayMemory[ledsPerStrip*6];
int drawingMemory[ledsPerStrip*6];

const int config = WS2811_GRB | WS2811_800kHz;

OctoWS2811 leds(ledsPerStrip, displayMemory, drawingMemory, config);

#define RED    0xFF0000
#define GREEN  0x00FF00
#define BLUE   0x0000FF
#define YELLOW 0xFFFF00
#define PINK   0xFF1088   
#define ORANGE 0xE05800
#define WHITE  0xFFFFFF

// constants won't change. They're used here to set pin numbers:
const int buttonPin = 19;         // the number of the pushbutton pin
const int ledPin = LED_BUILTIN;  // the number of the LED pin
  // LED_BUILTIN is set to the correct LED pin independent of which board is used

// Variables will change:
int ledState = HIGH;         // the current state of the output pin
int buttonState;             // the current reading from the input pin
int lastButtonState = LOW;   // the previous reading from the input pin

// the following variables are unsigned longs because the time, measured in
// milliseconds, will quickly become a bigger number than can be stored in an int.
unsigned long lastDebounceTime = 0;  // the last time the output pin was toggled
unsigned long debounceDelay = 10;    // the debounce time; increase if the output flickers

int led_indicator = 0;
int led_counter = 0;
int reading;

void setup() {
  Serial.begin(9600);
  
  pinMode(buttonPin, INPUT);
  //pinMode(ledPin, OUTPUT);
  reading = digitalRead(buttonPin);


  // set initial LED state
  //digitalWrite(ledPin, ledState);

  leds.begin();
  delayMicroseconds(5000000);
  Serial.println("Start");

}

int cnt = 0;

void loop() {
  // read the state of the switch into a local variable:
  reading = digitalRead(buttonPin);

  // check to see if you just pressed the button
  // (i.e. the input went from LOW to HIGH), and you've waited long enough
  // since the last press to ignore any noise:

  // If the switch changed, due to noise or pressing:
  if (reading != lastButtonState) {
    // reset the debouncing timer
    lastDebounceTime = millis();
  }
   if ((millis() - lastDebounceTime) > debounceDelay) {
    
    // whatever the reading is at, it's been there for longer than the debounce
    // delay, so take it as the actual current state:

    // if the button state has changed:
    if (reading != buttonState) {
      buttonState = reading;

      // only toggle the LED if the new button state is HIGH
      if (buttonState == HIGH) {
        ledState = !ledState;
        Serial.println(led_indicator);
      }
    }
  }

  // save the reading. Next time through the loop, it'll be the lastButtonState:
  lastButtonState = reading;

  // set the LED:
  //digitalWrite(ledPin, ledState);
  
  for (int i=0; i < leds.numPixels(); i++) {
    if (i == led_counter) {
      leds.setPixel(i, BLUE);
    } else {
      leds.setPixel(i, 0x100000);
    }

  }

  
  leds.show();
  delayMicroseconds(1000);

  if ((cnt % 4) == 0) {
    led_counter++;
    led_indicator++;
  }

  if (led_indicator >= ledsPerStrip) {
    led_indicator = 0;
  }
  
  if (led_counter >= (4 * ledsPerStrip)) {
    led_counter = 0;
  }
  //Serial.print(led_counter);
  //Serial.print("/");
  //Serial.println(led_indicator);
  cnt++;
}
