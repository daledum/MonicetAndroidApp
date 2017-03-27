package net.monicet.monicet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import static android.media.CamcorderProfile.get;

/**
 * Created by ubuntu on 23-01-2017.
 */

public class Trip implements Serializable {

    //private UserInput<GpsMode> mGpsModeUserInput;
    private volatile GpsMode gpsMode; // volatile because different threads read it (only the UI thread writes it) - get rid of these

    private ArrayList<Sighting> mSightingsArray;
    private String mUserName;
    private TimeAndPlace startTimeAndPlace;
    private TimeAndPlace endTimeAndPlace;
    private String mTripFileName;
    private String mRouteFileName;
    // continuousGpsSamples, continuousDateTime (Set): these will be empty by default.
    // If in Continuous Tracking Mode (DialogFragment Tracking YES), a lot of data will be added to the sets very often.
    // This will not be added to the JSON file.
    // A file with the appropriate extension (TRK, GPX, KML, KMZ, PLT) will be created/saved/sent
    // and its name will be assigned to to the routeFileName variable, which will appear in the JSON file.
    private transient HashMap<Long,double[]> mRouteData;//or use a map instead of double[]

    public Trip() {
        mSightingsArray = new ArrayList<Sighting>();
        mUserName = "";
        startTimeAndPlace = new TimeAndPlace();
        endTimeAndPlace = new TimeAndPlace();
        gpsMode = GpsMode.GPS_FIXING_FAST;//get rid
        mTripFileName = "";
        mRouteFileName = "";
        mRouteData = new HashMap<Long,double[]>();
    }

    public ArrayList<Sighting> getSightings() { return mSightingsArray; }
    public int getNumberOfSightings() { return mSightingsArray.size(); }
    public Sighting getLastCreatedSighting() {
        //if (mSightingsArray.size() == 0) { return null; }//TODO: should I uncomment this?
        return mSightingsArray.get(mSightingsArray.size() - 1);
    }

    public String getUserName() { return mUserName; }
    public void setUserName(String vUserName) { mUserName = vUserName; }

    public TimeAndPlace getStartTimeAndPlace() { return startTimeAndPlace; }
    public void setStartTimeAndPlace(TimeAndPlace vTimeAndPlace) {
        startTimeAndPlace = vTimeAndPlace;
    }

    public TimeAndPlace getEndTimeAndPlace() { return endTimeAndPlace; }
    public void setEndTimeAndPlace(TimeAndPlace vTimeAndPlace) {
        endTimeAndPlace = vTimeAndPlace;
    }

    //    public UserInput<GpsMode> getGpsModeUserInput() { return mGpsModeUserInput; }
//    public GpsMode getGpsMode() { return mGpsModeUserInput.getContent(); }
//    public void setGpsMode(GpsMode vGpsMode) {
//        mGpsModeUserInput.setContent(vGpsMode);
//    }

    //get rid
    public GpsMode getGpsMode() { return gpsMode; }
    public void setGpsMode(GpsMode vGpsMode) { gpsMode = vGpsMode; } // synchronized - NO, only one thread (main UI thread sets it)

    public String getTripFileName() { return mTripFileName; }
    public void setTripFileName(String vTripFileName) { mTripFileName = vTripFileName; }

    public String getRouteFileName() { return mRouteFileName; }
    public void setRouteFileName(String vRouteFileName) {
        mRouteFileName = vRouteFileName; //only if in Tracking mode
    }

    public HashMap<Long,double[]> getRouteData() { return mRouteData; }
    public void addRouteData(long vTimeInMilliseconds, double vLatitude, double vLongitude ) {
        mRouteData.put(vTimeInMilliseconds, new double[]{vLatitude,vLongitude});
    }

}
