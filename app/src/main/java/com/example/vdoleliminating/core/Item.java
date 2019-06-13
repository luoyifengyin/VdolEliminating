package com.example.vdoleliminating.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.vdoleliminating.R;

public class Item extends FrameLayout {

    public static final byte NOT_USING_ITEM = -1;
    public static final byte REFRESH = 0;
    public static final byte BACK = 1;
    public static final byte SWAP = 2;
    public static final byte ELIMINATE = 3;
    public static final int TOTAL_TYPE = 4;

    private boolean _init = false;

    private ImageView mImage;
    private TextView mCornerMark;

    private byte mType;
    private int mCnt;

    public Item(@NonNull Context context) {
        super(context);
    }

    public Item(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Item(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(){
        if (_init) return;
        _init = true;
        mImage = findViewById(R.id.image);
        mCornerMark = findViewById(R.id.cnt);
    }

    public int getType(){
        return mType;
    }

    public void setType(byte type){
        mType = type;
        switch (type){
            case REFRESH:
                mImage.setImageResource(R.drawable.refresh_item);
                break;
            case BACK:
                mImage.setImageResource(R.drawable.back_item);
                break;
            case SWAP:
                mImage.setImageResource(R.drawable.swap_item);
                break;
            case ELIMINATE:
                mImage.setImageResource(R.drawable.eliminate_item);
                break;
        }
    }

    public int getCnt(){
        return mCnt;
    }

    public void consume(){
        setItemCnt(mCnt - 1);
    }

    public void produce(){
        setItemCnt(mCnt + 1);
    }

    @SuppressLint("SetTextI18n")
    public void setItemCnt(int cnt){
        mCnt = cnt;
        if (mCnt > 0){
            mCornerMark.setText("" + mCnt);
            mCornerMark.setVisibility(VISIBLE);
        }
        else mCornerMark.setVisibility(INVISIBLE);
    }
}
