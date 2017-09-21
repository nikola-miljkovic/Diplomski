package com.mndev.diplomski.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

/**
 * Created by milja on 9/21/2017.
 */

public class TestAnimation {
    public int mIteration = 0;
    public int mSurface = 0;
    public int mSize = 0;
    public int mHeight = 0;

    public Paint mPaint;

    public TestAnimation(int width, int height) {
        mSurface = width;
        mSize = (int)Math.ceil(width / 128.0);
        mHeight = height;

        mPaint = new Paint();
        mPaint.setStrokeWidth((float)mSize);
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLUE);
    }

    public void draw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);

        int x = (mIteration % 128) * mSize;
        canvas.drawLine(x, 0, x, mHeight, mPaint);

        mIteration += 1;
    }
}
