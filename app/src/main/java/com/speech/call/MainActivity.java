/*
 * Copyright 2019 ~ https://github.com/braver-tool
 */

package com.speech.call;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.util.Patterns;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.speech.call.localdb.LocalDataBaseHelper;
import com.tool.speech.Speech;
import com.tool.speech.SupportedLanguagesListener;
import com.tool.speech.UnsupportedReason;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.speech.call.AppUtils.CONTACT_SYNC_DATE;
import static com.speech.call.AppUtils.IS_CONTACT_SORTED;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private TextView listenerTextView;
    private ImageButton micImageButton;
    private boolean isStart = true;
    private LocalDataBaseHelper dataBaseHelper;
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
                        Log.d("##@@speechRecReceiver", "-------->" + result);
                        if (result.toLowerCase().equals("hello android")) {
                            Speech.getInstance().say("Ready to make a call");
                        } else {
                            String[] callerName = result.split(" ");
                            if (callerName.length > 1 && callerName[0].toLowerCase().equals("call")) {
                                Toast.makeText(MainActivity.this, "Make call to " + callerName[1], Toast.LENGTH_SHORT).show();
                                ContactModel contactModel = dataBaseHelper.getMatchedContact(callerName[1]);
                                makeCall(contactModel);
                            } else {
                                Speech.getInstance().say("Contact name is mismatched");
                            }
                        }
                    }
                } else if (action != null && action.equals(AppService.ACTION_SPEECH_RECOGNIZER_DESTROY)) {
                    stopSpeechListener();
                    Speech.init(context);
                }
            } catch (Exception e) {
                Log.d("##@@speechRecReceiver", "-------->" + e.getMessage());
            }
        }
    };


    private final List<ContactModel> contactModelList = new ArrayList<>();
    private PreferencesManager preferencesManager;

    /**
     * @return - IntentFilter
     */
    private static IntentFilter intentActionsForReceiver() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppService.ACTION_SPEECH_RECOGNIZER_RESULT);
        intentFilter.addAction(AppService.ACTION_SPEECH_RECOGNIZER_DESTROY);
        return intentFilter;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listenerTextView = findViewById(R.id.listenerTextView);
        micImageButton = findViewById(R.id.micImageButton);
        preferencesManager = PreferencesManager.Companion.getInstance(MainActivity.this);
        dataBaseHelper = new LocalDataBaseHelper(getApplicationContext());
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
        syncContact();

    }

    @Override
    protected void onResume() {
        super.onResume();
        Speech.init(this);
        onSetSpeechToTextLanguage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(speechRecognizerReceiver);
        } catch (Exception e) {

        }
        if (dataBaseHelper != null) {
            dataBaseHelper.closeDB();
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
        } else if (requestCode == AppUtils.MANDATORY_CONTACT_PERMISSION_REQ_CODE) {
            boolean isContactPermissionGranted = true;
            for (int grantResult : grantResults) {
                isContactPermissionGranted &= grantResult == PackageManager.PERMISSION_GRANTED;
            }
            if (isContactPermissionGranted) {
                getContactList();
            } else {
                Log.d(TAG, "Permission Denied!");
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Method used to Sync User's contacts by every day(Once at one day) {@link #GetContactsIntoArrayList()}
     */
    private void syncContact() {
        String cDate = AppUtils.getCurrentDate();
        String contactLastSyncDate = preferencesManager.getStringValue(CONTACT_SYNC_DATE);
        if (contactLastSyncDate.isEmpty() || cDate.equals(contactLastSyncDate)) {
            preferencesManager.setStringValue(CONTACT_SYNC_DATE, AppUtils.calendarAddedDays(cDate, AppUtils.ymd_date_format, 1));
            preferencesManager.setBooleanValue(IS_CONTACT_SORTED, false);
            GetContactsIntoArrayList();
        }
    }

    /**
     * Checking permission and call {@link #getContactList())} method
     */
    private void GetContactsIntoArrayList() {
        if (AppUtils.isContactPermissionGranted(this)) {
            getContactList();
        } else {
            AppUtils.showContactPermissionRequestDialog(this);
        }
    }

    private void getContactList() {
        List<String> cList = new ArrayList<>();
        if (contactModelList.size() == 0) {
            Cursor cursor;
            String cName, cNumber;
            cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            while (cursor.moveToNext()) {
                cName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                cNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                cName = cName.replaceAll("[^\\p{L}\\p{M}\\p{N}\\p{P}\\p{Z}\\p{Cf}\\p{Cs}\\s]", "");
                cList.add(cName.concat(",").concat(cNumber));
            }
            cursor.close();
            HashSet<String> hashSet = new HashSet<>();
            hashSet.addAll(cList);
            cList.clear();
            cList.addAll(hashSet);
            for (int i = 0; i < cList.size(); i++) {
                String[] data = cList.get(i).split(",");
                contactModelList.add(new ContactModel(data[0], data[1]));

            }
            dataBaseHelper.insertContactDetailsToDb(preferencesManager, contactModelList);
        }
    }

    /**
     * Method used to start BackgroundService
     */
    private void statBackgroundService() {
        try {
            if (!Speech.getInstance().isListening()) {
                isStart = false;
                listenerTextView.setText("Stop Listening...");
                micImageButton.setBackground(ContextCompat.getDrawable(this, R.drawable.round_inactive));
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
     * Method used to stop BackgroundService
     */
    private void stopSpeechListener() {
        isStart = true;
        listenerTextView.setText("Start Listening...");
        micImageButton.setBackground(ContextCompat.getDrawable(this, R.drawable.round_active));
        AppService.stopOurService(this);
    }

    /**
     * Method used to make a call
     *
     * @param contactModel - ContactModel
     */
    private void makeCall(ContactModel contactModel) {
        //String cName = contactModel.getContactName();
        String cNumber = contactModel.getContactNumber();
        if (Patterns.PHONE.matcher(cNumber.trim()).matches()) {
            Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + cNumber.trim()));
            intent.putExtra("com.android.phone.extra.slot", 0); // 0 For sim 1
            intent.putExtra("simSlot", 0); //0 For sim 1
            startActivity(intent);
        }
    }

    private void onSetSpeechToTextLanguage() {
        Speech.getInstance().getSupportedSpeechToTextLanguages(new SupportedLanguagesListener() {
            @Override
            public void onSupportedLanguages(List<String> supportedLanguages) {
                CharSequence[] items = new CharSequence[supportedLanguages.size()];
                supportedLanguages.toArray(items);

             /*   new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Current language: " + Speech.getInstance().getSpeechToTextLanguage())
                        .setItems(items, (dialogInterface, i) -> {
                            Locale locale;
                            if (Build.VERSION.SDK_INT >= 21) {
                                locale = Locale.forLanguageTag(supportedLanguages.get(i));
                            } else {
                                String[] langParts = supportedLanguages.get(i).split("-");
                                if (langParts.length >= 2) {
                                    locale = new Locale(langParts[0], langParts[1]);
                                } else {
                                    locale = new Locale(langParts[0]);
                                }
                            }

                            Speech.getInstance().setLocale(locale);
                            Toast.makeText(MainActivity.this, "Selected: " + items[i], Toast.LENGTH_LONG).show();
                        })
                        .setPositiveButton("Cancel", null)
                        .create()
                        .show();*/
            }

            @Override
            public void onNotSupported(UnsupportedReason reason) {
                switch (reason) {
                    case GOOGLE_APP_NOT_FOUND:
                        //showSpeechNotSupportedDialog();
                        break;

                    case EMPTY_SUPPORTED_LANGUAGES:
                      /*  new AlertDialog.Builder(MainActivity.this)
                                .setTitle(R.string.set_stt_langs)
                                .setMessage(R.string.no_langs)
                                .setPositiveButton("OK", null)
                                .show();*/
                        break;
                }
            }
        });
    }
}