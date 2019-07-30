package com.keywish.blutooth.test;

import java.util.Timer;
import java.util.TimerTask;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import com.keywish.blutooth.adapter.BleDeviceListAdapter;
import com.keywish.blutooth.utils.Utils;
import com.keywish.blutooth.test.R;


public class MainActivity extends AppCompatActivity {
    ListView listView;
    SwipeRefreshLayout swagLayout;
    BluetoothAdapter mBluetoothAdapter;
    private LeScanCallback mLeScanCallback;
    private static final String TAG = "BleTools_MainActivity";
    BleDeviceListAdapter mBleDeviceListAdapter;
    boolean isExit;
    Handler handler;

    SharedPreferences sharedPreferences;
    Editor editor;

    private static String[] PERMISSIONS_STORAGE = {
            "android.permission.ACCESS_COARSE_LOCATION"};

    //  @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        // set no title
        // requestWindowFeature(Window.FEATURE_NO_TITLE);
        // set full screnn
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // set keep screen on
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getSupportActionBar().setTitle(R.string.app_title);
        setContentView(R.layout.activity_main);

        sharedPreferences = getPreferences(0);
        editor = sharedPreferences.edit();

        init();
        getBleAdapter();
        getScanResualt();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        int state = adapter.getState();
        if (state == BluetoothAdapter.STATE_OFF) {
            adapter.enable();
        } else if (state == BluetoothAdapter.STATE_ON) {
            checkPermission();
        }
        IntentFilter statusFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mStatusReceive, statusFilter);

    }

    private BroadcastReceiver mStatusReceive = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()){
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                    switch(blueState){
                        case BluetoothAdapter.STATE_TURNING_ON:
                            break;
                        case BluetoothAdapter.STATE_ON:
                            //开始扫描
                            checkPermission();
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            break;
                    }
                    break;
            }
        }
    };
    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            //判断是否有权限
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //请求权限
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE,
                        1);
            } else {
                new Thread(new Runnable() {
                    @SuppressWarnings("deprecation")
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        mBluetoothAdapter.startLeScan(mLeScanCallback);
                    }
                }).start();
            }
        }
    }

    //权限申请回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    checkPermission();
                    return;
                } else {
                    new Thread(new Runnable() {
                        @SuppressWarnings("deprecation")
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            mBluetoothAdapter.startLeScan(mLeScanCallback);
                        }
                    }).start();
                }
                break;
        }
    }

    private void init() {

        //adapter.disable();
        // TODO Auto-generated method stub
        listView = (ListView) findViewById(R.id.lv_deviceList);
        listView.setEmptyView(findViewById(R.id.pb_empty));
        swagLayout = (SwipeRefreshLayout) findViewById(R.id.swagLayout);
        swagLayout.setVisibility(View.VISIBLE);
        swagLayout.setOnRefreshListener(new OnRefreshListener() {

            @SuppressWarnings("deprecation")
            @SuppressLint("NewApi")
            @Override
            public void onRefresh() {
                // TODO Auto-generated method stub
                mBleDeviceListAdapter.clear();
                mBluetoothAdapter.startLeScan(mLeScanCallback);
                swagLayout.setRefreshing(false);
            }
        });
        mBleDeviceListAdapter = new BleDeviceListAdapter(this);
        listView.setAdapter(mBleDeviceListAdapter);

        setListItemListener();
    }

    @SuppressLint("NewApi")
    private void getBleAdapter() {
        final BluetoothManager bluetoothManager = (BluetoothManager) this
                .getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }

    @SuppressLint("NewApi")
    private void getScanResualt() {
        mLeScanCallback = new LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, final int rssi,
                                 final byte[] scanRecord) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        mBleDeviceListAdapter.addDevice(device, rssi,
                                Utils.bytesToHex(scanRecord));
                        mBleDeviceListAdapter.notifyDataSetChanged();
                        invalidateOptionsMenu();
                    }
                });
            }
        };
    }

    private void setListItemListener() {
        listView.setOnItemClickListener(new OnItemClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // TODO Auto-generated method stub
                BluetoothDevice device = mBleDeviceListAdapter
                        .getDevice(position);
                final Intent intent = new Intent(MainActivity.this,
                        DeviceConnect.class);
                intent.putExtra(DeviceConnect.EXTRAS_DEVICE_NAME,
                        device.getName());
                intent.putExtra(DeviceConnect.EXTRAS_DEVICE_ADDRESS,
                        device.getAddress());
                startActivity(intent);
            }
        });
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
        mBleDeviceListAdapter.clear();
        mBluetoothAdapter.cancelDiscovery();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        menu.getItem(0).setTitle(getResources().getString(R.string.count) + mBleDeviceListAdapter.getCount() + getResources().getString(R.string.individual));
        return true;
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_stop:
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                break;
            case R.id.menu_autoconnect:
                if (sharedPreferences.getBoolean("AutoConnect", true)) {
                    editor.putBoolean("AutoConnect", false);
                    editor.commit();
                    Toast.makeText(this, getText(R.string.cancel_automatic_connection), Toast.LENGTH_SHORT).show();
                } else {
                    editor.putBoolean("AutoConnect", true);
                    editor.commit();
                    Toast.makeText(this, getText(R.string.set_connect_disconnection), Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.menu_about:
                MainActivity.this.startActivity(new Intent(this,
                        AboutActivity.class));
                break;
            case R.id.menu_qrcode:
                MainActivity.this.startActivity(new Intent(this,
                        QrcodeActivity.class));
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exitBy2Click();
        }
        return false;
    }

    private void exitBy2Click() {
        if (!isExit) {
            isExit = true;
            Toast.makeText(this, getText(R.string.isExit_hint), Toast.LENGTH_SHORT).show();
            new Timer().schedule(new TimerTask() {
                public void run() {
                    isExit = false;
                }
            }, 2000);
        } else {
            onDestroy();
            finish();
            System.exit(0);
        }
    }
}