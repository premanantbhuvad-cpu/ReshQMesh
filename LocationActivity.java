package com.example.reshqmesh;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class LocationActivity extends AppCompatActivity {

    private TextView tvLocationStatus;
    private TextView tvLatitude;
    private TextView tvLongitude;
    private Button btnGetLocation;

    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {

                        boolean fineLocation = Boolean.TRUE.equals(
                                result.get(Manifest.permission.ACCESS_FINE_LOCATION)
                        );

                        boolean coarseLocation = Boolean.TRUE.equals(
                                result.get(Manifest.permission.ACCESS_COARSE_LOCATION)
                        );

                        if (fineLocation || coarseLocation) {

                            getCurrentLocation();

                        } else {

                            tvLocationStatus.setText(
                                    R.string.location_permission_denied
                            );
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        tvLocationStatus = findViewById(R.id.tvLocationStatus);
        tvLatitude = findViewById(R.id.tvLatitude);
        tvLongitude = findViewById(R.id.tvLongitude);
        btnGetLocation = findViewById(R.id.btnGetLocation);

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        btnGetLocation.setOnClickListener(v -> {

            if (hasLocationPermission()) {

                getCurrentLocation();

            } else {

                requestLocationPermission();
            }
        });
    }

    private boolean hasLocationPermission() {

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

                || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {

        locationPermissionLauncher.launch(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }
        );
    }

    private void getCurrentLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

                && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            requestLocationPermission();
            return;
        }

        tvLocationStatus.setText(
                R.string.location_getting
        );

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {

                    if (location != null) {

                        displayLocation(location);

                    } else {

                        tvLocationStatus.setText(
                                R.string.location_retrying
                        );

                        requestNewLocation();
                    }
                })
                .addOnFailureListener(this, exception -> {

                    tvLocationStatus.setText(
                            R.string.location_unable_to_get
                    );
                });
    }

    private void requestNewLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED

                && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            requestLocationPermission();
            return;
        }

        fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                null
        ).addOnSuccessListener(this, location -> {

            if (location != null) {

                displayLocation(location);

            } else {

                tvLocationStatus.setText(
                        R.string.location_unavailable
                );
            }

        }).addOnFailureListener(this, exception -> {

            tvLocationStatus.setText(
                    R.string.location_unable_to_retrieve
            );
        });
    }

    private void displayLocation(Location location) {

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        tvLatitude.setText(
                getString(
                        R.string.location_latitude,
                        latitude
                )
        );

        tvLongitude.setText(
                getString(
                        R.string.location_longitude,
                        longitude
                )
        );

        tvLocationStatus.setText(
                R.string.location_retrieved_successfully
        );
    }
}