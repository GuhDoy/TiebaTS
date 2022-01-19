package gm.tieba.tabswitch.hooker.auto;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.net.Uri;
import android.provider.MediaStore;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import gm.tieba.tabswitch.XposedContext;
import gm.tieba.tabswitch.hooker.IHooker;

public class RegisterInternalContentUri extends XposedContext implements IHooker {
    public void hook() throws Throwable {
        XposedHelpers.findAndHookMethod(ContentResolver.class, "registerContentObserver",
                Uri.class, boolean.class, ContentObserver.class, new XC_MethodHook() {
                    @Override
                    public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        var uri = (Uri) param.args[0];
                        if (MediaStore.Images.Media.INTERNAL_CONTENT_URI.equals(uri)) {
                            param.setResult(null);
                        }
                    }
                });
    }
}
