package chat.dim.sechat.push.xiaomipush;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.os.Process;
import android.util.Log;

import com.xiaomi.channel.commonutils.logger.LoggerInterface;
import com.xiaomi.mipush.sdk.Logger;
import com.xiaomi.mipush.sdk.MiPushClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import chat.dim.sechat.BuildConfig;
import cn.jpush.android.api.JPushInterface;

public class XiaomiPushManager {

    private Context context;
    public static final String APP_ID = BuildConfig.XIAOMI_PUSH_APP_ID;
    public static final String APP_KEY = BuildConfig.XIAOMI_PUSH_APPKEY;
    public static final String TAG = "XiaomiPushManager";

    private XiaomiPushManager() {

    }

    private static class Holder {
        private static XiaomiPushManager instance = new XiaomiPushManager();
    }

    public static XiaomiPushManager getInstance() {
        return Holder.instance;
    }

    public void init(Application app, boolean isDebug) {
        context = app;
        //初始化push推送服务
        if (shouldInit()) {
            MiPushClient.registerPush(context, APP_ID, APP_KEY);
        }
        if (isDebug) {
            //打开Log
            LoggerInterface newLogger = new LoggerInterface() {

                @Override
                public void setTag(String tag) {
                    // ignore
                }

                @Override
                public void log(String content, Throwable t) {
                    Log.d(TAG, content, t);
                }

                @Override
                public void log(String content) {
                    Log.d(TAG, content);
                }
            };
            Logger.setLogger(context, newLogger);
        }
    }


    public void setAlias(String alias) {
        verifyContext();
        MiPushClient.setAlias(context,alias,null);
    }

    private void verifyContext() {
        if (context == null) {
            throw new RuntimeException("pelease init first");
        }
    }

    public void deleteAlias(String alias) {
        verifyContext();
        MiPushClient.unsetAlias(context,alias,null);
    }

    private boolean shouldInit() {
        verifyContext();
        ActivityManager am = ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
        String mainProcessName = context.getApplicationInfo().processName;
        int myPid = Process.myPid();
        for (ActivityManager.RunningAppProcessInfo info : processInfos) {
            if (info.pid == myPid && mainProcessName.equals(info.processName)) {
                return true;
            }
        }
        return false;
    }
}
