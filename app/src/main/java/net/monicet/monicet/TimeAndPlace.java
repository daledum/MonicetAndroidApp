package net.monicet.monicet;

/**
 * Created by ubuntu on 28-02-2017.
 */

public class TimeAndPlace {
    private long timeInMillis;
    private double latitude;
    private double longitude;

    public TimeAndPlace() {
        timeInMillis = 0;
        latitude = 0;
        longitude = 0;
    }

    public long getTimeInMillis() { return timeInMillis; }
    public void setTimeInMillis(long vTimeInMillis) {
        timeInMillis = vTimeInMillis;
    }

    public double getLatitude() { return latitude; }
    public void setLatitude(double vLatitude) { latitude = vLatitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double vLongitude) { longitude = vLongitude; }
}
