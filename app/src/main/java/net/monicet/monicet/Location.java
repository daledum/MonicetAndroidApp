package net.monicet.monicet;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ubuntu on 23-01-2017.
 */

public class Location implements Serializable {

    private final ArrayList<Sighting> mSightingsArray;

    // All the variables below are sampled at the end of Location (when saving)
    private long mTimeInMilliseconds;
    // these are the GPS coordinates registered by the system (in code, in the background)
    private double mLatitude;
    private double mLongitude;

    //these are the user provided GPS coordinates (from their own, separate device)
    private class LatitudeUserInput extends AbstractUserInput {
        private double mLatitude;
        private LatitudeUserInput(double vLatitude, boolean vActive) {
            mLatitude = vLatitude;
            setActive(vActive);
        }
    }
    private LatitudeUserInput mLatitudeUserInput;

    private class LongitudeUserInput extends AbstractUserInput {
        private double mLongitude;
        private LongitudeUserInput(double vLongitude, boolean vActive) {
            mLongitude = vLongitude;
            setActive(vActive);
        }
    }
    private LongitudeUserInput mLongitudeUserInput;

    private class CommentsUserInput extends AbstractUserInput {
        private String mComments;
        private CommentsUserInput(String vComments, boolean vActive) {
            mComments = vComments;
            setActive(vActive);
        }
    }
    private CommentsUserInput mCommentsUserInput;

    public Location(ArrayList<Sighting> vSightingsArray) {
        mSightingsArray = vSightingsArray;
        mTimeInMilliseconds = 0;
        mLatitude = 0;
        mLongitude = 0;

        // when constructing from an array of sightings, we don't copy any state
        mLatitudeUserInput = new LatitudeUserInput(0, true);
        mLongitudeUserInput = new LongitudeUserInput(0, true);
        mCommentsUserInput = new CommentsUserInput("", true);
    }

    // Copy the state of the user changeable variables across locations, so we know if we should
    // display the comments dialog or not
    public Location(ArrayList<Sighting> vSightingsArray,
                    boolean vLatActive, boolean vLongActive, boolean vComActive) {
        this(vSightingsArray);
        mLatitudeUserInput.setActive(vLatActive);
        mLatitudeUserInput.setActive(vLongActive);
        mCommentsUserInput.setActive(vComActive);
    }

    // This passes the state of each user changeable variable into the constructor
    public Location(Location vLocation) {
        this(vLocation.getBlankSightings(), vLocation.isLatitudeUserInputActive(),
                vLocation.isLongitudeUserInputActive(), vLocation.isCommentsUserInputActive());
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

    public double getUserLatitude() { return mLatitudeUserInput.mLatitude; }
    public void setUserLatitude(double vUserLatitude) {
        mLatitudeUserInput.mLatitude = vUserLatitude;
    }

    public boolean isLatitudeUserInputActive() { return mLatitudeUserInput.isActive(); }
    public void setLatitudeUserInputActive(boolean vActive) {
        mLatitudeUserInput.setActive(vActive);
    }

    public double getUserLongitude() {
        return mLongitudeUserInput.mLongitude;
    }
    public void setUserLongitude(double vUserLongitude) {
        mLongitudeUserInput.mLongitude = vUserLongitude;
    }

    public boolean isLongitudeUserInputActive() { return mLongitudeUserInput.isActive(); }
    public void setLongitudeUserInputActive(boolean vActive) {
        mLongitudeUserInput.setActive(vActive);
    }

    public String getUserComments() { return mCommentsUserInput.mComments; }
    // use tostring on charsequence coming from edittext, getText
    public void setUserComments(String vUserComments) {
        mCommentsUserInput.mComments = vUserComments; //garbage collector will keep the reference alive
    }

    public boolean isCommentsUserInputActive() { return mCommentsUserInput.isActive(); }
    public void setCommentsUserInputActive(boolean vActive) {
        mCommentsUserInput.setActive(vActive);
    }

//    @Override
//    public String toString() {
//        return super.toString();// use Utility for gps, time etc
//    }
}
