package com.korkutsoftware.hilalim;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.korkutsoftware.hilalim.util.AppCalculators;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class KibleFragment extends Fragment implements SensorEventListener {

    private View compassView, qiblaIndicator;
    private TextView txtDegree, txtLocationInfo, txtQiblaStatus, txtCalibrationTitle, txtCalibrationDesc;
    private MaterialCardView calibrationCard;
    private SensorManager sensorManager;
    private Sensor accelerometer, magnetometer;

    private float[] gravity = new float[3];
    private float[] geomagnetic = new float[3];
    private boolean hasGravity = false;
    private boolean hasGeomagnetic = false;

    private float qiblaDegree = 0f;
    private float magneticDeclination = 0f;
    private boolean wasAligned = false;
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
        txtQiblaStatus = view.findViewById(R.id.txt_qibla_status);
        calibrationCard = view.findViewById(R.id.calibration_card);
        txtCalibrationTitle = view.findViewById(R.id.txt_calibration_title);
        txtCalibrationDesc = view.findViewById(R.id.txt_calibration_desc);

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
        GeomagneticField geoField = new GeomagneticField(
                (float) userLat, (float) userLng, 0f, System.currentTimeMillis());
        magneticDeclination = geoField.getDeclination();

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
            float[] rotationMatrix = new float[9];
            float[] inclinationMatrix = new float[9];
            if (SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, gravity, geomagnetic)) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(rotationMatrix, orientation);
                float magneticAzimuth = (float) Math.toDegrees(orientation[0]);
                float azimuth = AppCalculators.applyMagneticDeclination(magneticAzimuth, magneticDeclination);

                if (compassView != null) {
                    compassView.setRotation(-azimuth);
                }

                if (txtDegree != null) {
                    txtDegree.setText(String.format(Locale.getDefault(), "%.0f°", azimuth));
                }

                if (txtQiblaStatus != null && getContext() != null && qiblaDegree > 0f) {
                    float diff = Math.abs(((azimuth - qiblaDegree + 540f) % 360f) - 180f);
                    if (diff <= 4f) {
                        txtQiblaStatus.setText(R.string.qibla_aligned);
                        txtQiblaStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary));
                        if (!wasAligned) {
                            wasAligned = true;
                            vibrateAligned();
                        }
                    } else {
                        if (diff > 6f) {
                            wasAligned = false;
                        }
                        txtQiblaStatus.setText(R.string.qibla_turn_hint);
                        txtQiblaStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted));
                    }
                }
            }
        }
    }

    private void vibrateAligned() {
        Context ctx = getContext();
        if (ctx == null) return;
        Vibrator vibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(25);
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
        if (!isAdded() || getContext() == null || sensor == null) return;
        if (sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD) return;

        boolean lowAccuracy = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE
                || accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW;
        if (calibrationCard != null) {
            int strokeColor = ContextCompat.getColor(
                    requireContext(),
                    lowAccuracy ? R.color.md_theme_light_error : R.color.card_stroke
            );
            float density = getResources().getDisplayMetrics().density;
            calibrationCard.setStrokeColor(strokeColor);
            calibrationCard.setStrokeWidth((int) ((lowAccuracy ? 2f : 1f) * density));
        }
        if (txtCalibrationTitle != null) {
            txtCalibrationTitle.setText(lowAccuracy ? R.string.qibla_calibration_needed_title : R.string.calibration_title);
        }
        if (txtCalibrationDesc != null) {
            txtCalibrationDesc.setText(lowAccuracy ? R.string.qibla_calibration_needed_desc : R.string.calibration_desc);
        }
    }
}
