package com.baidu.paddle.lite.demo.ocr;

import android.speech.tts.TextToSpeech;
import android.os.Bundle;
import android.view.View;

import android.widget.Toast;
import java.util.Locale;
import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
// import android.media.ExifInterface;
import android.content.res.AssetManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
public class MainActivity2 extends AppCompatActivity implements TextToSpeech.OnInitListener {
    private static final String       TAG              = MainActivity2.class.getSimpleName();
    public static final  int          OPEN_GALLERY_REQUEST_CODE = 0;
    //   public static final int TAKE_PHOTO_REQUEST_CODE = 1;

    public static final int REQUEST_LOAD_MODEL = 0;
    public static final int REQUEST_RUN_MODEL = 1;
    public static final int RESPONSE_LOAD_MODEL_SUCCESSED = 0;
    public static final int RESPONSE_LOAD_MODEL_FAILED = 1;
    public static final int RESPONSE_RUN_MODEL_SUCCESSED = 2;
    public static final int RESPONSE_RUN_MODEL_FAILED = 3;

    protected ProgressDialog pbLoadModel = null;
    protected ProgressDialog pbRunModel  = null;

    protected Handler receiver = null; // Receive messages from worker thread
    protected Handler       sender = null; // Send command to worker thread
    protected HandlerThread worker = null; // Worker thread to load&run model

    // UI components of object detection
    protected TextView tvInputSetting;
    protected TextView  tvStatus;
    protected ImageView ivInputImage;
    protected TextView  tvOutputResult;
    protected TextView tvInferenceTime;

    // Model settings of object detection
    protected String modelPath = "";
    protected String labelPath = "";
    protected String imagePath = "";
    protected int cpuThreadNum = 1;
    protected String cpuPowerMode = "";
    protected String inputColorFormat = "";
    protected long[] inputShape = new long[]{};
    protected float[] inputMean = new float[]{};
    protected float[] inputStd = new float[]{};
    protected     float        scoreThreshold = 0.1f;
    //  private String currentPhotoPath;
    private       AssetManager assetManager   =null;
    public static TextToSpeech textToSpeech;
    protected Predictor predictor = new Predictor();
    private   MyButton          click;
    private int count = 0;
    private int singe=0;
    private String read = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        //   loadModel();
        // Clear all setting items to avoid app crashing due to the incorrect settings
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
        if (textToSpeech==null){
            textToSpeech = new TextToSpeech(this, this);
        }
        // Setup the UI components
        tvInputSetting = findViewById(R.id.tv_input_setting);
        tvStatus = findViewById(R.id.tv_model_img_status);
        ivInputImage = findViewById(R.id.iv_input_image);
        tvInferenceTime = findViewById(R.id.tv_inference_time);
        tvOutputResult = findViewById(R.id.tv_output_result);
        tvInputSetting.setMovementMethod(ScrollingMovementMethod.getInstance());
        tvOutputResult.setMovementMethod(ScrollingMovementMethod.getInstance());

