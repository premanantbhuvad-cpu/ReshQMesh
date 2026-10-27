package com.example.reshqmesh;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 2200L;

    private Button btnProfile;
    private Button btnMessages;
    private Button btnSOS;
    private Button btnLocation;
    private Button btnContacts;
    private Button btnBluetooth;

    private TextView tvNetworkStatus;
    private TextView tvNodeCount;

    private BluetoothManager bluetoothManager;

    private final Handler splashHandler = new Handler(Looper.getMainLooper());

    private final Runnable showHomeRunnable = () -> {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        setContentView(R.layout.activity_main);
        initializeViews();
        setupButtonListeners();
        updateNetworkStatus();
    };

    // --------------------------------------------------
    // Bluetooth listener
    // --------------------------------------------------

    private final BluetoothManager.BluetoothListener
            bluetoothListener =
            new BluetoothManager.BluetoothListener() {

                @Override
                public void onConnected(BluetoothDevice device) {
                    runOnUiThread(MainActivity.this::updateNetworkStatus);
                }

                @Override
                public void onConnectionFailed(String errorMessage) {
                    runOnUiThread(MainActivity.this::updateNetworkStatus);
                }

                @Override
                public void onMessageReceived(String message) {
                    // Messages are handled by ChatActivity.
                }

                @Override
                public void onDisconnected() {
                    runOnUiThread(MainActivity.this::updateNetworkStatus);
                }
            };

    // --------------------------------------------------
    // onCreate and splash screen
    // --------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the existing Bluetooth manager without changing its logic.
        bluetoothManager = BluetoothManager.getInstance(this);

        showSplashScreen();
        splashHandler.postDelayed(showHomeRunnable, SPLASH_DURATION_MS);
    }

    private void showSplashScreen() {
        setContentView(R.layout.activity_splash);

        Window window = getWindow();
        window.setStatusBarColor(Color.rgb(24, 54, 74));
        window.setNavigationBarColor(Color.rgb(24, 54, 74));
        window.getDecorView().setSystemUiVisibility(0);

        FrameLayout logoContainer = findViewById(R.id.splashLogoContainer);
        if (logoContainer != null) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
            );
            logoContainer.addView(new SplashLogoView(this), params);
        }
    }

    // --------------------------------------------------
    // Initialize views
    // --------------------------------------------------

    private void initializeViews() {
        btnProfile = findViewById(R.id.btnProfile);
        btnMessages = findViewById(R.id.btnMessages);
        btnSOS = findViewById(R.id.btnSOS);
        btnLocation = findViewById(R.id.btnLocation);
        btnContacts = findViewById(R.id.btnContacts);
        btnBluetooth = findViewById(R.id.btnBluetooth);
        tvNetworkStatus = findViewById(R.id.tvNetworkStatus);
        tvNodeCount = findViewById(R.id.tvNodeCount);
    }

    // --------------------------------------------------
    // Button listeners
    // --------------------------------------------------

    private void setupButtonListeners() {
        btnProfile.setOnClickListener(v -> startActivity(
                new Intent(MainActivity.this, ProfileActivity.class)));

        btnMessages.setOnClickListener(v -> startActivity(
                new Intent(MainActivity.this, ChatActivity.class)));

        btnSOS.setOnClickListener(v -> startActivity(
                new Intent(MainActivity.this, SOSActivity.class)));

        btnLocation.setOnClickListener(v -> startActivity(
                new Intent(MainActivity.this, LocationActivity.class)));

        btnContacts.setOnClickListener(v -> startActivity(
                new Intent(MainActivity.this, ContactsActivity.class)));

        btnBluetooth.setOnClickListener(v -> startActivity(
                new Intent(MainActivity.this, BluetoothActivity.class)));
    }

    // --------------------------------------------------
    // Update network status
    // --------------------------------------------------

    private void updateNetworkStatus() {
        // onStart/onResume can run while the splash view is still displayed.
        if (bluetoothManager == null || tvNetworkStatus == null || tvNodeCount == null) {
            return;
        }

        if (bluetoothManager.isConnected()) {
            tvNetworkStatus.setText("Network Status: Connected");
            tvNodeCount.setText("Connected Nodes: 1");
        } else {
            tvNetworkStatus.setText("Network Status: Disconnected");
            tvNodeCount.setText("Connected Nodes: 0");
        }
    }

    // --------------------------------------------------
    // Activity lifecycle
    // --------------------------------------------------

    @Override
    protected void onStart() {
        super.onStart();

        if (bluetoothManager != null) {
            bluetoothManager.registerListener(bluetoothListener);
            updateNetworkStatus();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNetworkStatus();
    }

    @Override
    protected void onStop() {
        if (bluetoothManager != null) {
            bluetoothManager.unregisterListener(bluetoothListener);
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        splashHandler.removeCallbacks(showHomeRunnable);
        super.onDestroy();
    }

    // --------------------------------------------------
    // Small vector logo: shield + connected mesh nodes
    // --------------------------------------------------

    private static class SplashLogoView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        SplashLogoView(Context context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float size = Math.min(getWidth(), getHeight());
            if (size <= 0f) {
                return;
            }

            float scale = size / 100f;
            float offsetX = (getWidth() - 100f * scale) / 2f;
            float offsetY = (getHeight() - 100f * scale) / 2f;

            int saveCount = canvas.save();
            canvas.translate(offsetX, offsetY);
            canvas.scale(scale, scale);

            Path shield = new Path();
            shield.moveTo(50f, 7f);
            shield.lineTo(80f, 19f);
            shield.lineTo(80f, 45f);
            shield.quadTo(78f, 69f, 50f, 91f);
            shield.quadTo(22f, 69f, 20f, 45f);
            shield.lineTo(20f, 19f);
            shield.close();

            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4.2f);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawPath(shield, paint);

            Path connections = new Path();
            connections.moveTo(50f, 30f);
            connections.lineTo(34f, 44f);
            connections.lineTo(50f, 64f);
            connections.lineTo(66f, 44f);
            connections.lineTo(50f, 30f);

            paint.setColor(Color.rgb(72, 185, 165));
            paint.setStrokeWidth(3.5f);
            canvas.drawPath(connections, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(72, 185, 165));
            canvas.drawCircle(50f, 30f, 5.5f, paint);

            paint.setColor(Color.WHITE);
            canvas.drawCircle(34f, 44f, 5.5f, paint);
            canvas.drawCircle(66f, 44f, 5.5f, paint);

            paint.setColor(Color.rgb(199, 70, 78));
            canvas.drawCircle(50f, 64f, 5.8f, paint);

            canvas.restoreToCount(saveCount);
        }
    }
}
