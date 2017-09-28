package com.mndev.diplomski.model;

import java.io.Serializable;
import java.util.Date;

public class AudioParamsModel implements Serializable {
    private Date mTime;
    private int mInterval;
    private int mIterations;
    private int mType;

    public AudioParamsModel(Date time, int interval, int iterations) {
        this.mTime = time;
        this.mInterval = interval;
        this.mIterations = iterations;
    }

    public AudioParamsModel(Date time, int interval, int iterations, int type) {
        this.mTime = time;
        this.mInterval = interval;
        this.mIterations = iterations;
        this.mType = type;
    }

    public Date getTime() {
        return mTime;
    }

    public void setTime(Date time) {
        this.mTime = time;
    }

    public int getInterval() {
        return mInterval;
    }

    public void setInterval(int interval) {
        this.mInterval = interval;
    }

    public int getIterations() {
        return mIterations;
    }

    public void setIterations(int iterations) {
        this.mIterations = iterations;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        mType = type;
    }
}
