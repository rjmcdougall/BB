package com.richardmcdougall.bb;

import java.nio.ByteBuffer;

/**
 * Created by Jonathan on 8/5/2017.
 */

public class SimpleImage {
    public ByteBuffer mPixelBuf;
    public int width, height;

    public SimpleImage dup() {
        SimpleImage r = new SimpleImage();
        r.mPixelBuf = ByteBuffer.allocate(mPixelBuf.array().length);
        r.mPixelBuf.rewind();
        r.mPixelBuf.put(mPixelBuf);
        r.mPixelBuf.rewind();
        r.width = width;
        r.height = height;
        return r;
    }
}
