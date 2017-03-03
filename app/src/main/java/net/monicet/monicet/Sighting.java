package net.monicet.monicet;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by ubuntu on 23-01-2017.
 */

public class Sighting implements Serializable {

    private Animal animal;

    private TimeAndPlace startTimeAndPlace;
    private TimeAndPlace endTimeAndPlace;

    private TimeAndPlace userStartTimeAndPlace;
    private TimeAndPlace userEndTimeAndPlace;

    private String userComments;

    public Sighting() {
        animal = null;
        startTimeAndPlace = new TimeAndPlace();
        endTimeAndPlace = new TimeAndPlace();
        userStartTimeAndPlace = new TimeAndPlace();
        userEndTimeAndPlace = new TimeAndPlace();
        userComments = "";
    }

    //this could return null
    public Animal getAnimal() {
        return animal;
    }
    public void setAnimal(Animal vAnimal) { animal = new Animal(vAnimal); }

    public TimeAndPlace getStartTimeAndPlace() { return startTimeAndPlace; }
    public void setStartTimeAndPlace(TimeAndPlace vTimeAndPlace) {
        startTimeAndPlace = vTimeAndPlace;
    }

    public TimeAndPlace getEndTimeAndPlace() { return endTimeAndPlace; }
    public void setEndTimeAndPlace(TimeAndPlace vTimeAndPlace) {
        endTimeAndPlace = vTimeAndPlace;
    }

    public TimeAndPlace getUserStartTimeAndPlace() { return userStartTimeAndPlace; }
    public void setUserStartTimeAndPlace(TimeAndPlace vTimeAndPlace) {
        userStartTimeAndPlace = vTimeAndPlace;
    }

    public TimeAndPlace getUserEndTimeAndPlace() { return userEndTimeAndPlace; }
    public void setUserEndTimeAndPlace(TimeAndPlace vTimeAndPlace) {
        userEndTimeAndPlace = vTimeAndPlace;
    }

    public String getUserComments() { return userComments; }
    public void setUserComments(String vUserComments) { userComments = vUserComments; }

}
