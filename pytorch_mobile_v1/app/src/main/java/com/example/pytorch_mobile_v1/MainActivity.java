package com.example.pytorch_mobile_v1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;



public class MainActivity extends AppCompatActivity {
    private Activity activity;
    public static final int PermissionCode = 1000;
    public static final int GetPhotoCode = 1001;
    private Button mBtnPic;
    private ImageView mShowImage;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Bitmap bitmap = null;
        Module module = null;


        activity = this;
        initView();
        initListener();

        try {
            // creating bitmap from packaged into app android asset 'image.jpg',
            // app/src/main/assets/image.jpg
            //bitmap = BitmapFactory.decodeStream(getAssets().open("image.jpg"));
            // loading serialized torchscript module from packaged into app android asset model.pt,
            // app/src/model/assets/model.pt
            module = Module.load(assetFilePath(this, "model.pt"));
        } catch (IOException e) {
            Log.e("Pytorch HelloWorld", "Error reading assets", e);
            finish();
        }
        mBtnPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, Detect.class));
            }
        });
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////

    private void initView() {
        mBtnPic = (Button) findViewById(R.id.btn_take_pic);
        //mShowImage = (ImageView) findViewById(R.id.show_image);
    }
    private void initListener() {
        mBtnPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //檢查是否取得權限
                final int permissionCheck = ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

                //沒有權限時
                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.CAMERA,
                                    Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            PermissionCode);
                } else { //已獲得權限
                    Toast.makeText(activity, "已經拿到權限囉!", Toast.LENGTH_SHORT).show();
                    startActivityForResult(new Intent(MainActivity.this, TakePicActivity.class), GetPhotoCode);
                }

            }
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionCode) {
            //假如允許了
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //do something
                Toast.makeText(this, "感謝賜予權限！", Toast.LENGTH_SHORT).show();
                startActivityForResult(new Intent(MainActivity.this, TakePicActivity.class), GetPhotoCode);
            }
            //假如拒絕了
            else {
                //do something
                Toast.makeText(this, "CAMERA權限FAIL", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


}
