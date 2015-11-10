package com.yangpeiwen.remotefish;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.googlecode.javacv.cpp.opencv_core;

import java.io.InputStream;
import java.net.Socket;

/**
 * Created by ypw
 * on 2015-11-07 下午11:57.
 */

public class RraspberryPi {
    public String RPi_IP = "10.10.100.123";
    Socket client_RPi = null;
    InputStream inputStream_RPi;
    Bitmap bitmap_display;
    opencv_core.IplImage image_now;

    Runnable runnable_connect_RPi;

    {
        runnable_connect_RPi = new Runnable() {
            @Override
            public void run() {
                int fail = 0;
                while (fail < 10 && !Thread.currentThread().isInterrupted()) {
                    boolean success = false;
                    try {
                        client_RPi = new Socket(RPi_IP, 8080);
                        client_RPi.setSoTimeout(200);
//                        client_RPi = new Socket("10.50.255.205", 8080);
                        inputStream_RPi = client_RPi.getInputStream();
                        success = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (success) {
                        fail = 0;
                        try {
                            int bytesRead, current = 0;
                            byte[] buffer = new byte[1024 * 1024];
                            do {
                                bytesRead = inputStream_RPi.read(buffer, current, (buffer.length - current));
                                if (bytesRead >= 0) current += bytesRead;
                            } while (bytesRead > -1);

                            bitmap_display = BitmapFactory.decodeByteArray(buffer, 0, current);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        fail++;
                    }
                }
            }
        };
    }
}
