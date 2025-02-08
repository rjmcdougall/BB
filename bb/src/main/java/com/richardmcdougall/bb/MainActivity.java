package com.richardmcdougall.bb;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;



public class MainActivity extends Activity {

    private static final String TAG = "BB.MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Launch the specified service when this message is received
        Intent startServiceIntent = new Intent(this, BBService.class);
        this.startService(startServiceIntent);
    }
}