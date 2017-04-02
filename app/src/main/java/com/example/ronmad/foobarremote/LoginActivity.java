package com.example.ronmad.foobarremote;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

public class LoginActivity extends AppCompatActivity {

    private UserLoginTask mAuthTask = null;
    private SocketAddress addr = null;

    // UI references.
    private EditText mIPView;
    private EditText mPortView;
    private View mProgressView;
    private View mLoginFormView;
    private Intent intent;

    private SocketService mBoundService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        intent = new Intent(this, FoobarActivity.class);
        // Set up the login form.
        mIPView = (EditText) findViewById(R.id.ip);
        mPortView = (EditText) findViewById(R.id.port);
        mPortView.setOnEditorActionListener((textView, id, keyEvent) -> {
            if (id == R.id.login || id == EditorInfo.IME_NULL) {
                attemptLogin();
                return true;
            }
            return false;
        });

        Button mIPSignInButton = (Button) findViewById(R.id.ip_sign_in_button);
        mIPSignInButton.setOnClickListener(view -> attemptLogin());

        CheckBox isPlayingBox = (CheckBox) findViewById(R.id.checkBox);
        isPlayingBox.setOnCheckedChangeListener((compoundButton, b) -> intent.putExtra(getString(R.string.isplaying_intent_extra_key), b));

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {

        // Reset errors.
        mIPView.setError(null);
        mPortView.setError(null);

        // Store values at the time of the login attempt.
        String ip = mIPView.getText().toString();
        String port = mPortView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid IP and port.
        if (TextUtils.isEmpty(ip)) {
            mIPView.setError(getString(R.string.error_field_required));
            focusView = mIPView;
            cancel = true;
        } else if (!isIPValid(ip)) {
            mIPView.setError(getString(R.string.error_invalid_ip));
            focusView = mIPView;
            cancel = true;
        }
        if (TextUtils.isEmpty(port) || !isPortValid(port)) {
            mPortView.setError(getString(R.string.error_invalid_port));
            focusView = mPortView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            addr = new InetSocketAddress(ip, Integer.parseInt(port));
            mAuthTask = new UserLoginTask();
            mAuthTask.execute();
        }
    }

    private boolean isIPValid(String ip) {
        int counter = 0;
        for (int i = 0; i < ip.length(); i++)
            if (ip.charAt(i) == '.')
                counter++;
        return counter == 3;
    }

    private boolean isPortValid(String port) {
        return Integer.parseInt(port) > 1024;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });

    }

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
        if(mBoundService == null)
            bindService(new Intent(this, SocketService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    private void doUnbindService() {
        if (mBoundService == null) return;
        unbindService(mConnection);
        mBoundService = null;
    }

    private class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        Intent serviceIntent;

        @Override
        protected Boolean doInBackground(Void... params) {
            serviceIntent = new Intent(getApplicationContext(), SocketService.class);
            serviceIntent.putExtra(getString(R.string.socketaddress_intent_extra_key), addr);
            startService(serviceIntent);
            doBindService();
            synchronized (LoginActivity.class) {
                try {
                    LoginActivity.class.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return mBoundService != null && mBoundService.isConnected();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgress(true);
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);
            if (success) {
                startActivity(intent);
            } else {
                stopService(serviceIntent);
                doUnbindService();
                mPortView.requestFocus();
                Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        doUnbindService();
    }
}
