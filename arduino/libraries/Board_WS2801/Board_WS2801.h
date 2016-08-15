#if (ARDUINO >= 100)
#include <Arduino.h>
#else
#include <WProgram.h>
#include <pins_arduino.h>
#endif

#include <Adafruit_GFX.h>
#include "Print.h"


// Not all LED pixels are RGB order; 36mm type expects GRB data.
// Optional flag to constructors indicates data order (default if
// unspecified is RGB).  As long as setPixelColor/getPixelColor are
// used, other code can always treat 'packed' colors as RGB; the
// library will handle any required translation internally.
#define WS2801_RGB (uint8_t)0
#define WS2801_GRB (uint8_t)1

class Board_WS2801 : public Adafruit_GFX {

  public:

    // Configurable pins:
/*
    Board_WS2801(uint16_t n, uint8_t dpin, uint8_t cpin, uint8_t order=WS2801_RGB, boolean sl = false);
    Board_WS2801(uint16_t x, uint16_t y, uint8_t dpin, uint8_t cpin, uint8_t order=WS2801_RGB, boolean sl = false);
    // Use SPI hardware; specific pins only:
    Board_WS2801(uint16_t n, uint8_t order=WS2801_RGB, boolean sl = false);
*/
    Board_WS2801(uint16_t x, uint16_t y, uint8_t order=WS2801_RGB, boolean sl = false);
    // Empty constructor; init pins/strand length/data order later:
    Board_WS2801();
    // Release memory (as needed):
    ~Board_WS2801();

    // GFX Library
		void drawPixel(int16_t x, int16_t y, uint16_t color);
		uint16_t Color565(uint8_t r, uint8_t g, uint8_t b);
	  

    void
      begin(void),
      show(void),
      setPixelColor(uint16_t n, uint8_t r, uint8_t g, uint8_t b),
      setPixelColor(uint16_t n, uint32_t c),
      setPixelColor(uint16_t x, uint16_t y, uint8_t r, uint8_t g, uint8_t b),
      setPixelColor(uint16_t x, uint16_t y, uint32_t c),
      updatePins(uint8_t dpin, uint8_t cpin), // Change pins, configurable
      updatePins(void), // Change pins, hardware SPI
      updateLength(uint16_t n), // Change strand length
      updateOrder(uint8_t order), // Change data order
      enableSidelights(boolean haslights), // Has side lights
      print(char *string, uint8_t x, uint8_t y, uint8_t size);


    uint16_t
      numPixels(void),
      *pixel_translate;
    uint32_t
      getPixelColor(uint16_t n),
      getPixelColor(uint16_t x, uint16_t y);

  private:

    uint16_t
      numvirtLEDs,
      numboardLEDs,
      width,     // used with matrix mode
      height;    // used with matrix mode
    uint8_t
      *pixels,   // Holds color values for each LED (3 bytes each)
      rgb_order, // Color order; RGB vs GRB (or others, if needed in future)
      clkpin    , datapin,     // Clock & data pin numbers
      clkpinmask, datapinmask; // Clock & data PORT bitmasks
    volatile uint8_t
      *clkport  , *dataport;   // Clock & data PORT registers
    boolean
      hardwareSPI, // If 'true', using hardware SPI
      begun;       // If 'true', begin() method was previously invoked
    boolean
      hasSidelights;       // If 'true', extra lights on side
    void
      alloc(uint16_t n),
      translationArray(uint16_t n),
      startSPI(void);
    uint32_t
      BoardPixel(uint32_t pixel);
};
