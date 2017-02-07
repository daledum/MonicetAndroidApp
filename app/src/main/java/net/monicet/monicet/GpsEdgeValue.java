package net.monicet.monicet;

/**
 * Created by ubuntu on 07-02-2017.
 */

public enum GpsEdgeValue {
    DEGREES_LATITUDE(90),
    DEGREES_LONGITUDE(180),
    MINUTES_OR_SECONDS(60);

    private final int gpsEdgeValue;

    GpsEdgeValue(int vGpsEdgeValueCode) {
        gpsEdgeValue = vGpsEdgeValueCode;
    }

    public int getGpsEdgeValue() { return gpsEdgeValue; }
}
