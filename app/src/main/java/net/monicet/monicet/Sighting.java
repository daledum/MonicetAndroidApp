package net.monicet.monicet;

import java.io.Serializable;

/**
 * Created by ubuntu on 23-01-2017.
 */

public class Sighting implements Serializable {

    private UserInput<Integer> mQuantityUserInput;

    private final Animal mAnimal;
    private long mTimeInMilliseconds;
    private double mLatitude;
    private double mLongitude;

    public Sighting(Animal vAnimal) {
        mAnimal = vAnimal;
        mTimeInMilliseconds = 0;
        mLatitude = 0;
        mLongitude = 0;
        mQuantityUserInput = new UserInput<Integer>(0, true);
    }

    public Sighting(Sighting vSighting) {
        this(vSighting.getAnimal());
    }

    public Animal getAnimal() {
        return mAnimal;
    }

    public UserInput<Integer> getQuantityUserInput() { return mQuantityUserInput; }

    // shorter form of getQuantityUserInput().getContent()
    public Integer getQuantity() {
        return mQuantityUserInput.getContent();
    }
    // shorter form of getQuantityUserInput().setContent()
    public void setQuantity(Integer vQuantity) {
        mQuantityUserInput.setContent(vQuantity);
    }

    public boolean isEmpty() {
        return 0 == (int) mQuantityUserInput.getContent();
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
//    @Override
//    public String toString() {
//        return super.toString();//use Utility time to string and gps to string for this
//    }
}
