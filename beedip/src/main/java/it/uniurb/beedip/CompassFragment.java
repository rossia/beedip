package it.uniurb.beedip;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.IntegerRes;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Log;
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
import com.google.android.gms.vision.CameraSource;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import it.uniurb.beedip.data.CompassMeasurement;
import it.uniurb.beedip.data.GeoPackageDatabases;
import android.graphics.Color;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.core.contents.Contents;
import mil.nga.geopackage.core.contents.ContentsDao;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.SENSOR_SERVICE;


/**
 * CompassFragment
 */

public class CompassFragment extends Fragment implements SensorEventListener {


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

    private static SensorManager sensorService;
    private Sensor sensor;
    private int prevDipangle;
    //private boolean msrLock;
    private boolean cfState;
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
    ImageButton photobutton;
    Button operator;
    Button coordinates;
    LatLng lastPositionAvaiable;
    private CompassMeasurement currentMeasure;


    //USEFUL DATA
    private int currentCompass;      // Magnetic compass measurement (degrees)
    private int currentDipdirection; //Geological compass dip direction (degrees)
    private int currentInlcination;  //Geological compass inclination (degrees)
    private int arrowDirection;
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
    private List<String> faultSubtypes;
    private float orientation[];

    //INITIALISING SENSORS
    Sensor accelerometer;
    Sensor magnetometer;

    //LinkedList used for the value stabilization algorithm
    private LinkedList<Integer> stabilityListCompass;
    private LinkedList<Integer> stabilityListInclination;
    private LinkedList<Integer> stabilityListDipDirection;

    //Array used for the determination of the rotation matrix
    private float[] mGravity;
    private float[] mGeomagnetic;

    //Token used for the plane animation
    private boolean isupsideDownTracker;

    //Costants
    //final int STABILITY_RANGE_CO = 100; //Stability range for the inclination
    final int STABILITY_RANGE_IN = 100; //Stability range for the inclination
    final int STABILITY_RANGE_DD = 250; //Stability range for the dip direction
    static final int REQUEST_IMAGE_CAPTURE = 1; //Photography parameter

    //To avoid warning
    final ViewGroup nullParent = null;

    //To photo
    String fileName;
    int photoDirection;
    int noAdjRoll;










    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Variables initialization
        currentCompass = 0;
        currentDipdirection = 0;
        currentType = null;
        isAccurate = true;
        //msrLock = false;
        isUpright = true;
        cfState = true;
        isupsideDownTracker = false;
        currentLocation = null;
        currentNotes = null;
        currentRockunit = null;
        currentSurveyor = null;
        currentSubtype = null;
        currentDisplacement = -1; //Incoherent lenght value when not defined
        currentKindicators = null;
        selectedYounging = CompassMeasurement.Younging.UPRIGHT;
        lastPositionAvaiable = new LatLng(43.70018, 12.64063); //default coordinates
        features = new LinkedList<>();
        faultSubtypes = new LinkedList<>();
        stabilityListCompass = new LinkedList<>();
        stabilityListInclination = new LinkedList<>();
        stabilityListDipDirection = new LinkedList<>();
        orientation = new float[3];
        mGravity = null;
        mGeomagnetic = null;
        fileName = null;

        for (CompassMeasurement.FaultType ft : CompassMeasurement.FaultType.values()) {
            faultSubtypes.add(ft.toString().toLowerCase());
        }


        //Hiding useless buttons if class launched with compass on the bigger clock face
        if(!cfState)
            hideButtons();

