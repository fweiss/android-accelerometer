package com.example;

import static android.hardware.SensorManager.DATA_X;
import static android.hardware.SensorManager.DATA_Y;
import static android.hardware.SensorManager.DATA_Z;
import static android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
import static android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_LOW;
import static android.hardware.SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM;
import static android.hardware.SensorManager.SENSOR_STATUS_UNRELIABLE;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Displays values from the accelerometer sensor.
 * 
 */
public class Accelerometer
extends Activity
implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private TextView accuracyLabel;
    private TextView xLabel, yLabel, zLabel, absLabel;
    private TextView sensorName;
    private TextView sample;
    private ProgressBar filter;
    private ProgressBar filterBar1;
    protected SensorEventListener sensorEventListener;
    private float currentSample;
    private float currentFilter;
    private Timer samplingTimer;

    private float x, y, z;

    private long lastUpdate = -1;
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);   
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        setContentView(R.layout.main);
        findViews();
        filterBar1.setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String name = accelerometer.getName();
        sensorName.setText(name);

        //int rate = SensorManager.SENSOR_DELAY_NORMAL; // ~ 200-400 msec
        int rate = SensorManager.SENSOR_DELAY_FASTEST; // ~ 10 msec
        sensorManager.registerListener(this, accelerometer, rate);

        samplingTimer = new Timer();
        long delay = 0;
        long period = 83;
        samplingTimer.scheduleAtFixedRate(new Filter(), delay, period);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (samplingTimer != null) {
            samplingTimer.cancel();
        }
    }
	
    @Override // SensorEventListener
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            switch (accuracy) {
                case SENSOR_STATUS_UNRELIABLE:
                    accuracyLabel.setText(R.string.accuracy_unreliable);
                    break;
                case SENSOR_STATUS_ACCURACY_LOW:
                    accuracyLabel.setText(R.string.accuracy_low);
                    break;
                case SENSOR_STATUS_ACCURACY_MEDIUM:
                    accuracyLabel.setText(R.string.accuracy_medium);
                    break;
                case SENSOR_STATUS_ACCURACY_HIGH:
                    accuracyLabel.setText(R.string.accuracy_high);
                    break;
            }
        }
    }

    @Override // SensorEventListener
    //public void onSensorChanged(SensorEvent sensorEvent, float[] values) {
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long curTime = System.currentTimeMillis();
            // only allow one update every 100ms, otherwise updates
            // come way too fast and the phone gets bogged down
            // with garbage collection
            //if (lastUpdate == -1 || (curTime - lastUpdate) > 100) {
            lastUpdate = curTime;

            x = sensorEvent.values[DATA_X];
            y = sensorEvent.values[DATA_Y];
            z = sensorEvent.values[DATA_Z];
            float abs = new Float(Math.sqrt(x*x + y*y + z*z));
            currentSample = x;

            xLabel.setText(String.format("X: %+2.5f", x));
            yLabel.setText(String.format("Y: %+2.5f", y));
            zLabel.setText(String.format("Z: %+2.5f", z));
            absLabel.setText(String.format("ABS: %+2.5f ", abs));
            int progress = 100 * (int) (Math.sqrt(currentFilter * currentFilter));
            filter.setProgress(progress);
            //}
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main_menu, menu);
    	return true;
    }
    @Override
	public boolean onOptionsItemSelected(MenuItem menu) {
    	switch (menu.getItemId()) {
    	case R.id.sensor_list:
    		startActivity(new Intent(this, SensorListActivity.class));
    	}
    	return false;
    }
    protected void findViews() {
    	sensorName = (TextView) findViewById(R.id.name_label);
        accuracyLabel = (TextView) findViewById(R.id.accuracy_label);
        xLabel = (TextView) findViewById(R.id.x_label);
        yLabel = (TextView) findViewById(R.id.y_label);
        zLabel = (TextView) findViewById(R.id.z_label);
        absLabel = (TextView) findViewById(R.id.abs_label);
        sample = (TextView) findViewById(R.id.sample_label);
        filter = (ProgressBar) findViewById(R.id.filter_label);
        filterBar1 = (ProgressBar) findViewById(R.id.filter_bar_1);
    }
    
    protected void displayRawData(float[] values) {
        x = values[DATA_X];
        y = values[DATA_Y];
        z = values[DATA_Z];
        float abs = new Float(Math.sqrt(x*x + y*y + z*z));

        xLabel.setText(String.format("X: %+2.5f", x));
        yLabel.setText(String.format("Y: %+2.5f", y));
        zLabel.setText(String.format("Z: %+2.5f", z));
        absLabel.setText(String.format("ABS: %+2.5f ", abs));
    }

    /**
    * A digital recursive band-pass filter with sampling frequency of 12 Hz,
    * center 3.6 Hz, bandwidth 3 Hz, low cut-off 2.1 Hz, high cut-off 5.1 Hz.
    *
    * Source: http://www.dspguide.com/ch19/3.htm
    */
    class Filter
    extends TimerTask {
        private float a0 = (float) +0.535144118;
        private float a1 = (float) +0.132788237;
        private float a2 = (float) -0.402355882;
        private float b1 = (float) -0.154508496;
        private float b2 = (float) -0.0625;
        private float x_1, x_2, y_1, y_2;

        @Override
        public void run() {
            float x_0 = currentSample;
            float y_0 = a0 * x_0 + a1 * x_1 + a2 * x_2
                    + b1 * y_1 + b2 * y_2;
            currentFilter = y_0;
            x_2 = x_1;
            x_1 = x_0;
            y_2 = y_1;
            y_1 = y_0;
        }
    }
}
