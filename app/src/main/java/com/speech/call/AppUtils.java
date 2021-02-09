package com.speech.call;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;


public class AppUtils {

    public static final int MANDATORY_AUDIO_PERMISSION_REQ_CODE = 1212;

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
}
