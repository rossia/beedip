package it.uniurb.beedip.data;

/**
 * Created by rossia on 11/24/2017.
 */

// TODO: mettere la classe insieme a CompassFragment
public class CompassMeasurement {
    private int dip; //degree 0-90
    private int dipDirection; // degree 0-360
    private Orientation orientation;
    // type : Upright/OverTurned/horizontal/Vertical
    private boolean isAccurate;

    public CompassMeasurement(double dip, double dipDirection, boolean isAccurate) {
        this.dip = (int) dip;
        this.dipDirection = (int) dipDirection;
        this.isAccurate = isAccurate;
        // TODO vedere se utilizzare i metodi Validate o Precondition;
        assert dip >= 0 && dip <= 90;
        assert dipDirection >= 0 && dipDirection < 360;

    }
    public CompassMeasurement(int dip, int dipDirection, Orientation orientation, boolean isAccurate) {
        this.dip = dip;
        this.dipDirection = dipDirection;
        this.isAccurate = isAccurate;
        this.orientation = orientation;
        // TODO vedere se utilizzare i metodi Validate o Precondition;
        assert dip >= 0 && dip <= 90;
        assert dipDirection >= 0 && dipDirection < 360;

    }

    public enum Orientation {
        UPRIGHT, OVERTURNED;
        public String toString() {
            String id = name();
            String lower = id.substring(1).toLowerCase();
            return id.charAt(0) + lower;
        }
        public int ValueOf() {
            String id = name();
            int rval = -1;
            switch (id) {
                case "UPRIGHT":
                    rval = 1;
                    break;
                case "OVERTURNED":
                    rval = 2;
                    break;
            }
            return rval;
        }
    }

    public int getDip() {
        return dip;
    }
    public int getDipDirection() {
        return dipDirection;
    }
    public int getOrientation() {return orientation.ValueOf();}

}
