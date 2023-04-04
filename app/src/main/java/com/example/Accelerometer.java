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

    protected ShakeFilterTask shakeFilterTask;

    private Timer samplingTimer;
    private LowPassFilterTask lowPassFilterTask;
    private Timer lowPassFilterTimer;

    private float x, y, z;

    private long lastUpdate = -1;
		
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        findViews();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        filterBar1.setVisibility(View.INVISIBLE);
        shakeFilterTask = new ShakeFilterTask();
    }

    @Override
    protected void onResume() {
        super.onResume();

        String name = accelerometer.getName();
        sensorName.setText(name);

        //int rate = SensorManager.SENSOR_DELAY_NORMAL; // ~ 200-400 msec
        int rate = SensorManager.SENSOR_DELAY_FASTEST; // ~ 10 msec
        sensorManager.registerListener(this, accelerometer, rate);

        shakeFilterTask.start();

        lowPassFilterTask = new LowPassFilterTask();
        lowPassFilterTimer = new Timer();
        lowPassFilterTimer.scheduleAtFixedRate(lowPassFilterTask, 0, lowPassFilterTask.periodMs());
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(this);

        shakeFilterTask.stop();

        // fixme move constructor to create or delete here
        if (lowPassFilterTimer != null) {
            lowPassFilterTimer.cancel();
        }
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

            lowPassFilterTask.inputXYZA(x, y, z, abs);

            xLabel.setText(String.format("X: %+2.2f", lowPassFilterTask.xOutput()));
            yLabel.setText(String.format("Y: %+2.2f", lowPassFilterTask.yOutput()));
            zLabel.setText(String.format("Z: %+2.2f", lowPassFilterTask.zOutput()));
            absLabel.setText(String.format("ABS: %+2.2f ", lowPassFilterTask.aOutput()));

            shakeFilterTask.shakeFilter.input = x;
            float shakeFilterOutput = shakeFilterTask.shakeFilter.output;
            int progress = 100 * (int) Math.sqrt(shakeFilterOutput * shakeFilterOutput);

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
     * A digital low pass filter to smooth out the noise accelerometer signal.
     * Source: http://www.dspguide.com/ch19/2.htm
     */
    class LowPassFilterTask
    extends TimerTask {
        private final LowPassFilter xFilter = new LowPassFilter();
        private final LowPassFilter yFilter = new LowPassFilter();
        private final LowPassFilter zFilter = new LowPassFilter();
        private final LowPassFilter aFilter = new LowPassFilter();

        public void inputXYZA(float x, float y, float z, float a) {
            xFilter.currentInput = x;
            yFilter.currentInput = y;
            zFilter.currentInput = z;
            aFilter.currentInput = a;
        }

        @Override
        public void run() {
            xFilter.update();
            yFilter.update();
            zFilter.update();
            aFilter.update();
        }

        public float xOutput() {
            return xFilter.currentOutput;
        }
        public float yOutput() {
            return yFilter.currentOutput;
        }
        public float zOutput() {
            return zFilter.currentOutput;
        }
        public float aOutput() {
            return aFilter.currentOutput;
        }

        public long periodMs() {
            return xFilter.periodMs();
        }
    }

    private class ShakeFilterTask {
        public Timer timer;
        public ShakeFilter shakeFilter;
        TimerTask timerTask;
        ShakeFilterTask() {
            timer = new Timer();
            shakeFilter = new ShakeFilter();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    shakeFilter.update();
                }
            };
        }
        public void start() {
            long delay = 0;
            long period = 83; // todo get from filter
            timer.scheduleAtFixedRate(timerTask, delay, period);
        }
        public void stop() {
            timer.cancel();
        }
    }
}
