package com.example.tflite;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;



import androidx.appcompat.app.AppCompatActivity;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.widget.Toast;

import org.tensorflow.lite.Interpreter;

import java.nio.MappedByteBuffer;
//이미지를 비트맵으로 변환
//비트맵을 바이트배열로 변환
//텐서플로 적용
//예측 결과를 텍스트 뷰에 출력

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



//에셋 폴더의 파일 읽기

        AssetManager am = getAssets();
        BufferedInputStream buf;

        try {
// 선택한 이미지에서 비트맵 생성
// buf = new BufferedInputStream(am.open("spam.png"));
            buf = new BufferedInputStream(am.open("ham_1.png"));

            Bitmap bmp = BitmapFactory.decodeStream(buf);
            int cx= 150, cy=150;
            bmp = Bitmap.createScaledBitmap(bmp, cx, cy, false);

            Log.v("test","성공3");

            int [] pixels=new int [cx*cy];
            bmp.getPixels(pixels,0,cx,0,0,cx,cy);


//각각의 픽셀이 4바이트로 구성되어 있음

            int bytes = bmp.getByteCount();
            Log.v("getByteCount", Integer.toString(bytes));

//비트맵을 바이트배열로 변환


            ByteBuffer input_img=getInputImage_2(pixels,cx,cy);

//파이썬에서 만든 모델 파일 로드
            Interpreter lite = getTfliteInterpreter("converted_model.tflite");

            float[][] output = new float[1][1];
            lite.run(input_img, output);


            Log.d("predict", Arrays.toString(output[0]));

            final String predText = String.format("%f", output[0][0]);
            if(output[0][0]>0.5){
                Toast toast= Toast.makeText(getApplicationContext(),String.format("스팸일 확률: %f",output[0][0]),Toast.LENGTH_LONG);toast.show();
            }
            else{
                Toast toast= Toast.makeText(getApplicationContext(),String.format("정상메시지입니다"),Toast.LENGTH_LONG);toast.show();

            }


            Log.d("예측", predText);





        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //1차원 배열 사용
    private ByteBuffer getInputImage_2(int[] pixels, int cx, int cy) {
        ByteBuffer input_img = ByteBuffer.allocateDirect(cx * cy * 3 * 4);
        input_img.order(ByteOrder.nativeOrder());

        for (int i = 0; i < cx * cy; i++) {
            int pixel = pixels[i]; // ARGB : ff4e2a2a
            input_img.putFloat(((pixel >> 16) & 0xff) / (float) 255);
            input_img.putFloat(((pixel >> 8) & 0xff) / (float) 255);
            input_img.putFloat(((pixel >> 0) & 0xff) / (float) 255);
        }

        return input_img;
    }




    //필수 코드
    private Interpreter getTfliteInterpreter (String modelPath){
        try {
            return new Interpreter(loadModelFile(MainActivity.this, modelPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private MappedByteBuffer loadModelFile (Activity activity, String modelPath) throws
            IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

}