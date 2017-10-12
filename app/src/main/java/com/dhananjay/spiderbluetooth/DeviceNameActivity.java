package com.dhananjay.spiderbluetooth;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class DeviceNameActivity extends AppCompatActivity {

    private static final String TAG="DeviceNameActivity";
    private static final String PREF_KEY_DEVICE_NAME = "PREF_KEY_DEVICE_NAME";

    private EditText deviceNameET;
    private Button confirmBt;

    //set view
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_name);

        init();
    }

    //initialize views
    private void init() {
        deviceNameET = (EditText) findViewById(R.id.device_name_et);
        confirmBt = (Button) findViewById(R.id.confirm_bt);
        confirmBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!deviceNameET.getText().toString().equals("")){

                    PreferenceManager.getDefaultSharedPreferences(getApplication()).edit()
                            .putString(PREF_KEY_DEVICE_NAME, deviceNameET.getText().toString())
                            .apply();//set the device name
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent);//goto main activity
                    finish();
                }else{
                    Toast.makeText(getApplicationContext(), "enter valid name",Toast.LENGTH_SHORT)
                            .show();//show error message
                }
            }
        });
    }
}
