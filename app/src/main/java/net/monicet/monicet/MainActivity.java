package net.monicet.monicet;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import static android.R.attr.value;
import static android.R.string.no;

public class MainActivity extends AppCompatActivity implements MainActivityInterface {

    // declaring trip as a class field made the app not start.. why?
    //final Trip trip = new Trip(buildNewSightingFromResources());
    final Trip trip = new Trip();
    final Sighting[] openedSightings = new Sighting[1]; // artifact so I can use it inside anonymous classes
    final ArrayList<Animal> seedAnimals = new ArrayList<Animal>();


    // Declare and initialize the receiver dynamically // TODO: maybe this should be done in a singleton, application level
    final BroadcastReceiver dynamicReceiver = new DynamicNetworkStateReceiver(); // or declare the class here, occupying more space

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
//        int errorCheck = api.isGooglePlayServicesAvailable(this);
//        if(errorCheck == ConnectionResult.SUCCESS) {
//            //google play services available, hooray
//        } else if(api.isUserResolvableError(errorCheck)) {
//            //GPS_REQUEST_CODE = 1000, and is used in onActivityResult
//            api.showErrorDialogFragment(this, errorCheck, GPS_REQUEST_CODE);
//            //stop our activity initialization code
//            return;
//        } else {
//            //GPS not available, user cannot resolve this error
//            //todo: somehow inform user or fallback to different method, this relates to the GCM too
//            //stop our activity initialization code
//            return;
//        }

        // before registering the dynamic receiver, which will trigger - inform the package where you saved the files
        // set directory here for the SendAndDeleteFiles Utils method
        Utils.setDirectory(Utils.EXTERNAL_DIRECTORY);
        //should be Utils.setDirectory(getFilesDir().toString()); // Alex: was, directory.toString(), toString should be optional
        // step III ends here

        // Register the dynamic receiver. Once registered it's enabled by default,
        // therefore it could fire on connectivity change before SEND.
        // android.net.conn.CONNECTIVITY_CHANGE is a sticky broadcast, so, the receiver fires when registered
        // in order to stop that, do work in onReceive only if if (!isInitialStickyBroadcast())
        // All the "file sending" receivers are enabled by default and after pressing SEND.
        // Disabling them here (start of onCreate), would stop them from working while the app
        // is alive (not forcefully closed by the user/system). So, let them do their thing.
        // They disable themselves if the work is done/or if there is no work to be done (except
        // the dynamic receiver, which lives and tries to send the files throughout the lifetime of the app)
//        IntentFilter filter = new IntentFilter();//TODO: maybe register it in onResume or onStart
//        filter.addAction(Utils.INTENT_CONNECTION_ACTION);
//        filter.addAction(Utils.START_ACTION);
//        getApplicationContext().registerReceiver(dynamicReceiver, filter); // application lifetime or just activity

        // TODO: check after installing the app if dynamic receiver runs even after restart
        // (bad, that means it's not unregistering), when the app is not running
        //check for receiver on monicet package
//        adb shell dumpsys activity broadcasts or
//        dumpsys activity -h
//        dumpsys activity b
//        dumpsys package net.monicet.monicet
        // Step 1 starts here:
        // using the data from resources (containing specie names, photos and description),
        // create the first Sighting, then instantiate a Trip (which will contain one sighting - not anymore)
        // and feed its array of animals to the custom ListView ArrayAdapter
        // Create the custom ArrayAdapter and populate it

        //this will create a trip with one sighting, Alex: trip was declared and initialized here
        //can move outside 'global', also, if I'm not using anonymous inners, can drop the final
        //final Trip trip = new Trip(buildNewSightingFromResources());//TODO: change constructor of trip() and make global

        // Initialization steps: not done in the constructor because they might not be kept in future versions
        // set label to MONICET - START TRIP
        setTitle(getText(R.string.app_name) + " - " + getText(R.string.start_trip));
        // TODO: start the GPS so that it's calibrated when you first sample (start button pressed)
        // set the mode to SLOW (before this, it was OFF, trip is constructed with OFF)
        trip.setGpsMode(GpsMode.SLOW); // TODO: setGpsMode should calibrate the google location services gps - should be connected
        // Initialization steps end here

