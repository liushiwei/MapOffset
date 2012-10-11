
package com.map.projection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

public class Projection {

    private static int zoom = 18;// 地图层级

    private static final String TAG = "Projection";

    private static String fileName;

    private static SQLiteDatabase mDatabase;

    private static boolean isInit;

    public Projection() {
    }

    /**
     * 经度转化成像素值
     * 
     * @param lng
     * @param zoom
     * @return
     */
    public static double lngToPixel(double lng, int zoom) {

        return (lng + 180) * (256L << zoom) / 360;

    }

    /**
     * 像素值转化为经度值
     * 
     * @param pixelX
     * @param zoom
     * @return
     */

    public static double pixelToLng(double pixelX, int zoom) {

        return pixelX * 360 / (256L << zoom) - 180;

    }

    /**
     * 纬度转化为像素值
     * 
     * @param lat
     * @param zoom
     * @return
     */

    public static double latToPixel(double lat, int zoom) {

        double siny = Math.sin(lat * Math.PI / 180);

        double y = Math.log((1 + siny) / (1 - siny));

        return (128 << zoom) * (1 - y / (2 * Math.PI));

    }

    /**
     * 像素值转化为纬度
     * 
     * @param pixelY
     * @param zoom
     * @return
     */
    public static double pixelToLat(double pixelY, int zoom) {

        double y = 2 * Math.PI * (1 - pixelY / (128 << zoom));

        double z = Math.pow(Math.E, y);

        double siny = (z - 1) / (z + 1);

        return Math.asin(siny) * 180 / Math.PI;

    }

    /**
     * 调整后的经纬度
     * 
     * @param lat
     * @param lng
     * @return
     */
    public static double[] adjustLatLng(double lat, double lng) {
        double[] adjustedLatLng = new double[2];
        if (isInit) {
            double[] offset = request(lng, lat);
            // double[] offset = {907,615};
            Log.e("Projection", "offx = " + offset[0] + ", offy=" + offset[1]);
            double latPixel = Math.round(latToPixel(lat, zoom));
            double lngPixel = Math.round(lngToPixel(lng, zoom));
            latPixel += offset[1];
            lngPixel += offset[0];
            adjustedLatLng[0] = pixelToLat(latPixel, zoom);
            adjustedLatLng[1] = pixelToLng(lngPixel, zoom);

        }
        return adjustedLatLng;
    }

    private static double[] request(double lat, double lng) {
        double[] offsetPix = new double[2];
        String[] columns = {
                "offx", "offy"
        };
        mDatabase = SQLiteDatabase.openDatabase(fileName, null, SQLiteDatabase.OPEN_READONLY);
        Cursor cursor = mDatabase.query("Projection", columns, "lat = " + (int) (lat * 10)
                + " and lng=" + (int) (lng * 10), null, null, null, null);
        Log.e("Projection", "lat = " + (int) (lat * 10) + " and lng=" + (int) (lng * 10));
        if (cursor.moveToFirst()) {
            do {
                Log.e("Projection", "offx = " + cursor.getInt(0) + ", offy=" + cursor.getInt(1));
                offsetPix[0] = cursor.getInt(0);
                offsetPix[1] = cursor.getInt(1);
            } while (cursor.moveToNext());
        } 
        cursor.close();
        mDatabase.close();
        return offsetPix;
    }

    public static void init() {
        fileName = Environment.getExternalStorageDirectory().getPath() + "/Mfile.dat";
        Log.d(TAG, "File name: " + fileName);
        File file = new File(fileName);
        //file.delete();
        if (!file.exists()) {
            InputStream input = Projection.class.getResourceAsStream("db/projection");
            byte[] buffer = new byte[300];
            try {
                FileOutputStream fileStream = new FileOutputStream(new File(fileName));
                int j = 0;
                //int t = j;
               
                while ((j = input.read(buffer)) > 0) {
                    fileStream.write(buffer, 0, j);
                    //t += j;
                }
                fileStream.close();
                input.close();
                //Log.d(TAG, "Created file name: " + fileName + " file size = " + t);
            } catch (FileNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
                file.delete();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                file.delete();
            }
        }

        //mDatabase = SQLiteDatabase.openOrCreateDatabase(fileName, null);
        isInit = true;
    }

    public static boolean isOpen() {
        if (mDatabase != null)
            return mDatabase.isOpen();
        else
            return false;
    }

    public static void destroy() {

        mDatabase.close();
        mDatabase = null;
    }

    public static boolean isInit() {
        return isInit;
    }
    
    public static double[] getOffset(double lat, double lng){
        return request(lat,lng);
    }

}
