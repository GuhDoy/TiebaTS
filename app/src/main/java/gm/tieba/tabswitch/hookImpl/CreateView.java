package gm.tieba.tabswitch.hookImpl;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.Hook;

public class CreateView extends Hook {
    public static void hook(ClassLoader classLoader) throws Throwable {
        XposedHelpers.findAndHookMethod("com.baidu.tieba.tblauncher.MainTabActivity", classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                for (int i = 0; i < ruleMapList.size(); i++) {
                    Map<String, String> map = ruleMapList.get(i);
                    if (Objects.equals(map.get("rule"), "Lcom/baidu/tieba/R$id;->navigationBarGoSignall:I"))
                        XposedHelpers.findAndHookMethod(map.get("class"), classLoader, map.get("method"), Bundle.class, new XC_MethodHook() {
                            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                Field[] fields = param.thisObject.getClass().getDeclaredFields();
                                for (Field field : fields) {
                                    field.setAccessible(true);
                                    if (field.get(param.thisObject) instanceof ImageView) {
                                        ImageView signButton = (ImageView) field.get(param.thisObject);
                                        RelativeLayout parent = (RelativeLayout) Objects.requireNonNull(signButton).getParent();
                                        RelativeLayout.LayoutParams signButtonLayoutParams = (RelativeLayout.LayoutParams) signButton.getLayoutParams();
                                        signButtonLayoutParams.leftMargin = 0;
                                        signButton.setLayoutParams(signButtonLayoutParams);
                                        //historyButton
                                        final ImageView historyDrawable = new ImageView(activity);
                                        historyDrawable.setImageResource(classLoader.loadClass("com.baidu.tieba.R$drawable").getField("icon_mask_wo_list_history24_svg").getInt(null));
                                        final RelativeLayout historyButton = new RelativeLayout(activity);
                                        RelativeLayout.LayoutParams historyLayoutParams = new RelativeLayout.LayoutParams(signButtonLayoutParams.height, RelativeLayout.LayoutParams.MATCH_PARENT);
                                        historyLayoutParams.addRule(RelativeLayout.LEFT_OF, signButton.getId());
                                        historyButton.setLayoutParams(historyLayoutParams);
                                        historyButton.setId(View.generateViewId());
                                        historyButton.addView(historyDrawable);
                                        RelativeLayout.LayoutParams drawableLayoutParams = (RelativeLayout.LayoutParams) historyDrawable.getLayoutParams();
                                        drawableLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                                        historyDrawable.setLayoutParams(drawableLayoutParams);
                                        parent.addView(historyButton);
                                        historyButton.setOnClickListener(view -> {
                                            Intent intent = new Intent().setClassName(activity, "com.baidu.tieba.myCollection.history.PbHistoryActivity");
                                            activity.startActivity(intent);
                                        });
                                        //collectButton
                                        final ImageView collectDrawable = new ImageView(activity);
                                        collectDrawable.setImageResource(classLoader.loadClass("com.baidu.tieba.R$drawable").getField("icon_mask_wo_list_collect24_svg").getInt(null));
                                        final RelativeLayout collectButton = new RelativeLayout(activity);
                                        RelativeLayout.LayoutParams collectButtonLayoutParams = new RelativeLayout.LayoutParams(signButtonLayoutParams.height, RelativeLayout.LayoutParams.MATCH_PARENT);
                                        collectButtonLayoutParams.addRule(RelativeLayout.LEFT_OF, historyButton.getId());
                                        collectButton.setLayoutParams(collectButtonLayoutParams);
                                        collectButton.addView(collectDrawable);
                                        collectDrawable.setLayoutParams(drawableLayoutParams);
                                        parent.addView(collectButton);
                                        collectButton.setOnClickListener(view -> {
                                            Intent intent = new Intent().setClassName(activity, "com.baidu.tieba.myCollection.CollectTabActivity");
                                            activity.startActivity(intent);
                                        });
                                        return;
                                    }
                                }
                            }
                        });
                }
            }
        });
    }
}