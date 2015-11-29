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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.javacv.cpp.avcodec;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.yangpeiwen.remotefish.connector.RaspberryPiConnector;
import com.yangpeiwen.remotefish.util.FFmpegFrameRecorder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
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
        SensorManager sensorManager;
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        Sensor oriSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorManager.registerListener(new SensorEventListener() {
            public void onSensorChanged(SensorEvent event) {
                /**
                 * 方向传感器
                 */

                float x=event.values[0];
                yaw = x;
                String output = "yaw=\t"+String.valueOf(x)+"\r\n";

                TextView outpuTextView =(TextView)findViewById(R.id.textView_ori);
                outpuTextView.setText(output);

            }
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        }, oriSensor, SensorManager.SENSOR_DELAY_GAME);

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

        SeekBar seekBar_x = (SeekBar) findViewById(R.id.seekBar);
        seekBar_x.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                command = new byte[4];
                command[0] = (byte) 0xC4;
                command[1] = (byte) 0xC5;
                command[2] = (byte) 0xC6;
                command[3] = (byte) (progress + 0xD0);
                executorService.execute(runnable_send_stm32);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });

        SeekBar seekBar_y = (SeekBar) findViewById(R.id.seekBar2);
        seekBar_y.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                command = new byte[4];
                command[0] = (byte) 0xC4;
                command[1] = (byte) 0xC5;
                command[2] = (byte) 0xC6;
                command[3] = (byte) (progress + 0xE0);
                executorService.execute(runnable_send_stm32);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

        });

        joystickTextview = (TextView) findViewById(R.id.textView_joystick);

        JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setOnJoystickMoveListener(new JoystickView.OnJoystickMoveListener() {
            @Override
            public void onValueChanged(int angle, int power, int direction) {
                angle = ((int)yaw+angle)%360;
                byte[] cmd = new byte[5];
                cmd[0] = (byte) 0xC1;
                cmd[1] = (byte) 0xC2;
                cmd[2] = (byte) 0xC3;
                cmd[3] = (byte) (angle / 10);
                cmd[4] = (byte) (0xD0 + power / 10);
                joystickTextview.setText(angle + "," + power + "\n" + Common.bytesToHexString(cmd));
                nowangle = angle;
                nowspd = power;
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
                            command = new byte[5];
                            command[0] = (byte) 0xC1;
                            command[1] = (byte) 0xC2;
                            command[2] = (byte) 0xC3;
                            command[3] = (byte) (nowangle / 10);
                            command[4] = (byte) (0xD0 + nowspd / 10);
                            executorService.execute(runnable_send_stm32);
                        }
                    }
                    SystemClock.sleep(10);
                }
            }
        });
        sendThread.setDaemon(true);
        sendThread.start();

        findViewById(R.id.button_w).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == KeyEvent.ACTION_DOWN) {
                    command = new byte[5];
                    command[0] = (byte) 0xC1;
                    command[1] = (byte) 0xC2;
                    command[2] = (byte) 0xC3;
                    command[3] = (byte) (0 / 10);
                    command[4] = (byte) (0xDA);
                    executorService.execute(runnable_send_stm32);
                } else if (action == KeyEvent.ACTION_UP) {
                    command = new byte[5];
                    command[0] = (byte) 0xC1;
                    command[1] = (byte) 0xC2;
                    command[2] = (byte) 0xC3;
                    command[3] = (byte) (0 / 10);
                    command[4] = (byte) (0xD0);
                    executorService.execute(runnable_send_stm32);
                }
                return false;
            }
        });

        findViewById(R.id.button_d).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == KeyEvent.ACTION_DOWN) {
                    command = new byte[5];
                    command[0] = (byte) 0xC1;
                    command[1] = (byte) 0xC2;
                    command[2] = (byte) 0xC3;
                    command[3] = (byte) (90 / 10);
                    command[4] = (byte) (0xDA);
                    executorService.execute(runnable_send_stm32);
                } else if (action == KeyEvent.ACTION_UP) {
                    command = new byte[5];
                    command[0] = (byte) 0xC1;
                    command[1] = (byte) 0xC2;
                    command[2] = (byte) 0xC3;
                    command[3] = (byte) (0 / 10);
                    command[4] = (byte) (0xD0);
                    executorService.execute(runnable_send_stm32);
                }
                return false;
            }
        });
        findViewById(R.id.button_s).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == KeyEvent.ACTION_DOWN) {
                    command = new byte[5];
                    command[0] = (byte) 0xC1;
                    command[1] = (byte) 0xC2;
                    command[2] = (byte) 0xC3;
                    command[3] = (byte) (180 / 10);
                    command[4] = (byte) (0xDA);
                    executorService.execute(runnable_send_stm32);
                } else if (action == KeyEvent.ACTION_UP) {
                    command = new byte[5];
                    command[0] = (byte) 0xC1;
                    command[1] = (byte) 0xC2;
                    command[2] = (byte) 0xC3;
                    command[3] = (byte) (0 / 10);
                    command[4] = (byte) (0xD0);
                    executorService.execute(runnable_send_stm32);
                }
                return false;
            }
        });
        findViewById(R.id.button_a).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == KeyEvent.ACTION_DOWN) {
                    command = new byte[5];
                    command[0] = (byte) 0xC1;
                    command[1] = (byte) 0xC2;
                    command[2] = (byte) 0xC3;
                    command[3] = (byte) (270 / 10);
                    command[4] = (byte) (0xDA);
                    executorService.execute(runnable_send_stm32);
                } else if (action == KeyEvent.ACTION_UP) {
                    command = new byte[5];
                    command[0] = (byte) 0xC1;
                    command[1] = (byte) 0xC2;
                    command[2] = (byte) 0xC3;
                    command[3] = (byte) (0 / 10);
                    command[4] = (byte) (0xD0);
                    executorService.execute(runnable_send_stm32);
                }
                return false;
            }
        });
    }

    int nowangle = 0, lastangle = 0;
    int nowspd = 0, lastspd = 0;
    long nowtime = 0, lasttime = 0;

    public void up(View v) {
        command = new byte[4];
        command[0] = (byte) 0xC4;
        command[1] = (byte) 0xC5;
        command[2] = (byte) 0xC6;
        command[3] = (byte) 0xBA;
        executorService.execute(runnable_send_stm32);
    }

    public void down(View v) {
        command = new byte[4];
        command[0] = (byte) 0xC4;
        command[1] = (byte) 0xC5;
        command[2] = (byte) 0xC6;
        command[3] = (byte) 0xBB;
        executorService.execute(runnable_send_stm32);
    }

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
            print(filename);
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

    byte[] command;

    public void light(View v) {
        send_one_byte((byte) 0xF0);
    }

    public void send_one_byte(byte cmd) {
        command = new byte[1];
        command[0] = cmd;
        executorService.execute(runnable_send_stm32);
    }


    Runnable runnable_send_stm32 = new Runnable() {
        @Override
        public void run() {
            send_STM32(command);
        }
    };

    Thread stm32_thread = null;

    String STM32_IP = "10.10.100.254";

    public void connect_STM32(View v) {
        if (stm32_thread != null) {
            stm32_thread.interrupt();
        }

        STM32_IP = "10.10.100.254";
        port_stm32 = 8899;
        stm32_thread = new Thread(runnable_connect_STM32);
        stm32_thread.setDaemon(true);
        stm32_thread.start();
        datastring = "连接中";
    }

    public void connect_STM32_2(View v) {
        if (stm32_thread != null) {
            stm32_thread.interrupt();
        }

        STM32_IP = "10.10.100.124";
        port_stm32 = 8090;
        stm32_thread = new Thread(runnable_connect_STM32);
        stm32_thread.setDaemon(true);
        stm32_thread.start();
        datastring = "连接中";
    }

    RaspberryPiConnector raspberryPiConnector = null;

    public void connect_RPi(View v) {
        if (raspberryPiConnector != null)
            raspberryPiConnector.stop();
//        raspberryPiConnector = new RaspberryPiConnector("192.168.1.103", 8080);
        raspberryPiConnector = new RaspberryPiConnector("10.10.100.123", 8080);
        raspberryPiConnector.setOnPictureReceivedListener(new RaspberryPiConnector.OnPictureReceivedListener() {
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
        if (raspberryPiConnector != null)
            raspberryPiConnector.stop();

//        raspberryPiConnector = new RaspberryPiConnector("192.168.0.128", 8080);
        raspberryPiConnector = new RaspberryPiConnector("10.10.100.124", 8080);
        raspberryPiConnector.setOnPictureReceivedListener(new RaspberryPiConnector.OnPictureReceivedListener() {
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

    String datastring;
    Runnable runnable_connect_STM32 = new Runnable() {
        @Override
        public void run() {
            boolean success = connect_STM32();
            if (success) {
                print("连接成功");
                datastring = "连接成功";
                while (!Thread.currentThread().isInterrupted()) {
                    byte[] data = read_STM32();
                    datastring = Common.bytesToHexString(data);
                }
            } else {
                print("连接失败");
            }
        }
    };

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

    Socket client_STM32 = null;
    OutputStream outputStream_STM32;
    InputStream inputStream_STM32;

    int port_stm32 = 8899;

    public boolean connect_STM32() {
        boolean f = false;
        try {
            client_STM32 = new Socket(STM32_IP, port_stm32);
            outputStream_STM32 = client_STM32.getOutputStream();
            inputStream_STM32 = client_STM32.getInputStream();
            f = true;
        } catch (Exception e) {
            e.printStackTrace();
            print("错误:" + e.getMessage());
        }
        return f;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.exit(0);
    }

    public boolean send_STM32(byte[] buf) {
        boolean f = false;
        try {
            outputStream_STM32.write(buf);
            outputStream_STM32.flush();
            f = true;
            datastring = "发送数据:" + Common.bytesToHexString(buf);
        } catch (Exception e) {
            //e.printStackTrace();
            datastring = "发送失败:" + Common.bytesToHexString(buf);
        }
        return f;
    }

    public byte[] read_STM32() {
        byte[] read2 = null;
        try {
            byte[] buffer = new byte[1024];
            int len = inputStream_STM32.read(buffer);
            read2 = new byte[len];
            int j;
            for (j = 0; j < len; j++)
                read2[j] = buffer[j];
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (read2 != null) {
            print("读到数据:" + Common.bytesToHexString(read2) + ",长度:" + read2.length);
        }
        return read2;
    }

    public void print(String p) {
        System.out.println("OUT:" + p);
        // show(p);
        // SystemClock.sleep(200);
    }

    private ExecutorService executorService = Executors.newFixedThreadPool(4);
}
