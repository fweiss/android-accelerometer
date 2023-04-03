package com.example;

/**
 * A digital low pass filter to smooth out the noise accelerometer signal.
 * Source: http://www.dspguide.com/ch19/2.htm
 */
public class LowPassFilter {
    public float currentInput;
    public float currentOutput;

    public final float sampleFrequency = 100;

    // for testing, compute the coofficients
    private double fc = 0.02;
    private double x = Math.exp(- 2 * Math.PI * fc);
    private final float a0 = (float) Math.pow(1 - x, 4);
    private final float b1 = (float) (4 * x);
    private final float b2 = (float) (- 6 * Math.pow(x, 2));
    private final float b3 = (float) (4 * Math.pow(x, 3));
    private final float b4 = (float) (- Math.pow(x, 4));

    private float x0;
    private float y0, y1, y2, y3, y4;

    public void update() {
        x0 = currentInput;

        // for extreme narrow-band LPF, seed to speed up convergence
        if (y0 == 0) {
            y0 = y1 = y2 = y3 = y4 = currentInput;
        }
        y0 = a0 * x0
                + b1 * y1 + b2 * y2 + b3 * y3 + b4 * y4;
        y4 = y3;
        y3 = y2;
        y2 = y1;
        y1 = y0;
        currentOutput = y0;
    }

    public long periodMs() {
        return (long)(1000 / sampleFrequency);
    }
}
