#include "SPI.h"
#include "Board_WS2801.h"
#include <Adafruit_GFX.h>
#include "Print.h"

#define TEENSY 1


/*****************************************************************************/

// Burner Board WS2801-based RGB LED Modules in a strand
// Original Written by Adafruit - MIT license

// Modified to map virtual BurnerBoard 70x10 into 544 pixels
// with Y strip sizes of 31, 45, 60, 66, 70, 70, 66, 60, 45, 31
// Richard McDougall, June 2013

// The - is a hole in the virtual matrix
// The * is translated to a real LED pixel
//
// This image needs correcting -- don't forget ;-)
// But you get the idea, right?
//
// Starting at 0,0 left to right, bottom to top
//    Front
//
//       9,69
// ----**----    
// --******--
// --******--
// --******--
// --******--
// --******--
// -********-
// -********-
// -********-
// -********-
// -********-
// -********-
// -********-
// **********
// **********
// **********
// **********
// **********
// **********
// **********
// **********
// **********
// **********
// -********-
// -********-
// -********-
// -********-
// -********-
// -********-
// -********-
// --******--
// --******--
// --******--
// --******--
// --******--
// ----**----   
// 0,0 
//   Back 
// In addition to the main array, the edge pixels are prepended to the physical string,
// and logically appended to array. There are 79 pixels on each side for a total of 158.
// So, setPixelColor(..., 544 is the start of the side).

#if defined(__AVR_ATmega1280__) || defined(__AVR_ATmega2560__)
#define MEGA 1
#else
#define DUE 1
#endif

extern void screenHook();


// Was 544 with just the matrix, now 544 + 158
//#define NUM_EDGE_PIXELS 158
#define NUM_EDGE_PIXELS 158
//#define NUM_EDGE_PIXELS 114
#define NUM_REAL_BOARD_PIXELS (544 + NUM_EDGE_PIXELS)
/*
// Constructor for use with hardware SPI (specific clock/data pins):
// Todo: need to update dynamic width/height for future boards
Board_WS2801::Board_WS2801(uint16_t n, uint8_t order, boolean sl)  : Adafruit_GFX(70, 10) {
  rgb_order = order;
  hasSidelights = sl;
  alloc(n + NUM_EDGE_PIXELS);
  translationArray(n + NUM_EDGE_PIXELS);
  updatePins();
}

// Constructor for use with arbitrary clock/data pins:
Board_WS2801::Board_WS2801(uint16_t n, uint8_t dpin, uint8_t cpin, uint8_t order, boolean sl) : Adafruit_GFX(70, 10) {
  rgb_order = order;
  hasSidelights = sl;
  alloc(n + NUM_EDGE_PIXELS);
  translationArray(n + NUM_EDGE_PIXELS);
  updatePins(dpin, cpin);
}

// Constructor for use with a matrix configuration, specify w, h for size of matrix
// assumes configuration where string starts at coordinate 0,0 and continues to h-1,0, h-1,1
// and on to 0,1, 0,2 and on to h-1,2 and so on. Snaking back and forth till the end.
// other function calls with provide access to pixels via an x,y coordinate system
// Board is x,y swapped to original Adafruit matrix.
// It starts 10x70 starts at 0,0, on to 69,0, then 69,1 to 0,1, and so on
Board_WS2801::Board_WS2801(uint16_t w, uint16_t h, uint8_t dpin, uint8_t cpin, uint8_t order, boolean sl) : Adafruit_GFX(70, 10) {
  rgb_order = order;
  hasSidelights = sl;
  alloc(w * h + NUM_EDGE_PIXELS);
  translationArray(w * h + NUM_EDGE_PIXELS);
  width = w;
  height = h;
  updatePins(dpin, cpin);
}

*/

Board_WS2801::Board_WS2801(uint16_t w, uint16_t h, uint8_t order, boolean sl)  : Adafruit_GFX(70, 10) {
  rgb_order = order;
  hasSidelights = sl;
  alloc(w * h + NUM_EDGE_PIXELS);
  translationArray(w * h + NUM_EDGE_PIXELS);
  width = w;
  height = h;
  updatePins();
}

// via Michael Vogt/neophob: empty constructor is used when strand length
// isn't known at compile-time; situations where program config might be
// read from internal flash memory or an SD card, or arrive via serial
// command.  If using this constructor, MUST follow up with updateLength()
// and updatePins() to establish the strand length and output pins!
// Also, updateOrder() to change RGB vs GRB order (RGB is default).
Board_WS2801::Board_WS2801(void) : Adafruit_GFX(70, 10) {
  begun     = false;
  numvirtLEDs   = 0;
  pixels    = NULL;
  rgb_order = WS2801_RGB;
  updatePins(); // Must assume hardware SPI until pins are set
}

