package net.monicet.monicet;

/**
 * Created by ubuntu on 24-03-2017.
 */

public class GpsModeState {// should this be volatile or not inside the trip... it should actually be final
    private volatile GpsModeX currentParentGpsMode;//or default?
    private volatile GpsModeX samplingGpsMode;//or current?
    private volatile GpsModeX userParentGpsMode;//maybe rename to user defined mode

    // submodes should be: off, fixing (normal), user (normal), sampling mode
    // trip goes by default into gpsmodex normal mode
    // samplingGpsSubmode is always SAMPLING(...) constructor or in new?
    // normal gps submode should be given in constructor only
    //GpsModeState(gpsmode normalsamplingmode) ..with fixing by default
    // when I finish fixing mode..I know I was in sampling mode, because ...? TODO:
    // I know in which mode I am because I ask..normal mode == FIXING
    // when changing trips gps mode, connect normalgpssubmode to the users value

    public GpsModeState() {
        currentParentGpsMode = GpsModeX.FIXING;
        samplingGpsMode = GpsModeX.FIXING;
        userParentGpsMode = GpsModeX.USER_5_MIN;//the default user mode
    }

    public GpsModeX getCurrentParentGpsMode() { return currentParentGpsMode; }
    public synchronized void setCurrentParentGpsMode(GpsModeX gpsMode) { currentParentGpsMode = gpsMode; }

    public GpsModeX getSamplingGpsMode() { return samplingGpsMode; }
    public synchronized void setSamplingGpsMode(GpsModeX gpsMode) { samplingGpsMode = gpsMode; }

    public GpsModeX getUserParentGpsMode() { return userParentGpsMode; }
    public synchronized void setUserParentGpsMode(GpsModeX vNextParentGpsMode) {
        userParentGpsMode = vNextParentGpsMode;
    }
}
