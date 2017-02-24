package net.monicet.monicet;

import java.io.Serializable;

/**
 * Created by ubuntu on 23-01-2017.
 */

public class Animal implements Serializable {

    // The specie field is retrieved from resources species array string
    private String mSpecie;

    // Link to drawable resource image(s)
    private String mPhoto;

    // Link to text file
    private String mDescription;

    public Animal(String vSpecie, String vPhoto, String vDescription) {
        mSpecie = vSpecie;
        mPhoto = vPhoto;
        mDescription = vDescription;
    }

    public String getSpecie() {
        return mSpecie;
    }

    public String getPhoto() {
        return mPhoto;
    }

    public String getDescription() {
        return mDescription;
    }

}
