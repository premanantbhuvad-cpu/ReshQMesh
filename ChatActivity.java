package com.example.reshqmesh;

import android.bluetooth.BluetoothDevice;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ChatActivity extends AppCompatActivity {

    private EditText etMessage;

    private Button btnSendMessage;

    private LinearLayout messageContainer;

    private ScrollView scrollMessages;

    private TextView tvChatNetworkStatus;

    private LocalStorage localStorage;

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

                        tvChatNetworkStatus.setText(
                                "Network Status: Connected"
                        );

                        btnSendMessage.setEnabled(
                                true
                        );
                    });
                }

                @Override
                public void onConnectionFailed(
                        String errorMessage
                ) {

                    runOnUiThread(() -> {

                        updateChatConnectionStatus();

                        Toast.makeText(
                                ChatActivity.this,
                                errorMessage,
                                Toast.LENGTH_SHORT
                        ).show();
                    });
                }

                @Override
                public void onMessageReceived(
                        String message
                ) {

                    runOnUiThread(() -> {

                        addReceivedMessageToScreen(
                                message
                        );

                        String nodeId =
                                "Bluetooth Device";

                        if (
                                bluetoothManager
                                        .getConnectedDevice()
                                        != null
                        ) {

                            @SuppressWarnings(
                                    "MissingPermission"
                            )
                            String deviceName =
                                    bluetoothManager
                                            .getConnectedDevice()
                                            .getName();

                            if (
                                    deviceName != null
                                            && !deviceName
                                            .trim()
                                            .isEmpty()
                            ) {

                                nodeId = deviceName;
                            }
                        }

                        localStorage
                                .saveReceivedMessage(
                                        message,
                                        nodeId
                                );

                        scrollMessages.post(() ->
                                scrollMessages
                                        .fullScroll(
                                                ScrollView
                                                        .FOCUS_DOWN
                                        )
                        );
                    });
                }

                @Override
                public void onDisconnected() {

                    runOnUiThread(() -> {

                        updateChatConnectionStatus();

                        btnSendMessage
                                .setEnabled(false);
                    });
                }
            };

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
                R.layout.activity_chat
        );

        etMessage =
                findViewById(
                        R.id.etMessage
                );

        btnSendMessage =
                findViewById(
                        R.id.btnSendMessage
                );

        messageContainer =
                findViewById(
                        R.id.messageContainer
                );

        scrollMessages =
                findViewById(
                        R.id.scrollMessages
                );

        tvChatNetworkStatus =
                findViewById(
                        R.id.tvChatNetworkStatus
                );

        localStorage =
                new LocalStorage(this);

        bluetoothManager =
                BluetoothManager.getInstance(
                        this
                );

        displaySavedSOS();

        displaySavedMessages();

        updateChatConnectionStatus();

        btnSendMessage.setOnClickListener(v -> {

            String message =
                    etMessage
                            .getText()
                            .toString()
                            .trim();

            if (message.isEmpty()) {

                Toast.makeText(
                        ChatActivity.this,
                        "Enter a message.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            if (
                    !bluetoothManager
                            .isConnected()
            ) {

                Toast.makeText(
                        ChatActivity.this,
                        "No Bluetooth node is connected.",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            String senderId =
                    localStorage.getUserId();

            String senderName =
                    localStorage.getName();

            String time =
                    new java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            java.util.Locale.getDefault()
                    ).format(
                            new java.util.Date()
                    );

            String nodeId =
                    getString(
                            R.string.phone_node_id
                    );

            MessagePacket packet =
                    new MessagePacket(
                            senderId,
                            senderName,
                            message,
                            time,
                            nodeId,
                            false
                    );

            // Send through the actual Bluetooth connection.
            bluetoothManager.sendMessage(
                    message
            );

            // Save locally.
            localStorage.saveMessagePacket(
                    packet
            );

            // Display locally.
            addMessageToScreen(
                    message
            );

            etMessage.setText("");

            scrollMessages.post(() ->
                    scrollMessages.fullScroll(
                            ScrollView.FOCUS_DOWN
                    )
            );
        });
    }

    // --------------------------------------------------
    // Connection status
    // --------------------------------------------------

    private void updateChatConnectionStatus() {

        if (
                bluetoothManager != null
                        && bluetoothManager
                        .isConnected()
        ) {

            tvChatNetworkStatus.setText(
                    "Network Status: Connected"
            );

            btnSendMessage.setEnabled(
                    true
            );

        } else {

            tvChatNetworkStatus.setText(
                    "Network Status: Disconnected"
            );

            btnSendMessage.setEnabled(
                    false
            );
        }
    }

    // --------------------------------------------------
    // Saved SOS
    // --------------------------------------------------

    private void displaySavedSOS() {

        if (
                !localStorage.hasSOSPacket()
        ) {

            return;
        }

        TextView sosMessage =
                new TextView(
                        ChatActivity.this
                );

        String sosDetails =
                getString(
                        R.string.chat_sos_details,
                        localStorage
                                .getSOSUserId(),
                        localStorage
                                .getSOSName(),
                        localStorage
                                .getSOSUserType(),
                        localStorage
                                .getSOSMessage(),
                        localStorage
                                .getSOSLatitude(),
                        localStorage
                                .getSOSLongitude(),
                        localStorage
                                .getSOSTime(),
                        localStorage
                                .getSOSNodeId()
                );

        sosMessage.setText(
                sosDetails
        );

        sosMessage.setTextSize(
                15
        );

        sosMessage.setTextColor(
                Color.rgb(
                        130,
                        20,
                        20
                )
        );

        sosMessage.setPadding(
                16,
                16,
                16,
                16
        );

        sosMessage.setBackground(
                createBackground(
                        Color.rgb(
                                255,
                                235,
                                238
                        ),
                        Color.rgb(
                                220,
                                100,
                                100
                        )
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
                15
        );

        sosMessage.setLayoutParams(
                params
        );

        messageContainer.addView(
                sosMessage
        );
    }

    // --------------------------------------------------
    // Saved messages
    // --------------------------------------------------

    private void displaySavedMessages() {

        String savedMessages =
                localStorage.getMessages();

        if (savedMessages.isEmpty()) {

            return;
        }

        String[] messages =
                savedMessages.split(
                        "\n"
                );

        for (
                String message
                : messages
        ) {

            if (!message.trim().isEmpty()) {

                addMessageToScreen(
                        message
                );
            }
        }
    }

    // --------------------------------------------------
    // Sent message display
    // --------------------------------------------------

    private void addMessageToScreen(
            String message
    ) {

        TextView newMessage =
                new TextView(
                        ChatActivity.this
                );

        newMessage.setText(
                message
        );

        newMessage.setTextSize(
                15
        );

        newMessage.setPadding(
                14,
                12,
                14,
                12
        );

        newMessage.setBackground(
                createBackground(
                        Color.rgb(
                                245,
                                247,
                                250
                        ),
                        Color.rgb(
                                215,
                                222,
                                230
                        )
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams
                                .WRAP_CONTENT,
                        LinearLayout.LayoutParams
                                .WRAP_CONTENT
                );

        params.setMargins(
                0,
                0,
                0,
                10
        );

        newMessage.setLayoutParams(
                params
        );

        messageContainer.addView(
                newMessage
        );
    }

    // --------------------------------------------------
    // Received message display
    // --------------------------------------------------

    private void addReceivedMessageToScreen(
            String message
    ) {

        TextView newMessage =
                new TextView(
                        ChatActivity.this
                );

        newMessage.setText(
                "Received: " + message
        );

        newMessage.setTextSize(
                15
        );

        newMessage.setPadding(
                14,
                12,
                14,
                12
        );

        newMessage.setBackground(
                createBackground(
                        Color.rgb(
                                232,
                                245,
                                233
                        ),
                        Color.rgb(
                                150,
                                190,
                                155
                        )
                )
        );

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams
                                .WRAP_CONTENT,
                        LinearLayout.LayoutParams
                                .WRAP_CONTENT
                );

        params.setMargins(
                0,
                0,
                0,
                10
        );

        newMessage.setLayoutParams(
                params
        );

        messageContainer.addView(
                newMessage
        );
    }

    // --------------------------------------------------
    // Background
    // --------------------------------------------------

    private GradientDrawable createBackground(
            int fillColor,
            int strokeColor
    ) {

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(
                fillColor
        );

        background.setCornerRadius(
                18
        );

        background.setStroke(
                1,
                strokeColor
        );

        return background;
    }

    // --------------------------------------------------
    // Lifecycle
    // --------------------------------------------------

    @Override
    protected void onStart() {

        super.onStart();

        bluetoothManager.registerListener(
                bluetoothListener
        );

        updateChatConnectionStatus();
    }

    @Override
    protected void onStop() {

        bluetoothManager.unregisterListener(
                bluetoothListener
        );

        super.onStop();
    }
}