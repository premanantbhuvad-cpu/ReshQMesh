package com.example.reshqmesh;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BluetoothManager {

    public interface BluetoothListener {

        void onConnected(BluetoothDevice device);

        void onConnectionFailed(String errorMessage);

        void onMessageReceived(String message);

        void onDisconnected();
    }

    private static final UUID SPP_UUID =
            UUID.fromString(
                    "00001101-0000-1000-8000-00805F9B34FB"
            );

    // --------------------------------------------------
    // SINGLETON INSTANCE
    // --------------------------------------------------

    private static volatile BluetoothManager instance;

    public static BluetoothManager getInstance(Context context) {

        if (instance == null) {

            synchronized (BluetoothManager.class) {

                if (instance == null) {

                    instance = new BluetoothManager(
                            context.getApplicationContext()
                    );
                }
            }
        }

        return instance;
    }

    // --------------------------------------------------
    // VARIABLES
    // --------------------------------------------------

    private final Context context;

    private final BluetoothAdapter bluetoothAdapter;

    private final ExecutorService executorService;

    private final CopyOnWriteArraySet<BluetoothListener>
            listeners =
            new CopyOnWriteArraySet<>();

    private volatile BluetoothSocket bluetoothSocket;

    private volatile OutputStream outputStream;

    private volatile InputStream inputStream;

    private volatile BluetoothDevice connectedDevice;

    private Thread readerThread;

    private volatile boolean manuallyDisconnected = false;

    // --------------------------------------------------
    // PRIVATE CONSTRUCTOR
    // --------------------------------------------------

    private BluetoothManager(Context context) {

        this.context = context;

        this.executorService =
                Executors.newSingleThreadExecutor();

        android.bluetooth.BluetoothManager
                systemBluetoothManager =
                (android.bluetooth.BluetoothManager)
                        this.context.getSystemService(
                                Context.BLUETOOTH_SERVICE
                        );

        if (systemBluetoothManager != null) {

            bluetoothAdapter =
                    systemBluetoothManager.getAdapter();

        } else {

            bluetoothAdapter = null;
        }
    }

    // --------------------------------------------------
    // LISTENER MANAGEMENT
    // --------------------------------------------------

    public void registerListener(
            BluetoothListener listener
    ) {

        if (listener != null) {

            listeners.add(listener);
        }
    }

    public void unregisterListener(
            BluetoothListener listener
    ) {

        if (listener != null) {

            listeners.remove(listener);
        }
    }

    // --------------------------------------------------
    // BLUETOOTH SUPPORT
    // --------------------------------------------------

    public boolean isBluetoothSupported() {

        return bluetoothAdapter != null;
    }

    @SuppressLint("MissingPermission")
    public boolean isBluetoothEnabled() {

        return bluetoothAdapter != null
                && bluetoothAdapter.isEnabled();
    }

    private boolean hasBluetoothConnectPermission() {

        if (Build.VERSION.SDK_INT <
                Build.VERSION_CODES.S) {

            return true;
        }

        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED;
    }

    // --------------------------------------------------
    // PAIRED DEVICES
    // --------------------------------------------------

    @SuppressLint("MissingPermission")
    public Set<BluetoothDevice> getPairedDevices() {

        if (bluetoothAdapter == null) {

            return Collections.emptySet();
        }

        if (!hasBluetoothConnectPermission()) {

            return Collections.emptySet();
        }

        return bluetoothAdapter.getBondedDevices();
    }

    // --------------------------------------------------
    // CONNECT
    // --------------------------------------------------

    public void connect(BluetoothDevice device) {

        if (device == null) {

            notifyConnectionFailed(
                    "Bluetooth device is unavailable."
            );

            return;
        }

        if (bluetoothAdapter == null) {

            notifyConnectionFailed(
                    "Bluetooth is not supported on this device."
            );

            return;
        }

        if (!hasBluetoothConnectPermission()) {

            notifyConnectionFailed(
                    "Bluetooth permission is not granted."
            );

            return;
        }

        // Close any old connection.
        closeConnection(false);

        manuallyDisconnected = false;

        executorService.execute(() -> {

            BluetoothSocket socket = null;

            try {

                socket = createSocket(device);

                socket.connect();

                bluetoothSocket = socket;

                inputStream =
                        socket.getInputStream();

                outputStream =
                        socket.getOutputStream();

                connectedDevice = device;

                notifyConnected(device);

                startReaderThread();

            } catch (IOException | SecurityException e) {

                if (socket != null) {

                    try {

                        socket.close();

                    } catch (IOException ignored) {
                    }
                }

                bluetoothSocket = null;

                inputStream = null;

                outputStream = null;

                connectedDevice = null;

                String errorMessage =
                        e.getMessage();

                if (
                        errorMessage == null
                                || errorMessage
                                .trim()
                                .isEmpty()
                ) {

                    errorMessage =
                            "Unable to connect to the Bluetooth device.";
                }

                notifyConnectionFailed(
                        errorMessage
                );
            }
        });
    }

    // --------------------------------------------------
    // CREATE SOCKET
    // --------------------------------------------------

    @SuppressLint("MissingPermission")
    private BluetoothSocket createSocket(
            BluetoothDevice device
    ) throws IOException {

        return device.createRfcommSocketToServiceRecord(
                SPP_UUID
        );
    }

    // --------------------------------------------------
    // READER THREAD
    // --------------------------------------------------

    private void startReaderThread() {

        readerThread =
                new Thread(() -> {

                    try {

                        InputStream currentInputStream =
                                inputStream;

                        if (currentInputStream == null) {

                            return;
                        }

                        BufferedReader reader =
                                new BufferedReader(
                                        new InputStreamReader(
                                                currentInputStream,
                                                StandardCharsets.UTF_8
                                        )
                                );

                        String receivedMessage;

                        while (
                                !Thread.currentThread()
                                        .isInterrupted()
                                        && (
                                        receivedMessage =
                                                reader.readLine()
                                ) != null
                        ) {

                            notifyMessageReceived(
                                    receivedMessage
                            );
                        }

                    } catch (IOException |
                             NullPointerException e) {

                        if (!manuallyDisconnected) {

                            handleUnexpectedDisconnect();
                        }
                    }
                });

        readerThread.start();
    }

    // --------------------------------------------------
    // SEND MESSAGE
    // --------------------------------------------------

    public void sendMessage(String message) {

        if (
                message == null
                        || message.trim().isEmpty()
        ) {

            return;
        }

        if (!isConnected()) {

            notifyConnectionFailed(
                    "No Bluetooth device is connected."
            );

            return;
        }

        OutputStream currentOutputStream =
                outputStream;

        executorService.execute(() -> {

            try {

                String data =
                        message + "\n";

                currentOutputStream.write(
                        data.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

                currentOutputStream.flush();

            } catch (IOException e) {

                notifyConnectionFailed(
                        "Message could not be sent."
                );
            }
        });
    }

    // --------------------------------------------------
    // CONNECTION STATUS
    // --------------------------------------------------

    public boolean isConnected() {

        BluetoothSocket socket =
                bluetoothSocket;

        return socket != null
                && socket.isConnected();
    }

    public BluetoothDevice getConnectedDevice() {

        return connectedDevice;
    }

    // --------------------------------------------------
    // DISCONNECT
    // --------------------------------------------------

    public void disconnect() {

        closeConnection(true);
    }

    // --------------------------------------------------
    // CLOSE CONNECTION
    // --------------------------------------------------

    private synchronized void closeConnection(
            boolean notifyListener
    ) {

        manuallyDisconnected = true;

        if (readerThread != null) {

            readerThread.interrupt();

            readerThread = null;
        }

        if (inputStream != null) {

            try {

                inputStream.close();

            } catch (IOException ignored) {
            }

            inputStream = null;
        }

        if (outputStream != null) {

            try {

                outputStream.close();

            } catch (IOException ignored) {
            }

            outputStream = null;
        }

        if (bluetoothSocket != null) {

            try {

                bluetoothSocket.close();

            } catch (IOException ignored) {
            }

            bluetoothSocket = null;
        }

        connectedDevice = null;

        if (notifyListener) {

            notifyDisconnected();
        }
    }

    // --------------------------------------------------
    // UNEXPECTED DISCONNECT
    // --------------------------------------------------

    private synchronized void
    handleUnexpectedDisconnect() {

        if (manuallyDisconnected) {

            return;
        }

        manuallyDisconnected = true;

        if (readerThread != null) {

            readerThread = null;
        }

        if (inputStream != null) {

            try {

                inputStream.close();

            } catch (IOException ignored) {
            }

            inputStream = null;
        }

        if (outputStream != null) {

            try {

                outputStream.close();

            } catch (IOException ignored) {
            }

            outputStream = null;
        }

        if (bluetoothSocket != null) {

            try {

                bluetoothSocket.close();

            } catch (IOException ignored) {
            }

            bluetoothSocket = null;
        }

        connectedDevice = null;

        notifyDisconnected();
    }

    // --------------------------------------------------
    // NOTIFICATION METHODS
    // --------------------------------------------------

    private void notifyConnected(
            BluetoothDevice device
    ) {

        for (BluetoothListener listener
                : listeners) {

            listener.onConnected(device);
        }
    }

    private void notifyConnectionFailed(
            String message
    ) {

        for (BluetoothListener listener
                : listeners) {

            listener.onConnectionFailed(
                    message
            );
        }
    }

    private void notifyMessageReceived(
            String message
    ) {

        for (BluetoothListener listener
                : listeners) {

            listener.onMessageReceived(
                    message
            );
        }
    }

    private void notifyDisconnected() {

        for (BluetoothListener listener
                : listeners) {

            listener.onDisconnected();
        }
    }
}