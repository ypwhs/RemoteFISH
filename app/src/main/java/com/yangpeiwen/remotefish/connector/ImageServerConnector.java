package com.yangpeiwen.remotefish.connector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by ypw
 * on 2015-11-07 下午11:57.
 */

public class ImageServerConnector {

    public ImageServerConnector(String RPi_ip, int RPi_port) {
        ip = RPi_ip;
        port = RPi_port;
        startReadImage();
    }

    boolean running = true;
    String ip = "192.168.8.1";
    int port = 8080;
    InputStream inputStream;
    OutputStream outputStream;

    public void startReadImage() {
        Thread thread_read_image = new Thread(runnable_read_image);
        thread_read_image.setDaemon(true);
        thread_read_image.start();
    }

    public boolean connect() {
        boolean success = false;
        try {
            Socket client = new Socket(ip, port);
            client.setSoTimeout(100);
            inputStream = client.getInputStream();
            outputStream = client.getOutputStream();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    public void stop() {
        running = false;
    }

    private Runnable runnable_read_image = new Runnable() {
        @Override
        public void run() {
            int fail = 0;
            while (fail < 100 && running) {
                if (connect()) {
                    fail = 0;
                    Bitmap bitmap = readimage();
                    if (bitmap != null) {
                        onPictureReceivedListener.onPictureReceived(bitmap);
                    } else {
                        fail++;
                    }
                } else {
                    fail++;
                }
            }
        }
    };

    private Bitmap readimage() {
        Bitmap bitmap = null;
        try {
            int bytesRead, current = 0;
            byte[] buffer = new byte[1024 * 1024];
            do {
                bytesRead = inputStream.read(buffer, current, (buffer.length - current));
                if (bytesRead >= 0) current += bytesRead;
            } while (bytesRead > -1);

            bitmap = BitmapFactory.decodeByteArray(buffer, 0, current);
        } catch (Exception e) {
//            e.printStackTrace();
        }
        return bitmap;
    }

    private OnPictureReceivedListener onPictureReceivedListener;

    public interface OnPictureReceivedListener {
        void onPictureReceived(Bitmap picture);
    }

    public void setOnPictureReceivedListener(OnPictureReceivedListener listener) {
        this.onPictureReceivedListener = listener;
    }

}
