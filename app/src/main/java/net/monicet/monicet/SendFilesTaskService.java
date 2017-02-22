package net.monicet.monicet;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.OneoffTask;
import com.google.android.gms.gcm.Task;
import com.google.android.gms.gcm.TaskParams;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static android.R.attr.data;
import static android.R.attr.path;

/**
 * Created by ubuntu on 15-02-2017.
 */

public class SendFilesTaskService extends GcmTaskService {

    public static final String GCM_ONEOFF_TAG = "oneoff|[0,0]";
    public static final String FILES_PATH = "path";

    @Override
    public void onInitializeTasks() {
        super.onInitializeTasks();
//        When your package is removed or updated, all of its network tasks are cleared by the GcmNetworkManager.
//        You can override this method to reschedule them in the case of an updated package.
//        This is not called when your application is first installed.
//        This is called on your application's main thread. Schedule your tasks again here.
        // TODO: uncomment super. and .scheduleOne when deloying (commented now because restarts task every time)
//        super.onInitializeTasks();
//        // this is a GcmTaskService which is a Service which is a Context
//        SendFilesTaskService.scheduleOneOff(this);

    }

    @Override
    public int onRunTask(TaskParams taskParams) {
        //Bundle data = taskParams.getExtras();
        //String path = (String)data.getCharSequence(FILES_PATH);

        // test starts here
        File dir = new File(Utils.EXTERNAL_DIRECTORY);
        File testFile = new File(dir, "oneOff" + System.currentTimeMillis());
        try {
            testFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //test

        // test ends here

        // this is a GcmTaskService which is a Service which is a Context
        boolean successful = Utils.sendAndDeleteFiles(this);
        if (successful) { return GcmNetworkManager.RESULT_SUCCESS; }

        return GcmNetworkManager.RESULT_RESCHEDULE;
    }

    public static void scheduleOneOff(Context context) {

        //Bundle data = new Bundle();
        //data.putString(FILES_PATH, Utils.getInternalDirPathFromContext(context)); // Alex: this is not DRY

        try {
            OneoffTask oneOff = new OneoffTask.Builder()
                    //specify target service - must extend GcmTaskService
                    .setService(SendFilesTaskService.class)
                    //tag that is unique to this task (can be used to cancel task)
                    .setTag(GCM_ONEOFF_TAG)
                    .setExecutionWindow(0, 1)
                    //specify whether this task should be persisted across reboots
                    .setPersisted(true)
                    //.setRequiredNetwork(Task.NETWORK_STATE_ANY) // default NETWORK_STATE_CONNECTED
                    //request that charging must be connected, this line is optional
                    .setRequiresCharging(false)
                    //set some data we want to pass to our task
                    //.setExtras(data)
                    //if another task with same tag is already scheduled, replace it with this task
                    .setUpdateCurrent(true)
                    .build();
            GcmNetworkManager.getInstance(context).schedule(oneOff);
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
