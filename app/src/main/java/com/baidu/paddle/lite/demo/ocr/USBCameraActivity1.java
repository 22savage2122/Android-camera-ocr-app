package com.baidu.paddle.lite.demo.ocr;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.hardware.usb.UsbDevice;
import android.os.Handler;
import android.os.Looper;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;
import com.baidu.paddle.lite.demo.ocr.R;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.jiangdg.usbcamera.UVCCameraHelper;
import com.jiangdg.usbcamera.utils.FileUtils;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.Size;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.common.AbstractUVCCameraHandler;
import com.serenegiant.usb.widget.CameraViewInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
public class USBCameraActivity1 extends AppCompatActivity implements CameraDialog.CameraDialogParent, CameraViewInterface.Callback ,TextToSpeech.OnInitListener{
    private static final String TAG = "Debug";
    @BindView(R.id.camera_view)
    public View mTextureView;
    public  Switch          mSwitchVoice;
    private   MyButton          click;
    public static UVCCameraHelper mCameraHelper;
    private CameraViewInterface mUVCCameraView;
    private AlertDialog mDialog;
    public static final String picPath=UVCCameraHelper.ROOT_PATH  +"USBCamera"+"/images/"
            + 2131 + UVCCameraHelper.SUFFIX_JPEG;

    private boolean isRequest;
    private boolean isPreview;
    public static int  one = 0;
    private static TextToSpeech textToSpeech;

