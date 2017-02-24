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
    private UserInput<Double> mLatitudeUserInput;
    private UserInput<Double> mLongitudeUserInput;
    private UserInput<String> mCommentsUserInput;

    public Location(ArrayList<Sighting> vSightingsArray) {
        mSightingsArray = vSightingsArray;
        mTimeInMilliseconds = 0;
        mLatitude = 0;
        mLongitude = 0;

        // when constructing from an array of sightings, we don't copy any state
        mLatitudeUserInput = new UserInput<Double>(0.0, true);
        mLongitudeUserInput = new UserInput<Double>(0.0, true);
        mCommentsUserInput = new UserInput<String>("", true);

    }

    // Copy the visibility of the user changeable variables across locations, so we know if we should
    // display the comments dialog or not
    public Location(ArrayList<Sighting> vSightingsArray,
                    boolean vLatVisible, boolean vLongVisible, boolean vComVisible) {
        this(vSightingsArray);
        mLatitudeUserInput.setVisible(vLatVisible);
        mLongitudeUserInput.setVisible(vLongVisible);
        mCommentsUserInput.setVisible(vComVisible);
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
            Log.d("MainActivity,LocatClass", "Building a Location from a location with no sightings");
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

    public UserInput<Double> getLatitudeUserInput() { return mLatitudeUserInput; }
    public Double getAdditionalLatitude() { return mLatitudeUserInput.getContent(); }
    public void setAdditionalLatitude(Double vAdditionalLatitude) {
        mLatitudeUserInput.setContent(vAdditionalLatitude);
    }

    public UserInput<Double> getLongitudeUserInput() { return mLongitudeUserInput; }
    public Double getAdditionalLongitude() {
        return mLongitudeUserInput.getContent();
    }
    public void setAdditionalLongitude(Double vAdditionalLongitude) {
        mLongitudeUserInput.setContent(vAdditionalLongitude);
    }

    public UserInput<String> getCommentsUserInput() { return mCommentsUserInput; }
    public String getComments() { return mCommentsUserInput.getContent(); }
    // use tostring on charsequence coming from edittext, getText
    public void setComments(String vComments) {
        mCommentsUserInput.setContent(vComments); //garbage collector will keep the reference alive
    }

}
