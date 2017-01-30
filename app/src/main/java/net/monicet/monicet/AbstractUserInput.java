package net.monicet.monicet;

/**
 * Created by ubuntu on 30-01-2017.
 */

// The purpose of this class is to wrap each of the variables directly changeable by
// the user (via a dialog, button etc), so that I know if this info was already retrieved from the user
// I will use this info to decide if I want to ask for this data again (maybe the user chose to not fill this in again,
// or maybe the user already filled this in (and the filling in should have been a one-time thing - but, because
// of how apps work... they call onCreate etc again), so, don't bother the user again (register inside the object
// that the user was asked for this already)

public abstract class AbstractUserInput {
    private boolean mActive;

    // if it's active that means that I want to ask the user to give me this again
    // if it's not active, that means I don't want to bother the user again
    public boolean isActive() { return mActive; }
    public void setActive(boolean vActive) { mActive = vActive; }
}
