package net.monicet.monicet;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;
import static android.media.CamcorderProfile.get;

/**
 * Created by ubuntu on 23-01-2017.
 */

public class Trip implements Serializable {

    private UserInput<GpsMode> mGpsModeUserInput;
    private ArrayList<Location> mLocationsArray;
    private String mUserName;
    private long mStartTimeInMilliseconds;
    private long mEndTimeInMilliseconds;
    private double mStartLatitude;
    private double mStartLongitude;
    private double mEndLatitude;
    private double mEndLongitude;

    private String mTripFileName;
    private String mRouteFileName;
    // continuousGpsSamples, continuousDateTime (Set): these will be empty by default.
    // If in Continuous Tracking Mode (DialogFragment Tracking YES), a lot of data will be added to the sets very often.
    // This will not be added to the JSON file.
    // A file with the appropriate extension (TRK, GPX, KML, KMZ, PLT) will be created/saved/sent
    // and its name will be assigned to to the routeFileName variable, which will appear in the JSON file.
    private transient HashMap<Long,double[]> mContinuousData;//or use a map instead of double[]

    public Trip() {
        mUserName = "";
        mStartTimeInMilliseconds = 0;
        mEndTimeInMilliseconds = 0;
        mStartLatitude = 0;
        mStartLongitude = 0;
        mEndLatitude = 0;
        mEndLongitude = 0;
        mGpsModeUserInput = new UserInput<GpsMode>(GpsMode.OFF, true);
        mTripFileName = "";
        mRouteFileName = "";
        mContinuousData = new HashMap<Long,double[]>();
        mLocationsArray = new ArrayList<Location>();
    }

    // not implementing this - programmer will have too much power, removing and adding locations and breaking the logic,
    // the trip must always have at least one location and adding locations is done only via addLocation
//    public ArrayList<Location> getLocations() { return mLocationsArray; }

    public int getNumberOfLocations() { return mLocationsArray.size(); }
    public Location getLocationAtIndex(int vIndex) { return mLocationsArray.get(vIndex); }
    public Location getCurrentLocation() {
        // TODO: decide if this should return null or not
        // get rid of this when deploying, do I really need this check?, non-dry MainActivity uses this check, too
        if (getNumberOfLocations() == 0) {
            Log.d("MainActivity,Trip class", "Getting a Location from a trip with no locations");
            return null;
        }
        return mLocationsArray.get(getNumberOfLocations() - 1); // Alex: was mLocationsArray.size()
    }

    public Location getSeedLocation() {
        return getCurrentLocation();
    }

    // the trip will always have at least one location (at index 0), due to its constructor - not true anymore
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
//    public void addLocation() { // get rid
//        // make sure it has a location already
//        // getcurrentlocation == null ? .add(new Location(from resources), .add(new Location(getcurrentLoc)
//        Location currentLocation = getCurrentLocation(); // mLocationsArray.get(mLocationsArray.size() - 1);
//        mLocationsArray.add(new Location(currentLocation));
//    }

    public void addLocation(Location location) { // use this instead
        mLocationsArray.add(location);
    }

//    public void addLocation(int s) { mLocationsArray.add(getBlankLocation()); }
//    }
//    private Location getBlankLocation() {
//        Location blankLocation = new Location(mLocationsArray.get(0));
//        return blankLocation;
//    }

    public UserInput<GpsMode> getGpsModeUserInput() { return mGpsModeUserInput; }

    public GpsMode getGpsMode() { return mGpsModeUserInput.getContent(); }
    public void setGpsMode(GpsMode vGpsMode) {
        mGpsModeUserInput.setContent(vGpsMode);
    }

    public String getTripFileName() { return mTripFileName; }
    public void setTripFileName(String vTripFileName) { mTripFileName = vTripFileName; }

    public String getRouteFileName() { return mRouteFileName; }
    public void setRouteFileName(String vRouteFileName) {
        mRouteFileName = vRouteFileName; //only if in Tracking mode
    }

    public HashMap<Long,double[]> getContinuousData() { return mContinuousData; }
    public void addContinuousData(long vTimeInMilliseconds, double vLatitude, double vLongitude ) {
        mContinuousData.put(vTimeInMilliseconds, new double[]{vLatitude,vLongitude});
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

}
