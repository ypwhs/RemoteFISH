package com.yangpeiwen.remotefish;

/**
 * Created by ypw
 * on 2015-09-04 下午2:24.
 */

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Controller extends Activity {
    //    compile 'com.android.support:appcompat-v7:22.2.1'

    ImageView imageview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_controller);
        imageview = (ImageView) findViewById(R.id.imageView);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    Message message = new Message();
                    message.what = 1;
                    handler.sendMessage(message);
                    SystemClock.sleep(100);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        SharedPreferences sp = getSharedPreferences("data", 0);
        EditText editText_RPi = (EditText) findViewById(R.id.editText_RPi);
        editText_RPi.setText(sp.getString("IP", "192.168.1.123"));

        SeekBar seekBar_speed = (SeekBar)findViewById(R.id.seekBar_speed);
        seekBar_speed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                print("Speed: " + progress);
                command = new byte[1];
                command[0] = (byte) (0xC0 + progress);
                executorService.execute(runnable_send_stm32);
                TextView textView_speed = (TextView) findViewById(R.id.textView_speed);
                textView_speed.setText("Speed: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        SeekBar seekBar_dir = (SeekBar)findViewById(R.id.seekBar_dir);
        seekBar_dir.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                print("Direction: " + progress);
                command = new byte[1];
                command[0] = (byte) (0xD0 + progress);
                executorService.execute(runnable_send_stm32);
                TextView textView_dir = (TextView) findViewById(R.id.textView_dir);
                textView_dir.setText("Direction: " + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        findViewById(R.id.button_m1).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    cmd1((byte) 0xA1);
                } else if (action == MotionEvent.ACTION_UP) {
                    cmd1((byte) 0xB1);
                }
                return false;
            }
        });

        findViewById(R.id.button_m2).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    cmd1((byte) 0xA2);
                } else if (action == MotionEvent.ACTION_UP) {
                    cmd1((byte) 0xB2);
                }
                return false;
            }
        });

        findViewById(R.id.button_m3).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    cmd1((byte) 0xA3);
                } else if (action == MotionEvent.ACTION_UP) {
                    cmd1((byte) 0xB3);
                }
                return false;
            }
        });

        findViewById(R.id.button_m4).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    cmd1((byte) 0xA4);
                } else if (action == MotionEvent.ACTION_UP) {
                    cmd1((byte) 0xB4);
                }
                return false;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_controller, menu);
        return true;
    }

    Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            print(Environment.getExternalStorageDirectory().toString());
            File file = new File(Environment.getExternalStorageDirectory() + "/update.apk");
            try{
                HttpURLConnection conn = (HttpURLConnection)new URL("http://yangpeiwen.com/a.apk")
                        .openConnection();
                InputStream is = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(file);
                byte[] buf = new byte[256];
                conn.connect();
                if (conn.getResponseCode() >= 400) {
                    Toast.makeText(Controller.this, "连接超时", Toast.LENGTH_SHORT)
                            .show();
                } else {
                    while (true) {
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
            }catch (Exception e){
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
        }else if(id == R.id.action_update){
            executorService.submit(updateRunnable);

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.exit(0);
    }

    byte[] command;
    int lightstate = 0;

    public void light(View v){
        if(lightstate == 0){
            lightstate = 1;
            ((Button)findViewById(R.id.button_light)).setText("light on");
            cmd1((byte)0xA5);
        }else{
            lightstate = 0;
            ((Button)findViewById(R.id.button_light)).setText("light off");
            cmd1((byte)0xB5);
        }
    }

    public void cmd1(byte cmd) {
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

    public void connect_STM32(View v) {
        executorService.execute(runnable_connect_STM32);
        TextView tv = (TextView) findViewById(R.id.textView_data);
        datastring = "连接中";
    }

    String RPi_IP = "192.168.1.123";

    public void connect_RPi(View v) {
        executorService.execute(runnable_connect_RPi);
        SharedPreferences.Editor editor = getSharedPreferences("data", 0).edit();
        EditText editText_RPi = (EditText) findViewById(R.id.editText_RPi);
        RPi_IP = editText_RPi.getText().toString();
        editor.putString("IP", editText_RPi.getText().toString());
        editor.apply();
    }

    String datastring;
    Runnable runnable_connect_STM32;

    {
        runnable_connect_STM32 = new Runnable() {
            @Override
            public void run() {
                boolean success = connect_STM32();
                if (success) {
                    print("连接成功");
                    datastring = "连接成功";
                    while (true) {
                        byte[] data = read_STM32();
//                        try {
//                            datastring = new String(data, "GBK");
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
                        datastring = Common.bytesToHexString(data);
                    }
                } else {
                    print("连接失败");
                }
            }
        };
    }

    Bitmap bitmap_display;
    Runnable runnable_connect_RPi;

    {
        runnable_connect_RPi = new Runnable() {
            @Override
            public void run() {
                int fail = 0;
                while (fail < 10) {
                    boolean success = false;
                    try {
                        client_RPi = new Socket(RPi_IP, 8080);
                        client_RPi.setSoTimeout(200);
//                        client_RPi = new Socket("10.50.255.205", 8080);
                        inputStream_RPi = client_RPi.getInputStream();
                        success = true;
                    } catch (Exception e) {
                        e.printStackTrace();
                        print("错误:" + e.getMessage());
                    }

                    if (success) {
                        fail = 0;
                        print("连接成功");
                        try {
                            int bytesRead, current = 0;
                            do {
                                bytesRead = inputStream_RPi.read(buffer, current, (buffer.length - current));
                                if (bytesRead >= 0) current += bytesRead;
                            } while (bytesRead > -1);
                            bitmap_display = BitmapFactory.decodeByteArray(buffer, 0, current);
                            Message message = new Message();
                            message.what = 2;
                            handler.sendMessage(message);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        fail++;
                        print("连接失败");
                        datastring = "连接失败";
                    }
                }
            }
        };
    }

    byte[] buffer = new byte[1024 * 1024];

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
                }
            } catch (Exception ignored) {

            }
        }
    };

    Socket client_STM32 = null;
    OutputStream outputStream_STM32;
    InputStream inputStream_STM32;

    public boolean connect_STM32() {
        boolean f = false;
        try {
            client_STM32 = new Socket("10.10.100.254", 8899);
            outputStream_STM32 = client_STM32.getOutputStream();
            inputStream_STM32 = client_STM32.getInputStream();
            f = true;
        } catch (Exception e) {
            e.printStackTrace();
            print("错误:" + e.getMessage());
        }
        return f;
    }

    Socket client_RPi = null;
    InputStream inputStream_RPi;


    public boolean send_STM32(byte[] buf) {
        boolean f = false;
        try {
            outputStream_STM32.write(buf);
            outputStream_STM32.flush();
            f = true;
            datastring = "发送数据:" + Common.bytesToHexString(buf);
            print("发送数据:" + Common.bytesToHexString(buf) + ",长度:" + buf.length);
        } catch (Exception e) {
            e.printStackTrace();
            datastring = "发送失败";
            print("错误:" + e.getMessage());
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
            print("错误:" + e.getMessage());
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
