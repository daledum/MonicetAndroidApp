package net.monicet.monicet;

import java.io.Serializable;

/**
 * Created by ubuntu on 23-01-2017.
 */

public class Specie implements Serializable {

    // The specie field is retrieved from resources species array string
    private String mName;
    private String mLatinName;
    private String mFamily;
    private int mRank;

    // Link to drawable resource image(s)
    private String mPhoto;

    // Link to text file
    private String mDescription;

    public Specie(String vName, String vLatinName, String vFamily, int vRank, String vPhoto, String vDescription) {
        mName = vName;
        mLatinName = vLatinName;
        mFamily = vFamily;
        mRank = vRank;
        mPhoto = vPhoto;
        mDescription = vDescription;
    }

    public String getName() {
        return mName;
    }

    public String getLatinName() { return mLatinName; }

    public void setLatinName(String vLatinName) { mLatinName = vLatinName; }

    public String getFamily() { return mFamily; }

    public int getRank() { return mRank; }

    public String getPhoto() {
        return mPhoto;
    }

    public String getDescription() {
        return mDescription;
    }

}
