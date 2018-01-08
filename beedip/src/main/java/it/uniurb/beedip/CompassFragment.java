package it.uniurb.beedip;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

import it.uniurb.beedip.data.CompassMeasurement;
import it.uniurb.beedip.data.GeoPackageDatabases;
import it.uniurb.beedip.data.OnMeasurementSentListener;
import android.graphics.Color;

import java.util.List;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.core.contents.Contents;
import mil.nga.geopackage.core.contents.ContentsDao;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter;

import static android.content.Context.LOCATION_SERVICE;


/**
 * Created by utente on 22/10/2017.
 */

public class CompassFragment extends Fragment implements SensorEventListener {

    //Dichiarazione variabili geopackage dao
    // TODO impostare private datasource

    /**
     * GeoPackage manager
     */
    private GeoPackageManager manager;

    /**
     * Active GeoPackages
     */
    private GeoPackageDatabases active;

    /**
     * Edit features database
     */
    private List<String> features;
    private String editFeaturesDatabase;

    /**
     * Edit features table
     */
    private String editFeaturesTable;

    /**
     * features table toast
     */

    Toast featureTableToast;

    View myView;
    private static SensorManager sensorService;
    private Sensor sensor;
    //private CompassMeasurement compassMesurement;
    //private int clickCounter;
    //private Bussola bussola;
    //private Inclinometer inclinometro;
    //private CharSequence choices[];
    private int prevDipangle;
    private boolean msrLock;
    private boolean cfState;
    //private boolean allowToSave;
    String tmp;
    ImageView lIndicator;
    ImageView bIndicator;
    TextView displayValues;
    ImageButton bigClockFace;
    ImageButton littleClockFace;
    Button rockUnit;
    Button locality;
    Button type;
    Button accuracy;
    Button note;
    Button save;
    Button operator;
    //private double lat;
    //private double lon;
    LatLng lastPositionAvaiable;
    private CompassMeasurement currentMeasure;


    //USEFUL DATA
    private int currentCompass;      // Magnetic compass measurement (degrees)
    private int currentDipdirection; //Geological compass dip direction (degrees)
    private int currentInlcination;  //Geological compass inclination (degrees)
    private boolean isUpright;       //True = upright, False = overturned
    private boolean isAccurate;      //True = accurate, False = not accurate
    private String currentRockunit;
    private String currentSurveyor;
    private String currentLocation;
    private String currentNotes;
    private String currentType;
    private CompassMeasurement.Younging selectedYounging;
    // fragment to which measurement data is sent
    private OnMeasurementSentListener onMeasurementSentListener;


    //TODO creare il costruttore
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Variables initialization
        //clickCounter = 1;
        //lat = 0.0;
        //lon = 0.0;
        currentCompass = 0;
        currentDipdirection = 0;
        //inclinometro = new Inclinometer();
        //choices = new CharSequence[]{"Bedding", "Cleavage", "Fault"};
        //currentType = (String) choices[0];
        isAccurate = true;
        msrLock = false;
        isUpright = false;
        cfState = true;
        currentLocation = null;
        currentNotes = null;
        currentRockunit = null;
        currentSurveyor = null;
        selectedYounging = CompassMeasurement.Younging.UPRIGHT;
        lastPositionAvaiable = new LatLng(43.700180, 12.640637);


