package com.example.pytorch_mobile_v1;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class TakePicActivity extends AppCompatActivity {


    private Button button;
    private CameraSurfaceView mCameraSurfaceView;

    private Activity activity;
    String filePath;

    private ImageView mImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = this;
        getBundleData();

        initSet();
        initView();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCameraSurfaceView.takePicture(activity, filePath);
            }
        });


        mImg = (ImageView) findViewById(R.id.scan);

        mCameraSurfaceView.setimgv(mImg);
//        if(mCameraSurfaceView.getimg()!=null){
//            mImg.setImageBitmap(mCameraSurfaceView.getimg());
//            System.out.println("Get img");
//        }else{
//            System.out.println("Get no img");
//        }


    }

    private void initSet() {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 全屏显示
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.scanner_take_picture);
    }


    private void initView() {
        mCameraSurfaceView = (CameraSurfaceView) findViewById(R.id.cameraSurfaceView);
        button = (Button) findViewById(R.id.takePic);
    }

    private void getBundleData() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            filePath = bundle.getString("url");
        }
        Log.d("checkpoint", "check filePath - " + filePath);
    }
}
