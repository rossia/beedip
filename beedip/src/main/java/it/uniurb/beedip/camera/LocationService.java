package it.uniurb.beedip.camera;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Button;

import com.google.android.gms.maps.model.LatLng;

import static android.content.Context.LOCATION_SERVICE;

public class LocationService {

    private Location mLocation;
    private Activity mActivity;

    public LocationService(Activity mActivity) {
        this.mActivity = mActivity;
    }

    public void enable() {
        LocationManager mLocationManager = (LocationManager) mActivity.getSystemService(LOCATION_SERVICE);

        mLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (mLocation == null) {
            mLocation = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (ActivityCompat.checkSelfPermission(mActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(mActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //Requesting the permission to the user
            //android.Manifest.permission.ACCESS_FINE_LOCATION
            //ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        //Getting coarse position.
        try {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new android.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    mLocation = location;
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
            //If able to, overwriting coarse position with finer one.
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000/*1000 * 60*/, 2, new android.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    mLocation = location;
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            });
        }
        catch(NullPointerException e){
            Log.e("Error", "Error"+e);
        }
    }


    public Location getLocation() {

        return mLocation;
    }
}