        // Prepare the worker thread for mode loading and inference
        receiver = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case RESPONSE_LOAD_MODEL_SUCCESSED:
                        if(pbLoadModel!=null && pbLoadModel.isShowing()){
                            pbLoadModel.dismiss();
                        }
                        onLoadModelSuccessed();
                        break;
                    case RESPONSE_LOAD_MODEL_FAILED:
                        if(pbLoadModel!=null && pbLoadModel.isShowing()){
                            pbLoadModel.dismiss();
                        }
                        Toast.makeText(MainActivity2.this, "Load model failed!", Toast.LENGTH_SHORT).show();
                        onLoadModelFailed();
                        break;
                    case RESPONSE_RUN_MODEL_SUCCESSED:
                        if(pbRunModel!=null && pbRunModel.isShowing()){
                            pbRunModel.dismiss();
                        }
                        onRunModelSuccessed();
                        break;
                    case RESPONSE_RUN_MODEL_FAILED:
                        if(pbRunModel!=null && pbRunModel.isShowing()){
                            pbRunModel.dismiss();
                        }
                        Toast.makeText(MainActivity2.this, "Run model failed!", Toast.LENGTH_SHORT).show();
                        onRunModelFailed();
                        break;
                    default:
                        break;
                }
            }
        };

        worker = new HandlerThread("Predictor Worker");
        worker.start();
        sender = new Handler(worker.getLooper()) {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case REQUEST_LOAD_MODEL:
                        // Load model and reload test image
                        if (onLoadModel()) {
                            receiver.sendEmptyMessage(RESPONSE_LOAD_MODEL_SUCCESSED);
                        } else {
                            receiver.sendEmptyMessage(RESPONSE_LOAD_MODEL_FAILED);
                        }
                        break;
                    case REQUEST_RUN_MODEL:
                        // Run model if model is loaded
                        if (onRunModel()) {
                            receiver.sendEmptyMessage(RESPONSE_RUN_MODEL_SUCCESSED);
                        } else {
                            receiver.sendEmptyMessage(RESPONSE_RUN_MODEL_FAILED);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        click = (MyButton)findViewById(R.id.click3);
        click.setOnDoubleTapListener(new MyButton.OnDoubleTapListener() {
            @Override
            public void onSingleTapConfirmed(MyButton myButton){
                if (textToSpeech!=null){
                    textToSpeech.stop();
                }

                Intent intent = new Intent(MainActivity2.this, USBCameraActivity1.class);
                startActivityForResult(intent, 1);

            }

            @Override
            public void onLongPress(MyButton myButton) {
                textToSpeech.speak("回到拍摄页面", TextToSpeech.QUEUE_FLUSH, null);
                if (textToSpeech!=null){

                    textToSpeech.stop();
                }
                Intent intent = new Intent(MainActivity2.this, USBCameraActivity1.class);
                startActivityForResult(intent, 1);
            }

            @Override
            public void onDoubleTap(MyButton myButton) {
                if (count % 2 == 0) {
                    if (textToSpeech != null) {
                        textToSpeech.stop();
                    }
                }

                else if (count % 2 == 1) {

                        textToSpeech.speak(read, TextToSpeech.QUEUE_FLUSH, null);

                }
                count = count + 1;
            }

        });

    }



    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean settingsChanged = false;
        String model_path = sharedPreferences.getString(getString(R.string.MODEL_PATH_KEY),
                getString(R.string.MODEL_PATH_DEFAULT));
        String label_path = sharedPreferences.getString(getString(R.string.LABEL_PATH_KEY),
                getString(R.string.LABEL_PATH_DEFAULT));
        String image_path = sharedPreferences.getString(getString(R.string.IMAGE_PATH_KEY),
                getString(R.string.IMAGE_PATH_DEFAULT));
        settingsChanged |= !model_path.equalsIgnoreCase(modelPath);
        settingsChanged |= !label_path.equalsIgnoreCase(labelPath);
        settingsChanged |= !image_path.equalsIgnoreCase(imagePath); //保存图片的路径
        int cpu_thread_num = Integer.parseInt(sharedPreferences.getString(getString(R.string.CPU_THREAD_NUM_KEY),
                getString(R.string.CPU_THREAD_NUM_DEFAULT)));
        settingsChanged |= cpu_thread_num != cpuThreadNum;
        String cpu_power_mode =
                sharedPreferences.getString(getString(R.string.CPU_POWER_MODE_KEY),
                        getString(R.string.CPU_POWER_MODE_DEFAULT));
        settingsChanged |= !cpu_power_mode.equalsIgnoreCase(cpuPowerMode);
        String input_color_format =
                sharedPreferences.getString(getString(R.string.INPUT_COLOR_FORMAT_KEY),
                        getString(R.string.INPUT_COLOR_FORMAT_DEFAULT));
        settingsChanged |= !input_color_format.equalsIgnoreCase(inputColorFormat);
        long[] input_shape =
                Utils.parseLongsFromString(sharedPreferences.getString(getString(R.string.INPUT_SHAPE_KEY),
                        getString(R.string.INPUT_SHAPE_DEFAULT)), ",");
        float[] input_mean =
                Utils.parseFloatsFromString(sharedPreferences.getString(getString(R.string.INPUT_MEAN_KEY),
                        getString(R.string.INPUT_MEAN_DEFAULT)), ",");
        float[] input_std =
                Utils.parseFloatsFromString(sharedPreferences.getString(getString(R.string.INPUT_STD_KEY)
                        , getString(R.string.INPUT_STD_DEFAULT)), ",");
        settingsChanged |= input_shape.length != inputShape.length;
        settingsChanged |= input_mean.length != inputMean.length;
        settingsChanged |= input_std.length != inputStd.length;
        if (!settingsChanged) {
            for (int i = 0; i < input_shape.length; i++) {
                settingsChanged |= input_shape[i] != inputShape[i];
            }
            for (int i = 0; i < input_mean.length; i++) {
                settingsChanged |= input_mean[i] != inputMean[i];
            }
            for (int i = 0; i < input_std.length; i++) {
                settingsChanged |= input_std[i] != inputStd[i];
            }
        }
        float score_threshold =
                Float.parseFloat(sharedPreferences.getString(getString(R.string.SCORE_THRESHOLD_KEY),
                        getString(R.string.SCORE_THRESHOLD_DEFAULT)));
        settingsChanged |= scoreThreshold != score_threshold;
        if (settingsChanged) {
            modelPath = model_path;
            labelPath = label_path;
            imagePath = image_path;
            cpuThreadNum = cpu_thread_num;
            cpuPowerMode = cpu_power_mode;
            inputColorFormat = input_color_format;
            inputShape = input_shape;
            inputMean = input_mean;
            inputStd = input_std;
            scoreThreshold = score_threshold;
            loadModel();
            set_img();
        }
    }

    public void loadModel() {
        pbLoadModel = ProgressDialog.show(this, "", "loading model...", false, false);
        sender.sendEmptyMessage(REQUEST_LOAD_MODEL);
    }

    public void runModel() {
        pbRunModel = ProgressDialog.show(this, "", "running model...", false, false);
        sender.sendEmptyMessage(REQUEST_RUN_MODEL);
    }

    public boolean onLoadModel() {
        return predictor.init(MainActivity2.this, modelPath, labelPath, cpuThreadNum,
                cpuPowerMode,
                inputColorFormat,
                inputShape, inputMean,
                inputStd, scoreThreshold);
    }

    public boolean onRunModel() {
        return predictor.isLoaded() && predictor.runModel();
    }

    public void onLoadModelSuccessed() {
        // Load test image from path and run model
        tvStatus.setText("STATUS: load model successed");
        if (USBCameraActivity1.mCameraHelper != null) {
            USBCameraActivity1.mCameraHelper.release();
        }
        Bitmap image = BitmapFactory.decodeFile(USBCameraActivity1.picPath);
            int width = image.getWidth() / 2;
            int heigh = image.getHeight();
            Bitmap image1 = Bitmap.createBitmap(image, 0, 0, width, heigh);
            Bitmap image2 = Bitmap.createBitmap(image, width, 0, width, heigh);

        //        Bitmap image =((BitmapDrawable)ivInputImage.getDrawable()).getBitmap();
        if(image == null) {
            tvStatus.setText("STATUS: image is not exists");
        }
        else if (!predictor.isLoaded()){
            tvStatus.setText("STATUS: model is not loaded");
        }else{
            tvStatus.setText("STATUS: run model ...... ");
            if(USBCameraActivity1.one==0) {
                predictor.setInputImage(image2);
                USBCameraActivity1.one = 0;}
            else if (USBCameraActivity1.one==1) {
                predictor.setInputImage(image);
                USBCameraActivity1.one = 0;
            }
            runModel();

        }
    }

    public void onLoadModelFailed() {
        tvStatus.setText("STATUS: load model failed");
    }

    public void onRunModelSuccessed() {

        Bitmap outputImage = predictor.outputImage();
        if (outputImage != null) {
            ivInputImage.setImageBitmap(outputImage);
        }

        if (MainActivity.textToSpeech!=null){
            //释放资源
            MainActivity.textToSpeech.stop();
        }
        textToSpeech.speak(predictor.outputResult, TextToSpeech.QUEUE_FLUSH, null);
        tvOutputResult.setText(predictor.outputResult());
        read = predictor.outputResult;
        tvOutputResult.scrollTo(0, 0);
    }

    public void onRunModelFailed() {
        tvStatus.setText("STATUS: run model failed");
    }

    public void onImageChanged(Bitmap image) {
        // Rerun model if users pick test image from gallery or camera
        if (image != null && predictor.isLoaded()) {
            predictor.setInputImage(image);
            runModel();
        }
    }

    public void set_img() {
        // Load test image from path and run model
        try {
            assetManager= getAssets();
            InputStream in=assetManager.open(imagePath);
            Bitmap bmp=BitmapFactory.decodeStream(in);
            ivInputImage.setImageBitmap(bmp);
        } catch (IOException e) {
            Toast.makeText(MainActivity2.this, "Load image failed!", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_action_options, menu);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean isLoaded = predictor.isLoaded();
        return super.onPrepareOptionsMenu(menu);
    }





    private boolean requestAllPermissions() {
        return true;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(intent, OPEN_GALLERY_REQUEST_CODE);
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".bmp",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case 1:
                    String pic_path = data.getStringExtra("data_return");
                    if (pic_path != null) {
                        ExifInterface exif = null;
                        try {
                            exif = new ExifInterface(pic_path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_UNDEFINED);
                        Log.i(TAG, "rotation " + orientation);
                        Bitmap image = BitmapFactory.decodeFile(pic_path);
                        image = Utils.rotateBitmap(image, orientation);
                        if (image != null) {
                            //                            onImageChanged(image);
                            ivInputImage.setImageBitmap(image);
                        }
                    } else {
                        Log.e(TAG, "pic_path is null");
                    }
                    break;

                //打开图册请求
                case OPEN_GALLERY_REQUEST_CODE:
                    if (data == null) {
                        break;
                    }
                    try {
                        ContentResolver resolver = getContentResolver();
                        Uri uri = data.getData();
                        Bitmap image = MediaStore.Images.Media.getBitmap(resolver, uri);
                        String[] proj = {MediaStore.Images.Media.DATA};
                        Cursor cursor = managedQuery(uri, proj, null, null, null);
                        cursor.moveToFirst();
                        if (image != null) {
                            //                            onImageChanged(image);
                            ivInputImage.setImageBitmap(image);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, e.toString());
                    }
                    break;

                default:
                    break;
            }
        }

    }


    //点击选取图片
//    public void btn_choice_img_click(View view) {
//
//        if (textToSpeech!=null){
//            textToSpeech.stop();
//        }
//
//        Intent intent = new Intent(MainActivity2.this, USBCameraActivity1.class);
//        startActivityForResult(intent, 1);
//    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            //初始化tts引擎
            int result = textToSpeech.setLanguage(Locale.CHINA);
            //设置参数
            ttsParam();
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "语音包丢失或语音不支持", Toast.LENGTH_SHORT).show();

            }
        }
    }
    private void ttsParam() {
        textToSpeech.setPitch(1.0f);// 设置音调，,1.0是常规
        textToSpeech.setSpeechRate(1.0f);//设定语速，1.0正常语速
    }
    @Override
    protected void onDestroy() {
        if (predictor != null) {
            predictor.releaseModel();
        }
        if (textToSpeech!=null){
            //释放资源
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        worker.quit();
        super.onDestroy();
    }
}