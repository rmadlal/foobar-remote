package com.example.ronmad.foobarremote;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
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
    InputStreamReader in;
    OutputStreamWriter out;
    Socket socket;
    int connectionAttemptCounter;
    volatile byte toSend;

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
            SocketAddress addr = (InetSocketAddress)intent.getSerializableExtra("Address");
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
        while (socket != null) {
            //wait for input
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            try {
                out.write(toSend);
                out.flush();
                int rec;
                if ((rec = in.read()) >= 0) {
                    Log.v("Received", String.valueOf(rec));
                }
            } catch (IOException e) {
                Log.e("Service", "Server disconnected");
                disconnect();
            }
            if (toSend == FoobarActivity.DISC) {
                disconnect();
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
        connectionAttemptCounter = 0;
        in = null;
        out = null;
        socket = null;
    }

    public void sendMessage(byte message) {
        toSend = message;
        synchronized (this) {
            notifyAll();
        }
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    public boolean hasDisconnected() {
        return socket == null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    private void disconnect() {
        try {
            if (socket != null && socket.isConnected()) {
                out.close();
                in.close();
                socket.close();
                socket = null;

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
