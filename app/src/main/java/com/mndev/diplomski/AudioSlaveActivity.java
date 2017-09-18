package com.mndev.diplomski;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.mndev.diplomski.model.AudioParamsModel;
import com.mndev.diplomski.utils.TimeUtils;

import java.util.Date;

import static com.mndev.diplomski.FunctionSurface.SAMPLE_RATE;

public class AudioSlaveActivity extends Activity implements SurfaceHolder.Callback {

    private long[] mTimestampVector;
    private AudioParamsModel mParams;
    private Paint mPaint;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_master);

        mParams = (AudioParamsModel)getIntent().getSerializableExtra(MainActivity.EXTRA_AUDIO_PARAMS);

        mTimestampVector = TimeUtils.getTimeVector(mParams.getTime(), mParams.getInterval(), mParams.getIterations());

        mSurfaceView = (SurfaceView)findViewById(R.id.audio_surface);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        mPaint = new Paint();
        mPaint.setStrokeWidth(8.0f);
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLUE);
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        recordAudio();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }

    void recordAudio() {
        new Thread(new Runnable() {
            private int mIteration = 0;

            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                // buffer size in bytes
                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT);

                if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                    bufferSize = SAMPLE_RATE * 2;
                }

                float drawSize;
                float drawY = (float) mSurfaceView.getHeight() / 2;

                short[] audioBuffer = new short[bufferSize / 2];

                AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize / 2);

                if (record.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e("AUDIO", "Audio Record can't initialize!");
                    return;
                }
                record.startRecording();

                Log.v("AUDIO", "Start recording");

                int timeBuffer = 0;
                long time = System.currentTimeMillis();
                float rms;
                int val = 75;
                boolean isAboveMargin = false;
                FunctionSurface functionSurface = new FunctionSurface(mSurfaceView.getMeasuredHeight(), mSurfaceView.getMeasuredWidth(), val);
                while (mIteration < mParams.getIterations()) {
                    long timeMil = System.currentTimeMillis();
                    int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);

                    rms = 0.0f;
                    for (int i = 0; i < audioBuffer.length; i += 1) {
                        rms += audioBuffer[i] * audioBuffer[i];
                    }

                    rms = 20.0f * (float)Math.log10(Math.sqrt(rms / audioBuffer.length));

                    if (!isAboveMargin && (int)rms * 2 > val) {
                        isAboveMargin = true;

                        long delta = System.currentTimeMillis() - 25 - mTimestampVector[mIteration];
                        Log.d("DELTA", "DELTA: " + delta + " Iteration " + mIteration + " Value " + mTimestampVector[mIteration]);
                        mIteration += 1;
                    } else if (isAboveMargin && (int)rms * 2 < val) {
                        isAboveMargin = false;
                    }
                    functionSurface.addValue(rms);

                    if (System.currentTimeMillis() - 16 > time) {
                        Canvas canvas = mSurfaceHolder.lockCanvas();

                        canvas.drawColor(Color.WHITE);
                        functionSurface.draw(canvas);

                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                        time = System.currentTimeMillis();
                    }
                }

                record.stop();
                record.release();
                Log.d("DELTA", "DONE!");
            }
        }).start();
    }
}