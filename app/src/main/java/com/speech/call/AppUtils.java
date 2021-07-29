/*
 * Copyright 2019 ~ https://github.com/braver-tool
 */

package com.speech.call;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class AppUtils {

    public static final int MANDATORY_AUDIO_PERMISSION_REQ_CODE = 1212;
    public static final int MANDATORY_CONTACT_PERMISSION_REQ_CODE = 1313;
    public static final String CONTACT_SYNC_DATE = "contact_sync_date";
    public static final String IS_CONTACT_SORTED = "is_contact_sorted";
    public static SimpleDateFormat ymd_date_format = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private static void checkPermissionGrant(Context context, ArrayList<String> permissionList, String permission) {
        if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(permission);
        }
    }

    public static boolean isAudioPermissionGranted(Context context) {
        boolean status = true;
        ArrayList<String> locationPermissionList = new ArrayList<>();
        checkPermissionGrant(context, locationPermissionList, Manifest.permission.RECORD_AUDIO);
        if (locationPermissionList.size() > 0) {
            status = false;
        }
        return status;
    }

    public static void showAudioPermissionRequestDialog(Context context) {
        ArrayList<String> locationPermissionList = new ArrayList<>();
        checkPermissionGrant(context, locationPermissionList, Manifest.permission.RECORD_AUDIO);
        if (locationPermissionList.size() > 0) {
            ActivityCompat.requestPermissions((Activity) context, locationPermissionList.toArray(new String[locationPermissionList.size()]), MANDATORY_AUDIO_PERMISSION_REQ_CODE);
        }
    }

    public static boolean isContactPermissionGranted(Context context) {
        boolean status = true;
        ArrayList<String> locationPermissionList = new ArrayList<>();
        checkPermissionGrant(context, locationPermissionList, Manifest.permission.READ_CONTACTS);
        checkPermissionGrant(context, locationPermissionList, Manifest.permission.CALL_PHONE);
        if (locationPermissionList.size() > 0) {
            status = false;
        }
        return status;
    }

    public static void showContactPermissionRequestDialog(Context context) {
        ArrayList<String> locationPermissionList = new ArrayList<>();
        checkPermissionGrant(context, locationPermissionList, Manifest.permission.READ_CONTACTS);
        checkPermissionGrant(context, locationPermissionList, Manifest.permission.CALL_PHONE);
        if (locationPermissionList.size() > 0) {
            ActivityCompat.requestPermissions((Activity) context, locationPermissionList.toArray(new String[locationPermissionList.size()]), MANDATORY_CONTACT_PERMISSION_REQ_CODE);
        }
    }

    public static String getCurrentDate() {
        String currentDate;
        SimpleDateFormat timeFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date currentDay = Calendar.getInstance().getTime();
        currentDate = timeFormat.format(currentDay);
        return currentDate;
    }

    public static String calendarAddedDays(String strDate, SimpleDateFormat simpleDateFormat, int count) {
        Date date = null;
        try {
            date = simpleDateFormat.parse(strDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, count);
        return simpleDateFormat.format(calendar.getTime());
    }
}
