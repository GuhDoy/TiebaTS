package gm.tieba.tabswitch.hooker.add;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.BaseHooker;
import gm.tieba.tabswitch.IHooker;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.dao.AcRules;
import gm.tieba.tabswitch.util.ReflectUtils;

public class CreateView extends BaseHooker implements IHooker {
    public void hook() throws Throwable {
        AcRules.findRule(sRes.getString(R.string.CreateView), (AcRules.Callback) (rule, clazz, method) ->
                XposedHelpers.findAndHookMethod(clazz, sClassLoader, method, Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        ImageView signButton = (ImageView) ReflectUtils.getObjectField(param.thisObject, ImageView.class);
                        Activity activity = (Activity) signButton.getContext();
                        RelativeLayout parent = (RelativeLayout) signButton.getParent();
                        RelativeLayout.LayoutParams signButtonLayoutParams =
                                (RelativeLayout.LayoutParams) signButton.getLayoutParams();
                        signButtonLayoutParams.leftMargin = 0;
                        signButton.setLayoutParams(signButtonLayoutParams);
                        //historyButton
                        final ImageView historyDrawable = new ImageView(activity);
                        historyDrawable.setImageResource(
                                ReflectUtils.getDrawableId("icon_mask_wo_list_history24_svg"));
                        final RelativeLayout historyButton = new RelativeLayout(activity);
                        RelativeLayout.LayoutParams historyLayoutParams =
                                new RelativeLayout.LayoutParams(signButtonLayoutParams.height,
                                        RelativeLayout.LayoutParams.MATCH_PARENT);
                        historyLayoutParams.addRule(RelativeLayout.LEFT_OF, signButton.getId());
                        historyButton.setLayoutParams(historyLayoutParams);
                        historyButton.setId(View.generateViewId());
                        historyButton.addView(historyDrawable);
                        RelativeLayout.LayoutParams drawableLayoutParams =
                                (RelativeLayout.LayoutParams) historyDrawable.getLayoutParams();
                        drawableLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                        historyDrawable.setLayoutParams(drawableLayoutParams);
                        parent.addView(historyButton);
                        historyButton.setOnClickListener(view -> {
                            Intent intent = new Intent().setClassName(activity,
                                    "com.baidu.tieba.myCollection.history.PbHistoryActivity");
                            activity.startActivity(intent);
                        });
                        //collectButton
                        final ImageView collectDrawable = new ImageView(activity);
                        collectDrawable.setImageResource(
                                ReflectUtils.getDrawableId("icon_mask_wo_list_collect24_svg"));
                        final RelativeLayout collectButton = new RelativeLayout(activity);
                        RelativeLayout.LayoutParams collectButtonLayoutParams =
                                new RelativeLayout.LayoutParams(signButtonLayoutParams.height,
                                        RelativeLayout.LayoutParams.MATCH_PARENT);
                        collectButtonLayoutParams.addRule(RelativeLayout.LEFT_OF, historyButton.getId());
                        collectButton.setLayoutParams(collectButtonLayoutParams);
                        collectButton.addView(collectDrawable);
                        collectDrawable.setLayoutParams(drawableLayoutParams);
                        parent.addView(collectButton);
                        collectButton.setOnClickListener(view -> {
                            Intent intent = new Intent().setClassName(activity,
                                    "com.baidu.tieba.myCollection.CollectTabActivity");
                            activity.startActivity(intent);
                        });
                    }
                }));
    }
}
