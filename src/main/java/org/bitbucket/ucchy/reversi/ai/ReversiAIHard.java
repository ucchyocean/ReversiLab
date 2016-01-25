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

    private static boolean DEBUG = false;

    private static final int DEPTH = 4;
    private static final int[][] PRIORITY = {
        { 30,-12,  0, -1, -1,  0,-12, 30},
        {-12,-15, -3, -3, -3, -3,-15,-12},
        {  0, -3,  0, -1, -1,  0, -3,  0},
        { -1, -3, -1, -1, -1, -1, -3, -1},
        { -1, -3, -1, -1, -1, -1, -3, -1},
        {  0, -3,  0, -1, -1,  0, -3,  0},
        {-12,-15, -3, -3, -3, -3,-15,-12},
        { 30,-12,  0, -1, -1,  0,-12, 30},
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
        int score = Integer.MIN_VALUE;
        for ( int x=0; x<8; x++ ) {
            for ( int y=0; y<8; y++ ) {
                if ( !board.canPutAt(x, y, piece) ) continue;
                GameBoard temp = board.clone();
                temp.putAt(x, y, piece);
                int s;
                if ( !temp.canPutAll() ) {
                    s = getBoardScore(temp, piece);
                } else {
                    s = getMinMaxScore(temp, piece.getReverse(), false, DEPTH - 1, Integer.MIN_VALUE);
                }

                if ( DEBUG ) {
                    System.out.println(String.format("(%2d,%2d) %3d", x, y, s));
                }

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
     * ミニマックス法でスコアを求める
     * @param board 盤面
     * @param piece 次の手番
     * @param isMax 最大を求めるか、最小を求めるか
     * @param depth 探索深度
     * @param threshold 探索のしきい値
     * @return スコア
     */
    private int getMinMaxScore(GameBoard board, Piece piece, boolean isMax, int depth, int threshold) {

        int score = isMax ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for ( int y=0; y<8; y++ ) {
            for ( int x=0; x<8; x++ ) {
                if ( !board.canPutAt(x, y, piece) ) continue;
                GameBoard temp = board.clone();
                temp.putAt(x, y, piece);
                int s;
                if ( depth <= 0 || !temp.canPutAll() ) {
                    s = getBoardScore(temp, piece);
                } else {
                    s = getMinMaxScore(temp, piece.getReverse(), !isMax, depth - 1, score);
                }

                if ( DEBUG ) {
                    for ( int i=0; i<(DEPTH - depth + 1); i++ ) {
                        System.out.print("  ");
                    }
                    System.out.println(String.format("(%2d,%2d) %3d", x, y, s));
                }

                if ( isMax && score < s ) {
                    score = s;
                    if ( threshold < score ) {
                        return score;
                    }
                } else if ( !isMax && score > s ) {
                    score = s;
                    if ( score < threshold ) {
                        return score;
                    }
                }
            }
        }

        return score;
    }

    /**
     * 現在のボード状況から、スコアを算出して返す。
     * @param board ボード
     * @param piece どちらの手番のスコアか
     * @return スコア
     */
    private int getBoardScore(GameBoard board, Piece piece) {

        int total = 0;

        for ( int y=0; y<8; y++ ) {
            for ( int x=0; x<8; x++ ) {
                if ( board.getPieceAt(x, y) == Piece.EMPTY ) {
                    continue;
                } else if ( board.getPieceAt(x, y) == piece ) {
                    total += PRIORITY[y][x];
                } else {
                    total -= PRIORITY[y][x];
                }
            }
        }

        return total;
    }

    // デバッグエントリ
    public static void main(String[] args) {

        DEBUG = false;
        GameBoard board = new GameBoard();
        ReversiAI ai = new ReversiAIHard();

        board.putAt(4, 2, Piece.BLACK);
        board.putAt(3, 2, Piece.WHITE);
        board.putAt(2, 2, Piece.BLACK);
        board.putAt(5, 4, Piece.WHITE);
        board.putAt(5, 5, Piece.BLACK);
        board.putAt(3, 1, Piece.WHITE);
        board.putAt(2, 0, Piece.BLACK);
        board.putAt(5, 3, Piece.WHITE);
        board.putAt(5, 2, Piece.BLACK);
        board.putAt(3, 0, Piece.WHITE);
        board.putAt(4, 0, Piece.BLACK);
        board.putAt(6, 4, Piece.WHITE);
        board.putAt(7, 5, Piece.BLACK);
        board.putAt(7, 4, Piece.WHITE);
        board.putAt(7, 3, Piece.BLACK);
        //board.putAt(6, 1, Piece.WHITE);

        for ( String line : board.getStringForPrint() ) System.out.println(line);

        DEBUG = true;
        int[] next = ai.getNext(board, Piece.WHITE);
        System.out.println(next[0] + " - " + next[1]);

        board.putAt(next[0], next[1], Piece.WHITE);
        for ( String line : board.getStringForPrint() ) System.out.println(line);
    }
}
