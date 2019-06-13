package com.example.vdoleliminating;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vdoleliminating.core.GameView;
import com.example.vdoleliminating.core.Item;
import com.example.vdoleliminating.core.GameListener;
import com.example.vdoleliminating.core.Tile;
import com.example.vdoleliminating.view.InTheLightOfDarkness;
import com.example.vdoleliminating.view.square.GridSquare;

import java.util.Random;

@SuppressLint("SetTextI18n")
public class PlayActivity extends AppCompatActivity {

    private static final String TAG = "PlayActivity";

    private static final int UP_BOUND = 50;
    private static final int LOW_BOUND = 20;

    private boolean _init = false;

    private ImageView background;

    private GameView gameView;

    private TextView[][] tileScoreMat;

    private TextView scoreText;

    private TextView stepText;

    private ViewGroup mMission;
    private int[] mTaskCntArr;
    private int[] mTaskPreCntArr;

    private Item[] items = new Item[Item.TOTAL_TYPE];
    private byte itemMode = Item.NOT_USING_ITEM;

    private InTheLightOfDarkness mask;

    private View itemDescription;
    private ImageView usingItemImage;
    private TextView usingItemName, usingItemDescription;

    private View bonusOpening;

    private View gameOpening;

    private Button backBtn;

    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play);
        gameView = findViewById(R.id.playground);
        background = findViewById(R.id.background);
        scoreText = findViewById(R.id.score);
        stepText = findViewById(R.id.step);
        mMission = findViewById(R.id.mission);
        mTaskCntArr = new int[mMission.getChildCount()];
        mTaskPreCntArr = new int[mMission.getChildCount()];
        tileScoreMat = new TextView[gameView.getRowCount()][gameView.getColumnCount()];
        GridSquare scoreMat = findViewById(R.id.score_matrix);
        scoreMat.setRowCount(gameView.getRowCount());
        scoreMat.setColumnCount(gameView.getColumnCount());
        for (int i = 0; i < scoreMat.getRowCount(); i++) {
            for (int j = 0; j < scoreMat.getColumnCount(); j++) {
                View view = LayoutInflater.from(this).inflate(R.layout.text_tile_score, scoreMat, false);
                scoreMat.addView(view);
                tileScoreMat[i][j] = (TextView) view;
            }
        }
        mask = findViewById(R.id.darkness);
        items[Item.REFRESH] = findViewById(R.id.refresh);
        items[Item.BACK] = findViewById(R.id.back);
        items[Item.SWAP] = findViewById(R.id.swap);
        items[Item.ELIMINATE] = findViewById(R.id.eliminate);
        for (byte i = 0; i < Item.TOTAL_TYPE; i++) {
            items[i].init();
            items[i].setType(i);
        }
        itemDescription = findViewById(R.id.item_description);
        usingItemImage = itemDescription.findViewById(R.id.item);
        usingItemName = itemDescription.findViewById(R.id.name);
        usingItemDescription = itemDescription.findViewById(R.id.description);
        bonusOpening = findViewById(R.id.bonus_opening);
        gameOpening = findViewById(R.id.game_opening);
        backBtn = findViewById(R.id.back_button);

        int rand = random.nextInt(1);
        int picId = getResources().getIdentifier("bg" + rand, "drawable", getPackageName());
        background.setImageResource(picId);

        gameView.setScoreTextMat(tileScoreMat);
        gameView.setOnGameListener(new GameListener() {
            @Override
            public void onEliminate(int[] eliminatingCntArr) {
                int cnt = 0;
                for (int i = 0; i < mMission.getChildCount(); i++) {
                    View taskBoard = mMission.getChildAt(i);
                    Tile tile = taskBoard.findViewById(R.id.tile);
                    mTaskCntArr[i] -= eliminatingCntArr[tile.getColor()];
                    TextView tv = taskBoard.findViewById(R.id.number);
                    if (mTaskCntArr[i] <= 0) {
                        if (++cnt == mMission.getChildCount())
                            gameView.win(bonusOpening);
                        tv.setVisibility(View.INVISIBLE);
                        taskBoard.findViewById(R.id.mission_complete).setVisibility(View.VISIBLE);
                    } else tv.setText("" + mTaskCntArr[i]);
                }
            }

            @Override
            public void onScoreChange(int score, int delta) {
                scoreText.setText("" + score);
            }

            @Override
            public void onStepChange(int step, int delta) {
                stepText.setText("" + step);
            }

            @Override
            public void onBackUp() {
                System.arraycopy(mTaskCntArr, 0, mTaskPreCntArr, 0, mTaskCntArr.length);
            }

            @Override
            public void onProduceItem(int itemType, int x, int y) {
                items[itemType].produce();

            }

            @Override
            public void onWin() {
                new AlertDialog.Builder(PlayActivity.this)
                        .setTitle(R.string.win)
                        .setMessage(R.string.win_content)
                        .setPositiveButton(R.string.replay, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startGame();
                            }
                        }).setNegativeButton(R.string.cancel, null)
                        .show();
            }

            @Override
            public void onGameOver() {
                new AlertDialog.Builder(PlayActivity.this)
                        .setTitle(R.string.game_over)
                        .setMessage(R.string.fail_content)
                        .setPositiveButton(R.string.replay, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startGame();
                            }
                        }).setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });


        items[Item.REFRESH].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemMode == Item.NOT_USING_ITEM) {
                    if (items[Item.REFRESH].getCnt() == 0) {
                        Toast.makeText(PlayActivity.this, R.string.have_no_item, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (gameView.refresh()) {
                        items[Item.REFRESH].consume();
                    }
                }
            }
        });

        items[Item.BACK].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (itemMode == Item.NOT_USING_ITEM) {
                    if (items[Item.BACK].getCnt() == 0) {
                        Toast.makeText(PlayActivity.this, R.string.have_no_item, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!gameView.returnPrevious()) return;
                    items[Item.BACK].consume();

                    for (int i = 0; i < mMission.getChildCount(); i++) {
                        mTaskCntArr[i] = mTaskPreCntArr[i];
                        View taskBoard = mMission.getChildAt(i);
                        TextView tv = taskBoard.findViewById(R.id.number);
                        ImageView complete = taskBoard.findViewById(R.id.mission_complete);
                        if (mTaskCntArr[i] > 0) {
                            complete.setVisibility(View.INVISIBLE);
                            tv.setText("" + mTaskCntArr[i]);
                            tv.setVisibility(View.VISIBLE);
                        } else {
                            tv.setVisibility(View.INVISIBLE);
                            complete.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });

        items[Item.SWAP].setOnClickListener(new View.OnClickListener() {
            View.OnTouchListener listener = new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_MOVE:
                        case MotionEvent.ACTION_UP:
                            Tile tile = gameView.getTile(event.getX(), event.getY());
                            if (tile != null) {
                                if (!tile.isMovable()) return true;
                                Tile selected = gameView.getSelectedTile();
                                if (selected != null) {
                                    if (gameView.isNeighbour(selected, tile)) {
                                        items[Item.SWAP].consume();
                                        gameView.swapVdolByItem(selected, tile);
                                        items[Item.SWAP].callOnClick();
                                        return true;
                                    }
                                }
                                tile.select();
                            }
                            break;
                        case MotionEvent.ACTION_OUTSIDE:

                    }
                    return true;
                }
            };

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onClick(View v) {
                if (itemMode == Item.NOT_USING_ITEM) {
                    if (items[Item.SWAP].getCnt() == 0) {
                        Toast.makeText(PlayActivity.this, R.string.have_no_item, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (gameView.isActionLocked()) {
                        Toast.makeText(PlayActivity.this, R.string.cannot_use_item, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    setItemMode(Item.SWAP);
                    gameView.setOnTouchListener(listener);
                } else if (itemMode == Item.SWAP) {
                    gameView.setOnTouchListener(null);
                    setItemMode(Item.NOT_USING_ITEM);
                }
            }
        });

        items[Item.ELIMINATE].setOnClickListener(new View.OnClickListener() {
            View.OnTouchListener listener = new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_MOVE:
                        case MotionEvent.ACTION_UP:
                            Tile tile = gameView.getTile(event.getX(), event.getY());
                            if (tile != null) {
                                if (gameView.eliminate(tile)) {
                                    items[Item.ELIMINATE].consume();
                                    items[Item.ELIMINATE].callOnClick();
                                }
                            }
                            break;
                    }
                    return true;
                }
            };

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onClick(View v) {
                if (itemMode == Item.NOT_USING_ITEM) {
                    if (items[Item.ELIMINATE].getCnt() == 0) {
                        Toast.makeText(PlayActivity.this, R.string.have_no_item, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (gameView.isActionLocked()) {
                        Toast.makeText(PlayActivity.this, R.string.cannot_use_item, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    setItemMode(Item.ELIMINATE);
                    gameView.setOnTouchListener(listener);
                } else if (itemMode == Item.ELIMINATE) {
                    gameView.setOnTouchListener(null);
                    setItemMode(Item.NOT_USING_ITEM);
                }
            }
        });

        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(PlayActivity.this)
                        .setTitle("")
                        .setMessage("是否退出游戏？")
                        .setPositiveButton(R.string.exit, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.restart, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startGame();
                            }
                        })
                        .setNeutralButton(R.string.cancel, null)
                        .show();
            }
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (_init) return;
        _init = true;

        for (int i = 0; i < mMission.getChildCount(); i++) {
            View tile = mMission.getChildAt(i).findViewById(R.id.tile);
            ViewGroup.LayoutParams params = tile.getLayoutParams();
            params.width = (int) gameView.getTileWidth();
            params.height = (int) gameView.getTileHeight();
            tile.setLayoutParams(params);
        }

        mask.bindView(gameView);
        mask.setCanceledOnClickDarkness(true, new InTheLightOfDarkness.OnDarknessCancelListener() {
            @Override
            public void onDarknessCancel() {
                switch (itemMode) {
                    case Item.SWAP:
                    case Item.ELIMINATE:
                        items[itemMode].performClick();
                        break;
                }
            }
        });

        startGame();
    }

    private void startGame() {
        boolean[] arr = new boolean[Tile.TOTAL_COLOR];
        for (int i = 0; i < mMission.getChildCount(); i++) {
            int color;
            do {
                color = random.nextInt(Tile.TOTAL_COLOR);
            } while (arr[color]);
            arr[color] = true;
            View taskBoard = mMission.getChildAt(i);
            Tile tile = taskBoard.findViewById(R.id.tile);
            tile.init(null, -1, -1, color);

            int num = random.nextInt(UP_BOUND - LOW_BOUND + 1) + LOW_BOUND;
            mTaskCntArr[i] = mTaskPreCntArr[i] = num;
            TextView tv = taskBoard.findViewById(R.id.number);
            tv.setText("" + num);
            tv.setVisibility(View.VISIBLE);
            taskBoard.findViewById(R.id.mission_complete).setVisibility(View.INVISIBLE);
        }

        items[Item.REFRESH].setItemCnt(1);
        items[Item.BACK].setItemCnt(1);
        items[Item.SWAP].setItemCnt(0);
        items[Item.ELIMINATE].setItemCnt(0);

        gameView.initGame(35);

        gameOpening.setVisibility(View.VISIBLE);
        final Animator endAnimator = ObjectAnimator.ofFloat(gameOpening, "alpha", 1f, 0f);
        endAnimator.setDuration(getResources().getInteger(R.integer.game_opening_appear));
        endAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                gameOpening.setVisibility(View.GONE);
            }
        });
        gameOpening.animate()
                .alpha(1f)
                .setDuration(getResources().getInteger(R.integer.game_opening_appear))
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        gameOpening.animate()
                                .setDuration(getResources().getInteger(R.integer.game_opening_wait))
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        gameOpening.setOnClickListener(null);
                                        endAnimator.start();
                                    }
                                }).start();
                        gameOpening.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                gameOpening.animate().cancel();
                                endAnimator.start();
                                gameOpening.setOnClickListener(null);
                            }
                        });
                    }
                }).start();
    }

    private void setItemMode(byte mode) {
        itemMode = mode;
        if (mode != Item.NOT_USING_ITEM) {
            Tile tile = gameView.getSelectedTile();
            if (tile != null) tile.cancelSelected();
            mask.turnOffTheLight(true);
            switch (mode) {
                case Item.SWAP:
                    usingItemImage.setImageResource(R.drawable.swap_item);
                    usingItemName.setText(R.string.swap);
                    usingItemDescription.setText(R.string.swap_description);
                    break;
                case Item.ELIMINATE:
                    usingItemImage.setImageResource(R.drawable.eliminate_item);
                    usingItemName.setText(R.string.eliminate);
                    usingItemDescription.setText(R.string.eliminate_description);
                    break;
            }
            itemDescription.setVisibility(View.VISIBLE);
            itemDescription.bringToFront();
        } else {
            mask.turnOffTheLight(false);
            itemDescription.setVisibility(View.INVISIBLE);
        }
    }
}
