package com.example;

/**
 * The input and output fields decouple the filter algorithm.
 * The update method samples input and calculates a new output.
 * The update method should be called at a fixed rate.
 */
public abstract class AbstractFilter {
    public float input;
    public float output;
    public abstract void update();
    public abstract long periodMs();
}
