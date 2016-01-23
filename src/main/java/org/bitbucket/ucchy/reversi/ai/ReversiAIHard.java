/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi.ai;

import org.bitbucket.ucchy.reversi.game.CellState;
import org.bitbucket.ucchy.reversi.game.GameBoard;
import org.bitbucket.ucchy.reversi.game.SingleGameDifficulty;

/**
 * HardのAI
 * @author ucchy
 */
public class ReversiAIHard implements ReversiAI {

    private static final int[][] PRIORITY = {
        {5, 1, 3, 4, 4, 3, 1, 5},
        {1, 0, 1, 1, 1, 1, 0, 1},
        {3, 1, 2, 2, 2, 2, 1, 3},
        {4, 1, 2, 2, 2, 2, 1, 4},
        {4, 1, 2, 2, 2, 2, 1, 4},
        {3, 1, 2, 2, 2, 2, 1, 3},
        {1, 0, 1, 1, 1, 1, 0, 1},
        {5, 1, 3, 4, 4, 3, 1, 5},
    };

    /**
     * @see org.bitbucket.ucchy.reversi.ai.ReversiAI#getDifficulty()
     */
    @Override
    public SingleGameDifficulty getDifficulty() {
        return SingleGameDifficulty.HARD;
    }

    /**
     * @see org.bitbucket.ucchy.reversi.ai.ReversiAI#getNext(org.bitbucket.ucchy.reversi.game.GameBoard, org.bitbucket.ucchy.reversi.game.CellState)
     */
    @Override
    public int[] getNext(GameBoard board, CellState state) {

        int[] coordinates = new int[2];
        int score = 0;
        for ( int x=0; x<8; x++ ) {
            for ( int y=0; y<8; y++ ) {
                int s = getScore(x, y, board, state);
                if ( score < s ) {
                    score = s;
                    coordinates[0] = x;
                    coordinates[1] = y;
                }
            }
        }
        return coordinates;
    }

    private static int getWeight(int x, int y) {

        switch ( PRIORITY[x][y] ) {
        case 0: return 1;
        case 1: return 10;
        case 2: return 100;
        case 3: return 1000;
        case 4: return 10000;
        case 5: return 100000;
        default: return 1;
        }
    }

    /**
     * 5手先読みしてスコアを決める。
     * @param x
     * @param y
     * @param board
     * @param state
     * @return
     */
    private static int getScore(int x, int y, GameBoard board, CellState state) {

        // 置くことができない場所は、スコア0
        if ( !board.canPutAt(x, y, state) ) return 0;

        // まず置いてみる。
        GameBoard first = board.clone();
        first.putAt(x, y, state);

        // 両者とも置けないなら、この時点で自分の石数をスコアとして返す。
        if ( !first.canPutAll() ) return first.getCountOf(state);

        // 相手を予測で置かせてみる。相手がパスになるなら最高点。
        GameBoard second = getNextBoardByPriority(first, state.getReverse());
        if ( second == null ) return 99999;
        if ( !second.canPutAll() ) return second.getCountOf(state);

        // 自分を予測で置かせてみる。自分がパスになるなら低得点。
        GameBoard third = getNextBoardByPriority(second, state);
        if ( third == null ) return 1;
        if ( !third.canPutAll() ) return third.getCountOf(state);

        // 相手を予測で置かせてみる。相手がパスになるなら最高点。
        GameBoard forth = getNextBoardByPriority(third, state.getReverse());
        if ( forth == null ) return 9999;
        if ( !forth.canPutAll() ) return forth.getCountOf(state);

        // 自分を予測で置かせてみる。自分がパスになるなら低得点。
        GameBoard fifth = getNextBoardByPriority(forth, state);
        if ( fifth == null ) return 2;
        return fifth.getCountOf(state);
    }

    private static GameBoard getNextBoardByPriority(GameBoard board, CellState state) {

        // パスになる場合は、nullを返す
        if ( !board.canPut(state) ) return null;

        // 現在おける場所で、裏返すことができる個数 x 重み付け が最大になる場所を探す。
        int[] coordinates = new int[2];
        int value = 0;

        for ( int x=0; x<8; x++ ) {
            for ( int y=0; y<8; y++ ) {
                int v = board.findPath(x, y, state).size() * getWeight(x, y);
                if ( value < v ) {
                    value = v;
                    coordinates[0] = x;
                    coordinates[1] = y;
                }
            }
        }

        // ボードを複製して、実際におく。
        GameBoard next = board.clone();
        next.putAt(coordinates[0], coordinates[1], state);
        return next;
    }
}
