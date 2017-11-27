package it.uniurb.beedip.data;

/**
 * Created by rossia on 11/24/2017.
 */

public class CompassMeasurement {
    private double dip; //degree 0-90
    private double dipDirection; // degree 0-360
    // type : Upright/OverTurned/horizontal/Vertical
    private boolean isAccurate;

    public CompassMeasurement(double dip, double dipDirection, boolean isAccurate) {
        // @todo vedere se utilizzare i metodi Validate o Precondition;
        assert dip >= 0 && dip <= 90.00;
        assert dipDirection >= 0 && dipDirection < 360.00;
        this.dip = dip;
        this.dipDirection = dipDirection;
        this.isAccurate = isAccurate;
    }
    public int getDip() {
        //@todo cast da rimuovere
        return ((int) dip);
    }
    public int getDipDirection() {
        //@todo cast da rimuovere
        return ((int) dipDirection);
    }
}
