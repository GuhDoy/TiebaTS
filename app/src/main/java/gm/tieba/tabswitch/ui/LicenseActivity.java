package gm.tieba.tabswitch.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.databinding.ActivityLicenseBinding;
import gm.tieba.tabswitch.util.DisplayHelper;

public class LicenseActivity extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityLicenseBinding binding = ActivityLicenseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        Map<String, String> map = new HashMap<>();
        map.put("source", "TiebaTS");
        map.put("abstract", "An Xposed module for Baidu Tieba with personalized functions.");
        map.put("license", " GPL-3.0 ");
        Map<String, String> map1 = new HashMap<>();
        map1.put("source", "Xpatch");
        map1.put("abstract", "免Root实现app加载Xposed插件工具。");
        map1.put("license", " Apache-2.0 ");
        Map<String, String> map2 = new HashMap<>();
        map2.put("source", "TiebaSignIn");
        map2.put("abstract", "利用github actions实现百度贴吧自动签到脚本，每日自动签到，获得8点经验。");
        map2.put("license", " MIT ");
        Map<String, String> map3 = new HashMap<>();
        map3.put("source", "xposed_module_loader");
        map3.put("abstract", "这是App加载已安装的Xposed Modules的一个库。");
        Map<String, String> map4 = new HashMap<>();
        map4.put("source", "OkHttp");
        map4.put("abstract", "Square's meticulous HTTP client for the JVM, Android, and GraalVM.");
        map4.put("license", " Apache-2.0 ");
        Map<String, String> map5 = new HashMap<>();
        map5.put("source", "smali");
        map5.put("abstract", "smali/baksmali is an assembler/disassembler for the dex format used by dalvik, Android's Java VM implementation.");
        List<Map<String, String>> licenseList = new ArrayList<>();
        licenseList.add(map);
        licenseList.add(map1);
        licenseList.add(map2);
        licenseList.add(map3);
        licenseList.add(map4);
        licenseList.add(map5);
        String[] from = new String[]{"source", "abstract", "license"};
        int[] to = new int[]{R.id.tv_source, R.id.tv_abstract, R.id.tv_license};
        SimpleAdapter licenseAdapter = new SimpleAdapter(getApplicationContext(), licenseList, R.layout.adapter_item_license, from, to);

        binding.lvLicense.setAdapter(licenseAdapter);
        binding.lvLicense.setOnItemClickListener((parent, view, position, id) -> {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            TextView tv = view.findViewById(R.id.tv_source);
            switch ((String) tv.getText()) {
                case "TiebaTS":
                    intent.setData(Uri.parse("https://github.com/GuhDoy/TiebaTS"));
                    break;
                case "Xpatch":
                    intent.setData(Uri.parse("https://github.com/WindySha/Xpatch"));
                    break;
                case "TiebaSignIn":
                    intent.setData(Uri.parse("https://github.com/srcrs/TiebaSignIn"));
                    break;
                case "xposed_module_loader":
                    intent.setData(Uri.parse("https://github.com/GuhDoy/xposed_module_loader"));
                    break;
                case "OkHttp":
                    intent.setData(Uri.parse("https://github.com/square/okhttp"));
                    break;
                case "smali":
                    intent.setData(Uri.parse("https://github.com/JesusFreke/smali"));
                    break;
                default:
                    intent.setData(Uri.parse("https://b23.tv/av80433022"));
                    break;
            }
            startActivity(intent);
        });
        binding.lvLicense.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private boolean isCompletedDraw = false;

            @Override
            public void onGlobalLayout() {
                if (!isCompletedDraw) {
                    isCompletedDraw = true;
                    binding.tvThanks.measure(0, 0);
                    final int maxHeight = ((LinearLayout) binding.lvLicense.getParent()).getMeasuredHeight() - binding.tvThanks.getMeasuredHeight() - DisplayHelper.dip2Px(LicenseActivity.this, 90);
                    int listViewTotalHeight = 0;
                    DisplayMetrics metrics = LicenseActivity.this.getResources().getDisplayMetrics();
                    int listViewWidth = metrics.widthPixels - DisplayHelper.dip2Px(LicenseActivity.this, 10);
                    int widthSpec = View.MeasureSpec.makeMeasureSpec(listViewWidth, View.MeasureSpec.AT_MOST);
                    for (int i = 0; i < licenseAdapter.getCount(); i++) {
                        View listItem = licenseAdapter.getView(i, null, binding.lvLicense);
                        listItem.measure(widthSpec, 0);
                        int itemHeight = listItem.getMeasuredHeight();
                        listViewTotalHeight += itemHeight;
                    }
                    listViewTotalHeight += (licenseAdapter.getCount() - 1) * binding.lvLicense.getDividerHeight();
                    ViewGroup.LayoutParams listViewLayoutParams = binding.lvLicense.getLayoutParams();
                    listViewLayoutParams.height = Math.min(listViewTotalHeight, maxHeight);
                    binding.lvLicense.setLayoutParams(listViewLayoutParams);
                }
            }
        });
    }
}