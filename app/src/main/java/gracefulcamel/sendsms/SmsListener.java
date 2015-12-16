package gracefulcamel.sendsms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;
import android.provider.Telephony.Sms.Intents;
import android.util.Log;
import gracefulcamel.sendsms.TcpClient;

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

                String data = "number: " + number + "; message: " + message + "\n";
                Log.i(LOG_TAG, data);

                new Thread(new TcpClient("192.168.0.103", 5000, data)).start();
            }

        } catch (Exception e) {
            Log.i(LOG_TAG, "Exception -- " + e);
        }
    }
}
