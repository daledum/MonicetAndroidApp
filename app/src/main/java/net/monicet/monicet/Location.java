package net.monicet.monicet;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ubuntu on 23-01-2017.
 */

public class Location implements Serializable {

    private final ArrayList<Sighting> mSightingsArray;

    // Formerly known as firstNumberPicker(/Quantity)Click, with a reversed logic
    // Don't make it final, maybe Locations will be changeable in the future, after they were saved,
    // would I want to turn on the GPS in that situation, though
    private boolean mQuantityChangedAtLeastOnce;

    // All the variables below are sampled at the end of Location (when saving)
    private long mTimeInMilliseconds;
    private double mLatitude;
    private double mLongitude;
    private double mUserLatitude;
    private double mUserLongitude;
    private String mUserComments;

    public Location(ArrayList<Sighting> vSightingsArray) {
        mSightingsArray = vSightingsArray;
        mQuantityChangedAtLeastOnce = false;
        mTimeInMilliseconds = 0;
        mLatitude = 0;
        mLongitude = 0;
        mUserLatitude = 0;
        mUserLongitude = 0;
        mUserComments = "";
    }

    public Location(Location vLocation) {
        this(vLocation.getBlankSightings());
    }

    public ArrayList<Sighting> getSightings() {
        return mSightingsArray;
    }

    // returns only the species, photos and descriptions - no state data
    private ArrayList<Sighting> getBlankSightings() {
        // get rid of this when deploying
        if (mSightingsArray.size() == 0) {
            Log.d("Main Activity", "Building a Location from a location with no sightings");
        }
        //do the new here, via Sighting constructor - there are only primitives there (no tied reference issues),
        // except the Animal, which is the same for everyone and cannot be removed from a Location
        ArrayList<Sighting> blankSightings = new ArrayList<Sighting>(mSightingsArray.size());
        for (Sighting sighting: mSightingsArray) {
            blankSightings.add(new Sighting(sighting));
        }
        return blankSightings;
    }

    public boolean isQuantityChangedAtLeastOnce() { return mQuantityChangedAtLeastOnce; }
    public void setQuantityChangedAtLeastOnce(boolean vQuantityChangedAtLeastOnce) {
        mQuantityChangedAtLeastOnce = vQuantityChangedAtLeastOnce;
    }

    public long getTimeInMilliseconds() {
        return mTimeInMilliseconds;
    }
    public void setTimeInMilliseconds(long vTimeInMilliseconds) {
        mTimeInMilliseconds = vTimeInMilliseconds;
    }

    public double getLatitude() {
        return mLatitude;
    }
    public void setLatitude(double vLatitude) {
        mLatitude = vLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }
    public void setLongitude(double vLongitude) {
        mLongitude = vLongitude;
    }

    public double getUserLatitude() {
        return mUserLatitude;
    }
    public void setUserLatitude(double vUserLatitude) {
        mUserLatitude = vUserLatitude;
    }

    public double getUserLongitude() {
        return mUserLongitude;
    }
    public void setUserLongitude(double vUserLongitude) {
        mUserLongitude = vUserLongitude;
    }

    public String getUserComments() { return mUserComments; }
    // use tostring on charsequence coming from edittext
    public void setUserComments(String vUserComments) {
        mUserComments = vUserComments; //garbage collector will keep the reference alive
    }

//    @Override
//    public String toString() {
//        return super.toString();// use Utility for gps, time etc
//    }
}
