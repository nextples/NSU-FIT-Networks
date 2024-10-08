package ru.nsu.nextples.server;

public class SessionInfo {
    private final int sessionId;

    private String fileName;
    private long bytesReceived;
    private long fileSize;
    private long startTime = 0;
    private double avgSpeed;
    private boolean isFinished = false;

    private static int idCounter = 1;

    public SessionInfo() {
        this.sessionId = idCounter++;
    }

    public int getSessionId() {
        return sessionId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getBytesReceived() {
        return bytesReceived;
    }

    public void setBytesReceived(long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public double getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(double avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }
}
