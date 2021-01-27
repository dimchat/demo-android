//package chat.dim.sechat.push.jpush;
//
//import android.app.Application;
//import android.content.Context;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Random;
//
//import cn.jpush.android.api.JPushInterface;
//
//public class JPushManager {
//
//    private Context context;
//    private Map<String, Integer> aliasMap = new HashMap<>();
//
//    private JPushManager() {
//
//    }
//
//    private static class Holder {
//        private static JPushManager instance = new JPushManager();
//    }
//
//    public static JPushManager getInstance() {
//        return Holder.instance;
//    }
//
//    public void init(Application app, boolean isDebug) {
//        context = app;
//        JPushInterface.init(app);
//        JPushInterface.setDebugMode(isDebug);
//    }
//
//    public void stopPush(){
//        verifyContext();
//          JPushInterface.stopPush(context);
//    }
//
//    public void resumePush(){
//        verifyContext();
//          JPushInterface.resumePush(context);
//    }
//
//    public void setAlias(String alias) {
//        verifyContext();
//        //限制：alias 命名长度限制为 40 字节。（判断长度需采用 UTF-8 编码）
//        int c = new Random().nextInt();
//        aliasMap.put(alias, c);
//        JPushInterface.setAlias(context,c,alias);
//    }
//
//    private void verifyContext() {
//        if (context == null) {
//            throw new RuntimeException("pelease init first");
//        }
//    }
//
//    public void deleteAlias(String alias) {
//        verifyContext();
//        Integer integer = aliasMap.get(alias);
//        if (integer != null) {
//            JPushInterface.deleteAlias(context, integer);
//        }
//    }
//
//    public void updateAlias(String alias) {
//        deleteAlias(alias);
//        setAlias(alias);
//    }
//}
