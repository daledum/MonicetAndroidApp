package net.monicet.monicet;

import android.util.Log;
import android.widget.NumberPicker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static android.R.attr.mode;
import static android.R.attr.start;
import static android.R.id.empty;
import static android.R.string.no;
import static android.content.ContentValues.TAG;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static android.icu.text.Normalizer.YES;
import static android.media.CamcorderProfile.get;
import static net.monicet.monicet.GpsMode.OFF;

/**
 * Created by ubuntu on 23-01-2017.
 */

public class Trip implements Serializable {

    private class GpsModeUserInput extends AbstractUserInput {
        private GpsMode mGpsMode;
        private GpsModeUserInput(GpsMode vGpsMode, boolean vActive) {
            mGpsMode = vGpsMode;
            setActive(vActive);
        }
    }
    GpsModeUserInput mGpsModeUserInput;

    private ArrayList<Location> mLocationsArray;
    private String mUserName;
    private long mStartTimeInMilliseconds;
    private long mEndTimeInMilliseconds;
    private double mStartLatitude;
    private double mStartLongitude;
    private double mEndLatitude;
    private double mEndLongitude;

    private String mRouteFileName;
    // continuousGpsSamples, continuousDateTime (Set): these will be empty by default.
    // If in Continuous Tracking Mode (DialogFragment Tracking YES), a lot of data will be added to the sets very often.
    // This will not be added to the JSON file.
    // A file with the appropriate extension (TRK, GPX, KML, KMZ, PLT) will be created/saved/sent
    // and its name will be assigned to to the routeFileName variable, which will appear in the JSON file.
    private HashMap<Long,double[]> mContinuousData;//or use a map instead of double[]

    public Trip(Location vLocation) {
        mUserName = "";
        mStartTimeInMilliseconds = 0;
        mEndTimeInMilliseconds = 0;
        mStartLatitude = 0;
        mStartLongitude = 0;
        mEndLatitude = 0;
        mEndLongitude = 0;

        // at the beginning of a trip: gps is off, tracking dialog (active state) is on (and comments are on)
        mGpsModeUserInput = new GpsModeUserInput(GpsMode.OFF, true);

        mRouteFileName = "";
        mContinuousData = new HashMap<Long,double[]>();
        mLocationsArray = new ArrayList<Location>();
        mLocationsArray.add(vLocation);
    }

    // not implementing this - programmer will have too much power, removing and adding locations and breaking the logic,
    // the trip must always have at least one location and adding locations is done only via addLocation
//    public ArrayList<Location> getLocations() { return mLocationsArray; }

    public int getNumberOfLocations() { return mLocationsArray.size(); }
    public Location getLocationAtIndex(int vIndex) { return mLocationsArray.get(vIndex); }
    public Location getCurrentLocation() { return mLocationsArray.get(mLocationsArray.size() - 1); }

    // the trip will always have at least one location (at index 0), due to its constructor
    // add a new, barebone(or blank) location (only containing species, photos and descriptions)
//    public void addLocation() {
//        Location firstLocation = mLocationsArray.get(0);
//        mLocationsArray.add(new Location(firstLocation));
//    }
    // addLocation is called on a Trip instance, therefore a trip with at least one Location
    // I am using the currentLocation, instead of the first Location (at index 0), because I ...
    // want its user_comments and gps active status, so that I know to ask for them or not
    // if user disabled the 'comments' message when saving a location, that gets sent through this ...
    // mechanism to the next location (via 'cloning' from the current one)
    // The only way to pass this info (user no longer wants to write comments) via locations, without using the trip
    public void addLocation() {
        Location currentLocation = mLocationsArray.get(mLocationsArray.size() - 1);
        mLocationsArray.add(new Location(currentLocation));
    }

//    public void addLocation(int s) { mLocationsArray.add(getBlankLocation()); }
//    }
//    private Location getBlankLocation() {
//        Location blankLocation = new Location(mLocationsArray.get(0));
//        return blankLocation;
//    }


    public GpsMode getGpsMode() { return mGpsModeUserInput.mGpsMode; }
    public void setGpsMode(GpsMode vGpsMode) {
        mGpsModeUserInput.mGpsMode = vGpsMode;
    }

    public boolean isGpsModeUserInputActive() {
        return mGpsModeUserInput.isActive();
    }

    public void setGpsModeUserInputActive(boolean vActive) {
        mGpsModeUserInput.setActive(vActive);
    }


    public String getRouteFileName() { return mRouteFileName; }
    public void setRouteFileName(String vRouteFileName) {
        mRouteFileName = vRouteFileName; //only if in Tracking mode
    }

    public HashMap<Long,double[]> getContinuousData() { return mContinuousData; }
    public void addContinuousData(long vTimeInMilliseconds, double vLatitude, double vLongitude ) {
        mContinuousData.put(Long.valueOf(vTimeInMilliseconds), new double[]{vLatitude,vLongitude});
    }

    public String getUserName() { return mUserName; }
    public void setUserName(String vUserName) { mUserName = vUserName; }

    public long getStartTimeInMilliseconds() { return mStartTimeInMilliseconds; }
    public void setStartTimeInMilliseconds(long vStartTimeInMilliseconds) {
        mStartTimeInMilliseconds = vStartTimeInMilliseconds;
    }

    public long getEndTimeInMilliseconds() { return mEndTimeInMilliseconds; }
    public void setEndTimeInMilliseconds(long vEndTimeInMilliseconds) {
        mEndTimeInMilliseconds = vEndTimeInMilliseconds;
    }

    // start_gps: initially empty.
    // Get after having connected to Google Location API (which will start sampling very infrequently
    // before the first NumberPicker click, when in no tracking mode).
    // Stop after the first sample (just check it's not empty, and only then assign).
    public double getStartLatitude() { return mStartLatitude; }
    public void setStartLatitude(double vStartLatitude) { mStartLatitude = vStartLatitude; }

    public double getStartLongitude() { return mStartLongitude; }
    public void setStartLongitude(double vStartLongitude) { mStartLongitude = vStartLongitude; }

    public double getEndLatitude() { return mEndLatitude; }
    public void setEndLatitude(double vEndLatitude) { mEndLatitude = vEndLatitude; }

    public double getEndLongitude() { return mEndLongitude; }
    public void setEndLongitude(double vEndLongitude) { mEndLongitude = vEndLongitude; }

//    @Override
//    public String toString() {
//        return super.toString();//time and lat utility methods
//    }
}