        // TODO: should these be set in the xml initially
        // should be invisible in xml, made visible here (before implementing the play button)
        //fabAdd.setVisibility(View.INVISIBLE);
        //fabSend.setVisibility(View.INVISIBLE);
        // Save button should be invisible in xml and made visible

        // TODO: get rid of all these assignment and just use findViewById, so that you can split in methods?
        // if coming back from a config change - I should first check if they are visible
        final FloatingActionButton fabStart = (FloatingActionButton) findViewById(R.id.fab_start);
        final FloatingActionButton fabSave = (FloatingActionButton) findViewById(R.id.fab_save);
        final FloatingActionButton fabAdd = (FloatingActionButton) findViewById(R.id.fab_add);
        final FloatingActionButton fabSend = (FloatingActionButton) findViewById(R.id.fab_send);
        final FloatingActionButton fabBack = (FloatingActionButton) findViewById(R.id.fab_back);
        final CheckBox checkboxTrackingGps = (CheckBox) findViewById(R.id.checkBox_tracking_gpsmode);
        final ListView listViewSightings = (ListView) findViewById(R.id.list_view_sightings);
        final ListView listViewAnimals = (ListView) findViewById(R.id.list_view_animals);

        // populate the seed animals from resources
        buildSeedAnimalsFromResources();
        // TODO: give it the gpsmodeuserinput.. it's a reference, but give it null in place of animals, also change AnimalAdapter to deal with null
        final AnimalAdapter animalAdapter = new AnimalAdapter(this, seedAnimals);
        // or maybe give it null
//        final AnimalAdapter animalAdapter = new AnimalAdapter(
//                this,
//                new ArrayList<Animal>(Arrays.asList(new Animal[]{null}))
//        );
        // set the adapter
        listViewAnimals.setAdapter(animalAdapter);

        final SightingAdapter sightingAdapter = new SightingAdapter(
                this,
                new ArrayList<Sighting>(Arrays.asList(new Sighting[]{null})),
                animalAdapter
        );
        // set the adapter
        listViewSightings.setAdapter(sightingAdapter);
        // Step 1 ends here

        //Step 2 starts here:
        // START floating action button
        fabStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // a) when pressed takes the value from the tracking gpsmode checkbox
                // (if selected set gpsmode to continuous)
                if (checkboxTrackingGps.isChecked()) {
                    trip.setGpsMode(GpsMode.CONTINUOUS);
                    // TODO: GPS this should trigger continuous gps
                }

                // b) TODO: get username and set it
                // trip.setUserName();

                // c) the gps was already started (when app was started): take gps sample, date and time
                // TODO: started (see above) with a gps mode SLOW, so that you can sample immediately, then turn it to 'really slow'?
                // TODO: remember to set the mode to continuous if the checkbox was checked
                //trip.setGpsMode(GpsMode.FAST); //? this before sampling
                //trip.setGpsMode(GpsMode.SLOW); //? this after sampling
                trip.getStartTimeAndPlace().setTimeInMillis(System.currentTimeMillis());
                //trip.getStartTimeAndPlace().setLatitude();
                //trip.getEndTimeAndPlace().setLongitude();

