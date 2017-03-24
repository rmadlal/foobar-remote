package com.example.ronmad.foobarremote;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

public class FoobarActivity extends AppCompatActivity {

    public static final byte LAUNCH = 1;
    public static final byte PLAY_PAUSE = 2;
    public static final byte NEXT = 3;
    public static final byte PREV = 4;
    public static final byte VOL_UP = 5;
    public static final byte VOL_DOWN = 6;
    public static final byte DISC = 7;
    public static final byte ACK = 8;

    SocketService mBoundService;
    InputTask mAuthTask = null;
    ImageButton mButton = null;

    private ImageButton playButton;
    private ImageButton pauseButton;
    private int mShortAnimationDuration;
    private boolean isPlaying;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBoundService = ((SocketService.LocalBinder)service).getService();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBoundService = null;
        }

    };

    private void doBindService() {
        if(mBoundService==null)
            bindService(new Intent(this, SocketService.class), mConnection, Context.BIND_AUTO_CREATE);
    }


    private void doUnbindService() {
        if (mBoundService != null) {
            unbindService(mConnection);
        }
    }

    private class InputTask extends AsyncTask<Byte, Void, Boolean> {

        byte response;

         @Override
        protected Boolean doInBackground(Byte... params) {
             mBoundService.sendMessage(params[0]);
             response = mBoundService.getResponse();
             return response == ACK;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            mAuthTask = null;
            if (!success) {
                Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conv);
        doBindService();

        isPlaying = getIntent().getBooleanExtra("isPlaying", false);
        playButton = (ImageButton) findViewById(R.id.playButton);
        pauseButton = (ImageButton) findViewById(R.id.pauseButton);
        playButton.setVisibility(isPlaying ? View.INVISIBLE : View.VISIBLE);
        pauseButton.setVisibility(isPlaying ? View.VISIBLE : View.INVISIBLE);

        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        doUnbindService();
    }

    public void sendMessage(View view) {
        mButton = (ImageButton) view;
        int id = mButton.getId();

        mAuthTask = new InputTask();
        mAuthTask.execute(buttonDescToByte(mButton.getContentDescription().toString()));

        if (id == R.id.closeButton) {
            finish();
        }
        if (id == R.id.playButton || id == R.id.pauseButton || (id == R.id.nextButton && !isPlaying))
            crossfadePlayPause();
    }

    private byte buttonDescToByte(String desc) {
        try {
            return getClass().getField(desc).getByte(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void crossfadePlayPause() {
        isPlaying = !isPlaying;
        final View toHide = isPlaying ? playButton : pauseButton;
        final View toAppear = isPlaying ? pauseButton : playButton;

        final float fullScale = toHide.getScaleX();
        toAppear.setAlpha(0f);
        toAppear.setScaleX(0f);
        toAppear.setScaleY(0f);
        toAppear.setVisibility(View.VISIBLE);

        toAppear.animate()
                .alpha(1f)
                .scaleX(fullScale)
                .scaleY(fullScale)
                .setDuration(mShortAnimationDuration)
                .setListener(null);

        toHide.animate()
                .alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        toHide.setVisibility(View.INVISIBLE);
                    }
                });
    }
}
