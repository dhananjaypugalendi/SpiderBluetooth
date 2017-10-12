package com.dhananjay.spiderbluetooth;

import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG="SplashActivity";
    private static final String PREF_KEY_DEVICE_NAME = "PREF_KEY_DEVICE_NAME";

    //initialize views
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        decideNextActivity();
    }

    //check if the bluetooth device name is present already
    private void decideNextActivity() {
        if(PreferenceManager.getDefaultSharedPreferences(getApplication()).getString(PREF_KEY_DEVICE_NAME, null)==null){
            Intent intent = new Intent(this, DeviceNameActivity.class);
            startActivity(intent);//get device name
            finish();
        }else {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);//goto main activity
            finish();
        }
    }
}
