package com.example.vdoleliminating.core;

public interface GameListener {
    void onEliminate(int[] eliminatingCntArr);
    void onScoreChange(int score, int delta);
    void onStepChange(int score, int delta);
    void onBackUp();
    void onProduceItem(int itemType, int x, int y);
    void onWin();
    void onGameOver();
}
