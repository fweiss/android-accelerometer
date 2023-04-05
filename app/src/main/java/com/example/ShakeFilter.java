package com.example;

/**
 * A digital recursive band-pass filter with sampling frequency of 12 Hz,
 * center 3.6 Hz, bandwidth 3 Hz, low cut-off 2.1 Hz, high cut-off 5.1 Hz.
 *
 * Source: http://www.dspguide.com/ch19/3.htm
 */
@SuppressWarnings("FieldCanBeLocal")
public class ShakeFilter
extends AbstractFilter {
    public final float sampleFrequency = 12;

    private final float a0 = (float) +0.535144118;
    private final float a1 = (float) +0.132788237;
    private final float a2 = (float) -0.402355882;
    private final float b1 = (float) -0.154508496;
    private final float b2 = (float) -0.062500000;

    private float x0, x1, x2;
    private float y0, y1, y2;

    public void update() {
        x0 = input;
        y0 = a0 * x0 + a1 * x1 + a2 * x2
                + b1 * y1 + b2 * y2;
        output = y0;
        x2 = x1;
        x1 = x0;
        y2 = y1;
        y1 = y0;
    }
    public long periodMs() {
        return (long)(1000 / sampleFrequency);
    }
}
