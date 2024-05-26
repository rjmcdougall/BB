package com.richardmcdougall.bb.video;

import com.richardmcdougall.bb.TimeSync;
import com.richardmcdougall.bbcommon.BLog;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.opengl.GLES20;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Extract frames from an MP4 using MediaExtractor, MediaCodec, and GLES.  Put a .mp4 file
 * in "/sdcard/source.mp4" and look for output files named "/sdcard/frame-XX.png".
 */
public class VideoDecoder {
    private static final String TAG = "VideoDecoder";

    // where to find files (note: requires WRITE_EXTERNAL_STORAGE permission)

    private static final int MAX_FRAMES = 10;       // stop extracting after this many

    public String sourceFilename;
    //public int outWidth = 118, outHeight = 46;
    public int outWidth = 230, outHeight = 70;
    private boolean stopRequested = false;
    public Thread decodeThread = null;

    private long videoDuration;

    enum StateType {
        STOPPED,
        DECODING
    }

    ;
    StateType state = StateType.STOPPED;

    public Boolean IsRunning() {
        if (decodeThread != null && !decodeThread.isAlive()) {
            BLog.v(TAG, "IsRunning() found decode thread dead");
            stopRequested = true;
            state = StateType.STOPPED;
            return false;
        }
        return state == StateType.DECODING;
    }

    public void Start(String fname, int xRes, int yRes) throws Throwable {
        BLog.v(TAG, "starting decodeThread");
        Stop();
        stopRequested = false;
        sourceFilename = fname;
        outWidth = xRes;
        outHeight = yRes;
        state = StateType.DECODING;
        decodeThread =
                new Thread(() -> {
                    Thread.currentThread().setName("BB Video Decoder");
                    VideoDecoderThread();
                });

        decodeThread.start();
        BLog.v(TAG, "starte decodeThread");

    }

