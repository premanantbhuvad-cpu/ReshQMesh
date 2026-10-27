package com.example.reshqmesh;

public class EmergencyPacket {

    private boolean sos;
    private String userId;
    private String name;
    private String userType;
    private String message;
    private double latitude;
    private double longitude;
    private String time;
    private String nodeId;

    public EmergencyPacket(
            boolean sos,
            String userId,
            String name,
            String userType,
            String message,
            double latitude,
            double longitude,
            String time,
            String nodeId
    ) {

        this.sos = sos;
        this.userId = userId;
        this.name = name;
        this.userType = userType;
        this.message = message;
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
        this.nodeId = nodeId;
    }

    public boolean isSos() {
        return sos;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getUserType() {
        return userType;
    }

    public String getMessage() {
        return message;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public String getTime() {
        return time;
    }

    public String getNodeId() {
        return nodeId;
    }
}