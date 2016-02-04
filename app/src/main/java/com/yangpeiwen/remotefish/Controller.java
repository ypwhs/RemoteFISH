package com.yangpeiwen.remotefish;

/**
 * Created by ypw
 * on 2015-09-04 下午2:24.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.javacv.cpp.avcodec;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.yangpeiwen.remotefish.connector.Encrypt;
import com.yangpeiwen.remotefish.connector.ImageServerConnector;
import com.yangpeiwen.remotefish.connector.Ser2netConnector;
import com.yangpeiwen.remotefish.util.FFmpegFrameRecorder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_8U;

public class Controller extends Activity {

    ImageView imageview;
    TextView joystickTextview;
    Thread refreshString;

    float yaw = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        //添加航向角
//        SensorManager sensorManager;
//        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
//
//        Sensor oriSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
//        sensorManager.registerListener(new SensorEventListener() {
//            public void onSensorChanged(SensorEvent event) {
//                /**
//                 * 方向传感器
//                 */
//
//                float x = event.values[0];
//                yaw = x;
//                String output = "yaw=\t" + String.valueOf(x) + "\r\n";
//
//                TextView outpuTextView = (TextView) findViewById(R.id.textView_ori);
//                outpuTextView.setText(output);
//
//            }
//
//            public void onAccuracyChanged(Sensor sensor, int accuracy) {
//            }
//        }, oriSensor, SensorManager.SENSOR_DELAY_GAME);

        //check wifi
        if (!Common.isWifiConnected(this)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("需要连接WiFi后才能操控哟~");
            builder.setPositiveButton("连接WiFi", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
//                    startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                }
            });
            builder.setNegativeButton("退出", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            builder.create().show();
        }

        //全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_controller);

        //载入RSA Key
        Encrypt.getPublicKey();

        imageview = (ImageView) findViewById(R.id.imageView);

        //更新datastring
        refreshString = new Thread(new Runnable() {
            @Override
            public void run() {
                for (; ; ) {
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                    SystemClock.sleep(100);
                }
            }
        });
        refreshString.setDaemon(true);
        refreshString.start();

        joystickTextview = (TextView) findViewById(R.id.textView_joystick);

        JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {
                angle = ( angle + 90) % 360;
                joystickTextview.setText(angle + "," + power + "\n" );
                nowangle = angle;
                nowspd = power;
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);

        JoystickView joystick2 = (JoystickView) findViewById(R.id.joystickView2);
        joystick2.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {
                angle = ( angle + 90) % 360;
                joystickTextview.setText(angle + "," + power + "\n" );
                nowangle2 = angle;
                nowspd2 = power;
            }
        }, JoystickView.DEFAULT_LOOP_INTERVAL);

        Thread sendThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (; ; ) {
                    nowtime = System.currentTimeMillis();
                    if (nowtime - lasttime > 100) {
                        if ((nowangle != lastangle) | (nowspd != lastspd)) {
                            lastspd = nowspd;
                            lasttime = nowtime;
                            lastangle = nowangle;
                            command = (byte)0x81;
                            raw_data = new byte[2];
                            raw_data[0] = (byte) (nowangle / 10);
                            raw_data[1] = (byte) (nowspd / 10);
                            datastring = "81 " + Common.bytesToHexString(raw_data);
                            executorService.execute(send_raw);
                        }
                    }

                    nowtime2 = System.currentTimeMillis();
                    if (nowtime2 - lasttime2 > 100) {
                        if ((nowangle2 != lastangle2) | (nowspd2 != lastspd2)) {
                            lastspd2 = nowspd2;
                            lasttime2 = nowtime2;
                            lastangle2 = nowangle2;
                            command = (byte)0x82;
                            raw_data = new byte[2];
                            raw_data[0] = (byte) (nowangle2 / 10);
                            raw_data[1] = (byte) (nowspd2 / 10);
                            datastring = "82 " + Common.bytesToHexString(raw_data);
                            executorService.execute(send_raw);
                        }
                    }
                    SystemClock.sleep(10);
                }
            }
        });
        sendThread.setDaemon(true);
        sendThread.start();
    }

    Runnable send_raw = new Runnable() {
        public void run() {
            if(ser2netConnector == null)return;
            if (ser2netConnector.sendCommandData(command, raw_data)) {
                show("发送指令成功");
            } else {
                show("发送指令失败");
            }
        }
    };

    byte[] raw_data;
    byte command;

    int nowangle = 0, lastangle = 0;
    int nowspd = 0, lastspd = 0;
    int nowangle2 = 0, lastangle2 = 0;
    int nowspd2 = 0, lastspd2 = 0;
    long nowtime2 = 0, lasttime2 = 0;
    long nowtime = 0, lasttime = 0;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_controller, menu);
        return true;
    }

    public void takephoto(View v) {
        File appDir = new File(Environment.getExternalStorageDirectory(), "BYGD");
        if (!appDir.exists()) appDir.mkdir();
        String filename = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, filename);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap_display.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();

            MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), filename, "BYGD");
        } catch (Exception ignore) {

        }
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(file.getPath()))));

        show("保存成功");
    }

    Runnable videoRunnable = new Runnable() {
        @Override
        public void run() {
            show("开始摄像");
            File appDir = new File(Environment.getExternalStorageDirectory(), "BYGD");
            if (!appDir.exists()) appDir.mkdir();
            String filename = System.currentTimeMillis() + ".mp4";
            File file = new File(appDir, filename);
            try {
                FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(file, 320, 240);
                recorder.setFrameRate(25);
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG4);
                recorder.setFormat("mp4");
                recorder.setVideoQuality(0);
                recorder.setVideoBitrate(96000);
                recorder.start();

//                FrameRecorder recorder = FrameRecorder.createDefault(file, 320, 240);
                long last = System.currentTimeMillis();
                while (taking_video) {
                    image_now = IplImage.create(bitmap_display.getWidth(), bitmap_display.getHeight(), IPL_DEPTH_8U, 4);
                    bitmap_display.copyPixelsToBuffer(image_now.getByteBuffer());
                    recorder.record(image_now);
                    long now = System.currentTimeMillis();
                    while (now - last < 40) {
                        now = System.currentTimeMillis();
                        SystemClock.sleep(1);
                    }
                    last = now;
                }

                recorder.stop();
                recorder.release();

                MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), filename, "BYGD");
            } catch (Exception ignore) {
                show("摄像失败");
            }
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(file.getPath()))));
            show("摄像完成");
        }
    };

    boolean taking_video = false;

    public void takevideo(View v) {
        if (taking_video) {
            taking_video = false;
            ((Button) findViewById(R.id.button_takevideo)).setText("摄像");
        } else {
            taking_video = true;
            ((Button) findViewById(R.id.button_takevideo)).setText("停止摄像");
            executorService.submit(videoRunnable);
        }
    }


    Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            Date date = new Date();
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            String time = format.format(date);
            String filename = Environment.getExternalStorageDirectory() + "/update_" + time + ".apk";
            Common.print(filename);
            File file = new File(filename);
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL("http://yangpeiwen.com/a.apk")
                        .openConnection();
                InputStream is = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buf = new byte[256];
                conn.connect();
                if (conn.getResponseCode() >= 400) {
                    show("连接超时");
                } else {
                    for (; ; ) {
                        if (is != null) {
                            int numRead = is.read(buf);
                            if (numRead <= 0) {
                                break;
                            } else {
                                fos.write(buf, 0, numRead);
                            }
                        } else {
                            break;
                        }
                    }
                }
                conn.disconnect();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Intent intent = new Intent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setAction(android.content.Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file),
                    "application/vnd.android.package-archive");
            startActivity(intent);
        }
    };

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_exit) {
            System.exit(0);
            return true;
        } else if (id == R.id.action_update) {
            executorService.submit(updateRunnable);

        }

        return super.onOptionsItemSelected(item);
    }

    public void light(View v) {
//        send_one_byte((byte) 0xF0);
    }

    String NanoPi2_IP = "192.168.8.1";
    static Ser2netConnector ser2netConnector;

    public void connect_STM32(View v) {
        if (ser2netConnector != null) {
            ser2netConnector.stop();
        }
        ser2netConnector = new Ser2netConnector(NanoPi2_IP, 8081);
        executorService.submit(ser2net_connect);
    }

    Runnable ser2net_connect = new Runnable() {
        @Override
        public void run() {
            ser2netConnector.connect();
            datastring = "连接成功，正在获取验证码";
            if (ser2netConnector.getVerifyCode()) datastring = "获取验证码成功";
            else datastring = "获取验证码失败";
        }
    };

    ImageServerConnector imageServerConnector = null;

    public void connect_RPi(View v) {
        if (imageServerConnector != null)
            imageServerConnector.stop();
        imageServerConnector = new ImageServerConnector(NanoPi2_IP, 8080);
//        imageServerConnector = new ImageServerConnector("10.10.100.123", 8080);
        imageServerConnector.setOnPictureReceivedListener(new ImageServerConnector.OnPictureReceivedListener() {
            @Override
            public void onPictureReceived(Bitmap picture) {
                bitmap_display = picture;
                Message message = new Message();
                message.what = 2;
                handler.sendMessage(message);
            }
        });
    }

    public void connect_RPi2(View v) {
        if (imageServerConnector != null)
            imageServerConnector.stop();

//        imageServerConnector = new ImageServerConnector("192.168.0.128", 8080);
        imageServerConnector = new ImageServerConnector("192.168.1.66", 8080);
        imageServerConnector.setOnPictureReceivedListener(new ImageServerConnector.OnPictureReceivedListener() {
            @Override
            public void onPictureReceived(Bitmap picture) {
                bitmap_display = picture;
                //收到图片，通知handler更新UI
                Message message = new Message();
                message.what = 2;
                handler.sendMessage(message);
            }
        });
    }

    Bitmap bitmap_display;
    IplImage image_now;

    public void update(View v) {
        executorService.submit(updateRunnable);
    }

    public void show(String str) {
        showString = str;
        Message message = new Message();
        message.what = 3;
        handler.sendMessage(message);
    }

    String showString = "";
    Toast toast;
    public static String datastring;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                super.handleMessage(msg);
                TextView tv = (TextView) findViewById(R.id.textView_data);

                if (msg.what == 1) {
                    tv.setText(datastring);
                } else if (msg.what == 2) {
                    BitmapDrawable bd = new BitmapDrawable(bitmap_display);
                    imageview.setBackground(bd);
                } else if (msg.what == 3) {
                    if (toast == null)
                        toast = Toast.makeText(getApplicationContext(),
                                showString, Toast.LENGTH_SHORT);
                    else
                        toast.setText(showString);
                    toast.show();
                }
            } catch (Exception ignored) {

            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }

    public void test(View v) {
        Intent intent = new Intent();
        intent.setClass(this, Setting.class);
        startActivity(intent);
    }

    private ExecutorService executorService = Executors.newFixedThreadPool(4);
}
