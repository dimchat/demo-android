package chat.dim.sechat.push;

import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RomUtil {
    private static final String TAG = "Rom";
    private static final  String ROM_MIUI = "MIUI";
    private static final  String ROM_EMUI = "EMUI";
    private static final  String ROM_FLYME = "FLYME";
    private  static final  String ROM_OPPO = "OPPO";
    private  static final  String ROM_SMARTISAN = "SMARTISAN";
    private  static final  String ROM_VIVO = "VIVO";
    private static final  String ROM_QIKU = "QIKU";
    private static final  String KEY_VERSION_MIUI = "ro.miui.ui.version.name";
    private static final  String KEY_VERSION_EMUI = "ro.build.version.emui";
    private static final  String KEY_VERSION_OPPO = "ro.build.version.opporom";
    private static final  String KEY_VERSION_SMARTISAN = "ro.smartisan.version";
    private static final  String KEY_VERSION_VIVO = "ro.vivo.os.version";
    private static String sName = null;
    private static String sVersion= null;

    //华为
    public static boolean isEmui(){
        return check(ROM_EMUI);
    }
    //华为
    public static boolean isMiui(){
        return check(ROM_MIUI);
    }

    //华为
    public static boolean isVivo(){
        return check(ROM_VIVO);
    }
    //华为
    public static boolean isOppo(){
        return check(ROM_OPPO);
    }
    //华为
    public static boolean isFlyme(){
        return check(ROM_FLYME);
    }
    public static boolean isSmartisan(){
        return check(ROM_SMARTISAN);
    }

    //360手机
    public static boolean is360(){
        return check(ROM_QIKU) || check("360");
    }

    public static String getName(){
        if (sName ==null){
             check("");
        }
        return sName;
    }

    public static String getVersion(){
        if (sVersion == null){
            check("");
        }
        return sVersion;
    }

    public static boolean check(String rom) {
        if (sName != null) {
            return sName.equals(rom);
        }
        if (!TextUtils.isEmpty(sVersion = getProp(KEY_VERSION_MIUI))) {
            sName = ROM_MIUI;
        } else if (!TextUtils.isEmpty(sVersion =getProp(KEY_VERSION_EMUI))) {
            sName = ROM_EMUI;
        } else if (!TextUtils.isEmpty(sVersion =getProp(KEY_VERSION_OPPO))) {
            sName = ROM_OPPO;
        } else if (!TextUtils.isEmpty(sVersion =getProp(KEY_VERSION_VIVO))) {
            sName = ROM_VIVO;
        } else if (!TextUtils.isEmpty(sVersion =getProp(KEY_VERSION_SMARTISAN))) {
            sName = ROM_SMARTISAN;
        } else {
            sVersion = Build.DISPLAY;
            if (!TextUtils.isEmpty(sVersion)&&sVersion.toUpperCase().contains(ROM_FLYME)) {
                sName = ROM_FLYME;
            } else {
                sVersion = Build.UNKNOWN;
                sName = Build.MANUFACTURER.toUpperCase();
            }
        }
        return sName.equals(rom);
    }

   private static String getProp(String name) {
       String line = null;
       BufferedReader input = null;
        try {
            Process p = Runtime.getRuntime().exec("getprop $name");
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            Log.e(TAG, "Unable to read prop $name", ex);
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch ( IOException e ) {
                    e.printStackTrace();
                }
            }
        }
        return line;
    }
}
