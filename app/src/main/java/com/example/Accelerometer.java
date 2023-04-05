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
    private ProgressBar filter;
    private ProgressBar filterBar1;

    protected SensorEventListener sensorEventListener;

    protected FilterTask shakeFilterTask;
    protected FilterTask xLowPassFilterTask;
    protected FilterTask yLowPassFilterTask;
    protected FilterTask zLowPassFilterTask;
    protected FilterTask aLowPassFilterTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        findViews();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        filterBar1.setVisibility(View.INVISIBLE);

        shakeFilterTask = new FilterTask(new ShakeFilter());
        xLowPassFilterTask = new FilterTask(new LowPassFilter());
        yLowPassFilterTask = new FilterTask(new LowPassFilter());
        zLowPassFilterTask = new FilterTask(new LowPassFilter());
        aLowPassFilterTask = new FilterTask(new LowPassFilter());
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
        xLowPassFilterTask.start();
        yLowPassFilterTask.start();
        zLowPassFilterTask.start();
        aLowPassFilterTask.start();
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener(this);

        shakeFilterTask.stop();
        xLowPassFilterTask.stop();
        yLowPassFilterTask.stop();
        zLowPassFilterTask.stop();
        aLowPassFilterTask.stop();
    }

    protected void findViews() {
        sensorName = (TextView) findViewById(R.id.name_label);
        accuracyLabel = (TextView) findViewById(R.id.accuracy_label);
        xLabel = (TextView) findViewById(R.id.x_label);
        yLabel = (TextView) findViewById(R.id.y_label);
        zLabel = (TextView) findViewById(R.id.z_label);
        absLabel = (TextView) findViewById(R.id.abs_label);
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

    // may need to throttle to 1/100ms to avoid GC overload
    @Override // SensorEventListener
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = sensorEvent.values[DATA_X];
            float y = sensorEvent.values[DATA_Y];
            float z = sensorEvent.values[DATA_Z];
            float abs = (float) Math.sqrt(x*x + y*y + z*z);

            xLowPassFilterTask.filter.input = x;
            yLowPassFilterTask.filter.input = y;
            zLowPassFilterTask.filter.input = z;
            aLowPassFilterTask.filter.input = abs;
            xLabel.setText(String.format("X: %+2.2f", xLowPassFilterTask.filter.output));
            yLabel.setText(String.format("Y: %+2.2f", yLowPassFilterTask.filter.output));
            zLabel.setText(String.format("Z: %+2.2f", zLowPassFilterTask.filter.output));
            absLabel.setText(String.format("ABS: %+2.2f ", aLowPassFilterTask.filter.output));


            shakeFilterTask.filter.input = x;
            float shakeFilterOutput = shakeFilterTask.filter.output;
            int progress = 100 * (int) Math.sqrt(shakeFilterOutput * shakeFilterOutput);
            filter.setProgress(progress);
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
    	if (menu.getItemId() == R.id.sensor_list) {
    		startActivity(new Intent(this, SensorListActivity.class));
    	}
    	return false;
    }

    /**
     * Wrap a recursive filter with a timer so that the calculation of each iteration
     * matches the real-time samples.
     */
    static private class FilterTask {
        public final AbstractFilter filter;
        public final Timer timer;
        private TimerTask timerTask;

        FilterTask(AbstractFilter filter) {
            this.filter = filter;
            timer = new Timer();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    filter.update();
                }
            };
        }
        public void start() {
            long delay = 0;
            long period = filter.periodMs();
            timer.scheduleAtFixedRate(timerTask, delay, period);
        }
        public void stop() {
            timer.cancel();
        }
    }
}
