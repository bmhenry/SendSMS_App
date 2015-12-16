package gracefulcamel.sendsms;

import android.Manifest;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ToggleButton;


public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "GracefulSMS";

    // to help keep track of the button
    boolean enabled = false;
    boolean enableRequested = false;

    // ID for sms permission
    private static final int ID_RECEIVE_SMS = 0;
    String[] PERMISSIONS = new String[] {Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS};

    // layout of the activity
    private View layout;
    ToggleButton enableButton;

    // preferences
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // set layout
        layout = findViewById(R.id.snackbarPosition);

        // get settings
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        enabled = prefs.getBoolean("enabled", false); // returns false if setting doesn't exist

        // set button
        enableButton = (ToggleButton) findViewById(R.id.enableToggle);
        enableButton.setChecked(enabled);

        // start or stop receiver depending on enable state
        if (checkPermissions())
            smsReceiver(enabled);

        // deal with toggle button
        enableButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // check for permission
                    if (!checkPermissions()) {
                        requestPermissions();

                        enableRequested = true;
                        enableButton.setChecked(false);
                    } else {
                        // enable the background service
                        Log.i(LOG_TAG, "SMS service started");
                        enabled = true;
                    }
                } else {
                    // disable the background service
                    Log.i(LOG_TAG, "SMS service stopped");
                    enabled = false;
                }

                smsReceiver(enabled);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        // save settings
        SharedPreferences.Editor edit = prefs.edit();
        edit.putBoolean("enabled", enabled);
        edit.apply();
    }

    private void smsReceiver(boolean enabled) {
        int state;
        if (enabled)
            state = PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
        else
            state = PackageManager.COMPONENT_ENABLED_STATE_DISABLED;

        PackageManager pm = MainActivity.this.getPackageManager();
        ComponentName componentName = new ComponentName(MainActivity.this, SmsListener.class);
        pm.setComponentEnabledSetting(componentName, state, PackageManager.DONT_KILL_APP);
        Snackbar.make(layout, "Started background service", Snackbar.LENGTH_SHORT);
    }

    private boolean checkPermissions() {
        // check once
        int check;
        boolean perms = true;

        for (String s : PERMISSIONS) {
            check = ContextCompat.checkSelfPermission(this, s);
            perms &= (check == PackageManager.PERMISSION_GRANTED);
        }

        return perms;
    }

    /**
     * Requests permission to receive SMS:
     *  for notification when new sms arrives
     *  to send this sms to another computer
     */
    private void requestPermissions() {
        Log.i(LOG_TAG, "SMS permissions haven't been granted yet, asking now.");

        Snackbar.make(layout, R.string.permissionBar, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.choose, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, ID_RECEIVE_SMS);
                    }
                })
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == ID_RECEIVE_SMS) {
            if (grantResults.length >= 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Log.i(LOG_TAG, "SMS permissions granted");

                if (enableRequested) {
                    enabled = true;
                    enableRequested = false;
                    enableButton.setChecked(true);
                    Snackbar.make(layout, R.string.yesPermissionAlert, Snackbar.LENGTH_SHORT).show();
                }

            } else {
                // not what we were looking for, pass it up on to the base class
                Log.i(LOG_TAG, "SMS permissions not granted");
                enableButton.setChecked(false);
                Snackbar.make(layout, R.string.noPermissionAlert, Snackbar.LENGTH_SHORT).show();

                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }

        enableRequested = false;

    }
}
