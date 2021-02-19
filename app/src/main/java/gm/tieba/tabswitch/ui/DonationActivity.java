package gm.tieba.tabswitch.ui;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.FileOutputStream;
import java.util.Objects;

import gm.tieba.tabswitch.R;
import gm.tieba.tabswitch.databinding.ActivityDonationBinding;
import gm.tieba.tabswitch.util.IO;

public class DonationActivity extends AppCompatActivity {
    @SuppressLint("ResourceType")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityDonationBinding binding = ActivityDonationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.alipay.setOnClickListener(v -> {
            String uri = "intent://platformapi/startapp?saId=10000007&clientVersion=3.7.0.0718&qrcode=https%3A%2F%2Fqr.alipay.com%2F" +
                    "fkx11506l6izd365sumyde6?t=1608475251273" +
                    "%3F_s%3Dweb-other&_t=1472443966571#Intent;scheme=alipayqr;package=com.eg.android.AlipayGphone;end";
            try {
                Intent intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME);
                startActivity(intent);
            } catch (Throwable ignored) {
            }
        });
        binding.mm.setOnClickListener(v -> {
            try {
                ContentValues newQrCodeDetails = new ContentValues();
                newQrCodeDetails.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);
                newQrCodeDetails.put(MediaStore.MediaColumns.DISPLAY_NAME, "rewardQrCode");
                newQrCodeDetails.put(MediaStore.MediaColumns.MIME_TYPE, "image/webp");
                ContentResolver resolver = getApplicationContext().getContentResolver();
                Uri qrCodeUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, newQrCodeDetails);
                ParcelFileDescriptor descriptor = resolver.openFileDescriptor(qrCodeUri, "w");
                IO.copyFile(getResources().openRawResource(R.drawable.mm_reward_qrcode), new FileOutputStream(descriptor.getFileDescriptor()));
                Toast.makeText(this, "已将微信赞赏码保存至Pictures文件夹", Toast.LENGTH_SHORT).show();

                Intent intent = getPackageManager().getLaunchIntentForPackage("com.tencent.mm");
                intent.putExtra("LauncherUI.From.Scaner.Shortcut", true);
                intent.setAction("android.intent.action.VIEW");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            } catch (Throwable throwable) {
                Toast.makeText(this, "微信只能截屏扫码哦", Toast.LENGTH_SHORT).show();
            }
        });
    }
}