// Allocate 3 bytes per pixel, init to RGB 'off' state:
void Board_WS2801::alloc(uint16_t n) {
  begun   = false;
  numvirtLEDs = ((pixels = (uint8_t *)calloc(n, 3)) != NULL) ? n : 0;
}


// A cache of the pixel translations for the Burner Board LED strings
// For performance, translate and cache the result (math is more expensive than memory op)
// Cycles for load = 1
// Performance per instruction on page 10 of http://www.atmel.com/Images/doc0856.pdf
// translation is real board string pixel# = pixel_translate(virtual matrix with holes pixel#)
void Board_WS2801::translationArray(uint16_t n) {
  uint16_t virtpixel, newpixel, rgb;

  numboardLEDs = n;

  // Allocate Burner Board pix translation map
  pixel_translate = (uint16_t *)calloc(numboardLEDs, 3);

  if (pixel_translate == NULL)
    return;

  // For each virt board pixel translate to real board matrix positions
  for(virtpixel = 0; virtpixel < numvirtLEDs; virtpixel ++) {
    // Array Pixel starts at 0,0 and translation map calc'ed with start of 1,1
    newpixel = this->BoardPixel(virtpixel + 1);
    if (newpixel) {
      // Array Pixel starts at 0,0 and translation map calc'ed with start of 1,1
      newpixel--;
      for (rgb = 0; rgb < 3; rgb ++) {
        pixel_translate[(newpixel * 3) + rgb] = (virtpixel * 3) + rgb;
      }
    }
  }
}


// Release memory (as needed):
Board_WS2801::~Board_WS2801(void) {
  if (pixels != NULL) {
    free(pixels);
  }
}

// Activate hard/soft SPI as appropriate:
void Board_WS2801::begin(void) {
  if(hardwareSPI == true) {
    startSPI();
  } else {
    pinMode(datapin, OUTPUT);
    pinMode(clkpin , OUTPUT);
  }
  begun = true;
}

// Change pin assignments post-constructor, switching to hardware SPI:
void Board_WS2801::updatePins(void) {
  hardwareSPI = true;
  datapin     = clkpin = 0;
  // If begin() was previously invoked, init the SPI hardware now:
  if(begun == true) startSPI();
  // Otherwise, SPI is NOT initted until begin() is explicitly called.

  // Note: any prior clock/data pin directions are left as-is and are
  // NOT restored as inputs!
}

// Enable SPI hardware and set up protocol details:
void Board_WS2801::startSPI(void) {
#ifdef TEENSY
  SPI.setMOSI(7);
  SPI.setMISO(8);
  SPI.setSCK(14);
#endif
  SPI.begin();
  SPI.setBitOrder(MSBFIRST);
  SPI.setDataMode(SPI_MODE0);
  //    SPI.setClockDivider(SPI_CLOCK_DIV16); // 1 MHz max, else flicker
#ifdef MEGA
  //SPI.setClockDivider(SPI_CLOCK_DIV16); // 1 MHz max, else flicker
#else
  // DUE
  SPI.setClockDivider(82); // 1 MHz max, else flicker
  // TEENSY
  SPI.setClockDivider(SPI_CLOCK_DIV16); // 1 MHz max, else flicker
#endif
}

uint16_t Board_WS2801::numPixels(void) {
  return numvirtLEDs;
}

// Change strand length (see notes with empty constructor, above):
void Board_WS2801::updateLength(uint16_t n) {
  if(pixels != NULL) free(pixels); // Free existing data (if any)
  // Allocate new data -- note: ALL PIXELS ARE CLEARED
  numvirtLEDs = ((pixels = (uint8_t *)calloc(n, 3)) != NULL) ? n : 0;
  // 'begun' state does not change -- pins retain prior modes
}

// Change RGB data order (see notes with empty constructor, above):
void Board_WS2801::updateOrder(uint8_t order) {
  rgb_order = order;
  // Existing LED data, if any, is NOT reformatted to new data order.
  // Calling function should clear or fill pixel data anew.
}

// Clock out the actual Burner Board Pixels without holes
// 
void Board_WS2801::show(void) {
  uint16_t i, nl3 = NUM_REAL_BOARD_PIXELS * 3; // 3 bytes per LED
  uint8_t  bit;

  // Write 24 bits per pixel:
  if(hardwareSPI) {
  //noInterrupts();
  //startSPI();
//    for(i = hasSidelights ? 0 : NUM_EDGE_PIXELS * 3; i<nl3; i++) {
    for(i = 0; i < nl3; i++) {
	SPI.transfer(pixels[pixel_translate[i]] / brightness);
//      SPDR = pixels[pixel_translate[i]];
//      while(!(SPSR & (1<<SPIF)));
    }
		//interrupts();
  } else {
    for(i=0; i<nl3; i++ ) {
      for(bit=0x80; bit; bit >>= 1) {
        if(pixels[pixel_translate[i]] & bit) 
          *dataport |=  datapinmask;
        else                
          *dataport &= ~datapinmask;
        *clkport |=  clkpinmask;
        *clkport &= ~clkpinmask;
      }
    }
  }
  updates++;
  delay(1); // Data is latched by holding clock pin low for 1 millisecond
  screenHook();

}

