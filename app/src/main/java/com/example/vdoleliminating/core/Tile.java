package com.example.vdoleliminating.core;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.vdoleliminating.R;
import com.makeramen.roundedimageview.RoundedImageView;

public class Tile extends FrameLayout implements Cloneable {

    private static final String TAG = "Tile";

    public static final int TOTAL_COLOR = 6;

    public static final byte EFFECT_NONE            = 0;
    public static final byte EFFECT_LINE_VERTICAL   = 0b0001;
    public static final byte EFFECT_LINE_HORIZONTAL = 0b0010;
    public static final byte EFFECT_BURST           = 0b0100;
    public static final byte EFFECT_MAGIC           = 0b1000;

    public static final byte TYPE_NORMAL = 0;
    public static final byte TYPE_BALLOON = 1;
    public static final byte TYPE_BOX = 2;

    private static final int SCORE_NORMAL = 5;
    private static final int SCORE_LINE_EFFECT = 100;
    private static final int SCORE_BURST_EFFET = 200;
    private static final int SCORE_MAGIC_EFFECT = 300;

    private boolean _init = false;

    private GameView gameView;

    private RoundedImageView imageView;

    private int row;
    private int col;

    private short color;
    private byte effect;
    private byte type;

    private boolean eliminatable = true;
    private boolean movable = true;

    private ImageView horizontalEffect, verticalEffect;
    private ImageView selectedImage;
    private TextView textView;

    public Tile(Context context) {
        super(context);
    }

    public Tile(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Tile(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void init(GameView gameView, int r, int c, int color, byte type, byte e) {
        if (!_init) {
            _init = true;
            imageView = findViewById(R.id.image);
            horizontalEffect = findViewById(R.id.horizontal);
            verticalEffect = findViewById(R.id.vertical);
            selectedImage = findViewById(R.id.selected);
            textView = findViewById(R.id.text);
        }

        this.gameView = gameView;
        row = r;
        col = c;
        this.color = (short) color;
        //this.type = type;
        //effect = e;

        setType(type);
        setEffect(e);
    }

    public void init(GameView gameView, int r, int c, int color) {
        init(gameView, r, c, color, TYPE_NORMAL, EFFECT_NONE);
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public short getColor() {
        return color;
    }

    public byte getEffect() {
        if ((effect & EFFECT_MAGIC) != 0) return EFFECT_MAGIC;
        if ((effect & EFFECT_BURST) != 0) return EFFECT_BURST;
        return effect;
    }

    public boolean hasEffect() {
        return effect != EFFECT_NONE;
    }

    public boolean isEffect(byte e) {
        return (getEffect() & e) != 0;
    }

    public void setEffect(byte e) {
        //effect |= e;
        effect = e;
        horizontalEffect.setVisibility(GONE);
        verticalEffect.setVisibility(GONE);
        imageView.setBorderWidth(0f);
        if ((effect & EFFECT_MAGIC) != 0) {
            this.color = -1;
            imageView.setImageResource(R.drawable.magic);
        }
        else if ((effect & EFFECT_BURST) != 0) {
            imageView.setBorderColor(getResources().getColor(R.color.effectBurst));
            imageView.setBorderWidth(R.dimen.effect_burst_border);
        }
        else if ((effect & EFFECT_LINE_HORIZONTAL) != 0) {
            horizontalEffect.setVisibility(View.VISIBLE);
        }
        else if ((effect & EFFECT_LINE_VERTICAL) != 0) {
            verticalEffect.setVisibility(View.VISIBLE);
        }
    }

    public boolean isLineEffect() {
        //return isEffect(EFFECT_LINE_VERTICAL) || isEffect(EFFECT_LINE_HORIZONTAL);
        return isEffect((byte)(EFFECT_LINE_VERTICAL | EFFECT_LINE_HORIZONTAL));
    }

    public byte getAllEffect(){
        return effect;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type){
        this.type = type;
        int id;
        switch (type){
            case TYPE_NORMAL:
                id = getResources().getIdentifier("p" + color, "drawable", getContext().getPackageName());
                imageView.setImageResource(id);
                imageView.setOval(false);
                textView.setVisibility(GONE);
                break;
            case TYPE_BOX:
                id = getResources().getIdentifier("t" + color, "color", getContext().getPackageName());
                imageView.setColorFilter(ContextCompat.getColor(getContext(), id));
                imageView.setOval(true);
                if (color == 3)             //bella
                    textView.setTextColor(Color.BLACK);
                else textView.setTextColor(Color.WHITE);
                textView.setVisibility(VISIBLE);
                break;
        }
    }

    public int getContributeScore(){
        if (effect == EFFECT_NONE) return SCORE_NORMAL;
        int score = 0;
        if ((effect & EFFECT_LINE_VERTICAL) != 0) score += SCORE_LINE_EFFECT;
        if ((effect & EFFECT_LINE_HORIZONTAL) != 0) score += SCORE_LINE_EFFECT;
        if ((effect & EFFECT_BURST) != 0) score += SCORE_BURST_EFFET;
        if ((effect & EFFECT_MAGIC) != 0) score += SCORE_MAGIC_EFFECT;
        return score;
    }

    public void translateX(int offset) {
        col += offset;
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "translationX",
                getTranslationX(), getTranslationX() + offset * gameView.getTileWidth());
        gameView.animatorSet.play(animator);
    }

    public void translateY(int offset) {
        row += offset;
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, "translationY",
                getTranslationY(), getTranslationY() + offset * gameView.getTileHeight());
        gameView.animatorSet.play(animator);
    }

    public void eliminate() {
        Animator animator = AnimatorInflater.loadAnimator(getContext(), R.animator.eliminate);
        animator.setTarget(this);
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                Tile.this.bringToFront();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                gameView.removeView(Tile.this);
            }
        });
        gameView.animatorSet.play(animator);
    }

    public void select(){
        if (!movable) return;
        if (gameView.selectedTile != null) {
            if (gameView.isNeighbour(this, gameView.selectedTile)){
                gameView.swapVdol(gameView.selectedTile, this);
                return;
            }
            gameView.selectedTile.cancelSelected();
        }
        gameView.selectedTile = this;
        selectedImage.setVisibility(VISIBLE);
    }

    public void cancelSelected(){
        gameView.selectedTile = null;
        selectedImage.setVisibility(GONE);
    }

    public boolean isEliminatable() {
        return eliminatable;
    }

    public boolean isMovable(){
        return movable;
    }

    @Override
    public Tile clone() throws CloneNotSupportedException {
        return (Tile) super.clone();
    }
}
