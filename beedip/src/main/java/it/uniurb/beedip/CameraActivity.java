package it.uniurb.beedip;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;

import it.uniurb.beedip.camera.LocationService;
import it.uniurb.beedip.camera.PersistentPicture;
import it.uniurb.beedip.data.GeoPackageDatabase;
import it.uniurb.beedip.data.GeoPackageDatabases;

public class CameraActivity extends Activity implements SensorEventListener {

    private static final String TAG = "CameraActivity";

    private Camera mCamera;
    private CameraPreview mPreview;

    TextView textViewDip, textViewAzimuth;

    int dip, azimuth;

    private SensorManager mSensorManager;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

    private LocationService locationService = new LocationService(this);

    private GeoPackageDatabase selectedDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        mPreview = new CameraPreview(this);

        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        ImageButton captureButton = (ImageButton) findViewById(R.id.button);
        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCamera.takePicture(null, null, mPicture);
            }
        });

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        textViewDip = (TextView) findViewById(R.id.dip);
        textViewAzimuth = (TextView) findViewById(R.id.azimuth);

        locationService.enable();

        initSelectedDatabase();
        checkDatabaseIsSelected();

    }

    private void initSelectedDatabase() {
        GeoPackageDatabase[] dbs = GeoPackageDatabases.getInstance(this)
                .getDatabases().toArray(new GeoPackageDatabase[0]);

        if (GeoPackageDatabases.getInstance(this) == null || dbs.length == 0)
            selectedDatabase = null;
        else
            selectedDatabase = dbs[0];
    }

    private void checkDatabaseIsSelected() {
        if (selectedDatabase == null) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyle)
                    .setTitle(R.string.camera_project_db_alert_title)
                    .setMessage(R.string.camera_project_db_alert_message)
                    .setPositiveButton(R.string.button_ok_label,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                }
                            });
            dialog.show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    Camera.PictureCallback mPicture = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                savePicture(data);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                throw new RuntimeException(e);
            }

            mCamera.startPreview();
        }
    };

    private void savePicture(byte[] data) throws IOException {
        Location location = locationService.getLocation();

        PersistentPicture picture = new PersistentPicture(data);
        picture.setAzimuth(azimuth);
        picture.setDip(dip);
        if (isLocationAvailable(location))
            picture.setLocation(location);

        picture.save(getDatabaseName());

        String baseMessage = "Photo saved under Pictures/BeeDip";
        if (isLocationAvailable(location))
            Toast.makeText(getApplicationContext(), baseMessage, Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(getBaseContext(), baseMessage + "\nGPS data unavailable", Toast.LENGTH_LONG).show();
    }

    private boolean isLocationAvailable(Location location) {
        return location != null;
    }

    private String getDatabaseName() {
        if (selectedDatabase != null && selectedDatabase.getDatabase() != null) {
            return selectedDatabase.getDatabase();
        } else {
            return "NoProject";
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();

        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            mSensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            mSensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }

        mCamera = Camera.open();

        mPreview.setCamera(mCamera);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSensorManager.unregisterListener(this);

        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mAccelerometerReading, 0, mAccelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMagnetometerReading, 0, mMagnetometerReading.length);
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER || event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

            boolean success = SensorManager.getRotationMatrix(mRotationMatrix, null,
                    mAccelerometerReading, mMagnetometerReading);

            if (success) {
                SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

                dip = (int) Math.toDegrees(mOrientationAngles[2]);
                if (dip < 0)
                    dip = Math.abs(dip);
                dip -= 90;

                azimuth = (int) Math.toDegrees(mOrientationAngles[0]);
                azimuth += 360;
                if (azimuth >= 360)
                    azimuth -= 360;
                azimuth += 90;
                if (azimuth >= 360)
                    azimuth -= 360;
            }

            textViewDip.setText(formatText(R.string.dip_format, dip));
            textViewAzimuth.setText(formatText(R.string.azimuth_format, azimuth));
        }
    }

    private String formatText(int resId, double value) {
        String formattedValue = String.format(Locale.ENGLISH, "%d", Math.round(value));
        return String.format(getString(resId), formattedValue);
    }
}
