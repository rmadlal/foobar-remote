package com.example.ronmad.foobarremote;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

public class FoobarActivity extends AppCompatActivity {

    public static final int LAUNCH = 1;
    public static final int PLAY_PAUSE = 2;
    public static final int NEXT = 3;
    public static final int PREV = 4;
    public static final int VOL_UP = 5;
    public static final int VOL_DOWN = 6;
    public static final int DISC = 7;
    public static final int ACK = 8;

    SocketService mBoundService;
    private boolean isBound;

    private ImageButton playButton;
    private ImageButton pauseButton;
    private int mShortAnimationDuration;
    private boolean isPlaying;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBoundService = ((SocketService.LocalBinder)service).getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBoundService = null;
            isBound = false;
        }
    };

    private void doBindService() {
        if (!isBound)
            bindService(new Intent(this, SocketService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    private void doUnbindService() {
        if (isBound) {
            unbindService(mConnection);
            isBound = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conv);

        doBindService();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        isPlaying = getIntent().getBooleanExtra(getString(R.string.isplaying_intent_extra_key), false);
        playButton = (ImageButton) findViewById(R.id.playButton);
        pauseButton = (ImageButton) findViewById(R.id.pauseButton);
        playButton.setVisibility(isPlaying ? View.INVISIBLE : View.VISIBLE);
        pauseButton.setVisibility(isPlaying ? View.VISIBLE : View.INVISIBLE);

        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    public boolean onSupportNavigateUp() {
        mBoundService.sendMessage(DISC);
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, SocketService.class));
        doUnbindService();
    }

    public void sendMessage(View view) {
        ImageButton button = (ImageButton) view;
        int id = button.getId();

        int response = mBoundService.sendMessage(buttonDescToInt(button.getContentDescription().toString()));
        if (response == -1) {
            Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
            finish();
        }

        if (id == R.id.playButton || id == R.id.pauseButton || (id == R.id.nextButton && !isPlaying))
            crossfadePlayPause();
    }

    private int buttonDescToInt(String desc) {
        try {
            return getClass().getField(desc).getInt(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private void crossfadePlayPause() {
        isPlaying = !isPlaying;
        View toHide = isPlaying ? playButton : pauseButton;
        View toAppear = isPlaying ? pauseButton : playButton;

        float fullScale = toHide.getScaleX();
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
