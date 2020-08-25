package chat.dim.sechat.push;

import android.app.Application;
import android.content.Context;

import java.util.Random;

import chat.dim.sechat.push.jpush.JPushManager;
import chat.dim.sechat.push.xiaomipush.XiaomiPushManager;
import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.api.JPushMessage;

public class PushManager {

    private Context context;

    private static class Holder {
        private static PushManager instance = new PushManager();
    }

    public static PushManager getInstance() {
        return PushManager.Holder.instance;
    }

    public void init(Application app, boolean isDebug) {
        context = app;
        if (RomUtil.isMiui()){
            XiaomiPushManager.getInstance().init(app,isDebug);
        }
       JPushManager.getInstance().init(app,isDebug);
    }

    public void setAlias(String alias) {
        verifyContext();
        //限制：alias 命名长度限制为 40 字节。（判断长度需采用 UTF-8 编码）
        if (RomUtil.isMiui()){
            XiaomiPushManager.getInstance().setAlias(alias);
        }
        JPushManager.getInstance().setAlias(alias);
    }

    private void verifyContext() {
        if (context == null) {
            throw new RuntimeException("pelease init first");
        }
    }

    public void deleteAlias(String alias) {
        verifyContext();
        if (RomUtil.isMiui()){
            XiaomiPushManager.getInstance().deleteAlias(alias);
        }
        JPushManager.getInstance().deleteAlias(alias);
    }
}
