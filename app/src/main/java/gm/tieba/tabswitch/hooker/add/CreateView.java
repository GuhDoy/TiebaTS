package gm.tieba.tabswitch.hooker.add;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;
import gm.tieba.tabswitch.util.ReflectUtils;

public class CreateView extends XposedContext implements IHooker {

    @NonNull
    @Override
    public String key() {
        return "create_view";
    }

    @Override
    public void hook() throws Throwable {
        var method = XposedHelpers.findMethodExactIfExists(
                "com.baidu.tieba.enterForum.home.EnterForumTabFragment", sClassLoader,
                "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class
        );
        if (method == null) {
            // 12.25.4.1 +
            method = ReflectUtils.findFirstMethodByExactType(
                    "com.baidu.tieba.enterForum.home.EnterForumTabFragment", View.class, Bundle.class
            );
        }
        XposedBridge.hookMethod(method, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                final var controller = ReflectUtils.getObjectField(param.thisObject, 1);
                final var signButton = ReflectUtils.getObjectField(controller, ImageView.class);
                final var activity = (Activity) XposedHelpers.callMethod(param.thisObject, "requireActivity");
                final var parent = (RelativeLayout) signButton.getParent();
                final var signButtonLayoutParams = (RelativeLayout.LayoutParams) signButton.getLayoutParams();
                signButtonLayoutParams.leftMargin = 0;
                signButton.setLayoutParams(signButtonLayoutParams);
                // historyButton
                final var historyDrawable = new ImageView(activity);
                historyDrawable.setImageResource(
                        ReflectUtils.getDrawableId("icon_mask_wo_list_history24_svg"));
                final var historyButton = new RelativeLayout(activity);
                final var historyLayoutParams = new RelativeLayout.LayoutParams(
                        signButtonLayoutParams.height, RelativeLayout.LayoutParams.MATCH_PARENT);
                historyLayoutParams.addRule(RelativeLayout.LEFT_OF, signButton.getId());
                historyButton.setLayoutParams(historyLayoutParams);
                historyButton.setId(View.generateViewId());
                historyButton.addView(historyDrawable);
                final var drawableLayoutParams = (RelativeLayout.LayoutParams) historyDrawable.getLayoutParams();
                drawableLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                historyDrawable.setLayoutParams(drawableLayoutParams);
                parent.addView(historyButton);
                historyButton.setOnClickListener(view -> {
                    final var intent = new Intent().setClassName(activity,
                            "com.baidu.tieba.myCollection.history.PbHistoryActivity");
                    activity.startActivity(intent);
                });
                // collectButton
                final var collectDrawable = new ImageView(activity);
                collectDrawable.setImageResource(
                        ReflectUtils.getDrawableId("icon_mask_wo_list_collect24_svg"));
                final var collectButton = new RelativeLayout(activity);
                final var collectButtonLayoutParams = new RelativeLayout.LayoutParams(
                        signButtonLayoutParams.height, RelativeLayout.LayoutParams.MATCH_PARENT);
                collectButtonLayoutParams.addRule(RelativeLayout.LEFT_OF, historyButton.getId());
                collectButton.setLayoutParams(collectButtonLayoutParams);
                collectButton.addView(collectDrawable);
                collectDrawable.setLayoutParams(drawableLayoutParams);
                parent.addView(collectButton);
                collectButton.setOnClickListener(view -> {
                    final var intent = new Intent().setClassName(activity,
                            "com.baidu.tieba.myCollection.CollectTabActivity");
                    activity.startActivity(intent);
                });
            }
        });
    }
}
