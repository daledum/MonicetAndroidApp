package net.monicet.monicet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import static android.media.CamcorderProfile.get;

/**
 * Created by ubuntu on 23-01-2017.
 */

public class Trip implements Serializable {

    private ArrayList<Sighting> mSightingsArray;
    private long mId;
    private String mUserName;
    private TimeAndPlace startTimeAndPlace;
    private TimeAndPlace endTimeAndPlace;
    private volatile GpsMode gpsMode;

    public class MyFile {
        private String fileTitle;
        private AllowedFileExtension fileExtension;

        private MyFile() {
            fileTitle = "";
            fileExtension = AllowedFileExtension.JSON;
        }

        public String getFileTitle() { return fileTitle; }
        public void setFileTitle(String vFileTitle) { fileTitle = vFileTitle; }

        public AllowedFileExtension getFileExtension() { return fileExtension; }
        public void setFileExtension(AllowedFileExtension vFileExtension) {
            fileExtension = vFileExtension;
        }
    }

    final private MyFile tripFile;
    final private MyFile routeFile;

    public Trip() {
        mSightingsArray = new ArrayList<Sighting>();
        mId = 0;
        mUserName = "";
        startTimeAndPlace = new TimeAndPlace();
        endTimeAndPlace = new TimeAndPlace();
        //mRouteData = new HashMap<Long,double[]>();// get rid
        gpsMode = GpsMode.USER_5_MIN;

        tripFile = new MyFile();
        routeFile = new MyFile();
    }

    public ArrayList<Sighting> getSightings() { return mSightingsArray; }
    public int getNumberOfSightings() { return mSightingsArray.size(); }
    public Sighting getLastCreatedSighting() {
        //if (mSightingsArray.size() == 0) { return null; }//TODO: should I uncomment this?
        return mSightingsArray.get(mSightingsArray.size() - 1);
    }

    public long getId() { return mId; }
    public void setId(long vId) { mId = vId; }

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

    public GpsMode getGpsMode() { return gpsMode; }
    public synchronized void setGpsMode(GpsMode vGpsMode) { gpsMode = vGpsMode; }

    public MyFile getTripFile() { return tripFile; }
    public MyFile getRouteFile() { return routeFile; }

}
