package com.example.ronmad.foobarremote;

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
import android.util.Log;

public class SocketService extends Service {

    SocketAddress addr;
    Socket socket;
    InputStreamReader in;
    OutputStreamWriter out;
    int response;

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
        new Thread(() -> {
            try {
                Log.v("Service", "Connecting...");
                socket = new Socket();
                socket.connect(addr, 2000);
                in = new InputStreamReader(socket.getInputStream());
                out = new OutputStreamWriter(socket.getOutputStream());
                Log.v("Service", "Connection established");
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

    public int sendMessage(int message) {
        response = -1;
        Thread sendThread = new Thread(() -> {
            try {
                if (out != null) {
                    out.write(message);
                    out.flush();
                }
                response = in.read();
                Log.v("Service", "Received " + response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        sendThread.start();
        try {
            sendThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return response;
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
