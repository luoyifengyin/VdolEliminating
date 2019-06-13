package com.example.vdoleliminating.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.example.vdoleliminating.R;

public class InTheLightOfDarkness extends FrameLayout implements View.OnLayoutChangeListener, View.OnClickListener {

    public interface OnDarknessCancelListener{
        void onDarknessCancel();
    }

    private FrameLayout maskTop, maskBottom, maskLeft, maskRight;

    private View focusedView;

    private boolean lightOff = false;

    private OnDarknessCancelListener listener;

    public InTheLightOfDarkness(@NonNull Context context) {
        super(context);
        init();
    }

    public InTheLightOfDarkness(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public InTheLightOfDarkness(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        FrameLayout[] mask = new FrameLayout[4];
        for(int i = 0;i < 4;i++){
            mask[i] = new FrameLayout(getContext());
            addView(mask[i]);
            ViewGroup.LayoutParams params = mask[i].getLayoutParams();
            params.width = LayoutParams.MATCH_PARENT;
            params.height = LayoutParams.MATCH_PARENT;
            mask[i].setLayoutParams(params);
            mask[i].setBackgroundColor(ContextCompat.getColor(getContext(), R.color.colorMask));
            mask[i].setVisibility(INVISIBLE);
        }
        maskTop = mask[0];
        maskBottom = mask[1];
        maskLeft = mask[2];
        maskRight = mask[3];
    }

    public void bindView(View v){
        if (focusedView != null) focusedView.removeOnLayoutChangeListener(this);
        focusedView = v;
        if (focusedView != null) {
            focusedView.addOnLayoutChangeListener(this);
            onLayoutChange(v, v.getLeft(), v.getTop(), v.getRight(), v.getBottom(), 0,0,0,0);
        }
    }

    public void turnOffTheLight(boolean off){
        lightOff = off;
        if (off){
            maskTop.setVisibility(View.VISIBLE);
            maskBottom.setVisibility(View.VISIBLE);
            maskLeft.setVisibility(View.VISIBLE);
            maskRight.setVisibility(View.VISIBLE);
            maskTop.bringToFront();
            maskBottom.bringToFront();
            maskLeft.bringToFront();
            maskRight.bringToFront();
        }
        else {
            maskTop.setVisibility(View.INVISIBLE);
            maskBottom.setVisibility(View.INVISIBLE);
            maskLeft.setVisibility(View.INVISIBLE);
            maskRight.setVisibility(View.INVISIBLE);
        }
    }

    public boolean isLightOff(){
        return lightOff;
    }

    public void setCanceledOnClickDarkness(boolean flag, OnDarknessCancelListener listener){
        if (flag){
            maskTop.setOnClickListener(this);
            maskBottom.setOnClickListener(this);
            maskLeft.setOnClickListener(this);
            maskRight.setOnClickListener(this);
            this.listener = listener;
        }
        else {
            maskTop.setOnClickListener(null);
            maskBottom.setOnClickListener(null);
            maskLeft.setOnClickListener(null);
            maskRight.setOnClickListener(null);
            this.listener = null;
        }
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        int[] loc = new int[2], viewLoc = new int[2];
        getLocationInWindow(loc);
        v.getLocationInWindow(viewLoc);
        left = viewLoc[0] - loc[0]; top = viewLoc[1] - loc[1];
        right = left + v.getWidth(); bottom = top + v.getHeight();

        ViewGroup.LayoutParams params = maskTop.getLayoutParams();
        params.height = top;
        maskTop.setLayoutParams(params);

        maskBottom.setY(bottom);
        params = maskBottom.getLayoutParams();
        params.height = getHeight() - bottom;
        maskBottom.setLayoutParams(params);

        maskLeft.setY(top);
        params = maskLeft.getLayoutParams();
        params.height = bottom - top;
        params.width = left;
        maskLeft.setLayoutParams(params);

        maskRight.setY(top);
        maskLeft.setX(right);
        params = maskRight.getLayoutParams();
        params.height = bottom - top;
        params.width = getWidth() - right;
        maskRight.setLayoutParams(params);
    }

    @Override
    public void onClick(View v) {
        turnOffTheLight(false);
        if (listener != null) listener.onDarknessCancel();
    }
}
