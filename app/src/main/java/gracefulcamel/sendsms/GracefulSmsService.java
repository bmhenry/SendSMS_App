package gracefulcamel.sendsms;


import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsMessage;
import android.text.format.DateFormat;
import android.util.Log;
import android.provider.ContactsContract.PhoneLookup;

import java.util.Calendar;
import java.util.Locale;

/**
 * Service to look for incoming SMS and forward it to the GracefulClient
 */
public class GracefulSmsService extends Service {
    // receiver to be registered
    private SmsListener smsListener;
    private IntentFilter intentFilter;

    public static final String LOG_TAG = "GracefulSmsService";
    private GracefulClient client;

    // handles status updates
    @Override
    public void onCreate() {
        Log.i(LOG_TAG, "Service created");
        super.onCreate();

        startServer();
        Log.i(LOG_TAG, "Server started");

        // create receiver
        smsListener = new SmsListener();
        intentFilter = new IntentFilter();
        intentFilter.addAction("android.provider.Telephony.SMS_RECEIVED");
        intentFilter.addAction("android.provider.Telephony.SMS_SENT");

        registerReceiver(smsListener, intentFilter);
        Log.i(LOG_TAG, "Listener added");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // no binding, for now
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopServer();
        unregisterReceiver(smsListener);

        Log.i(LOG_TAG, "Service destroyed");
    }

    private void startServer() {
        client = new GracefulClient(8000);
    }

    private void stopServer() {
        client.stop();
    }

    public String getContactDisplayNameByNumber(String number) {
        Uri uri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String name = number;  // by default (unknown name), name returns as phone number

        ContentResolver contentResolver = getContentResolver();
        Cursor contactLookup = contentResolver.query(uri, new String[] {BaseColumns._ID, PhoneLookup.DISPLAY_NAME}, null, null, null);

        try {
            if (contactLookup != null && contactLookup.getCount() > 0) {
                contactLookup.moveToNext();
                name = contactLookup.getString(contactLookup.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
            }
        } finally {
            if (contactLookup != null) {
                contactLookup.close();
            }
        }

        return name;
    }

    private class SmsListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            try {
                final SmsMessage[] messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);

                for (SmsMessage msg : messages) {
                    String number = msg.getOriginatingAddress();
                    String message = msg.getMessageBody();
                    String name = getContactDisplayNameByNumber(number);

                    long timestamp = msg.getTimestampMillis();
                    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                    cal.setTimeInMillis(timestamp);
                    String datetime = DateFormat.format("h:mm A, MM/dd", cal).toString();

                    String data = "";
                    if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED"))
                        data = "RECEIVED\n";
                    else if (intent.getAction().equals("android.provider.Telephony.SMS_SENT"))
                        data = "SENT\n";
                    data += datetime + "\n";
                    data += number + "\n";
                    data += name + "\n";
                    data += message + Character.toString((char) 0x1d) + "\n";
                    Log.i(LOG_TAG, data);

                    client.send(data);
                }

            } catch (Exception e) {
                Log.i(LOG_TAG, "Exception -- " + e);
            }
        }
    }
}
