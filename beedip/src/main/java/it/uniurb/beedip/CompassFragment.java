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
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
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
import mil.nga.geopackage.schema.columns.DataColumnsDao;


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
    private boolean msrLock;
    ImageView iv_arrow;
    ImageView back;
    TextView testo;
    ImageButton ibutton;
    Button rockUnit;
    Button locality;
    Button type;
    Button accuracy;
    Button note;
    Button save;
    CharSequence choices[];
    private String typeChosen;
    private boolean accuracyChosen;
    LinkedList<String> rockUnits = new LinkedList();
    String location;
    String userNotes;
    String tmp;
    boolean allowToSave;
    Button saveButton;
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
        msrLock = false;
        inclinometro = new Inclinometer();
        choices =  new CharSequence[] {"Bedding", "Cleavage", "Fault"};
        typeChosen = (String) choices[0];
        accuracyChosen = true;


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

        iv_arrow = (ImageView) getView().findViewById(it.uniurb.beedip.R.id.freccia);
        //back = (ImageView) getView().findViewById(R.id.quadrante);
        testo = (TextView) getView().findViewById(it.uniurb.beedip.R.id.testo);

        //SensorManager's initialization (It allow to declare sensor variables)

        ibutton = (ImageButton) getView().findViewById(it.uniurb.beedip.R.id.quadrante);
        saveButton = (Button) getView().findViewById(R.id.compass_save_button);
        //Inizializzazione SensorManager (permette di inizializzare nuovi sensori)
        sensorService = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        //Initialization of useful sensors
        sensor = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorService.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        //Buttons init
        ibutton = (ImageButton) getView().findViewById(it.uniurb.beedip.R.id.quadrante);
        rockUnit = (Button) getView().findViewById(R.id.rockUnit);
        locality = (Button) getView().findViewById(R.id.locality);
        type = (Button) getView().findViewById(R.id.type);
        accuracy = (Button) getView().findViewById(R.id.accuracy);
        note = (Button) getView().findViewById(R.id.note);
        save = (Button) getView().findViewById(R.id.save);
        //Setting initial background color of buttons
        accuracy.setBackgroundColor(Color.GREEN);
        save.setBackgroundColor(Color.BLUE);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                featureTableToast = Toast.makeText(getActivity(),
                        "saving in table " + editFeaturesTable + " of db " + editFeaturesDatabase, Toast.LENGTH_LONG);
                featureTableToast.show();
                saveMeasurement(new LatLng(new Double(43.6700),new Double(12.2300)),
                        new CompassMeasurement(90,0, CompassMeasurement.DipType.BEDDING,true));

            }
        });
        saveButton.setEnabled(false);



        //Creation of clock face's buttons
        //By simple click on button
        ibutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Lock the current result
                 msrLock = !msrLock;
            }

        });
        //By long click on button
        ibutton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Switch compass and clinometer depending on what's active
                if (clickCounter == 0) {
                    iv_arrow.setImageResource(it.uniurb.beedip.R.drawable.ic_compass_hand);
                    //back.setImageResource(R.drawable.q_comp);
                    clickCounter++;
                } else {
                    iv_arrow.setImageResource(it.uniurb.beedip.R.drawable.ic_inclinometer_hand);
                    //back.setImageResource(R.drawable.q_clin);
                    clickCounter--;
                }

                return true;
            }
        });

        //Rock Unit button
        rockUnit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                alertDialog.setTitle("Rock Unit");
                alertDialog.setMessage("Write a rock unit to add");

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
                                    rockUnits.add(tmp);

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
        type.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                //Printing popup menu
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Pick a type");
                builder.setItems(choices, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selected) {
                        typeChosen = (String) choices[selected];
                        dialog.cancel(); //not sure about this
                    }
                });
                builder.show();
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

        //Save button
        //On short click
        save.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if(allowToSave){
                    //Calculate the subtypes
                    //Then send
                }
            }

        });
        //On long click
        save.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(allowToSave){
                    //Calculate the subtypes
                    //Then send
                }
                return true;
            }
        });



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
        if((clickCounter == 1) && (!msrLock)) {
            //Compass animation
            //Acquiring values
            degree = Math.round(sensorEvent.values[0]);
            RotateAnimation ra = new RotateAnimation(currentDegree, -degree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            ra.setDuration(1000);
            ra.setFillAfter(true);
            iv_arrow.setAnimation(ra);
            iv_arrow.startAnimation(ra);

            //ruotare l'animazione

            currentDegree = -degree;

            //Compass text-printed values
            testo.setText(degree+"°");
        } else if((clickCounter == 0) && (!msrLock)) {

            //Clinometer animation
            //Acquiring values
            int x = Math.round(sensorEvent.values[1]);
            int y = Math.round(sensorEvent.values[2]);
            RotateAnimation ra = new RotateAnimation(currentClino, y, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            ra.setDuration(1000);
            ra.setFillAfter(true);
            iv_arrow.startAnimation(ra);
            currentClino = y;



            //Clino text-printed values
            testo.setText( x + "/" + y);


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

    /**
     * Save the Compass Measurement in the selected layer
     */

    public void setEditFeaturesTable (String editFeaturesDatabase, String editFeaturesTable){
        this.editFeaturesDatabase = editFeaturesDatabase;
        this.editFeaturesTable = editFeaturesTable;
        saveButton.setEnabled(true);
    }
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
}



