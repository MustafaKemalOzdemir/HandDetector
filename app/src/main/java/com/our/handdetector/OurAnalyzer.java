package com.our.handdetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import com.google.mlkit.vision.common.InputImage;

import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class OurAnalyzer implements ImageAnalysis.Analyzer {
    private Context context;
    private  Interpreter interpreter;
    public OurAnalyzer(Context context){
        initializeModel();
        this.context = context;
    }
    @Override
    public void analyze(ImageProxy imageProxy) {
        Log.v("ourmessage", "analayzdayım");
        Log.v("ourmessage", imageProxy.getHeight() + "height" + imageProxy.getWidth() + "width");

        ImageProxy.PlaneProxy[] planes = imageProxy.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        //U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21, imageProxy.getWidth(), imageProxy.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 75, out);

        byte[] imageBytes = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 1280, 960, false);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        scaledBitmap.recycle();
        float[][] result = new float[1][1];
        if(interpreter != null){
            interpreter.run(byteArray, result);
        }
    }

    private void initializeModel(){
        try {
            InputStream inputStream = context.getAssets().open("our_model.tflite");
            byte[] model = new byte[inputStream.available()];
            inputStream.read(model);
            ByteBuffer buffer = ByteBuffer.allocateDirect(model.length)
                    .order(ByteOrder.nativeOrder());
            buffer.put(model);

            Interpreter interpreter = new Interpreter(buffer);
            //interpreter.run(iput out);
        } catch (IOException e) {
            // File not found?
        }
    }

}
