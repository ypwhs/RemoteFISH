package com.yangpeiwen.remotefish;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.yangpeiwen.remotefish.connector.Ser2netConnector;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Setting extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        Button record = (Button) findViewById(R.id.button_record);
        record.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    command = 5;
                    raw_data = "1".getBytes();
                    executorService.submit(send_raw);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    command = 6;
                    raw_data = "1".getBytes();
                    executorService.submit(send_raw);
                }
                return false;
            }
        });
    }

    public void reset_audio(View v) {
        command = 7;
        raw_data = "".getBytes();
        executorService.submit(send_raw);
    }

    Ser2netConnector ser2netConnector = Controller.ser2netConnector;

    public void change_wifi_name(View v) {
        AlertDialog.Builder builder = new Builder(this);
        final EditText data = new EditText(this);
        builder.setView(data);
        builder.setMessage("请输入新的WiFi名称：");
        builder.setTitle("提示");
        builder.setPositiveButton("确定", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (data.getText().length() < 1) {
                    show("WiFi名称不能为空");
                    return;
                }
                new_wifi_name = data.getText().toString().getBytes();
                executorService.submit(change_wifi_name);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.create().show();
    }

    public void change_wifi_psk(View v) {
        AlertDialog.Builder builder = new Builder(this);
        final EditText password = new EditText(this);
        password.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(password);
        builder.setMessage("请输入新的WiFi密码：");
        builder.setTitle("提示");
        builder.setPositiveButton("确定", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (password.getText().length() < 8) {
                    show("WiFi密码不能少于8位");
                    return;
                }
                new_wifi_psk = password.getText().toString().getBytes();
                executorService.submit(change_wifi_psk);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.create().show();
    }

    public void play_music(View v) {
        AlertDialog.Builder builder = new Builder(this);
        final EditText data = new EditText(this);
        data.setText("qhc.mp3");
        builder.setView(data);
        builder.setMessage("请输入mp3文件名：");
        builder.setTitle("提示");
        builder.setPositiveButton("确定", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (data.getText().length() < 1) {
                    show("文件名不能为空");
                    return;
                }
                command = 4;
                raw_data = data.getText().toString().getBytes();
                executorService.submit(send_raw);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.create().show();
    }

    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    byte[] new_wifi_name;
    byte[] new_wifi_psk;
    byte[] raw_data;
    byte command;

    Runnable change_wifi_psk = new Runnable() {
        public void run() {
            if (ser2netConnector.sendCommandData((byte) 2, new_wifi_psk)) {
                show("修改WiFi密码成功，请使用新的密码连接WiFi。");
            } else {
                show("修改WiFi密码失败");
            }
        }
    };

    Runnable change_wifi_name = new Runnable() {
        @Override
        public void run() {
            if (ser2netConnector.sendCommandData((byte) 3, new_wifi_name)) {
                show("修改WiFi名称成功，请使用新的名称连接WiFi。");
            } else {
                show("修改WiFi名称失败");
            }
        }
    };


    Runnable send_raw = new Runnable() {
        public void run() {
            if (ser2netConnector.sendCommandData(command, raw_data)) {
                show("发送指令成功");
            } else {
                show("发送指令失败");
            }
        }
    };

    public void emo(View v){
        int tag = Integer.valueOf((String) v.getTag());
        command = 8;
        raw_data = new byte[1];
        raw_data[0] = (byte)tag;
        executorService.submit(send_raw);
    }

    public void bootloader(View v){
        int tag = Integer.valueOf((String) v.getTag());
        command = (byte)0x83;
        raw_data = new byte[1];
        raw_data[0] = (byte)tag;
        executorService.submit(send_raw);
    }

    String showString = "";

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                super.handleMessage(msg);
                if (msg.what == 1) {
                    Common.show(getApplicationContext(), showString);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void show(String str) {
        showString = str;
        Message message = new Message();
        message.what = 1;
        handler.sendMessage(message);
    }

}
