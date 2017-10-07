package com.example.ronmad.foobarremote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class SocketService extends Service {

    SocketAddress addr;
    Socket socket;
    BufferedReader in;
    OutputStreamWriter out;

    static final int STR_WHAT = 200;

    FoobarActivity.MessageHandler messageHandler;
    String response;
    int responseNum;
    boolean firstAck;

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        if (intent == null)  {
            return START_STICKY;
        }
        addr = (InetSocketAddress)intent.getSerializableExtra(getString(R.string.socketaddress_intent_extra_key));
        firstAck = true;
        responseNum = -1;
        new Thread(() -> {
            try {
                Log.v("Service", "Connecting...");
                socket = new Socket();
                socket.connect(addr, 2000);
                synchronized (LoginActivity.class) {
                    LoginActivity.class.notifyAll();
                }
                Log.v("Service", "Connection established");
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new OutputStreamWriter(socket.getOutputStream());
                while (true) {
                    response = in.readLine();
                    if (response == null) {
                        break;
                    }
                    Log.v("Service", "Received " + response);
                    try {
                        responseNum = Integer.parseInt(response);
                    }
                    catch (NumberFormatException e) {
                        responseNum = -1;
                    }
                    if (responseNum == FoobarActivity.ACK && firstAck) {
                        synchronized (LoginActivity.class) {
                            LoginActivity.class.notifyAll();
                        }
                        firstAck = false;
                    }
                    else if (messageHandler != null) {
                        messageHandler.sendEmptyMessage(responseNum != -1 ? responseNum : STR_WHAT);
                    }
                }
            } catch (IOException e) {
                Log.e("Service", "Connection failed");
            } finally {
                synchronized (LoginActivity.class) {
                    LoginActivity.class.notifyAll();
                }
            }
        }).start();
        return START_STICKY;
    }

    private final IBinder myBinder = new LocalBinder();

    class LocalBinder extends Binder {
        SocketService getService() {
            return SocketService.this;
        }
    }

    public void sendMessage(int message) {
        new Thread(() -> {
            try {
                if (out != null) {
                    out.write(message);
                    out.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        })
        .start();
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v("Server", "disconnecting");
        disconnect();
    }

    private void disconnect() {
        try {
            if (socket != null && socket.isConnected()) {
                out.close();
                in.close();
                socket.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        socket = null;
    }
}
