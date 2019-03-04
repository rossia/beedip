package it.uniurb.beedip.camera;

import android.location.Location;

public abstract class PersistentPicture {
    protected byte[] data;
    protected Integer azimuth;
    protected Integer dip;
    protected Location location;

    public PersistentPicture(byte[] data) {
        this.data = data;
    }

    public void setAzimuth(Integer azimuth) {
        this.azimuth = azimuth;
    }

    public void setDip(Integer dip) {
        this.dip = dip;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public abstract void save(String baseName) throws Exception;
}
