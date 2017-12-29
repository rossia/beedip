package it.uniurb.beedip;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
    private String editFeaturesDatabase = null;

    /**
     * Edit features table
     */
    private String editFeaturesTable = null;

    /**
     * features table toast
     */

    Toast featureTableToast;

    //Variables declaration
    View myView;
    private static SensorManager sensorService;
    private Sensor sensor;
    private CompassMeasurement compassMesurement;
    private int currentDegree;
    private int currentClino;
    private int clickCounter;
    //private Bussola bussola;
    private Inclinometer inclinometro;
    ImageView lIndicator;
    ImageView bIndicator;
    TextView testo;
    ImageButton bigButton;
    ImageButton littleButton;
    Button rockUnit;
    Button locality;
    Button type;
    Button accuracy;
    Button note;
    Button save;
    Button operator;
    CharSequence choices[];
    private String typeChosen;
    private boolean accuracyChosen;
    private boolean msrLock;
    private boolean reverted;
    private boolean cfState;
    String rockUnits;
    String surveyor;
    String location;
    String userNotes;
    String tmp;
    boolean allowToSave;
    // fragment to which measurement data is sent
    private OnMeasurementSentListener onMeasurementSentListener;


    //TODO creare il costruttore
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Variables initialization
        clickCounter = 1;
        currentDegree = 0;
        currentClino = 0;
        inclinometro = new Inclinometer();
        choices =  new CharSequence[] {"Bedding", "Cleavage", "Fault"};
        typeChosen = (String) choices[0];
        accuracyChosen = true;
        msrLock = false;
        reverted = false;
        cfState = true;

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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        lIndicator = (ImageView) getView().findViewById(R.id.littleIndicator);
        bIndicator = (ImageView) getView().findViewById(R.id.bigIndicator);
        //back = (ImageView) getView().findViewById(R.id.quadrante);
        testo = (TextView) getView().findViewById(it.uniurb.beedip.R.id.testo);
        //SensorManager's initialization (It allow to declare sensor variables)
        sensorService = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        //Initialization of useful sensors
        sensor = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorService.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        //Buttons init
        bigButton = (ImageButton) getView().findViewById(R.id.bigClockface);
        littleButton = (ImageButton) getView().findViewById(R.id.littleClockface);
        rockUnit = (Button) getView().findViewById(R.id.rockUnit);
        locality = (Button) getView().findViewById(R.id.locality);
        //type = (Spinner) getView().findViewById(R.id.fragment_compass_layer_spinner);
        type = (Button) getView().findViewById(R.id.type);
        operator = (Button) getView().findViewById(R.id.surveyor);
        accuracy = (Button) getView().findViewById(R.id.accuracy);
        note = (Button) getView().findViewById(R.id.note);
        save = (Button) getView().findViewById(R.id.save);
        //Setting initial background color of buttons
        accuracy.setBackgroundColor(Color.GREEN);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(msrLock){
                    featureTableToast = Toast.makeText(getActivity(),
                            "saving in table " + editFeaturesTable + " of db " + editFeaturesDatabase, Toast.LENGTH_LONG);
                    featureTableToast.show();
                    saveMeasurement(new LatLng(new Double(43.6700), new Double(12.2300)),
                            new CompassMeasurement(90, 0, CompassMeasurement.DipType.BEDDING, true));
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
        littleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Clicking the little clock face compass and inclinometer will be switched in the
                //main clock face
                //Switching compass and clinometer depending on what's active
                if(cfState) {
                    lIndicator.setImageResource(it.uniurb.beedip.R.drawable.mid);
                    bIndicator.setImageResource(R.drawable.arrow);
                    //Hiding buttons
                    hideButtons();
                    cfState = !cfState;
                }
                else {
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
        bigButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Lock the current result
                 msrLock = !msrLock;
            }

        });
        //By long click on button
        bigButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Switch from reverted indicator to normal and viceversa
                reverted = !reverted;
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

                final EditText input = new EditText(getActivity());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);

                alertDialog.setPositiveButton("Add",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                tmp = input.getText().toString();
                                if((tmp != null) && (!tmp.equals("")))
                                    rockUnits = tmp;

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
                alertDialog.setMessage("Write the surveyor's name");

                final EditText input = new EditText(getActivity());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);

                alertDialog.setPositiveButton("Add",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                tmp = input.getText().toString();
                                if((tmp != null) && (!tmp.equals("")))
                                    surveyor = tmp;

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
                alertDialog.setMessage("Write a location.");

                final EditText input = new EditText(getActivity());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);
                //Do you want to set an icon?
                //alertDialog.setIcon(R.drawable.ICON_ID);
                alertDialog.setPositiveButton("Add",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                tmp = input.getText().toString();
                                if((tmp != null) && (!tmp.equals("")))
                                    location = tmp;
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
        type.setText(typeChosen);
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
                        //editFeaturesTable = featuresAdapter.getItem(selected);
                        typeChosen = (String) choices[selected];
                        type.setText(typeChosen);
                        dialog.dismiss();
                    }
                }).create().show();
            }

        });

        //Accuracy button
        accuracy.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                //Changing button colour
                if(!accuracyChosen)
                    accuracy.setBackgroundColor(Color.RED);
                else
                    accuracy.setBackgroundColor(Color.GREEN);
                accuracyChosen = !accuracyChosen;

            }

        });

        //Note button
        note.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Comment");

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
                        if(tmp != null && !tmp.equals(""))
                            userNotes = tmp;
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


        //At this point we check the status of the application to hide/let see buttons

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
        if(!msrLock) {
            //COMPASS
            //Compass animation
            //Acquiring values
            degree = Math.round(sensorEvent.values[0]);
            RotateAnimation ra_comp = new RotateAnimation(currentDegree, -degree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            ra_comp.setDuration(1000);
            ra_comp.setFillAfter(true);
            currentDegree = -degree;

            if(cfState) {
                lIndicator.setAnimation(ra_comp);
                lIndicator.startAnimation(ra_comp);
            }
            else{
                bIndicator.setAnimation(ra_comp);
                bIndicator.startAnimation(ra_comp);
                //Compass text-printed values
                testo.setText(degree+"°");
            }

            //CLINOMETER
            //Clinometer animation
            //Acquiring values
            int y = Math.round(sensorEvent.values[1]);
            int z = Math.round(sensorEvent.values[2]);
            int dipAngle;
            //Calculating dipdirection in degrees
            if(z >= 0){
                // II and III
                //Setting the offset
                dipAngle = y + 90;
                int north = Math.round(sensorEvent.values[0]);
                dipAngle = dipAngle - north;

            }
            else{
                //IV and I
                //Setting the offset
                dipAngle = Math.abs(y - 90) + 180;
                int north = Math.round(sensorEvent.values[0]);
                dipAngle = dipAngle - north;
                if(dipAngle < 0)
                    dipAngle = 360 + dipAngle; //Sum cause dipAngle is negative
            }
            RotateAnimation ra_clino = new RotateAnimation(currentClino, -dipAngle, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            ra_clino.setDuration(1000);
            ra_clino.setFillAfter(true);
            if(dipAngle < 0)
                dipAngle = 360 + dipAngle; //Sum cause dipAngle is negative
            currentClino = -dipAngle;


            if(cfState) {
                if((!reverted) && (dipAngle > 3) && (dipAngle <= 30)){
                    bIndicator.setImageResource(R.drawable.sho);
                }
                else if((!reverted) && (dipAngle > 30) && (dipAngle <= 60)){
                    bIndicator.setImageResource(R.drawable.mid);
                }
                else if((!reverted) && (dipAngle > 60) && (dipAngle <= 89)){
                    bIndicator.setImageResource(R.drawable.nlong);
                }
                else if((reverted) && (dipAngle > 3) && (dipAngle <= 30)){
                    bIndicator.setImageResource(R.drawable.rlong);
                }
                else if((reverted) && (dipAngle > 30) && (dipAngle <= 60)){
                    bIndicator.setImageResource(R.drawable.rmid);
                }
                else if((reverted) && (dipAngle > 60) && (dipAngle <= 89)){
                    bIndicator.setImageResource(R.drawable.rsho);
                }
                else if(dipAngle <= 3){
                    bIndicator.setImageResource(R.drawable.cross);
                }
                else if(dipAngle == 90){
                    bIndicator.setImageResource(R.drawable.point);
                }
                bIndicator.setAnimation(ra_clino);
                bIndicator.startAnimation(ra_clino);
                //Clino text-printed values
                testo.setText( y + "/" + dipAngle);
                sendMeasurementData(compassMesurement);
            }
            else{
                if((!reverted) && (dipAngle > 3) && (dipAngle <= 30)){
                    lIndicator.setImageResource(R.drawable.sho);
                }
                else if((!reverted) && (dipAngle > 30) && (dipAngle <= 60)){
                    lIndicator.setImageResource(R.drawable.mid);
                }
                else if((!reverted) && (dipAngle > 60) && (dipAngle <= 89)){
                    lIndicator.setImageResource(R.drawable.nlong);
                }
                else if((reverted) && (dipAngle > 3) && (dipAngle <= 30)){
                    lIndicator.setImageResource(R.drawable.rlong);
                }
                else if((reverted) && (dipAngle > 30) && (dipAngle <= 60)){
                    lIndicator.setImageResource(R.drawable.rmid);
                }
                else if((reverted) && (dipAngle > 60) && (dipAngle <= 89)){
                    lIndicator.setImageResource(R.drawable.rsho);
                }
                else if(dipAngle <= 3){
                    lIndicator.setImageResource(R.drawable.cross);
                }
                else if(dipAngle == 90){
                    lIndicator.setImageResource(R.drawable.point);
                }
                lIndicator.setAnimation(ra_clino);
                lIndicator.startAnimation(ra_clino);
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
        // TODO: gestire il caso in cui il db isEmpty()
        features = geoPackage.getFeatureTables();
        type.setEnabled(true);
        save.setEnabled(true);
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

    private  void resetParameters(){
        userNotes = null;
        typeChosen = (String) choices[0];
        rockUnits = null;
        accuracyChosen = true;
    }
}



