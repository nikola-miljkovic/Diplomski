package com.mndev.diplomski;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
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

public class BluetoothMasterActivity extends AppCompatActivity implements Communicator {

    private BluetoothController mBluetoothController;
    private Thread mConnectionThread;

    private TextView mActualTimestampTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_slave);

        mActualTimestampTV = (TextView)findViewById(R.id.tv_actualts);

        mBluetoothController = new BluetoothController(this, BluetoothController.TYPE_MASTER, this);
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
                            numBytes = mInputStream.read(mBuffer);

                            if (numBytes > 0) {
                                mOutputStream.write(15);
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
