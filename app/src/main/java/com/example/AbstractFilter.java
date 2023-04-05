package com.example;

public abstract class AbstractFilter {
    public float input;
    public float output;
    public abstract void update();
    public abstract long periodMs();
}