        //Hiding useless buttons if class launched with compass on the bigger clock face
        if(!cfState)
            hideButtons();
    }

    @Nullable
    @Override

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        active = GeoPackageDatabases.getInstance(getActivity());
        manager = GeoPackageFactory.getManager(getActivity());
        myView = inflater.inflate(it.uniurb.beedip.R.layout.fragment_compass, container, false);

        return myView;
    }

    @Override
    public void onViewCreated(View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lIndicator = (ImageView) getView().findViewById(R.id.littleIndicator);
        bIndicator = (ImageView) getView().findViewById(R.id.bigIndicator);
        //back = (ImageView) getView().findViewById(R.id.quadrante);
        displayValues = (TextView) getView().findViewById(it.uniurb.beedip.R.id.testo);
        //SensorManager's initialization (It allow to declare sensor variables)
        sensorService = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        //Initialization of useful sensors
        sensor = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorService.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        //Buttons init
        bigClockFace = (ImageButton) getView().findViewById(R.id.bigClockface);
        littleClockFace = (ImageButton) getView().findViewById(R.id.littleClockface);
        rockUnit = (Button) getView().findViewById(R.id.rockUnit);
        locality = (Button) getView().findViewById(R.id.locality);
        //type = (Spinner) getView().findViewById(R.id.fragment_compass_layer_spinner);
        type = (Button) getView().findViewById(R.id.type);
        operator = (Button) getView().findViewById(R.id.surveyor);
        accuracy = (Button) getView().findViewById(R.id.accuracy);
        note = (Button) getView().findViewById(R.id.note);
        save = (Button) getView().findViewById(R.id.save);
        //Setting initial background color of buttons
        //accuracy.setBackgroundColor(Color.GREEN);
        accuracy.setBackgroundColor(Color.parseColor("#32ae16"));
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentMeasure != null){
                    getLocation();
                     /*----------------------------------------------DEBUG MESSAGE--------------------------------------------------*/
                    featureTableToast = Toast.makeText(getActivity(),
                            "Lat: " + lastPositionAvaiable.latitude+" Lon :" + lastPositionAvaiable.longitude , Toast.LENGTH_SHORT);
                    featureTableToast.show();
                    /*-------------------------------------------------------------------------------------------------------------*/
                    featureTableToast = Toast.makeText(getActivity(),
                            "saving in table " + editFeaturesTable + " of db " + editFeaturesDatabase, Toast.LENGTH_LONG);
                    featureTableToast.show();

                    if (currentSurveyor != null)
                        currentMeasure.setSurveyor(currentSurveyor);
                    if (currentLocation != null)
                        currentMeasure.setSite(currentLocation);
                    if (currentRockunit != null)
                        currentMeasure.setRockUnit(currentRockunit);
                    if (currentNotes != null)
                        currentMeasure.setNote(currentNotes);
                    saveMeasurement(new LatLng(lastPositionAvaiable.latitude, lastPositionAvaiable.longitude), currentMeasure);

                    resetParameters();
                }
                else{
                    featureTableToast = Toast.makeText(getActivity(),
                            "Not able to save if clock face is not locked.", Toast.LENGTH_LONG);
                    featureTableToast.show();
                }
            }
        });
        type.setEnabled(false);
        save.setEnabled(false);




        //Button relative to the little clock face
        littleClockFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Clicking the little clock face compass and inclinometer will be switched in the
                //main clock face
                //Switching compass and clinometer depending on what's active
                if (cfState) {
                    lIndicator.setImageResource(it.uniurb.beedip.R.drawable.mid);
                    bIndicator.setImageResource(R.drawable.arrow);
                    //Hiding buttons
                    hideButtons();
                    cfState = !cfState;
                } else {
                    lIndicator.setImageResource(it.uniurb.beedip.R.drawable.arrow);
                    bIndicator.setImageResource(R.drawable.mid);
                    //Reshowing buttons
                    showButtons();
                    cfState = !cfState;
                }
            }

        });
        //Creation of clock face's buttons
        //By simple click on button
        bigClockFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Lock the current result
               if (currentMeasure == null) {
                   currentMeasure = new CompassMeasurement(currentInlcination, currentDipdirection, selectedYounging, isAccurate);
               } else {
                   currentMeasure = null;
               }
                if (!isUpright) {
                    selectedYounging = CompassMeasurement.Younging.OVERTURNED;
                }


            }

        });
        //By long click on button
        bigClockFace.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Switch from isUpright indicator to normal and viceversa
                isUpright = !isUpright;
                return true;
            }
        });

        //Rock Unit button
        rockUnit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setTitle("Rock Unit");
                alertDialog.setMessage("Write a rock unit");
                if (currentRockunit != null)
                    alertDialog.setMessage(currentRockunit);

                final EditText input = new EditText(getActivity());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);

                alertDialog.setPositiveButton("Set",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                tmp = input.getText().toString();
                                if (!tmp.equals(""))
                                    currentRockunit = tmp;

                            }
                        });

                alertDialog.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                alertDialog.show();
            }

        });

        //Surveyor button
        operator.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setTitle("Surveyor");
                alertDialog.setMessage("Write the Surveyor's name");
                if (currentSurveyor != null)
                    alertDialog.setMessage(currentSurveyor);

                final EditText input = new EditText(getActivity());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);

                alertDialog.setPositiveButton("Set",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                tmp = input.getText().toString();
                                if (!tmp.equals(""))
                                    currentSurveyor = tmp;

                            }
                        });

                alertDialog.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                alertDialog.show();
            }

        });

        //Locality button
        locality.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setTitle("Location");
                alertDialog.setMessage("Write the Location.");
                if (currentLocation != null)
                    alertDialog.setMessage(currentLocation);

                final EditText input = new EditText(getActivity());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);
                //Do you want to set an icon?
                //alertDialog.setIcon(R.drawable.ICON_ID);
                alertDialog.setPositiveButton("Set",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                tmp = input.getText().toString();
                                if (!tmp.equals(""))
                                    currentLocation = tmp;
                            }
                        });

                alertDialog.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                alertDialog.show();
            }

        });

        //Type button
        type.setText(currentType);
        type.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                //Printing popup menu
                AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                dialog.setTitle("Pick a type");
                final ArrayAdapter<String> featuresAdapter = new ArrayAdapter<String>(
                        getActivity(), android.R.layout.simple_spinner_item, features);
                dialog.setAdapter(featuresAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selected) {
                        editFeaturesTable = featuresAdapter.getItem(selected);
                        currentType = (String) editFeaturesTable;
                        type.setText(currentType);
                        dialog.dismiss();
                    }
                }).create().show();
            }

        });

        //Accuracy button
        accuracy.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                //Changing button color
                if (!isAccurate) {
                    //accuracy.setBackgroundColor(Color.RED);
                    accuracy.setBackgroundColor(Color.parseColor("#e11919"));
                }
                else{
                    //accuracy.setBackgroundColor(Color.GREEN);
                    accuracy.setBackgroundColor(Color.parseColor("#32ae16"));
                }
                isAccurate = !isAccurate;
                if (currentMeasure != null) {
                    currentMeasure.setAccurate(isAccurate);
                }

            }

        });

        //Note button
        note.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Comment");
                if (currentNotes != null)
                    builder.setMessage(currentNotes);

                final EditText input = new EditText(getActivity());

                input.setText("Leave a comment.");
                input.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                input.setSingleLine(false);
                input.setLines(5);
                input.setMaxLines(5);
                input.setGravity(Gravity.LEFT | Gravity.TOP);
                builder.setView(input);

                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        tmp = input.getText().toString();
                        if (!tmp.equals(""))
                            currentNotes = tmp;
                    }
                });

                builder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }

        });

        getGPSPermission();
        turnOnGPS();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            onMeasurementSentListener = (OnMeasurementSentListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnMeasurementSentListener");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //TO END TO DEFINE
        int degree;
        if (currentMeasure == null) {
            //COMPASS
            //Compass animation
            //Acquiring values
            degree = Math.round(sensorEvent.values[0]);
            RotateAnimation ra_comp = new RotateAnimation(-currentCompass, -degree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            ra_comp.setDuration(300);
            ra_comp.setFillAfter(true);
            currentCompass = degree;

            if (cfState) {
                lIndicator.setAnimation(ra_comp);
                lIndicator.startAnimation(ra_comp);
            } else {
                bIndicator.setAnimation(ra_comp);
                bIndicator.startAnimation(ra_comp);
                //Compass text-printed values
                displayValues.setText(degree + "°");
            }

            //CLINOMETER
            //Clinometer animation
            //Acquiring values
            boolean upsideDown = false;
            //int usOffset;
            int x = Math.round(sensorEvent.values[0]);
            int y = Math.round(sensorEvent.values[1]);
            int z = Math.round(sensorEvent.values[2]);
            //Offsetting y axis
            if (y == 0) {
                //nothing
            } else if ((y > 90) && (y < 179)) {
                y = 90 - (y - 90);
            } else if ((y == 180) || (y == -180)) {
                y = 0;
            } else if ((y < -90) && (y > -179)) {
                y = -(90 + (y + 90));
            }
            //double y1 = sensorEvent.values[1];
            double y1 = (double) y;
            double z1 = sensorEvent.values[2];
            double posy,
                    posz,
                    ypar,
                    zpar,
                    res,
                    dipDouble;
            int dipAngle,
                    distance,
                    toDisplay;

            posy = Math.abs(y1);
            posz = Math.abs(z1);
            //Converting posy and posz from degrees to radians
            posy = posy * Math.PI / 180;
            posz = posz * Math.PI / 180;
            ypar = Math.sin(posy);
            zpar = Math.sin(posz);
            //Finding the resultant vector
            res = Math.sqrt(Math.pow(ypar, 2.0) + Math.pow(zpar, 2.0));
            //Angle in degrees
            dipDouble = (Math.asin(ypar / res) * 180 / Math.PI);
            dipAngle = (int) dipDouble;
            //Getting the offset to have the real angle
            if ((y > 0) && (z > 0)) {
                //I
                dipAngle = 90 - dipAngle;
            } else if ((y > 0) && (z < 0)) {
                //IV
                dipAngle = dipAngle + 270;
            } else if ((y < 0) && (z > 0)) {
                //II
                dipAngle = dipAngle + 90;
            } else if ((y < 0) && (z < 0)) {
                //III
                dipAngle = 90 - dipAngle + 180;
            }else if ((y == 0) && (z > 0)) {
                dipAngle = 90;
            } else if ((y == 0) && (z < 0)) {
                dipAngle = 270;
            } else if ((y > 0) && (z == 0)) {
                dipAngle = 0;
            } else if ((y < 0) && (z == 0)) {
                dipAngle = 180;
            } else if ((y == 0) && (z == 0)) {
                dipAngle = 0;
            }

            //Finding out if phone is upside down and telling related values
            if(Math.abs(Math.round(sensorEvent.values[1])) > 90){
                upsideDown = true;
                dipAngle = dipAngle + 180;
                if(dipAngle >= 360)
                    dipAngle = dipAngle - 360;

            }

            distance = dipAngle - x;
            //dipAngle = direction of the phone's inclination
            //Distance dipDirection
            if (distance <= 0)
                distance = 360 - x + dipAngle;
            distance = Math.abs(360 - distance);


            //Calculating inclination to display
            toDisplay = (int) Math.abs(Math.toDegrees(Math.asin(Math.sqrt(Math.pow(Math.sin(Math.toRadians(y1)), 2.0) +Math.pow(Math.sin(Math.toRadians(z1)), 2.0)))));
            if((toDisplay == 0) && ((Math.abs(y) > 3) || (Math.abs(z) > 3)))
                toDisplay = 90;

            //Offsetting dipAngle for animation
            dipAngle = dipAngle + 180;
            if(dipAngle >= 360)
                dipAngle = dipAngle - 360;
            if((dipAngle > 0) && (dipAngle <= 180)) {
                //dipAngle = dipAngle + 180;
                dipAngle = 180 - dipAngle;
                dipAngle = 180 + dipAngle;
            }
            else if((dipAngle > 180) && (dipAngle < 360)) {
                //dipAngle = dipAngle - 180;
                dipAngle = dipAngle - 180;
                dipAngle = 180 - dipAngle;
            }

            if(upsideDown) {
                dipAngle = dipAngle + 180;
                if(dipAngle >= 360)
                    dipAngle = dipAngle - 360;
            }


            RotateAnimation ra_clino = new RotateAnimation(prevDipangle, dipAngle, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            ra_clino.setDuration(300);
            ra_clino.setFillAfter(true);
            if(upsideDown){
                dipAngle = dipAngle - 180;
                if(dipAngle < 0)
                    dipAngle = 360 + dipAngle;
            }
            prevDipangle = dipAngle;
            currentDipdirection = distance;
            currentInlcination = toDisplay;
            //displayValues.setText(currentInlcination + "/" + currentDipdirection);

            if(cfState) {
                if((!isUpright) && (currentInlcination > 3) && (currentInlcination <= 30)){
                    bIndicator.setImageResource(R.drawable.sho);
                    bIndicator.getLayoutParams().width = 150;
                    bIndicator.getLayoutParams().height = 150;
                    bIndicator.requestLayout();
                }
                else if((!isUpright) && (currentInlcination > 30) && (currentInlcination <= 60)){
                    bIndicator.setImageResource(R.drawable.mid);
                    bIndicator.getLayoutParams().width = 150;
                    bIndicator.getLayoutParams().height = 150;
                    bIndicator.requestLayout();
                }
                else if((!isUpright) && (((currentInlcination > 60) && (currentInlcination <= 89)) ||
                        (Math.abs(y) > 43)  && (Math.abs(z) > 43))){
                    bIndicator.setImageResource(R.drawable.nlong);
                    bIndicator.getLayoutParams().width = 150;
                    bIndicator.getLayoutParams().height = 150;
                    bIndicator.requestLayout();
                }
                else if((isUpright) && (currentInlcination > 3) && (currentInlcination <= 30)){
                    bIndicator.setImageResource(R.drawable.rlong);
                    bIndicator.getLayoutParams().width = 150;
                    bIndicator.getLayoutParams().height = 150;
                    bIndicator.requestLayout();
                }
                else if((isUpright) && (currentInlcination > 30) && (currentInlcination <= 60)){
                    bIndicator.setImageResource(R.drawable.rmid);
                    bIndicator.getLayoutParams().width = 150;
                    bIndicator.getLayoutParams().height = 150;
                    bIndicator.requestLayout();
                }
                else if((isUpright) && (currentInlcination > 60) && (currentInlcination <= 89)){
                    bIndicator.setImageResource(R.drawable.rsho);
                    bIndicator.getLayoutParams().width = 150;
                    bIndicator.getLayoutParams().height = 150;
                    bIndicator.requestLayout();
                }
                else if((currentInlcination <= 3) && (currentInlcination >= 0)){
                    bIndicator.setImageResource(R.drawable.cross);
                    bIndicator.getLayoutParams().width = 150;
                    bIndicator.getLayoutParams().height = 150;
                    bIndicator.requestLayout();
                    currentDipdirection = 0;
                }
                else if(currentInlcination == 90){
                    bIndicator.setImageResource(R.drawable.point);
                    bIndicator.getLayoutParams().width = 150;
                    bIndicator.getLayoutParams().height = 150;
                    bIndicator.requestLayout();
                }
                bIndicator.setAnimation(ra_clino);
                bIndicator.startAnimation(ra_clino);
                //Clino text-printed values
                displayValues.setText(currentInlcination + "/" + currentDipdirection);
                //sendMeasurementData(compassMesurement);
            }
            else{
                if((!isUpright) && (currentInlcination > 3) && (currentInlcination <= 30)){
                    lIndicator.setImageResource(R.drawable.sho);
                    lIndicator.getLayoutParams().width = 35;
                    lIndicator.getLayoutParams().height = 35;
                    lIndicator.requestLayout();
                }
                else if((!isUpright) && (currentInlcination > 30) && (currentInlcination <= 60)){
                    lIndicator.setImageResource(R.drawable.mid);
                    lIndicator.getLayoutParams().width = 35;
                    lIndicator.getLayoutParams().height = 35;
                    lIndicator.requestLayout();
                }
                else if((!isUpright) && (currentInlcination > 60) && (currentInlcination <= 89)){
                    lIndicator.setImageResource(R.drawable.nlong);
                    lIndicator.getLayoutParams().width = 35;
                    lIndicator.getLayoutParams().height = 35;
                    lIndicator.requestLayout();
                }
                else if((isUpright) && (currentInlcination > 3) && (currentInlcination <= 30)){
                    lIndicator.setImageResource(R.drawable.rlong);
                    lIndicator.getLayoutParams().width = 35;
                    lIndicator.getLayoutParams().height = 35;
                    lIndicator.requestLayout();
                }
                else if((isUpright) && (currentInlcination > 30) && (currentInlcination <= 60)){
                    lIndicator.setImageResource(R.drawable.rmid);
                    lIndicator.getLayoutParams().width = 35;
                    lIndicator.getLayoutParams().height = 35;
                    lIndicator.requestLayout();
                }
                else if((isUpright) && (currentInlcination > 60) && (currentInlcination <= 89)){
                    lIndicator.setImageResource(R.drawable.rsho);
                    lIndicator.getLayoutParams().width = 35;
                    lIndicator.getLayoutParams().height = 35;
                    lIndicator.requestLayout();
                }
                else if((currentInlcination <= 3) && (currentInlcination >= 0)){
                    lIndicator.setImageResource(R.drawable.cross);
                    lIndicator.getLayoutParams().width = 35;
                    lIndicator.getLayoutParams().height = 35;
                    lIndicator.requestLayout();
                }
                else if(currentInlcination == 90){
                    lIndicator.setImageResource(R.drawable.point);
                    lIndicator.getLayoutParams().width = 35;
                    lIndicator.getLayoutParams().height = 35;
                    lIndicator.requestLayout();
                }
                lIndicator.setAnimation(ra_clino);
                lIndicator.startAnimation(ra_clino);
                //Clino text-printed values
                //displayValues.setText( y + "/" + dipAngle);
                //sendMeasurementData(compassMesurement);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //nothing
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void setEditFeaturesTable (String editFeaturesDatabase){
        this.editFeaturesDatabase = editFeaturesDatabase;
        GeoPackage geoPackage = manager.open(editFeaturesDatabase);
        if (!editFeaturesDatabase.isEmpty() && !geoPackage.getFeatureTables().isEmpty()){
            features = geoPackage.getFeatureTables();
            type.setEnabled(true);
            editFeaturesTable = features.get(0);
            type.setText(editFeaturesTable.toString());
            save.setEnabled(true);
        } else {

        }
    }

    /**
     * Save the Compass Measurement in the selected layer
     */

    private void saveMeasurement(LatLng position, CompassMeasurement measurement ) {
        if (editFeaturesDatabase != null && editFeaturesTable != null) {
            GeoPackage geoPackage = manager.open(editFeaturesDatabase);
            try {
                FeatureDao featureDao = geoPackage.getFeatureDao(editFeaturesTable);
                long srsId = featureDao.getGeometryColumns().getSrsId();
                GoogleMapShapeConverter converter = new GoogleMapShapeConverter(
                        featureDao.getProjection());
                final mil.nga.wkb.geom.Point point = converter.toPoint(position);
                        FeatureRow newPoint = featureDao.newRow();
                newPoint.setValue(getString(R.string.dip_field_name), measurement.getDip());
                newPoint.setValue(getString(R.string.dip_direction_field_name), measurement.getDipDirection());
                newPoint.setValue(getString(R.string.accuracy_field_name), measurement.isAccurate());
                newPoint.setValue(getString(R.string.orientation_field_name), measurement.getYounging());
                if (measurement.getRockUnit() != null) {
                    newPoint.setValue(getString(R.string.rock_unit_field_name), measurement.getRockUnit());
                }
                if (measurement.getSite() != null) {
                    newPoint.setValue(getString(R.string.site_field_name), measurement.getSite());
                }
                if (measurement.getSurveyor() != null) {
                    newPoint.setValue(getString(R.string.surveyor_field_name), measurement.getSurveyor());
                }
                if (measurement.getNote() != null) {
                    newPoint.setValue(getString(R.string.userNote_field_name), measurement.getNote());
                }

                GeoPackageGeometryData pointGeomData = new GeoPackageGeometryData(srsId);
                pointGeomData.setGeometry(point);
                newPoint.setGeometry(pointGeomData);
                featureDao.insert(newPoint);
                active.setModified(true);
                Contents contents = featureDao.getGeometryColumns().getContents();
                contents.setLastChange(new Date());
                ContentsDao contentsDao = geoPackage.getContentsDao();
                contentsDao.update(contents);
            } catch (Exception e) {
                if (GeoPackageUtils.isFutureSQLiteException(e)) {
                    GeoPackageUtils
                            .showMessage(
                                    getActivity(),
                                    getString(R.string.edit_features_save_label),
                                    "GeoPackage was created using a more recent SQLite version unsupported by Android");
                } else {
                    GeoPackageUtils.showMessage(getActivity(),
                            getString(R.string.edit_features_save_label) + " ", e.getMessage());
                }
            } finally {
                if (geoPackage != null) {
                    geoPackage.close();
                }
            }
        }
        // TODO utilizzare sendMeasurementData per forzare il draw della mappa
    }

    private void sendMeasurementData(CompassMeasurement measurement){
        if(onMeasurementSentListener != null)
            onMeasurementSentListener.onMeasurementSent(measurement);
    }

    private void hideButtons(){
        rockUnit.setVisibility(View.GONE);
        note.setVisibility(View.GONE);
        accuracy.setVisibility(View.GONE);
        type.setVisibility(View.GONE);
        save.setVisibility(View.GONE);
    }

    private void showButtons(){
        rockUnit.setVisibility(View.VISIBLE);
        note.setVisibility(View.VISIBLE);
        accuracy.setVisibility(View.VISIBLE);
        type.setVisibility(View.VISIBLE);
        save.setVisibility(View.VISIBLE);
    }

    private void resetParameters() {
        currentNotes = null;
        //currentType = (String) choices[0];
        //type.setText(currentType);
        currentRockunit = null;
        isAccurate = true;
        accuracy.setBackgroundColor(Color.parseColor("#32ae16"));
    }

    private void getLocation(){
        LocationManager mLocationManager;
        mLocationManager = (LocationManager) getActivity().getSystemService(LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

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
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new android.location.LocationListener(){
            @Override
            public void onLocationChanged(Location location) {
                lastPositionAvaiable = new LatLng(location.getLatitude(),location.getLongitude());
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
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000 * 60, 2, new android.location.LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                lastPositionAvaiable = new LatLng(location.getLatitude(),location.getLongitude());
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

    public void turnOnGPS(){
        //Request to turn on GPS if it's offline.
        int off = 0;
        try {
            off = Settings.Secure.getInt(getActivity().getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        if(off==0){
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setTitle("GPS");
            alertDialog.setMessage("Do you wish to turn on your GPS?");
            if (currentSurveyor != null)
                alertDialog.setMessage(currentSurveyor);

            //Useful to correct message part
            /*final EditText input = new EditText(getActivity());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);
            input.setLayoutParams(lp);
            alertDialog.setView(input);*/

            alertDialog.setPositiveButton("Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(onGPS);

                        }
                    });

            alertDialog.setNegativeButton("No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            alertDialog.show();
        }
        /*----------------------------------------------DEBUG MESSAGE--------------------------------------------------*/
        else {
            featureTableToast = Toast.makeText(getActivity(),
                    "GPS already turned on", Toast.LENGTH_SHORT);
            featureTableToast.show();
        }
        /*-------------------------------------------------------------------------------------------------------------*/
    }

    public void getGPSPermission(){
        if (ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //Requesting the permission to the user
            //android.Manifest.permission.ACCESS_FINE_LOCATION
            ActivityCompat.requestPermissions(getActivity(), new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},1);
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},1);
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
        }
        /*----------------------------------------------DEBUG MESSAGE--------------------------------------------------*/
        else{
            featureTableToast = Toast.makeText(getActivity(),
                    "Permission already acquired", Toast.LENGTH_SHORT);
            featureTableToast.show();
        }
        /*-------------------------------------------------------------------------------------------------------------*/
    }
}



