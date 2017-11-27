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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import it.uniurb.beedip.data.CompassMeasurement;
import it.uniurb.beedip.data.OnMeasurementSentListener;

/**
 * Created by utente on 22/10/2017.
 */

public class CompassFragment extends Fragment implements SensorEventListener {

    //Dichiarazione variabili
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
    // fragment to which measurement data is sent
    private OnMeasurementSentListener onMeasurementSentListener;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Parti da inizializzare quando il frammento viene creato
        contaClick = 1;
        currentDegree = 0f;
        currentClino = 0f;
        blocco = false;
        inclinometro = new Inclinometer();


    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
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
        //Inizializzazione SensorManager (permette di inizializzare nuovi sensori)
        sensorService = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        //Inizializzazione dei sensori
        sensor = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorService.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL);





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
            saveMesurement();
            sendMeasurementData(compassMesurement);
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
    public void saveMesurement() {
        try {
            compassMesurement = new CompassMeasurement(currentDegree, currentClino, true);
        } catch (Exception e) {
            // @todo gestire eccezione a livello di gui
        }
    }
    private void sendMeasurementData(CompassMeasurement measurement){
        if(onMeasurementSentListener != null)
            onMeasurementSentListener.onMeasurementSent(measurement);
    }
}



