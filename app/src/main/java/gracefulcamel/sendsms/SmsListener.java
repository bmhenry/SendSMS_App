package gracefulcamel.sendsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.provider.Telephony.Sms.Intents;
import android.text.format.DateFormat;
import android.util.Log;

import java.util.Calendar;
import java.util.Locale;

/**
 * Created by Brandon on 12/13/2015.
 */

public class SmsListener extends  BroadcastReceiver {

    public static final String LOG_TAG = "SmsListener:";

    public void onReceive(Context context, Intent intent) {

        try {
            final SmsMessage[] messages = Intents.getMessagesFromIntent(intent);

            for (SmsMessage msg : messages) {
                String number = msg.getOriginatingAddress();
                String message = msg.getMessageBody();

                long timestamp = msg.getTimestampMillis();
                Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                cal.setTimeInMillis(timestamp);
                String datetime = DateFormat.format("h:mm A, MM/dd", cal).toString();

                String data = "RECEIVED\n";
                data += datetime + "\n";
                data += number + "\n";
                data += message + Character.toString((char)0x1d) + "\n";
                Log.i(LOG_TAG, data);

                Thread t = new Thread(new TcpClient("192.168.15.17", 5000, data));
                t.start();
                t.join(); // wait for thread to finish before releasing wakelock
            }

        } catch (Exception e) {
            Log.i(LOG_TAG, "Exception -- " + e);
        }
    }
}
