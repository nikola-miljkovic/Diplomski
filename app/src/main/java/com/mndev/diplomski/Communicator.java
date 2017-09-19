package com.mndev.diplomski;

import android.bluetooth.BluetoothSocket;

public interface Communicator {
    void handleConnection(BluetoothSocket socket);
}
