package com.yangpeiwen.remotefish.connector;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by ypw
 * on 2015-11-12 上午11:31.
 */
public class Connector {

    public Connector(String RPi_ip, int RPi_port) {
        ip = RPi_ip;
        port = RPi_port;
        //connect();
    }

    boolean running = true;
    String ip = "10.10.100.123";
    int port = 8080;
    InputStream inputStream;
    OutputStream outputStream;

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

    private Runnable runnable_write = new Runnable() {
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

    public void startTransfer() {
        Thread thread_write = new Thread(runnable_write);
        thread_write.setDaemon(true);
        thread_write.start();
    }

    private Queue<byte[]> queue = new LinkedList<>();

    public void write(byte[] d) {
        queue.offer(d);
    }


}
