package com.example.reshqmesh;

import android.content.Context;
import android.content.SharedPreferences;

public class LocalStorage {

    private static final String PREF_NAME = "ResQMeshData";

    private final SharedPreferences preferences;

    public LocalStorage(Context context) {

        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    // ---------------- PROFILE ----------------

    public void saveProfile(
            String name,
            String userId,
            String phone,
            String userType
    ) {

        preferences.edit()
                .putString("name", name)
                .putString("userId", userId)
                .putString("phone", phone)
                .putString("userType", userType)
                .apply();
    }

    public String getName() {
        return preferences.getString("name", "");
    }

    public String getUserId() {
        return preferences.getString("userId", "");
    }

    public String getPhone() {
        return preferences.getString("phone", "");
    }

    public String getUserType() {
        return preferences.getString("userType", "Victim");
    }

    // ---------------- CONTACTS ----------------

    public void saveContacts(String contacts) {

        preferences.edit()
                .putString("contacts", contacts)
                .apply();
    }

    public String getContacts() {

        return preferences.getString("contacts", "");
    }

    // ---------------- SOS PACKET ----------------

    public void saveSOSPacket(EmergencyPacket packet) {

        preferences.edit()
                .putBoolean("sos", packet.isSos())
                .putString("sosUserId", packet.getUserId())
                .putString("sosName", packet.getName())
                .putString("sosUserType", packet.getUserType())
                .putString("sosMessage", packet.getMessage())
                .putString("sosLatitude",
                        String.valueOf(packet.getLatitude()))
                .putString("sosLongitude",
                        String.valueOf(packet.getLongitude()))
                .putString("sosTime", packet.getTime())
                .putString("sosNodeId", packet.getNodeId())
                .apply();
    }

    public boolean hasSOSPacket() {

        return preferences.contains("sos");
    }

    public boolean getSOSStatus() {

        return preferences.getBoolean("sos", false);
    }

    public String getSOSUserId() {

        return preferences.getString("sosUserId", "");
    }

    public String getSOSName() {

        return preferences.getString("sosName", "");
    }

    public String getSOSUserType() {

        return preferences.getString("sosUserType", "");
    }

    public String getSOSMessage() {

        return preferences.getString("sosMessage", "");
    }

    public String getSOSLatitude() {

        return preferences.getString("sosLatitude", "");
    }

    public String getSOSLongitude() {

        return preferences.getString("sosLongitude", "");
    }

    public String getSOSTime() {

        return preferences.getString("sosTime", "");
    }

    public String getSOSNodeId() {

        return preferences.getString("sosNodeId", "");
    }
    // ---------------- NORMAL MESSAGES ----------------

    // ---------------- NORMAL MESSAGES ----------------

    public void saveMessages(String messages) {
        preferences.edit()
                .putString("messages", messages)
                .apply();
    }

    public String getMessages() {
        return preferences.getString("messages", "");
    }

    public void saveMessagePacket(MessagePacket packet) {

        String existingMessages = getMessages();

        String packetData =
                packet.getSenderId() + "|" +
                        packet.getSenderName() + "|" +
                        packet.getMessage() + "|" +
                        packet.getTime() + "|" +
                        packet.getNodeId() + "|" +
                        packet.isSos();

        if (existingMessages.isEmpty()) {

            existingMessages = packetData;

        } else {

            existingMessages =
                    existingMessages + "\n" + packetData;
        }

        saveMessages(existingMessages);
    }

    public void saveReceivedMessage(
            String message,
            String nodeId
    ) {

        String time =
                new java.text.SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        java.util.Locale.getDefault()
                ).format(
                        new java.util.Date()
                );

        MessagePacket packet =
                new MessagePacket(
                        "REMOTE",
                        "Remote Node",
                        message,
                        time,
                        nodeId,
                        false
                );

        saveMessagePacket(packet);
    }
    // ---------------- ACTIVE SOS STATUS ----------------

    public void setSOSActive(boolean active) {
        preferences.edit()
                .putBoolean("activeSOS", active)
                .apply();
    }

    public boolean isSOSActive() {
        return preferences.getBoolean("activeSOS", false);
    }
}