// Set pixel color from separate 8-bit R, G, B components:
void Board_WS2801::setPixelColor(uint16_t n, uint8_t r, uint8_t g, uint8_t b) {
  if(n < numvirtLEDs) { // Arrays are 0-indexed, thus NOT '<='
    uint8_t *p = &pixels[n * 3];
    // See notes later regarding color order
    if(rgb_order == WS2801_RGB) {
      *p++ = r;
      *p++ = g;
    } else {
      *p++ = g;
      *p++ = r;
    }
    *p++ = b;
  }
}

// Set pixel color from separate 8-bit R, G, B components using x,y coordinate system:
void Board_WS2801::setPixelColor(uint16_t x, uint16_t y, uint8_t r, uint8_t g, uint8_t b) {
  // calculate x offset first
  uint16_t offset = y % height;
  // add x offset
  offset += x * height;
  setPixelColor(offset, r, g, b);
}

// Set pixel color from 'packed' 32-bit RGB value:
void Board_WS2801::setPixelColor(uint16_t n, uint32_t c) {
  if(n < numvirtLEDs) { // Arrays are 0-indexed, thus NOT '<='
    uint8_t *p = &pixels[n * 3];
    // To keep the show() loop as simple & fast as possible, the
    // internal color representation is native to different pixel
    // types.  For compatibility with existing code, 'packed' RGB
    // values passed in or out are always 0xRRGGBB order.
    if(rgb_order == WS2801_RGB) {
      *p++ = c >> 16; // Red
      *p++ = c >>  8; // Green
    } else {
      *p++ = c >>  8; // Green
      *p++ = c >> 16; // Red
    }
    *p++ = c;         // Blue
  }
}

// Set pixel color from 'packed' 32-bit RGB value using x,y coordinate system:
void Board_WS2801::setPixelColor(uint16_t x, uint16_t y, uint32_t c) {
  // calculate x offset first
  uint16_t offset = y % height;
  // add x offset
  offset += x * height;
  setPixelColor(offset, c);
}

// Query color from previously-set pixel (returns packed 32-bit RGB value)
uint32_t Board_WS2801::getPixelColor(uint16_t n) {
  if(n < numvirtLEDs) {
    uint16_t ofs = n * 3;
    // To keep the show() loop as simple & fast as possible, the
    // internal color representation is native to different pixel
    // types.  For compatibility with existing code, 'packed' RGB
    // values passed in or out are always 0xRRGGBB order.
    return (rgb_order == WS2801_RGB) ?
      ((uint32_t)pixels[ofs] << 16) | ((uint16_t) pixels[ofs + 1] <<  8) | pixels[ofs + 2] :
      (pixels[ofs] <<  8) | ((uint32_t)pixels[ofs + 1] << 16) | pixels[ofs + 2];
  }

  return 0; // Pixel # is out of bounds
}

// Query color from previously-set pixel (returns packed 32-bit RGB value)
uint32_t Board_WS2801::getPixelColor(uint16_t x, uint16_t y) {
  // calculate x offset first
  uint16_t n = y % height;
  // add x offset
  n += x * height;
  if(n < numvirtLEDs) {
    uint16_t ofs = n * 3;
    // To keep the show() loop as simple & fast as possible, the
    // internal color representation is native to different pixel
    // types.  For compatibility with existing code, 'packed' RGB
    // values passed in or out are always 0xRRGGBB order.
    return (rgb_order == WS2801_RGB) ?
      ((uint32_t)pixels[ofs] << 16) | ((uint16_t) pixels[ofs + 1] <<  8) | pixels[ofs + 2] :
      (pixels[ofs] <<  8) | ((uint32_t)pixels[ofs + 1] << 16) | pixels[ofs + 2];
  }

  return 0; // Pixel # is out of bounds
}


void Board_WS2801::setBrightness(uint8_t b) {

  if (b > 0) 
    brightness = 255 / b;
  else
    brightness = 255;
}




