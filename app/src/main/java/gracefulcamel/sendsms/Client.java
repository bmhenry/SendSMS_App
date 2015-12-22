package gracefulcamel.sendsms;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

/**
 * Created by Brandon on 12/19/2015.
 */
public class Client {

    public static final String LOG_TAG = "GracefulSMS Client";

    String destIP;
    int destPort;

    boolean should_run = false;

    Socket socket;
    DataInputStream inputStream;
    DataOutputStream outputStream;

    Thread listenThread;


    public Client(String dstIP, int dstPort) {
        destIP = dstIP;
        destPort = dstPort;

        start();
        beginListen();
    }

    public void start() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    should_run = true;

                    socket = new Socket(destIP, destPort);
                    socket.setKeepAlive(true);

                    inputStream = new DataInputStream(socket.getInputStream());
                    outputStream = new DataOutputStream(socket.getOutputStream());
                } catch (Exception e) {
                    Log.i(LOG_TAG, "Error starting socket");
                }
            }
        };

        try {
            Thread t = new Thread(r);
            t.start();
            t.join();
        } catch (Exception e) {
            Log.i(LOG_TAG, "Error running thread to start socket" + e);
        }
    }

    public void beginListen() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    while (should_run) {
                        int length = inputStream.readInt();

                        byte[] buffer = new byte[length];
                        int flag = inputStream.read(buffer);

                        if (flag != -1) {
                            String data = new String(buffer);
                            Log.i(LOG_TAG, "Data: " + data);
                        } else {
                            Log.i(LOG_TAG, "End of buffer");
                        }
                    }
                } catch (Exception e) {
                    Log.i(LOG_TAG, "Error listening on socket " + e);
                }
            }
        };

        try {
            Thread t = new Thread(r);
            t.start();
        } catch (Exception e) {
            Log.i(LOG_TAG, "Error starting thread to listen on socket");
        }
    }

    public void send(String str) {
        final String string = str;
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (should_run) {
                    try {
                        outputStream.writeBytes(string);
                    } catch (Exception e) {
                        Log.i(LOG_TAG, "Exception -- " + e);
                    }
                }
            }
        };

        try {
            Thread t = new Thread(r);
            t.start();
        } catch (Exception e) {
            Log.i(LOG_TAG, "Error sending from socket " + e);
        }

    }

    public void stop() {
        should_run = false;

        if (!socket.isClosed()) {
            try {
                inputStream.close();
                outputStream.close();
                socket.close();
            } catch (Exception e) {
                Log.i(LOG_TAG, "Couldn't close connection");
            }
        }
    }
}
