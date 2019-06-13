package com.example.vdoleliminating.view.square;

import android.content.Context;
import android.support.v7.widget.GridLayout;
import android.util.AttributeSet;

public class GridSquare extends GridLayout {
    public GridSquare(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public GridSquare(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GridSquare(Context context) {
        super(context);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, widthSpec);
    }
}
