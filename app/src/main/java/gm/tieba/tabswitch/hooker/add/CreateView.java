package gm.tieba.tabswitch.hooker.add;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.ReflectUtils;

public class CreateView extends XposedContext implements IHooker {
    public void hook() throws Throwable {
        var method = ReflectUtils.findFirstMethodByExactType(
                "com.baidu.tieba.enterForum.home.EnterForumTabFragment", View.class, Bundle.class
        );
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                var controller = ReflectUtils.getObjectField(param.thisObject, 1);
                var signButton = (ImageView) ReflectUtils.getObjectField(controller, ImageView.class);
                var activity = (Activity) XposedHelpers.callMethod(param.thisObject, "requireActivity");
                var parent = (RelativeLayout) signButton.getParent();
                var signButtonLayoutParams = (RelativeLayout.LayoutParams) signButton.getLayoutParams();
                signButtonLayoutParams.leftMargin = 0;
                signButton.setLayoutParams(signButtonLayoutParams);
                // historyButton
                final var historyDrawable = new ImageView(activity);
                historyDrawable.setImageResource(
                        ReflectUtils.getDrawableId("icon_mask_wo_list_history24_svg"));
                final var historyButton = new RelativeLayout(activity);
                var historyLayoutParams = new RelativeLayout.LayoutParams(
                        signButtonLayoutParams.height, RelativeLayout.LayoutParams.MATCH_PARENT);
                historyLayoutParams.addRule(RelativeLayout.LEFT_OF, signButton.getId());
                historyButton.setLayoutParams(historyLayoutParams);
                historyButton.setId(View.generateViewId());
                historyButton.addView(historyDrawable);
                var drawableLayoutParams = (RelativeLayout.LayoutParams) historyDrawable.getLayoutParams();
                drawableLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                historyDrawable.setLayoutParams(drawableLayoutParams);
                parent.addView(historyButton);
                historyButton.setOnClickListener(view -> {
                    var intent = new Intent().setClassName(activity,
                            "com.baidu.tieba.myCollection.history.PbHistoryActivity");
                    activity.startActivity(intent);
                });
                // collectButton
                final var collectDrawable = new ImageView(activity);
                collectDrawable.setImageResource(
                        ReflectUtils.getDrawableId("icon_mask_wo_list_collect24_svg"));
                final var collectButton = new RelativeLayout(activity);
                var collectButtonLayoutParams = new RelativeLayout.LayoutParams(
                        signButtonLayoutParams.height, RelativeLayout.LayoutParams.MATCH_PARENT);
                collectButtonLayoutParams.addRule(RelativeLayout.LEFT_OF, historyButton.getId());
                collectButton.setLayoutParams(collectButtonLayoutParams);
                collectButton.addView(collectDrawable);
                collectDrawable.setLayoutParams(drawableLayoutParams);
                parent.addView(collectButton);
                collectButton.setOnClickListener(view -> {
                    var intent = new Intent().setClassName(activity,
                            "com.baidu.tieba.myCollection.CollectTabActivity");
                    activity.startActivity(intent);
                });
            }
        });
    }
}
