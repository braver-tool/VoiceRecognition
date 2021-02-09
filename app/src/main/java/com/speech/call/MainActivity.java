package com.speech.call;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.tool.speech.Speech;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView listenerTextView;
    private ImageButton micImageButton;
    private boolean isStart = true;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listenerTextView = findViewById(R.id.listenerTextView);
        micImageButton = findViewById(R.id.micImageButton);
        micImageButton.setOnClickListener(v -> {
            if (isStart) {
                if (AppUtils.isAudioPermissionGranted(this)) {
                    statBackgroundService();
                } else {
                    AppUtils.showAudioPermissionRequestDialog(this);
                }
            } else {
                stopSpeechListener();
            }

        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Speech.init(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(speechRecognizerReceiver);
        } catch (Exception e) {

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == AppUtils.MANDATORY_AUDIO_PERMISSION_REQ_CODE) {
            boolean isAudioPermissionGranted = true;
            for (int grantResult : grantResults) {
                isAudioPermissionGranted &= grantResult == PackageManager.PERMISSION_GRANTED;
            }
            if (isAudioPermissionGranted) {
                // Start function....!!!
                statBackgroundService();
            } else {
                Log.d(TAG, "Permission Denied!");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Method used to start BroadcastReceiver
     */
    private final BroadcastReceiver speechRecognizerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                final String action = intent.getAction();
                if (action != null && action.equals(AppService.ACTION_SPEECH_RECOGNIZER_RESULT)) {
                    if (intent.getExtras() != null && intent.getStringExtra(AppService.RESPONSE_STATUS) != null) {
                        String result = intent.getStringExtra(AppService.RESPONSE_STATUS);
                        Log.d("##@@speechRecoReceiver", "-------->" + result);
                        Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
                    }
                } else if (action != null && action.equals(AppService.ACTION_SPEECH_RECOGNIZER_DESTROY)) {
                    stopSpeechListener();
                    Speech.init(context);
                }
            } catch (Exception e) {
                e.getMessage();
            }
        }
    };

    /**
     * Method used to start BackgroundService
     */
    private void statBackgroundService() {
        try {
            if (!Speech.getInstance().isListening()) {
                isStart = false;
                listenerTextView.setText("Stop Listening...");
                micImageButton.setBackground(getResources().getDrawable(R.drawable.round_inactive));
                startService(new Intent(this, AppService.class));
                registerReceiver(speechRecognizerReceiver, intentActionsForReceiver());
            }
        } catch (IllegalStateException stateException) {
            Speech.init(MainActivity.this);
            micImageButton.performClick();
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * @return - IntentFilter
     */
    private static IntentFilter intentActionsForReceiver() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppService.ACTION_SPEECH_RECOGNIZER_RESULT);
        intentFilter.addAction(AppService.ACTION_SPEECH_RECOGNIZER_DESTROY);
        return intentFilter;
    }

    /**
     * Method used to stop BackgroundService
     */
    private void stopSpeechListener() {
        isStart = true;
        listenerTextView.setText("Start Listening...");
        micImageButton.setBackground(getResources().getDrawable(R.drawable.round_active));
        AppService.stopOurService(this);
    }
}