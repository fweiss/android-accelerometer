package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.ListActivity;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class SensorList
extends ListActivity {
	private SensorManager sensorManager;
	static final String[] SAMPLES = new String[] { "alpha", "beta" };
	List<String> sensorNames = Arrays.asList(SAMPLES);
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);   
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		list();
		
        setContentView(R.layout.sensor_list_view);
		setListAdapter(new ArrayAdapter<String>(this, R.layout.sensor_list_item, sensorNames));
    }
	
	private void list() {
		List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
		ArrayList<String> names = new ArrayList<String>();
		for (Sensor sensor : sensors) {
			names.add(sensor.getName());
		}
		sensorNames = names;
	}

}
