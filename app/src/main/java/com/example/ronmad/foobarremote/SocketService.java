package com.example.ronmad.foobarremote;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class SocketService extends IntentService {
    InputStreamReader in = null;
    OutputStreamWriter out = null;
    Socket socket = null;
    int connectionAttemptCounter;

    public SocketService() {
        super("Server");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            connectionAttemptCounter++;
            String serverIP = intent.getStringExtra("ip");
            int serverPort = intent.getIntExtra("port", 5050);
            SocketAddress addr = new InetSocketAddress(serverIP, serverPort);
            socket = new Socket();
            socket.connect(addr, 2000);
            synchronized (LoginActivity.class) {
                LoginActivity.class.notifyAll();
            }
            Log.v("TCP Client", "Connection established.");
            in = new InputStreamReader(socket.getInputStream());
            out = new OutputStreamWriter(socket.getOutputStream());
        } catch (ConnectException e) {
            if (connectionAttemptCounter < 3) {
                Log.v("TCP Client", "Connection refused. Retrying.");
                onHandleIntent(intent);
            } else {
                Log.e("TCP Client", "Connection refused 3 times. Aborting.");
            }
        } catch (SocketTimeoutException e) {
            Log.e("TCP Client", "Connection timed out. Aborting.");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            synchronized (LoginActivity.class) {
                LoginActivity.class.notifyAll();
            }
        }
    }

    private final IBinder myBinder = new LocalBinder();

    class LocalBinder extends Binder {
        SocketService getService() {
            return SocketService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public boolean sendMessage(byte message) {
        try {
            if (out != null) {
                out.write(message);
                out.flush();
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public byte getResponse() {
        byte response = -1;
        try {
            if (in != null)
                response = (byte) in.read();
        } catch (IOException e) {
            Log.e("TCP Client", "No response from server");
        }
        return response;
    }

    public boolean isConnected() {
        return socket.isConnected();
    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        connectionAttemptCounter = 0;
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    private void disconnect() {
        try {
            if (socket != null && socket.isConnected()) {
                socket.shutdownOutput();
                socket.shutdownInput();
                socket.close();
            }
        }
        catch (SocketException e) {
            Log.e("TCP Server", "Transport endpoint is not connected");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
