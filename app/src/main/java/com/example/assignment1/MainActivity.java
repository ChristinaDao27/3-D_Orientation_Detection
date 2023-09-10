package com.example.assignment1;

import androidx.appcompat.app.AppCompatActivity;

//import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;
    private Sensor magnetometer;
    float alpha = (float) 0.98;
    private EditText editTextPitchG, editTextRollG, editTextYaw;
    private TextView textViewPitch, textViewRoll, textViewYaw;
    private Button sensorButton, stopButton;
    private long gTimestamp = 0;
    private float pitchAcc, rollAcc, yawMag;
    private float acclx = 0, accly = 0, acclz = 0;
    private float gyrox = 0, gyroy = 0, gyroz = 0;
    private float magx = 0, magy = 0, magz = 0;
    private Handler myHandler = new Handler();

    //@SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager =  (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = (Sensor)  sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        magnetometer = (Sensor) sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        editTextPitchG = (EditText) findViewById(R.id.editTextPitchG);
        editTextRollG = (EditText) findViewById(R.id.editTextRollG);
        editTextYaw = (EditText) findViewById(R.id.editTextYaw);

        textViewPitch = (TextView) findViewById(R.id.textViewPitch);
        textViewRoll = (TextView) findViewById(R.id.textViewRoll);
        textViewYaw = (TextView) findViewById(R.id.textViewYaw);

        sensorButton = (Button) findViewById(R.id.sensorButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        sensorButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.sensorButton:
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
                sensorManager.registerListener(this, magnetometer,SensorManager.SENSOR_DELAY_FASTEST);
                sensorButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
                break;
            case R.id.stopButton:
                sensorButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.GONE);
                sensorManager.unregisterListener(this, accelerometer);
                sensorManager.unregisterListener(this, gyroscope);
                sensorManager.unregisterListener(this, magnetometer);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] gyroInstant = new float[3];

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            acclx = event.values[0];
            accly = event.values[1];
            acclz = event.values[2];
        }

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            for (int i = 0; i < 3; i++) {
                gyroInstant[i] = event.values[i];
            }

            if (gTimestamp <= 0) {
                gTimestamp = event.timestamp;
            }
            long dT = event.timestamp - gTimestamp;
            gTimestamp = event.timestamp;

            dT *= 0.000000001f; // Convert NanoS to S

            // Getting Pitch Roll and Yaw from Gyroscope measurements
            gyrox = (float) (gyrox + (gyroInstant[0] * dT * 57.3)) % 360;
            gyroy = (float) (gyroy + (gyroInstant[1] * dT * 57.3)) % 360;
            gyroz = (float) (gyroz + (gyroInstant[2] * dT * 57.3)) % 360;

        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magx = event.values[0];
            magy = event.values[1];
            magz = event.values[2];
        }

        // Algorithm for Pitch and Roll from Accelerometer
        pitchAcc = (float) (-(Math.atan2(acclz, accly) * 180) / Math.PI);
        rollAcc = (float) ((Math.atan2(acclx, accly) * 180) / Math.PI);

        // Algorithm for Yaw from magnetometer
        if((magx > 0 && magz > 0) || (magx < 0 && magz < 0) || (magx > 0 && magz < 0)){
            yawMag = (float) (((Math.atan2(magz, magx) * 180) / Math.PI) + 90);
        }
        else if (magz == 0) {
            if(magx < 0){
                yawMag = -90;
            }
            else{
                yawMag = 90;
            }
        }
        else if (magx < 0 && magz > 0) {
            yawMag = (float) -(180 + ((Math.atan2(magx, magz) * 180) / Math.PI));
        }
        else{
            yawMag = 0;
        }


        // Filter Results
        DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);
        gyrox = Float.parseFloat(df.format((alpha * gyrox) + ((1 - alpha) * pitchAcc)));
        gyroy = Float.parseFloat(df.format((alpha * gyroy) + ((1 - alpha) * rollAcc)));
        gyroz = Float.parseFloat(df.format((alpha * gyroz) + ((1 - alpha) * yawMag)));

        DisplayUI myDisplay = new DisplayUI(gyrox, gyroy, gyroz);
        myHandler.post(myDisplay);
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, gyroscope);
        sensorManager.unregisterListener(this, magnetometer);
        sensorButton.setVisibility(View.VISIBLE);
        stopButton.setVisibility(View.GONE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        //sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        //sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        //sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private class DisplayUI implements Runnable{
        float finalPitch, finalRoll, finalYaw;

        public DisplayUI(float pitch, float roll, float yaw){
            this.finalPitch = pitch;
            this.finalRoll = roll;
            this.finalYaw = yaw;
        }

        @Override
        public void run() {
            editTextPitchG.setText(new Float(finalPitch).toString());
            editTextRollG.setText(new Float(finalRoll).toString());
            editTextYaw.setText(new Float(finalYaw).toString());
        }
    }
}


