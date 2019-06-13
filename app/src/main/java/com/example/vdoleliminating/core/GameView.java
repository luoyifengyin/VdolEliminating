package com.example.vdoleliminating.core;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArraySet;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.vdoleliminating.R;
import com.example.vdoleliminating.view.square.GridSquare;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import static java.lang.Math.max;
import static java.lang.Math.min;

public class GameView extends GridSquare {

    public static final String TAG = "GameView";

    static final int[] dirx = {-1, 1, 0, 0};
    static final int[] diry = {0, 0, -1, 1};

    private static final int BURST_RANGE = 2;
    private static final int BOOST_RANGE = 4;

    private static final int MULPOWER_LINE = 2;
    private static final int MULPOWER_BURST = 3;
    private static final int MULPOWER_MAGIC = 5;

    private static final short REASON_VERTICAL          = 0x0001;
    private static final short REASON_HORIZONTAL        = 0x0002;
    private static final short REASON_EFFECT_LINE       = 0x0004;
    private static final short REASON_EFFECT_BURST      = 0x0008;
    private static final short REASON_EFFECT_MAGIC      = 0x0010;
    private static final short REASON_EFFECT            = 0x001c;
    private static final short REASON_COMPOSE_EFFECT    = 0x0020;

    private static final short NOT_EFFECTIVE            = 0x0800;
    private static final short SAME_ELIMINATING_GROUP   = 0x0400;

    private static final short FLAG_COMPOSE_LINE        = 0x1000;
    private static final short FLAG_COMPOSE_BURST       = 0x2000;
    private static final short FLAG_COMPOSE_MAGIC       = 0x4000;

    private static final int ITEM_SCORE_BASE_INTERVAL = 2000;
    private static final int ITEM_INITIAL_SCORE = 1000;

    private float tileWidth;
    private float tileHeight;
    //View[][] cells = new View[getRowCount()][getColumnCount()];

    private Random random = new Random();
    
    private class State implements Cloneable {
        private Tile[][] gameBoard = new Tile[getRowCount()][getColumnCount()];
        private int score;
        private int step;

        @Override
        protected State clone() throws CloneNotSupportedException {
            State state = (State) super.clone();
            state.gameBoard = new Tile[getRowCount()][getColumnCount()];
            for(int i = 0;i < getRowCount();i++){
                for(int j = 0;j < getColumnCount();j++){
                    state.gameBoard[i][j] = gameBoard[i][j].clone();
                }
            }
            return state;
        }
    }
    
    private State cur, pre;

    private short[][] eliminatedFlag[] = new short[2][getRowCount()][getColumnCount()];
    private int pE = 0;
    private byte[][] composed = new byte[getRowCount()][getColumnCount()];
    private int[][] scoreMat = new int [getRowCount()][getColumnCount()];
    private int[][] scoreMat2 = new int[getRowCount()][getColumnCount()];
    
    private int combo = 0;
    private int chain = 0;
    private int eliminatingGroupCnt = 0;

    Tile selectedTile = null;
    private boolean actionLocked = false;

    private int nextGoalScore, itemScoreInterval;
    private boolean itemProvideLocked;
    private int itemProvideCnt;

    AnimatorSet animatorSet = new AnimatorSet();

    private GameListener listener;
    private TextView[][] scoreTextMat = null;

    private byte mBonus = 0;
    private View mBonusOpening;

    public GameView(Context context) {
        this(context, null);
    }

