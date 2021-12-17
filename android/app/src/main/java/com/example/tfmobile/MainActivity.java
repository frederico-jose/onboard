package com.example.tfmobile;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;

import com.example.tfmobile.env.Utils;
import com.example.tfmobile.env.BorderedText;
import com.example.tfmobile.env.ImageUtils;
import com.example.tfmobile.tflite.Detector;
import com.example.tfmobile.tflite.YoloV5Classifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chooseImageButton = findViewById(R.id.chooseImageButton);
        detectButton = findViewById(R.id.detectButton);
        imageView = findViewById(R.id.imageView);

        chooseImageButton.setOnClickListener(v -> {
            // Choose image from phone data
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Title"), SELECT_IMAGE_CODE);
        });

        detectButton.setOnClickListener(v -> {
            Handler handler = new Handler();
            // Use yolo model to detect objects
            new Thread(() -> {
                final List<Detector.Recognition> results = detector.recognizeImage(cropBitmap);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        handleResult(cropBitmap, results);
                    }
                });
            }).start();

        });
        this.sourceBitmap = Utils.getBitmapFromAsset(MainActivity.this, "001921029_620x393_c.jpg");
        this.cropBitmap = Utils.processBitmap(sourceBitmap, TF_OD_API_INPUT_SIZE);
        this.imageView.setImageBitmap(cropBitmap);

        initBox();
    }

    public static int SELECT_IMAGE_CODE = 1;

    public static final int TF_OD_API_INPUT_SIZE = 640;

    private static final boolean TF_OD_API_IS_QUANTIZED = false;

    private static final String TF_OD_API_MODEL_FILE = "yolov5s-fp16.tflite";

    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/classes.txt";

    // Minimum detection confidence to track a detection.
    private static final boolean MAINTAIN_ASPECT = false;
    private Integer sensorOrientation = 90;

    private Detector detector;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private Bitmap sourceBitmap;
    private Bitmap cropBitmap;

    private Button chooseImageButton, detectButton;
    private ImageView imageView;

    private void initBox() {
        previewHeight = TF_OD_API_INPUT_SIZE;
        previewWidth = TF_OD_API_INPUT_SIZE;
        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        try {
            detector =
                    YoloV5Classifier.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_IS_QUANTIZED,
                            TF_OD_API_INPUT_SIZE);
        } catch (final IOException e) {
            e.printStackTrace();
            finish();
        }
    }

    private void handleResult(Bitmap bitmap, List<Detector.Recognition> results) {
        final Canvas canvas = new Canvas(bitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2.0f);

        for (final Detector.Recognition result : results) {
            final RectF location = result.getLocation();
            if (location != null && result.getConfidence() >= MINIMUM_CONFIDENCE_TF_OD_API) {
                canvas.drawRect(location, paint);
                BorderedText b = new BorderedText(15);
                double roundOff = Math.round(result.getConfidence() * 100.0) / 100.0;
                b.drawText(canvas, location.left, location.top, result.getTitle() + " " + roundOff);
            }
        }
        imageView.setImageBitmap(bitmap);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            if (requestCode == 1) {
                // Get image and display it on screen
                Uri uri = data.getData();
                final InputStream imageStream = getContentResolver().openInputStream(uri);
                final Bitmap sourceBitmap = BitmapFactory.decodeStream(imageStream);
                cropBitmap = Utils.processBitmap(sourceBitmap, TF_OD_API_INPUT_SIZE);
                imageView.setImageBitmap(cropBitmap);
            }
        }
        catch (Exception e) {

        }
    }
}
