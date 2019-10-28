package com.example.aishideru;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

public class imageselect extends AppCompatActivity {

    private static int GALLERY_REQUEST_CODE = 2;

    private Activity activity;

    private Button select_bt;
    private Button detect_bt;
    private ImageView imgshow;
    private TextView ans;

    private int imgW;
    private int imgH;

    private Bitmap bit2de = null;

    private Module module;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_imageselect);

        activity = this;
        select_bt = (Button)findViewById(R.id.select_bt);
        detect_bt = (Button)findViewById(R.id.detect_bt);
        imgshow = (ImageView)findViewById(R.id.showg);
        ans = (TextView)findViewById(R.id.ans);

        try {
            module = Module.load(assetFilePath(this, "model_scope.pt"));
        } catch (IOException e) {
            Log.e("Pytorch HelloWorld", "Error reading assets", e);
            finish();
        }

        select_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickFromGallery();
            }
        });


        detect_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bit2de  = Bitmap.createScaledBitmap(bit2de, 256, 256, true);

                bit2de = rotateBitmapByDegree(bit2de, 90);

                float mean[] = new float[] {0.485f,0.456f,0.406f};
                float std[] = new float[] {0.229f,0.224f,0.225f};

                // preparing input tensor

//                final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bit2de,
//                        TensorImageUtils.TORCHVISION_NORM_MEAN_RGB, TensorImageUtils.TORCHVISION_NORM_STD_RGB);
                final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(bit2de, mean, std);
                System.out.println("Convert to tensor.");
                // running the model
                final Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();
                System.out.println("Tensor forward.");
                // getting tensor content as java array of floats
                final float[] scores = outputTensor.getDataAsFloatArray();
                System.out.println("Get score.");
//                System.out.println(Arrays.toString(s));
                for(float log : scores)
                {
                    //Log.v("Tag",log);
                    //System.out.println(log);
                }


                // searching for the index with maximum score
                float maxScore = -Float.MAX_VALUE;

                float maxcoScore = -Float.MAX_VALUE;

                float maxLikeIdx = -1;

                float maxcomIdx = -1;

                for (int i = 0; i < scores.length; i++) {

                    if (i<30){
                        if (scores[i] > maxScore) {
                            maxScore = scores[i];
                            maxLikeIdx = i;
                        }
                    }else {
                        if (scores[i] > maxcoScore) {
                            maxcoScore = scores[i];
                            maxcomIdx = i-30;
                        }
                    }
                }

                System.out.println(">>>>"+scores.length);

                System.out.println(">>>>"+maxLikeIdx);
                System.out.println(">>>>"+maxcomIdx);

                float pop = (maxLikeIdx/30.0f)*0.30f + (maxcomIdx/355.0f)*0.70f;
                pop = pop*100;

                String s = String.valueOf( pop );
                String t = s.substring(0, s.indexOf(".") + 2);


                ans.setText("Popularity : " + t +" %");
            }
        });
    }

    //https://androidclarified.com/pick-image-gallery-camera-android/
    private void pickFromGallery(){
        //Create an Intent with action as ACTION_PICK
        Intent intent=new Intent(Intent.ACTION_PICK);
        // Sets the type as image/*. This ensures only components of type image are selected
        intent.setType("image/*");
        //We pass an extra array with the accepted mime types. This will ensure only components with these MIME types as targeted.
        String[] mimeTypes = {"image/jpeg", "image/png"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES,mimeTypes);
        // Launching the Intent
        startActivityForResult(intent,GALLERY_REQUEST_CODE);

    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // Result code is RESULT_OK only if the user selects an Image
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == imageselect.RESULT_OK)
            switch (requestCode) {
                case (2):
                    //data.getData returns the content URI for the selected Image
                    Uri selectedImage = data.getData();
                    Bitmap bit = null;
                    try {
                        bit = getBitmapFormUri(activity,selectedImage);
                        //bit = rotateBitmapByDegree(bit, 90);
                        bit  = Bitmap.createScaledBitmap(bit, imgW, imgH, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    imgshow.setAdjustViewBounds(true);
                    imgshow.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imgshow.setImageBitmap(bit);
                    bit2de = bit;
                    ans.setText("Popularity : ");
                    break;
            }

    }

    //https://www.cnblogs.com/popqq520/p/5404738.html
    public static Bitmap getBitmapFormUri(Activity ac, Uri uri) throws FileNotFoundException, IOException {
        InputStream input = ac.getContentResolver().openInputStream(uri);
        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither = true;//optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        int originalWidth = onlyBoundsOptions.outWidth;
        int originalHeight = onlyBoundsOptions.outHeight;
        if ((originalWidth == -1) || (originalHeight == -1))
            return null;
        //图片分辨率以480x800为标准
        float hh = 800f;//这里设置高度为800f
        float ww = 480f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (originalWidth > originalHeight && originalWidth > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (originalWidth / ww);
        } else if (originalWidth < originalHeight && originalHeight > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (originalHeight / hh);
        }
        if (be <= 0)
            be = 1;
        //比例压缩
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = be;//设置缩放比例
        bitmapOptions.inDither = true;//optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        input = ac.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();

        return compressImage(bitmap);//再进行质量压缩
    }
    public static Bitmap compressImage(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) {  //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            //第一个参数 ：图片格式 ，第二个参数： 图片质量，100为最高，0为最差  ，第三个参数：保存压缩后的数据的流
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片
        return bitmap;
    }

    public static Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Bitmap returnBm = null;

        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        // TODO Auto-generated method stub
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            ImageView im = (ImageView)findViewById(R.id.showg);
            imgW = im.getWidth();
            imgH = im.getHeight()-241;
            System.out.println("W:"+imgW);
            System.out.println("H:"+imgH);
            //Log.d(TAG, "width : " + imgv.getWidth());
        }

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
}
