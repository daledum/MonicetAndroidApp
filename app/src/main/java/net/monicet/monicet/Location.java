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

    //these are the user provided GPS coordinates (from their own, additional, separate device)
    private UserInput<Double> gLatitudeUserInput;
    private UserInput<Double> gLongitudeUserInput;
    private UserInput<String> gCommentsUserInput;

    public Location(ArrayList<Sighting> vSightingsArray) {
        mSightingsArray = vSightingsArray;
        mTimeInMilliseconds = 0;
        mLatitude = 0;
        mLongitude = 0;

        // when constructing from an array of sightings, we don't copy any state
        gLatitudeUserInput = new UserInput<Double>(0.0, true);
        gLongitudeUserInput = new UserInput<Double>(0.0, true);
        gCommentsUserInput = new UserInput<String>("", true);

    }

    // Copy the visibility of the user changeable variables across locations, so we know if we should
    // display the comments dialog or not
    public Location(ArrayList<Sighting> vSightingsArray,
                    boolean vLatVisible, boolean vLongVisible, boolean vComVisible) {
        this(vSightingsArray);
        gLatitudeUserInput.setVisible(vLatVisible);
        gLongitudeUserInput.setVisible(vLongVisible);
        gCommentsUserInput.setVisible(vComVisible);
    }

    // This passes the state of each user changeable variable into the constructor
    public Location(Location vLocation) {
        this(vLocation.getBlankSightings(), vLocation.getLatitudeUserInput().isVisible(),
                vLocation.getLongitudeUserInput().isVisible(), vLocation.getCommentsUserInput().isVisible());
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

    public UserInput<Double> getLatitudeUserInput() { return gLatitudeUserInput; }
    public Double getAdditionalLatitude() { return gLatitudeUserInput.getContent(); }
    public void setAdditionalLatitude(Double vAdditionalLatitude) {
        gLatitudeUserInput.setContent(vAdditionalLatitude);
    }

    public UserInput<Double> getLongitudeUserInput() { return gLongitudeUserInput; }
    public Double getAdditionalLongitude() {
        return gLongitudeUserInput.getContent();
    }
    public void setAdditionalLongitude(Double vAdditionalLongitude) {
        gLongitudeUserInput.setContent(vAdditionalLongitude);
    }

    public UserInput<String> getCommentsUserInput() { return gCommentsUserInput; }
    public String getComments() { return gCommentsUserInput.getContent(); }
    // use tostring on charsequence coming from edittext, getText
    public void setComments(String vComments) {
        gCommentsUserInput.setContent(vComments); //garbage collector will keep the reference alive
    }

//    @Override
//    public String toString() {
//        return super.toString();// use Utility for gps, time etc
//    }
}
