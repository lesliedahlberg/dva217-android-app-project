package com.lesliedahlberg.placemem;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.pm.PackageManager;
import android.widget.Toast;


import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class AddMemActivity extends Activity {

    //Constants
    static final int REQUEST_TAKE_PHOTO = 1;
    static final String PHOTO_TAKEN = "photoTaken";
    static final String PHOTO_URI = "photoUri";
    static final String AUDIO_URI = "audioUri";
    private static final String LOG_TAG = "AudioRecord";

    //UI elements
    ImageView uiPhotoView;
    TextView uiTitleField;
    TextView uiGpsCoordsField;
    TextView uiDateField;
    TextView uiLocationField;
    ImageButton uiAudioRecordButton;
    ImageButton uiAudioPlayButton;

    //Values
    String currentLocation;
    String currentDate;
    double currentLatitude;
    double currentLongitude;
    String currentTitle;

    Boolean photoTaken;
    boolean hasGps;
    boolean recording = false;
    boolean playing = false;

    String tripId;

    //URI
    Uri currentPhotoUri;
    Uri currentAudioUri;

    //Media Resources
    MediaRecorder mRecorder = null;
    MediaPlayer mPlayer = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }

        tripId = getIntent().getStringExtra(MemActivity.TRIP_ID);

        if (photoTaken == null){
            photoTaken = false;
        }


        //Inflate UI
        setContentView(R.layout.activity_add_mem);

        //Get UI references
        uiPhotoView = (ImageView) findViewById(R.id.photoView);
        uiTitleField = (TextView) findViewById(R.id.titleField);
        uiAudioRecordButton = (ImageButton) findViewById(R.id.audio_record_button);
        uiAudioPlayButton = (ImageButton) findViewById(R.id.audio_play_button);


        //Get date
        currentDate = new SimpleDateFormat("dd. MM. yyyy", Locale.getDefault()).format(new Date());
        
        //For checking if device has gps
        PackageManager packageManager = this.getPackageManager();
        hasGps = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);

        //Get location data
        getLocationData();
        createAndSetAudioFilePath();

        if (currentPhotoUri != null) {
            if (!currentPhotoUri.toString().isEmpty()) {
                showPhoto();
            }
        }

        //Get photo
        if (photoTaken == false) {
            takePhoto();
            photoTaken = true;
        }

        //Set button listeners
        uiAudioRecordButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                if(recording)
                {
                    stopRecording();
                    recording = false;
                    uiAudioRecordButton.setImageResource(R.drawable.ic_action_microphone_white);
                }
                else
                {
                    if(playing)
                        stopPlaying();

                    startRecording();
                    recording = true;
                    uiAudioRecordButton.setImageResource(R.drawable.ic_action_microphone_red);
                }
            }
        });

        uiAudioPlayButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                if(playing)
                {
                    stopPlaying();
                    playing = false;
                    uiAudioPlayButton.setImageResource(R.drawable.ic_play);
                }
                else
                {
                    if(recording)
                        stopRecording();

                    startPlaying();
                    playing = true;
                    uiAudioPlayButton.setImageResource(R.drawable.ic_stop);
                }
            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PHOTO_TAKEN, photoTaken);
        outState.putString(PHOTO_URI, currentPhotoUri.toString());
        outState.putString(AUDIO_URI, currentAudioUri.toString());
        outState.putString(MemActivity.TRIP_ID, tripId);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            photoTaken = savedInstanceState.getBoolean(PHOTO_TAKEN);

            String mPhotoUri = savedInstanceState.getString(PHOTO_URI);
            String mAudioUri = savedInstanceState.getString(AUDIO_URI);

            if (mPhotoUri != null){
                currentPhotoUri = Uri.parse(mPhotoUri);
            }
            if (mAudioUri != null){
                currentAudioUri = Uri.parse(mAudioUri);
            }

            tripId = savedInstanceState.getString(MemActivity.TRIP_ID);

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_mem, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Save mem and exit activity
    public void save (View view) {
        currentTitle = uiTitleField.getText().toString();
        //Write to DB
        new DBInterface(this).addRow(currentPhotoUri.toString(), currentAudioUri.toString(), currentLocation, currentLatitude, currentLongitude, currentDate, currentTitle, tripId);
        //Set result OK
        setResult(RESULT_OK);
        //Exit
        finish();

    }

    public void discard (View view) {
        setResult(RESULT_CANCELED);
        finish();
    }

    //Take photo
    public void takePhoto() {
        dispatchTakePictureIntent();
    }

    //Plays back audio recording
    private void startPlaying()
    {
        mPlayer = new MediaPlayer();
        try{
            mPlayer.setDataSource(currentAudioUri.getPath());
            mPlayer.prepare();
            mPlayer.start();
        }catch(IOException e) {
            e.printStackTrace();
        }

        Toast toast = Toast.makeText(this, "Playing", Toast.LENGTH_SHORT);
        toast.show();
    }


    //Stops audio playback
    private void stopPlaying()
    {
        mPlayer.release();
        mPlayer = null;

        Toast toast = Toast.makeText(this, "Paused", Toast.LENGTH_SHORT);
        toast.show();
    }


    //Starts recording audio to file specified by currentAudioUri
    private void startRecording()
    {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP); // Might have to change to some other format //AAC_ADTS
        mRecorder.setOutputFile(currentAudioUri.getPath()); //audioUri
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try
        {
            mRecorder.prepare();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        mRecorder.start();

        Toast toast = Toast.makeText(this, "Recording", Toast.LENGTH_SHORT);
        toast.show();
    }


    //Stops recording audio
    private void stopRecording()
    {
        mRecorder.stop();
        mRecorder.release(); //Release resources
        mRecorder = null; //null reference

        Toast toast = Toast.makeText(this, "Stopped Recording", Toast.LENGTH_SHORT);
        toast.show();
    }



    //Send intent to take photo
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //On photo taken
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            showPhoto();

        }else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void createAndSetAudioFilePath()
    {
        File audioFile = null;
        try {
            audioFile = createAudioFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        currentAudioUri = Uri.fromFile(audioFile);
    }

    private void showPhoto() {
        if (!currentPhotoUri.toString().isEmpty()) {
            final int THUMBSIZE = 512;
            Bitmap bitmap = LoadBitmap.decodeSampledBitmapFromResource(this, currentPhotoUri, THUMBSIZE, THUMBSIZE);
            setUiBackgroundView(bitmap);
        }
    }

    //Set background to taken photo
    private void setUiBackgroundView (Bitmap bitmap) {
        uiPhotoView.setImageBitmap(bitmap);
    }

    //Create file to store photo in (locally in private app storage)
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStorageDirectory();
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        //Save URI to file
        currentPhotoUri = Uri.fromFile(image);

        //return file
        return image;
    }

    //Create file to store audio in (locally in private app storage)
    private File createAudioFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "3GP_" + timeStamp + "_"; // AAC_
        File storageDir = Environment.getExternalStorageDirectory();
        File audio = File.createTempFile(
                imageFileName,  /* prefix */
                ".3gp",         /* suffix */ //.acc
                storageDir      /* directory */
        );

        //Save URI to file
        currentAudioUri = Uri.fromFile(audio);

        //return file
        return audio;
    }

    //Get location data
    private void getLocationData () {
        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                if(hasGps) {
                    currentLatitude = location.getLatitude();
                    currentLongitude = location.getLongitude();
                }
                else{
                    currentLatitude = 54.127537;
                    currentLongitude = 18.627353;
                }

                Geocoder gcd = new Geocoder(getApplicationContext(), Locale.getDefault());
                List<Address> addresses = null;
                try {
                    addresses = gcd.getFromLocation(currentLatitude, currentLongitude, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (addresses.size() > 0){
                    currentLocation = addresses.get(0).getLocality();
                }

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        // Register the listener with the Location Manager to receive location updates
        if(hasGps)
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }


}
