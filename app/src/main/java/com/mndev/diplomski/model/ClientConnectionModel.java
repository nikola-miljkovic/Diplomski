package com.mndev.diplomski.model;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ClientConnectionModel {

    private BluetoothSocket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private byte[] mBuffer = new byte[1024];


    public ClientConnectionModel(BluetoothSocket mSocket) throws IOException {
        this.mSocket = mSocket;
        mInputStream = mSocket.getInputStream();
        mOutputStream = mSocket.getOutputStream();
    }

    public BluetoothSocket getSocket() {
        return mSocket;
    }

    public void setSocket(BluetoothSocket socket) {
        mSocket = socket;
    }

    public InputStream getInputStream() {
        return mInputStream;
    }

    public void setInputStream(InputStream inputStream) {
        mInputStream = inputStream;
    }

    public OutputStream getOutputStream() {
        return mOutputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        mOutputStream = outputStream;
    }

    public byte[] getBuffer() {
        return mBuffer;
    }

    public void setBuffer(byte[] buffer) {
        mBuffer = buffer;
    }
}
