package com.example.ronmad.foobarremote;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class FoobarActivity extends AppCompatActivity {

    public static final int LAUNCH = 1;
    public static final int PLAY_PAUSE = 2;
    public static final int NEXT = 3;
    public static final int PREV = 4;
    public static final int STOP = 5;
    public static final int RANDOM = 6;
    public static final int VOL_UP = 7;
    public static final int VOL_DOWN = 8;
    public static final int DEFAULT = 9;
    public static final int REPEAT_PLAYLIST = 10;
    public static final int REPEAT_TRACK = 11;
    public static final int ORDER_RANDOM = 12;
    public static final int SHUFFLE_TRACKS = 13;
    public static final int SHUFFLE_ALBUMS = 14;
    public static final int SHUFFLE_FOLDERS = 15;
    public static final int[] ORDER = {
            DEFAULT,
            REPEAT_PLAYLIST,
            REPEAT_TRACK,
            ORDER_RANDOM,
            SHUFFLE_TRACKS,
            SHUFFLE_ALBUMS,
            SHUFFLE_FOLDERS
    };
    public static final int DISC = 16;
    public static final int ACK = 17;

    SocketService mBoundService;

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
        if (mBoundService == null)
            bindService(new Intent(this, SocketService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    private void doUnbindService() {
        if (mBoundService != null) {
            unbindService(mConnection);
            mBoundService = null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conv);

        doBindService();

        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null)
            actionBar.setDisplayHomeAsUpEnabled(true);

        isPlaying = getIntent().getBooleanExtra(getString(R.string.isplaying_intent_extra_key), false);
        playButton = (ImageButton) findViewById(R.id.playButton);
        pauseButton = (ImageButton) findViewById(R.id.pauseButton);
        playButton.setVisibility(isPlaying ? View.INVISIBLE : View.VISIBLE);
        pauseButton.setVisibility(isPlaying ? View.VISIBLE : View.INVISIBLE);

        setupSpinner();
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

    private void setupSpinner() {
        Spinner orderSpinner = (Spinner) findViewById(R.id.orderSpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item) {

            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (position == getCount()) {
                    ((TextView)v.findViewById(android.R.id.text1)).setText("");
                    ((TextView)v.findViewById(android.R.id.text1)).setHint(getItem(getCount()));
                }

                return v;
            }

            @Override
            public int getCount() {
                return super.getCount() - 1;
            }

        };

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.addAll(getResources().getStringArray(R.array.playback_order));

        orderSpinner.setAdapter(adapter);
        orderSpinner.setSelection(adapter.getCount());

        orderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i < ORDER.length)
                    send(ORDER[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void send(int message) {
        if (mBoundService.sendMessage(message) != ACK) {
            Toast.makeText(getApplicationContext(), "Connection lost", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public void sendMessage(View view) {
        ImageButton button = (ImageButton) view;
        int id = button.getId();

        send(buttonDescToInt(button.getContentDescription().toString()));

        if (id == R.id.playButton || id == R.id.pauseButton ||
                ((id == R.id.nextButton || id == R.id.randomButton) && !isPlaying) ||
                id ==R.id.stopButton && isPlaying)
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

        toHide.setClickable(false);
        toAppear.setClickable(false);

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
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        toAppear.setClickable(true);
                    }
                });

        toHide.animate()
                .alpha(0f)
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        toHide.setVisibility(View.INVISIBLE);
                        toHide.setClickable(true);
                    }
                });
    }
}
