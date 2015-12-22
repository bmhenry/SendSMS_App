package gracefulcamel.sendsms;

import android.util.Log;

import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Enumeration;

/**
 * Created by Brandon on 12/16/2015.
 */
public class TcpClient implements Runnable {

    String localIP;
    int localPort; //= 5000;
    String destIP;
    int destPort;

    String data;

    boolean started = false;

    Socket socket;
    DataOutputStream dataStream;

    public TcpClient(String dstIP, int dstPort, String message) {
        //localIP = getLocalIpAddress();
        Log.i("GracefulSMS TcpClient", "Started tcp class");
        destIP = dstIP;
        destPort = dstPort;
        data = message;
    }

    @Override
    public void run() {
        start();
        Log.i("GracefulSMS TcpClient", "Sending " + data);
        send(data);
        close();
    }

    public void start() {
        try {
            socket = new Socket(destIP, destPort);

            localIP = socket.getLocalAddress().toString();
            localPort = socket.getLocalPort();
            Log.i("GracefulSMS TcpClient", "Local ip: " + localIP + "; Local port: " + localPort);

            dataStream = new DataOutputStream(socket.getOutputStream());

            started = true;

        } catch (Exception e) {
            Log.i("GracefulSMS TcpClient", "Exception starting server -- " + e);
            started = false;
        }
    }

    public void send(String str) {
        if (started) {
            try {
                dataStream.writeBytes(str);
            } catch (Exception e) {
                Log.i("GracefulSMS TcpClient", "Exception -- " + e);
            }
        }
    }

    public void close() {
        if (started) {
            try {
                dataStream.close();
                socket.close();
                started = false;
            } catch (Exception e) {
                Log.i("GracefulSMS TcpClient", "Exception -- " + e);
                started = false;
            }
        }
    }


    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface ni = en.nextElement();
                for (Enumeration<InetAddress> enIP = ni.getInetAddresses(); enIP.hasMoreElements();) {
                    InetAddress inet = enIP.nextElement();
                    if (!inet.isLoopbackAddress()) {
                        return inet.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            Log.i("GracefulSMS TcpClient", "Exception -- " + e);
        }

        return null;
    }
}
