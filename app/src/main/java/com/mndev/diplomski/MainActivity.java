package com.mndev.diplomski;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.mndev.diplomski.controller.BluetoothController;
import com.mndev.diplomski.model.AudioParamsModel;
import com.mndev.diplomski.utils.TimeUtils;

import java.util.Date;

public class MainActivity extends Activity {
    boolean mShouldContinue = true;

    public static final String EXTRA_AUDIO_PARAMS = "START_TIME";
    public static final String EXTRA_TEST = "TEST";
    public static int TestType = 0;

    private TextView mStartTime;
    private TextView mInterval;
    private TextView mIterations;
    private Spinner mFunctionSpinner;
    private Spinner mTestSpinner;

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

        mFunctionSpinner = (Spinner)findViewById(R.id.functionSpinner);
        mTestSpinner = (Spinner)findViewById(R.id.testSpinner);

        mMasterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                if (mFunctionSpinner.getSelectedItemId() == 0) {
                    intent = new Intent(getBaseContext(), AudioMasterActivity.class);
                    intent.putExtra(EXTRA_AUDIO_PARAMS, new AudioParamsModel(TimeUtils.textToDateTime(mStartTime.getText()),
                            Integer.parseInt(mInterval.getText().toString()),
                            Integer.parseInt(mIterations.getText().toString())));
                } else if (mFunctionSpinner.getSelectedItemId() == 1) {
                    intent = new Intent(getBaseContext(), AudioSharedActivity.class);
                    intent.putExtra(EXTRA_AUDIO_PARAMS, new AudioParamsModel(TimeUtils.textToDateTime(mStartTime.getText()),
                            Integer.parseInt(mInterval.getText().toString()),
                            Integer.parseInt(mIterations.getText().toString()),
                            BluetoothController.TYPE_MASTER));
                } else {
                    intent = new Intent(getBaseContext(), BluetoothMasterActivity.class);
                }
                intent.putExtra(EXTRA_TEST, mTestSpinner.getSelectedItemId());
                startActivity(intent);
            }
        });

        mSlaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;
                if (mFunctionSpinner.getSelectedItemId() == 0) {
                    intent = new Intent(getBaseContext(), AudioSlaveActivity.class);
                    intent.putExtra(EXTRA_AUDIO_PARAMS, new AudioParamsModel(TimeUtils.textToDateTime(mStartTime.getText()),
                            Integer.parseInt(mInterval.getText().toString()),
                            Integer.parseInt(mIterations.getText().toString())));
                } else if (mFunctionSpinner.getSelectedItemId() == 1) {
                    intent = new Intent(getBaseContext(), AudioSharedActivity.class);
                    intent.putExtra(EXTRA_AUDIO_PARAMS, new AudioParamsModel(TimeUtils.textToDateTime(mStartTime.getText()),
                            Integer.parseInt(mInterval.getText().toString()),
                            Integer.parseInt(mIterations.getText().toString()),
                            BluetoothController.TYPE_SLAVE));
                } else {
                    intent = new Intent(getBaseContext(), BluetoothSlaveActivity.class);
                }
                intent.putExtra(EXTRA_TEST, mTestSpinner.getSelectedItemId());
                startActivity(intent);
            }
        });

        Date currentTime = new Date();
        mStartTime.setText(TimeUtils.dateTimeToText(currentTime));
        mInterval.setText("500");
        mIterations.setText("10");
    }
}
