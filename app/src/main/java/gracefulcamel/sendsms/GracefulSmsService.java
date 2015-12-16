package gracefulcamel.sendsms;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Intent;

/**
 * Created by Brandon on 12/13/2015.
 */
public class GracefulSmsService extends IntentService {

    public static final String LOG_TAG = "GracefulSmsService:";

    // handles status updates


    public GracefulSmsService()
    {
        super("GracefulSmsService");
    }

    /**
     *  In an IntentService, onHandleIntent is run on a background thread.
     *  It broadcasts its current status using the LocalBroadcastManager
     * @param workIntent The Intent that starts the IntentService
     */
    @Override
    protected void onHandleIntent(Intent workIntent)
    {
        // gets data from incoming intent
        String dataString = workIntent.getDataString();

        // do work here based on contents of data string
    }
}
