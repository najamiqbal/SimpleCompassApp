package com.compass.softsolstudio.app;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Compass compass;
    private ImageView arrowView;
    private TextView sotwLabel;  // SOTW is for "side of the world"
    AdView LH_bottom;
    private float currentAzimuth;
    TextView text_bawah;
    private SOTWFormatter sotwFormatter;
    private AlertDialog dialogBuilder;
    SharedPreferences prefs;

    GPSTracker gps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sotwFormatter = new SOTWFormatter(this);
        prefs = getSharedPreferences("", MODE_PRIVATE);
        arrowView = (ImageView) findViewById(R.id.main_image_hands);
        sotwLabel = (TextView) findViewById(R.id.sotw_label);
        text_bawah = (TextView) findViewById(R.id.teks_bawah);
        MobileAds.initialize(MainActivity.this, getString(R.string.ApAdId));
        AdRequest adRequest = new AdRequest.Builder().build();
        LH_bottom = findViewById(R.id.bottomAd);
        LH_bottom.loadAd(adRequest);
        gps = new GPSTracker(this);

        setupCompass();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_compass, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                calibrateDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void calibrateDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(this);
        View calibrateView = layoutInflater.inflate(R.layout.calibrate_dialog_layout, null);

        dialogBuilder = new AlertDialog.Builder(this).create();
        dialogBuilder.setTitle("Calibration");
        dialogBuilder.setIcon(R.mipmap.ic_launcher);
        dialogBuilder.setView(calibrateView);

        Button btnClose = (Button) calibrateView.findViewById(R.id.close);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogBuilder.dismiss();
            }
        });

        dialogBuilder.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "start compass");
        compass.start();
        fetch_GPS();
    }

    @Override
    protected void onPause() {
        super.onPause();
        compass.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        compass.start();
        fetch_GPS();
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "stop compass");
        compass.stop();
    }

    private void setupCompass() {
        Boolean permission_granted = GetBoolean("permission_granted");
        if (permission_granted) {
            fetch_GPS();
            //getBearing();
        } else {
            //text_atas.setText(getResources().getString(R.string.msg_permission_not_granted_yet));
            text_bawah.setText(getResources().getString(R.string.msg_permission_not_granted_yet));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        1);
            }
        }
        compass = new Compass(this);
        Compass.CompassListener cl = getCompassListener();
        compass.setListener(cl);

    }

    private void adjustArrow(float azimuth) {
        Log.d(TAG, "will set rotation from " + currentAzimuth + " to "
                + azimuth);

        Animation an = new RotateAnimation(-currentAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        currentAzimuth = azimuth;

        an.setDuration(600);
        an.setRepeatCount(0);
        an.setFillAfter(true);

        arrowView.startAnimation(an);
    }

    private void adjustSotwLabel(float azimuth) {
        sotwLabel.setText(sotwFormatter.format(azimuth));
    }


    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            @Override
            public void onNewAzimuth(final float azimuth) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adjustArrow(azimuth);
                        adjustSotwLabel(azimuth);

                    }
                });
            }
        };
    }


    public void SaveBoolean(String Judul, Boolean bbb) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean(Judul, bbb);
        edit.apply();
    }

    public Boolean GetBoolean(String Judul) {
        Boolean result = prefs.getBoolean(Judul, false);
        return result;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    SaveBoolean("permission_granted", true);
                    fetch_GPS();

                } else {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_permission_required), Toast.LENGTH_LONG).show();
                    finish();
                }
                return;
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    public void fetch_GPS() {

        double result = 0;
        gps = new GPSTracker(this);
        if (gps.canGetLocation()) {
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();

            text_bawah.setText(getResources().getString(R.string.your_location) + "\nLat: " + latitude + " Long: " + longitude);

        } else {
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
            gps.showSettingsAlert();
            text_bawah.setText(getResources().getString(R.string.pls_enable_location));
        }
    }
}//MainActivity






