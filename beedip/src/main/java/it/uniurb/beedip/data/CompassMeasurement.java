package it.uniurb.beedip.data;

import android.support.annotation.NonNull;

/**
 * Created by rossia on 11/24/2017.
 */

// TODO: mettere la classe insieme a CompassFragment
public class CompassMeasurement {
    private int dip = -1; //degree 0-90
    private int dipDirection = -1; // degree 0-360
    private int younging;

    private int isAccurate;
    private String rockUnit;
    private String site;
    private String surveyor;
    private String note;



    public CompassMeasurement(int dip, int dipDirection, Younging younging, boolean isAccurate) {
        if (dip >= 0 && dip <= 90)
            this.dip = dip;
        if (dipDirection >= 0 && dipDirection < 360)
            this.dipDirection = dipDirection;
        setAccurate(isAccurate);
        this.younging = younging.getValue();
    }
    public void setAccurate(boolean accurate) {
        if (accurate)
            isAccurate = 0;
        else
            this.isAccurate = 1;
    }
    public void setRockUnit(String rockUnit) { this.rockUnit = rockUnit; }
    public void setSite(String site) { this.site = site; }
    public void setSurveyor(String surveyor) { this.surveyor = surveyor;  }
    public void setNote(String note) { this.note = note; }
    public enum Younging {
        UPRIGHT(0), OVERTURNED(1);
        private final int value;
        Younging(int value) { this.value = value;}
        public String toString() {
            return String.valueOf(value);
        }
        public int getValue() {
            return value;
        }
    }
    public enum SurfaceType {
        BEDDING(0), CLEAVAGE(1), JOINT(2), FAULT(3), LINEATION(4);
        private final int value;
        SurfaceType(int value) { this.value = value;}
        public String toString() {
            String s = "";
            switch (value) {
                case 0:
                    s = "bedding";
                    break;
                case 1:
                    s = "cleavage";
                    break;
                case 2:
                    s = "joint";
                    break;
                case 3:
                    s = "fault";
                    break;
                case 4:
                    s = "lineation";
                    break;
            }
            return s;
        }
    }
    public enum FaultType {
        NORMAL(0), REVERSE(1), Dextral_SS(2), SINIXTRAL_SS(3), UNDEFINED(4);
        private final int value;
        private static final int size = FaultType.values().length;
        FaultType(int value) { this.value = value;}
        @NonNull
        public String toString() {
            return String.valueOf(value);
        }
        public int getValue() {
            return value;
        }
        public static int size() {return size;}
    }
    public int getDip() {
        return dip;
    }
    public int getDipDirection() {
        return dipDirection;
    }
    public int getYounging() {return younging;}
    public String getRockUnit() {return rockUnit;}
    public String getSite() {return site;}
    public String getSurveyor() { return surveyor; }
    public String getNote() { return note; }
    public int isAccurate() {return isAccurate; }

}
