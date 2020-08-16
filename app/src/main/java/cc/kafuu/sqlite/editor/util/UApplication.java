package cc.kafuu.sqlite.editor.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import cc.kafuu.sqlite.editor.BuildConfig;

public class UApplication {

    /**
     * 获取软件版本
     */
    public static String getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context
                    .getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName + " [" + BuildConfig.BUILD_TIME + "]";
        } catch (PackageManager.NameNotFoundException e) {
            return "null";
        }
    }
}
