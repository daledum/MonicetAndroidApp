package net.monicet.monicet;

import java.io.Serializable;

/**
 * Created by ubuntu on 23-01-2017.
 */

public class Sighting implements Serializable {

    private class QuantityUserInput extends AbstractUserInput {
        private int mQuantity;
        private QuantityUserInput(int vQuantity, boolean vActive) {
            mQuantity = vQuantity;
            setActive(vActive);
        }
    }
    private QuantityUserInput mQuantityUserInput;

    private final Animal mAnimal;
    private long mTimeInMilliseconds;
    private double mLatitude;
    private double mLongitude;

    public Sighting(Animal vAnimal) {
        mAnimal = vAnimal;
        mTimeInMilliseconds = 0;
        mLatitude = 0;
        mLongitude = 0;
        mQuantityUserInput = new QuantityUserInput(0, true);
    }

    public Sighting(Sighting vSighting) {
        this(vSighting.getAnimal());
    }

    public Animal getAnimal() {
        return mAnimal;
    }

    public int getQuantity() {
        return mQuantityUserInput.mQuantity;
    }
    public void setQuantity(int vQuantity) {
        mQuantityUserInput.mQuantity = vQuantity;
    }

    public boolean isQuantityUserInputActive() { return mQuantityUserInput.isActive(); }
    public void setQuantityUserInputActive(boolean vActive) {
        mQuantityUserInput.setActive(vActive);
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
