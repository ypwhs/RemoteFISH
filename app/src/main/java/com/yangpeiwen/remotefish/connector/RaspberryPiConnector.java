package com.yangpeiwen.remotefish.connector;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Created by ypw
 * on 2015-11-07 下午11:57.
 */

public class RaspberryPiConnector extends Connector {

    public RaspberryPiConnector(String RPi_ip, int RPi_port) {
        super(RPi_ip, RPi_port);
        startReadImage();
    }

    public void startReadImage() {
        Thread thread_read_image = new Thread(runnable_read_image);
        thread_read_image.setDaemon(true);
        thread_read_image.start();
    }

    private Runnable runnable_read_image = new Runnable() {
        @Override
        public void run() {
            int fail = 0;
            while (fail < 10 && running) {
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