                // deal with the views
                showSightings(sightingAdapter); // shared between the START, SAVE, BACK and DELETE buttons
            }
        });
        //Step 2 ends here

        // Step 3 starts here:
        // SAVE floating action button
        // Check first if this Sighting has at least one non-empty Animal (quantity different to 0)
        // If it is non-empty, open Comments dialogFragment
        // Else (all quantities are 0): put a toast on the screen (and do nothing), with the message:
        //"your trip has no sightings/animals. There is nothing to send. Please add the quantity of individuals that you've seen.
        // Or no species were seen/No animals were seen during this sighting. There is nothing to save."


        fabBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // maybe move ADD logic to SAVE logic, so here, we do nothing,
                // like we should, see SAVE button logic for more info

                // openedSighting should no longer point to our unsaved (if coming from ADD)
                // or opened sighting (if coming from CLICK on sight)
                // shared by the SAVE and BACK button
                // call this before removing the unsaved sighting, so, openedSighting doesn't point to it
                // hopefully, the unsaved sighting will be GC-ed soon
                disconnectOpenedSighting();

                // check that the most recent sighting has an Animal
                // we are here after ADD or CLICK on a sighting logic, therefore at least a sighting exists
                if (trip.getLastCreatedSighting().getAnimal() == null) {
                    // in this case, we arrived here after ADD (newly created, animal-less sighting
                    // so, remove this sighting from this trip
                    trip.getSightings().remove(trip.getLastCreatedSighting());
                }
                // else - we're coming here from CLICK on Sight, we are not touching that already existing sighting

                stopFastStartSlowGps(); // shared between the BACK and SAVE buttons
                // prepare views - hide and show what's needed
                showSightings(sightingAdapter); // shared between the START, SAVE, BACK and DELETE buttons
            }
        });

        // saving is done inside the animal adapter and inside the sighting adapter
        fabSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // read the seedAnimals that have just been changed by the AnimalAdapter
                int numberOfSeenSpecies = 0;
                // stop when you've found quantities different from zero at 2 different species
                // or stop at the end, when you've found zero quantities different from zero from all the species
                Animal animalToInsertInSighting = null;

                for (Animal seedAnimal : seedAnimals) {
                    if (seedAnimal.getStartQuantity() != 0) {
                        // if it found an animal with the quantity different from 0, assign it to our animal
                        animalToInsertInSighting = seedAnimal;
                        numberOfSeenSpecies++;
                        if (numberOfSeenSpecies > 1) { break; }
                    }
                }

                if (numberOfSeenSpecies == 0) {
                    // no animal had the quantity different from 0 (in this case our animal to be inserted is still null)
                    Toast.makeText(
                            MainActivity.this,
                            R.string.no_animals_to_save_message,
                            Toast.LENGTH_LONG
                    ).show();
                } else {
                    // more than one animal had the quantity different from 0 (our animal to be inserted is the second animal found)
                    if (numberOfSeenSpecies == 2) {
                        Toast.makeText(
                                MainActivity.this,
                                R.string.only_one_animal_message,
                                Toast.LENGTH_LONG
                        ).show();
                    } else { // everything OK, only one animal was selected

                        // TODO: maybe move SAVE logic here? See below why not:
                        // ADD adds time and gps data after having been pressed.
                        // I would have to use a temp sighting, that would complicate things even further:
                        //I just added time and gps (in ADD) to the temp sighting. so it has them.
                        //If temp has time (gps might take a while), that means I just came from ADD.
                        //Now, when saving, I want to add a new sighting to the trip - a clone of
                        //seed, if coming from ADD, of course...temp sighting needs to be global
                        // and final (array hack again).
                        //add sighting (new sighting(seedSighting))
                        //Clear all data from temp sighting on SAVE and BACK. Null animal and all zero.
                        //I was in ADD, I added time and gps to the temp sighting
                        //otherwise (opened sighting is not the temp sighting), here (in save) just
                        //save the opened sighting - coming from click on a sighting

                        stopFastStartSlowGps(); // shared between the BACK and SAVE buttons

                        // insert animal into opened Sighting, setAnimal() calls new Animal
                        openedSightings[0].setAnimal(animalToInsertInSighting);

                        // SAVE no longer works on the sighting, so openedSighting does not need to connect to it anymore
                        // SAVE and BACK share this
                        disconnectOpenedSighting();

                        // saved successfully message
                        Toast.makeText(MainActivity.this,
                                R.string.sighting_saved_confirmation_message, Toast.LENGTH_SHORT).show();
//                        Toast.makeText(activity,
//                                R.string.sighting_saved_instructions_message, Toast.LENGTH_LONG).show();

                        // show and hide the appropriate views
                        showSightings(sightingAdapter); // shared among the START, SAVE, BACK and DELETE buttons

//                        // update sighting adapter Alex
//                        sightingAdapter.clear();
//                        sightingAdapter.addAll(trip.getSightings());
//                        sightingAdapter.notifyDataSetChanged();
                    }
                }
            }
        });
        // Step 3 ends here

        //Step 4 starts here:
        // ADD + button logic
        fabAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // create a sighting here and now
                // TODO: This is needed because we want to save time and gps to it from when ADD was pressed
                trip.getSightings().add(new Sighting());

                // a sighting was created above, so the trip will have at least one

                // TODO: this is connected only to the initial value?.. it should be ok, a reference? enum ..TEST
                // start sampling GPS data more often (fast/quickly)
                if (trip.getGpsMode() != GpsMode.CONTINUOUS) {
                    if (trip.getGpsMode() != GpsMode.FAST) {
                        trip.setGpsMode(GpsMode.FAST);
                        // TODO: GPS this should actually change the sampling rate (via a View listener?)
                    }
                }
                // TODO: give it a few seconds for the gps to start up?
                // TODO: sample (and save Sighting instance start GPS, date and time)
                //trip.getLastCreatedSighting().setLatitude();
                //trip.getLastCreatedSighting().setLongitude();
                trip.getLastCreatedSighting().getStartTimeAndPlace().setTimeInMillis(System.currentTimeMillis());
                //TODO: later, slow down the gps sampling

                // and link the openedSighting to it (most recently added sighting),
                // so that the save button knows where to save
                // Alex: this is hiding the animals list view
                openSighting(
                        getText(R.string.app_name) + " - " + getText(R.string.add_sighting),
                        trip.getLastCreatedSighting(),
                        animalAdapter
                );
            }
        });
        //Step 4 ends here

        //Step 5 starts here:
        // SEND button logic
        fabSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (trip.getNumberOfSightings() == 0) {
                    Toast.makeText(
                            MainActivity.this,
                            R.string.no_sighting_to_send_message,
                            Toast.LENGTH_LONG
                    ).show();

                } else {
                    // TODO: step I - Sample GPS, Date, Time and save Trip instance end_gps, end_date/time
                    //trip.setEndLatitude();
                    //trip.setEndLongitude()
                    trip.getEndTimeAndPlace().setTimeInMillis(System.currentTimeMillis());
                    // step I ends here

                    // Alex: android:installLocation is internalOnly
                    // TODO:  Give the files good names (XXXX is the time, or user or trip number etc.)
                    // TODO: write the json and csv files internally
                    // TODO: try to send them via a http post request to a server... if successful, delete the files, if not don't delete the files
                    // TODO: maybe just create the files and leave all the rest to the service (so that the 2 don't step on each other's toes)
                    // TODO: create that server page (page will display all non-empty sightings for the trip, and also create a kml with the csv)
                    // TODO: create that service that runs continuously, goes to the Monicet folder and sends all the json and csv files to the server (if successful, deletes them)
                    // TODO: be careful so that both the service and the send button try to send the data to the same place (DRY)

                    // step II - create the files to be sent
                    try {

                        // test external storage version
                        File directory = new File(Utils.getDirectory());
                        //File directory = new File(Environment.getExternalStorageDirectory(), "Monicet");
                        //File directory = new File(Utils.EXTERNAL_DIRECTORY);
                        if (!directory.exists()) {
                            directory.mkdirs();
                        } // only for external, TODO: remove this?
                        //deployment - use internal storage
                        //File directory = new File(getFilesDir().toString());

                        //test - show files in directory
//                final File d = new File(Utils.INTERNAL_DIRECTORY);
//                File[] files = d.listFiles();
//                for (File file: files) {
//                    Toast.makeText(getApplicationContext(), "After:" + file.getName(), Toast.LENGTH_SHORT).show();
//                }
                        // end test

                        String routePrefix = "route";
                        String tripPrefix = "trip";

                        String tripFileTitle = tripPrefix + System.currentTimeMillis();
                        String tripFileName = tripFileTitle + AllowedFileExtension.JSON;
                        trip.setTripFileName(tripFileName);

                        if (trip.getGpsMode() == GpsMode.CONTINUOUS) {

                            String routeFileTitle = routePrefix + System.currentTimeMillis();
                            String routeFileName = routeFileTitle + AllowedFileExtension.CSV;
                            trip.setRouteFileName(routeFileName); // this will be written to the JSON file
                            File routeFile = new File(directory, routeFileTitle);
                            FileWriter routeWriter = new FileWriter(routeFile);
                            routeWriter.append(trip.getUserName());
                            routeWriter.append(",");
                            routeWriter.append(tripFileName);
                            routeWriter.append(",");
                            routeWriter.append(routeFileName);
                            routeWriter.append("\r\n"); //routeWriter.append(System.getProperty("line.separator"));

                            for (Map.Entry<Long, double[]> entry : trip.getContinuousData().entrySet()) {
                                double[] coords = entry.getValue();
                                routeWriter.append(entry.getKey().toString());
                                routeWriter.append(",");
                                routeWriter.append("" + coords[0]);
                                routeWriter.append(",");
                                routeWriter.append("" + coords[1]);
                                routeWriter.append("\r\n"); //routeWriter.append(System.getProperty("line.separator"));
                            }
                            routeWriter.flush(); // Alex: redundant?
                            routeWriter.close();
                            // add the extension at the end, so that the broadcast receiver doesn't
                            // try to sent it before we're finished with the file
                            routeFile.renameTo(new File(directory, routeFileName));
                        }

                        Gson gson = new GsonBuilder().create();
                        File tripFile = new File(directory, tripFileTitle);
                        FileWriter tripWriter = new FileWriter(tripFile);
                        // get rid of the empty animals ?? TODO: change this
                        // jasonize the trip
                        tripWriter.append(gson.toJson(trip));
                        tripWriter.flush(); // Alex: redundant?
                        tripWriter.close();
                        // add the extension at the end, so that the broadcast receiver doesn't try to
                        // sent it before we've finished with the file
                        tripFile.renameTo(new File(directory, tripFileName));

                        // test
//                    File[] files2 = directory.listFiles();
//                    for (File file: files2) {
//                        Toast.makeText(getApplicationContext(), "Before:" + file.getName(), Toast.LENGTH_SHORT).show();
//                    }
                        //test
                        //test - delete internal dir
//                    FileFilter fileFilter = new FileFilter() {
//                        @Override
//                        public boolean accept(File pathname) {
//                            return false;
//                        }
//                    };
//                    File[] fs = d.listFiles(fileFilter);
//                    for (File f: fs) {
//                        f.delete();
//                    }
                        //test

                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "file exception", Toast.LENGTH_SHORT).show(); // Alex: remove this
                    }
                    // step II ends here

                    // step III - enable and start mechanisms designed to send (and delete if sent)
                    // those files via the Internet (they all use the Utils method SendAndDeleteFiles,
                    // which first checks for a live Internet connection)
                    // This is the right moment (after SEND) for enabling them, because they've disabled
                    // themselves by now (if folder doesn't contain json or csv files aka empty...
                    // although it always has one instant-run file)

                    // first - use GCM Network Manager. I updates itself and stops when folder is empty
                    // (setPersisted and updateCurrent are true). Sends the files within a minute when connected.
                    // maybe pass it the application context - it only uses it for the path anyways
                    // MainActivity.this.getApplicationContext(); //getApplication().getBaseContext();
                    // TODO: new Thread here or try retrofit inside the sendAndDeleteFiles method ? uses a different thread?
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            SendFilesTaskService.scheduleOneOff(MainActivity.this);//maybe the thread needs to be inside scheduleOneOff
                        }
                    });

                    // secondly, use broadcast receivers
                    // a) Alarm Manager receiver: starts an hourly alarm after BOOT COMPLETED or START_ACTION
                    // which starts the receiver, which sends and deletes files.
                    // It disables itself if folder is empty. It doesn't listen to the network
                    // i) enable it
