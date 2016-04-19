package com.mono;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {

    private static PermissionManager instance;

    private PermissionManager() {}

    public static PermissionManager getInstance() {
        if (instance == null) {
            instance = new PermissionManager();
        }

        return instance;
    }

    private static String[] filterPermissions(Activity activity, String[] permissions) {
        List<String> requests = new ArrayList<>();

        for (String permission : permissions) {
            int status = ContextCompat.checkSelfPermission(activity, permission);
            if (status != PackageManager.PERMISSION_GRANTED) {
                requests.add(permission);
            }
        }

        return requests.toArray(new String[requests.size()]);
    }

    public static void checkPermissions(Activity activity, int requestCode) {
        try {
            PackageManager manager = activity.getPackageManager();
            PackageInfo info = manager.getPackageInfo(activity.getPackageName(),
                PackageManager.GET_PERMISSIONS);

            if (info.requestedPermissions != null) {
                String[] permissions = filterPermissions(activity, info.requestedPermissions);
                if (permissions.length > 0) {
                    ActivityCompat.requestPermissions(activity, permissions, requestCode);
                }
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void onRequestPermissionsResult(int requestCode, String permissions[],
            int[] grantResults) {
        if (grantResults.length > 0) {

        }
    }
}
