package com.yangpeiwen.remotefish;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by ypw
 * on 2015-11-07 下午11:57.
 */

public class RaspberryPi{

    private boolean running = true;

    public RaspberryPi(String ip, int pt){
        RPi_IP = ip;
        port = pt;
    }

    class OnlyOneThread{
        Thread thread = null;
        public void start(Runnable runnable) throws Exception {
            if(thread!=null){
                throw new Exception("已经在运行了");
            }
        }
    }

    public void startReadImage(){
        Thread thread_read_image = new Thread(runnable_read_image);
        thread_read_image.setDaemon(true);
        thread_read_image.start();
    }

    public void startTransfer(){
        Thread thread_write = new Thread(runnable_write);
        thread_write.setDaemon(true);
        thread_write.start();
    }

    Runnable runnable_read_image = new Runnable() {
        @Override
        public void run() {
            int fail = 0;
            while (fail < 10 && running) {
                if (connect_RPi()) {
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

    Runnable runnable_write = new Runnable() {
        @Override
        public void run() {
            while (running) {
                if (!queue.isEmpty()) {
                    byte[] data = queue.poll();
                    try {
                        outputStream.write(data);
                        outputStream.flush();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    public void stop(){
        running = false;
    }

    public String RPi_IP = "10.10.100.123";
    int port = 8080;

    private InputStream inputStream;

    private boolean connect_RPi(){
        boolean success = false;
        try {
            Socket client = new Socket(RPi_IP, 8080);
            client.setSoTimeout(200);
            inputStream = client.getInputStream();
            outputStream = client.getOutputStream();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    private Bitmap readimage(){
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

    public interface OnPictureReceivedListener{
        void onPictureReceived(Bitmap picture);
    }

    public void setOnPictureReceivedListener(OnPictureReceivedListener listener){
        this.onPictureReceivedListener = listener;
    }

    private Queue<byte[]> queue = new LinkedList<>();
    
    public void write(byte[] d){
        queue.offer(d);
    }

    private OutputStream outputStream;

}