    public void Stop() {
        BLog.v(TAG, "stopping decodeThread");
        if (decodeThread != null) {
            stopRequested = true;
            while (decodeThread.isAlive()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    BLog.e(TAG, e.getMessage());
                }
            }
        }
        BLog.v(TAG, "stopped decodeThread");
    }

    interface OnFrameReadyCallback {
        public void callbackCall(SimpleImage out);
    }

    OnFrameReadyCallback onFrameReady = null;


    public void VideoDecoderThread() {
        try {
            extractMpegFrames();
        } catch (Throwable th) {
            BLog.e(TAG, "extractMpegFrames failed");
            BLog.e(TAG, "extractMpegFrames thread failed" + th.getMessage());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        BLog.v(TAG, "extractMpegFrames thread exiting");
    }


    /**
     * Extraction from an MP4 to a series of PNG files.
     * <p>
     * We scale the video to 640x480 for the PNG just to demonstrate that we can scale the
     * video with the GPU.  If the input video has a different aspect ratio, we could preserve
     * it by adjusting the GL viewport to get letterboxing or pillarboxing, but generally if
     * you're extracting frames you don't want black bars.
     */
    private void extractMpegFrames() throws IOException {
        MediaCodec codec = null;
        CodecOutputSurface outputSurface = null;
        MediaExtractor extractor = null;
        int saveWidth = outWidth;
        int saveHeight = outHeight;

        while (!stopRequested) {
            try {

                File inputFile = new File(sourceFilename);   // must be an absolute path
                // The MediaExtractor error messages aren't very useful.  Check to see if the input
                // file exists so we can throw a better one if it's not there.
                BLog.v(TAG, "filename: " + sourceFilename);

                if (!inputFile.canRead()) {
                    throw new FileNotFoundException("Unable to read " + inputFile);
                }

                extractor = new MediaExtractor();
                extractor.setDataSource(inputFile.toString());
                int trackIndex = selectTrack(extractor);
                if (trackIndex < 0) {
                    throw new RuntimeException("No video track found in " + inputFile);
                }
                extractor.selectTrack(trackIndex);

                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(inputFile.toString());
                String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long timeInMillisec = Long.parseLong(time);
                BLog.d(TAG, "Video file " + inputFile.toString() + " is " + timeInMillisec / 1000 + "seconds long");
                retriever.release();

                MediaFormat format = extractor.getTrackFormat(trackIndex);

                // Could use width/height from the MediaFormat to get full-size frames.
                outputSurface = new CodecOutputSurface(saveWidth, saveHeight);

                // Create a MediaCodec decoder, and configure it with the MediaFormat from the
                // extractor.  It's very important to use the format from the extractor because
                // it contains a copy of the CSD-0/CSD-1 codec-specific data chunks.
                //String mime = format.getString(MediaFormat.KEY_MIME);
                //codec = MediaCodec.createDecoderByType(mime);

                // DKW note that the NPI would fail to display videos because of an effort to use
                // the hardware codec OMX.rk.video_decoder.avc when allowing automatic selection.
                // Dragonboard used the software codec  so I am setting it explicitly.
                codec = MediaCodec.createByCodecName("OMX.google.h264.decoder");

                codec.configure(format, outputSurface.getSurface(), null, 0);

                BLog.v(TAG, "Dec: Video size is " + format.getInteger(MediaFormat.KEY_WIDTH) + "x" + format.getInteger(MediaFormat.KEY_HEIGHT));
                BLog.v(TAG, "Dec: Duration: " + format.getLong(MediaFormat.KEY_DURATION));
                videoDuration = format.getLong(MediaFormat.KEY_DURATION);

                // dkw note that this does not exist on Dragonboard
                try {
                    BLog.v(TAG, "Dec: Frame Rate: " + format.getInteger(MediaFormat.KEY_FRAME_RATE));
                } catch (Exception e) {
                }
                BLog.v(TAG, "Dec: MIME: " + format.getString(MediaFormat.KEY_MIME));
                BLog.v(TAG, "Dec: Codec: " + codec.getName());

                codec.start();

                // Start the video at the same offset on each board through timesync
                long syncClockMicroseconds = TimeSync.CurrentClockAdjusted() * 1000;
                long seekOffsetMicroseconds = syncClockMicroseconds % videoDuration;
                BLog.d(TAG, "seekTo: " + seekOffsetMicroseconds / 1000000);
                extractor.seekTo(seekOffsetMicroseconds, MediaExtractor.SEEK_TO_CLOSEST_SYNC);


                doExtract(extractor, trackIndex, codec, outputSurface, onFrameReady, this);

            } finally {
                // release everything we grabbed
                BLog.v(TAG, "Exiting extractMpegFrames");
                if (outputSurface != null) {
                    outputSurface.release();
                    outputSurface = null;
                }
                if (codec != null) {
                    codec.stop();
                    codec.release();
                    codec = null;
                }
                if (extractor != null) {
                    extractor.release();
                    extractor = null;
                }
            }
        }

        state = StateType.STOPPED;
    }

    /**
     * Selects the video track, if any.
     *
     * @return the track index, or -1 if no video track is found.
     */
    private int selectTrack(MediaExtractor extractor) {
        // Select the first video track we find, ignore the rest.
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                BLog.v(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                return i;
            }
        }

        return -1;
    }

    private long getVideoDuration() {
        return videoDuration;
    }

    /**
     * Work loop.
     */
    private void doExtract(MediaExtractor extractor, int trackIndex, MediaCodec codec,
                           CodecOutputSurface outputSurface, OnFrameReadyCallback callback,
                           VideoDecoder parent) throws IOException {
        final int TIMEOUT_USEC = 10000;

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        int inputChunk = 0;
        int decodeCount = 0;


        boolean outputDone = false;
        boolean inputDone = false;
        long lastFrameTime = System.currentTimeMillis();
        long lastLogTime = System.currentTimeMillis();


        while (!outputDone && !parent.stopRequested) {

            // Feed more data to the decoder.
            if (!inputDone) {
                int inputBufIndex = codec.dequeueInputBuffer(TIMEOUT_USEC);
                if (inputBufIndex >= 0) {
                    ByteBuffer inputBuf = codec.getInputBuffer(inputBufIndex);

                    // Read the sample data into the ByteBuffer.  This neither respects nor
                    // updates inputBuf's position, limit, etc.
                    int chunkSize = extractor.readSampleData(inputBuf, 0);
                    //BLog.v(TAG,"chuck size" + chunkSize);
                    if (chunkSize < 0) {
                        // End of stream -- send empty frame with EOS flag set.
                        codec.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                        BLog.v(TAG, "sent input EOS");
                    } else {
                        if (extractor.getSampleTrackIndex() != trackIndex) {
                            BLog.w(TAG, "WEIRD: got sample from track " +
                                    extractor.getSampleTrackIndex() + ", expected " + trackIndex);
                        }
                        long presentationTimeUs = extractor.getSampleTime();
                        codec.queueInputBuffer(inputBufIndex, 0, chunkSize, presentationTimeUs, 0 /*flags*/);
                        if ((System.currentTimeMillis() - lastLogTime) > 1000) {
                            //lastLogTime = System.currentTimeMillis();
                            BLog.v(TAG, "submitted frame " + inputChunk + " to dec, size=" + chunkSize + " presentation time: " + presentationTimeUs / 1000000);
                        }

                        inputChunk++;
                        extractor.advance();
                    }
                } else {
                    BLog.v(TAG, "input buffer not available");
                }
            }

            if (!outputDone) {

                int decoderStatus = codec.dequeueOutputBuffer(info, TIMEOUT_USEC);
                if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    // no output available yet
                    BLog.v(TAG, "no output from decoder available");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    // not important for us, since we're using Surface
                    BLog.v(TAG, "decoder output buffers changed");
                } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = codec.getOutputFormat();
                    BLog.v(TAG, "decoder output format changed: " + newFormat);
                } else if (decoderStatus < 0) {
                    throw (new IOException("unexpected result from decoder.dequeueOutputBuffer: " + decoderStatus));
                } else { // decoderStatus >= 0

                    //BLog.v(TAG,"surface decoder given buffer " + decoderStatus + " (size=" + info.size + ")");

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        BLog.v(TAG, "output EOS");
                        outputDone = true;
                    }

                    boolean doRender = (info.size != 0);

                    // As soon as we call releaseOutputBuffer, the buffer will be forwarded
                    // to SurfaceTexture to convert to a texture.  The API doesn't guarantee
                    // that the texture will be available before the call returns, so we
                    // need to wait for the onFrameAvailable callback to fire.
                    codec.releaseOutputBuffer(decoderStatus, doRender);

                    if (doRender) {
                        //BLog.v(TAG,"awaiting decode of frame " + decodeCount);
                        outputSurface.awaitNewImage();
                        outputSurface.drawImage(true);

                        outputSurface.outImage.mPixelBuf.rewind();

                        GLES20.glReadPixels(0, 0, outputSurface.outImage.width, outputSurface.outImage.height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
                                outputSurface.outImage.mPixelBuf);

                        if (callback != null)
                            callback.callbackCall(outputSurface.outImage);

                        long curFrameTime = System.currentTimeMillis();

                        // limit output framerate to 30fps (33ms per frame)
                        if ((curFrameTime - lastFrameTime) < 33) {
                            try {
                                Thread.sleep(33 - (curFrameTime - lastFrameTime));
                            } catch (Throwable er) {
                                BLog.e(TAG, er.getMessage());
                            }
                        }
                        lastFrameTime = curFrameTime;
                        if ((System.currentTimeMillis() - lastLogTime) > 1000) {
                            lastLogTime = System.currentTimeMillis();
                            BLog.v(TAG, "Decoding frame " + decodeCount);
                        }
                        decodeCount++;
                    }
                }
            }
        }

        int numSaved = (MAX_FRAMES < decodeCount) ? MAX_FRAMES : decodeCount;
        BLog.v(TAG, "Saving " + numSaved);
    }

}