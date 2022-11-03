package com.baidu.paddle.lite.demo.ocr;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;

/**
 * Created by Administrator on 2015/9/24.
 */
public class MyButton extends androidx.appcompat.widget.AppCompatButton {
    private GestureDetector mGesture;
    public MyButton(Context context) {
        super(context);
    }
    public interface OnDoubleTapListener{
        void onDoubleTap(MyButton myButton);
        void onSingleTapConfirmed(MyButton myButton);
        void onLongPress(MyButton myButton);
    }

    private OnScrollListener onScrollListener;

    public void setOnScrollListener(OnScrollListener onScrollListener){
        this.onScrollListener = onScrollListener;
    }


    public interface OnScrollListener{
        void OnScroll(Button button,float distanceX, float distanceY);
    }

    private OnDoubleTapListener onDoubleTapListener;

    public void setOnDoubleTapListener(OnDoubleTapListener onDoubleTapListener){
        this.onDoubleTapListener = onDoubleTapListener;
    }

    public MyButton(Context context, AttributeSet attrs) {
        super(context, attrs);

        mGesture =new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            public boolean onSingleTapConfirmed(MotionEvent e) {
                onDoubleTapListener.onSingleTapConfirmed(MyButton.this);
                return super.onSingleTapConfirmed(e);
            }
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if(onDoubleTapListener!=null){
                    onDoubleTapListener.onDoubleTap(MyButton.this);
                }
                Log.d("heinikamGesture", "连续点击了两次");
                return super.onDoubleTap(e);
            }

            @Override
            public void onLongPress(MotionEvent e) {
                onDoubleTapListener.onLongPress(MyButton.this);
                super.onLongPress(e);
            }


        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mGesture.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    public MyButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}