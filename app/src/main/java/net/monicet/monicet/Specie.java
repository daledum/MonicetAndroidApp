package net.monicet.monicet;

import java.io.Serializable;

/**
 * Created by ubuntu on 23-01-2017.
 */

public class Specie implements Serializable {

    // The specie field is retrieved from resources species array string
    private String mName;

    // Link to drawable resource image(s)
    private String mPhoto;

    // Link to text file
    private String mDescription;

    public Specie(String vName, String vPhoto, String vDescription) {
        mName = vName;
        mPhoto = vPhoto;
        mDescription = vDescription;
    }

    public String getName() {
        return mName;
    }

    public String getPhoto() {
        return mPhoto;
    }

    public String getDescription() {
        return mDescription;
    }

}
