package com.yangpeiwen.remotefish.connector;

import com.yangpeiwen.remotefish.Common;
import com.yangpeiwen.remotefish.Controller;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by ypw
 * on 2015-11-11 下午12:08.
 */
public class Ser2netConnector {

    public Ser2netConnector(String ser_ip, int ser_port) {
        ip = ser_ip;
        port = ser_port;
    }

    boolean running = true;
    String ip = "192.168.8.1";
    int port = 8081;
    InputStream inputStream;
    OutputStream outputStream;

    Socket client;

    public boolean connect() {
        boolean success = false;
        try {
            client = new Socket(ip, port);
            client.setSoTimeout(1000);
            inputStream = client.getInputStream();
            outputStream = client.getOutputStream();
            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    public byte[] read() {
        byte[] read2 = null;
        try {
            byte[] buffer = new byte[1024];
            int len = inputStream.read(buffer);
            read2 = new byte[len];
            int j;
            for (j = 0; j < len; j++)
                read2[j] = buffer[j];
        } catch (Exception e) {
            //e.printStackTrace();
        }
        if (read2 != null) {
            Common.printHex("read data:", read2);
        }
        return read2;
    }

    public boolean send(byte[] buf) {
        boolean f = false;
        try {
            outputStream.write(buf);
            outputStream.flush();
            f = true;
            Common.printHex("send data:", buf);
        } catch (Exception e) {
            //e.printStackTrace();
            Common.print("发送失败:" + Common.bytesToHexString(buf) + ", " + e.getMessage());
        }
        return f;
    }

    public boolean getVerifyCode() {
        byte data[] = new byte[2];
        data[0] = (byte) 0x00;
        data[1] = (byte) 0x01;

        byte encrypt[] = Encrypt.encrypt(data);
        byte escape[] = Encrypt.escape(encrypt);
        byte send_buf[] = Encrypt.package_tcp(escape);
        send(send_buf);

        byte read_buf[] = read();
        if (read_buf == null) return false;
        Common.printHex("tcp read", read_buf);

        byte unescape[] = Encrypt.unescape(read_buf);
        if ((unescape.length != 258) | (unescape[0] != (byte) 0xAA) | (unescape[unescape.length - 1] != (byte) 0xBB)) {
            return false;
        }
        byte unpackage[] = Encrypt.unpackage(unescape);
        byte decrypt[] = Encrypt.decrypt(unpackage);
        if (decrypt.length != 14) {
            return false;
        }
        byte v[] = new byte[14];
        System.arraycopy(decrypt, 0, v, 0, 14);
        verifycode = v;
        Common.printHex("verifycode", verifycode);

        return true;
    }

    static byte[] verifycode;

    public boolean sendCommandData(byte command, byte[] data) {
        if (verifycode == null) return false;
        byte raw[] = new byte[17 + data.length];
        raw[0] = (byte) data.length;
        raw[1] = command;
        System.arraycopy(verifycode, 0, raw, 2, 14);
        System.arraycopy(data, 0, raw, 16, data.length);
        raw[16 + data.length] = 0;
        Common.printHex("command and data", data);
        byte encrypt[] = Encrypt.encrypt(raw);
        byte escape[] = Encrypt.escape(encrypt);
        byte send_buf[] = Encrypt.package_tcp(escape);
        return send(send_buf);
    }

    public void stop() {
        running = false;
        try {
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}