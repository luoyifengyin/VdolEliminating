package com.example.vdoleliminating.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

import com.example.vdoleliminating.R;

import java.lang.reflect.Field;

public class StrokeTextView extends AppCompatTextView {

    private TextPaint mTextPaint;

    private int textStrokeColor;

    private float textStrokeWidth;

    public StrokeTextView(Context context) {
        this(context, null);
    }

    public StrokeTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 16842884);
    }

    public StrokeTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mTextPaint = getPaint();
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.StrokeTextView, defStyleAttr, 0);
        textStrokeColor = typedArray.getColor(R.styleable.StrokeTextView_textStrokeColor, Color.BLACK);
        textStrokeWidth = typedArray.getDimension(R.styleable.StrokeTextView_textStrokeWidth, 0);
        typedArray.recycle();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (textStrokeWidth > 0){
            int textColor = getCurrentTextColor();
            Paint.Style style = mTextPaint.getStyle();
            setTextColorUseReflection(textStrokeColor);
            mTextPaint.setStrokeWidth(textStrokeWidth);
            mTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            super.onDraw(canvas);

            setTextColorUseReflection(textColor);
            mTextPaint.setStrokeWidth(0);
            mTextPaint.setStyle(style);

        }
        super.onDraw(canvas);
    }

    private void setTextColorUseReflection(int color){
        Field textColorField;
        try {
            textColorField = TextView.class.getDeclaredField("mCurTextColor");
            textColorField.setAccessible(true);
            textColorField.set(this, color);
            textColorField.setAccessible(false);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        mTextPaint.setColor(color);
    }
}
