package chat.dim.sechat.jpush;

import android.app.Application;
import android.app.TaskInfo;
import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import chat.dim.sechat.BuildConfig;
import cn.jpush.android.api.JPushInterface;
import cn.jpush.android.api.TagAliasCallback;

public class JPushManager {

    private Context context;
    private Map<String, Integer> aliasMap = new HashMap<>();

    private JPushManager() {

    }

    private static class Holder {
        private static JPushManager instance = new JPushManager();
    }

    public static JPushManager getInstance() {
        return Holder.instance;
    }

    public void init(Application app, boolean isDebug) {
        context = app;
        JPushInterface.init(app);
        JPushInterface.setDebugMode(isDebug);
    }

    public void stopPush(){
        if (context == null) {
            throw new RuntimeException("pelease init first");
        }
      //  JPushInterface.stopPush(context);
    }

    public void resumePush(){
        if (context == null) {
            throw new RuntimeException("pelease init first");
        }
      //  JPushInterface.resumePush(context);
    }

    public void setAlias(String alias) {
        if (context == null) {
            throw new RuntimeException("pelease init first");
        }
        //限制：alias 命名长度限制为 40 字节。（判断长度需采用 UTF-8 编码）
        int c = new Random().nextInt();
        aliasMap.put(alias, c);
        JPushInterface.setAlias(context, alias, new TagAliasCallback() {
            @Override
            public void gotResult(int i, String s, Set<String> set) {
                Log.i("jpush",i+" "+s);
            }
        });
    }

    public void deleteAlias(String alias) {
        if (context == null) {
            throw new RuntimeException("pelease init first");
        }
        Integer integer = aliasMap.get(alias);
        if (integer != null) {
            JPushInterface.deleteAlias(context, integer);
        }
    }

    public void updateAlias(String alias) {
        deleteAlias(alias);
        setAlias(alias);
    }
}
