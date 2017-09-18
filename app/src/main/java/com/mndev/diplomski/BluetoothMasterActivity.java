package com.mndev.diplomski;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.mndev.diplomski.controller.BluetoothController;

public class BluetoothMasterActivity extends AppCompatActivity {

    private BluetoothController mBluetoothController;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_slave);

        mBluetoothController = new BluetoothController(this, BluetoothController.TYPE_MASTER);
    }

}
