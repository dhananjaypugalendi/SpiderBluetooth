package com.dhananjay.spiderbluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private static final String TAG="MainActivity";
    private static final int ENABLE_BT_REQUEST_CODE = 1;
    private static final int SENSOR_FREQUENCY = 250;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastUpdate=0;

    private BluetoothAdapter adapter;
    private BluetoothDevice device;
    private BluetoothSocket socket;
    //HC-05	98:D3:35:00:A9:F7
    private static final String PREF_KEY_DEVICE_NAME = "PREF_KEY_DEVICE_NAME";
    private String deviceName ="";
    private final String DEVICE_ADDRESS = "98:D3:35:00:A9:F7";
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");//Serial Port Service ID
    private OutputStream outputStream;
    private InputStream inputStream;
    boolean deviceConnected;
    boolean stopThread;
    SharedPreferences sharedPreferences;
    TextView accValXTV;
    TextView accValYTV;
    TextView accValZTV;
    TextView deviceNameTV;
    ImageView directionIV;

    //set layout
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        checkAndEnableBluetooth();

    }

    //callback if the bluetooth was enabled successfully
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Log.d(TAG, "onActivityResult: ");
        if(requestCode == ENABLE_BT_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                //Log.d(TAG, "onActivityResult: bluetooth enabled");
                Toast.makeText(getApplicationContext(), "bluetooth enabled", Toast.LENGTH_SHORT).show();
                //scanForDevices();
                getBondedDevices();
            }
            if(resultCode == Activity.RESULT_CANCELED){
                //Log.d(TAG, "onActivityResult: error occurred");
                Toast.makeText(getApplicationContext(), "error occurred", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //create options menu to display the edit icon
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_name, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //create an intent to trigger DeviceNameActivity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.edit_device) {
            Intent intent = new Intent(this, DeviceNameActivity.class);
            startActivity(intent);
            finish();
        }
        return true;
    }

    //unregister listener to stop receiving accelerometer updates
    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    //register listener to resume receiving accelerometer updates
    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //disconnect the device and free up resources
    @Override
    protected void onDestroy() {
        try {
            stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    //initialize views, sensors,and bluetooth
    private void init() {
        accValXTV = (TextView) findViewById(R.id.acc_val_x_tv);
        accValYTV = (TextView) findViewById(R.id.acc_val_y_tv);
        accValZTV = (TextView) findViewById(R.id.acc_val_z_tv);
        directionIV = (ImageView) findViewById(R.id.direction);
        deviceNameTV = (TextView) findViewById(R.id.device_name_tv);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
        deviceName = sharedPreferences.getString(PREF_KEY_DEVICE_NAME, "HC-05");
        deviceNameTV.setText(deviceName);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        adapter = BluetoothAdapter.getDefaultAdapter();
    }

    //intent to enable bluetooth
    private void checkAndEnableBluetooth() {
        if(adapter == null){
            //Log.d(TAG, "checkAndEnableBluetooth: this device doesnt support bluetooth");
            Toast.makeText(getApplicationContext(), "this device doesnt support bluetooth", Toast.LENGTH_SHORT).show();
        }else {
            if(!adapter.isEnabled()){
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, ENABLE_BT_REQUEST_CODE);
                //Log.d(TAG, "checkAndEnableBluetooth: enabling bluetooth");
                Toast.makeText(getApplicationContext(), "enabling bluetooth", Toast.LENGTH_SHORT).show();
            }else {
                //Log.d(TAG, "checkAndEnableBluetooth: bluetooth already enabled");
                getBondedDevices();
            }
        }
    }

    //get already bonded devices
    private void getBondedDevices() {
        Set<BluetoothDevice> bondedDevices = adapter.getBondedDevices();
        if(bondedDevices.isEmpty()) {
            Toast.makeText(getApplicationContext(),"please Pair the Device first",Toast.LENGTH_SHORT).show();
        }
        else {
            for (BluetoothDevice iterator : bondedDevices) {
                //Log.d(TAG, "getBondedDevices: "+iterator.getName()+ "\t" + iterator.getAddress());
                if(iterator.getName().equals(deviceName)){
                    //Log.d(TAG, "getBondedDevices: device found");
                    device = iterator;
                    start();
                    break;
                }
            }
            if(device == null){
                Toast.makeText(getApplicationContext(), "pair the device first", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //connect to the microcontroller
    private boolean connectBluetoothDevice(){
        //Log.d(TAG, "connectBluetoothDevice: ");
        boolean connected = true;
        try{
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID);
            socket.connect();
            Toast.makeText(getApplicationContext(), "connected", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            //Log.d(TAG, "connectBluetoothDevice: exception");
            Toast.makeText(getApplicationContext(), "not connected", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            connected = false;
        }
        if(connected){
            try {
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                inputStream = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return connected;
    }

    //listen for the microcontroller data
    private void listenForData(){
        //Log.d(TAG, "listenForData: ");
        final Handler handler = new Handler();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!Thread.currentThread().isInterrupted() && !stopThread){
                    try {
                        int byteCount = inputStream.available();
                        if(byteCount>0){
                            byte[] rawbytes = new byte[byteCount];
                            inputStream.read(rawbytes);
                            final String string = new String(rawbytes, "UTF-8");
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    //Log.d(TAG, "run: message to main thread\n"+string);
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        stopThread = true;
                    }
                }
            }
        });
        thread.start();
    }

    //send string data to microcontroller
    private void sendData(String s){
        try{
            outputStream.write(s.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Log.d(TAG, "sendData: "+s);
    }

    //send integer data to microcontroller
    private void sendData(int i){
        try{
            outputStream.write(i);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Log.d(TAG, "sendData: "+i);
    }

    //start connection procedures
    private void start(){
        //Log.d(TAG, "start: ");
        if(connectBluetoothDevice()){
            deviceConnected = true;
            //Log.d(TAG, "start: device connected");
            //listenForData();
        }
    }

    //end connection and release the resources
    private void stop() throws IOException{
        //Log.d(TAG, "stop: ");
        stopThread = true;
        if(outputStream!=null) outputStream.close();
        if(inputStream!=null) inputStream.close();
        if(socket!=null) socket.close();
        deviceConnected = false;
        //Log.d(TAG, "stop: connection closed");
    }

    //called every time the sensor value changes
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        double x = sensorEvent.values[0];
        double y = sensorEvent.values[1];
        double z = sensorEvent.values[2];

        long currTime = System.currentTimeMillis();
        if(currTime - lastUpdate > SENSOR_FREQUENCY){
            lastUpdate = currTime;
            int s;
            x = Math.round(x * 100.0) / 100.0;
            y = Math.round(y * 100.0) / 100.0;
            z = Math.round(z * 100.0) / 100.0;
            accValXTV.setText(String.valueOf(x));
            accValYTV.setText(String.valueOf(y));
            accValZTV.setText(String.valueOf(z));
            //Log.d(TAG, "onSensorChanged: "+x+" "+y+" "+z);
            //s=(x>0?"+":"-")+Math.abs((int)x)+""+(y>0?"+":"-")+Math.abs((int)y)+""+(z>0?"+":"-")+Math.abs((int)z)+"!";
            if(y>6) {
                s=1; //backward
                directionIV.setImageResource(R.drawable.ic_chevron_down);
            }
            else if(y<-4) {
                s=2; //forward
                directionIV.setImageResource(R.drawable.ic_chevron_up);
            }
            else if(x<-6) {
                s=3; //right
                directionIV.setImageResource(R.drawable.ic_chevron_right);
            }
            else if(x>6) {
                s=4; //left
                directionIV.setImageResource(R.drawable.ic_chevron_left);
            }
            else {
                s=0; //idle
                directionIV.setImageResource(R.drawable.ic_target);
            }
            if(deviceConnected) sendData(s);
        }
    }

    //called every time sensor accuracy changes
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