// Map virtual pixels to physical pixels on the Burner Board Layout
// Emulate a 70 x 10 rectangle matrix 
// Optionally add a two 79 pixel (158) strips on the side 
// as pixels 0-157 before the real board pixels start
// Strip lengths are 31, 45, 60, 66, 70, 70, 66, 60, 45, 31
// format is colx: virt pixel offset -> real pixel offset
// col1: 1-19, 20-50, 51-70: 20-50->1-31
// col2: 71-140: 71-82, 83-127, 128-140: 83-127->76-32 
// col3: 141-210: 141-145, 146-205, 206-210: 146-205->77-136
// col4: 211-280: 211-212, 213->278, 279-280: 213-278->202-137
// col5: 281-350: 281-350: 281-350->203-272
// col6: 351-420: 351-420: 351-420->342-273
// col7: 421-490: 421-422, 423-488, 489-490: 423-488->343-408
// col8: 491-560: 491-495, 496-555, 556-560: 496-555->468-409
// col9: 561-630: 561-572, 573-617, 618-630: 573-617->469-513
// col10: 631-700: 631-649, 650-680, 681-700: 650-680->544-514
uint32_t Board_WS2801::BoardPixel(uint32_t pixel) {
  uint32_t newpixel;

  // Pixel is a hole in the map, returns 0
  newpixel = 0;

  // Virt Pixels 701-858 are the edge pixels
  if ((hasSidelights == true) && (pixel > 700)) {
 
    pixel = pixel - 701;

    // 1st strip of edge 
    if (pixel < (NUM_EDGE_PIXELS / 2)) {
      newpixel = pixel;
    }

    // 2nd strip of edge - reverse order
    if (pixel >= (NUM_EDGE_PIXELS / 2)) {
      newpixel = NUM_EDGE_PIXELS - 1 - (pixel - (NUM_EDGE_PIXELS / 2));
    }

    // we calc +1
    newpixel++;

  } else {

      // Map linear row x column strip into strip with holes in grid
      // to cater for pixels that are missing from the corners of the
      // Burner Board layout
    
      //1  20-50->1-31
      if (pixel >= 20 && pixel <=50)
        newpixel = pixel - 19;
      //2 83-127->76-32
      if (pixel >= 83 && pixel <= 127)
        newpixel = 127 - pixel + 32;
      //3 146-205->77-136
      if (pixel >= 146 && pixel <= 205)
        newpixel = pixel - 146 + 77;
      //4 213-278->202-137
      if (pixel >= 213 && pixel <= 278)
        newpixel = 278 - pixel + 137;
      //5 281-350->203-272
      if (pixel >= 281 && pixel <= 350)
        newpixel = pixel - 281 + 203;
      //6 351-420->342-273
      if (pixel >= 351 && pixel <=420)
        newpixel = 420 - pixel + 273;
      //7 423-488->343-408
      if (pixel >= 423 && pixel <= 488)
        newpixel = pixel - 423 + 343;
      //8 496-555->468-409
      if (pixel >= 496 && pixel <= 555)
        newpixel = 555 - pixel + 409;
      //9 573-617->469-513
      if (pixel >= 573 && pixel <= 617)
        newpixel = pixel - 573 + 469;
      //10 650-680->544-514
      if (pixel >= 650 && pixel <= 680)
        newpixel = 680 - pixel + 514;

      if (hasSidelights == true) {
	newpixel += NUM_EDGE_PIXELS;
      }
  }

  return newpixel;
}  

// Functions for GFX Library

// Pass 8-bit (each) R,G,B, get back 16-bit packed color
uint16_t Board_WS2801::Color565(uint8_t r, uint8_t g, uint8_t b) {
  return ((r & 0xF8) << 8) | ((g & 0xFC) << 3) | (b >> 3);
}

// Pass 8-bit (each) R,G,B, get back 16-bit packed color
uint32_t Color2rgb(uint16_t color) {
  uint32_t r, g, b;
  uint32_t return_color;

  r = color & 0xf800;
	r = r << (5 + 3);
	
  g = color & 0x7e0;
	g = g << (3 + 2);

  b = color & 0x1F;
	b = b << (0 + 3);	
	
  return_color = r | g | b;

  return return_color;
}

// Callback from GFX engine to draw pixel on the board
void Board_WS2801::drawPixel(int16_t x, int16_t y, uint16_t color) {

	// calculate x offset first
  uint16_t offset = (69 - x) % height;
  // add x offset
  offset += (9 - y) * height;
  setPixelColor(offset, Color2rgb(color));

}

void Board_WS2801::print(char *string, uint8_t x, uint8_t y, uint8_t size) {
		  setTextSize(size);
		  setTextColor(Color565(255, 255, 255));
//		  fillRect(x, y,  7 * size * strlen(string) + 1, 7 * size, Color565(0, 0, 0));
//		  setRotation(1);
		  setCursor(x, y);
		  Adafruit_GFX::print(string);	
}


