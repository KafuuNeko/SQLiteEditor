package com.smallcake.sqlite.editor.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

public class FileOperation {
    public static boolean checkPermission(Context context, int requestCode) {
        //READ_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE
        boolean check = (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (check == false) {
            ActivityCompat.requestPermissions(
                    (Activity) context,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    requestCode);
        }
        return check;
    }

    /**
     * 获取sd卡的绝对路径
     * @return String 如果sd卡存在，返回sd卡的绝对路径，否则返回null
     **/
    public static String getSDPath() {
        final String sdcard = Environment.getExternalStorageState();
        if (sdcard.equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            return null;
        }
    }
}
