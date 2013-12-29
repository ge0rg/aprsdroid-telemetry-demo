package org.aprsdroid.telemetrydemo;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class TelemetrySender extends Activity implements SensorEventListener
{
	// constants for sensor configuration
	// values from http://developer.android.com/reference/android/hardware/Sensor.html
	static final int SENSOR_TYPES[] = { 5, 6, 7, 8 };
	static final String SENSOR_NAMES[] = {
		"Light", "Pressure", "Temp", "Prox"
	};
	static final String SENSOR_UNITS[] = {
		"Lux", "hPa", "deg.C", "cm"
	};

	// the sensors and values are kept in two arrays to allow asynchronous sending
	Sensor sensors[];
	float values[];
	int seq_no = 0;

	// UI elements
	TextView mInfoText = null;
	EditText mCallSsid = null;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mInfoText = (TextView)findViewById(R.id.info);
		mCallSsid = (EditText)findViewById(R.id.callssid);

		sensors = new Sensor[SENSOR_TYPES.length];
		values = new float[SENSOR_TYPES.length];
	}

	@Override
	public void onResume() {
		super.onResume();
		registerSensors();
	}

	@Override
	public void onPause() {
		super.onPause();
		unregisterSensors();
	}

	// button handlers
	public void onStartService(View view) {
		Intent i = new Intent("org.aprsdroid.app.SERVICE");
		startService(i);
	}

	public void onSendParams(View view) {
		String callssid = String.format(":%-9s:", mCallSsid.getText().toString());
		StringBuilder sb_names = new StringBuilder("PARM.");
		StringBuilder sb_units = new StringBuilder("UNIT.");
		StringBuilder sb_eqns = new StringBuilder("EQNS.");
		for (int id = 0; id < SENSOR_TYPES.length; id++)
	       		if (sensors[id] != null) {
				sb_names.append(SENSOR_NAMES[id]);
				sb_names.append(",");

				sb_units.append(SENSOR_UNITS[id]);
				sb_units.append(",");

				sb_eqns.append("0,");
				float scale = sensors[id].getMaximumRange()/255;
				sb_eqns.append(scale);
				sb_eqns.append(",0,");
			}

		sendPacket(callssid + sb_names.toString());
		sendPacket(callssid + sb_units.toString());
		sendPacket(callssid + sb_eqns.toString());
	}

	public void onSendValues(View view) {
		StringBuilder sb = new StringBuilder(String.format("T#%03d,", seq_no++));
		int count = 0;
		for (int id = 0; id < SENSOR_TYPES.length; id++) {
			if (sensors[id] != null) {
				int value = (int)(values[id] * 255 / sensors[id].getMaximumRange());
				sb.append(String.format("%03d", value));
				sb.append(",");
				count++;
			}
		}
		while (count < 5) {
			sb.append("000,");
			count++;
		}
		sb.append("00000000");
		sendPacket(sb.toString());
	}

	// SensorEventListener callbacks
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	public void onSensorChanged(SensorEvent event) {
		for (int id = 0; id < SENSOR_TYPES.length; id++)
			if (sensors[id] == event.sensor) {
				values[id] = event.values[0];
				displayValues();
			}
	}

	// helper methods
	public void sendPacket(String packet) {
		Intent i = new Intent("org.aprsdroid.app.SEND_PACKET");
		if (packet.endsWith(","))
			packet = packet.substring(0, packet.length() - 1);
		Log.d("SENSOR", "TX >>> " + packet);
		i.putExtra("data", packet);
		startService(i);
	}

	public void displayValues() {
		StringBuilder sb = new StringBuilder();
		for (int id = 0; id < SENSOR_TYPES.length; id++)
			if (sensors[id] != null) {
				sb.append(sensors[id].getName());
				sb.append(": ");
				sb.append(values[id]);
				sb.append("\n");
			}
		mInfoText.setText(sb.toString());
	}

	public void registerSensors() {
		SensorManager sm = (SensorManager)getSystemService(SENSOR_SERVICE);
		for (int id = 0; id < SENSOR_TYPES.length; id++) {
			sensors[id] = sm.getDefaultSensor(SENSOR_TYPES[id]);
			if (sensors[id] != null)
				sm.registerListener(this, sensors[id], SensorManager.SENSOR_DELAY_NORMAL);
		}
	}

	public void unregisterSensors() {
		SensorManager sm = (SensorManager)getSystemService(SENSOR_SERVICE);
		for (int id = 0; id < SENSOR_TYPES.length; id++) {
			if (sensors[id] != null)
				sm.unregisterListener(this, sensors[id]);
		}
	}

}
