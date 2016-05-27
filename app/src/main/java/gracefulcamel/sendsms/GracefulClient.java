package gracefulcamel.sendsms;

import android.text.format.DateFormat;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;

import android.telephony.SmsManager;

/**
 * Handles TCP Socket connection between phone and computer
 */
public class GracefulClient {

    public static final String LOG_TAG = "GracefulClient";

    int PORT;

    boolean should_run = false;

    Socket socket;
    ServerSocket server;
    SmsManager manager;

    Thread listenThread;

    Queue<String> sendQueue;


    public GracefulClient(int port) {
        this.PORT = port;
        manager = SmsManager.getDefault();
        sendQueue = new LinkedList<>();
        should_run = true;

        start();
    }

    class ListenerRunnable implements Runnable {
        GracefulClient parent;

        ListenerRunnable(GracefulClient parent) {
            this.parent = parent;
        }

        public void run() {
            socket = null;
            try {
                server = new ServerSocket(PORT);
                Log.i(LOG_TAG, "Created server on port " + Integer.toString(PORT));
            } catch (IOException e) {
                Log.e(LOG_TAG, "Exception creating server -- " + e);
            }

            while (should_run && !Thread.currentThread().isInterrupted()) {
                try {
                    socket = server.accept();
                    Log.i(LOG_TAG, "Opened socket to " + socket.getRemoteSocketAddress().toString());
                    CommRunnable comm = new CommRunnable(parent, socket);
                    new Thread(comm).start();
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error accepting socket -- " + e);
                }
            }
        }
    }

    class CommRunnable implements Runnable {
        GracefulClient parent;
        Socket socket;
        DataInputStream inputStream;
        DataOutputStream outputStream;

        CommRunnable(GracefulClient parent, Socket socket) {
            this.parent = parent;
            this.socket = socket;

            try {
                inputStream = new DataInputStream(socket.getInputStream());
                outputStream = new DataOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error getting data streams -- " + e);
            }
        }

        @Override
        public void run() {
            while (should_run && socket.isConnected()) {
                try {
                    if (inputStream.available() > 0) {
                        int length = inputStream.readInt();

                        byte[] buffer = new byte[length];
                        int flag = inputStream.read(buffer);

                        if (flag != -1) {
                            String data = new String(buffer);
                            Log.i(LOG_TAG, "Data from PC:\n" + data);

                            String[] split_data = data.split("\n", 3);
                            String phoneNum = split_data[0];
                            String name = split_data[1];
                            String message = split_data[2];
                            message = message.substring(0, message.length() - 2);

                            manager.sendTextMessage(phoneNum, null, message, null, null);

                            String datetime = DateFormat.format("hh:mm A, MM/dd", Calendar.getInstance(Locale.ENGLISH)).toString();
                            String returnData = "SENT\n";
                            returnData += datetime + "\n";
                            returnData += phoneNum + "\n";
                            returnData += name + "\n";
                            returnData += message + Character.toString((char) 0x1d) + "\n";

                            parent.send(returnData);
                        } else {
                            Log.i(LOG_TAG, "Connection closed");
                            socket.close();
                        }
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error reading from input stream -- " + e);
                }

                try {
                    if (sendQueue != null && sendQueue.size() > 0) {
                        String message = sendQueue.peek();
                        outputStream.writeBytes(message);
                        sendQueue.remove();
                    }
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error sending over TCP -- " + e);
                }
            }

            // end of while loop
            try {
                inputStream.close();
                outputStream.close();
                socket.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error closing socket -- " + e);
            }
        }
    }

    public void start() {
        try {
            listenThread = new Thread(new ListenerRunnable(this));
            listenThread.start();
        } catch (Exception e) {
            Log.i(LOG_TAG, "Error starting thread to listen on socket -- " + e);
        }
    }

    public void send(String str) {
        sendQueue.add(str);
    }

    public void stop() {
        should_run = false;

        if (!server.isClosed()) {
            try {
                server.close();
            } catch (Exception e) {
                Log.i(LOG_TAG, "Couldn't close server -- " + e);
            }
        }
    }
}
