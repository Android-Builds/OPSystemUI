package com.airbnb.lottie.utils;

public class MeanCalculator {

    /* renamed from: n */
    private int f50n;
    private float sum;

    public void add(float f) {
        this.sum += f;
        this.f50n++;
        int i = this.f50n;
        if (i == Integer.MAX_VALUE) {
            this.sum /= 2.0f;
            this.f50n = i / 2;
        }
    }

    public float getMean() {
        int i = this.f50n;
        if (i == 0) {
            return 0.0f;
        }
        return this.sum / ((float) i);
    }
}
