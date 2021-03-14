package gm.tieba.tabswitch.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;

import gm.tieba.tabswitch.BuildConfig;
import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.databinding.ActivityMainBinding;
import gm.tieba.tabswitch.util.DisplayHelper;
import gm.tieba.tabswitch.util.IO;
import gm.tieba.tabswitch.util.ManifestParser;
import gm.tieba.tabswitch.util.RepackageProcessor;
import gm.tieba.tabswitch.util.XpatchAssetHelper;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_TS);// Make sure this is before calling super.onCreate
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (isModuleActive()) {
            binding.status.setCardBackgroundColor(getResources().getColor(R.color.colorNormal, null));
            binding.statusIcon.setImageResource(R.drawable.ic_check_circle);
            binding.statusTitle.setText("已激活");
        }
        binding.statusSummary.setText(BuildConfig.VERSION_NAME);
        binding.settings.setOnClickListener(v -> {
            Intent intent = getPackageManager().getLaunchIntentForPackage("com.baidu.tieba");
            if (intent == null) return;
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            intent.putExtra("showTSPreference", true);
            startActivity(intent);
        });
        binding.xpatch.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/vnd.android.package-archive");
            startActivityForResult(intent, 1);
            RepackageProcessor.initDialog(this);
        });
        SharedPreferences sharedPreferences = getSharedPreferences("config", Context.MODE_PRIVATE);
        if (sharedPreferences.getBoolean("icon", false)) binding.iconTitle.setText("隐藏图标");
        else binding.iconTitle.setText("显示图标");
        binding.icon.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            PackageManager packageManager = getPackageManager();
            if (sharedPreferences.getBoolean("icon", false)) {
                AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setIcon(R.mipmap.ic_launcher).setTitle("提示").setMessage("隐藏图标后就只能从Xposed Manager中打开模块了哦").setCancelable(true)
                        .setNegativeButton("取消", (dialogInterface, i) -> {
                        }).setPositiveButton("确定", (dialogInterface, i) -> {
                            packageManager.setComponentEnabledSetting(new ComponentName(getPackageName(), "gm.tieba.tabswitch.ui.MainActivity"), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                            editor.putBoolean("icon", false);
                            editor.apply();
                            binding.iconTitle.setText("显示图标");
                        }).create();
                alertDialog.show();
            } else {
                packageManager.setComponentEnabledSetting(new ComponentName(getPackageName(), "gm.tieba.tabswitch.ui.MainActivity"), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                editor.putBoolean("icon", true);
                editor.apply();
                binding.iconTitle.setText("隐藏图标");
            }
        });
        binding.telegram.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setData(Uri.parse("https://t.me/TabSwitch"));
            startActivity(intent);
        });
        binding.license.setOnClickListener(v -> startActivity(new Intent(this, LicenseActivity.class)));
        binding.donation.setOnClickListener(v -> startActivity(new Intent(this, DonationActivity.class)));
        if (RepackageProcessor.manifestData != null && savedInstanceState.getBoolean("isShowing"))
            RepackageProcessor.xpatchStartDialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 0 && RepackageProcessor.outApk != null) {
            try {
                if (getPackageManager().getPackageInfo(ManifestParser.packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo.getApkContentsSigners()[0].hashCode() == -524758043)
                    throw new PackageManager.NameNotFoundException();
            } catch (PackageManager.NameNotFoundException e) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                Uri uri = FileProvider.getUriForFile(this, "gm.FileProvider", RepackageProcessor.outApk);
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                startActivity(intent);
                RepackageProcessor.xpatchSuccessDialog.dismiss();
            }
        } else
            try {
                if (resultCode != Activity.RESULT_OK) return;
                File tmpFile = new File(getExternalCacheDir().getAbsolutePath(), "temp.apk");
                IO.copyFile(getContentResolver().openInputStream(data.getData()), tmpFile);
                String apkName = getPackageManager().getApplicationLabel(getPackageManager().getPackageArchiveInfo(tmpFile.getPath(), PackageManager.GET_ACTIVITIES).applicationInfo).toString();
                File newFile = new File(getExternalCacheDir().getAbsolutePath(), apkName + ".apk");
                tmpFile.renameTo(newFile);
                switch (requestCode) {
                    case 1:
                        while (RepackageProcessor.manifestData != null)
                            Thread.sleep(500);//前一个打包任务还没完成，阻塞主线程
                        RepackageProcessor.inApk = newFile;
                        bin.zip.ZipFile zipFile = new bin.zip.ZipFile(RepackageProcessor.inApk);
                        bin.zip.ZipEntry manifestEntry = zipFile.getEntry("AndroidManifest.xml");
                        RepackageProcessor.manifestData = ManifestParser.parseManifest(zipFile.getInputStream(manifestEntry), "com.wind.xpatch.proxy.XpatchProxyApplication");
                        RepackageProcessor.xpatchStartDialog.setMessage(RepackageProcessor.inApk.getName());
                        RepackageProcessor.xpatchStartDialog.show();

                        RepackageProcessor.moduleList = XpatchAssetHelper.loadAllInstalledModule(this);
                        RecyclerView recyclerView = RepackageProcessor.xpatchStartDialog.findViewById(RepackageProcessor.recyclerViewId);
                        ModuleListAdapter adapter = new ModuleListAdapter(this);
                        recyclerView.setAdapter(adapter);
                        adapter.setOnItemClickListener((view, layoutPosition) -> {
                            CheckBox cb = view.findViewById(R.id.cb);
                            cb.performClick();
                            Map<String, Object> map = RepackageProcessor.moduleList.get(layoutPosition);
                            map.put("isChecked", !(Boolean) map.get("isChecked"));
                            RepackageProcessor.moduleList.set(layoutPosition, map);
                        });
                        DisplayMetrics metrics = getResources().getDisplayMetrics();
                        final int maxWidth = DisplayHelper.px2Dip(this, metrics.widthPixels) - 91;
                        final int maxHeight = DisplayHelper.px2Dip(this, metrics.heightPixels) - 322;//306
                        final int recyclerViewTotalHeight = RepackageProcessor.moduleList.size() * 64;
                        if (recyclerViewTotalHeight > maxHeight) {
                            ViewGroup.LayoutParams recyclerViewLayoutParams = recyclerView.getLayoutParams();
                            recyclerViewLayoutParams.width = DisplayHelper.dip2Px(this, maxWidth);
                            recyclerViewLayoutParams.height = DisplayHelper.dip2Px(this, maxHeight);
                            recyclerView.setLayoutParams(recyclerViewLayoutParams);
                        }

                        RepackageProcessor.insertModulesPath = new ArrayList<>();
                        RepackageProcessor.xpatchStartDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setText("内置模块");
                        RepackageProcessor.xpatchStartDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType("application/vnd.android.package-archive");
                            startActivityForResult(intent, 2);
                        });
                        RepackageProcessor.xpatchStartDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnLongClickListener(v -> {
                            StringBuilder modules = new StringBuilder();
                            for (int i = 0; i < RepackageProcessor.insertModulesPath.size(); i++) {
                                if (!"".equals(modules.toString())) modules.append("\n");
                                modules.append(new File(RepackageProcessor.insertModulesPath.get(i)).getName());
                            }
                            AlertDialog alertDialog = new AlertDialog.Builder(this)
                                    .setIcon(R.mipmap.ic_launcher).setTitle("已添加模块").setMessage(modules.toString()).setCancelable(true)
                                    .setNeutralButton("清空", (dialogInterface, i) -> {
                                        RepackageProcessor.insertModulesPath = new ArrayList<>();
                                        RepackageProcessor.xpatchStartDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setText("内置模块");
                                    }).setPositiveButton("确定", (dialogInterface, i) -> {
                                    }).create();
                            alertDialog.show();
                            return true;
                        });
                        RepackageProcessor.getSignThread = new Thread(new RepackageProcessor.GetSignTask());
                        RepackageProcessor.getSignThread.start();
                        break;
                    case 2:
                        if (RepackageProcessor.inApk.getPath().replace(".apk", "_xpatch.apk").equals(newFile.getPath()))
                            Toast.makeText(getApplicationContext(), "你搁这搁这呢？", Toast.LENGTH_SHORT).show();
                        else {
                            RepackageProcessor.insertModulesPath.add(newFile.getPath());
                            Toast.makeText(getApplicationContext(), newFile.getName(), Toast.LENGTH_SHORT).show();
                            RepackageProcessor.xpatchStartDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setText(String.format(Locale.CHINA, "内置模块：%d", RepackageProcessor.insertModulesPath.size()));
                        }
                        break;
                }
            } catch (Throwable throwable) {
                RepackageProcessor.xpatchFailureDialog.setMessage(Log.getStackTraceString(throwable));
                RepackageProcessor.xpatchFailureDialog.show();
                throwable.printStackTrace();
            }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (RepackageProcessor.xpatchStartDialog != null)
            outState.putBoolean("isShowing", RepackageProcessor.xpatchStartDialog.isShowing());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (RepackageProcessor.xpatchStartDialog != null)
            RepackageProcessor.xpatchStartDialog.dismiss();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }

    public static boolean isModuleActive() {
        Log.i("gm.tieba.tabswitch", "ModuleNotActive");
        return false;
    }
}