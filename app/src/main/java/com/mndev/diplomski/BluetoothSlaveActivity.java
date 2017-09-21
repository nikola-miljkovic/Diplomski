package com.mndev.diplomski;

import android.bluetooth.BluetoothSocket;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mndev.diplomski.controller.BluetoothController;
import com.mndev.diplomski.utils.TestAnimation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class BluetoothSlaveActivity extends AppCompatActivity implements Communicator, SurfaceHolder.Callback {

    private BluetoothController mBluetoothController;
    private Thread mUpdateThread;
    private BluetoothSocket mSocket;
    private Handler mHandler = new Handler();

    private TextView mNewTimestampTV;
    private TextView mActualTimestampTV;
    private TextView mDeltaTV;
    private TextView mDeltaAvgTV;
    private TextView mIterationTV;
    private TextView mDelayTV;

    private TextView mTerminalTV;

    private Button mRequestTime;
    private Button mStartTest;

    private long mDelta;
    private long mPhi;
    private long mTime;
    private long mRenderTime = 0;

    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private byte[] mBuffer;

    private long mT0 = System.currentTimeMillis();

    private long mTestType = 0;
    private AtomicBoolean mIsTesting = new AtomicBoolean(false);
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private TestAnimation mTestAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_slave);

        mDeltaTV = (TextView)findViewById(R.id.tv_delta);
        mDeltaAvgTV = (TextView)findViewById(R.id.tv_delta_avg);
        mIterationTV = (TextView)findViewById(R.id.tv_iteration);
        mActualTimestampTV = (TextView)findViewById(R.id.tv_actualts);
        mNewTimestampTV = (TextView)findViewById(R.id.tv_newts);
        mDelayTV = (TextView)findViewById(R.id.tv_delay);

        mTerminalTV = (TextView)findViewById(R.id.tv_terminal);

        mRequestTime = (Button)findViewById(R.id.btn_time);
        mStartTest = (Button)findViewById(R.id.btn_test);

        mTestType = getIntent().getLongExtra(MainActivity.EXTRA_TEST, 0);

        mSurfaceView = (SurfaceView)findViewById(R.id.test_surface);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        mBluetoothController = new BluetoothController(this, BluetoothController.TYPE_SLAVE, this);

        mRequestTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestTimeUpdate();
            }
        });

        mStartTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestTestStart();
            }
        });

        mUpdateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (mSocket != null && mSocket.isConnected()) {
                            synchronized (mSocket) {
                                if (mInputStream.available() > 0) {
                                    mInputStream.read(mBuffer);
                                    final long t3 = System.currentTimeMillis();

                                    ByteBuffer buffer = ByteBuffer.wrap(mBuffer);
                                    switch (buffer.getInt()) {
                                        case BluetoothController.MSG_SYNC_RES: {
                                            appendTerminal("Received MSG_SYNC_RES");

                                            final long t1 = buffer.getLong();
                                            final long t2 = buffer.getLong();

                                            mDelta = ((t1 - mT0) + (t2 -t3))/2;
                                            mPhi = (t3 - mT0 - t2 + t1) / 2;

                                            appendTerminal("New time delta: " + mDelta);
                                            appendTerminal("New phi: " + mPhi);
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        mTime = System.currentTimeMillis();
                        if (mTime - 34 > mRenderTime) {
                            BluetoothSlaveActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mNewTimestampTV.setText(String.valueOf(mTime + (mDelta - mPhi)));
                                    mDeltaTV.setText(String.valueOf(mDelta));
                                    mDelayTV.setText(String.valueOf(mPhi));
                                    mActualTimestampTV.setText(String.valueOf(mTime));
                                }
                            });

                            if (mIsTesting.get()) {
                                Canvas canvas = mSurfaceHolder.lockCanvas();
                                mTestAnimation.draw(canvas);
                                mSurfaceHolder.unlockCanvasAndPost(canvas);
                            }

                            mRenderTime = System.currentTimeMillis();
                        }
                    } catch (Exception e) {
                        Log.d("DEBUG", "Input stream was disconnected", e);
                        break;
                    }
                }
            }
        });

        mUpdateThread.start();
    }

    private void requestTestStart() {
        if (mSocket != null) {
            synchronized (mSocket) {
                try {
                    long testTime = 5000 + System.currentTimeMillis() + (mDelta - mPhi);
                    ByteBuffer outBuffer = ByteBuffer.allocate(12);
                    outBuffer.putInt(BluetoothController.MSG_TEST_REQ);
                    outBuffer.putLong(testTime);
                    mOutputStream.write(outBuffer.array());
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (mTestType == 0) {
                                MediaPlayer mPlayer = MediaPlayer.create(BluetoothSlaveActivity.this, R.raw.mario);
                                mPlayer.start();
                            } else {
                                mTestAnimation = new TestAnimation(mSurfaceView.getWidth(), mSurfaceView.getHeight());
                                mIsTesting.set(true);
                            }
                        }
                    }, testTime - (System.currentTimeMillis() + (mDelta - mPhi)));
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private void requestTimeUpdate() {
        if (mSocket != null) {
            synchronized (mSocket) {
                mT0 = System.currentTimeMillis();
                try {
                    mOutputStream.write(BluetoothController.MSG_SYNC_REQ);
                    appendTerminal("Sending MSG_SYNC_REQ");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }

    private void appendTerminal(final String value) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTerminalTV.setText(value + "\n" + mTerminalTV.getText());
            }
        });
    }

    @Override
    public void handleConnection(final BluetoothSocket socket) {
        try {
            mSocket = socket;
            mBuffer = new byte[1024];
            mInputStream = socket.getInputStream();
            mOutputStream = socket.getOutputStream();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mUpdateThread != null && mUpdateThread.isAlive()) {
            mUpdateThread.interrupt();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {

    }
}
