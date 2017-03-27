package net.monicet.monicet;

/**
 * Created by ubuntu on 24-03-2017.
 */

public class GpsModeState {
    private volatile GpsMode currentParentGpsMode;
    private volatile GpsMode samplingGpsMode;
    private volatile GpsMode userParentGpsMode;

    public GpsModeState() {
        currentParentGpsMode = GpsMode.FIXING;
        samplingGpsMode = GpsMode.FIXING;
        userParentGpsMode = GpsMode.USER_5_MIN;//the default user mode
    }

    public GpsMode getCurrentParentGpsMode() { return currentParentGpsMode; }
    public synchronized void setCurrentParentGpsMode(GpsMode gpsMode) { currentParentGpsMode = gpsMode; }

    public GpsMode getSamplingGpsMode() { return samplingGpsMode; }
    public synchronized void setSamplingGpsMode(GpsMode gpsMode) { samplingGpsMode = gpsMode; }

    public GpsMode getUserParentGpsMode() { return userParentGpsMode; }
    public synchronized void setUserParentGpsMode(GpsMode vNextParentGpsMode) {
        userParentGpsMode = vNextParentGpsMode;
    }
}
