package it.uniurb.beedip;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;

import it.uniurb.beedip.data.CompassMeasurement;
import it.uniurb.beedip.data.GeoPackageDatabases;
import it.uniurb.beedip.data.OnMeasurementSentListener;
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
    private float currentDegree;
    private float currentClino;
    private int contaClick;
    //private Bussola bussola;
    private Inclinometer inclinometro;
    private boolean blocco;
    ImageView iv_arrow;
    ImageView back;
    TextView testo;
    ImageButton ibutton;
    Button saveButton;
    // fragment to which measurement data is sent
    private OnMeasurementSentListener onMeasurementSentListener;


    //TODO creare il costruttore
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Variables initialization
        contaClick = 1;
        currentDegree = 0f;
        currentClino = 0f;
        blocco = false;
        inclinometro = new Inclinometer();


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
        //Qua dentro ci andremo a trovare le view
        super.onViewCreated(view, savedInstanceState);

        //inclinometro = new Inclinometro();
        //bussola = new Bussola();
        //Inizializzazione degli id
        iv_arrow = (ImageView) getView().findViewById(it.uniurb.beedip.R.id.freccia);
        //back = (ImageView) getView().findViewById(R.id.quadrante);
        testo = (TextView) getView().findViewById(it.uniurb.beedip.R.id.testo);
        ibutton = (ImageButton) getView().findViewById(it.uniurb.beedip.R.id.quadrante);
        saveButton = (Button) getView().findViewById(R.id.compass_save_button);
        //Inizializzazione SensorManager (permette di inizializzare nuovi sensori)
        sensorService = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        //Inizializzazione dei sensori
        sensor = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorService.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);
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




        ibutton = (ImageButton) getView().findViewById(it.uniurb.beedip.R.id.quadrante);
        ibutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //blocca il risultato
                if(blocco == true)
                    blocco = false;
                else
                    blocco = true;
            }

        });

        ibutton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (contaClick == 0) {
                    iv_arrow.setImageResource(it.uniurb.beedip.R.drawable.ic_compass_hand);
                    //back.setImageResource(R.drawable.q_comp);
                    contaClick++;
                } else {
                    iv_arrow.setImageResource(it.uniurb.beedip.R.drawable.ic_inclinometer_hand);
                    //back.setImageResource(R.drawable.q_clin);
                    contaClick--;
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
        //da definire
        int degree;
        if((contaClick == 1) && (blocco == false)) {
            //Animazione della bussola
            //Acquisizione dei valori
            degree = Math.round(sensorEvent.values[0]);
            RotateAnimation ra = new RotateAnimation(currentDegree, -degree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            ra.setDuration(1000);
            ra.setFillAfter(true);
            iv_arrow.setAnimation(ra);
            iv_arrow.startAnimation(ra);

            //ruotare l'animazione

            currentDegree = -degree;

            //Valori testo bussola
            testo.setText(degree+"°");
        } else if((contaClick == 0) && (blocco == false)) {

            //Animazione della livella
            //Acquisizione dei valori
            //inclinometro.ModBeta(Math.round(sensorEvent.values[1]));
            //inclinometro.ModGamma(Math.round(sensorEvent.values[2]));
            float x = sensorEvent.values[1];
            int y = Math.round(sensorEvent.values[2]);
            RotateAnimation ra = new RotateAnimation(currentClino, y, Animation.RELATIVE_TO_SELF,
                    0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            ra.setDuration(1000);
            ra.setFillAfter(true);
            iv_arrow.startAnimation(ra);
            currentClino = y;



            //Valori testo livella
            testo.setText( x + "/" + y);


        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //nulla
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



