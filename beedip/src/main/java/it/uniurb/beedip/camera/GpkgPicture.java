package it.uniurb.beedip.camera;

import android.content.Context;

import com.google.android.gms.maps.model.LatLng;

import java.sql.SQLException;
import java.util.Date;

import it.uniurb.beedip.data.GeoPackageDatabases;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.core.contents.Contents;
import mil.nga.geopackage.core.contents.ContentsDao;
import mil.nga.geopackage.factory.GeoPackageFactory;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.map.geom.GoogleMapShapeConverter;
import mil.nga.wkb.geom.Point;

public class GpkgPicture extends PersistentPicture {
    private static final String FEATURE_PICTURE = "picture";
    private Context context;

    public GpkgPicture(byte[] data, Context context) {
        super(data);
        this.context = context;
    }

    @Override
    public void save(String databaseName) throws Exception {
        GeoPackage geoPackage = openGeoPackage(databaseName);
        FeatureDao featureDao = insertPictureFeature(geoPackage);
        updateLastChange(geoPackage, featureDao);
    }

    private GeoPackage openGeoPackage(String databaseName) {
        GeoPackageManager manager = GeoPackageFactory.getManager(this.context);
        return manager.open(databaseName);
    }

    private void updateLastChange(GeoPackage geoPackage, FeatureDao featureDao) throws SQLException {
        Contents contents = featureDao.getGeometryColumns().getContents();
        contents.setLastChange(new Date());
        ContentsDao contentsDao = geoPackage.getContentsDao();
        contentsDao.update(contents);
    }

    private FeatureDao insertPictureFeature(GeoPackage geoPackage) {
        FeatureDao featureDao = geoPackage.getFeatureDao(FEATURE_PICTURE);
        FeatureRow featureRow = featureDao.newRow();

        GeoPackageGeometryData geometryData = makeGeometry(featureDao);
        featureRow.setGeometry(geometryData);
        featureRow.setValue("azimuth", this.azimuth);
        featureRow.setValue("dip", this.dip);
        featureRow.setValue("data", this.data);

        featureDao.insert(featureRow);
        return featureDao;
    }

    private GeoPackageGeometryData makeGeometry(FeatureDao featureDao) {
        long srsId = featureDao.getGeometryColumns().getSrsId();
        GeoPackageGeometryData pointGeomData = new GeoPackageGeometryData(srsId);
        Point point = convertLocationToPoint(featureDao);
        pointGeomData.setGeometry(point);
        return pointGeomData;
    }

    private Point convertLocationToPoint(FeatureDao featureDao) {
        GoogleMapShapeConverter converter = new GoogleMapShapeConverter(featureDao.getProjection());
        LatLng latLng = new LatLng(this.location.getLatitude(), this.location.getLongitude());
        return converter.toPoint(latLng);
    }
}
