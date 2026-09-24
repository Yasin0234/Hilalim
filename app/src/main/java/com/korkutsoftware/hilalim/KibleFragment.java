package com.korkutsoftware.hilalim;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

public class KibleFragment extends Fragment implements SensorEventListener {

    private View compassView, qiblaIndicator;
    private TextView txtDegree, txtLocationInfo;
    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer;

    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private boolean hasGravity = false;
    private boolean hasGeomagnetic = false;

    private float qiblaDegree = 0f;
    private static final float ALPHA = 0.15f; // Low-pass filter factor

    public KibleFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_kible, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        compassView = view.findViewById(R.id.compass_area);
        qiblaIndicator = view.findViewById(R.id.qibla_indicator);
        txtDegree = view.findViewById(R.id.degree_label);
        txtLocationInfo = view.findViewById(R.id.qibla_subtitle);

        view.findViewById(R.id.btn_location).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), LocationActivity.class));
        });

        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        if (accelerometer == null || magnetometer == null) {
            if (txtLocationInfo != null) {
                txtLocationInfo.setText(R.string.qibla_no_sensor);
            }
        }
    }

    private void calculateQibla() {
        if (getActivity() == null) return;
        SharedPreferences prefs = requireActivity().getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE);
        float userLat = prefs.getFloat("last_lat", 0);
        float userLng = prefs.getFloat("last_lng", 0);

        if (userLat != 0 && userLng != 0) {
            double mekkaLat = Math.toRadians(21.422487);
            double mekkaLng = Math.toRadians(39.826206);
            double userLatRad = Math.toRadians(userLat);
            double userLngRad = Math.toRadians(userLng);

            double deltaLng = mekkaLng - userLngRad;
            double y = Math.sin(deltaLng);
            double x = Math.cos(userLatRad) * Math.tan(mekkaLat) - Math.sin(userLatRad) * Math.cos(deltaLng);
            
            double qiblaRad = Math.atan2(y, x);
            qiblaDegree = (float) Math.toDegrees(qiblaRad);
            if (qiblaDegree < 0) qiblaDegree += 360;

            if (txtLocationInfo != null) {
                txtLocationInfo.setText(String.format(Locale.getDefault(), "Mekke Yönü: %.1f°", qiblaDegree));
            }
            
            if (qiblaIndicator != null) {
                qiblaIndicator.setRotation(qiblaDegree);
            }
        } else {
            if (txtLocationInfo != null) {
                txtLocationInfo.setText(R.string.qibla_location_required);
                txtLocationInfo.setOnClickListener(v -> {
                    startActivity(new Intent(getActivity(), LocationActivity.class));
                });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        calculateQibla();
        if (sensorManager != null) {
            if (accelerometer != null) sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
            if (magnetometer != null) sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            gravity = lowPass(event.values, gravity);
            hasGravity = true;
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = lowPass(event.values, geomagnetic);
            hasGeomagnetic = true;
        }

        if (hasGravity && hasGeomagnetic) {
            float[] R = new float[9];
            float[] I = new float[9];
            if (SensorManager.getRotationMatrix(R, I, gravity, geomagnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]);
                azimuth = (azimuth + 360) % 360;

                if (compassView != null) {
                    compassView.setRotation(-azimuth);
                }

                if (txtDegree != null) {
                    txtDegree.setText(String.format(Locale.getDefault(), "%.0f°", azimuth));
                }
            }
        }
    }

    private float[] lowPass(float[] input, float[] output) {
        if (output == null || output.length != input.length) {
            return input.clone();
        }
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}
