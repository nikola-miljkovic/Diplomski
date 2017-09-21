package com.mndev.diplomski;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.mndev.diplomski.model.AudioParamsModel;
import com.mndev.diplomski.utils.TimeUtils;

import static com.mndev.diplomski.FunctionSurface.SAMPLE_RATE;

public class AudioSlaveActivity extends Activity implements SurfaceHolder.Callback {

    private long[] mTimestampVector;
    private AudioParamsModel mParams;
    private Paint mPaint;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private MediaPlayer mPlayer;
    private Handler mHandler = new Handler();

    private TextView mNewTimestampTV;
    private TextView mActualTimestampTV;
    private TextView mDeltaTV;
    private TextView mDeltaAvgTV;
    private TextView mIterationTV;
    private TextView mDeltaMinTV;
    private TextView mIntervalAvgTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_slave);

        mDeltaTV = (TextView)findViewById(R.id.tv_delta);
        mDeltaAvgTV = (TextView)findViewById(R.id.tv_delta_avg);
        mDeltaMinTV = (TextView)findViewById(R.id.tv_delta_min);
        mIntervalAvgTV = (TextView)findViewById(R.id.tv_interval_avg);
        mIterationTV = (TextView)findViewById(R.id.tv_iteration);
        mActualTimestampTV = (TextView)findViewById(R.id.tv_actualts);
        mNewTimestampTV = (TextView)findViewById(R.id.tv_newts);

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
            private long mDelta;
            private long mDeltaAvg;
            private long mDeltaSum = 0;
            private long mDeltaMin = 0;
            private long mIntervalAvg = 0;
            private long mIntervalSum = 0;
            private long mIntervalTime = mTimestampVector[0];

            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO);

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
                long iterationTime;
                float rms;
                int val = 110;
                int valOverhead = 60;
                boolean isAboveMargin = false;
                FunctionSurface functionSurface = new FunctionSurface(mSurfaceView.getMeasuredHeight(), mSurfaceView.getMeasuredWidth(), val);
                while (true) {
                    iterationTime = System.currentTimeMillis();
                    int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);

                    rms = 0.0f;
                    for (int i = 0; i < audioBuffer.length; i += 1) {
                        rms += audioBuffer[i] * audioBuffer[i];
                    }

                    rms = 20.0f * (float)Math.log10(Math.sqrt(rms / audioBuffer.length));

                    if (mIteration < mParams.getIterations()) {
                        if (!isAboveMargin && (int)rms * 2 > val) {
                            isAboveMargin = true;

                            mIntervalSum += Math.abs(iterationTime - mIntervalTime);
                            mIntervalAvg = mIntervalSum / (mIteration + 1);
                            mIntervalTime = iterationTime;
                            mDelta = iterationTime - mTimestampVector[mIteration];
                            mDeltaSum += mDelta;
                            mDeltaAvg = mDeltaSum / (mIteration + 1);

                            if (mDeltaMin == 0) {
                                mDeltaMin = mDelta;
                            } else if (mDeltaMin > 0) {
                                mDeltaMin = Math.min(mDelta, mDeltaMin);
                            } else {
                                mDeltaMin = Math.max(mDelta, mDeltaMin);
                            }

                            mIteration += 1;

                            if (mIteration == mParams.getIterations()) {
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        MediaPlayer mPlayer = MediaPlayer.create(AudioSlaveActivity.this, R.raw.mario);
                                        mPlayer.start();
                                    }
                                }, mTimestampVector[mIteration - 1] + 5000 - (System.currentTimeMillis() - mDeltaMin + mIntervalAvg));
                            }
                        } else if (isAboveMargin && (int)rms * 2 < val) {
                            isAboveMargin = false;
                        }
                    }

                    functionSurface.addValue(rms);

                    if (System.currentTimeMillis() - 16 > time) {
                        Canvas canvas = mSurfaceHolder.lockCanvas();

                        canvas.drawColor(Color.WHITE);
                        functionSurface.draw(canvas);

                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                        time = System.currentTimeMillis();

                        AudioSlaveActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                long currentTime = System.currentTimeMillis();
                                mNewTimestampTV.setText(String.valueOf(currentTime - mDeltaMin + mIntervalAvg * 2));
                                mActualTimestampTV.setText(String.valueOf(currentTime));
                                mDeltaTV.setText(String.valueOf(mDelta));
                                mDeltaAvgTV.setText(String.valueOf(mDeltaAvg));
                                mDeltaMinTV.setText(String.valueOf(mDeltaMin));
                                mIntervalAvgTV.setText(String.valueOf(mIntervalAvg));
                                mIterationTV.setText(String.valueOf(mIteration));
                            }
                        });
                    }
                }

                /*record.stop();
                record.release();
                Log.d("DELTA", "DONE!");*/
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mPlayer != null) {
            mPlayer.stop();
        }
    }
}