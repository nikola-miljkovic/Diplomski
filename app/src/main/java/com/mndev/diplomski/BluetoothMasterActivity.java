package com.mndev.diplomski;

import android.bluetooth.BluetoothSocket;
import android.graphics.Canvas;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.mndev.diplomski.controller.BluetoothController;
import com.mndev.diplomski.model.ClientConnectionModel;
import com.mndev.diplomski.utils.TestAnimation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class BluetoothMasterActivity extends AppCompatActivity implements Communicator, SurfaceHolder.Callback {

    private final ArrayList<ClientConnectionModel> mSockets = new ArrayList<>();
    private BluetoothController mBluetoothController;
    private Thread mUpdateThread;
    private Handler mHandler = new Handler();

    private TextView mActualTimestampTV;
    private TextView mTerminalTV;

    private long mTime = 0;
    private long mRenderTime = 0;

    private long mTestType = 0;
    private AtomicBoolean mIsTesting = new AtomicBoolean(false);
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private TestAnimation mTestAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_master);

        mActualTimestampTV = (TextView)findViewById(R.id.tv_actualts);

        mTerminalTV = (TextView)findViewById(R.id.tv_terminal);

        mBluetoothController = new BluetoothController(this, BluetoothController.TYPE_MASTER, this);
        mTestType = getIntent().getLongExtra(MainActivity.EXTRA_TEST, 0);

        mSurfaceView = (SurfaceView)findViewById(R.id.test_surface);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        mUpdateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    synchronized (mSockets) {
                        for (ClientConnectionModel connection : mSockets) {
                            InputStream inputStream = connection.getInputStream();
                            OutputStream outputStream = connection.getOutputStream();
                            byte[] buffer = connection.getBuffer();

                            try {
                                if (inputStream.available() > 0) {
                                    inputStream.read(buffer);
                                    long t1 = System.currentTimeMillis();

                                    ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);

                                    switch (byteBuffer.getInt()) {
                                        case BluetoothController.MSG_SYNC_REQ: {
                                            ByteBuffer outBuffer = ByteBuffer.allocate(20);
                                            long t2 = System.currentTimeMillis();
                                            outBuffer.putInt(BluetoothController.MSG_SYNC_RES);
                                            outBuffer.putLong(t1);
                                            outBuffer.putLong(t2);

                                            outputStream.write(outBuffer.array());

                                            appendTerminal("Received: MSG_SYNC_REQ; responded with MSG_SYNC_RES.");
                                            break;
                                        }
                                        case BluetoothController.MSG_TEST_REQ: {
                                            long testTime = byteBuffer.getLong();
                                            mHandler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    if (mTestType == 0) {
                                                        MediaPlayer mPlayer = MediaPlayer.create(BluetoothMasterActivity.this, R.raw.mario);
                                                        mPlayer.start();
                                                    } else {
                                                        mTestAnimation = new TestAnimation(mSurfaceView.getWidth(), mSurfaceView.getHeight());
                                                        mIsTesting.set(true);
                                                    }
                                                }
                                            }, testTime - System.currentTimeMillis());
                                        }
                                    }
                                }
                            } catch (Exception exc) {
                                exc.printStackTrace();
                            }
                        }
                    }

                    mTime = System.currentTimeMillis();
                    if (mTime - 34 > mRenderTime) {
                        BluetoothMasterActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
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
                }
            }
        });

        mUpdateThread.start();
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
            mSockets.add(new ClientConnectionModel(socket));
            appendTerminal("Accepted new connection from: " + socket.getRemoteDevice().getName());
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        synchronized (mSockets) {
            for (ClientConnectionModel connection : mSockets) {
                try {
                    connection.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        mSockets.clear();
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
