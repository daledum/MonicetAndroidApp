package net.monicet.monicet;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.TaskParams;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by ubuntu on 15-02-2017.
 */

public class SendFilesTaskService extends GcmTaskService {

    public static final String GCM_ONEOFF_TAG = "oneoff|[0,0]";
    public static final String FILES_PATH = "path";

    @Override
    public void onInitializeTasks() {
        //TODO: called when app is updated to a new version, reinstalled etc.
        //you have to schedule your repeating tasks again
        super.onInitializeTasks();
    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        Bundle data = taskParams.getExtras();
        String path = (String)data.getCharSequence(FILES_PATH); // give it the default path: Environment.getDataDirectory(), Utils.INTERNAL_DIR_PATH

        // test starts here
        File directory = new File(Environment.getExternalStorageDirectory(), "Monicet");
        File testFileContext = new File(directory, "filesdir.txt");
        try {
            //testFileContext.createNewFile();
            FileWriter routeWriter = new FileWriter(testFileContext);
            routeWriter.append(path);
            routeWriter.flush();
            routeWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // test ends here

        // null because we haven't added the context to the bundle
        // and also because the task is triggered when the device connects to the Internet
        boolean result = Utils.sendAndDeleteFiles(null, path);
        if (result) { return GcmNetworkManager.RESULT_SUCCESS; }

        return GcmNetworkManager.RESULT_RESCHEDULE;
    }

    public static void scheduleOneOff(Context context) {
        //in this method, single OneOff task is scheduled (the target service that will be called is MyTaskService.class)
        Bundle data = new Bundle();
        data.putString(FILES_PATH, Utils.getInternalDirPathFromContext(context));

        try {
            OneoffTask oneoff = new OneoffTask.Builder()
                    //specify target service - must extend GcmTaskService
                    .setService(SendFilesTaskService.class)
                    //tag that is unique to this task (can be used to cancel task)
                    .setTag(GCM_ONEOFF_TAG)
                    //executed between 0 - 20s from now (earliest point to latest point at which your task must be run)
                    .setExecutionWindow(10, 11) //was 10, 10 ---- 10,11
                    //specify whether this task should be persisted across reboots
                    .setPersisted(true)
                    //set required network state, this line is optional, NETWORK_STATE_CONNECTED is the default
                    //.setRequiredNetwork(Task.NETWORK_STATE_ANY)
                    //request that charging must be connected, this line is optional
                    .setRequiresCharging(false)
                    //set some data we want to pass to our task
                    .setExtras(data)
                    //if another task with same tag is already scheduled, replace it with this task
                    .setUpdateCurrent(true)
                    .build();
            GcmNetworkManager.getInstance(context).schedule(oneoff);
            //Log.v("SendFilesTaskService", "oneoff task scheduled");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void cancelOneOff(Context context) {
        GcmNetworkManager
                .getInstance(context)
                .cancelTask(GCM_ONEOFF_TAG, SendFilesTaskService.class);
    }


}