        super.onCreate(savedInstanceState);
        sensorService = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);

        assert sensorService != null;
        accelerometer = sensorService.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorService.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorService.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Nullable
    @Override

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        active = GeoPackageDatabases.getInstance(getActivity());
        manager = GeoPackageFactory.getManager(getActivity());
        return inflater.inflate(it.uniurb.beedip.R.layout.fragment_compass, container, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onViewCreated(View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            lIndicator = (ImageView) getView().findViewById(R.id.littleIndicator);
            bIndicator = (ImageView) getView().findViewById(R.id.bigIndicator);
            pLock = (ImageView) getView().findViewById(R.id.padLock);
            displayValues = (TextView) getView().findViewById(it.uniurb.beedip.R.id.testo);


        /*SensorManager's initialization (It allow to declare sensor variables)
        sensorService = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        sensor = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION); //---------------------------- change, is deprecated!
        sensorService.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);*/

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
            photobutton = (ImageButton) getView().findViewById(R.id.photobutton);
            photobutton.setBackgroundResource(R.drawable.camerapic);
        }catch (NullPointerException x){
            Log.e("Error", "Error:"+x);
        }
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
                if (currentMeasure != null) {
                    Toast.makeText(getActivity(),
                            "saving in table " + currentType + " of db " + editFeaturesDatabase,
                            Toast.LENGTH_LONG).show();
                    if (currentSurveyor != null)
                        currentMeasure.setSurveyor(currentSurveyor);
                    if (currentLocation != null)
                        currentMeasure.setSite(currentLocation);
                    if (currentRockunit != null)
                        currentMeasure.setRockUnit(currentRockunit);
                    if (currentNotes != null)
                        currentMeasure.setNote(currentNotes);
                    if (saveMeasurement(new LatLng(lastPositionAvaiable.latitude, lastPositionAvaiable.longitude),
                            currentMeasure, editFeaturesDatabase, currentType) < 0) {
                        Toast.makeText(getActivity(),"Not able to save measurement", Toast.LENGTH_LONG).show();
                    }
                    save.setBackgroundResource(R.drawable.save_shape_green);
                    save.setTextColor(Color.parseColor("#32ae16"));
                    resetParameters();
                    save.setEnabled(false);
                    //Eventually cancel the name of the photo binded to the saved measure.
                    fileName = null;
                    photobutton.setBackgroundResource(R.drawable.camerapic);
                }
                else {
                    Toast.makeText(getActivity(), "Not able to save if clock face is not locked.", Toast.LENGTH_LONG).show();
                }
            }
        });
        type.setEnabled(false);
        save.setEnabled(false);
        save.setText("");




        /*-------------------------------------------PHOTOBUTTON---------------------------------------------------------------------------------------------*/
        photobutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Here, we are making a folder named Beedip to store
                // pics taken by the camera using this application.
                final String PicSaveDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/Beedip/";
                boolean iscreated = false;
                File newdir = new File(PicSaveDir);
                if(newdir.mkdirs() || newdir.isDirectory()){

                    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Calendar.getInstance().getTime());
                    String file = PicSaveDir + timeStamp + ".jpg";
                    File newfile = new File(file);
                    try {
                        iscreated = newfile.createNewFile();
                    } catch (IOException e) {
                        Log.e("Error", "Error:"+ e);
                    }

                    if(iscreated){
                        Uri outputFileUri = Uri.fromFile(newfile);
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                        //startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);

                        //In this way we allow a single photo per save
                        if(fileName != null){
                            File toDelete = new File(fileName);
                            toDelete.delete();
                        }
                        fileName = file; //fileName is a global variable to pass
                        startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
                    }
                }
                else
                    Toast.makeText(getActivity(), "Error: directory cannot be created.", Toast.LENGTH_LONG).show();
            }
        });

        /*-------------------------------------------LITTLE CLOCK FACE BUTTON---------------------------------------------------------------------------------------------*/
        littleClockFace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Clicking the little clock face compass and inclinometer will be switched in the
                //main clock face
                String textToSwap;
                //Switching compass and clinometer depending on what's active
                lIndicator.clearAnimation();
                bIndicator.clearAnimation();
                if (cfState) {
                    if(currentType != null) {
                        if (!currentType.equals(CompassMeasurement.SurfaceType.LINEATION.toString())) {
                            lIndicator.setImageResource(it.uniurb.beedip.R.drawable.mid);
                            bIndicator.setImageResource(R.drawable.arrow);
                        } else {
                            lIndicator.setImageResource(it.uniurb.beedip.R.drawable.arrow);
                            bIndicator.setImageResource(R.drawable.arrow);
                        }
                    }
                    else{
                        lIndicator.setImageResource(it.uniurb.beedip.R.drawable.mid);
                        bIndicator.setImageResource(R.drawable.arrow);
                    }
                    textToSwap = currentCompass+"°";
                    displayValues.setText(textToSwap);
                    //Hiding buttons
                    hideButtons();
                } else {
                    if(currentType != null) {
                        if (!currentType.equals(CompassMeasurement.SurfaceType.LINEATION.toString())) {
                            lIndicator.setImageResource(it.uniurb.beedip.R.drawable.arrow);
                            bIndicator.setImageResource(R.drawable.mid);
                        } else {
                            lIndicator.setImageResource(it.uniurb.beedip.R.drawable.arrow);
                            bIndicator.setImageResource(R.drawable.arrow);
                        }
                    }
                    else{
                        lIndicator.setImageResource(it.uniurb.beedip.R.drawable.arrow);
                        bIndicator.setImageResource(R.drawable.mid);
                    }
                    textToSwap = currentInlcination+"/"+currentDipdirection;
                    displayValues.setText(textToSwap);
                    //Reshowing buttons
                    showButtons();
                }
                cfState = !cfState;
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
                   if(currentType != null) {
                       save.setBackgroundResource(R.drawable.save_shape_blue);
                       save.setTextColor(Color.parseColor("#00d2ff"));
                       //Switch to engage alertdialog to gain subtype on lock
                       if (currentType.equals(CompassMeasurement.SurfaceType.CLEAVAGE.toString())
                           || currentType.equals(CompassMeasurement.SurfaceType.LINEATION.toString()) ) {
                           getTextSubtype();
                       }
                       else if(currentType.equals(CompassMeasurement.SurfaceType.FAULT.toString()))
                           getListSubtype();
                   }
               } else {
                   currentMeasure = null;
                   pLock.setVisibility(View.GONE);
                   save.setBackgroundResource(R.drawable.save_shape_red);
                   save.setTextColor(Color.parseColor("#e11919"));
                   //If a table inside a project is selected
                   //Enables save
                   if(currentType != null)
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
                //Only if currentType = "bedding"
                if(currentType != null) {
                    if (currentType.equals(CompassMeasurement.SurfaceType.BEDDING.toString()))
                        isUpright = !isUpright;
                }
                else
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
                                if (!input.getText().toString().isEmpty()) {
                                    currentRockunit = input.getText().toString();
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
                try {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
                catch(NullPointerException e){
                    Log.e("Error", "Error"+e);
                }
                dialog.setView(customStyle);
                dialog.show();
            }

        });

        /*-------------------------------------------SURVEYOR BUTTON---------------------------------------------------------------------------------------------*/
        operator.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));
                View customStyle = LayoutInflater.from(getActivity()).inflate(R.layout.custom_dialog_s, nullParent);
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
                                if (!input.getText().toString().isEmpty()) {
                                    currentSurveyor = input.getText().toString();
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
                try {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
                catch(NullPointerException e){
                    Log.e("Error", "Error"+e);
                }
                dialog.setView(customStyle);
                dialog.show();
            }

        });

        /*-------------------------------------------LOCALITY BUTTON---------------------------------------------------------------------------------------------*/
        locality.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));

                View customStyle = LayoutInflater.from(getActivity()).inflate(R.layout.custom_dialog_l, nullParent);

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
                                if (!input.getText().toString().isEmpty()) {
                                    currentLocation = input.getText().toString();
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
                try {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
                catch(NullPointerException e) {
                    Log.e("Error", "Error"+e);
                }
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


                final ArrayAdapter<String> featuresAdapter = new ArrayAdapter<>(
                        getActivity(), android.R.layout.simple_list_item_1, features);
                dialog.setAdapter(featuresAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selected) {
                        currentType = features.get(selected);
                        type.setText(currentType);
                        dialog.dismiss();
                        initStability();
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

                View customStyle = LayoutInflater.from(getActivity()).inflate(R.layout.custom_dialog_n, nullParent);

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
                input.setText(R.string.Note);
                input.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                input.setSingleLine(false);
                input.setLines(5);
                input.setMaxLines(5);
                //input.setGravity(Gravity.LEFT | Gravity.TOP);
                input.setGravity(Gravity.START | Gravity.TOP);
                //builder.setView(input);
                if (currentNotes != null) {
                    input.setText(currentNotes);
                }
                input.setSelectAllOnFocus(true);
                input.setHighlightColor(Color.parseColor("#4d4d4d"));

                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if (!input.getText().toString().isEmpty()) {
                            currentNotes = input.getText().toString();
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
                try {
                    alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                }
                catch(NullPointerException e){
                    Log.e("Error", "Error:"+e);
                }
                alert.setView(customStyle);
                alert.show();
            }

        });

        getGPSPermission();
        turnOnGPS();
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            //Store photo direction?
            Toast.makeText(getActivity(), "Photo successfully saved.", Toast.LENGTH_LONG).show();
            photobutton.setBackgroundResource(R.drawable.camerapic2);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        boolean success;
        //float orientation[] = new float[3];
        if (currentMeasure == null) {
            //Da qui
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                this.mGravity = sensorEvent.values;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                this.mGeomagnetic = sensorEvent.values;
            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    //Toast.makeText(getActivity(),"SUCCESS", Toast.LENGTH_LONG).show();
                    SensorManager.getOrientation(R, orientation);
                    orientation[0] = (float) Math.toDegrees(orientation[0]);
                    orientation[1] = (float) Math.toDegrees(orientation[1]);
                    orientation[2] = (float) Math.toDegrees(orientation[2]);
                }
                //adjustements
                //Yaw
                orientation[0] += 360;
                if (orientation[0] >= 360)
                    orientation[0] -= 360;

                //Pitch
                //Fine as it is

                noAdjRoll = (int) orientation[2];

                //Roll
                if (orientation[2] >= 90)
                    orientation[2] = Math.abs(orientation[2] - 180);
                if (orientation[2] <= -90) {
                    orientation[2] = Math.abs(orientation[2] + 180);
                    orientation[2] = -orientation[2];
                }
            }
        }


        if(currentMeasure == null) {
            this.compassCalculation(orientation);
            if (currentType != null) {
                //Do something while feature list is not empty
                //Choosing the right behavior
                if (currentType.equals(CompassMeasurement.SurfaceType.LINEATION.toString()))
                    this.lineCalculation(orientation);
                else
                    this.planeCalculation(orientation);
            } else {
                //Do something when feature list is empty
                //Bedding as default
                this.planeCalculation(orientation);
            }
        }
        else if(currentType != null){
            this.compassAnimation(currentCompass);
            if(currentType.equals(CompassMeasurement.SurfaceType.LINEATION.toString()))
                this.lineAnimation(currentInlcination, currentDipdirection, arrowDirection);
            else
                this.planeAnimation(prevDipangle, currentDipdirection, currentInlcination, isupsideDownTracker, false);
            //this.lineAnimation(currentInlcination, currentDipdirection);
        }
        else {
            this.compassAnimation(currentCompass);
            this.planeAnimation(prevDipangle, currentDipdirection, currentInlcination, isupsideDownTracker, false);
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //nothing
    }

    @Override
    public void onPause() {
        super.onPause();
        //sensorService.unregisterListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        sensorService.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorService.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void setEditFeaturesTable (String editFeaturesDatabase){
        this.editFeaturesDatabase = editFeaturesDatabase;
        GeoPackage geoPackage = manager.open(editFeaturesDatabase);
        if (!editFeaturesDatabase.isEmpty() && !geoPackage.getFeatureTables().isEmpty()){
            features = geoPackage.getFeatureTables();
            type.setEnabled(true);
            currentType = features.get(0);
            type.setText(currentType);
            save.setEnabled(true);
            save.setText(R.string.Save);
        } else {
            Toast.makeText(getActivity(), "no feature table available!", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Save the Compass Measurement in the selected layer
     */

    private long saveMeasurement(LatLng position, CompassMeasurement measurement, String db, String feature )  {
        long resultRowId = -1;
        if (db != null && feature != null && manager != null) {
            GeoPackage geoPackage = manager.open(db);
            try {
                FeatureDao featureDao = geoPackage.getFeatureDao(feature);
                long srsId = featureDao.getGeometryColumns().getSrsId();
                GoogleMapShapeConverter converter = new GoogleMapShapeConverter(
                        featureDao.getProjection());
                final mil.nga.wkb.geom.Point point = converter.toPoint(position);
                        FeatureRow newPoint = featureDao.newRow();
                if (measurement.getDip() > 0)
                    newPoint.setValue(getString(R.string.dip_field_name), measurement.getDip());
                if (measurement.getDipDirection() > 0)
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
                resultRowId = featureDao.insert(newPoint);
                Contents contents = featureDao.getGeometryColumns().getContents();
                contents.setLastChange(new Date());
                ContentsDao contentsDao = geoPackage.getContentsDao();
                contentsDao.update(contents);
                active.setModified(true);

            } catch (Exception e) {
                    GeoPackageUtils.showMessage(getActivity(),getString(R.string.edit_features_save_label),"GeoPackage error saving");
            }
        }
        return resultRowId;
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
            Log.e("Error", "Error"+e);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
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

    public void compassCalculation(float[] orientation){
        //COMPASS
        //Compass animation
        //Acquiring values
        int degree;
        degree = Math.round(orientation[0]);

        //Stabilization algorithm
        this.populateStability(degree, 0);
        degree = this.getAverageStability(0);

        this.compassAnimation(degree);
    }

    /*Big clock face animation on compass mode*/
    private void compassAnimation(int degree){
        String toShow;
        RotateAnimation ra_comp = new RotateAnimation(-currentCompass, -degree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        RotateAnimation ra_comp_l = new RotateAnimation(-currentCompass, -degree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ra_comp.setDuration(4000);
        ra_comp.setFillAfter(true);
        ra_comp.setRepeatCount(Animation.INFINITE);
        ra_comp_l.setDuration(4000);
        ra_comp_l.setFillAfter(true);
        ra_comp_l.setRepeatCount(Animation.INFINITE);
        currentCompass = degree;
        if (cfState) {
            /*Older animation
            lIndicator.setAnimation(ra_comp);
            lIndicator.startAnimation(ra_comp);*/
            littleClockFace.setAnimation(ra_comp_l);
            bigClockFace.setAnimation(ra_comp);
            littleClockFace.startAnimation(ra_comp_l);
            bigClockFace.startAnimation(ra_comp);
        } else {
            /*Older animation
            bIndicator.setAnimation(ra_comp);
            bIndicator.startAnimation(ra_comp);*/
            littleClockFace.setAnimation(ra_comp_l);
            bigClockFace.setAnimation(ra_comp);
            littleClockFace.startAnimation(ra_comp_l);
            bigClockFace.startAnimation(ra_comp);
            //Compass text-printed values
            toShow = degree + "°";
            /*int deg1 = (int) orientation[1];
            int deg2 = (int) orientation[2];
            toShow = deg1 +" / " + noAdjRoll;*/
            displayValues.setText(toShow);
        }
    }

    /*Planes value as dip and dip direction are calculated from the "orientation" array*/
    public void planeCalculation(float[] orientation){

        boolean upsideDown = false;
        double ya = (double) orientation[0]; //yaw
        double pi = (double) orientation[1]; //pitch
        double ro = (double) orientation[2]; //roll

        /*int x = Math.round(orientation[0]); //yaw
        int y = Math.round(orientation[1]); //pitch
        int z = Math.round(orientation[2]); //roll*/
        /*Offsetting y axis
        if (y == 0) {
            //nothing
        } else if ((y > 90) && (y < 179)) {
            y = 90 - (y - 90);
        } else if ((y == 180) || (y == -180)) {
            y = 0;
        } else if ((y < -90) && (y > -179)) {
            y = -(90 + (y + 90));
        }*/

        //double y1 = (double) pi;
        //double z1 = orientation[2];

        double pi_abs,
                ro_abs,
                pi_sin,
                ro_sin,
                res;

        int dipAngle,
                dipDirection,
                inclination;

        pi_abs = Math.abs(pi);
        ro_abs = Math.abs(ro);
        //Converting absolute pitch and roll from degrees to radians
        pi_abs = Math.toRadians(pi_abs);
        ro_abs = Math.toRadians(ro_abs);
        //Calculating sines of pitch and roll
        pi_sin = Math.sin(pi_abs);
        ro_sin = Math.sin(ro_abs);
        //Finding the resultant vector
        res = Math.sqrt(Math.pow(pi_sin, 2.0) + Math.pow(ro_sin, 2.0));
        //Angle in degrees
        res = Math.toDegrees(Math.asin(pi_sin / res));
        dipAngle = (int) res;

        //Getting the offset to have the real angle
        if ((pi > 0) && (ro > 0)) {
            //I
            dipAngle = 90 - dipAngle;
        } else if ((pi > 0) && (ro < 0)) {
            //IV
            dipAngle = dipAngle + 270;
        } else if ((pi < 0) && (ro > 0)) {
            //II
            dipAngle = dipAngle + 90;
        } else if ((pi < 0) && (ro < 0)) {
            //III
            dipAngle = 90 - dipAngle + 180;
        }else if ((pi == 0) && (ro > 0)) {
            dipAngle = 90;
        } else if ((pi == 0) && (ro < 0)) {
            dipAngle = 270;
        } else if ((pi > 0) && (ro == 0)) {
            dipAngle = 0;
        } else if ((pi < 0) && (ro == 0)) {
            dipAngle = 180;
        } else if ((pi == 0) && (ro == 0)) {
            dipAngle = 0;
        }

        //Finding out if phone is upside down and telling related values
        if((Math.abs(Math.round(pi)) > 90)){
            upsideDown = true;
            dipAngle = dipAngle + 180;
            if(dipAngle >= 360)
                dipAngle = dipAngle - 360;
        }

        dipDirection = dipAngle - (int) ya;
        //dipAngle represents the direction of the phone's inclination
        if (dipDirection <= 0)
            dipDirection = 360 - (int) ya + dipAngle;
        dipDirection = Math.abs(360 - dipDirection);

        //Calculating inclination to display
        inclination = (int) Math.abs(Math.toDegrees(Math.asin(Math.sqrt(Math.pow(Math.sin(Math.toRadians(pi)), 2.0) +Math.pow(Math.sin(Math.toRadians(ro)), 2.0)))));
        if((inclination == 0) && ((Math.abs(pi) > 3) || (Math.abs(ro) > 3)))
            inclination = 90;

        //Stabilization algorithm
        this.populateStability(inclination, 1);
        this.populateStability(dipDirection, 2);
        inclination = this.getAverageStability(1);
        dipDirection = this.getAverageStability(2);

        //Calls for the animation method
        this.planeAnimation(dipAngle, dipDirection, inclination, upsideDown, true);
    }

    /*Big clock face animation on geological compass mode*/
    private void planeAnimation(int dipAngle, int dipdirection, int inclination, boolean upsideDown, boolean doOffset){
        String toShow;
        boolean avoidAnimation = false;

        /*Offset for the animation*/
        if(doOffset) {
            //if(!upsideDown) {
            //Offsetting dipAngle for animation
            dipAngle = dipAngle + 180;
            if (dipAngle >= 360)
                dipAngle = dipAngle - 360;
            if ((dipAngle > 0) && (dipAngle <= 180)) {
                dipAngle = 180 - dipAngle;
                dipAngle = 180 + dipAngle;
            } else if ((dipAngle > 180) && (dipAngle < 360)) {
                dipAngle = dipAngle - 180;
                dipAngle = 180 - dipAngle;
            }
            //}
            if (upsideDown) {
                dipAngle = dipAngle + 180;
                if (dipAngle >= 360)
                    dipAngle = dipAngle - 360;
            }
            //Mirroring the quadrant
            dipAngle = 180 - dipAngle;
            dipAngle += 180;
            if(dipAngle == 360)
                dipAngle = 0;
        }

        RotateAnimation ra_clino = new RotateAnimation(prevDipangle, dipAngle, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ra_clino.setDuration(4000);
        ra_clino.setFillAfter(true);
        ra_clino.setRepeatCount(Animation.INFINITE);
        this.prevDipangle = dipAngle;
        this.currentDipdirection = dipdirection;
        this.currentInlcination = inclination;
        this.isupsideDownTracker = upsideDown;

        if(cfState) {
            if((isUpright) && (currentInlcination > 3) && (currentInlcination <= 30)){
                bIndicator.setImageResource(R.drawable.nlong);
            }
            else if((isUpright) && (currentInlcination > 30) && (currentInlcination <= 60)){
                bIndicator.setImageResource(R.drawable.mid);
            }
            else if((isUpright) && (((currentInlcination > 60) && (currentInlcination <= 89)) /*||
                    (Math.abs(y) > 43)  && (Math.abs(z) > 43)*/)){
                bIndicator.setImageResource(R.drawable.sho);
            }
            else if((!isUpright) && (currentInlcination > 3) && (currentInlcination <= 30)){
                bIndicator.setImageResource(R.drawable.rlong);
            }
            else if((!isUpright) && (currentInlcination > 30) && (currentInlcination <= 60)){
                bIndicator.setImageResource(R.drawable.rmid);
            }
            else if((!isUpright) && (currentInlcination > 60) && (currentInlcination <= 89)){
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
            if((isUpright) && (currentInlcination > 3) && (currentInlcination <= 30)){
                lIndicator.setImageResource(R.drawable.nlong);
            }
            else if((isUpright) && (currentInlcination > 30) && (currentInlcination <= 60)){
                lIndicator.setImageResource(R.drawable.mid);
            }
            else if((isUpright) && (currentInlcination > 60) && (currentInlcination <= 89)){
                lIndicator.setImageResource(R.drawable.sho);
            }
            else if((!isUpright) && (currentInlcination > 3) && (currentInlcination <= 30)){
                lIndicator.setImageResource(R.drawable.rlong);
            }
            else if((!isUpright) && (currentInlcination > 30) && (currentInlcination <= 60)){
                lIndicator.setImageResource(R.drawable.rmid);
            }
            else if((! isUpright) && (currentInlcination > 60) && (currentInlcination <= 89)){
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

    private void lineCalculation(float[] orientation) {
        //Calculate values and change images
        int dipDirection = Math.round(orientation[0]);
        int inclination = Math.round(orientation[1]);

        /*if(inclination <= 90 && inclination >= 0){
            //Nothing
        }
        else*/ if(((inclination < 180) && (inclination > 90)) || (Math.abs(inclination) == 180)) {
            inclination = Math.abs(Math.abs(inclination) - 180);
            dipDirection += 180;
            if(dipDirection > 360){
                dipDirection -= 360;
            }
        }
        else if((inclination > -180) && (inclination <= -90)) {
            inclination = Math.abs(inclination + 180);
        }
        else if((inclination > -90) && (inclination < 0)) {
            inclination = Math.abs(inclination);
            dipDirection += 180;
            if(dipDirection > 360){
                dipDirection -= 360;
            }
        }
        //Stabilization algorithm
        this.populateStability(inclination, 1);
        this.populateStability(dipDirection, 2);
        inclination = this.getAverageStability(1);
        dipDirection = this.getAverageStability(2);

        if((orientation[1] > 0) && (inclination != 0))
            arrowDirection = 0;
        else if((orientation[1] < 0) && (inclination != 0))
            arrowDirection = 1;
        else if(inclination == 0)
            arrowDirection = 2;

        //Calls for the animation
        this.lineAnimation(inclination, dipDirection, arrowDirection);
    }

    private void lineAnimation(int plunge, int plungeDirection, int arrowDirection){
        String toShow;
        //Displaying the values
        currentInlcination = plunge;
        currentDipdirection = plungeDirection;

        //Animating the compass
        RotateAnimation ra_comp = new RotateAnimation(0,0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ra_comp.setDuration(4000);
        ra_comp.setFillAfter(true);
        ra_comp.setRepeatCount(Animation.INFINITE);
        if (cfState) {
            if(arrowDirection == 0)
                bIndicator.setImageResource(R.drawable.arrow);
            else if(arrowDirection == 1)
                bIndicator.setImageResource(R.drawable.udarrow);
            else if(arrowDirection == 2)
                bIndicator.setImageResource(R.drawable.darrow);
            bIndicator.getLayoutParams().width = 150;
            bIndicator.getLayoutParams().height = 150;
            bIndicator.requestLayout();
            bIndicator.setAnimation(ra_comp);
            bIndicator.startAnimation(ra_comp);
            //Displaying measured values
            toShow = currentInlcination + "/" + currentDipdirection;
            displayValues.setText(toShow);
        } else {
            if(arrowDirection == 0)
                lIndicator.setImageResource(R.drawable.arrow);
            else if(arrowDirection == 1)
                lIndicator.setImageResource(R.drawable.udarrow);
            else if(arrowDirection == 2)
                lIndicator.setImageResource(R.drawable.darrow);
            lIndicator.getLayoutParams().width = 35;
            lIndicator.getLayoutParams().height = 35;
            lIndicator.requestLayout();
            lIndicator.setAnimation(ra_comp);
            lIndicator.startAnimation(ra_comp);
            //Not displaying values
        }
    }

    /*-----------------FUNCTION TO ACQUIRE DATA ON BIG CLOCK FACE LOCK-------------------------------------------------------------------------------------------------------*/

    private void getTextSubtype(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));
        View customStyle = LayoutInflater.from(getActivity()).inflate(R.layout.custom_dialog_s, nullParent);
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
                        if (!input.getText().toString().isEmpty()) {
                            currentSubtype = input.getText().toString();
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
        try {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        catch (NullPointerException e){
            Log.e("Error", "Error:"+e);
        }
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
                getActivity(), android.R.layout.simple_list_item_1, faultSubtypes);
        dialog.setAdapter(featuresAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int selected) {
                //editFeaturesTable = featuresAdapter.getItem(selected);
                //currentType = editFeaturesTable;
                if(selected < (faultSubtypes.size() - 1)) //"Not defined" it has to be last element
                    currentSubtype = faultSubtypes.get(selected);
                else
                    currentSubtype = null;
                dialog.dismiss();
                getDisplacement();
                //getKindicators();
            }
        }).create().show();
    }

    private void getDisplacement(){

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));
        View customStyle = LayoutInflater.from(getActivity()).inflate(R.layout.custom_dialog_int, nullParent);
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

        final TextView input = (TextView) customStyle.findViewById(R.id.etext_int);
        if (currentDisplacement >= 0) {
            input.setText(String.valueOf(currentDisplacement));
            input.setSelectAllOnFocus(true);
        }

        input.setHighlightColor(Color.parseColor("#4d4d4d"));

        alertDialog.setPositiveButton("Set",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (!input.getText().toString().isEmpty()) {
                            currentDisplacement = Integer.parseInt(input.getText().toString());
                        }
                        else {
                            currentDisplacement = -1;
                        }
                        getKindicators();

                    }
                });

        alertDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        getKindicators();
                    }
                });

        AlertDialog dialog = alertDialog.create();
        try {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        catch (NullPointerException e){
            Log.e("Error", "Error:"+e);
        }
        dialog.setView(customStyle);
        dialog.show();
    }

    private void getKindicators(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(getActivity(), R.style.AlertDialogCustom));
        View customStyle = LayoutInflater.from(getActivity()).inflate(R.layout.custom_dialog_s, nullParent);
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
                        if (!input.getText().toString().isEmpty()) {
                            currentKindicators = input.getText().toString();
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
        try {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        catch (NullPointerException e){
            Log.e("Error", "Error:"+e);
        }
        dialog.setView(customStyle);
        dialog.show();
    }

    //This function it is used both for initialization and reset
    private void initStability(){
        //this.stabilityListCompass.clear(); //This one can be avoided to reset
        this.stabilityListCompass.clear();
        this.stabilityListInclination.clear();
        this.stabilityListDipDirection.clear();
    }

    private void populateStability(int currentValue, int pick){
        switch(pick) {
            case 0:
                /*Little trick to avoid wrong value displaying while indicator floats around 0*/
                if((!stabilityListCompass.isEmpty()) && (Math.abs(stabilityListCompass.getLast() - currentValue) > 330)){
                    this.initStability();
                }
                if (this.stabilityListCompass.size() < STABILITY_RANGE_IN)
                    this.stabilityListCompass.add(currentValue);
                else {
                    this.stabilityListCompass.removeFirst();
                    this.stabilityListCompass.add(currentValue);
                }
                break;
            case 1:
                if((!stabilityListInclination.isEmpty()) && (Math.abs(stabilityListInclination.getLast() - currentValue) > 330)){
                    this.initStability();
                }
                if (this.stabilityListInclination.size() < STABILITY_RANGE_IN)
                    this.stabilityListInclination.add(currentValue);
                else {
                    this.stabilityListInclination.removeFirst();
                    this.stabilityListInclination.add(currentValue);
                }
                break;
            case 2:
                if((!stabilityListDipDirection.isEmpty()) && (Math.abs(stabilityListDipDirection.getLast() - currentValue) > 330)){
                    this.initStability();
                }
                if (this.stabilityListDipDirection.size() < STABILITY_RANGE_DD)
                    this.stabilityListDipDirection.add(currentValue);
                else {
                    this.stabilityListDipDirection.removeFirst();
                    this.stabilityListDipDirection.add(currentValue);
                }
                break;
            default:
                System.out.println("Wrong pick");
                break;

        }
    }

    private int getAverageStability(int pick){
        int sum = 0,
            divider = 1;

        switch(pick) {
            case 0:
                for (int i = 0; i < stabilityListCompass.size(); i++) {
                    sum += stabilityListCompass.get(i);
                }
                divider = stabilityListCompass.size();
                break;
            case 1:
                for (int i = 0; i < stabilityListInclination.size(); i++) {
                    sum += stabilityListInclination.get(i);
                }
                divider = stabilityListInclination.size();
                break;
            case 2:
                for (int i = 0; i < stabilityListDipDirection.size(); i++) {
                    sum += stabilityListDipDirection.get(i);
                }
                divider = stabilityListDipDirection.size();
                break;
            default:
                System.out.println("Wrong pick");
                sum = -1;
                // -1 ... something gone wrong!
                break;

        }
        /*It used to divide by zero, this one seemed a good solution though*/
        if(divider == 0)
            divider = 1;
        return (sum/divider);
    }
}



