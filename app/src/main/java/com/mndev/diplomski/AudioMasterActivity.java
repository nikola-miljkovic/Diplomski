package com.mndev.diplomski;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
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

import static com.mndev.diplomski.FunctionSurface.SAMPLE_RATE;

public class AudioMasterActivity extends Activity implements SurfaceHolder.Callback {

    private final Handler mHandler = new Handler();

    private long[] mTimestampVector;
    private AudioParamsModel mParams;
    private Paint mPaint;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private BeepThread mBeepThread;

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

        mBeepThread = new BeepThread(mHandler, mTimestampVector, mParams);
        long delta = mTimestampVector[0] - System.currentTimeMillis();
        Log.d("DELTA", "DELTA " + delta);
        mHandler.postDelayed(mBeepThread, delta);
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

    @Override
    protected void onStop() {
        super.onStop();

        mHandler.removeCallbacks(mBeepThread);
    }

    void recordAudio() {
        new Thread(new Runnable() {
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

                int shortsRead = 0;
                float newX = 0;
                float currentSample;
                float avg = 0.0f;
                float secondsPassed = 0.0f;
                float rms = 0.0f;
                int timeBuffer = 0;
                long time = System.currentTimeMillis();
                long timeDelta = 0;
                FunctionSurface functionSurface = new FunctionSurface(mSurfaceView.getMeasuredHeight(), mSurfaceView.getMeasuredWidth(), 100);
                while (true) {
                    long timeMil = System.currentTimeMillis();
                    int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
                    drawSize = (float) mSurfaceView.getWidth() * 4 / bufferSize;

                    shortsRead += numberOfShort;
                    timeBuffer += 44;
                    if (timeBuffer > 1000) {
                        secondsPassed += 1.0f;
                        timeBuffer = timeBuffer - 1000;
                    }

                    rms = 0.0f;
                    for (int i = 0; i < audioBuffer.length; i += 1) {
                        rms += audioBuffer[i] * audioBuffer[i];
                    }

                    rms = 20.0f * (float)Math.log10(Math.sqrt(rms / audioBuffer.length));
                    functionSurface.addValue(rms);

                    if (System.currentTimeMillis() - 16 > time) {
                        Canvas canvas = mSurfaceHolder.lockCanvas();

                        canvas.drawColor(Color.WHITE);
                        functionSurface.draw(canvas);

                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                        time = System.currentTimeMillis();
                    }
                }

                /*record.stop();
                record.release();

                Log.v("OUTPUTS", String.format("Recording stopped. Samples read: %d", shortsRead));*/
            }
        }).start();
    }

    private class BeepThread implements Runnable {
        private Handler mHandler;
        private ToneGenerator mTone = new ToneGenerator(AudioManager.STREAM_ALARM, 75);

        long[] mVector;
        AudioParamsModel mParams;
        int mIteration = 0;

        public BeepThread(Handler handler, long[] vector, AudioParamsModel params) {
            mHandler = handler;
            mVector = vector;
            mParams = params;
        }

        @Override
        public void run() {
            if (mIteration == mVector.length) {
                return;
            }

            mTone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);

            long delta = System.currentTimeMillis() - mVector[mIteration];
            Log.d("OUTPUT", "DELTA: " + delta);
            mIteration += 1;
            mHandler.postDelayed(this, mParams.getInterval() - delta);
        }
    }
}