//                Utils.setComponentState(
//                        MainActivity.this,
//                        FilesAndBootReceiver.class,
//                        true
//                );
//                // ii) fire it:
//                Intent startIntent = new Intent(Utils.START_ACTION);
//                MainActivity.this.sendBroadcast(startIntent);

                    // b) dynamicReceiver: a dynamically created broadcast receiver to listen to the network change
                    // i) enable it: don't touch. It's an error prone mechanism. It was already registered.
                    // ii) fire it, so that it tries to send the files now (before a connection
                    // change occurs, in case the phone is already connected)
                    // it's fired above (the alarm and this dynamic receiver are both fired by START_ACTION)

                    // c) a static receiver (defined in xml, only working pre API 24 - Nougat)
                    // is deployed to listen to the network. It disables itself if the folder is empty
                    // i) enable it
//                Utils.setComponentState(
//                        MainActivity.this,
//                        StaticNetworkStateReceiver.class,
//                        true
//                );
                    // ii) fire it: no need to fire this one right know, because
                    // the dynamic receiver deals with the present moment

                    //step III ends here

                    // TODO: Then turn off the GPS service
                    trip.setGpsMode(GpsMode.OFF);
                    // also, actually turn the gps off

                    // Final point - Then stop the application. *make sure you finish it off. Test the order.
                    // http://stackoverflow.com/questions/10847526/what-exactly-activity-finish-method-is-doing
                    // returning to this app from Gmail ?
                    // http://stackoverflow.com/questions/2197741/how-can-i-send-emails-from-my-android-application
                    // TODO: finish(); send: stop application from being in the foreground (exit)...kills the activity and? eventually the app
                    // but then I want a fresh trip object and initial view (just show them quickly, create object) and exit...
                    // There should be a button for starting a trip and a button for adding a sighting

                    //        if (files.length < 1 || files[0] == null) {//AsyncTask
                    //            return null;
                    //        }
                }// Alex: up to here
            }
        });
        //Step 5 ends here

    }

    public void buildSeedAnimalsFromResources() {

        // get the resources (specie_names, descriptions, photos)
        String[] specie_names = getResources().getStringArray(R.array.speciesArray);
        // TODO: implement getting the photo ids and description data later
        String[] photos = new String[30];
        String[] descriptions = new String[30]; // all descriptions can be in one single text file
        Arrays.fill(photos, "photo"); // remember to give the photos names like SpermWhale_1, CommonDolphin_X
        Arrays.fill(descriptions, "description");

        // here (if the 3 arrays have the same size, at least check) add each animal to the list, one by one
        int sizeOfArrays = specie_names.length;

        if ( sizeOfArrays != photos.length || sizeOfArrays != descriptions.length) {
            Log.d("MainActivity", "the sizes of the specie_names, photos and descriptions arrays are not the same");
        }

        for (int i = 0; i < sizeOfArrays; i++ ) {
            Specie specie = new Specie(specie_names[i], photos[i], descriptions[i]);
            seedAnimals.add(new Animal(specie));
        }

    }

    // method called by the BACK and SAVE buttons - I will probably get rid of this (just use SLOW and FAST gps)
    public void stopFastStartSlowGps() {
        // stop the fast gps mode and start the slow one
        if (trip.getGpsMode() != GpsMode.CONTINUOUS) {
            // not too slow, it's still needed by the SEND button, when saving the trip
            trip.setGpsMode(GpsMode.SLOW);//TODO: this actually needs to change the sampling rate
        }
    }

    // method called by BACK and SAVE
    public void disconnectOpenedSighting() {
        // Java is Pass-by-value/Call by sharing - therefore the referred object will not be nullified
        openedSightings[0] = null;
    }

    @Override
    public void openSighting(String label, Sighting sighting, AnimalAdapter animalAdapter) {
        // set openedSighting - to be later used by SAVE
        openedSightings[0] = sighting; // TODO: issues here?

        // set label
        setTitle(label);

        Animal animal = sighting.getAnimal();
        String specieName = null;
        if (animal != null) {
            specieName = animal.getSpecie().getName();
        }

        // Insert the sighting's animal quantity within the seed animals for displaying in the animal adapter
        // it might not have an animal, it might return null
        for (Animal seedAnimal: seedAnimals) {

            if (specieName != null && specieName.equals(seedAnimal.getSpecie().getName())) {
                // this is the same animal (with the same specie as this sighting's animal)
                // set its quantity so that the animal adapter displays it
                seedAnimal.setStartQuantity(sighting.getAnimal().getStartQuantity());
            } else {
                // we are going through the seed animals which are different from this sighting's animal
                // and clear (set to 0) whatever was there from before
                seedAnimal.setStartQuantity(0);
            }
        }

        // let the adapter now that the seedAnimals changed
        animalAdapter.notifyDataSetChanged();

        // make sighting list view invisible
        findViewById(R.id.list_view_sightings).setVisibility(View.INVISIBLE);
        // make "no sightings message" invisible
        findViewById(R.id.no_sightings_text_view).setVisibility(View.INVISIBLE);

        // make animal list view visible
        findViewById(R.id.list_view_animals).setVisibility(View.VISIBLE);

        findViewById(R.id.fab_add).setVisibility(View.INVISIBLE);
        findViewById(R.id.fab_send).setVisibility(View.INVISIBLE);
        findViewById(R.id.fab_save).setVisibility(View.VISIBLE);
        findViewById(R.id.fab_back).setVisibility(View.VISIBLE);
    }

    // method called by START, BACK, SAVE, DELETE, STOP/END and Comments buttons
    @Override
    public void showSightings(SightingAdapter sightingAdapter) {
        // set label
        setTitle(getText(R.string.app_name) +  " - " + getText(R.string.my_sightings));

        // hide START button
        findViewById(R.id.fab_start).setVisibility(View.INVISIBLE);
        // hide GPS mode checkbox
        findViewById(R.id.checkBox_tracking_gpsmode).setVisibility(View.INVISIBLE);
        // hide BACK button
        findViewById(R.id.fab_back).setVisibility(View.INVISIBLE);
        // hide SAVE button
        findViewById(R.id.fab_save).setVisibility(View.INVISIBLE);
        // hide animals list view
        findViewById(R.id.list_view_animals).setVisibility(View.INVISIBLE); // it was already INVISIBLE, when coming from START

        // show ADD button
        findViewById(R.id.fab_add).setVisibility(View.VISIBLE);
        // show SEND button
        findViewById(R.id.fab_send).setVisibility(View.VISIBLE);

        if (trip.getNumberOfSightings() == 0) {
            // show the no sightings message (its text is set in XML), if the trip is empty
            findViewById(R.id.no_sightings_text_view).setVisibility(View.VISIBLE);
            findViewById(R.id.list_view_sightings).setVisibility(View.INVISIBLE);
        } else {
            // show sightings list view, if the trip has any sightings
            findViewById(R.id.list_view_sightings).setVisibility(View.VISIBLE);
            findViewById(R.id.no_sightings_text_view).setVisibility(View.INVISIBLE);
        }

        // update sighting adapter Alex
        sightingAdapter.clear();
        sightingAdapter.addAll(trip.getSightings());
        sightingAdapter.notifyDataSetChanged();
    }

    @Override
    public void showSightingCommentsDialog(final Sighting sighting,
                                           final SightingAdapter sightingAdapter) {
        // TODO: take other smartphone gps reading (from trip, sighting, animal)
        // and compare the sign (if near the 0 degree point, don't do this check)
        LayoutInflater layoutInflater = LayoutInflater.from(this);

        View rootView = layoutInflater.inflate(R.layout.comments_dialog, null);
        final EditText latitudeDegrees = (EditText)rootView.findViewById(R.id.lat_degrees_edit_text);
        latitudeDegrees.setText(String.valueOf(sighting.getUserEndTimeAndPlace().getLatitude()));
        final EditText latitudeMinutes = (EditText)rootView.findViewById(R.id.lat_minutes_edit_text);
        final EditText latitudeSeconds = (EditText)rootView.findViewById(R.id.lat_seconds_edit_text);

        final EditText longitudeDegrees = (EditText)rootView.findViewById(R.id.long_degrees_edit_text);
        longitudeDegrees.setText(String.valueOf(sighting.getUserEndTimeAndPlace().getLongitude()));
        final EditText longitudeMinutes = (EditText)rootView.findViewById(R.id.long_minutes_edit_text);
        final EditText longitudeSeconds = (EditText)rootView.findViewById(R.id.long_seconds_edit_text);

        final EditText comments = (EditText)rootView.findViewById(R.id.comments_edit_text);
        comments.setText(sighting.getUserComments());

        AlertDialog.Builder comAlertDialogBuilder = new AlertDialog.Builder(this);

        comAlertDialogBuilder.setTitle(R.string.comments_message_title);

        comAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                // take and set the user's latitude
                double gpsDegrees = Utils.parseGpsToDouble(
                        latitudeDegrees.getText().toString(), GpsEdgeValue.DEGREES_LATITUDE
                );
                double gpsMinutes = Utils.parseGpsToDouble(
                        latitudeMinutes.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                );
                double gpsSeconds = Utils.parseGpsToDouble(
                        latitudeSeconds.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                );
                sighting.getUserEndTimeAndPlace().setLatitude(
                        Utils.convertDegMinSecToDecimal(gpsDegrees, gpsMinutes, gpsSeconds)
                );

                // take and set the user's longitude
                gpsDegrees = Utils.parseGpsToDouble(
                        longitudeDegrees.getText().toString(), GpsEdgeValue.DEGREES_LONGITUDE
                );
                gpsMinutes = Utils.parseGpsToDouble(
                        longitudeMinutes.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                );
                gpsSeconds = Utils.parseGpsToDouble(
                        longitudeSeconds.getText().toString(), GpsEdgeValue.MINUTES_OR_SECONDS
                );
                sighting.getUserEndTimeAndPlace().setLongitude(
                        Utils.convertDegMinSecToDecimal(gpsDegrees, gpsMinutes, gpsSeconds)
                );

                // take the system's time
                // this is giving me the time when they edited the comments the last time
                sighting.getUserEndTimeAndPlace().setTimeInMillis(System.currentTimeMillis());

                // take and set the user's comments
                sighting.setUserComments(comments.getText().toString());

                // refresh the views (maybe the final quantity was changed)
                showSightings(sightingAdapter);
            }
        });
        comAlertDialogBuilder.setNegativeButton(no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dialog.dismiss();
            }
        });

        comAlertDialogBuilder.setView(rootView);
        comAlertDialogBuilder.create();
        comAlertDialogBuilder.show();
    }

    @Override
    public void deleteSightingCommentsDialog(final Sighting sighting,
                                             final SightingAdapter sightingAdapter) {
        AlertDialog.Builder comAlertDialogBuilder = new AlertDialog.Builder(this);

        comAlertDialogBuilder.setTitle(R.string.delete_sighting_title);
        comAlertDialogBuilder.setMessage(R.string.delete_sighting_message);

        comAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                trip.getSightings().remove(sighting);
                // refresh the views
                showSightings(sightingAdapter);
            }
        });
        comAlertDialogBuilder.setNegativeButton(no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // dialog.dismiss();
            }
        });

        comAlertDialogBuilder.create();
        comAlertDialogBuilder.show();
    }

    @Override
    public Activity getMyActivity() {
        return this;
    }

    // no argument for this one, trip is a class variable
    //the activity has finish() at the end, therefore the big trip gets gc-ed, so decide what it is that you want to save
    // to the json file... for reopening...do you really want to reopen the big trip on the phone?
//    public void trimTrip() {
//
//        for (int i = 0; i < trip.getNumberOfSightings(); i++) {
//            Iterator<Animal> iter = trip.getSightingAtIndex(i).getAnimal().iterator();
//
//            while (iter.hasNext()) {
//                Animal animal = iter.next();
//
//                if (animal.getStartQuantity() == 0) {
//                    iter.remove();
//                }
//            }
//        }
//    }

}
