package ru.nsu.nextples.server;

public class TransferSpeedometer implements Runnable {
    private final SessionInfo sessionInfo;

    public static final int SECOND = 1000;
    public static final int MB = 1024 * 1024;
    public static final int PERIOD = 500;

    public TransferSpeedometer(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    @Override
    public void run() {
        long time1 = sessionInfo.getStartTime();
        long timePassed = 0;

        double currentSpeed = 0;
        double avgSpeed = 0;
        double sumPeriodSpeed = 0;
        double checksPeriodNum = 0;


        while (!sessionInfo.isFinished()) {
            timePassed = System.currentTimeMillis() - sessionInfo.getStartTime();

            if (System.currentTimeMillis() - time1 >= PERIOD) {
                currentSpeed = (sessionInfo.getBytesReceived() / (double) MB) / (timePassed / (double) SECOND);
                sumPeriodSpeed += currentSpeed;
                checksPeriodNum++;
                time1 = System.currentTimeMillis();
                avgSpeed = sumPeriodSpeed / checksPeriodNum;

                double currPercent = ( (double)100 * sessionInfo.getBytesReceived() / (double)sessionInfo.getFileSize() );
                System.out.printf("%s - %.2f%%: current speed is %.3f MB/sec; average speed is %.3f MB/sec. %n",
                        sessionInfo.getFileName(), currPercent, currentSpeed, avgSpeed);
            }
        }

        if (timePassed < PERIOD) {
            sessionInfo.setAvgSpeed((sessionInfo.getBytesReceived() / (double) MB) / (timePassed / (double) SECOND));
        }
        else {
            sessionInfo.setAvgSpeed(avgSpeed);
        }
    }
}
