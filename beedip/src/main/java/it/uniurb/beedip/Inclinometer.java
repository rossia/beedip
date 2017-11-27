package it.uniurb.beedip;

/**
 * Created by utente on 20/10/2017.
 */

public class Inclinometer {

    //Dichiarazione variabili iniziali
    int gradiBeta = 0;
    int gradiGamma = 0;

    //Permette di modificare Beta
    public void ModBeta(int gradi){
        if(gradi <= 360)
            this.gradiBeta = -gradi;
    }

    //Permette di modificare Gamma
    public void ModGamma(int gradi){
        if(gradi <= 360)
            this.gradiGamma = -gradi;
    }

    //Permette di vedere il valore di Beta
    public int seeBeta(){
        return this.gradiBeta;
    }

    //Permette di vedere il valore di Gamma
    public int seeGamma(){
        return this.gradiGamma;
    }
}
