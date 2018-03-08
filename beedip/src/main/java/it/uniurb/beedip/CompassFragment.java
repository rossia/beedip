package it.uniurb.beedip;

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
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;
import java.util.Date;

import it.uniurb.beedip.data.CompassMeasurement;
import it.uniurb.beedip.data.GeoPackageDatabases;
import it.uniurb.beedip.data.OnMeasurementSentListener;
import android.graphics.Color;

import java.util.LinkedList;
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
    private int prevDipangle;
    private boolean msrLock;
    private boolean cfState;
    String tmp;
    ImageView lIndicator;
    ImageView bIndicator;
    ImageView pLock;
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
    Button coordinates;
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
    private String currentSubtype;
    /*These two are used only for fault as type*/
    private String currentKindicators;
    private int currentDisplacement;
    /*-----------------------------------------*/
    private CompassMeasurement.Younging selectedYounging;
    // Fragment to which measurement data is sent
    private OnMeasurementSentListener onMeasurementSentListener;
    private List<String> faultSubtypes;


    //TODO creare il costruttore
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Variables initialization
        currentCompass = 0;
        currentDipdirection = 0;
        currentType = null;
        isAccurate = true;
        msrLock = false;
        isUpright = true;
        cfState = true;
        currentLocation = null;
        currentNotes = null;
        currentRockunit = null;
        currentSurveyor = null;
        currentSubtype = null;
        currentDisplacement = -1; //Incoherent lenght value when not defined
        currentKindicators = null;
        selectedYounging = CompassMeasurement.Younging.UPRIGHT;
        lastPositionAvaiable = new LatLng(43.70018, 12.64063);
        features = new LinkedList<>();
        faultSubtypes = new LinkedList<String>(Arrays.asList("Normal","Reverse","Dextral ss","Sinixtral ss"));
        getLocation();


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
        pLock = (ImageView) getView().findViewById(R.id.padLock);
        displayValues = (TextView) getView().findViewById(it.uniurb.beedip.R.id.testo);
        //SensorManager's initialization (It allow to declare sensor variables)
        sensorService = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION); //---------------------------- change, is deprecated!
        sensorService.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        //Buttons init
        bigClockFace = (ImageButton) getView().findViewById(R.id.bigClockface);
        littleClockFace = (ImageButton) getView().findViewById(R.id.littleClockface);
        rockUnit = (Button) getView().findViewById(R.id.rockUnit);
        locality = (Button) getView().findViewById(R.id.locality);
        type = (Button) getView().findViewById(R.id.type);
        operator = (Button) getView().findViewById(R.id.surveyor);
        accuracy = (Button) getView().findViewById(R.id.accuracy);
        note = (Button) getView().findViewById(R.id.note);
        save = (Button) getView().findViewById(R.id.save);
        coordinates = (Button) getView().findViewById(R.id.coordinates);
        this.getLocation();
        String initialCoordinates = lastPositionAvaiable.latitude+" / "+lastPositionAvaiable.longitude;
        coordinates.setText(initialCoordinates);
        pLock.setVisibility(View.GONE);

        /*-------------------------------------------SAVE BUTTON---------------------------------------------------------------------------------------------*/
        save.setBackgroundResource(R.drawable.save_shape_red);
        save.setTextColor(Color.parseColor("#e11919"));
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
                    save.setBackgroundResource(R.drawable.save_shape_green);
                    save.setTextColor(Color.parseColor("#32ae16"));
                    resetParameters();

                    save.setEnabled(false);
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
        save.setText("");


        /*-------------------------------------------COORDINATES BUTTON---------------------------------------------------------------------------------------------*/
        coordinates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLocation();
            }
        });




        /*-------------------------------------------LITTLE CLOCK FACE BUTTON---------------------------------------------------------------------------------------------*/
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
        /*-------------------------------------------BIG CLOCK FACE BUTTON---------------------------------------------------------------------------------------------*/
        //By simple click on button
        bigClockFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Lock the current result
               if (currentMeasure == null) {
                   currentMeasure = new CompassMeasurement(currentInlcination, currentDipdirection, selectedYounging, isAccurate);
                   pLock.setVisibility(View.VISIBLE);
                   //Changes color to save button only if a project is selected
                   if(!features.isEmpty()) {
                       save.setBackgroundResource(R.drawable.save_shape_blue);
                       save.setTextColor(Color.parseColor("#00d2ff"));
                       //Switch to engage alertdialog to gain subtype on lock
                       for(int i = 0; i < features.size(); i++) {
                           if (features.get(i).equals(currentType)) {
                               switch (i) {
                                   //case 0 do not need subtype
                                   case 1:
                                       getTextSubtype();
                                       break;
                                   //case 2 do not need subtype
                                   case 3:
                                       getListSubtype();
                                       getDisplacement();
                                       getKindicators();
                                       break;
                                   case 4:
                                       getTextSubtype();
                                       break;
                               }
                           }
                       }
                   }
               } else {
                   currentMeasure = null;
                   pLock.setVisibility(View.GONE);
                   save.setBackgroundResource(R.drawable.save_shape_red);
                   save.setTextColor(Color.parseColor("#e11919"));
                   //If list is not empty, so a project is selected
                   //Enables save
                   if(!features.isEmpty())
                       save.setEnabled(true);
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

        /*-------------------------------------------ROCK UNIT BUTTON---------------------------------------------------------------------------------------------*/
        rockUnit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));

                View customStyle = LayoutInflater.from(getActivity()).inflate(R.layout.custom_dialog_r, null);


                //CHANGING ALERT DIALOG TITLE'S COLOR
                // Specify the alert dialog title
                String titleText = "Rock Unit";
                // Initialize a new foreground color span instance
                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.parseColor("#f2de00"));
                // Initialize a new spannable string builder instance
                SpannableStringBuilder ssBuilder = new SpannableStringBuilder(titleText);
                // Apply the text color span
                ssBuilder.setSpan(
                        foregroundColorSpan,
                        0,
                        titleText.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                // Set the alert dialog title using spannable string builder
                alertDialog.setTitle(ssBuilder);

                //CHANGING ALERT DIALOG MESSAGE'S COLOR
                // Specify the alert dialog message
                String messageText = "Write a rock unit.";
                // Initialize a new spannable string builder instance
                SpannableStringBuilder ssBuildermessage = new SpannableStringBuilder(messageText);
                // Apply the text color span
                ssBuildermessage.setSpan(
                        foregroundColorSpan,
                        0,
                        messageText.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                // Set the alert dialog message using spannable string builder
                alertDialog.setMessage(ssBuildermessage);

                final TextView input = (TextView) customStyle.findViewById(R.id.etext_r);
                if (currentRockunit != null) {
                    input.setText(currentRockunit);
                    input.setSelectAllOnFocus(true);
                }

                input.setHighlightColor(Color.parseColor("#4d4d4d"));

                alertDialog.setPositiveButton("Set",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                tmp = input.getText().toString();
                                if (!tmp.equals("")) {
                                    currentRockunit = tmp;
                                    rockUnit.setTextColor(Color.parseColor("#00d2ff"));
                                }
                                else {
                                    currentRockunit = null;
                                    rockUnit.setTextColor(Color.parseColor("#f2de00"));
                                }

                            }
                        });

                alertDialog.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                //alertDialog.show();



                AlertDialog dialog = alertDialog.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                dialog.setView(customStyle);
                dialog.show();
            }

        });

        /*-------------------------------------------SURVEYOR BUTTON---------------------------------------------------------------------------------------------*/
        operator.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));

                View customStyle = LayoutInflater.from(getActivity()).inflate(R.layout.custom_dialog_s, null);

                //CHAGING ALERT DIALOG TITLE'S COLOR
                // Specify the alert dialog title
                String titleText = "Surveyor";
                // Initialize a new foreground color span instance
                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.parseColor("#f2de00"));
                // Initialize a new spannable string builder instance
                SpannableStringBuilder ssBuilder = new SpannableStringBuilder(titleText);
                // Apply the text color span
                ssBuilder.setSpan(
                        foregroundColorSpan,
                        0,
                        titleText.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                // Set the alert dialog title using spannable string builder
                alertDialog.setTitle(ssBuilder);

                //CHANGING ALERT DIALOG MESSAGE'S COLOR
                // Specify the alert dialog message
                String messageText = "Write the surveyor's name.";
                // Initialize a new spannable string builder instance
                SpannableStringBuilder ssBuildermessage = new SpannableStringBuilder(messageText);
                // Apply the text color span
                ssBuildermessage.setSpan(
                        foregroundColorSpan,
                        0,
                        messageText.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                // Set the alert dialog message using spannable string builder
                alertDialog.setMessage(ssBuildermessage);

                final TextView input = (TextView) customStyle.findViewById(R.id.etext_s);
                if (currentSurveyor != null) {
                    input.setText(currentSurveyor);
                    input.setSelectAllOnFocus(true);
                }

                input.setHighlightColor(Color.parseColor("#4d4d4d"));

                alertDialog.setPositiveButton("Set",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                tmp = input.getText().toString();
                                if (!tmp.equals("")) {
                                    currentSurveyor = tmp;
                                    operator.setTextColor(Color.parseColor("#00d2ff"));
                                }
                                else {
                                    currentSurveyor = null;
                                    operator.setTextColor(Color.parseColor("#f2de00"));
                                }

                            }
                        });

                alertDialog.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                AlertDialog dialog = alertDialog.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                dialog.setView(customStyle);
                dialog.show();
            }

        });

        /*-------------------------------------------LOCALITY BUTTON---------------------------------------------------------------------------------------------*/
        locality.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));

                View customStyle = LayoutInflater.from(getActivity()).inflate(R.layout.custom_dialog_l, null);

                //CHANGING ALERT DIALOG TITLE'S COLOR
                // Specify the alert dialog title
                String titleText = "Location";
                // Initialize a new foreground color span instance
                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.parseColor("#f2de00"));
                // Initialize a new spannable string builder instance
                SpannableStringBuilder ssBuilder = new SpannableStringBuilder(titleText);
                // Apply the text color span
                ssBuilder.setSpan(
                        foregroundColorSpan,
                        0,
                        titleText.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                // Set the alert dialog title using spannable string builder
                alertDialog.setTitle(ssBuilder);

                //CHANGING ALERT DIALOG MESSAGE'S COLOR
                // Specify the alert dialog message
                String messageText = "Write the Location.";
                // Initialize a new spannable string builder instance
                SpannableStringBuilder ssBuildermessage = new SpannableStringBuilder(messageText);
                // Apply the text color span
                ssBuildermessage.setSpan(
                        foregroundColorSpan,
                        0,
                        messageText.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                // Set the alert dialog message using spannable string builder
                alertDialog.setMessage(ssBuildermessage);

                /*Do you want to set an icon?
                alertDialog.setIcon(R.drawable.ICON_ID);*/

                final TextView input = (TextView) customStyle.findViewById(R.id.etext_l);
                if (currentLocation != null) {
                    input.setText(currentLocation);
                    input.setSelectAllOnFocus(true);
                }

                input.setHighlightColor(Color.parseColor("#4d4d4d"));

                alertDialog.setPositiveButton("Set",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                tmp = input.getText().toString();
                                if (!tmp.equals("")) {
                                    currentLocation = tmp;
                                    locality.setTextColor(Color.parseColor("#00d2ff"));
                                }
                                else {
                                    currentLocation = null;
                                    locality.setTextColor(Color.parseColor("#f2de00"));
                                }
                            }
                        });

                alertDialog.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                AlertDialog dialog = alertDialog.create();
                dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                dialog.setView(customStyle);
                dialog.show();
            }

        });

        /*-------------------------------------------TYPE BUTTON---------------------------------------------------------------------------------------------*/
        type.setText(currentType);
        type.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                //Printing popup menu
                AlertDialog.Builder dialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));

                //CHANGING ALERT DIALOG TITLE'S COLOR
                // Specify the alert dialog title
                String titleText = "Pick a type";
                // Initialize a new foreground color span instance
                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.parseColor("#f2de00"));
                // Initialize a new spannable string builder instance
                SpannableStringBuilder ssBuilder = new SpannableStringBuilder(titleText);
                // Apply the text color span
                ssBuilder.setSpan(
                        foregroundColorSpan,
                        0,
                        titleText.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                // Set the alert dialog title using spannable string builder
                dialog.setTitle(ssBuilder);


                final ArrayAdapter<String> featuresAdapter = new ArrayAdapter<String>(
                        getActivity(), android.R.layout.simple_spinner_item, features);
                dialog.setAdapter(featuresAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selected) {
                        editFeaturesTable = featuresAdapter.getItem(selected);
                        currentType = editFeaturesTable;
                        type.setText(currentType);
                        //If current type is  bedding enable long press on big clock face
                        if(features.get(0).equals(currentType)){
                            setLongPress();
                        }
                        //else disable it
                        else{
                            bigClockFace.cancelLongPress();
                        }
                        dialog.dismiss();
                    }
                }).create().show();
            }

        });

        /*-------------------------------------------ACCURACY BUTTON---------------------------------------------------------------------------------------------*/
        accuracy.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                isAccurate = !isAccurate;
                //Changing button color
                if (!isAccurate) {
                    //If it's not accurate then the text will be red
                    accuracy.setTextColor(Color.parseColor("#e11919"));
                }
                else{
                    //if it's accurate then the text will be green
                    accuracy.setTextColor(Color.parseColor("#32ae16"));
                }
                if (currentMeasure != null) {
                    currentMeasure.setAccurate(isAccurate);
                }

            }

        });

        /*-------------------------------------------NOTE BUTTON---------------------------------------------------------------------------------------------*/
        note.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));
                //builder.setTitle("Notes");

                View customStyle = LayoutInflater.from(getActivity()).inflate(R.layout.custom_dialog_n, null);

                // Specify the alert dialog title
                String titleText = "Notes";

                // Initialize a new foreground color span instance
                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.parseColor("#f2de00"));

                // Initialize a new spannable string builder instance
                SpannableStringBuilder ssBuilder = new SpannableStringBuilder(titleText);

                // Apply the text color span
                ssBuilder.setSpan(
                        foregroundColorSpan,
                        0,
                        titleText.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                // Set the alert dialog title using spannable string builder
                builder.setTitle(ssBuilder);

                //final EditText input = new EditText(getActivity());
                final TextView input = (TextView) customStyle.findViewById(R.id.etext_n);
                input.setTextColor(Color.parseColor("#f2de00"));
                input.setText("Leave a note.");
                input.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                input.setSingleLine(false);
                input.setLines(5);
                input.setMaxLines(5);
                input.setGravity(Gravity.LEFT | Gravity.TOP);
                //builder.setView(input);
                if (currentNotes != null) {
                    input.setText(currentNotes);
                }
                input.setSelectAllOnFocus(true);
                input.setHighlightColor(Color.parseColor("#4d4d4d"));

                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        tmp = input.getText().toString();
                        if (!tmp.equals("")) {
                            currentNotes = tmp;
                            note.setTextColor(Color.parseColor("#00d2ff"));
                        }
                        else {
                            currentNotes = null;
                            note.setTextColor(Color.parseColor("#f2de00"));
                        }
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
                alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                alert.setView(customStyle);
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
        if (currentMeasure == null) {
            if(!features.isEmpty()) {
                //Do something while feature list is not empty
                for(int i = 0; i < features.size(); i++){
                    if(features.get(i).equals(currentType)){
                        //Choosing the right behavior
                        switch (i){
                            case 0:
                                this.compassCalculation(sensorEvent);
                                this.planeCalculation(sensorEvent);
                                break;
                            case 1:
                                this.compassCalculation(sensorEvent);
                                this.planeCalculation(sensorEvent);
                                break;
                            case 2:
                                this.compassCalculation(sensorEvent);
                                this.planeCalculation(sensorEvent);
                                break;
                            case 3:
                                this.compassCalculation(sensorEvent);
                                this.planeCalculation(sensorEvent);
                                break;
                            case 4:
                                this.compassCalculation(sensorEvent);
                                this.lineCalculation(sensorEvent);
                                break;
                        }
                    }
                }
            }
            else{
                //Do something when feature list is empty
                //Bedding as default? (Dunno!)
                this.compassCalculation(sensorEvent);
                this.planeCalculation(sensorEvent);
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
            save.setText("Save");
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
                Contents contents = featureDao.getGeometryColumns().getContents();
                contents.setLastChange(new Date());
                ContentsDao contentsDao = geoPackage.getContentsDao();
                contentsDao.update(contents);
                active.setModified(true);
            } catch (Exception e) {

                    GeoPackageUtils
                            .showMessage(
                                    getActivity(),
                                    getString(R.string.edit_features_save_label),
                                    "GeoPackage error saving");

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
        note.setTextColor(Color.parseColor("#f2de00"));
        currentRockunit = null;
        rockUnit.setTextColor(Color.parseColor("#f2de00"));
        isAccurate = true;
        accuracy.setTextColor(Color.parseColor("#32ae16"));
        currentSubtype = null;
        currentDisplacement = -1; //Incoherent lenght value when not defined
        currentKindicators = null;
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
        try {
            mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, new android.location.LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    lastPositionAvaiable = new LatLng(location.getLatitude(), location.getLongitude());
                    String position = location.getLatitude() + " / " + location.getLongitude();
                    coordinates.setText(position);
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
                    lastPositionAvaiable = new LatLng(location.getLatitude(), location.getLongitude());
                    String position = location.getLatitude() + " / " + location.getLongitude();
                    coordinates.setText(position);
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
            System.out.println(e);
        }
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
            alertDialog.setTitle("Position");
            alertDialog.setMessage("Do you wish to upload your position?");
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

    public void compassCalculation(SensorEvent sensorEvent){
        //COMPASS
        //Compass animation
        //Acquiring values
        String toShow;
        int degree;
        degree = Math.round(sensorEvent.values[0]);
        RotateAnimation ra_comp = new RotateAnimation(-currentCompass, -degree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ra_comp.setDuration(4000);
        ra_comp.setFillAfter(true);
        ra_comp.setRepeatCount(Animation.INFINITE);
        currentCompass = degree;
        if (cfState) {
            lIndicator.setAnimation(ra_comp);
            lIndicator.startAnimation(ra_comp);
        } else {
            bIndicator.setAnimation(ra_comp);
            bIndicator.startAnimation(ra_comp);
            //Compass text-printed values
            toShow = degree + "°";
            displayValues.setText(toShow);
        }
    }

    public void planeCalculation(SensorEvent sensorEvent){
        //INCLINOMETER
        //Acquiring values
        String toShow;
        boolean upsideDown = false;
        boolean avoidAnimation = false;
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
        if((Math.abs(Math.round(sensorEvent.values[1])) > 90)){
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
            dipAngle = 180 - dipAngle;
            dipAngle = 180 + dipAngle;
        }
        else if((dipAngle > 180) && (dipAngle < 360)) {
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
        ra_clino.setDuration(4000);
        ra_clino.setFillAfter(true);
        ra_clino.setRepeatCount(Animation.INFINITE);
        prevDipangle = dipAngle;
        currentDipdirection = distance;
        currentInlcination = toDisplay;

        if(cfState) {
            if((!isUpright) && (currentInlcination > 3) && (currentInlcination <= 30)){
                bIndicator.setImageResource(R.drawable.sho);
            }
            else if((!isUpright) && (currentInlcination > 30) && (currentInlcination <= 60)){
                bIndicator.setImageResource(R.drawable.mid);
            }
            else if((!isUpright) && (((currentInlcination > 60) && (currentInlcination <= 89)) ||
                    (Math.abs(y) > 43)  && (Math.abs(z) > 43))){
                bIndicator.setImageResource(R.drawable.nlong);
            }
            else if((isUpright) && (currentInlcination > 3) && (currentInlcination <= 30)){
                bIndicator.setImageResource(R.drawable.rlong);
            }
            else if((isUpright) && (currentInlcination > 30) && (currentInlcination <= 60)){
                bIndicator.setImageResource(R.drawable.rmid);
            }
            else if((isUpright) && (currentInlcination > 60) && (currentInlcination <= 89)){
                bIndicator.setImageResource(R.drawable.rsho);
            }
            else if((currentInlcination <= 3) && (currentInlcination >= 0)){
                bIndicator.setImageResource(R.drawable.cross);
                bIndicator.clearAnimation();
                currentDipdirection = 0;
                avoidAnimation = true;
            }
            else if(currentInlcination == 90){
                bIndicator.setImageResource(R.drawable.point);
            }
            bIndicator.getLayoutParams().width = 150;
            bIndicator.getLayoutParams().height = 150;
            bIndicator.requestLayout();

            if(!avoidAnimation) {
                bIndicator.setAnimation(ra_clino);
                bIndicator.startAnimation(ra_clino);
            }
            //Clino text-printed values
            toShow = currentInlcination+"/"+currentDipdirection;
            displayValues.setText(toShow);
            //sendMeasurementData(compassMesurement);
        }
        else{
            if((!isUpright) && (currentInlcination > 3) && (currentInlcination <= 30)){
                lIndicator.setImageResource(R.drawable.sho);
            }
            else if((!isUpright) && (currentInlcination > 30) && (currentInlcination <= 60)){
                lIndicator.setImageResource(R.drawable.mid);
            }
            else if((!isUpright) && (currentInlcination > 60) && (currentInlcination <= 89)){
                lIndicator.setImageResource(R.drawable.nlong);
            }
            else if((isUpright) && (currentInlcination > 3) && (currentInlcination <= 30)){
                lIndicator.setImageResource(R.drawable.rlong);
            }
            else if((isUpright) && (currentInlcination > 30) && (currentInlcination <= 60)){
                lIndicator.setImageResource(R.drawable.rmid);
            }
            else if((isUpright) && (currentInlcination > 60) && (currentInlcination <= 89)){
                lIndicator.setImageResource(R.drawable.rsho);
            }
            else if((currentInlcination <= 3) && (currentInlcination >= 0)){
                lIndicator.setImageResource(R.drawable.cross);
                lIndicator.clearAnimation();
                avoidAnimation = true;
            }
            else if(currentInlcination == 90){
                lIndicator.setImageResource(R.drawable.point);

            }
            lIndicator.getLayoutParams().width = 35;
            lIndicator.getLayoutParams().height = 35;
            lIndicator.requestLayout();

            if(!avoidAnimation) {
                lIndicator.setAnimation(ra_clino);
                lIndicator.startAnimation(ra_clino);
            }
        }
    }

    private void lineCalculation(SensorEvent sensorEvent) {
        //Calculate values and change images
    }

    private void setLongPress(){
        bigClockFace.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Switch from isUpright indicator to normal and viceversa
                isUpright = !isUpright;
                return true;
            }
        });
    }

    /*-----------------FUNCTION TO ACQUIRE DATA ON BIG CLOCK FACE LOCK-------------------------------------------------------------------------------------------------------*/

    private void getTextSubtype(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));
        View customStyle = LayoutInflater.from(getActivity()).inflate(R.layout.custom_dialog_s, null);
        //CHAGING ALERT DIALOG TITLE'S COLOR
        // Specify the alert dialog title
        String titleText = "Type";
        // Initialize a new foreground color span instance
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.parseColor("#f2de00"));
        // Initialize a new spannable string builder instance
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(titleText);
        // Apply the text color span
        ssBuilder.setSpan(
                foregroundColorSpan,
                0,
                titleText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        // Set the alert dialog title using spannable string builder
        alertDialog.setTitle(ssBuilder);

        //CHANGING ALERT DIALOG MESSAGE'S COLOR
        // Specify the alert dialog message
        String messageText = "Define the "+currentType+" type.";
        // Initialize a new spannable string builder instance
        SpannableStringBuilder ssBuildermessage = new SpannableStringBuilder(messageText);
        // Apply the text color span
        ssBuildermessage.setSpan(
                foregroundColorSpan,
                0,
                messageText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        // Set the alert dialog message using spannable string builder
        alertDialog.setMessage(ssBuildermessage);

        final TextView input = (TextView) customStyle.findViewById(R.id.etext_s);
        if (currentSubtype != null) {
            input.setText(currentSubtype);
            input.setSelectAllOnFocus(true);
        }

        input.setHighlightColor(Color.parseColor("#4d4d4d"));

        alertDialog.setPositiveButton("Set",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        tmp = input.getText().toString();
                        if (!tmp.equals("")) {
                            currentSubtype = tmp;
                        }
                        else {
                            currentSubtype = null;
                        }

                    }
                });

        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        AlertDialog dialog = alertDialog.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.setView(customStyle);
        dialog.show();
    }

    private void getListSubtype(){
        //Printing popup menu
        AlertDialog.Builder dialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));

        //CHANGING ALERT DIALOG TITLE'S COLOR
        // Specify the alert dialog title
        String titleText = "Choose a type of "+ currentType;
        // Initialize a new foreground color span instance
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.parseColor("#f2de00"));
        // Initialize a new spannable string builder instance
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(titleText);
        // Apply the text color span
        ssBuilder.setSpan(
                foregroundColorSpan,
                0,
                titleText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        // Set the alert dialog title using spannable string builder
        dialog.setTitle(ssBuilder);


        final ArrayAdapter<String> featuresAdapter = new ArrayAdapter<String>(
                getActivity(), android.R.layout.simple_spinner_item, faultSubtypes);
        dialog.setAdapter(featuresAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int selected) {
                //editFeaturesTable = featuresAdapter.getItem(selected);
                //currentType = editFeaturesTable;
                currentSubtype = faultSubtypes.get(selected);
                dialog.dismiss();
            }
        }).create().show();
    }

    private void getDisplacement(){



        //SAREBBE DA RIFARE L'XML Di "AlertDialogCustom" IN MODO TALE DA SETTARE DIRETTAMENTE DA LI' TASTIERA E TIPO DI INPUT



        AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));
        View customStyle = LayoutInflater.from(getActivity()).inflate(R.layout.custom_dialog_s, null);
        //CHAGING ALERT DIALOG TITLE'S COLOR
        // Specify the alert dialog title
        String titleText = "Displacement";
        // Initialize a new foreground color span instance
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.parseColor("#f2de00"));
        // Initialize a new spannable string builder instance
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(titleText);
        // Apply the text color span
        ssBuilder.setSpan(
                foregroundColorSpan,
                0,
                titleText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        // Set the alert dialog title using spannable string builder
        alertDialog.setTitle(ssBuilder);

        //CHANGING ALERT DIALOG MESSAGE'S COLOR
        // Specify the alert dialog message
        String messageText = "Define the displacement.";
        // Initialize a new spannable string builder instance
        SpannableStringBuilder ssBuildermessage = new SpannableStringBuilder(messageText);
        // Apply the text color span
        ssBuildermessage.setSpan(
                foregroundColorSpan,
                0,
                messageText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        // Set the alert dialog message using spannable string builder
        alertDialog.setMessage(ssBuildermessage);

        final TextView input = (TextView) customStyle.findViewById(R.id.etext_s);
        if (currentDisplacement >= 0) {
            input.setText(currentDisplacement);
            input.setSelectAllOnFocus(true);
            input.setInputType(InputType.TYPE_CLASS_PHONE); //IF KEYBOARD STAY "PHONE" LOOK FOR THE ERROR RIGHT HEEEEEERRREEEEEEE ;) ;) ;)
        }

        input.setHighlightColor(Color.parseColor("#4d4d4d"));

        alertDialog.setPositiveButton("Set",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        tmp = input.getText().toString();
                        if (!tmp.equals("")) {
                            currentSubtype = tmp;
                        }
                        else {
                            currentSubtype = null;
                        }

                    }
                });

        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        AlertDialog dialog = alertDialog.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.setView(customStyle);
        dialog.show();
    }

    private void getKindicators(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));
        View customStyle = LayoutInflater.from(getActivity()).inflate(R.layout.custom_dialog_s, null);
        //CHAGING ALERT DIALOG TITLE'S COLOR
        // Specify the alert dialog title
        String titleText = "Kinematics indicators";
        // Initialize a new foreground color span instance
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(Color.parseColor("#f2de00"));
        // Initialize a new spannable string builder instance
        SpannableStringBuilder ssBuilder = new SpannableStringBuilder(titleText);
        // Apply the text color span
        ssBuilder.setSpan(
                foregroundColorSpan,
                0,
                titleText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        // Set the alert dialog title using spannable string builder
        alertDialog.setTitle(ssBuilder);

        //CHANGING ALERT DIALOG MESSAGE'S COLOR
        // Specify the alert dialog message
        String messageText = "Define the kinematics indicators.";
        // Initialize a new spannable string builder instance
        SpannableStringBuilder ssBuildermessage = new SpannableStringBuilder(messageText);
        // Apply the text color span
        ssBuildermessage.setSpan(
                foregroundColorSpan,
                0,
                messageText.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        // Set the alert dialog message using spannable string builder
        alertDialog.setMessage(ssBuildermessage);

        final TextView input = (TextView) customStyle.findViewById(R.id.etext_s);
        if (currentKindicators != null) {
            input.setText(currentKindicators);
            input.setSelectAllOnFocus(true);
        }

        input.setHighlightColor(Color.parseColor("#4d4d4d"));

        alertDialog.setPositiveButton("Set",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        tmp = input.getText().toString();
                        if (!tmp.equals("")) {
                            currentKindicators = tmp;
                        }
                        else {
                            currentKindicators = null;
                        }

                    }
                });

        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        AlertDialog dialog = alertDialog.create();

        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.setView(customStyle);
        dialog.show();
    }
}



