package com.mndev.diplomski;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.mndev.diplomski.model.AudioParamsModel;
import com.mndev.diplomski.utils.TimeUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends Activity {
    boolean mShouldContinue = true;

    public static final String EXTRA_AUDIO_PARAMS = "START_TIME";

    private TextView mStartTime;
    private TextView mInterval;
    private TextView mIterations;
    private Spinner mSpinner;

    private Button mMasterButton;
    private Button mSlaveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMasterButton = (Button)findViewById(R.id.btn_master);
        mSlaveButton = (Button)findViewById(R.id.btn_slave);
        mStartTime = (TextView)findViewById(R.id.start_time);
        mInterval = (TextView)findViewById(R.id.interval);
        mIterations = (TextView)findViewById(R.id.iterations);
        mSpinner = (Spinner)findViewById(R.id.functionSpinner);

        mMasterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                if (mSpinner.getSelectedItemId() == 0) {
                    intent = new Intent(getBaseContext(), AudioMasterActivity.class);
                    intent.putExtra(EXTRA_AUDIO_PARAMS, new AudioParamsModel(TimeUtils.textToDateTime(mStartTime.getText()),
                            Integer.parseInt(mInterval.getText().toString()),
                            Integer.parseInt(mIterations.getText().toString())));
                } else {
                    intent = new Intent(getBaseContext(), BluetoothMasterActivity.class);
                }
                startActivity(intent);
            }
        });

        mSlaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                if (mSpinner.getSelectedItemId() == 0) {
                    intent = new Intent(getBaseContext(), AudioSlaveActivity.class);
                    intent.putExtra(EXTRA_AUDIO_PARAMS, new AudioParamsModel(TimeUtils.textToDateTime(mStartTime.getText()),
                            Integer.parseInt(mInterval.getText().toString()),
                            Integer.parseInt(mIterations.getText().toString())));
                } else {
                    intent = new Intent(getBaseContext(), BluetoothSlaveActivity.class);
                }
                startActivity(intent);
            }
        });
    }


}