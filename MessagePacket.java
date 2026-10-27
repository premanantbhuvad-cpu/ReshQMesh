package com.example.reshqmesh;

public class MessagePacket {

    private String senderId;
    private String senderName;
    private String message;
    private String time;
    private String nodeId;
    private boolean sos;

    public MessagePacket(
            String senderId,
            String senderName,
            String message,
            String time,
            String nodeId,
            boolean sos
    ) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.time = time;
        this.nodeId = nodeId;
        this.sos = sos;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getMessage() {
        return message;
    }

    public String getTime() {
        return time;
    }

    public String getNodeId() {
        return nodeId;
    }

    public boolean isSos() {
        return sos;
    }
}