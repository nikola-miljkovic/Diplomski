package com.mndev.diplomski;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.mndev.diplomski.controller.BluetoothController;

import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothSlaveActivity extends AppCompatActivity implements Communicator {

    private BluetoothController mBluetoothController;
    private Thread mConnectionThread;

    private TextView mNewTimestampTV;
    private TextView mActualTimestampTV;
    private TextView mDeltaTV;
    private TextView mDeltaAvgTV;
    private TextView mIterationTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_slave);

        mDeltaTV = (TextView)findViewById(R.id.tv_delta);
        mDeltaAvgTV = (TextView)findViewById(R.id.tv_delta_avg);
        mIterationTV = (TextView)findViewById(R.id.tv_iteration);
        mActualTimestampTV = (TextView)findViewById(R.id.tv_actualts);
        mNewTimestampTV = (TextView)findViewById(R.id.tv_newts);

        mBluetoothController = new BluetoothController(this, BluetoothController.TYPE_SLAVE, this);
    }

    @Override
    public void handleConnection(final BluetoothSocket socket) {
        try {
            mConnectionThread = new Thread(new Runnable() {
                private final BluetoothSocket mSocket = socket;
                private final InputStream mInputStream = socket.getInputStream();
                private final OutputStream mOutputStream = socket.getOutputStream();
                private byte[] mBuffer;

                @Override
                public void run() {
                    mBuffer = new byte[1024];
                    int numBytes; // bytes returned from read()

                    // Keep listening to the InputStream until an exception occurs.
                    while (true) {
                        try {
                            mOutputStream.write(15);

                            numBytes = mInputStream.read(mBuffer);
                            if (numBytes > 0) {
                                Log.d("DEBUG", "Wrks");
                            }
                        } catch (Exception e) {
                            Log.d("DEBUG", "Input stream was disconnected", e);
                            break;
                        }
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        mConnectionThread.start();
    }
}
