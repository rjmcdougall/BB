package com.richardmcdougall.bb.visualization;

import java.util.ArrayList;

public class BBColor {

    /**
     * Initialize the color list that we have.
     */

    private ArrayList<ColorName> initColorList() {
        ArrayList<ColorName> colorList = new ArrayList<ColorName>();
        colorList.add(new ColorName("aliceblue", 0xf0, 0xf8, 0xFF));
        colorList.add(new ColorName("antiquewhite", 0xfa, 0xeb, 0xD7));
        colorList.add(new ColorName("aqua", 0x00, 0xff, 0xFF));
        colorList.add(new ColorName("aquamarine", 0x7f, 0xff, 0xD4));
        colorList.add(new ColorName("azure", 0xf0, 0xff, 0xFF));
        colorList.add(new ColorName("beige", 0xf5, 0xf5, 0xDC));
        colorList.add(new ColorName("bisque", 0xff, 0xe4, 0xC4));
        colorList.add(new ColorName("black", 0x00, 0x00, 0x00));
        colorList.add(new ColorName("blanchedalmond", 0xff, 0xeb, 0xCD));
        colorList.add(new ColorName("blue", 0x00, 0x00, 0xFF));
        colorList.add(new ColorName("blueviolet", 0x8a, 0x2b, 0xE2));
        colorList.add(new ColorName("brown", 0xa5, 0x2a, 0x2A));
        colorList.add(new ColorName("burlywood", 0xde, 0xb8, 0x87));
        colorList.add(new ColorName("cadetblue", 0x5f, 0x9e, 0xA0));
        colorList.add(new ColorName("chartreuse", 0x7f, 0xff, 0x00));
        colorList.add(new ColorName("chocolate", 0xd2, 0x69, 0x1E));
        colorList.add(new ColorName("coral", 0xff, 0x7f, 0x50));
        colorList.add(new ColorName("cornflowerblue", 0x64, 0x95, 0xED));
        colorList.add(new ColorName("cornsilk", 0xff, 0xf8, 0xDC));
        colorList.add(new ColorName("crimson", 0xdc, 0x14, 0x3C));
        colorList.add(new ColorName("cyan", 0x00, 0xff, 0xFF));
        colorList.add(new ColorName("darkblue", 0x00, 0x00, 0x8B));
        colorList.add(new ColorName("darkcyan", 0x00, 0x8b, 0x8B));
        colorList.add(new ColorName("darkgoldenrod", 0xb8, 0x86, 0x0B));
        colorList.add(new ColorName("darkgray", 0xa9, 0xa9, 0xA9));
        colorList.add(new ColorName("darkgreen", 0x00, 0x64, 0x00));
        colorList.add(new ColorName("darkkhaki", 0xbd, 0xb7, 0x6B));
        colorList.add(new ColorName("darkmagenta", 0x8b, 0x00, 0x8B));
        colorList.add(new ColorName("darkolivegreen", 0x55, 0x6b, 0x2F));
        colorList.add(new ColorName("darkorange", 0xff, 0x8c, 0x00));
        colorList.add(new ColorName("darkorchid", 0x99, 0x32, 0xCC));
        colorList.add(new ColorName("darkred", 0x8b, 0x00, 0x00));
        colorList.add(new ColorName("darksalmon", 0xe9, 0x96, 0x7A));
        colorList.add(new ColorName("darkseagreen", 0x8f, 0xbc, 0x8F));
        colorList.add(new ColorName("darkslateblue", 0x48, 0x3d, 0x8B));
        colorList.add(new ColorName("darkslategray", 0x2f, 0x4f, 0x4F));
        colorList.add(new ColorName("darkturquoise", 0x00, 0xce, 0xD1));
        colorList.add(new ColorName("darkviolet", 0x94, 0x00, 0xD3));
        colorList.add(new ColorName("deeppink", 0xff, 0x14, 0x93));
        colorList.add(new ColorName("deepskyblue", 0x00, 0xbf, 0xFF));
        colorList.add(new ColorName("dimgray", 0x69, 0x69, 0x69));
        colorList.add(new ColorName("dodgerblue", 0x1e, 0x90, 0xFF));
        colorList.add(new ColorName("firebrick", 0xb2, 0x22, 0x22));
        colorList.add(new ColorName("floralwhite", 0xff, 0xfa, 0xF0));
        colorList.add(new ColorName("forestgreen", 0x22, 0x8b, 0x22));
        colorList.add(new ColorName("fuchsia", 0xff, 0x00, 0xFF));
        colorList.add(new ColorName("gainsboro", 0xdc, 0xdc, 0xDC));
        colorList.add(new ColorName("ghostwhite", 0xf8, 0xf8, 0xFF));
        colorList.add(new ColorName("gold", 0xff, 0xd7, 0x00));
        colorList.add(new ColorName("goldenrod", 0xda, 0xa5, 0x20));
        colorList.add(new ColorName("gray", 0x80, 0x80, 0x80));
        colorList.add(new ColorName("green", 0x00, 0x80, 0x00));
        colorList.add(new ColorName("greenyellow", 0xad, 0xff, 0x2F));
        colorList.add(new ColorName("honeydew", 0xf0, 0xff, 0xF0));
        colorList.add(new ColorName("hotpink", 0xff, 0x69, 0xB4));
        colorList.add(new ColorName("indianred", 0xcd, 0x5c, 0x5C));
        colorList.add(new ColorName("indigo", 0x4b, 0x00, 0x82));
        colorList.add(new ColorName("ivory", 0xff, 0xff, 0xF0));
        colorList.add(new ColorName("khaki", 0xf0, 0xe6, 0x8C));
        colorList.add(new ColorName("lavender", 0xe6, 0xe6, 0xFA));
        colorList.add(new ColorName("lavenderblush", 0xff, 0xf0, 0xF5));
        colorList.add(new ColorName("lawngreen", 0x7c, 0xfc, 0x00));
        colorList.add(new ColorName("lemonchiffon", 0xff, 0xfa, 0xCD));
        colorList.add(new ColorName("lightblue", 0xad, 0xd8, 0xE6));
        colorList.add(new ColorName("lightcoral", 0xf0, 0x80, 0x80));
        colorList.add(new ColorName("lightcyan", 0xe0, 0xff, 0xFF));
        colorList.add(new ColorName("lightgoldenrodyellow", 0xfa, 0xfa, 0xD2));
        colorList.add(new ColorName("lightgray", 0xd3, 0xd3, 0xD3));
        colorList.add(new ColorName("lightgreen", 0x90, 0xee, 0x90));
        colorList.add(new ColorName("lightpink", 0xff, 0xb6, 0xC1));
        colorList.add(new ColorName("lightsalmon", 0xff, 0xa0, 0x7A));
        colorList.add(new ColorName("lightseagreen", 0x20, 0xb2, 0xAA));
        colorList.add(new ColorName("lightskyblue", 0x87, 0xce, 0xFA));
        colorList.add(new ColorName("lightslategray", 0x77, 0x88, 0x99));
        colorList.add(new ColorName("lightsteelblue", 0xb0, 0xc4, 0xDE));
        colorList.add(new ColorName("lightyellow", 0xff, 0xff, 0xE0));
        colorList.add(new ColorName("lime", 0x00, 0xff, 0x00));
        colorList.add(new ColorName("limegreen", 0x32, 0xcd, 0x32));
        colorList.add(new ColorName("linen", 0xfa, 0xf0, 0xE6));
        colorList.add(new ColorName("magenta", 0xff, 0x00, 0xFF));
        colorList.add(new ColorName("maroon", 0x80, 0x00, 0x00));
        colorList.add(new ColorName("mediumaquamarine", 0x66, 0xcd, 0xAA));
        colorList.add(new ColorName("mediumblue", 0x00, 0x00, 0xCD));
        colorList.add(new ColorName("mediumorchid", 0xba, 0x55, 0xD3));
        colorList.add(new ColorName("mediumpurple", 0x93, 0x70, 0xDB));
        colorList.add(new ColorName("mediumseagreen", 0x3c, 0xb3, 0x71));
        colorList.add(new ColorName("mediumslateblue", 0x7b, 0x68, 0xEE));
        colorList.add(new ColorName("mediumspringgreen", 0x00, 0xfa, 0x9A));
        colorList.add(new ColorName("mediumturquoise", 0x48, 0xd1, 0xCC));
        colorList.add(new ColorName("mediumvioletred", 0xc7, 0x15, 0x85));
        colorList.add(new ColorName("midnightblue", 0x19, 0x19, 0x70));
        colorList.add(new ColorName("mintcream", 0xf5, 0xff, 0xFA));
        colorList.add(new ColorName("mistyrose", 0xff, 0xe4, 0xE1));
        colorList.add(new ColorName("moccasin", 0xff, 0xe4, 0xB5));
        colorList.add(new ColorName("navajowhite", 0xff, 0xde, 0xAD));
        colorList.add(new ColorName("navy", 0x00, 0x00, 0x80));
        colorList.add(new ColorName("oldlace", 0xfd, 0xf5, 0xE6));
        colorList.add(new ColorName("olive", 0x80, 0x80, 0x00));
        colorList.add(new ColorName("olivedrab", 0x6b, 0x8e, 0x23));
        colorList.add(new ColorName("orange", 0xff, 0xa5, 0x00));
        colorList.add(new ColorName("orangered", 0xff, 0x45, 0x00));
        colorList.add(new ColorName("orchid", 0xda, 0x70, 0xD6));
        colorList.add(new ColorName("palegoldenrod", 0xee, 0xe8, 0xAA));
        colorList.add(new ColorName("palegreen", 0x98, 0xfb, 0x98));
        colorList.add(new ColorName("paleturquoise", 0xaf, 0xee, 0xEE));
        colorList.add(new ColorName("palevioletred", 0xdb, 0x70, 0x93));
        colorList.add(new ColorName("papayawhip", 0xff, 0xef, 0xD5));
        colorList.add(new ColorName("peachpuff", 0xff, 0xda, 0xB9));
        colorList.add(new ColorName("peru", 0xcd, 0x85, 0x3F));
        colorList.add(new ColorName("pink", 0xff, 0xc0, 0xCB));
        colorList.add(new ColorName("plum", 0xdd, 0xa0, 0xDD));
        colorList.add(new ColorName("powderblue", 0xb0, 0xe0, 0xE6));
        colorList.add(new ColorName("purple", 0x80, 0x00, 0x80));
        colorList.add(new ColorName("red", 0xff, 0x00, 0x00));
        colorList.add(new ColorName("rosybrown", 0xbc, 0x8f, 0x8F));
        colorList.add(new ColorName("royalblue", 0x41, 0x69, 0xE1));
        colorList.add(new ColorName("saddlebrown", 0x8b, 0x45, 0x13));
        colorList.add(new ColorName("salmon", 0xfa, 0x80, 0x72));
        colorList.add(new ColorName("sandybrown", 0xf4, 0xa4, 0x60));
        colorList.add(new ColorName("seagreen", 0x2e, 0x8b, 0x57));
        colorList.add(new ColorName("seashell", 0xff, 0xf5, 0xEE));
        colorList.add(new ColorName("sienna", 0xa0, 0x52, 0x2D));
        colorList.add(new ColorName("silver", 0xc0, 0xc0, 0xC0));
        colorList.add(new ColorName("skyblue", 0x87, 0xce, 0xEB));
        colorList.add(new ColorName("slateblue", 0x6a, 0x5a, 0xCD));
        colorList.add(new ColorName("slategray", 0x70, 0x80, 0x90));
        colorList.add(new ColorName("snow", 0xff, 0xfa, 0xFA));
        colorList.add(new ColorName("springgreen", 0x00, 0xff, 0x7F));
        colorList.add(new ColorName("steelblue", 0x46, 0x82, 0xB4));
        colorList.add(new ColorName("tan", 0xd2, 0xb4, 0x8C));
        colorList.add(new ColorName("teal", 0x00, 0x80, 0x80));
        colorList.add(new ColorName("thistle", 0xd8, 0xbf, 0xD8));
        colorList.add(new ColorName("tomato", 0xff, 0x63, 0x47));
        colorList.add(new ColorName("turquoise", 0x40, 0xe0, 0xD0));
        colorList.add(new ColorName("violet", 0xee, 0x82, 0xEE));
        colorList.add(new ColorName("wheat", 0xf5, 0xde, 0xB3));
        colorList.add(new ColorName("white", 0xff, 0xff, 0xFF));
        colorList.add(new ColorName("whitesmoke", 0xf5, 0xf5, 0xF5));
        colorList.add(new ColorName("yellow", 0xff, 0xff, 0x00));
        colorList.add(new ColorName("yellowgreen", 0x9a, 0xcd, 0x32));
        return colorList;
    }

    public String getColorNameFromRgb(int r, int g, int b) {
        ArrayList<ColorName> colorList = initColorList();
        ColorName closestMatch = null;
        int minMSE = Integer.MAX_VALUE;
        int mse;
        for (ColorName c : colorList) {
            mse = c.computeMSE(r, g, b);
            if (mse < minMSE) {
                minMSE = mse;
                closestMatch = c;
            }
        }

        if (closestMatch != null) {
            return closestMatch.getName();
        } else {
            return "No matched color name.";
        }
    }

    public ColorName getColor(String name) {
        ArrayList<ColorName> colorList = initColorList();
        for (ColorName c : colorList) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }

    public class ColorName {
        public int r, g, b;
        public String name;

        public ColorName(String name, int r, int g, int b) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.name = name;
        }

        public int computeMSE(int pixR, int pixG, int pixB) {
            return (int) (((pixR - r) * (pixR - r) + (pixG - g) * (pixG - g) + (pixB - b)
                    * (pixB - b)) / 3);
        }

        public int getR() {
            return r;
        }

        public int getG() {
            return g;
        }

        public int getB() {
            return b;
        }

        public String getName() {
            return name;
        }
    }
}