    public GameView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GameView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        for(int i = 0;i < getRowCount();i++) {
            for(int j = 0;j < getColumnCount();j++) {
                View cell = LayoutInflater.from(getContext()).inflate(R.layout.empty_area, this, false);
                addView(cell);
                //cells[i][j] = cell;
            }
        }
        cur = new State();

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        tileWidth = (getWidth() - getPaddingLeft() - getPaddingRight()) * 1f / getColumnCount();
        tileHeight = (getHeight() - getPaddingTop() - getPaddingBottom()) * 1f / getRowCount();
    }

    public float getTileWidth(){
        return tileWidth;
    }

    public float getTileHeight() {
        return tileHeight;
    }

    private void putVdol(Tile tile, int row, int col){
        cur.gameBoard[row][col] = tile;
        LayoutParams params = new LayoutParams(spec(row,1f),spec(col,1f));
        params.width = 0;
        params.height = 0;
        addView(tile,params);
    }

    private Tile createVdol(int x, int y, boolean put){
        Tile tile = (Tile)LayoutInflater.from(getContext()).inflate(R.layout.tile, this, false);
        tile.init(this, x, y, random.nextInt(Tile.TOTAL_COLOR));
        if (put) putVdol(tile, x, y);
        return tile;
    }

    private void removeVdol(Tile tile){
        if (tile == null) return;
        int r = tile.getRow(), c = tile.getCol();
        cur.gameBoard[r][c] = null;
        removeView(tile);
    }

    public void initGame(int totalStep){
        pre = null;
        mBonus = 0;
        actionLocked = false;
        do {
            for (int i = 0; i < cur.gameBoard.length; i++) {
                for (int j = 0; j < cur.gameBoard[i].length; j++) {
                    if (cur.gameBoard[i][j] != null) {
                        removeView(cur.gameBoard[i][j]);
                    }
                    Tile tile;
                    short color;
                    do {
                        tile = createVdol(i, j, false);
                        color = tile.getColor();
                    } while ((isColorEqual(i - 1, j, color) && isColorEqual(i - 2, j, color)) ||
                            (isColorEqual(i, j - 1, color) && isColorEqual(i, j - 2, color)));
                    putVdol(tile, i, j);

                    eliminatedFlag[0][i][j] = eliminatedFlag[1][i][j] = 0;
                    composed[i][j] = 0;
                    scoreMat[i][j] = scoreMat2[i][j] = 0;
                }
            }
        } while(!isEliminatableByOnestep());

        changeScore(0);
        changeStep(totalStep);
        itemScoreInterval = ITEM_SCORE_BASE_INTERVAL;
        nextGoalScore = ITEM_INITIAL_SCORE;
        itemProvideLocked = false;
        itemProvideCnt = 0;
    }

    private void shuffle(){
        int n = getRowCount(), m = getColumnCount();
        List<Integer> arr = new ArrayList<>();
        for(int i = 0;i < n * m;i++){
            arr.add(i);
        }
        Collections.shuffle(arr, random);

        Tile[][] tmp = new Tile[getRowCount()][getColumnCount()];
        for(int i = 0;i < arr.size();i++){
            int index = arr.get(i);
            tmp[index / n][index % m] = cur.gameBoard[i / n][i % m];
            cur.gameBoard[i / n][i % m].translateX(index % m - i % m);
            cur.gameBoard[i / n][i % m].translateY(index / n - i / n);
        }
        for(int i = 0;i < n;i++){
            for(int j = 0;j < m;j++){
                cur.gameBoard[i][j] = tmp[i][j];
            }
        }

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animatorSet = new AnimatorSet();
                if (checkEliminatable()) startElimination();
                else if (!isEliminatableByOnestep()) {
                    Toast.makeText(getContext(), R.string.shuffle, Toast.LENGTH_SHORT).show();
                    shuffle();
                }
                if (!animatorSet.isRunning()) onActionEnd();
            }
        });
        animatorSet.setDuration(1000);
        animatorSet.start();
    }

    public boolean isInRegionOfGame(int x, int y){
        return 0 <= x && x < cur.gameBoard.length && 0 <= y && y < cur.gameBoard[x].length;
    }

    private boolean isColorEqual(int x, int y, short color){
        return isInRegionOfGame(x, y) && cur.gameBoard[x][y].getColor() == color;
    }

    private boolean checkEliminatable(int x, int y){
        if (!cur.gameBoard[x][y].isEliminatable()) return false;
        short color = cur.gameBoard[x][y].getColor();
        boolean ret = false;
        int[] cnt = new int[4];
        for (int i = 0; i < 4; i++) {
//            if ((eliminatedFlag[pE][x][y] & (1 << (i/2))) != 0){
//                flag = true;
//                i++;
//                continue;
//            }
            int xx = x + dirx[i], yy = y + diry[i];
            while (isColorEqual(xx, yy, color)) {
                cnt[i]++;
                xx += dirx[i];
                yy += diry[i];
            }
            
            if ((i & 1) == 1){
                int num = cnt[i-1] + cnt[i] + 1;
                if (num >= 3){
                    short flag = eliminatedFlag[pE][x][y];
                    ret = true;
                    for(int j = -cnt[i-1];j <= cnt[i];j++){
                        int r = x + j*dirx[i], c = y + j*diry[i];
                        if (eliminatedFlag[pE][r][c] != 0) ret = false;
                        eliminatedFlag[pE][r][c] |= (1 << (i/2));
                    }
                    if (ret){
                        eliminatingGroupCnt++;
                        if (num >= 5 && (eliminatedFlag[pE][x][y] & (1 << (i/2))) == 0){
                            eliminatingGroupCnt += num - 5;
                        }
                    }
                    ret = true;
                    
                    if (num == 4){
                        if ((flag & (1 << (i/2))) == 0) {
                            if (i < 2) composed[x][y] |= Tile.EFFECT_LINE_HORIZONTAL;
                            else composed[x][y] |= Tile.EFFECT_LINE_VERTICAL;
                            for (int j = -cnt[i - 1]; j <= cnt[i]; j++) {
                                eliminatedFlag[pE][x + j * dirx[i]][y + j * diry[i]] |= FLAG_COMPOSE_LINE;
                            }
                        }
                    }
                    else if (num >= 5) {
                        if ((eliminatedFlag[pE][x][y] & FLAG_COMPOSE_MAGIC) == 0) {
                            composed[x][y] |= Tile.EFFECT_MAGIC;
                            for(int j = -cnt[i-1];j <= cnt[i];j++){
                                int r = x + j*dirx[i], c = y + j*diry[i];
                                eliminatedFlag[pE][r][c] |= FLAG_COMPOSE_MAGIC;
                            }
                        }
                    }
                }
            }
        }
        if ((eliminatedFlag[pE][x][y] & 0b11) == 0b11) {
            if ((eliminatedFlag[pE][x][y] & FLAG_COMPOSE_BURST) == 0) {
                composed[x][y] |= Tile.EFFECT_BURST;
                for (int k = 1; k < 4; k += 2) {
                    for (int j = -cnt[k - 1]; j <= cnt[k]; j++) {
                        int r = x + j * dirx[k], c = y + j * diry[k];
                        eliminatedFlag[pE][r][c] |= FLAG_COMPOSE_BURST;
                        if ((composed[x][y] & composed[r][c]) == 0) {
                            composed[x][y] |= composed[r][c];
                            composed[r][c] = 0;
                        }
                    }
                }
            }
            else eliminatingGroupCnt++;
        }
        return ret;
    }

    private boolean checkEliminatable(Tile tile){
        return checkEliminatable(tile.getRow(), tile.getCol());
    }

    private boolean checkEliminatable(List<Tile> list){
        boolean flag = false;
        for(Tile i : list){
            if (checkEliminatable(i)) flag = true;
        }
        return flag;
    }

    private boolean checkEliminatable(){
        boolean flag = false;
        for(int i = 0;i < cur.gameBoard.length;i++) {
            for(int j = 0;j < cur.gameBoard[i].length;j++){
                if (checkEliminatable(i, j)) flag = true;
            }
        }
        return flag;
    }

    private boolean isComposable(Tile a, Tile b){
        return (a.hasEffect() && b.hasEffect()) ||
                a.isEffect(Tile.EFFECT_MAGIC) || b.isEffect(Tile.EFFECT_MAGIC);
    }

    private boolean isEliminatableByOnestep(){
        boolean flag = false;
        for(int i = 0;i < cur.gameBoard.length && !flag;i++){
            for(int j = 0;j < cur.gameBoard[i].length && !flag;j++){
                if (!cur.gameBoard[i][j].isMovable()) continue;
                if (isInRegionOfGame(i + 1, j) && cur.gameBoard[i+1][j].isMovable()) {
                    if (isComposable(cur.gameBoard[i][j], cur.gameBoard[i+1][j])){
                        flag = true;
                        break;
                    }
                    swap(i, j, i + 1, j);
                    if (checkEliminatable(i, j) || checkEliminatable(i + 1, j))
                        flag = true;
                    swap(i, j, i + 1, j);
                }
                if (!flag && isInRegionOfGame(i, j + 1) && cur.gameBoard[i][j+1].isMovable()){
                    if (isComposable(cur.gameBoard[i][j], cur.gameBoard[i][j+1])){
                        flag = true;
                        break;
                    }
                    swap(i, j, i, j + 1);
                    if (checkEliminatable(i, j) || checkEliminatable(i, j + 1))
                        flag = true;
                    swap(i, j, i, j + 1);
                }
            }
        }
        if (flag) {
            eliminatingGroupCnt = 0;
            for(int i = 0; i < eliminatedFlag[pE].length; i++){
                for(int j = 0; j < eliminatedFlag[pE][i].length; j++){
                    eliminatedFlag[pE][i][j] = 0;
                    composed[i][j] = 0;
                    //scoreMat[i][j] = scoreMat2[i][j] = 0;
                }
            }
        }
        return flag;
    }

    private void swap(int x, int y, int x2, int y2){
        Tile tmp = cur.gameBoard[x][y];
        cur.gameBoard[x][y] = cur.gameBoard[x2][y2];
        cur.gameBoard[x2][y2] = tmp;
    }

    private void burstBfs(int x, int y, int depth){
        int[][] vis = new int[getRowCount()][getColumnCount()];
        vis[x][y] = depth;
        byte r = REASON_EFFECT_BURST;
        if ((eliminatedFlag[pE][x][y] & REASON_COMPOSE_EFFECT) != 0)
            r |= REASON_COMPOSE_EFFECT;
        Queue<Point> q = new LinkedList<>();
        q.offer(new Point(x, y));
        while(!q.isEmpty()) {
            x = q.peek().x; y = q.peek().y;
            q.poll();
            eliminatedFlag[pE][x][y] |= r;
            if (vis[x][y] == 0) continue;
            for (int i = 0; i < 4; i++) {
                int xx = x + dirx[i];
                int yy = y + diry[i];
                if (isInRegionOfGame(xx, yy) && vis[xx][yy] == 0){
                    vis[xx][yy] = vis[x][y] - 1;
                    q.offer(new Point(xx, yy));
                }
            }
        }
    }

    private void expand(int x, int y, boolean composed){
        if (!isInRegionOfGame(x, y)) return;
        Tile tile = cur.gameBoard[x][y];
        if (tile == null) return;
        if (!tile.hasEffect() || (eliminatedFlag[1-pE][x][y] & NOT_EFFECTIVE) != 0) return;

        if ((eliminatedFlag[1-pE][x][y] & SAME_ELIMINATING_GROUP) != 0)
            eliminatingGroupCnt = 1;
        else eliminatingGroupCnt++;
        short r = composed ? REASON_COMPOSE_EFFECT : 0;
        if (tile.isEffect(Tile.EFFECT_LINE_HORIZONTAL)) {
            for (int i = 0; i < cur.gameBoard[x].length; i++) {
                eliminatedFlag[pE][x][i] |= REASON_EFFECT_LINE | r;
            }
        }
        if (tile.isEffect(Tile.EFFECT_LINE_VERTICAL)) {
            for (int i = 0; i < cur.gameBoard.length; i++) {
                eliminatedFlag[pE][i][y] |= REASON_EFFECT_LINE | r;
            }
        }
        if (tile.isEffect(Tile.EFFECT_BURST)) {
            burstBfs(x, y, BURST_RANGE);
        }
        if (tile.isEffect(Tile.EFFECT_MAGIC)) {
            short color = (short) random.nextInt(Tile.TOTAL_COLOR);
            for (int i = 0; i < cur.gameBoard.length; i++) {
                for (int j = 0; j < cur.gameBoard[i].length; j++) {
                    if (cur.gameBoard[i][j] != null && cur.gameBoard[i][j].getColor() == color) {
                        eliminatedFlag[pE][i][j] |= REASON_EFFECT_MAGIC | r;
                    }
                }
            }
        }
    }

    private void expand(int x, int y){
        expand(x, y, false);
    }

    private void onScoreCal(int x, int y){
        Tile tile = cur.gameBoard[x][y];
        if (tile == null) return;
        if (tile.getType() != Tile.TYPE_NORMAL) return;

        int mulpower = 1;
        if (!tile.hasEffect()) {
            int mul = 0, cnt = 0;
            if ((eliminatedFlag[pE][x][y] & REASON_EFFECT_MAGIC) != 0) {
                mul += MULPOWER_MAGIC;
                cnt++;
            }
            if ((eliminatedFlag[pE][x][y] & REASON_EFFECT_BURST) != 0) {
                if (mul == 0 || (eliminatedFlag[pE][x][y] & REASON_COMPOSE_EFFECT) != 0)
                    mul += MULPOWER_BURST;
                cnt++;
            }
            if ((eliminatedFlag[pE][x][y] & REASON_EFFECT_LINE) != 0) {
                if (mul == 0 || (eliminatedFlag[pE][x][y] & REASON_COMPOSE_EFFECT) != 0)
                    mul += MULPOWER_LINE;
                cnt++;
            }
            if ((eliminatedFlag[pE][x][y] & REASON_COMPOSE_EFFECT) != 0) {
                if (cnt == 1) mul *= 2;
                mul++;
            }
            if (mul > 0) mulpower *= mul;
        }
        else if ((eliminatedFlag[pE][x][y] & REASON_COMPOSE_EFFECT) != 0){
            mulpower *= 2;
        }
        int c = chain >= 0 ? chain : 0;
        scoreMat[x][y] = max(scoreMat[x][y], tile.getContributeScore() * (mulpower + c) * eliminatingGroupCnt);
        if (scoreTextMat != null && scoreTextMat[x][y] != null) {
            if (0 <= tile.getColor() && tile.getColor() < Tile.TOTAL_COLOR) {
                int colorId = getResources().getIdentifier("t" + tile.getColor(), "color", getContext().getPackageName());
                scoreTextMat[x][y].setTextColor(ContextCompat.getColor(getContext(), colorId));
            } else scoreTextMat[x][y].setTextColor(Color.WHITE);
        }
    }

    private void onScoreCal(Tile tile){
        if (tile != null) onScoreCal(tile.getRow(), tile.getCol());
    }

    @SuppressLint("SetTextI18n")
    private Animator createTileScoreAnimator(int x, int y, int val, boolean needSetColor){
        if (scoreTextMat != null && scoreTextMat[x][y] != null){
            scoreTextMat[x][y].setText("" + val);
            if (needSetColor) {
                Tile tile = cur.gameBoard[x][y];
                if (tile != null) {
                    if (0 <= tile.getColor() && tile.getColor() < Tile.TOTAL_COLOR) {
                        int colorId = getResources().getIdentifier("t" + tile.getColor(), "color", getContext().getPackageName());
                        scoreTextMat[x][y].setTextColor(ContextCompat.getColor(getContext(), colorId));
                    } else scoreTextMat[x][y].setTextColor(Color.WHITE);
                }
            }
            Animator animator = AnimatorInflater.loadAnimator(getContext(), R.animator.score_show);
            animator.setTarget(scoreTextMat[x][y]);
            return animator;
        }
        return null;
    }

    private AnimatorSet gainScore(){
        AnimatorSet scoreAnimator = new AnimatorSet();
        int sum = 0;
        for(int i = 0;i < scoreMat.length;i++){
            for(int j = 0;j < scoreMat[i].length;j++){
                if (scoreMat2[i][j] > 0){
                    scoreMat2[i][j] = scoreMat2[i][j] * combo;
                    Animator animator = createTileScoreAnimator(i, j, scoreMat2[i][j], false);
                    if (animator != null) scoreAnimator.play(animator);
                    sum += scoreMat2[i][j];
                    scoreMat[i][j] = scoreMat2[i][j] = 0;
                }
            }
        }
        changeScore(cur.score + sum);
        return scoreAnimator;
    }

    private void startElimination(){
        int[] eliminatingCntArr = new int[Tile.TOTAL_COLOR];
        int n = getRowCount(), m = getColumnCount();
        ++combo;
        do {
            for(int i = 0; i < n; i++){
                for(int j = 0; j < m; j++){
                    if (eliminatedFlag[pE][i][j] != 0) {
                        onScoreCal(i, j);
                    }
                }
            }

            eliminatingGroupCnt = 0;
            pE = 1 - pE;
            for(int i = 0; i < n; i++){
                for(int j = 0; j < m; j++){
                    if (eliminatedFlag[1-pE][i][j] != 0) {
                        short flag = eliminatedFlag[1-pE][i][j];
                        expand(i, j);
                        eliminatedFlag[1-pE][i][j] = 0;
                        Tile tile = cur.gameBoard[i][j];
                        if (tile == null) continue;
                        tile.eliminate();
                        cur.gameBoard[i][j] = null;

                        if (tile.getType() == Tile.TYPE_BALLOON){
                            changeStep(cur.step + 5);
                        }
                        else if (tile.getType() == Tile.TYPE_BOX){
                            int itemType = random.nextInt(Item.TOTAL_TYPE);
                            if (listener != null) listener.onProduceItem(itemType, i, j);
                        }

                        if (composed[i][j] != 0) {
                            Tile newTile = (Tile) LayoutInflater.from(getContext()).inflate(R.layout.tile, this, false);
                            newTile.init(this, i, j, tile.getColor(), Tile.TYPE_NORMAL, composed[i][j]);
                            putVdol(newTile, i, j);
                            composed[i][j] = 0;
                            if ((flag & REASON_EFFECT) != 0){
                                expand(i, j);
                            }
                        }

                        scoreMat2[i][j] += scoreMat[i][j];
                        scoreMat[i][j] = 0;

                        if (0 <= tile.getColor() && tile.getColor() < Tile.TOTAL_COLOR)
                            eliminatingCntArr[tile.getColor()]++;
                    }
                }
            }
            chain++;
        } while(eliminatingGroupCnt > 0);
        chain = 0;

        final AnimatorSet scoreAnimator = gainScore();
        if (listener != null) listener.onEliminate(eliminatingCntArr);

        if (animatorSet.getChildAnimations().size() > 0) {
            animatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    if (scoreAnimator.getChildAnimations().size() > 0)
                        scoreAnimator.start();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    animatorSet = new AnimatorSet();
                    drop();
                    //if (!animatorSet.isRunning()) onActionEnd();
                }
            });
            animatorSet.start();
        }
    }

    private void drop(){
        final ArrayList<Tile> arr = new ArrayList<>(), dropNewArr = new ArrayList<>();
        for(int j = 0;j < cur.gameBoard[0].length;j++){
            for(int i = cur.gameBoard.length - 1, k = i - 1;i >= 0;i--, k--){
                if (cur.gameBoard[i][j] == null){
                    while(k >= 0 && cur.gameBoard[k][j] == null) k--;
                    if (k >= 0) {
                        assert cur.gameBoard[k][j] != null;
                        cur.gameBoard[k][j].translateY(i - k);
                        cur.gameBoard[i][j] = cur.gameBoard[k][j];
                        cur.gameBoard[k][j] = null;
                    }
                    else {
                        createVdol(i, j, true);
                        Animator animator = ObjectAnimator.ofFloat(cur.gameBoard[i][j],
                                "translationY",
                                (k-i) * tileHeight, 0);
                        animatorSet.play(animator);
                        dropNewArr.add(cur.gameBoard[i][j]);
                    }
                    arr.add(cur.gameBoard[i][j]);
                }
            }
        }
        if (itemProvideCnt > 0 && !itemProvideLocked){
            dropNewArr.get(random.nextInt(dropNewArr.size())).setType(Tile.TYPE_BOX);
            itemProvideCnt--;
            itemProvideLocked = true;
        }
        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animatorSet = new AnimatorSet();
                if (checkEliminatable(arr)) startElimination();
                else if (!isEliminatableByOnestep()) {
                    Toast.makeText(getContext(), R.string.shuffle, Toast.LENGTH_SHORT).show();
                    shuffle();
                }
                if (!animatorSet.isRunning()) onActionEnd();
            }
        });
        animatorSet.setDuration(100);
        animatorSet.start();
    }

    private boolean composeEffect(Tile a, Tile b){
        if (!isComposable(a, b)) return false;
        if (a.getEffect() > b.getEffect()){
            Tile tmp = a;
            a = b;
            b = tmp;
        }

        if (b.isEffect(Tile.EFFECT_MAGIC)){
            eliminatedFlag[pE][b.getRow()][b.getCol()] |= NOT_EFFECTIVE | SAME_ELIMINATING_GROUP;
            if (a.isEffect(Tile.EFFECT_MAGIC)){
                eliminatingGroupCnt = 4;
                for(int i = 0;i < cur.gameBoard.length;i++){
                    for(int j = 0;j < cur.gameBoard[i].length;j++){
                        eliminatedFlag[pE][i][j] |= NOT_EFFECTIVE | REASON_EFFECT_MAGIC
                                | REASON_COMPOSE_EFFECT | SAME_ELIMINATING_GROUP;
                    }
                }
            }
            else {
                eliminatingGroupCnt++;
                for(int i = 0;i < cur.gameBoard.length;i++){
                    for(int j = 0;j < cur.gameBoard[i].length;j++){
                        if (cur.gameBoard[i][j].getColor() == a.getColor()){
                            eliminatedFlag[pE][i][j] |= REASON_EFFECT_MAGIC | SAME_ELIMINATING_GROUP;
                            Animator animator = AnimatorInflater.loadAnimator(getContext(), R.animator.magic_enlarge);
                            animator.setTarget(cur.gameBoard[i][j]);
                            animatorSet.play(animator);
                            if (cur.gameBoard[i][j] == a) continue;
                            if (a.hasEffect()) {
                                if (a.isLineEffect()) {
                                    if (random.nextBoolean())
                                        cur.gameBoard[i][j].setEffect(Tile.EFFECT_LINE_VERTICAL);
                                    else
                                        cur.gameBoard[i][j].setEffect(Tile.EFFECT_LINE_HORIZONTAL);
                                }
                                else cur.gameBoard[i][j].setEffect(a.getEffect());
                            }
                        }
                    }
                }
                animatorSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        animatorSet = new AnimatorSet();
                        startElimination();
                    }
                });
                animatorSet.start();
                return true;
            }
        }
        else if (a.isLineEffect() && b.isLineEffect()){
            expand(a.getRow(), a.getCol(), true);
            expand(b.getRow(), b.getCol(), true);
            eliminatingGroupCnt--;
            chain++;
            eliminatedFlag[pE][a.getRow()][a.getCol()] |= NOT_EFFECTIVE;
            eliminatedFlag[pE][b.getRow()][b.getCol()] |= NOT_EFFECTIVE;
        }
        else if (a.isLineEffect() && b.isEffect(Tile.EFFECT_BURST)){
            eliminatedFlag[pE][b.getRow()][b.getCol()] |= NOT_EFFECTIVE;
            eliminatedFlag[pE][a.getRow()][a.getCol()] |= NOT_EFFECTIVE;
            eliminatingGroupCnt++;
            chain++;
            if (a.isEffect(Tile.EFFECT_LINE_VERTICAL)){
                int ac = a.getCol(), bc = b.getCol();
                int low = min(ac, bc) - 1, high = max(ac, bc) + 1;
                if (high - low < 3){
                    if (random.nextBoolean()) low--;
                    else high++;
                }
                for(int i = low;i <= high;i++){
                    for(int j = 0;j < getRowCount();j++){
                        if (isInRegionOfGame(j, i)) {
                            eliminatedFlag[pE][j][i] |= REASON_EFFECT_LINE | REASON_EFFECT_BURST
                                    | REASON_COMPOSE_EFFECT;
                        }
                    }
                }
            }
            else if (a.isEffect(Tile.EFFECT_LINE_HORIZONTAL)){
                int ar = a.getRow(), br = b.getRow();
                int low = min(ar, br) - 1, high = max(ar, br) + 1;
                if (high - low < 3){
                    if (random.nextBoolean()) low--;
                    else high++;
                }
                for(int i = low;i <= high;i++){
                    for(int j = 0;j < getColumnCount();j++){
                        if (isInRegionOfGame(i, j)) {
                            eliminatedFlag[pE][i][j] |= REASON_EFFECT_LINE | REASON_EFFECT_BURST
                                    | REASON_COMPOSE_EFFECT;
                        }
                    }
                }
            }
        }
        else if (a.isEffect(Tile.EFFECT_BURST) && b.isEffect(Tile.EFFECT_BURST)){
            eliminatedFlag[pE][a.getRow()][a.getCol()] |= NOT_EFFECTIVE | REASON_COMPOSE_EFFECT;
            eliminatedFlag[pE][b.getRow()][b.getCol()] |= NOT_EFFECTIVE | REASON_COMPOSE_EFFECT;
            burstBfs(a.getRow(), a.getCol(), BOOST_RANGE);
            burstBfs(b.getRow(), b.getCol(), BOOST_RANGE);
            eliminatingGroupCnt += 1;
            chain++;
        }
        return false;
    }

    public void swapVdol(final Tile a, final Tile b, final boolean force){
        onActionStart();

        int offsetax = b.getCol() - a.getCol();
        a.translateX(offsetax);
        b.translateX(-offsetax);
        int offsetay = b.getRow() - a.getRow();
        a.translateY(offsetay);
        b.translateY(-offsetay);

        swap(a.getRow(), a.getCol(), b.getRow(), b.getCol());

        animatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animatorSet = new AnimatorSet();
                boolean flag = false;
                flag = checkEliminatable(a) || flag;
                flag = checkEliminatable(b) || flag;
                boolean composable = false;
                if (!force) {
                    composable = isComposable(a, b);
                    flag = composable || flag;
                }
                if (flag) {
                    if (!force) {
                        swap(a.getRow(), a.getCol(), b.getRow(), b.getCol());
                        backup();
                        swap(a.getRow(), a.getCol(), b.getRow(), b.getCol());
                        changeStep(cur.step - 1);
                    }
                    if (composable){
                        if (composeEffect(a, b)) return;
                    }
                    startElimination();
                }
                else if (!force) swapVdol(a, b, true);
                if (!animatorSet.isRunning()) onActionEnd();
            }
        });
        animatorSet.setDuration(200);
        animatorSet.start();
    }

    public void swapVdol(Tile a, Tile b){
        swapVdol(a, b, false);
    }

    public void swapVdolByItem(Tile a, Tile b){
        backup();
        swapVdol(a, b, true);
    }

    public boolean isNeighbour(Tile a, Tile b){
        int ar = a.getRow(), ac = a.getCol();
        int br = b.getRow(), bc = b.getCol();
        for (int i = 0; i < 4; i++) {
            if (ar + dirx[i] == br && ac + diry[i] == bc) {
                return true;
            }
        }
        return false;
    }

    public void backup(){
        try {
            pre = cur.clone();
            if (listener != null) listener.onBackUp();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    private void onActionStart(){
        actionLocked = true;
        if (selectedTile != null) selectedTile.cancelSelected();
    }

    private void onActionEnd(){
        eliminatingGroupCnt = 0;
        chain = 0;
        combo = 0;
        if (mBonus > 0){
            bonusTime();
            return;
        }
        else if (cur.step == 0) {
            if (listener != null) listener.onGameOver();
            return;
        }
        actionLocked = false;
        itemProvideLocked = false;
    }

    public void win(View bonusOpening){
        if (mBonus != 0) return;
        mBonus = 1;
        mBonusOpening = bonusOpening;
    }

    private boolean bonus(){
        List<Point> list = new LinkedList<>();
        int n = getRowCount(), m = getColumnCount();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (cur.gameBoard[i][j].hasEffect()){
                    list.add(new Point(i, j));
                }
            }
        }
        if (!list.isEmpty()) {
            Point p = list.get(random.nextInt(list.size()));
            eliminatedFlag[pE][p.x][p.y] |= REASON_EFFECT;
            expand(p.x, p.y);
            startElimination();
            return true;
        }
        else return false;
    }

    private void stepBonus(final ArraySet<Tile> tiles){
        if (cur.step > 0) {
            changeStep(cur.step - 1);
            if (!tiles.isEmpty()) {
                final Tile tile = tiles.valueAt(random.nextInt(tiles.size()));
                assert tile != null;
                tiles.remove(tile);
                Animator animator = AnimatorInflater.loadAnimator(getContext(), R.animator.bonus);
                animator.setTarget(tile);
                ArrayList<Animator> list = ((AnimatorSet)animator).getChildAnimations();

                list.get(0).addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (random.nextBoolean())
                            tile.setEffect(Tile.EFFECT_LINE_HORIZONTAL);
                        else tile.setEffect(Tile.EFFECT_LINE_VERTICAL);
                        changeScore(cur.score + 2500);
                        stepBonus(tiles);
                    }
                });

                animatorSet.play(animator);
                animator = createTileScoreAnimator(tile.getRow(), tile.getCol(), 2500, true);
                if (animator != null){
                    list = ((AnimatorSet)animator).getChildAnimations();
                    list.get(1).setDuration(1000);
                    animatorSet.play(animator);
                }

                if (cur.step == 0 || tiles.isEmpty()){
                    animatorSet.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            bonusTime();
                        }
                    });
                }
                animatorSet.start();
                animatorSet = new AnimatorSet();
            }
            else {
                changeScore(cur.score + 10000);
                stepBonus(tiles);
            }
        }
    }

    private void bonusTime(){
        switch(mBonus){
            case 1:
                Animator animator = AnimatorInflater.loadAnimator(getContext(), R.animator.bonus_opening);
                animator.setTarget(mBonusOpening);
                animator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mBonus++;
                        bonusTime();
                    }
                });
                animator.start();
                break;

            case 2:
                if (bonus()) break;
                else mBonus++;

            case 3:
                int n = getRowCount(), m = getColumnCount();
                ArraySet<Tile> set = new ArraySet<>();
                for(int i = 0;i < n;i++){
                    for(int j = 0;j < m;j++){
                        if (!cur.gameBoard[i][j].hasEffect())
                            set.add(cur.gameBoard[i][j]);
                    }
                }
                mBonus++;
                if (cur.step > 0) stepBonus(set);
                else bonusTime();
                break;

            case 4:
                if (bonus()) break;
                else mBonus++;

            case 5:
                if (listener != null) listener.onWin();
                break;
        }
    }

    public boolean refresh(){
        if (actionLocked) {
            Toast.makeText(getContext(), R.string.cannot_use_item, Toast.LENGTH_SHORT).show();
            return false;
        }
        onActionStart();
        backup();
        shuffle();
        return true;
    }

    public boolean returnPrevious(){
        if (actionLocked){
            Toast.makeText(getContext(), R.string.cannot_use_item, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (pre == null) {
            Toast.makeText(getContext(), R.string.cannot_back, Toast.LENGTH_SHORT).show();
            return false;
        }
        for(int i = 0;i < getRowCount();i++){
            for(int j = 0;j < getColumnCount();j++){
                removeVdol(cur.gameBoard[i][j]);
            }
        }
        for(int i = 0;i < getRowCount();i++){
            for(int j = 0;j < getColumnCount();j++){
                Tile t = pre.gameBoard[i][j];
                Tile newTile = (Tile)LayoutInflater.from(getContext()).inflate(R.layout.tile, this, false);
                newTile.init(this, i, j, t.getColor(), t.getType(), t.getAllEffect());
                putVdol(newTile, i, j);
            }
        }
        changeScore(pre.score);
        changeStep(pre.step);
        pre = null;
        return true;
    }

    public boolean eliminate(Tile tile){
        if (actionLocked) return false;
        if (tile == null) return false;
        if (!tile.isEliminatable()) return false;
        onActionStart();
        backup();
        int x = tile.getRow(), y = tile.getCol();
        eliminatedFlag[pE][x][y] |= 0b11;
        //expand(x, y);
        eliminatingGroupCnt = 1;
        startElimination();
        return true;
    }

    public int getScore(){
        return cur.score;
    }

    private void changeScore(int val){
        int delta = val - cur.score;
        cur.score = val;
        while (val > nextGoalScore){
            itemProvideCnt++;
            nextGoalScore += itemScoreInterval;
            itemScoreInterval *= 1.2;
        }
        if (listener != null) listener.onScoreChange(cur.score, delta);
    }

    public int getStep(){
        return cur.step;
    }

    private void changeStep(int val){
        int delta = val - cur.step;
        cur.step = val;
        if (listener != null) listener.onStepChange(cur.step, delta);
    }

    public void setScoreTextMat(TextView[][] textViews){
        //scoreTextMat = textViews;
        int n = getRowCount(), m = getColumnCount();
        scoreTextMat = new TextView[n][m];
        for(int i = 0;i < n;i++){
            System.arraycopy(textViews[i], 0, scoreTextMat[i], 0, m);
        }
    }

    public void setOnGameListener(GameListener listener){
        this.listener = listener;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (actionLocked) return super.onTouchEvent(event);
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                Tile tile = getTile(event.getX(), event.getY());
                if (tile != null) tile.select();
                break;
        }
        return true;
    }

    public Tile getTile(float x, float y){
        int c = (int)((x - getPaddingLeft()) / tileWidth);
        int r = (int)((y - getPaddingTop()) / tileHeight);
        return getTile(r, c);
    }

    public Tile getTile(int r, int c){
        if (isInRegionOfGame(r, c)) return cur.gameBoard[r][c];
        else return null;
    }

    public Tile getSelectedTile(){
        return selectedTile;
    }

    public boolean isActionLocked(){
        return actionLocked;
    }
}
