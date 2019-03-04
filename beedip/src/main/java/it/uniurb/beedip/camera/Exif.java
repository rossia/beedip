package it.uniurb.beedip.camera;

import android.location.Location;
import android.media.ExifInterface;

import java.io.IOException;
import java.util.Locale;

public class Exif {
    private ExifInterface exif;
    private Integer azimuth;
    private Integer dip;
    private Location location;

    public Exif(String filename) throws IOException {
        this.exif = new ExifInterface(filename);
    }

    public void setAzimuth(int azimuth) {
        this.azimuth = azimuth;
    }

    public void setDip(int dip) {
        this.dip = dip;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void saveAttributes() throws IOException {
        saveDipAndAzimuth();

        if (location != null) {
            saveLatitude();
            saveLongitude();
        }

        exif.saveAttributes();
    }

    private void saveLatitude() {
        double latitude = location.getLatitude();
        String latitudeRef = "N";
        if (latitude < 0) {
            latitude = -latitude;
            latitudeRef = "S";
        }
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitudeRef);
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, formatLatLongString(latitude));
    }

    private void saveLongitude() {
        double longitude = location.getLongitude();
        String longitudeRef = "E";
        if (longitude < 0) {
            longitude = -longitude;
            longitudeRef = "W";
        }
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitudeRef);
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, formatLatLongString(longitude));
    }

    private void saveDipAndAzimuth() {
        String dipAndAzimuth = String.format(Locale.ENGLISH, "dip:%d; azimuth:%d", dip, azimuth);
        exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, dipAndAzimuth);
    }

    private static String formatLatLongString(double d) {
        StringBuilder b = new StringBuilder();
        b.append((int) d);
        b.append("/1,");
        d = (d - (int) d) * 60;
        b.append((int) d);
        b.append("/1,");
        d = (d - (int) d) * 60000;
        b.append((int) d);
        b.append("/1000");
        return b.toString();
    }
}
