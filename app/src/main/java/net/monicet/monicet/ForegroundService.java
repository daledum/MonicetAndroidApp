package net.monicet.monicet;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ForegroundService extends Service {

    //TODO: what's with this here, should I call super or what...see other code
    public ForegroundService() {
        //super();//?
    }

    //    @Override
//    public int onStartCommand(Intent intent, @IntDef(value = {Service.START_FLAG_REDELIVERY, Service.START_FLAG_RETRY}, flag = true) int flags, int startId) {
//        return super.onStartCommand(intent, flags, startId);
//    }

    //Called by the system every time a client explicitly starts the service by calling startService(Intent),
    // providing the arguments it supplied and a unique integer token representing the start request.
    // Do not call this method directly.
    // The system calls this on your service's main thread. A service's main thread is the same thread where UI operations take place for Activities running in the same process. You should always avoid stalling the main thread's event loop.
    // When doing long-running operations, network calls, or heavy disk I/O, you should kick off a new thread, or use AsyncTask

    //TODO: onStartCommand - be careful you don't start two different services when you stop your service with startService..maybe should use stopService

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //TODO:? do I need to call super.onStartCommand(intent, flags, startId);
        // onStartCommand() is called every time a client starts the service using startService(Intent intent).
        // This means that onStartCommand() can get called multiple times.
        // You should do the things in this method that are needed each time a client requests something from your service.
        // When you call startService(), if the service is not running, Android will create an instance of the service class
        // (this is a service object)
        // and will then call onCreate() on that object. It will then call onStartCommand() on that object.
        // If, some time later, you again call startService(), if the service is still running, Android
        // will not create a new service object.
        // Instead, it will just call onStartCommand() on the existing service object.

        switch (intent.getAction()) {//possible from Java 7 and upwards, uses .equals

            case Utils.START_FOREGROUND_SERVICE_FROM_ACTIVITY: {
                // It arrives here from the app every time the main activity is started
                // and the START button had already been pressed OR after the user started
                // the trip (pressed START), which comes after minGpsFixing.
                // We want to make sure, in case the notification (fgr service) was
                // killed (by the OS, low memory), that, when the activity is started again,
                // the service (and alarm, and thus the gps sampling) restart.
                // This will also take place even if the fgr service/notification is already running,
                // but everything will be overwritten, therefore no duplicate services/alarms.

                //TODO: test with two consecutive reboots (re-enabling an enabled component should be fine)
                // But do I want to enable it every time the activity starts?
                //If this doesn't work - add a boolean bundle extra with true if coming after START
                // Enable the boot completed receiver, which starts this service after the device restarts
                Utils.setComponentState(
                        this,
                        BootCompletedReceiver.class,
                        true
                );

                Bundle extras = intent.getExtras();
                long samplingInterval = extras.getLong(Utils.GPS_SAMPLING_INTERVAL);
                long tripDuration = extras.getLong(Utils.TRIP_DURATION);
                String userName = extras.getString(Utils.USERNAME);

                // Here, if this case is being re-run because it hadn't finished (system killed it
                // and the flag is start_redeliver_intent), I want to see if file was already created
                // Get the foreground route file
                File extensionlessFgrRouteFile = getExtensionlessFile(Utils.FOREGROUND_PREFIX);
                if (extensionlessFgrRouteFile == null) {
                    // The file does not exist (the file was never created, either because:
                    // 1 this is the first run, or
                    // 2 it tried but it did not succeed - see if filename == null, or
                    // 3 the system killed this case before it got to creating the file... or the
                    // file was deleted)

                    long startingTime = extras.getLong(Utils.TRIP_START_TIME);

                    // Try to create a file fgrMinMHoursHTimeId, without the extension
                    String fileName = createForegroundRouteFile(samplingInterval, tripDuration, startingTime);

                    if (fileName == null) {
                        // The foreground route file couldn't be created. Maybe after a reboot
                        // (boot completed receiver was enabled previously), or after another go of this case
                        // it will be able to create the file (If SEND is pressed the boot completed receiver is disabled anyway).

                        // Start the foreground service, but don't start the alarm (which starts the gps sampling intent service)
                        // the intent service won't have a file to write to
                        startForeground(Utils.NOTIFICATION_ID, getCompatNotification()); //or NOTIFICATION_ID ?
                    } else { // meaning the foreground route file was created
                        // Set and start the alarm and notification - this will overwrite old alarm
                        // and notification
                        // Alarms get killed on reboot, just like pendingIntents
                        // However, the alarm has to work after reboot (I will get a start service from bootcompletedreceiver)
                        //TODO: If you want a new interval for the alarm, cancel the old one
                        startAlarm(fileName, samplingInterval, tripDuration, userName);
                        // TODO: start foreground and build notification (maybe do this later, if file created successfully)
                        startForeground(Utils.NOTIFICATION_ID, getCompatNotification()); //or  Utils.FOREGROUND_ID?
                    }
                } else { //meaning the extensionless foreground route file already exists
                    String fileName = extensionlessFgrRouteFile.getName();
                    startAlarm(fileName, samplingInterval, tripDuration, userName);
                    startForeground(Utils.NOTIFICATION_ID, getCompatNotification()); //or  Utils.FOREGROUND_ID?
                }

                break;
            }

            //TODO: get rid of this case
//            case Utils.RESTART_FOREGROUND_SERVICE_FROM_ACTIVITY: {
//                // It arrives here every time the main activity is started and the START button had
//                // already been pressed.
//                // This case is here just to make sure, in case the notification (fgr service) was
//                // killed, that, when the activity is started again, the service (and alarm, and
//                // thus the gps sampling) restart.
//                // This will also take place even if the fgr service/notification is already running,
//                // but everything will be overwritten, therefore no duplicate services/alarms.
//                // Foreground route file is created here only if the START_FOREGROUND... case failed
//                // to create one
//
//                Bundle extras = intent.getExtras();
//                long samplingInterval = extras.getLong(Utils.GPS_SAMPLING_INTERVAL);
//                long tripDuration = extras.getLong(Utils.TRIP_DURATION);
//                String userName = extras.getString(Utils.USERNAME);
//
//                // get the foreground route file
//                File extensionlessFgrRouteFile = getExtensionlessFile(Utils.FOREGROUND_PREFIX);
//                if (extensionlessFgrRouteFile == null) {
//                    // The file does not exist (it was not created in the START_FOREGROUND...
//                    // case - write error, see above when I try to create the file OR it was deleted)
//
//                    long startingTime = extras.getLong(Utils.TRIP_START_TIME);
//                    // Try to create a file fgrMinMHoursHTimeId, without the extension
//                    String fileName = createForegroundRouteFile(samplingInterval, tripDuration, startingTime);
//                    if (fileName == null) {
//                        // The foreground route file couldn't be created this time, either. Maybe after a reboot
//                        // (boot completed receiver was enabled previously), or after RESTART_FOREGROUND...
//                        // it will be able to create the file (If SEND is pressed the boot completed receiver is disabled anyway).
//                        // start the foreground service, but don't start the alarm (which starts the gps sampling intent service)
//                        // the intent service won't have a file to write to
//                        startForeground(Utils.NOTIFICATION_ID, getCompatNotification()); //or NOTIFICATION_ID ?
//                    } else { // meaning the foreground route file was created
//                        // Set and start the alarm and notification - this will overwrite old alarm
//                        // and notification
//                        startAlarm(fileName, samplingInterval, tripDuration, userName);
//                        startForeground(Utils.NOTIFICATION_ID, getCompatNotification()); //or  Utils.FOREGROUND_ID?
//                    }
//                } else { //meaning the extensionless foreground route file already exists
//                    String fileName = extensionlessFgrRouteFile.getName();
//                    startAlarm(fileName, samplingInterval, tripDuration, userName);
//                    startForeground(Utils.NOTIFICATION_ID, getCompatNotification()); //or  Utils.FOREGROUND_ID?
//                }
//
//                break;
//            }

            case Utils.START_FOREGROUND_SERVICE_FROM_BOOT_RECEIVER: {

                File extensionlessFgrRouteFile = getExtensionlessFile(Utils.FOREGROUND_PREFIX);
                if (extensionlessFgrRouteFile == null) {
                    // The file does not exist (it was not created - write error, see above when I try to create the file OR it was deleted)

                    // File that contains the word TRIP and has no extension
                    // All of this is happening right after restart, so, the Monicet activity is not (should not be running)...
                    // Maybe write this data to the route file and open that up.
                    // When the Monicet app starts and sees an extensionless tempTrip file, it deletes it //TODO: work on this NB
                    // tempTrip files cannot have an extension, anyway. Only finishedTrip files can (but don't always have)
                    File extensionlessTripFile = getExtensionlessFile(Utils.TRIP);
                    if (extensionlessTripFile == null) {
                        // no trip file without an extension, I don't know what interval to set for the alarm, to what to tie
                        // the foreground file to (in case I'll create it), so just stop boot receiver and stop service
                        Utils.setComponentState(
                                this,
                                BootCompletedReceiver.class,
                                false
                        );
                        stopSelf();
                    } else {// extensionlessTripFile exists
                        // Dejsonize it temporarily (nulify trip after). Take the gps user values for the alarm interval
                        // Also take the trip duration and the ID out (used for creating the fgr file)
                        try {
                            JsonReader reader = new JsonReader(new FileReader(extensionlessTripFile));
                            Trip trip = new Gson().fromJson(reader, Trip.class);
                            try {
                                reader.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            GpsMode gpsMode = trip.getGpsMode();

                            // The file tempTrip file can be created after the minimum gps fixing (5 method calls)
                            // So, it could have been created before the pressing of the START button
                            // We don't want this service to run before the pressing of the START button
                            boolean wasStartButtonPressed = false;
                            if (trip.getStartTimeAndPlace().getTimeInMillis() != Utils.INITIAL_VALUE) {
                                wasStartButtonPressed = true;
                            }

                            if (gpsMode == null || !wasStartButtonPressed) {
                                // this means that there device is not
                                // allowed or cannot access the GPS (app will not save to file if this happens anyway)
                                // OR the START button was not pressed
                                Utils.setComponentState(
                                        this,
                                        BootCompletedReceiver.class,
                                        false
                                );
                                stopSelf();
                            } else {// this means the device can sample gps coordinates and that the START button was pressed
                                long samplingInterval = gpsMode.getIntervalInMillis();
                                long tripDuration = trip.getDuration();
                                long startingTime = trip.getId();
                                String userName = trip.getUserName();


                                String fileName = createForegroundRouteFile(
                                        samplingInterval,
                                        tripDuration,
                                        startingTime
                                );

                                if (fileName != null) {
                                    startAlarm(fileName, samplingInterval, tripDuration, userName);

                                    // TODO: start foreground and build notification
                                    startForeground(Utils.NOTIFICATION_ID, getCompatNotification());// or Notification id

                                } else { // The foreground route file couldn't be created.
                                    Utils.setComponentState(
                                            this,
                                            BootCompletedReceiver.class,
                                            false
                                    );
                                    stopSelf();
                                }
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();

                            Utils.setComponentState(
                                    this,
                                    BootCompletedReceiver.class,
                                    false
                            );
                            stopSelf();
                        }
                    }
                } else {// extensionlessFgrRouteFile exists
                    String fileName = extensionlessFgrRouteFile.getName();

                    //TODO: read first part (up to ",") of first line of file
                    String userName = null;
                    try {
                        BufferedReader in = new BufferedReader(new FileReader("foo.in"));

                        try {
                            String lineContainingUserName = in.readLine();
                            // take the user's email address, which is written to the file at the beginning
                            int indexOfComma = lineContainingUserName.indexOf(",");
                            if (indexOfComma != -1) {
                                userName = lineContainingUserName.substring(0, indexOfComma).trim();
                                if (!userName.contains("@")) {//it's not a valid user if it doesn't have @
                                    userName = null;
                                }
                            }
                        } catch(IOException e) {
                            e.printStackTrace();
                        }

                        try {
                            in.close();
                        } catch(IOException ex) {
                            ex.printStackTrace();
                        }
                    } catch(FileNotFoundException exc) {
                        exc.printStackTrace();
                    }

                    // could not extract the user name from the file, therefore we'll use the
                    // current user name of the device running the service now
                    if (userName == null) {
                        userName = Utils.getUserCredentials(this).trim();
                    }

                    // start the alarm
                    startAlarm(fileName,
                            getIntervalMillisFromFileName(fileName),
                            getDurationMillisFromFileName(fileName),
                            userName
                    );

                    // TODO: start foreground and build notification
                    startForeground(Utils.NOTIFICATION_ID, getCompatNotification());//or notif id
                }

                break;
            }

            case Utils.STOP_FOREGROUND_SERVICE: {
                // I am using this action so I could differentiate between the moments my app asks for this Service to stop
                // and when the OS kills this Service (if using stopService() from Activity, onDestroy() gets called,
                // but onDestroy could get called when the OS is killing the Service)

                // Cancel the alarm
                // Create the same intent (extras are not taken into consideration),
                // and thus a matching PendingIntent/IntentSender, for the one that was scheduled.
                AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);//was Context.ALARM_SERVICE
                alarmMgr.cancel(getAlarmPendingIntent(null, 0, null));// here it sends null as the filename

                // also disable the boot completed receiver, so it doesn't start this service at start-up (reboot/restart)
                Utils.setComponentState(
                        this,
                        BootCompletedReceiver.class,
                        false
                );

                // stop the foreground (and disable notification)
                stopForeground(true);

                // this needs to be done after alarm is stopped (file should not be written to at the time)
                // IntentServices are difficult to interrupt.
                // The IntentService only writes to file at the end, so, this might not try to rename the file
                // while it's being written to
                if (intent.getExtras().getBoolean(Utils.ADD_EXTENSION)) {
                    // Add extension to file
                    // The extension is added when Service ends...after SEND is pressed
                    File extensionlessFgrRouteFile = getExtensionlessFile(Utils.FOREGROUND_PREFIX);
                    if (extensionlessFgrRouteFile != null) {
                        String fileName = extensionlessFgrRouteFile.getName();
                        //File directory = new File(Utils.getDirectory());
                        File directory = new File(Utils.getDirectory(this));
                        if (!extensionlessFgrRouteFile.renameTo(
                                new File(directory, fileName + AllowedFileExtension.CSV))) {
                            // maybe the file was written to by the Gps IntentService, so, wait a second
                            try {
                                Thread.sleep(Utils.ONE_SECOND_IN_MILLIS);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } finally {
                                extensionlessFgrRouteFile.renameTo(
                                        new File(directory, fileName + AllowedFileExtension.CSV));
                            }
                        }
                    }
                } // no need for else, here, if trip is being deleted saveAndFinish will delete the fgr Route file, too

                //void stopForeground (int flags)
                //stopSelf() is used to always stop the current service.
                //stopSelf(int startId) is also used to stop the current service, but only if startId was the ID specified the last time the service was started.
                //stopService(Intent service) is used to stop services, but from outside the service to be stopped.  If the service is not running, nothing happens. Otherwise it is stopped. Note that calls to startService() are not counted - this stops the service no matter how many times it was started.
                //onDestroy() state called for all 3
                //stopService(new Intent(SummaryActivity.this, SocketService.class));
                //AppController.getInstance().stopService();
                stopSelf();

                break;
            }

            case Utils.STOP_GPS_ALARM_INTENT_SERVICE: {
                AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);//was Context.ALARM_SERVICE
                alarmMgr.cancel(getAlarmPendingIntent(null, 0, null));

                Utils.setComponentState(
                        this,
                        BootCompletedReceiver.class,
                        false
                );

                break;
            }

        }

        return START_REDELIVER_INTENT; //was START_STICKY;
        // useful for services that are receiving commands of work to do, and want to make sure
        // they do eventually complete the work for each command sent
    }

    protected PendingIntent getAlarmPendingIntent(String fileName, long tripDuration, String userName) {
        // TODO: only works with local storage ? - see manifest
        // Set the Intent (even when creating a new one, comparison will be done using filterEquals)
        Intent intent = new Intent(this, GpsAlarmIntentService.class);

        // alarm receiver reads the duration and startingTime from the fileName
        // filename is null only when we want a pending intent that will cancel the alarm
        if (fileName != null) {
            intent.putExtra(Utils.FILENAME, fileName);
            intent.putExtra(Utils.TRIP_DURATION, tripDuration);
            intent.putExtra(Utils.USERNAME, userName);
        }

        // Returns an existing or new PendingIntent (if it wasn't previously created) matching the given parameters
        // If you just want to change the extras without actually rescheduling the existing alarm,
        // that is what FLAG_UPDATE_CURRENT is for. If you want to reschedule, don't use any flags at all.
        // Flag indicating that if the described PendingIntent already exists,
        // then keep it but replace its extra data with what is in this new Intent.TODO: test that the receiver is getting the fileName

        return PendingIntent.getService(this, Utils.GPS_SAMPLING_ALARM_REQUEST_CODE, intent, 0);////PendingIntent.FLAG_UPDATE_CURRENT //? TODO: also, test that it wakes up the phone to sample
    }

    protected String createForegroundRouteFile(long samplingInterval, long tripDuration, long startingTime) {

        //File directory = new File(Utils.getDirectory());// set directory method uses the external directory
        File directory = new File(Utils.getDirectory(this));

        if (!directory.exists()) {
            directory.mkdirs();
        } // only for external?

        File fgrRouteFile = new File(directory,
                Utils.FOREGROUND_PREFIX +
                        Utils.MINUTES + String.valueOf(samplingInterval / Utils.ONE_MINUTE_IN_MILLIS) +
                        Utils.HOURS + String.valueOf(tripDuration / Utils.ONE_HOUR_IN_MILLIS) +
                        Utils.TIME + String.valueOf(startingTime)
        );

        try {//try to create the file (getPath(), getName(), getAbsolutePath() ?canonical too)
            if(fgrRouteFile.createNewFile()) {
                return fgrRouteFile.getName();
            }
        } catch (IOException e) {
            Log.d("UtilsStartFgrService", "IOException creating the fgr route file");
        }
        return null;
    }

    protected File getExtensionlessFile(final String type) {
        //File directory = new File(Utils.getDirectory());// set directory method uses the external directory
        File directory = new File(Utils.getDirectory(this));

        // Array of files (max should be 1 element) for files and directories in this directory
        // which contain the word Utils.WHATEVER and have no extension
        if (directory.exists()) {
            File[] files = directory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    String filename = pathname.getName().toLowerCase();

                    if (filename.contains(type.toLowerCase()) &&
                            !filename.contains(".")) {//Specify which extension? What if it get's changed later?
                        return true;
                    }

                    return false;
                }
            });

            if (files.length > 0) {
                if (files.length > 1) {
                    // test purposes only
                    Log.d("FgrService", "You should not have more than one extensionless foreground file in your directory");
                }
                return files[0];
            }
        }

        return null;
    }

    protected void startAlarm(String fileName, long samplingInterval,
                              long tripDuration, String userName) {

        // set the alarm to sample the first time 'interval' millis from now. The alarm receiver
        // samples for a minute, so set the interval between alarms as 'interval' + 1 minute
        AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmMgr.setInexactRepeating(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,// will awake the device
                SystemClock.elapsedRealtime() + samplingInterval,// first time the alarm should go off (it went off immediately, nonetheless)
                samplingInterval + Utils.ONE_MINUTE_IN_MILLIS,// interval in milliseconds between subsequent repeats of the alarm
                // sending the args to the alarm recipient (gps alarm intent service) as extra (it doesn't need the sampling interval, only the alarms needs it)
                getAlarmPendingIntent(fileName, tripDuration, userName)
        );
    }

    protected Notification getCompatNotification() {

        Intent openTripNotificationIntent = new Intent(this, MainActivity.class);
        //openTripNotificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent openTripPendingIntent = PendingIntent.getActivity(this,
                Utils.OPEN_TRIP_REQUEST_CODE, openTripNotificationIntent, 0);
        //The default behaviour (setting no flags, ie: 0 as the flags parameter) is to return
        // an existing PendingIntent if there is one that matches the parameters provided.
        // If there is no existing matching PendingIntent then a new one will be created and returned.

        //TODO: get rid of this
        Intent deleteTripNotificationIntent = new Intent(this, MainActivity.class);
        deleteTripNotificationIntent.putExtra(Utils.DELETE_TRIP, true);
        // this is a different pending intent, with a different request code
        PendingIntent deleteTripPendingIntent = PendingIntent.getActivity(this,
                Utils.DELETE_TRIP_REQUEST_CODE, deleteTripNotificationIntent, 0);//PendingIntent.FLAG_UPDATE_CURRENT
        //Flag indicating that if the described PendingIntent already exists, then keep it but
        // replace its extra data with what is in this new Intent.

        // For opening or creating a new instance of the activity
        // intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);//override onNewIntent method in MainActivity.
        // You will get intent "init" which will passed as a parameter to onNewIntent method.

        // If you want to create a new instance and close the current instance you need to set
        // Intent.FLAG_ACTIVITY_CLEAR_TOP.
        // If you want to reuse the same instance of the activity in this case you need to set
        // both Intent.FLAG_ACTIVITY_CLEAR_TOP and Intent.FLAG_ACTIVITY_SINGLE_TOP after that it
        // will not create another instance of the activity but call onNewIntent() of activity to run the new Intent.

        return new NotificationCompat.Builder(this)//TODO: change these (also maybe get rid of Compat 4.1 and up anyway)
                .setContentTitle(getText(R.string.app_name))// was getText(R.string.notification_title)
                .setContentText(getText(R.string.trip_was_started))
                .setSmallIcon(R.mipmap.ic_launcher_whale_tail_blue)//TODO: this needs to be a special status bar icon (transparent, and filled with white) material design
                .setContentIntent(openTripPendingIntent)
                //.addAction(R.drawable.ic_open, getText(R.string.open_trip), openTripPendingIntent)//this appears only when app is not visible
                //.addAction(R.drawable.ic_delete, getText(R.string.delete_trip), deleteTripPendingIntent)
                .build();
    }
