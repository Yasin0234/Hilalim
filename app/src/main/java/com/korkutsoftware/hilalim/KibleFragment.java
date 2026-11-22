package com.korkutsoftware.hilalim;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.korkutsoftware.hilalim.util.AppCalculators;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

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

        View btnSettings = view.findViewById(R.id.btn_settings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), WelcomeActivity.class);
                intent.putExtra("language_only", true);
                startActivity(intent);
            });
        }

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
        String manualCity = prefs.getString("last_city", null);
        String manualDistrict = prefs.getString("last_district", null);

        if (userLat != 0 && userLng != 0) {
            calculateQiblaForCoords(userLat, userLng);
        } else if (manualCity != null && manualDistrict != null) {
            resolveManualLocationForQibla(manualCity, manualDistrict);
        } else {
            showLocationRequired();
        }
    }

    private void calculateQiblaForCoords(double userLat, double userLng) {
        qiblaDegree = AppCalculators.calculateQiblaBearing(userLat, userLng);

        if (txtLocationInfo != null) {
            txtLocationInfo.setOnClickListener(null);
            txtLocationInfo.setText(getString(R.string.qibla_direction_format, qiblaDegree));
        }

        if (qiblaIndicator != null) {
            qiblaIndicator.setRotation(qiblaDegree);
        }
    }

    private void resolveManualLocationForQibla(String city, String district) {
        Context appContext = requireContext().getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(appContext, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocationName(district + ", " + city + ", Turkey", 1);
                if (addresses != null && !addresses.isEmpty()) {
                    double lat = addresses.get(0).getLatitude();
                    double lng = addresses.get(0).getLongitude();
                    appContext.getSharedPreferences("hilalim_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putFloat("last_lat", (float) lat)
                            .putFloat("last_lng", (float) lng)
                            .apply();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (isAdded()) {
                            calculateQiblaForCoords(lat, lng);
                        }
                    });
                    return;
                }
            } catch (Exception ignored) {
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                if (isAdded()) {
                    showLocationRequired();
                }
            });
        });
    }

    private void showLocationRequired() {
        if (txtLocationInfo != null) {
            txtLocationInfo.setText(R.string.qibla_location_required);
            txtLocationInfo.setOnClickListener(v -> {
                startActivity(new Intent(getActivity(), LocationActivity.class));
            });
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
