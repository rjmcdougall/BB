package com.richardmcdougall.bb.visualization;

import com.richardmcdougall.bb.board.RGB;

import java.util.ArrayList;

public class RGBList {

    // Static cache to avoid recreating color list every time
    private static ArrayList<RGB> COLOR_LIST = null;
    
    private static ArrayList<RGB> getColorList() {
        if (COLOR_LIST == null) {
            COLOR_LIST = initColorList();
        }
        return COLOR_LIST;
    }

    private static ArrayList<RGB> initColorList() {
        ArrayList<RGB> colorList = new ArrayList<RGB>();
        colorList.add(new RGB("aliceblue", 0xf0, 0xf8, 0xFF));
        colorList.add(new RGB("antiquewhite", 0xfa, 0xeb, 0xD7));
        colorList.add(new RGB("aqua", 0x00, 0xff, 0xFF));
        colorList.add(new RGB("aquamarine", 0x7f, 0xff, 0xD4));
        colorList.add(new RGB("azure", 0xf0, 0xff, 0xFF));
        colorList.add(new RGB("beige", 0xf5, 0xf5, 0xDC));
        colorList.add(new RGB("bisque", 0xff, 0xe4, 0xC4));
        colorList.add(new RGB("black", 0x00, 0x00, 0x00));
        colorList.add(new RGB("blanchedalmond", 0xff, 0xeb, 0xCD));
        colorList.add(new RGB("blue", 0x00, 0x00, 0xFF));
        colorList.add(new RGB("blueviolet", 0x8a, 0x2b, 0xE2));
        colorList.add(new RGB("brown", 0xa5, 0x2a, 0x2A));
        colorList.add(new RGB("burlywood", 0xde, 0xb8, 0x87));
        colorList.add(new RGB("cadetblue", 0x5f, 0x9e, 0xA0));
        colorList.add(new RGB("chartreuse", 0x7f, 0xff, 0x00));
        colorList.add(new RGB("chocolate", 0xd2, 0x69, 0x1E));
        colorList.add(new RGB("coral", 0xff, 0x7f, 0x50));
        colorList.add(new RGB("cornflowerblue", 0x64, 0x95, 0xED));
        colorList.add(new RGB("cornsilk", 0xff, 0xf8, 0xDC));
        colorList.add(new RGB("crimson", 0xdc, 0x14, 0x3C));
        colorList.add(new RGB("cyan", 0x00, 0xff, 0xFF));
        colorList.add(new RGB("darkblue", 0x00, 0x00, 0x8B));
        colorList.add(new RGB("darkcyan", 0x00, 0x8b, 0x8B));
        colorList.add(new RGB("darkgoldenrod", 0xb8, 0x86, 0x0B));
        colorList.add(new RGB("darkgray", 0xa9, 0xa9, 0xA9));
        colorList.add(new RGB("darkgreen", 0x00, 0x64, 0x00));
        colorList.add(new RGB("darkkhaki", 0xbd, 0xb7, 0x6B));
        colorList.add(new RGB("darkmagenta", 0x8b, 0x00, 0x8B));
        colorList.add(new RGB("darkolivegreen", 0x55, 0x6b, 0x2F));
        colorList.add(new RGB("darkorange", 0xff, 0x8c, 0x00));
        colorList.add(new RGB("darkorchid", 0x99, 0x32, 0xCC));
        colorList.add(new RGB("darkred", 0x8b, 0x00, 0x00));
        colorList.add(new RGB("darksalmon", 0xe9, 0x96, 0x7A));
        colorList.add(new RGB("darkseagreen", 0x8f, 0xbc, 0x8F));
        colorList.add(new RGB("darkslateblue", 0x48, 0x3d, 0x8B));
        colorList.add(new RGB("darkslategray", 0x2f, 0x4f, 0x4F));
        colorList.add(new RGB("darkturquoise", 0x00, 0xce, 0xD1));
        colorList.add(new RGB("darkviolet", 0x94, 0x00, 0xD3));
        colorList.add(new RGB("deeppink", 0xff, 0x14, 0x93));
        colorList.add(new RGB("deepskyblue", 0x00, 0xbf, 0xFF));
        colorList.add(new RGB("dimgray", 0x69, 0x69, 0x69));
        colorList.add(new RGB("dodgerblue", 0x1e, 0x90, 0xFF));
        colorList.add(new RGB("firebrick", 0xb2, 0x22, 0x22));
        colorList.add(new RGB("floralwhite", 0xff, 0xfa, 0xF0));
        colorList.add(new RGB("forestgreen", 0x22, 0x8b, 0x22));
        colorList.add(new RGB("fuchsia", 0xff, 0x00, 0xFF));
        colorList.add(new RGB("gainsboro", 0xdc, 0xdc, 0xDC));
        colorList.add(new RGB("ghostwhite", 0xf8, 0xf8, 0xFF));
        colorList.add(new RGB("gold", 0xff, 0xd7, 0x00));
        colorList.add(new RGB("goldenrod", 0xda, 0xa5, 0x20));
        colorList.add(new RGB("gray", 0x80, 0x80, 0x80));
        colorList.add(new RGB("green", 0x00, 0x80, 0x00));
        colorList.add(new RGB("greenyellow", 0xad, 0xff, 0x2F));
        colorList.add(new RGB("honeydew", 0xf0, 0xff, 0xF0));
        colorList.add(new RGB("hotpink", 0xff, 0x69, 0xB4));
        colorList.add(new RGB("indianred", 0xcd, 0x5c, 0x5C));
        colorList.add(new RGB("indigo", 0x4b, 0x00, 0x82));
        colorList.add(new RGB("ivory", 0xff, 0xff, 0xF0));
        colorList.add(new RGB("khaki", 0xf0, 0xe6, 0x8C));
        colorList.add(new RGB("lavender", 0xe6, 0xe6, 0xFA));
        colorList.add(new RGB("lavenderblush", 0xff, 0xf0, 0xF5));
        colorList.add(new RGB("lawngreen", 0x7c, 0xfc, 0x00));
        colorList.add(new RGB("lemonchiffon", 0xff, 0xfa, 0xCD));
        colorList.add(new RGB("lightblue", 0xad, 0xd8, 0xE6));
        colorList.add(new RGB("lightcoral", 0xf0, 0x80, 0x80));
        colorList.add(new RGB("lightcyan", 0xe0, 0xff, 0xFF));
        colorList.add(new RGB("lightgoldenrodyellow", 0xfa, 0xfa, 0xD2));
        colorList.add(new RGB("lightgray", 0xd3, 0xd3, 0xD3));
        colorList.add(new RGB("lightgreen", 0x90, 0xee, 0x90));
        colorList.add(new RGB("lightpink", 0xff, 0xb6, 0xC1));
        colorList.add(new RGB("lightsalmon", 0xff, 0xa0, 0x7A));
        colorList.add(new RGB("lightseagreen", 0x20, 0xb2, 0xAA));
        colorList.add(new RGB("lightskyblue", 0x87, 0xce, 0xFA));
        colorList.add(new RGB("lightslategray", 0x77, 0x88, 0x99));
        colorList.add(new RGB("lightsteelblue", 0xb0, 0xc4, 0xDE));
        colorList.add(new RGB("lightyellow", 0xff, 0xff, 0xE0));
        colorList.add(new RGB("lime", 0x00, 0xff, 0x00));
        colorList.add(new RGB("limegreen", 0x32, 0xcd, 0x32));
        colorList.add(new RGB("linen", 0xfa, 0xf0, 0xE6));
        colorList.add(new RGB("magenta", 0xff, 0x00, 0xFF));
        colorList.add(new RGB("maroon", 0x80, 0x00, 0x00));
        colorList.add(new RGB("mediumaquamarine", 0x66, 0xcd, 0xAA));
        colorList.add(new RGB("mediumblue", 0x00, 0x00, 0xCD));
        colorList.add(new RGB("mediumorchid", 0xba, 0x55, 0xD3));
        colorList.add(new RGB("mediumpurple", 0x93, 0x70, 0xDB));
        colorList.add(new RGB("mediumseagreen", 0x3c, 0xb3, 0x71));
        colorList.add(new RGB("mediumslateblue", 0x7b, 0x68, 0xEE));
        colorList.add(new RGB("mediumspringgreen", 0x00, 0xfa, 0x9A));
        colorList.add(new RGB("mediumturquoise", 0x48, 0xd1, 0xCC));
        colorList.add(new RGB("mediumvioletred", 0xc7, 0x15, 0x85));
        colorList.add(new RGB("midnightblue", 0x19, 0x19, 0x70));
        colorList.add(new RGB("mintcream", 0xf5, 0xff, 0xFA));
        colorList.add(new RGB("mistyrose", 0xff, 0xe4, 0xE1));
        colorList.add(new RGB("moccasin", 0xff, 0xe4, 0xB5));
        colorList.add(new RGB("navajowhite", 0xff, 0xde, 0xAD));
        colorList.add(new RGB("navy", 0x00, 0x00, 0x80));
        colorList.add(new RGB("oldlace", 0xfd, 0xf5, 0xE6));
        colorList.add(new RGB("olive", 0x80, 0x80, 0x00));
        colorList.add(new RGB("olivedrab", 0x6b, 0x8e, 0x23));
        colorList.add(new RGB("orange", 0xff, 0xa5, 0x00));
        colorList.add(new RGB("orangered", 0xff, 0x45, 0x00));
        colorList.add(new RGB("orchid", 0xda, 0x70, 0xD6));
        colorList.add(new RGB("palegoldenrod", 0xee, 0xe8, 0xAA));
        colorList.add(new RGB("palegreen", 0x98, 0xfb, 0x98));
        colorList.add(new RGB("paleturquoise", 0xaf, 0xee, 0xEE));
        colorList.add(new RGB("palevioletred", 0xdb, 0x70, 0x93));
        colorList.add(new RGB("papayawhip", 0xff, 0xef, 0xD5));
        colorList.add(new RGB("peachpuff", 0xff, 0xda, 0xB9));
        colorList.add(new RGB("peru", 0xcd, 0x85, 0x3F));
        colorList.add(new RGB("pink", 0xff, 0xc0, 0xCB));
        colorList.add(new RGB("plum", 0xdd, 0xa0, 0xDD));
        colorList.add(new RGB("powderblue", 0xb0, 0xe0, 0xE6));
        colorList.add(new RGB("purple", 0x80, 0x00, 0x80));
        colorList.add(new RGB("red", 0xff, 0x00, 0x00));
        colorList.add(new RGB("rosybrown", 0xbc, 0x8f, 0x8F));
        colorList.add(new RGB("royalblue", 0x41, 0x69, 0xE1));
        colorList.add(new RGB("saddlebrown", 0x8b, 0x45, 0x13));
        colorList.add(new RGB("salmon", 0xfa, 0x80, 0x72));
        colorList.add(new RGB("sandybrown", 0xf4, 0xa4, 0x60));
        colorList.add(new RGB("seagreen", 0x2e, 0x8b, 0x57));
        colorList.add(new RGB("seashell", 0xff, 0xf5, 0xEE));
        colorList.add(new RGB("sienna", 0xa0, 0x52, 0x2D));
        colorList.add(new RGB("silver", 0xc0, 0xc0, 0xC0));
        colorList.add(new RGB("skyblue", 0x87, 0xce, 0xEB));
        colorList.add(new RGB("slateblue", 0x6a, 0x5a, 0xCD));
        colorList.add(new RGB("slategray", 0x70, 0x80, 0x90));
        colorList.add(new RGB("snow", 0xff, 0xfa, 0xFA));
        colorList.add(new RGB("springgreen", 0x00, 0xff, 0x7F));
        colorList.add(new RGB("steelblue", 0x46, 0x82, 0xB4));
        colorList.add(new RGB("tan", 0xd2, 0xb4, 0x8C));
        colorList.add(new RGB("teal", 0x00, 0x80, 0x80));
        colorList.add(new RGB("thistle", 0xd8, 0xbf, 0xD8));
        colorList.add(new RGB("tomato", 0xff, 0x63, 0x47));
        colorList.add(new RGB("turquoise", 0x40, 0xe0, 0xD0));
        colorList.add(new RGB("violet", 0xee, 0x82, 0xEE));
        colorList.add(new RGB("wheat", 0xf5, 0xde, 0xB3));
        colorList.add(new RGB("white", 0xff, 0xff, 0xFF));
        colorList.add(new RGB("whitesmoke", 0xf5, 0xf5, 0xF5));
        colorList.add(new RGB("yellow", 0xff, 0xff, 0x00));
        colorList.add(new RGB("yellowgreen", 0x9a, 0xcd, 0x32));
        return colorList;
    }

    public RGB getColor(String name) {
        ArrayList<RGB> colorList = getColorList();
        for (RGB c : colorList) {
            if (c.getName().equals(name)) {
                return c;
            }
        }
        return null;
    }
}
