package net.monicet.monicet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

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

    private String behavior;
    final private Map<String, Integer> associations;
    private String seaState;
    private String visibility;

    public Sighting() {
        animal = null;
        startTimeAndPlace = new TimeAndPlace();
        endTimeAndPlace = new TimeAndPlace();
        userStartTimeAndPlace = new TimeAndPlace();
        userEndTimeAndPlace = new TimeAndPlace();
        userComments = "";
        behavior = "";
        associations = new HashMap<>();
        seaState = "";
        visibility = "";
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

    // TODO: If logic is changed and one sighting will no longer be limited to having a single animal:
    // move the behaviour and associations fields to the Animal object
    public String getBehavior() { return behavior; }
    public void setBehavior(String vBehaviour) { behavior = vBehaviour; }

    public Map<String, Integer> getAssociations() { return associations; }

    public String getSeaState() { return seaState; }
    public void setSeaState(String vSeaState) { seaState = vSeaState; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String vVisibility) { visibility = vVisibility; }
}
