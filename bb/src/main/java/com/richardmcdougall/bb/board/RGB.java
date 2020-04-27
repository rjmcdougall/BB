package com.richardmcdougall.bb.board;

public class RGB {

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
    public static RGB rgbaTorgb(int color) {

        RGB x = new RGB();

        x.r = (color & 0xff);
        x.g = ((color & 0xff00) >> 8);
        x.b = ((color & 0xff0000) >> 16);

        int a = ((color & 0xff0000) >> 24);

        return x;
    }

    static public int getRGB(int r, int g, int b) {
        return (r * 65536 + g * 256 + b);
    }

}
