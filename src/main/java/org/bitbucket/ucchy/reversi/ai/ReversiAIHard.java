/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi.ai;

import org.bitbucket.ucchy.reversi.game.GameBoard;
import org.bitbucket.ucchy.reversi.game.Piece;
import org.bitbucket.ucchy.reversi.game.SingleGameDifficulty;

/**
 * HardのAI
 * @author ucchy
 */
public class ReversiAIHard implements ReversiAI {

    private static final int[][] PRIORITY = {
        {120,-20, 20,  5,  5, 20,-20,120},
        {-20,-40, -5, -5, -5, -5,-40,-20},
        { 20, -5, 15,  3,  3, 15, -5, 20},
        {  5, -5,  3,  3,  3,  3, -5,  5},
        {  5, -5,  3,  3,  3,  3, -5,  5},
        { 20, -5, 15,  3,  3, 15, -5, 20},
        {-20,-40, -5, -5, -5, -5,-40,-20},
        {120,-20, 20,  5,  5, 20,-20,120},
    };

    /**
     * @see org.bitbucket.ucchy.reversi.ai.ReversiAI#getDifficulty()
     */
    @Override
    public SingleGameDifficulty getDifficulty() {
        return SingleGameDifficulty.HARD;
    }

    /**
     * @see org.bitbucket.ucchy.reversi.ai.ReversiAI#getNext(org.bitbucket.ucchy.reversi.game.GameBoard, org.bitbucket.ucchy.reversi.game.Piece)
     */
    @Override
    public int[] getNext(GameBoard board, Piece piece) {

        int[] coordinates = new int[2];
        int score = 0;
        for ( int x=0; x<8; x++ ) {
            for ( int y=0; y<8; y++ ) {
                int s = getScore(x, y, board, piece);
                if ( score < s ) {
                    score = s;
                    coordinates[0] = x;
                    coordinates[1] = y;
                }
            }
        }
        return coordinates;
    }

    /**
     * 5手先読みしてスコアを決める。
     * @param x
     * @param y
     * @param board
     * @param piece
     * @return
     */
    private static int getScore(int x, int y, GameBoard board, Piece piece) {

        // 置くことができない場所は、スコア0
        if ( !board.canPutAt(x, y, piece) ) return 0;

        // まず置いてみる。
        GameBoard first = board.clone();
        first.putAt(x, y, piece);

        // 両者とも置けないなら、この時点で自分の石数をスコアとして返す。
        if ( !first.canPutAll() ) return first.getCountOf(piece);

        // 相手を予測で置かせてみる。相手がパスになるなら最高点。
        GameBoard second = getNextBoardByPriority(first, piece.getReverse());
        if ( second == null ) return 99999;
        if ( !second.canPutAll() ) return second.getCountOf(piece);

        // 自分を予測で置かせてみる。自分がパスになるなら低得点。
        GameBoard third = getNextBoardByPriority(second, piece);
        if ( third == null ) return 1;
        if ( !third.canPutAll() ) return third.getCountOf(piece);

        // 相手を予測で置かせてみる。相手がパスになるなら最高点。
        GameBoard forth = getNextBoardByPriority(third, piece.getReverse());
        if ( forth == null ) return 9999;
        if ( !forth.canPutAll() ) return forth.getCountOf(piece);

        // 自分を予測で置かせてみる。自分がパスになるなら低得点。
        GameBoard fifth = getNextBoardByPriority(forth, piece);
        if ( fifth == null ) return 2;
        return fifth.getCountOf(piece);
    }

    private static GameBoard getNextBoardByPriority(GameBoard board, Piece piece) {

        // パスになる場合は、nullを返す
        if ( !board.canPut(piece) ) return null;

        // 現在おける場所で、PRIORITYが最大、かつ、裏返せる個数がなるべく少なくなる場所を探す。
        int[] coordinates = new int[2];
        int priority = -999;
        int value = 999;

        for ( int x=0; x<8; x++ ) {
            for ( int y=0; y<8; y++ ) {
                int p = PRIORITY[x][y];
                int v = board.findPath(x, y, piece).size();
                if ( v == 0 ) continue;
                if ( priority < p || (priority == p && value > v) ) {
                    priority = p;
                    value = v;
                    coordinates[0] = x;
                    coordinates[1] = y;
                }
            }
        }

        // ボードを複製して、実際におく。
        GameBoard next = board.clone();
        next.putAt(coordinates[0], coordinates[1], piece);
        return next;
    }

    // デバッグエントリ
    public static void main(String[] args) {

        GameBoard board = new GameBoard();
        ReversiAI ai = new ReversiAIHard();

        for ( String line : board.getStringForPrint() ) System.out.println(line);

        int[] next = ai.getNext(board, Piece.BLACK);
        System.out.println(next[0] + " - " + next[1]);

        board.putAt(next[0], next[1], Piece.BLACK);
        for ( String line : board.getStringForPrint() ) System.out.println(line);
    }
}
