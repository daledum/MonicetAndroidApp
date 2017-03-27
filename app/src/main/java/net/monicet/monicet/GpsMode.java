package net.monicet.monicet;

/**
 * Created by ubuntu on 24-03-2017.
 */

public enum GpsMode {
    FIXING(2 * Utils.ONE_SECOND_IN_MILLIS),// 2 seconds for fixing the gps signal
    SAMPLING(Utils.ONE_SECOND_IN_MILLIS),// 1 second for capturing coordinates
    USER_1_MIN(Utils.ONE_MINUTE_IN_MILLIS), // 1 minute - this should be the default for the number picker
    USER_5_MIN(Utils.FIVE_MINUTES_IN_MILLIS), // 5 minutes
    USER_10_MIN(2 * Utils.FIVE_MINUTES_IN_MILLIS), // 10 minutes
    USER_15_MIN(3 * Utils.FIVE_MINUTES_IN_MILLIS),
    USER_20_MIN(4 * Utils.FIVE_MINUTES_IN_MILLIS),
    USER_25_MIN(5 * Utils.FIVE_MINUTES_IN_MILLIS),
    USER_30_MIN(6 * Utils.FIVE_MINUTES_IN_MILLIS);
    // remember to iterate through this for your number picker, larger>60000

    private final long intervalInMillis;

    GpsMode(long vIntervalInMillis) {
        intervalInMillis = vIntervalInMillis;
    }

    public long getIntervalInMillis() { return intervalInMillis; }
}