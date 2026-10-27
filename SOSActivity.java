package com.example.reshqmesh;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SOSActivity extends AppCompatActivity {

    private TextView tvSosStatus;
    private TextView tvSosMessage;
    private EditText etEmergencyMessage;
    private Button btnActivateSOS;
    private Button btnResolveSOS;

    private LocalStorage localStorage;
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

                            createEmergencyPacket();

                        } else {

                            Toast.makeText(
                                    SOSActivity.this,
                                    R.string.sos_location_permission_required,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sos);

        tvSosStatus = findViewById(R.id.tvSosStatus);
        tvSosMessage = findViewById(R.id.tvSosMessage);
        etEmergencyMessage = findViewById(R.id.etEmergencyMessage);
        btnActivateSOS = findViewById(R.id.btnActivateSOS);
        btnResolveSOS = findViewById(R.id.btnResolveSOS);

        localStorage = new LocalStorage(this);

        fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(this);

        loadSavedSOSPacket();
        updateResolveButton();

        btnActivateSOS.setOnClickListener(v -> {

            String emergencyMessage =
                    etEmergencyMessage.getText().toString().trim();

            if (emergencyMessage.isEmpty()) {

                etEmergencyMessage.setError(
                        getString(R.string.emergency_message_required)
                );

                return;
            }

            if (hasLocationPermission()) {

                createEmergencyPacket();

            } else {

                requestLocationPermission();
            }
        });

        btnResolveSOS.setOnClickListener(v -> {

            if (!localStorage.isSOSActive()) {

                return;
            }

            localStorage.setSOSActive(false);

            tvSosStatus.setText(
                    R.string.sos_status_not_activated
            );

            tvSosMessage.setText(
                    R.string.sos_resolved
            );

            etEmergencyMessage.setText("");

            updateResolveButton();

            Toast.makeText(
                    SOSActivity.this,
                    R.string.sos_resolved,
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private boolean hasLocationPermission() {

        return ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

                || ActivityCompat.checkSelfPermission(
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

    private void createEmergencyPacket() {

        if (!hasLocationPermission()) {

            requestLocationPermission();
            return;
        }

        tvSosStatus.setText(
                R.string.sos_creating_packet
        );

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

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {

                    if (location != null) {

                        buildPacket(location);

                    } else {

                        requestNewLocation();
                    }

                })
                .addOnFailureListener(this, exception -> {

                    tvSosStatus.setText(
                            R.string.sos_location_failed
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

                buildPacket(location);

            } else {

                tvSosStatus.setText(
                        R.string.sos_location_unavailable
                );
            }

        }).addOnFailureListener(this, exception -> {

            tvSosStatus.setText(
                    R.string.sos_location_failed
            );
        });
    }

    private void buildPacket(Location location) {

        String userId = localStorage.getUserId();
        String name = localStorage.getName();
        String userType = localStorage.getUserType();

        String emergencyMessage =
                etEmergencyMessage.getText().toString().trim();

        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        String time = new SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
        ).format(new Date());

        String nodeId =
                getString(R.string.phone_node_id);

        EmergencyPacket packet = new EmergencyPacket(
                true,
                userId,
                name,
                userType,
                emergencyMessage,
                latitude,
                longitude,
                time,
                nodeId
        );

        localStorage.saveSOSPacket(packet);
        localStorage.setSOSActive(true);

        displayPacket(packet);
        updateResolveButton();
    }

    private void displayPacket(EmergencyPacket packet) {

        tvSosStatus.setText(
                R.string.sos_status_activated
        );

        String packetDetails =
                getString(
                        R.string.sos_packet_details,
                        packet.getUserId(),
                        packet.getName(),
                        packet.getUserType(),
                        packet.getMessage(),
                        packet.getLatitude(),
                        packet.getLongitude(),
                        packet.getTime(),
                        packet.getNodeId()
                );

        tvSosMessage.setText(packetDetails);

        etEmergencyMessage.setText(
                packet.getMessage()
        );
    }

    private void loadSavedSOSPacket() {

        if (!localStorage.hasSOSPacket()) {
            return;
        }

        boolean sos = localStorage.getSOSStatus();

        String userId = localStorage.getSOSUserId();
        String name = localStorage.getSOSName();
        String userType = localStorage.getSOSUserType();
        String message = localStorage.getSOSMessage();

        double latitude = Double.parseDouble(
                localStorage.getSOSLatitude()
        );

        double longitude = Double.parseDouble(
                localStorage.getSOSLongitude()
        );

        String time = localStorage.getSOSTime();
        String nodeId = localStorage.getSOSNodeId();

        EmergencyPacket packet = new EmergencyPacket(
                sos,
                userId,
                name,
                userType,
                message,
                latitude,
                longitude,
                time,
                nodeId
        );

        if (localStorage.isSOSActive()) {

            displayPacket(packet);

        } else {

            tvSosStatus.setText(
                    R.string.sos_status_not_activated
            );

            tvSosMessage.setText(
                    R.string.sos_ready_message
            );

            etEmergencyMessage.setText("");
        }
    }

    private void updateResolveButton() {

        btnResolveSOS.setEnabled(
                localStorage.isSOSActive()
        );
    }
}