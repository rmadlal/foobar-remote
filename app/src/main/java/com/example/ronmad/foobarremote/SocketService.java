package com.example.ronmad.foobarremote;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;

import android.app.IntentService;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class SocketService extends IntentService {
    String serverIP; //your computer IP address should be written here
    int serverPort;
    InputStreamReader in = null;
    OutputStreamWriter out = null;
    Socket socket = null;

    public SocketService() {
        super("Server");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        int connectionAttemptCounter = 0;
        try {
            Log.v("TCP Client", "C: Connecting...");
            connectionAttemptCounter++;
            socket = new Socket(serverIP, serverPort);
            synchronized (LoginActivity.class) {
                LoginActivity.class.notifyAll();
            }
            Log.v("TCP Client", "Connection established.");
            in = new InputStreamReader(socket.getInputStream());
            out = new OutputStreamWriter(socket.getOutputStream());
        } catch (ConnectException e) {
            if (connectionAttemptCounter == 3) {
                Log.e("TCP Client", "Connection refused 3 times. Aborting.");
            } else {
                Log.v("TCP Client", "Connection refused. Retrying.");
                onHandleIntent(intent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final IBinder myBinder = new LocalBinder();
   // TCPClient mTcpClient = new TCPClient();

    class LocalBinder extends Binder {
        SocketService getService() {
            return SocketService.this;

        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void sendMessage(byte message) {
        try {
            if (out != null) {
                out.write(message);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte getResponse() {
        byte response = -1;
        try {
            if (in != null)
                response = (byte) in.read();
        } catch (Exception e) {
            Log.e("TCP", "S: Error", e);
        }
        return response;
    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        serverIP = intent.getStringExtra("ip");
        serverPort = intent.getIntExtra("port", 5050);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();
    }


    private void disconnect() {
        try {
            if (socket != null) {
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
