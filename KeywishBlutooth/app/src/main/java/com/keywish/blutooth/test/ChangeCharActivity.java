package com.keywish.blutooth.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.keywish.blutooth.service.BleService;
import com.keywish.blutooth.test.R;

public class ChangeCharActivity extends AppCompatActivity implements OnClickListener {
    TextView descriptor1;
    TextView descriptor2;
    TextView charHex;
    TextView charString;
    TextView charformat;
    TextView time;
    TextView resultcount;
    Button writeButton;
    Button readButton;
    Button clear_result;
    Button notifyButton;
    Button save_result;
    ScrollView scroll;
    TextView notify_resualt;
    RadioGroup radioGroup;
    RadioButton format_hex;
    RadioButton format_string;
    Button timing_write;
    Boolean writing = false;
    EditText write_time;
    UUID charUuid;
    UUID serUuid;
    BleService bleService;
    BluetoothGattCharacteristic gattChar;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    String text_hex = null;
    String text_string = null;
    String result = null;
    String resultLength = null;
    int resultLengthNum;
    int prop;
    int rssi;
    long period;
    int write_byte_number;
    boolean startNotify;
    boolean isNotifyHex;
    boolean isHex;

    private ServiceConnection conn = new ServiceConnection() {
        @SuppressLint("NewApi")
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            // TODO Auto-generated method stub
            bleService = ((BleService.LocalBinder) service).getService();
            gattChar = bleService.mBluetoothGatt.getService(serUuid)
                    .getCharacteristic(charUuid);
            bleService.mBluetoothGatt.readCharacteristic(gattChar);
            if (gattChar.getDescriptors().size() != 0) {
                BluetoothGattDescriptor des = gattChar.getDescriptors().get(0);
                des.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bleService.mBluetoothGatt.writeDescriptor(des);
            }
            int prop = gattChar.getProperties();
            if ((prop & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                bleService.mBluetoothGatt.setCharacteristicNotification(
                        gattChar, false);
            }
            if ((prop & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
                bleService.mBluetoothGatt.setCharacteristicNotification(
                        gattChar, false);
            }
            if ((prop & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                bleService.mBluetoothGatt.setCharacteristicNotification(
                        gattChar, false);
            }
            if ((prop & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                bleService.mBluetoothGatt.setCharacteristicNotification(
                        gattChar, true);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            // TODO Auto-generated method stub
            bleService = null;
        }
    };

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @SuppressLint("NewApi")
        @Override
        public void onReceive(Context arg0, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if (BleService.ACTION_GATT_RSSI.equals(action)) {
                rssi = intent.getExtras().getInt(BleService.EXTRA_DATA_RSSI);
                ChangeCharActivity.this.invalidateOptionsMenu();
            }
            if (BleService.ACTION_CHAR_READED.equals(action)) {
                final String des1String = intent.getExtras().getString(
                        "desriptor1");
                final String des2String = intent.getExtras().getString(
                        "desriptor2");
                final String stringValue = intent.getExtras().getString(
                        "StringValue");
                final String hexValue = intent.getExtras()
                        .getString("HexValue");
                final String readTime = intent.getExtras().getString("time");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated methdo stub
                        charString.setText(getText(R.string.character_string) +":"+ stringValue);
                        charHex.setText(getText(R.string.hexadecimal) +":"+ hexValue);
                        time.setText(getText(R.string.read_time) +":"+ readTime);
                        descriptor1.setText(des1String);
                        descriptor2.setText(des2String);
                    }
                });
            }

            if (BleService.ACTION_DATA_AVAILABLE.equals(action)) {
                if (isNotifyHex) {
                    result = intent.getExtras()
                            .getString(BleService.EXTRA_DATA);
                } else {
                    result = intent.getExtras().getString(
                            BleService.EXTRA_STRING_DATA);
                }
                int countNumber = intent.getExtras().getInt(
                        BleService.EXTRA_DATA_LENGTH);
                if (resultLengthNum != 0) {
                    resultLengthNum += countNumber;
                } else {
                    resultLengthNum = countNumber;
                }
                if (text_string != null) {
                    text_string = text_string + result;
                } else {
                    text_string = result;
                }
                resultLength = resultLengthNum + "";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        notify_resualt.setText(text_string);
                        resultcount.setText(getText(R.string.result_count) + resultLength);
                    }
                });
            }
            if (BleService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(ChangeCharActivity.this, getText(R.string.disconnection_equipment),
                        Toast.LENGTH_SHORT).show();
                if (sharedPreferences.getBoolean("AutoConnect", true)) {
                    bleService.connect(DeviceConnect.bleAddress);
                }
            }
        }
    };

    private IntentFilter makeIntentFilter() {
        // TODO Auto-generated method stub
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_CHAR_READED);
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleService.BATTERY_LEVEL_AVAILABLE);
        intentFilter.addAction(BleService.ACTION_GATT_RSSI);
        return intentFilter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_char);
        getSupportActionBar().setTitle(getResources().getString(R.string.feature_operation));
        sharedPreferences = this
                .getSharedPreferences("writedata", MODE_PRIVATE);
        editor = sharedPreferences.edit();
        init();
        bindService(new Intent(this, BleService.class), conn, BIND_AUTO_CREATE);
        registerReceiver(mBroadcastReceiver, makeIntentFilter());
    }

    private void init() {
        // TODO Auto-generated method stub
        descriptor1 = (TextView) findViewById(R.id.tv_descriptor1);
        descriptor2 = (TextView) findViewById(R.id.tv_descriptor2);
        charHex = (TextView) findViewById(R.id.tv_charHex);
        charString = (TextView) findViewById(R.id.tv_charString);
        charformat = (TextView) findViewById(R.id.tv_charformat);
        time = (TextView) findViewById(R.id.tv_time);
        writeButton = (Button) findViewById(R.id.btn_write);
        readButton = (Button) findViewById(R.id.btn_read);
        notifyButton = (Button) findViewById(R.id.btn_notify);
        clear_result = (Button) findViewById(R.id.clear_result);
        save_result = (Button) findViewById(R.id.save_result);
        notify_resualt = (TextView) findViewById(R.id.et_notify_resualt);
        resultcount = (TextView) findViewById(R.id.result_count);
        scroll = (ScrollView) findViewById(R.id.scrollview);
        radioGroup = (RadioGroup) findViewById(R.id.rg_hex_string);
        radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup arg0, int arg1) {
                // TODO Auto-generated method stub
                if (arg1 == R.id.rb_string) {
                    if (text_string != null) {
                        if (isNotifyHex) {
                            text_string = hexStr2Str(text_string);
                        }
                        notify_resualt.setText(text_string);
                    }
                    isNotifyHex = false;
                } else {
                    if (text_string != null) {
                        if (!isNotifyHex) {
                            text_string = str2HexStr(text_string);
                        }
                        notify_resualt.setText(text_string);
                    }
                    isNotifyHex = true;
                }
            }
        });
        writeButton.setOnClickListener(this);
        readButton.setOnClickListener(this);
        notifyButton.setOnClickListener(this);

        charUuid = UUID.fromString(getIntent().getExtras().get("charUUID")
                .toString());
        serUuid = UUID.fromString(getIntent().getExtras().get("serUUID")
                .toString());
        prop = getIntent().getExtras().getInt("properties");

        if ((prop & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
            writeButton.setVisibility(View.VISIBLE);
        }
        if ((prop & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) > 0) {
            writeButton.setVisibility(View.VISIBLE);
        }
        if ((prop & BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            readButton.setVisibility(View.VISIBLE);
        }
        if ((prop & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
            notifyButton.setVisibility(View.VISIBLE);
            radioGroup.setVisibility(View.VISIBLE);
            scroll.setVisibility(View.VISIBLE);
            resultcount.setVisibility(View.VISIBLE);
            clear_result.setVisibility(View.VISIBLE);
            save_result.setVisibility(View.VISIBLE);
        }
        clear_result.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                text_string = null;
                result = null;
                text_hex = null;
                notify_resualt.setText("");
                resultLengthNum = 0;
                resultcount.setText(getResources().getString(R.string.result_count)+"0");
            }
        });
        save_result.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                saveResualt(notify_resualt.getText().toString());
            }
        });
    }

    private void saveResualt(String result) {
        // TODO Auto-generated method stub
        if (result.isEmpty()) {
            Toast.makeText(this, getText(R.string.empty_hint), Toast.LENGTH_SHORT).show();
        } else {
            try {
                File file = new File(Environment.getExternalStorageDirectory(),
                        "BLElog" + System.currentTimeMillis() + ".txt");
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                if (!file.exists())
                    file.createNewFile();
                FileOutputStream out = new FileOutputStream(file);
                OutputStreamWriter writer = new OutputStreamWriter(out);
                writer.write(result);
                writer.flush();
                writer.close();
                out.close();
                Toast.makeText(
                        ChangeCharActivity.this,
                        "BLElog" + System.currentTimeMillis()
                                + R.string.save_hint, Toast.LENGTH_SHORT)
                        .show();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unbindService(conn);
        unregisterReceiver(mBroadcastReceiver);
    }

    @SuppressLint("InflateParams")
    public void writeDialog() {
        AlertDialog.Builder dialog = new Builder(this);
        View dialogview = LayoutInflater.from(this).inflate(
                R.layout.writedialog, null);
        final RadioGroup writeGroup = (RadioGroup) dialogview
                .findViewById(R.id.rg_write_format);
        final EditText editText = (EditText) dialogview
                .findViewById(R.id.char_value);
        final CheckBox textAddNewLineCheckbox = dialogview.findViewById(R.id.text_add_newline);
        final Button timing_write = (Button) dialogview
                .findViewById(R.id.btn_timing_write);
        write_time = (EditText) dialogview.findViewById(R.id.ed_writetime);
        final Button btn_0 = (Button) dialogview.findViewById(R.id.btn_00);
        final Button btn_1 = (Button) dialogview.findViewById(R.id.btn_01);
        final Button btn_2 = (Button) dialogview.findViewById(R.id.btn_02);
        final TextView timing_write_count = (TextView) dialogview
                .findViewById(R.id.tv_timing_write_count);
        final RadioButton radio1 = (RadioButton) dialogview
                .findViewById(R.id.rb_write_string);
        final RadioButton radio2 = (RadioButton) dialogview
                .findViewById(R.id.rb_write_hex);
        final EditText editbtn1 = (EditText) dialogview
                .findViewById(R.id.ed_edit_btn1);
        final EditText editbtn2 = (EditText) dialogview
                .findViewById(R.id.ed_edit_btn2);
        final EditText editbtn3 = (EditText) dialogview
                .findViewById(R.id.ed_edit_btn3);

        btn_0.setText(sharedPreferences.getString("btn00" + charUuid, "00"));
        btn_1.setText(sharedPreferences.getString("btn01" + charUuid, "01"));
        btn_2.setText(sharedPreferences.getString("btn02" + charUuid, "02"));
        if (sharedPreferences.getBoolean("isHex", false)) {
            isHex = true;
            radio2.setChecked(true);
            radio1.setChecked(false);
        } else {
            isHex = false;
            radio1.setChecked(true);
            radio2.setChecked(false);
        }

        writeGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup arg0, int arg1) {
                // TODO Auto-generated method stub
                if (arg1 == R.id.rb_write_string) {
                    isHex = false;
                    editor.putBoolean("isHex", false);
                }
                if (arg1 == R.id.rb_write_hex) {
                    isHex = true;
                    editor.putBoolean("isHex", true);
                }
                editor.commit();
            }
        });
        editbtn1.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
                checkInputkey(arg0);
                String str = arg0.toString();
                if (arg0.length() > 0) {
                    btn_0.setText(str);
                }
                editor.putString("btn00" + charUuid, str);
                editor.commit();
            }
        });
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                                      int count) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                checkInputkey(s);
            }
        });
        editbtn2.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {
                // TODO Auto-generated method stub
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
                checkInputkey(arg0);
                if (arg0.length() > 0) {
                    btn_1.setText(arg0);
                }
                editor.putString("btn01" + charUuid, arg0.toString());
                editor.commit();
            }
        });
        editbtn3.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence arg0, int arg1, int arg2,
                                      int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
                // TODO Auto-generated method stub
            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
                checkInputkey(arg0);
                if (arg0.length() > 0) {
                    btn_2.setText(arg0);
                }
                editor.putString("btn02" + charUuid, arg0.toString());
                editor.commit();
            }
        });
        btn_0.setOnClickListener(new OnClickListener() {

            @SuppressLint("NewApi")
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                String transStr = btn_0.getText().toString();
                try {
                    if (isHex) {
                        gattChar.setValue(str2Byte(transStr));
                    } else {
                        if (transStr.contains("\n")) {
                            transStr = transStr.replace("\n", "\r\n");
                        }
                        gattChar.setValue(transStr);
                    }
                    bleService.mBluetoothGatt.writeCharacteristic(gattChar);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
            }
        });
        btn_1.setOnClickListener(new OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                String transStr = btn_1.getText().toString();
                try {
                    if (isHex) {
                        gattChar.setValue(str2Byte(transStr));
                    } else {
                        if (transStr.contains("\n")) {
                            transStr = transStr.replace("\n", "\r\n");
                        }
                        gattChar.setValue(transStr);
                    }
                    bleService.mBluetoothGatt.writeCharacteristic(gattChar);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
            }
        });
        btn_2.setOnClickListener(new OnClickListener() {

            @SuppressLint("NewApi")
            @Override
            public void onClick(View arg0) {
                // TODO Auto-generated method stub
                String transStr = btn_2.getText().toString();
                try {
                    if (isHex) {
                        gattChar.setValue(str2Byte(transStr));
                    } else {
                        if (transStr.contains("\n")) {
                            transStr = transStr.replace("\n", "\r\n");
                        }
                        gattChar.setValue(transStr);
                    }
                    bleService.mBluetoothGatt.writeCharacteristic(gattChar);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
            }
        });
        timing_write.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                period = Integer.parseInt(write_time.getText().toString());
                if (writing) {
                    timing_write.setText(getText(R.string.delivery_on_time));
                    writing = false;
                } else {
                    timing_write.setText(getText(R.string.stop_sending));
                    write_byte_number = 0;
                    writing = true;
                }
                new Thread(new Runnable() {
                    @SuppressLint("NewApi")
                    public void run() {
                        try {
                            while (writing) {
                                String charvalue = editText.getText()
                                        .toString();
                                if (textAddNewLineCheckbox.isChecked()) {
                                    charvalue += '\r';
                                    charvalue += '\n';
                                }
                                if (!charvalue.isEmpty()) {
                                    if (isHex) {
                                        write_byte_number += charvalue.length()
                                                / 2 + charvalue.length() % 2;
                                        byte[] str = str2Byte(charvalue);
                                        gattChar.setValue(str);
                                    } else {
                                        write_byte_number += charvalue.length();
                                        gattChar.setValue(charvalue);
                                    }
                                    bleService.mBluetoothGatt
                                            .writeCharacteristic(gattChar);
                                    Thread.sleep(period);
                                    runOnUiThread(new Runnable() {
                                        @SuppressLint("NewApi")
                                        @Override
                                        public void run() {
                                            // TODO Auto-generated method stub
                                            timing_write_count.setText(getApplicationContext().getResources().getString(R.string.count)
                                                    + write_byte_number);
                                        }
                                    });
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        dialog.setView(dialogview);
        dialog.setPositiveButton("发送", new DialogInterface.OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                // TODO Auto-generated method stub
                String charvalue = editText.getText().toString();
                if (textAddNewLineCheckbox.isChecked()) {
                    charvalue += '\r';
                    charvalue += '\n';
                }
                if (!charvalue.isEmpty()) {
                    if (isHex) {
                        byte[] str = str2Byte(charvalue);
                        gattChar.setValue(str);
                    } else {
                        gattChar.setValue(charvalue);
                    }
                    bleService.mBluetoothGatt.writeCharacteristic(gattChar);
                }
            }
        });
        dialog.show();
    }

    private void checkInputkey(Editable arg0) {
        if (isHex && arg0.length() > 0) {
            int pos = arg0.length() - 1;
            for (int i = 0; i < arg0.length(); i++) {
                char c = arg0.charAt(i);
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
                        || (c >= 'A' && c <= 'F')) {
                } else {
                    arg0.delete(pos, pos + 1);
                }
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        switch (view.getId()) {
            case R.id.btn_read:
                bleService.mBluetoothGatt.readCharacteristic(gattChar);
                break;
            case R.id.btn_write:
                writeDialog();
                break;
            case R.id.btn_notify:
                if (!startNotify) {
                    bleService.mBluetoothGatt.setCharacteristicNotification(
                            gattChar, true);
                    startNotify = true;
                    notifyButton.setText(getText(R.string.stop_notice));
                } else {
                    bleService.mBluetoothGatt.setCharacteristicNotification(
                            gattChar, false);
                    startNotify = false;
                    notifyButton.setText(getText(R.string.start_notice));
                }
                break;
        }
    }

    public static String str2HexStr(String str) {

        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = str.getBytes();
        int bit;
        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
        }
        return sb.toString().trim();
    }

    public static String hexStr2Str(String hexStr) {
        String str = "0123456789ABCDEF";
        char[] hexs = hexStr.toCharArray();
        byte[] bytes = new byte[hexStr.length() / 2];
        int n;
        for (int i = 0; i < bytes.length; i++) {
            n = str.indexOf(hexs[2 * i]) * 16;
            n += str.indexOf(hexs[2 * i + 1]);
            bytes[i] = (byte) (n & 0xff);
        }
        return new String(bytes);
    }

    public static byte[] str2Byte(String hexStr) {
        int b = hexStr.length() % 2;
        if (b != 0) {
            hexStr = "0" + hexStr;
        }
        String[] a = new String[hexStr.length() / 2];
        byte[] bytes = new byte[hexStr.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            a[i] = hexStr.substring(2 * i, 2 * i + 2);
        }
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) Integer.parseInt(a[i], 16);
        }
        return bytes;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        getMenuInflater().inflate(R.menu.services, menu);
        menu.getItem(1).setVisible(false);
        menu.getItem(0).setTitle(rssi + "");
        return true;
    }

}