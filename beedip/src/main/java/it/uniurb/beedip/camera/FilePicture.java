package it.uniurb.beedip.camera;

import android.os.Environment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FilePicture extends PersistentPicture {

    public FilePicture(byte[] data) {
        super(data);
    }

    public void save(String baseName) throws IOException {
        String absolutePath = saveFile(baseName);
        updateExif(absolutePath);
    }

    private String saveFile(String baseName) throws IOException {
        File pictureFile = getOutputMediaFile(baseName);
        FileOutputStream fos = new FileOutputStream(pictureFile);
        fos.write(data);
        fos.close();
        return pictureFile.getAbsolutePath();
    }

    private void updateExif(String absolutePath) throws IOException {
        Exif exif = new Exif(absolutePath);
        exif.setAzimuth(azimuth);
        exif.setDip(dip);
        exif.setLocation(location);
        exif.saveAttributes();
    }

    private static File getOutputMediaFile(String fileName) throws FileNotFoundException {
        String pictureDir = makePictureDir();
        String timeStamp = makeTimeStamp();
        return makeJpgFile(pictureDir, fileName, timeStamp);
    }

    private static String makePictureDir() throws FileNotFoundException {
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "BeeDip");

        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs())
            throw new FileNotFoundException("failed to create directory");

        return mediaStorageDir.getPath();
    }

    private static String makeTimeStamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
    }

    private static File makeJpgFile(String dirPath, String fileName, String timeStamp) {
        return new File(dirPath + File.separator + fileName + "_"+ timeStamp + ".jpg");
    }
}
