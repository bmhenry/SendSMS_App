package gracefulcamel.sendsms;


import gracefulcamel.sendsms.Client;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

/**
 * Created by Brandon on 12/13/2015.
 */
public class GracefulSmsService extends Service {
    public static final String LOG_TAG = "GracefulSmsService";
    private Client client;

    // handles status updates
    @Override
    public void onCreate() {
        startServer();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // no binding, for now
    }

    @Override
    public void onDestroy() {
        stopServer();
    }

    private void startServer() {
        client = new Client("192.168.15.17", 5000);
    }

    private void stopServer() {
        client.stop();
    }
}
