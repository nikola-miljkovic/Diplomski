package com.mndev.diplomski;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mndev.diplomski.model.AudioParamsModel;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static com.mndev.diplomski.FunctionSurface.SAMPLE_RATE;

public class AudioSharedActivity extends Activity implements SurfaceHolder.Callback {

    public static int STATE_LISTENER = 0;
    public static int STATE_INITIATOR = 1;

    private long[] mTimestampVector;
    private int mPingCount;
    private int mExpectedPingCount;
    private int mRole = STATE_LISTENER; // 0 - not started 1 - initiator - 2 - listener
    private AudioParamsModel mParams;
    private Paint mPaint;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private MediaPlayer mPlayer;
    private Handler mHandler = new Handler();
    private ToneGenerator mTone = new ToneGenerator(AudioManager.STREAM_ALARM, 80);

    private TextView mNewTimestampTV;
    private TextView mActualTimestampTV;
    private TextView mDeltaTV;
    private TextView mEmitDelayTV;
    private TextView mIterationTV;
    private TextView mDeltaMinTV;
    private TextView mIntervalAvgTV;

    private AtomicLong mEmittedTime = new AtomicLong(0);
    private AtomicBoolean mHasEmitted = new AtomicBoolean(false);

    private Button mSharedButton;

    private Thread mBackgroundThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_shared);

        mDeltaTV = (TextView)findViewById(R.id.tv_delta);
        mEmitDelayTV = (TextView)findViewById(R.id.tv_emit_delay);
        mDeltaMinTV = (TextView)findViewById(R.id.tv_delta_min);
        mIntervalAvgTV = (TextView)findViewById(R.id.tv_interval_avg);
        mIterationTV = (TextView)findViewById(R.id.tv_iteration);
        mActualTimestampTV = (TextView)findViewById(R.id.tv_actualts);
        mNewTimestampTV = (TextView)findViewById(R.id.tv_newts);

        mParams = (AudioParamsModel)getIntent().getSerializableExtra(MainActivity.EXTRA_AUDIO_PARAMS);
        mPingCount = 0;
        mExpectedPingCount = mParams.getIterations() * 2 + 1;

        mTimestampVector = new long[mParams.getIterations() * 2 + 1];

        mSurfaceView = (SurfaceView)findViewById(R.id.audio_surface);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        mPaint = new Paint();
        mPaint.setStrokeWidth(8.0f);
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.BLUE);

        mSharedButton = (Button)findViewById(R.id.btn_shared);
        mSharedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRole = STATE_INITIATOR;
                playPing();
            }
        });
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
        if (mBackgroundThread != null) {
            mBackgroundThread.interrupt();
        }
    }

    public void playPing() {
        mEmittedTime.set(System.currentTimeMillis());
        mHasEmitted.set(true);
        mTone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
    }

    void recordAudio() {
        mBackgroundThread = new Thread(new Runnable() {
            private int mIteration = 0;
            private long mDeltaAvg;
            private long mDeltaSum = 0;
            private long mDelta = 0;
            private long mIntervalAvg = 0;
            private long mIntervalSum = 0;
            private long mIntervalTime = 0;
            private long mIntervalDelta = 0;

            private long mEmitDelay = 0;
            private long mEmitSum = 0;
            private long mEmitAvg = 0;

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
                while (!Thread.currentThread().isInterrupted()) {
                    iterationTime = System.currentTimeMillis();
                    int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);

                    rms = 0.0f;
                    for (int i = 0; i < audioBuffer.length; i += 1) {
                        rms += audioBuffer[i] * audioBuffer[i];
                    }

                    rms = 20.0f * (float)Math.log10(Math.sqrt(rms / audioBuffer.length));

                    if (mPingCount < mExpectedPingCount) {
                        if (!isAboveMargin && (int)rms * 2 > val) {
                            isAboveMargin = true;

                            if (mHasEmitted.get()) {
                                long emitValue = mEmittedTime.get();
                                mEmitDelay = iterationTime - emitValue;
                                mHasEmitted.set(false);
                                mTimestampVector[mPingCount] = emitValue;

                                if (mPingCount > 1) {
                                    mEmitSum += mEmitDelay;
                                    mEmitAvg = mEmitSum / (mPingCount / 2);
                                }
                            } else {
                                mTimestampVector[mPingCount] = iterationTime;
                            }


                            if (mRole == STATE_LISTENER && mPingCount == 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mSharedButton.setEnabled(false);
                                    }
                                });
                            } else if (mRole == STATE_LISTENER && mPingCount >= 2 && mPingCount % 2 == 1) {
                                mIntervalSum += (mTimestampVector[mPingCount] - mTimestampVector[mPingCount - 2]);
                                mIntervalAvg = mIntervalSum / (mPingCount / 2);
                                mDelta = mTimestampVector[mPingCount] - mTimestampVector[mPingCount - 2] - mParams.getInterval() * 2;
                            } else if (mRole == STATE_INITIATOR && mPingCount >= 2 && mPingCount % 2 == 0) {
                                mIntervalSum += (mTimestampVector[mPingCount] - mTimestampVector[mPingCount - 2]);
                                mIntervalAvg = mIntervalSum / (mPingCount / 2);
                                mDelta = mTimestampVector[mPingCount] - mTimestampVector[mPingCount - 2] - mParams.getInterval() * 2;
                            }

                            if (mRole == STATE_INITIATOR && mPingCount % 2 == 1 ||
                                    mRole == STATE_LISTENER && mPingCount % 2 == 0) {
                                mHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        playPing();
                                    }
                                }, mParams.getInterval() - mEmitDelay - mDelta / 2);
                            }

                            mPingCount += 1;
                            mIteration = mPingCount / 2;

                            if (mPingCount == mExpectedPingCount) {
                                if (mRole == STATE_INITIATOR) {
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            MediaPlayer mPlayer = MediaPlayer.create(AudioSharedActivity.this, R.raw.mario);
                                            mPlayer.start();
                                        }
                                    }, mParams.getInterval() - mEmitDelay - mDelta / 2);
                                } else {
                                    mHandler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            MediaPlayer mPlayer = MediaPlayer.create(AudioSharedActivity.this, R.raw.mario);
                                            mPlayer.start();
                                        }
                                    }, mParams.getInterval() - mEmitDelay - mDelta / 2);
                                }
                            }
                        } else if (isAboveMargin && (int)rms * 2 < val) {
                            isAboveMargin = false;
                        }
                    }

                    functionSurface.addValue(rms);

                    if (System.currentTimeMillis() - 16 > time) {
                        Canvas canvas = mSurfaceHolder.lockCanvas();

                        if (canvas != null) {
                            canvas.drawColor(Color.WHITE);
                            functionSurface.draw(canvas);

                            mSurfaceHolder.unlockCanvasAndPost(canvas);
                            time = System.currentTimeMillis();

                            AudioSharedActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    long currentTime = System.currentTimeMillis();
                                    mNewTimestampTV.setText(String.valueOf(currentTime - mDelta - mIntervalDelta));
                                    mActualTimestampTV.setText(String.valueOf(currentTime));
                                    mDeltaTV.setText(String.valueOf(mDelta));
                                    mEmitDelayTV.setText(String.valueOf(mEmitDelay));
                                    mIntervalAvgTV.setText(String.valueOf(mIntervalAvg));
                                    mIterationTV.setText(String.valueOf(mIteration));
                                }
                            });
                        }
                    }
                }

                /*record.stop();
                record.release();
                Log.d("DELTA", "DONE!");*/
            }
        });

        mBackgroundThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mPlayer != null) {
            mPlayer.stop();
        }
    }
}