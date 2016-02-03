package com.yangpeiwen.remotefish;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

/**
 * Created by ypw
 * on 2015-09-04 下午2:24.
 */

public class Common {

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv.toUpperCase() + " ");
        }
        return stringBuilder.toString();
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return wifiNetworkInfo.isConnected();
    }

    public static void print(String p) {
        System.out.println("OUT:" + p);
        // show(p);
        // SystemClock.sleep(200);
    }

    public static void printHex(String d, byte[] s){
        print(d + ",length:" + s.length + ",data:" + bytesToHexString(s));
    }

    public static byte[] bytesAdd(byte[] data1, byte[] data2) {
        byte[] data3 = new byte[data1.length + data2.length];
        System.arraycopy(data1, 0, data3, 0, data1.length);
        System.arraycopy(data2, 0, data3, data1.length, data2.length);
        return data3;
    }

    static Toast toast;
    public static void show(Context con, String str){
        if(toast==null){
            toast = Toast.makeText(con,
                    str, Toast.LENGTH_SHORT);
        }else{
            toast.setText(str);
        }
        toast.show();
    }

}
