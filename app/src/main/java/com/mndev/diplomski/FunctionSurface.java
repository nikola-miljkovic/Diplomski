package com.mndev.diplomski;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

import java.util.LinkedList;
import java.util.Queue;

public class FunctionSurface {

    public static int SAMPLE_RATE = 44100;
    private static float LINE_WIDTH = 4.0f;

    private Queue<Float> values;
    private Paint mFunctionPathPaint;
    private Paint mMarginPaint;
    private int mHeight;
    private int mWidth;
    private int mCapacity;
    private int mMarginValue;

    public FunctionSurface(int height, int width, int marginValue) {
        mWidth = width;
        mCapacity = width / (int)LINE_WIDTH;
        mHeight = height;
        mMarginValue = marginValue;

        values = new LinkedList<Float>();

        mFunctionPathPaint = new Paint();
        mFunctionPathPaint.setStrokeWidth(LINE_WIDTH);
        mFunctionPathPaint.setAntiAlias(true);
        mFunctionPathPaint.setColor(Color.GREEN);
        mFunctionPathPaint.setStyle(Paint.Style.STROKE);

        mMarginPaint = new Paint();
        mMarginPaint.setStrokeWidth(1.0f);
        mMarginPaint.setAntiAlias(true);
        mMarginPaint.setColor(Color.RED);
        mMarginPaint.setAlpha(128);
        mMarginPaint.setStyle(Paint.Style.STROKE);
    }

    public void addValue(float dbValue) {
        if (values.size() == mCapacity) {
            values.remove();
        }

        values.add(dbValue);
    }

    public void draw(Canvas canvas) {
        int counter = 0;

        Path drawPath = new Path();
        drawPath.moveTo(0, mHeight);

        for (float value : values) {
            drawPath.lineTo(counter++ * LINE_WIDTH, mHeight - value * 2.0f);
        }

        canvas.drawPath(drawPath, mFunctionPathPaint);
        canvas.drawLine(0, mHeight - mMarginValue, mWidth, mHeight - mMarginValue, mMarginPaint);
    }
}
