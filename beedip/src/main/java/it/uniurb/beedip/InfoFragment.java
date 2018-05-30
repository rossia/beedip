package it.uniurb.beedip;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import it.uniurb.beedip.data.GeoPackageDatabases;
import mil.nga.geopackage.factory.GeoPackageFactory;

/**
 * Created by Luca- on 27/05/2018.
 */

public class InfoFragment extends Fragment {

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_info, container, false);
    }
}
