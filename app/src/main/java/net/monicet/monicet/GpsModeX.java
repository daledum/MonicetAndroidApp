package net.monicet.monicet;

/**
 * Created by ubuntu on 24-03-2017.
 */

public enum GpsModeX {
    FIXING(2000),// 2 seconds for fixing the gps signal
    SAMPLING(1000),// 1 second for capturing coordinates
    USER_1_MIN(60 * 1000), // 1 minute - this should be the default for the number picker
    USER_5_MIN(300 * 1000), // 5 minutes
    USER_10_MIN(600 * 1000), // 10 minutes
    USER_15_MIN(900 * 1000),
    USER_20_MIN(1200 * 1000),
    USER_25_MIN(1500 * 1000),
    USER_30_MIN(1800 * 1000);
    // remember to iterate through this for your number picker, larger>60000

    private final long intervalInMillis;

    GpsModeX(long vIntervalInMillis) {
        intervalInMillis = vIntervalInMillis;
    }

    public long getIntervalInMillis() { return intervalInMillis; }
}