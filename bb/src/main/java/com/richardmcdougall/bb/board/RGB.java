package com.richardmcdougall.bb.board;

import java.util.concurrent.ConcurrentLinkedQueue;

public class RGB {

        public String name = "";
        public int r;
        public int g;
        public int b;
        
        // Object pool to reduce GC pressure
        private static final ConcurrentLinkedQueue<RGB> POOL = new ConcurrentLinkedQueue<>();
        private static final int MAX_POOL_SIZE = 1000;
        
        public boolean isBlack(){
            return r==0 && g==0 && b==0;
        }
        public boolean isWhite(){
            return r==255 && g==255 && b==255;
        }
        
        // Method to update existing RGB object values
        public void setValues(int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
        }
        
        // Method to update from RGBA int without creating new object
        public void setFromRGBAInt(int color) {
            this.r = color & 0x000000ff;
            this.g = (color & 0x0000ff00) >> 8;
            this.b = (color & 0x00ff0000) >> 16;
        }
        
        // Method to update from ARGB int without creating new object
        public void setFromARGBInt(int color) {
            this.r = (color & 0xff0000) >> 16;
            this.g = (color & 0xff00) >> 8;
            this.b = color & 0xff;
        }
        
        // Get RGB object from pool or create new one
        public static RGB obtain() {
            RGB rgb = POOL.poll();
            if (rgb == null) {
                rgb = new RGB();
            }
            rgb.name = "";
            rgb.r = 0;
            rgb.g = 0;
            rgb.b = 0;
            return rgb;
        }
        
        // Return RGB object to pool for reuse
        public void recycle() {
            if (POOL.size() < MAX_POOL_SIZE) {
                POOL.offer(this);
            }
        }

    //https://stackoverflow.com/questions/47970384/why-is-copypixelsfrombuffer-giving-incorrect-color-setpixels-is-correct-but-slo
    public static RGB fromRGBAInt(int color) {

        RGB x = new RGB();

        x.r = color & 0x000000ff;
        x.g = (color & 0x0000ff00) >> 8;
        x.b =  (color & 0x00ff0000) >> 16;
 
        return x;
    }

    public static RGB fromARGBInt(int color){
        int r = ((color & 0xff0000) >> 16);
        int g = ((color & 0xff00) >> 8);
        int b = (color & 0xff);
        return new RGB(r,g,b);
    }

    public int getARGBInt(){
        return (this.r * 65536 + this.g * 256 + this.b);
    }

    static public int getARGBInt(int r, int g, int b) {
            return (r * 65536 + g * 256 + b);
    }

    private RGB(){
    }

    public RGB(String name, int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.name = name;
    }

    public RGB(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public void colorDim(int dimValue) {
       b = dimValue * b / 255;
       g = dimValue * g / 255;
       r = dimValue * r / 255;
    }

    public String getName() {
        return name;
    }
}
