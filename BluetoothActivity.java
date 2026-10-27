package com.example.reshqmesh;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity {

    private static final int REQUEST_BLUETOOTH_PERMISSION = 101;

    private TextView tvBluetoothStatus;
    private TextView tvSelectedDevice;

    private Button btnDisconnectBluetooth;
    private Button btnRefreshDevices;
    private Button btnOpenBluetoothSettings;

    private LinearLayout deviceContainer;

    private BluetoothManager bluetoothManager;

    // --------------------------------------------------
    // Bluetooth listener
    // --------------------------------------------------

    private final BluetoothManager.BluetoothListener
            bluetoothListener =
            new BluetoothManager.BluetoothListener() {

                @Override
                public void onConnected(
                        BluetoothDevice device
                ) {

                    runOnUiThread(() -> {

                        String deviceName =
                                getDeviceDisplayName(device);

                        tvBluetoothStatus.setText(
                                getString(
                                        R.string.bluetooth_connected,
                                        deviceName
                                )
                        );

                        tvSelectedDevice.setText(
                                getString(
                                        R.string.bluetooth_selected,
                                        deviceName
                                )
                        );

                        Toast.makeText(
                                BluetoothActivity.this,
                                R.string.bluetooth_connection_successful,
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                }

                @Override
                public void onConnectionFailed(
                        String errorMessage
                ) {

                    runOnUiThread(() -> {

                        tvBluetoothStatus.setText(
                                R.string.bluetooth_status_connection_failed
                        );

                        Toast.makeText(
                                BluetoothActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                }

                @Override
                public void onMessageReceived(
                        String message
                ) {

                    // Messages are now handled by ChatActivity.
                }

                @Override
                public void onDisconnected() {

                    runOnUiThread(() -> {

                        tvBluetoothStatus.setText(
                                R.string.bluetooth_status_disconnected
                        );

                        tvSelectedDevice.setText(
                                R.string.bluetooth_selected_none
                        );

                        Toast.makeText(
                                BluetoothActivity.this,
                                R.string.bluetooth_disconnected,
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                }
            };

    // --------------------------------------------------
    // Bluetooth enable launcher
    // --------------------------------------------------

    private final ActivityResultLauncher<Intent>
            enableBluetoothLauncher =
            registerForActivityResult(
                    new ActivityResultContracts
                            .StartActivityForResult(),
                    result -> {

                        if (bluetoothManager == null) {
                            return;
                        }

                        if (
                                bluetoothManager
                                        .isBluetoothEnabled()
                        ) {

                            tvBluetoothStatus.setText(
                                    R.string
                                            .bluetooth_status_ready
                            );

                            refreshPairedDevices();

                        } else {

                            tvBluetoothStatus.setText(
                                    R.string
                                            .bluetooth_status_disabled
                            );
                        }
                    }
            );

    // --------------------------------------------------
    // onCreate
    // --------------------------------------------------

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {

        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_bluetooth
        );

        initializeViews();

        bluetoothManager =
                BluetoothManager.getInstance(
                        this
                );

        setupListeners();

        checkBluetoothAndInitialize();
    }

    // --------------------------------------------------
    // Initialize views
    // --------------------------------------------------

    private void initializeViews() {

        tvBluetoothStatus =
                findViewById(
                        R.id.tvBluetoothStatus
                );

        tvSelectedDevice =
                findViewById(
                        R.id.tvSelectedDevice
                );

        btnDisconnectBluetooth =
                findViewById(
                        R.id.btnDisconnectBluetooth
                );

        btnRefreshDevices =
                findViewById(
                        R.id.btnRefreshDevices
                );

        btnOpenBluetoothSettings =
                findViewById(
                        R.id.btnOpenBluetoothSettings
                );

        deviceContainer =
                findViewById(
                        R.id.deviceContainer
                );
    }

    // --------------------------------------------------
    // Setup listeners
    // --------------------------------------------------

    private void setupListeners() {

        btnOpenBluetoothSettings.setOnClickListener(
                v -> {

                    Intent intent =
                            new Intent(
                                    Settings
                                            .ACTION_BLUETOOTH_SETTINGS
                            );

                    startActivity(intent);
                }
        );

        btnRefreshDevices.setOnClickListener(
                v -> {

                    if (
                            hasBluetoothConnectPermission()
                    ) {

                        checkBluetoothAndInitialize();

                    } else {

                        requestBluetoothPermission();
                    }
                }
        );

        btnDisconnectBluetooth.setOnClickListener(
                v -> {

                    bluetoothManager.disconnect();
                }
        );
    }

    // --------------------------------------------------
    // Bluetooth initialization
    // --------------------------------------------------

    private void checkBluetoothAndInitialize() {

        if (
                !bluetoothManager
                        .isBluetoothSupported()
        ) {

            tvBluetoothStatus.setText(
                    R.string.bluetooth_not_supported
            );

            return;
        }

        if (
                !hasBluetoothConnectPermission()
        ) {

            if (
                    Build.VERSION.SDK_INT
                            >= Build.VERSION_CODES.S
            ) {

                requestBluetoothPermission();
            }

            return;
        }

        if (
                !bluetoothManager
                        .isBluetoothEnabled()
        ) {

            tvBluetoothStatus.setText(
                    R.string
                            .bluetooth_status_enable_request
            );

            Intent enableIntent =
                    new Intent(
                            android.bluetooth
                                    .BluetoothAdapter
                                    .ACTION_REQUEST_ENABLE
                    );

            enableBluetoothLauncher.launch(
                    enableIntent
            );

            return;
        }

        if (bluetoothManager.isConnected()) {

            BluetoothDevice device =
                    bluetoothManager
                            .getConnectedDevice();

            if (device != null) {

                String deviceName =
                        getDeviceDisplayName(
                                device
                        );

                tvBluetoothStatus.setText(
                        getString(
                                R.string.bluetooth_connected,
                                deviceName
                        )
                );

                tvSelectedDevice.setText(
                        getString(
                                R.string.bluetooth_selected,
                                deviceName
                        )
                );

            }

        } else {

            tvBluetoothStatus.setText(
                    R.string.bluetooth_status_ready
            );
        }

        refreshPairedDevices();
    }

    // --------------------------------------------------
    // Permission
    // --------------------------------------------------

    private boolean hasBluetoothConnectPermission() {

        if (
                Build.VERSION.SDK_INT
                        < Build.VERSION_CODES.S
        ) {

            return true;
        }

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission
                        .BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBluetoothPermission() {

        if (
                Build.VERSION.SDK_INT
                        >= Build.VERSION_CODES.S
        ) {

            requestPermissions(
                    new String[]{

                            Manifest.permission
                                    .BLUETOOTH_CONNECT
                    },
                    REQUEST_BLUETOOTH_PERMISSION
            );
        }
    }

    // --------------------------------------------------
    // Refresh paired devices
    // --------------------------------------------------

    @SuppressLint("MissingPermission")
    private void refreshPairedDevices() {

        deviceContainer.removeAllViews();

        if (
                !hasBluetoothConnectPermission()
        ) {

            return;
        }

        Set<BluetoothDevice> pairedDevices =
                bluetoothManager
                        .getPairedDevices();

        if (pairedDevices.isEmpty()) {

            TextView noDevices =
                    new TextView(this);

            noDevices.setText(
                    R.string
                            .bluetooth_no_paired_devices
            );

            noDevices.setTextSize(15);

            noDevices.setPadding(
                    10,
                    15,
                    10,
                    15
            );

            deviceContainer.addView(
                    noDevices
            );

            return;
        }

        List<BluetoothDevice> deviceList =
                new ArrayList<>(
                        pairedDevices
                );

        Collections.sort(
                deviceList,
                Comparator.comparing(
                        this::getDeviceDisplayName,
                        String.CASE_INSENSITIVE_ORDER
                )
        );

        for (
                BluetoothDevice device
                : deviceList
        ) {

            Button deviceButton =
                    new Button(this);

            String deviceName =
                    getDeviceDisplayName(
                            device
                    );

            String deviceAddress =
                    device.getAddress();

            deviceButton.setText(
                    getString(
                            R.string
                                    .bluetooth_device_button,
                            deviceName,
                            deviceAddress
                    )
            );

            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams
                                    .MATCH_PARENT,
                            LinearLayout.LayoutParams
                                    .WRAP_CONTENT
                    );

            params.setMargins(
                    0,
                    0,
                    0,
                    10
            );

            deviceButton.setLayoutParams(
                    params
            );

            deviceButton.setOnClickListener(
                    v -> {

                        tvBluetoothStatus.setText(
                                getString(
                                        R.string
                                                .bluetooth_connecting,
                                        deviceName
                                )
                        );

                        bluetoothManager.connect(
                                device
                        );
                    }
            );

            deviceContainer.addView(
                    deviceButton
            );
        }
    }

    // --------------------------------------------------
    // Device name
    // --------------------------------------------------

    @SuppressLint("MissingPermission")
    private String getDeviceDisplayName(
            BluetoothDevice device
    ) {

        String deviceName =
                device.getName();

        if (
                deviceName == null
                        || deviceName
                        .trim()
                        .isEmpty()
        ) {

            return "Unknown Bluetooth Device";
        }

        return deviceName;
    }

    // --------------------------------------------------
    // Permission result
    // --------------------------------------------------

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (
                requestCode
                        == REQUEST_BLUETOOTH_PERMISSION
        ) {

            if (
                    grantResults.length > 0
                            && grantResults[0]
                            == PackageManager
                            .PERMISSION_GRANTED
            ) {

                checkBluetoothAndInitialize();

            } else {

                tvBluetoothStatus.setText(
                        R.string
                                .bluetooth_permission_denied
                );

                Toast.makeText(
                        this,
                        R.string
                                .bluetooth_permission_required,
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // --------------------------------------------------
    // Activity lifecycle
    // --------------------------------------------------

    @Override
    protected void onStart() {

        super.onStart();

        if (bluetoothManager != null) {

            bluetoothManager.registerListener(
                    bluetoothListener
            );
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (
                bluetoothManager != null
                        && hasBluetoothConnectPermission()
                        && bluetoothManager
                        .isBluetoothEnabled()
        ) {

            if (bluetoothManager.isConnected()) {

                BluetoothDevice device =
                        bluetoothManager
                                .getConnectedDevice();

                if (device != null) {

                    String deviceName =
                            getDeviceDisplayName(
                                    device
                            );

                    tvBluetoothStatus.setText(
                            getString(
                                    R.string
                                            .bluetooth_connected,
                                    deviceName
                            )
                    );

                    tvSelectedDevice.setText(
                            getString(
                                    R.string
                                            .bluetooth_selected,
                                    deviceName
                            )
                    );
                }

            } else {

                tvBluetoothStatus.setText(
                        R.string
                                .bluetooth_status_ready
                );
            }

            refreshPairedDevices();
        }
    }

    @Override
    protected void onStop() {

        if (bluetoothManager != null) {

            bluetoothManager.unregisterListener(
                    bluetoothListener
            );
        }

        super.onStop();
    }
}