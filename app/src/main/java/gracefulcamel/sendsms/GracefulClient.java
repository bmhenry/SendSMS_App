package gracefulcamel.sendsms;

import android.text.format.DateFormat;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Calendar;
import java.util.Locale;

import android.telephony.SmsManager;

/**
 * Handles TCP Socket connection between phone and computer
 */
public class GracefulClient {

    public static final String LOG_TAG = "GracefulClient";

    String destIP;
    int destPort;

    boolean should_run = false;

    Socket socket;
    SocketAddress remoteAddress;
    DataInputStream inputStream;
    DataOutputStream outputStream;
    SmsManager manager;


    public GracefulClient(String dstIP, int dstPort) {
        destIP = dstIP;
        destPort = dstPort;
        manager = SmsManager.getDefault();

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
                    remoteAddress = socket.getRemoteSocketAddress();

                    inputStream = new DataInputStream(socket.getInputStream());
                    outputStream = new DataOutputStream(socket.getOutputStream());
                    Log.i(LOG_TAG, "socket opened");
                } catch (Exception e) {
                    Log.i(LOG_TAG, "Error starting socket");
                }
            }
        };

        try {
            Thread t = new Thread(r);
            t.start();
            t.join();  // forces wait to finish
        } catch (Exception e) {
            Log.i(LOG_TAG, "Error running thread to start socket" + e);
        }
    }

    public void beginListen() {
        class ListenerRunnable implements Runnable {
            GracefulClient parent;

            ListenerRunnable(GracefulClient parent) {
                this.parent = parent;
            }

            public void run() {
                try {
                    while (should_run) {
                        if (!socket.isConnected()) {
                            Log.i(LOG_TAG, "Client disconnected");
                            socket.connect(remoteAddress, 100); // timeout 100ms, try to reconnect every tenth of second
                        }
                        else {
                            if (inputStream.available() > 0) {
                                int length = inputStream.readInt();

                                byte[] buffer = new byte[length];
                                int flag = inputStream.read(buffer);

                                if (flag != -1) {
                                    String data = new String(buffer);
                                    Log.i(LOG_TAG, "Data from PC:\n" + data);

                                    String[] split_data = data.split("\n", 2);
                                    String phoneNum = split_data[0];
                                    String message = split_data[1];
                                    message = message.substring(0, message.length() - 2);

                                    manager.sendTextMessage(phoneNum, null, message, null, null);

                                    String datetime = DateFormat.format("hh:mm A, MM/dd", Calendar.getInstance(Locale.ENGLISH)).toString();
                                    String returnData = "SENT\n";
                                    returnData += datetime + "\n";
                                    returnData += phoneNum + "\n";
                                    returnData += message + Character.toString((char) 0x1d) + "\n";
                                    parent.send(returnData);
                                } else {
                                    Log.i(LOG_TAG, "End of buffer");
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.i(LOG_TAG, "Error listening on socket; " + e);
                }
            }
        }

        try {
            Thread t = new Thread(new ListenerRunnable(this));
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
                        Log.i(LOG_TAG, "wrote:\n" + string);
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
