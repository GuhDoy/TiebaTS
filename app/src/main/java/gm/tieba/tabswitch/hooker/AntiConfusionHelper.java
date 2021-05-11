package gm.tieba.tabswitch.hooker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import org.jf.dexlib.ClassDataItem;
import org.jf.dexlib.ClassDefItem;
import org.jf.dexlib.DexFile;
import org.jf.util.IndentingWriter2;
import org.jf.util.Parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.dao.Preferences;
import gm.tieba.tabswitch.dao.Rule;

public class AntiConfusionHelper {
    static List<String> matcherList = new ArrayList<>();

    static {
        //TSPreference
        matcherList.add("Lcom/baidu/tieba/R$id;->black_address_list:I");
        //TSPreferenceHelper
        matcherList.add("Lcom/baidu/tieba/R$layout;->dialog_bdalert:I");
        matcherList.add("\"can not be call not thread! trace = \"");
        //Purify
        Collections.addAll(matcherList, getPurifyMatchers());
        //PurifyEnter
        matcherList.add("Lcom/baidu/tieba/R$id;->square_background:I");//吧广场
        matcherList.add("Lcom/baidu/tieba/R$id;->create_bar_container:I");//创建自己的吧
        //PurifyMy
        Collections.addAll(matcherList, getPurifyMyMatchers());
        //CreateView
        matcherList.add("Lcom/baidu/tieba/R$id;->navigationBarGoSignall:I");
        //Ripple
        matcherList.add("Lcom/baidu/tieba/R$layout;->new_sub_pb_list_item:I");
        //ThreadStore
        matcherList.add("\"c/f/post/threadstore\"");
        //NewSub
        matcherList.add("Lcom/baidu/tieba/R$id;->subpb_head_user_info_root:I");
        //MyAttention
        // matcherList.add("Lcom/baidu/tieba/R$layout;->person_list_item:I");
        //StorageRedirect
        matcherList.add("0x4197d783fc000000L");
        //ForbidGesture
        matcherList.add("Lcom/baidu/tieba/R$id;->new_pb_list:I");
    }

    public static String[] getPurifyMatchers() {
        return new String[]{"\"c/s/splashSchedule\"",//旧启动广告
                "Lcom/baidu/tieba/recapp/lego/model/AdCard;-><init>(Lorg/json/JSONObject;)V",//卡片广告
                "\"pic_amount\"",//图片广告
                "\"key_frs_dialog_ad_last_show_time\"",//吧推广弹窗
                "Lcom/baidu/tieba/R$id;->frs_ad_banner:I",//吧推广横幅
                "Lcom/baidu/tieba/R$layout;->pb_child_title:I"};//视频相关推荐
    }

    public static String[] getPurifyMyMatchers() {
        return new String[]{"Lcom/baidu/tieba/R$drawable;->icon_pure_topbar_store44_svg:I",//商店
                "Lcom/baidu/tieba/R$id;->function_item_bottom_divider:I",//分割线
                "\"https://tieba.baidu.com/mo/q/duxiaoman/index?noshare=1\""};//我的ArrayList
    }

    public static List<String> getRulesLost() {
        List<String> lostList = new ArrayList<>(matcherList);
        for (int i = 0; i < lostList.size(); i++) {
            if (Rule.isRuleFound(lostList.get(i))) {
                lostList.remove(i);
                i--;
            }
        }
        return lostList;
    }

    public static boolean isVersionChanged(Context context) {
        SharedPreferences tsConfig = context.getSharedPreferences("TS_config", Context.MODE_PRIVATE);
        return !tsConfig.getString("anti-confusion_version", "unknown").equals(getTbVersion(context));
    }

    public static boolean isDexChanged(Context context) {
        try {
            bin.zip.ZipFile zipFile = new bin.zip.ZipFile(new File(context.getPackageResourcePath()));
            byte[] bytes = new byte[32];
            zipFile.getInputStream(zipFile.getEntry("classes.dex")).read(bytes);
            DexFile.calcSignature(bytes);
            return Arrays.hashCode(bytes) != Preferences.getSignature();
        } catch (IOException e) {
            XposedBridge.log(e);
        }
        return false;
    }

    @SuppressLint("ApplySharedPref")
    public static void saveAndRestart(Activity activity, String value, Class<?> springboardActivity) {
        SharedPreferences.Editor editor = activity.getSharedPreferences("TS_config", Context.MODE_PRIVATE).edit();
        editor.putString("anti-confusion_version", value);
        editor.commit();
        if (springboardActivity != null) {
            XposedHelpers.findAndHookMethod(springboardActivity, "onCreate", Bundle.class, new XC_MethodHook() {
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(intent);
                    }
                    System.exit(0);
                }
            });
            Intent intent = new Intent(activity, springboardActivity);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
        } else {
            Intent intent = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            }
            System.exit(0);
        }
    }

    static void searchAndSave(ClassDefItem classItem, int type, SQLiteDatabase db) throws IOException {
        ClassDataItem.EncodedMethod[] methods = null;
        try {
            if (type == 0) methods = classItem.getClassData().getDirectMethods();
            else if (type == 1) methods = classItem.getClassData().getVirtualMethods();
        } catch (NullPointerException e) {
            return;
        }
        for (ClassDataItem.EncodedMethod method : methods) {
            Parser parser = new Parser(method.codeItem);
            IndentingWriter2 writer = new IndentingWriter2();
            parser.dump(writer);
            for (int i = 0; i < matcherList.size(); i++) {
                if (writer.getString().contains(matcherList.get(i))) {
                    String clazz = classItem.getClassType().getTypeDescriptor();
                    clazz = clazz.substring(clazz.indexOf("L") + 1, clazz.indexOf(";")).replace("/", ".");
                    db.execSQL("insert into rules(rule,class,method) values(?,?,?)", new Object[]{matcherList.get(i), clazz, method.method.methodName.getStringValue()});
                    return;
                }
            }
        }
    }

    public static String getTbVersion(Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            switch ((Integer) applicationInfo.metaData.get("versionType")) {
                case 3:
                    return pm.getPackageInfo(context.getPackageName(), 0).versionName;
                case 2:
                    return String.valueOf(applicationInfo.metaData.get("grayVersion"));
                case 1:
                    return String.valueOf(applicationInfo.metaData.get("subVersion"));
                default:
                    throw new PackageManager.NameNotFoundException("unknown tb version");
            }
        } catch (PackageManager.NameNotFoundException e) {
            XposedBridge.log(e);
            return "unknown";
        }
    }
}