//    @Override
//    public void onDestroy() {
//        // This might be called if service is destroyed by the OS, too
//        //You may or may not get called with onDestroy() as part of this, depending on how urgent the need is for your process' system RAM.
//        //If, from onStartCommand(), you returned START_STICKY or START_REDELIVER_INTENT, your process
//        // and service is supposed to be restarted automatically at some time in the not-too-distant future,
//        // when RAM needs permit it.
//    }

    // Extract the sampling interval (for setting the alarm)
    protected long getIntervalMillisFromFileName(String fileName) {

        // extract it from the fileName, which has the form:
//            Utils.FOREGROUND_PREFIX +
//            Utils.MINUTES + String.valueOf(samplingInterval) +
//            Utils.HOURS + String.valueOf(tripDuration) +
//            Utils.TIME + String.valueOf(startingTime)

        int beginIndex = fileName.indexOf(Utils.MINUTES) + Utils.MINUTES.length();
        int endIndex = fileName.indexOf(Utils.HOURS);

        // Then I multiply that with ONE_MINUTE
        return Long.parseLong(fileName.substring(beginIndex, endIndex)) * Utils.ONE_MINUTE_IN_MILLIS;
    }

    // Extract the approx duration of the trip interval (for setting the alarm)
    protected long getDurationMillisFromFileName(String fileName) {

        int beginIndex = fileName.indexOf(Utils.HOURS) + Utils.HOURS.length();
        int endIndex = fileName.indexOf(Utils.TIME);
        return Long.parseLong(fileName.substring(beginIndex, endIndex)) * Utils.ONE_HOUR_IN_MILLIS;

        // for ID (or trip starting time):
//        beginIndex = fileName.indexOf(Utils.TIME) + Utils.TIME.length();
//        long time = Long.parseLong(fileName.substring(beginIndex));
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }
}