    private UVCCameraHelper.OnMyDevConnectListener listener = new UVCCameraHelper.OnMyDevConnectListener() {

        // ??????USB??????
        @Override
        public void onAttachDev(UsbDevice device) {
            // request open permission
            if (!isRequest) {
                isRequest = true;
                if (mCameraHelper != null) {
                    mCameraHelper.requestPermission(0);
                }
            }

        }

        // ??????USB??????
        @Override
        public void onDettachDev(UsbDevice device) {
            // close camera
            if (isRequest) {
                isRequest = false;
                mCameraHelper.closeCamera();
                showShortMsg(device.getDeviceName() + " is out");
            }
        }

        // ??????USB????????????
        @Override
        public void onConnectDev(UsbDevice device, boolean isConnected)  //???????????????,????????????
        {

            if (!isConnected) {

                isPreview = false;
            } else {
                isPreview = true;

                // initialize seekbar,????????????????????????????????????
                // need to wait UVCCamera initialize over
 // 1.?????????????????????????????????
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(2500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        Looper.prepare();
                        if(mCameraHelper != null && mCameraHelper.isCameraOpened()) {
                            //
                            //                            mSeekBrightness.setProgress(mCameraHelper.getModelValue(UVCCameraHelper.MODE_BRIGHTNESS));
                            //                            mSeekContrast.setProgress(mCameraHelper.getModelValue(UVCCameraHelper.MODE_CONTRAST));
                            mCameraHelper.setModelValue(UVCCameraHelper.MODE_BRIGHTNESS,100);
                            mCameraHelper.setModelValue(UVCCameraHelper.MODE_CONTRAST,50);
                        }
                        Looper.loop();
                    }
                }).start();
            }
        }

        // ???USB??????????????????
        @Override
        public void onDisConnectDev(UsbDevice device) {

        }
    };

    //?????????
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        ButterKnife.bind(this);
        if (textToSpeech==null){
            textToSpeech = new TextToSpeech(this, this);
        }


        // step.1 initialize UVCCameraHelper
        mUVCCameraView = (CameraViewInterface) mTextureView; //????????????
        mUVCCameraView.setCallback(this);
        mCameraHelper = UVCCameraHelper.getInstance();
        mCameraHelper.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG);
        mCameraHelper.initUSBMonitor(this, mUVCCameraView, listener);
        //????????????
        click = (MyButton)findViewById(R.id.click);
        click.setOnDoubleTapListener(new MyButton.OnDoubleTapListener() {
            @Override
            public void onSingleTapConfirmed(MyButton myButton){
                one = 0;
                if (textToSpeech!=null){
                    textToSpeech.stop();
                }
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    return ;
                }

                if (MainActivity2.textToSpeech!=null){
                    //????????????
                    MainActivity2.textToSpeech.stop();
                }

                mCameraHelper.capturePicture(picPath, new AbstractUVCCameraHandler.OnCaptureListener() {
                    @Override
                    public void onCaptureResult(String path) {
                        if(TextUtils.isEmpty(path)) {
                            return;
                        }
                        new Handler(getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(USBCameraActivity1.this, "save path:"+path, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                //        if (mCameraHelper != null) {
                //            mCameraHelper.release();
                //        }
                Intent intent = new Intent(USBCameraActivity1.this, MainActivity.class);
                startActivityForResult(intent, 1);

            }

            @Override
            public void onLongPress(MyButton myButton) {
                textToSpeech.speak("??????????????????????????????????????????????????????????????????", TextToSpeech.QUEUE_FLUSH, null);
            }

            @Override
            public void onDoubleTap(MyButton myButton) {
                one = 1;
                if (textToSpeech!=null){
                    textToSpeech.stop();
                }
                if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
                    return ;
                }

                if (MainActivity2.textToSpeech!=null){
                    //????????????
                    MainActivity2.textToSpeech.stop();
                }

                mCameraHelper.capturePicture(picPath, new AbstractUVCCameraHandler.OnCaptureListener() {
                    @Override
                    public void onCaptureResult(String path) {
                        if(TextUtils.isEmpty(path)) {
                            return;
                        }
                        new Handler(getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(USBCameraActivity1.this, "save path:"+path, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                //        if (mCameraHelper != null) {
                //            mCameraHelper.release();
                //        }
                Intent intent = new Intent(USBCameraActivity1.this, MainActivity2.class);
                startActivityForResult(intent, 2);
            }
        });
    }

//    public void photo_click(View view){
//        if (textToSpeech!=null){
//            textToSpeech.stop();
//        }
//        if (mCameraHelper == null || !mCameraHelper.isCameraOpened()) {
//            return ;
//        }
//
//        if (MainActivity2.textToSpeech!=null){
//            //????????????
//            MainActivity2.textToSpeech.stop();
//        }
//
//        mCameraHelper.capturePicture(picPath, new AbstractUVCCameraHandler.OnCaptureListener() {
//            @Override
//            public void onCaptureResult(String path) {
//                if(TextUtils.isEmpty(path)) {
//                    return;
//                }
//                new Handler(getMainLooper()).post(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(USBCameraActivity1.this, "save path:"+path, Toast.LENGTH_SHORT).show();
//                    }
//                });
//            }
//        });
//        //        if (mCameraHelper != null) {
//        //            mCameraHelper.release();
//        //        }
//        Intent intent = new Intent(USBCameraActivity1.this, MainActivity.class);
//        startActivityForResult(intent, 1);
//    }


    @Override
    protected void onStart() {
        super.onStart();
        // step.2 register USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.registerUSB();
            //mUVCCameraView.onResume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // step.3 unregister USB event broadcast
        if (mCameraHelper != null) {
            mCameraHelper.unregisterUSB();
        }
        //mUVCCameraView.onPause();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.main_toobar, menu);
//        return true;
//    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//
//        }
//        return super.onOptionsItemSelected(item);
//    }

    private List<String> getResolutionList() {
        List<Size> list = mCameraHelper.getSupportedPreviewSizes();
        List<String> resolutions = null;
        if (list != null && list.size() != 0) {
            resolutions = new ArrayList<>();
            for (Size size : list) {
                if (size != null) {
                    resolutions.add(size.width + "x" + size.height);
                }
            }
        }
        return resolutions;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FileUtils.releaseFile();
        if (textToSpeech!=null){
            //????????????
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        // step.4 release uvc camera resources
        if (mCameraHelper != null) {
            mCameraHelper.release();
        }
    }

    private void showShortMsg(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public USBMonitor getUSBMonitor() {
        return mCameraHelper.getUSBMonitor();
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            showShortMsg("????????????");
        }
    }

    public boolean isCameraOpened() {
        return mCameraHelper.isCameraOpened();
    }

//2.???????????????????????????
    @Override
    public void onSurfaceCreated(CameraViewInterface view, Surface surface) //????????????
    {
//                if (!isPreview && mCameraHelper.isCameraOpened()) {
//                    mCameraHelper.startPreview(mUVCCameraView);
//                    isPreview = true;
//                }
    }

    @Override
    public void onSurfaceChanged(CameraViewInterface view, Surface surface, int width, int height) {

    }

    @Override
    public void onSurfaceDestroy(CameraViewInterface view, Surface surface) {
        if (isPreview && mCameraHelper.isCameraOpened()) {
            mCameraHelper.stopPreview();
            isPreview = false;
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            //?????????tts??????
            int result = textToSpeech.setLanguage(Locale.CHINA);
            //????????????
            ttsParam();
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "?????????????????????????????????", Toast.LENGTH_SHORT).show();

            }
        }
    }
    private void ttsParam() {
        textToSpeech.setPitch(1.0f);// ???????????????,1.0?????????
        textToSpeech.setSpeechRate(1.0f);//???????????????1.0????????????
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
