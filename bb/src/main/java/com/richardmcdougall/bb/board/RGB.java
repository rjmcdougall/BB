package com.richardmcdougall.bb.board;

public class RGB {

        public String name = "";
        public int r;
        public int g;
        public int b;
        public boolean isBlack(){
            return r==0 && g==0 & b==0;
        }
        public boolean isWhite(){
            return r==255 && g==255 & b==255;
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
