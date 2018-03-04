package it.uniurb.beedip;


import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.google.android.gms.maps.model.LatLng;

import junit.framework.Assert;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.net.ssl.ManagerFactoryParameters;

import it.uniurb.beedip.data.CompassMeasurement;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.user.FeatureCursor;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;

import static android.R.id.button3;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class CompassFragmentSaveTest {
    private MainActivity mainActivity;
    private GeoPackageManager manager;
    private String projectName = "prova01";
    private String featureTable = "bedding";

    Class clsCompassFragment;
    Object objCompassFragment;
    Method saveMethod;
    LatLng position;
    private CompassMeasurement measurement;
    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void Before() {
        // screen on device must be on
        mainActivity = mActivityTestRule.getActivity();
        manager = GeoPackageFactory.getManager(mainActivity);

        // create a new test geopackage
        onView(allOf(withId(R.id.create_geopackage), withContentDescription("Create"), isDisplayed())).perform(click());
        onView(allOf(
                withParent(allOf(withId(R.id.custom),
                        withParent(withId(R.id.customPanel)))),
                isDisplayed()))
                .perform(replaceText(projectName), closeSoftKeyboard());
        onView(allOf(withId(android.R.id.button1), withText("OK"))).perform(scrollTo(), click());
        onView(allOf(
                withId(R.id.manager_group_name),
                withText(projectName),
                withParent(childAtPosition(withId(R.id.fragment_manager_view_ui),0)),
                isDisplayed()))
                .perform(click());


    }

    @After
    public void After() throws Exception {
        manager.delete(projectName);
    }

    @Test
    public void saveDataTest() throws InterruptedException, InvocationTargetException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchFieldException, NoSuchMethodException {
        clsCompassFragment = Class.forName("it.uniurb.beedip.CompassFragment");
        Object objCompassFragment = clsCompassFragment.newInstance();
        saveMethod = clsCompassFragment.getDeclaredMethod("saveMeasurement", LatLng.class, CompassMeasurement.class, String.class, String.class);
        saveMethod.setAccessible(true);
        int currentInclination = 30;
        int currentDipdirection  = 60;
        position = new LatLng(43.700180, 12.640637);

        measurement = new CompassMeasurement(
                currentInclination, currentDipdirection,
                CompassMeasurement.Younging.UPRIGHT,true);

        GeoPackage geoPackage = manager.open(projectName);
        Assert.assertNotNull(projectName);
        FeatureDao featureDao = geoPackage.getFeatureDao(featureTable);
        Assert.assertNotNull(projectName);
        Assert.assertNotNull(featureDao);

        saveMethod.invoke(objCompassFragment, position, measurement, projectName, featureTable);

        FeatureCursor cursor = featureDao.queryForAll();
        Assert.assertTrue("feature count not zero", cursor.getCount() > 0);
        int rows = 0;
        int found = 0;
        while (cursor.moveToNext()) {
            try {
                FeatureRow row = cursor.getRow();
                rows++;
                Log.e("Error","dip Direction value: " + row.getValue(R.string.dip_direction_field_name));
                if (Integer.valueOf(row.getValue(R.string.dip_direction_field_name).toString()) == currentDipdirection) {
                    found++;
                    Assert.assertEquals((int)Integer.valueOf(row.getValue(R.string.dip_field_name).toString()),currentInclination);
                }
            } catch (Exception e) {
                Assert.fail();
            }
        }
        Assert.assertTrue(rows  > 0);
        Assert.assertTrue(found > 0);
    }

    // matcher
    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
