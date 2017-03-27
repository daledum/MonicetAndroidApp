package net.monicet.monicet;

/**
 * Created by ubuntu on 30-01-2017.
 */

public enum GpsMode {
    OFF(Long.MAX_VALUE, Utils.DEFAULT_SMALLEST_DISPLACEMENT_IN_M),//its values are never used
    GPS_FIXING_FAST(Utils.GPS_FIXING_INTERVAL_IN_MILLIS, Utils.DEFAULT_SMALLEST_DISPLACEMENT_IN_M),
    DEFAULT_SLOW(Utils.BATTERY_SAVING_INTERVAL_IN_MILLIS, Utils.DEFAULT_SMALLEST_DISPLACEMENT_IN_M),
    SAMPLING_FAST(Utils.SAMPLING_INTERVAL_IN_MILLIS, Utils.DEFAULT_SMALLEST_DISPLACEMENT_IN_M),
    CONTINUOUS(Utils.ROUTE_BUILDING_INTERVAL_IN_MILLIS, Utils.SMALLEST_DISPLACEMENT_IN_M);

    private final long intervalInMillis;
    private final float smallestDisplacementInMeters;

    GpsMode(long vIntervalInMillis, float vSmallestDisplacementInMeters) {
        intervalInMillis = vIntervalInMillis;
        smallestDisplacementInMeters = vSmallestDisplacementInMeters;
    }

    public long getIntervalInMillis() { return intervalInMillis; }

    public float getSmallestDisplacementInMeters() { return smallestDisplacementInMeters; }
}