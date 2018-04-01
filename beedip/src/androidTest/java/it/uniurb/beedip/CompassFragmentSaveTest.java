package it.uniurb.beedip;

import android.app.Fragment;
import android.content.Context;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter;
import com.google.android.gms.maps.model.LatLng;
import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Rule;
import org.junit.Before;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import it.uniurb.beedip.data.CompassMeasurement;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;

/**
 * Created by rossia on 3/16/2018.
 */
@RunWith(AndroidJUnit4.class)
public class CompassFragmentSaveTest {
    private MainActivity mainActivity;
    private GeoPackageManager manager;
    private final String projectTestName = "test001";
    private final String featureTable = "bedding";
    private Class clsManagerFragment;
    private Class clsCompassFragment;
    private Fragment fgmtManager;
    private Fragment fgmCompassFragment;
    private Method createNewProjectMethod;
    private Method saveMethod;
    @Rule
    public ActivityTestRule<MainActivity> activityActivityTestRule = new ActivityTestRule<MainActivity>(MainActivity.class);
    @Before
    public void init() throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException {

        activityActivityTestRule.getActivity().getFragmentManager().beginTransaction();
        mainActivity = activityActivityTestRule.getActivity();
        manager = GeoPackageFactory.getManager(mainActivity);
        clsManagerFragment = Class.forName("it.uniurb.beedip.GeoPackageManagerFragment");
        clsCompassFragment = Class.forName("it.uniurb.beedip.CompassFragment");
        saveMethod = clsCompassFragment.getDeclaredMethod("saveMeasurement", LatLng.class, CompassMeasurement.class, String.class, String.class);
        createNewProjectMethod = clsManagerFragment.getDeclaredMethod("createFromTemplate",Context.class, String.class);
        createNewProjectMethod.setAccessible(true);
        saveMethod.setAccessible(true);
        fgmtManager = activityActivityTestRule.getActivity().getFragmentManager().findFragmentById(R.id.fragment_manager);
        fgmCompassFragment = activityActivityTestRule.getActivity().getFragmentManager().findFragmentById(R.id.fragment_compass);

    }
    @Test
    public void testManager() throws InvocationTargetException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException  {
        assertNotNull(manager);
        assertNotNull(fgmtManager);
        mainActivity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    assertNotNull(createNewProjectMethod.invoke(fgmtManager, mainActivity, projectTestName));
                    Integer currentInclination = 30;
                    Integer currentDipdirection  = 60;
                    LatLng position = new LatLng(43.0, 12.0);
                    CompassMeasurement measurement = new CompassMeasurement(
                            currentInclination, currentDipdirection,
                            CompassMeasurement.Younging.UPRIGHT,true);

                    long insertedRowId = (long) saveMethod.invoke(fgmCompassFragment, position, measurement, projectTestName, featureTable);
                    assertNotEquals(insertedRowId, -1, 0);
                    GeoPackage geoPackage = manager.open(projectTestName);
                    FeatureDao featureDao = geoPackage.getFeatureDao(featureTable);
                    assertNotNull(featureDao);
                    GoogleMapShapeConverter converter = new GoogleMapShapeConverter(featureDao.getProjection());
                    final mil.nga.wkb.geom.Point insertedPoint = converter.toPoint(position);
                    GeoPackageGeometryData insertedPointGeomData = new GeoPackageGeometryData(featureDao.getGeometryColumns().getSrsId());
                    insertedPointGeomData.setGeometry(insertedPoint);
                    FeatureCursor cursor = featureDao.queryForAll();
                    assertTrue("feature count not zero", cursor.getCount() > 0);
                    cursor.moveToFirst();
                    boolean found = false;
                    for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                        FeatureRow row = cursor.getRow();
                        GeoPackageGeometryData geometryData = row.getGeometry();
                        assertFalse(geometryData.isEmpty());
                        if (geometryData.getSrsId() == insertedPointGeomData.getSrsId() && geometryData.getGeometry() == geometryData.getGeometry()) {
                            found = true;
                            assertEquals(Integer.valueOf(row.getValue("dipDirection").toString()), currentDipdirection);
                            assertEquals(Integer.valueOf(row.getValue("dip").toString()), currentInclination);
                        }
                    }
                    assertTrue(found);
                    cursor.close();
                    geoPackage.close();

                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    @After
    public void end() {

        manager.delete(projectTestName);
    }

}