package com.richardmcdougall.bbinstaller;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.richardmcdougall.bbcommon.BLog;

/**
 * Created by rmc on 10/22/17.
 */


public class onBoot extends BroadcastReceiver {

    private static final int JOB_ID = 1;

    public void onReceive(Context context, Intent arg1)
    {
        //Intent intent = new Intent(context,Installer.class);
        //context.startService(intent);
        BLog.i("BBInstaller", "onBoot()");
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo jobInfo = new JobInfo.Builder(JOB_ID, new ComponentName(context, Installer.class))
                .setPersisted(true) // To survive reboots
                //... other constraints like network, charging, etc....
                .build();
        jobScheduler.schedule(jobInfo);
        BLog.i("BBInstaller", "started");
    }
}


//public class onBoot extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        Intent intent = new Intent(getApplicationContext(),Installer.class);
//        getApplicationContext().startService(intent);
//        BLog.i("BBInstaller", "started");
//
//    }
//}
