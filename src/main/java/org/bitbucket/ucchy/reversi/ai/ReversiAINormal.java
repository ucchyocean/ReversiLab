package org.bitbucket.ucchy.reversi.ai;

import org.bitbucket.ucchy.reversi.game.GameBoard;
import org.bitbucket.ucchy.reversi.game.Piece;
import org.bitbucket.ucchy.reversi.game.SingleGameDifficulty;

/**
 * NormalのAI
 * @author ucchy
 */
public class ReversiAINormal implements ReversiAI {

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
        return SingleGameDifficulty.NORMAL;
    }

    /**
     * @see org.bitbucket.ucchy.reversi.ai.ReversiAI#getNext(org.bitbucket.ucchy.reversi.game.GameBoard, org.bitbucket.ucchy.reversi.game.Piece)
     */
    @Override
    public int[] getNext(GameBoard board, Piece piece) {

        if ( board.getEmptyCount() > 12 ) {
            // 序盤は、PRIORITYが最大、かつ、裏返せる個数がなるべく少なくなる場所を探す。
            int[] coordinates = new int[2];
            int priority = Integer.MIN_VALUE;
            int value = Integer.MAX_VALUE;

            for ( int x=0; x<8; x++ ) {
                for ( int y=0; y<8; y++ ) {
                    if ( !board.canPutAt(x, y, piece) ) continue;
                    int p = PRIORITY[x][y] + (int)(Math.random() * 10) - 5;
                    int v = board.findPath(x, y, piece).size() + (int)(Math.random() * 4) - 2;
                    if ( priority < p || (priority == p && value > v) ) {
                        priority = p;
                        value = v;
                        coordinates[0] = x;
                        coordinates[1] = y;
                    }
                }
            }

            return coordinates;

        } else {
            // 終盤は、PRIORITYが最大、かつ、裏返せる個数がなるべく多くなる場所を探す。
            int[] coordinates = new int[2];
            int priority = Integer.MIN_VALUE;
            int value = Integer.MIN_VALUE;

            for ( int x=0; x<8; x++ ) {
                for ( int y=0; y<8; y++ ) {
                    if ( !board.canPutAt(x, y, piece) ) continue;
                    int p = PRIORITY[x][y] + (int)(Math.random() * 10) - 5;
                    int v = board.findPath(x, y, piece).size() + (int)(Math.random() * 4) - 2;
                    if ( priority < p || (priority == p && value < v) ) {
                        priority = p;
                        value = v;
                        coordinates[0] = x;
                        coordinates[1] = y;
                    }
                }
            }

            return coordinates;

        }
    }

    public static void main(String[] args) {

        GameBoard board = new GameBoard();
        ReversiAI ai = new ReversiAIHard();

        board.putAt(3, 5, Piece.BLACK);
        board.putAt(2, 5, Piece.WHITE);
        board.putAt(5, 3, Piece.BLACK);
        board.putAt(5, 2, Piece.WHITE);
        board.putAt(4, 2, Piece.BLACK);
        board.putAt(3, 2, Piece.WHITE);
        board.putAt(2, 2, Piece.BLACK);
        board.putAt(2, 4, Piece.WHITE);
        board.putAt(2, 3, Piece.BLACK);
        board.putAt(5, 4, Piece.WHITE);
        board.putAt(5, 5, Piece.BLACK);
        board.putAt(4, 5, Piece.WHITE);
        board.putAt(2, 6, Piece.BLACK);
        board.putAt(1, 3, Piece.WHITE);
        board.putAt(0, 4, Piece.BLACK);
        board.putAt(2, 7, Piece.WHITE);
        board.putAt(1, 5, Piece.BLACK);
        board.putAt(0, 2, Piece.WHITE);
        board.putAt(3, 7, Piece.BLACK);
        board.putAt(4, 7, Piece.WHITE);
        board.putAt(6, 2, Piece.BLACK);
        board.putAt(4, 6, Piece.WHITE);
        board.putAt(0, 3, Piece.BLACK);
        board.putAt(0, 5, Piece.WHITE);
        board.putAt(5, 7, Piece.BLACK);
        board.putAt(6, 7, Piece.WHITE);
        board.putAt(6, 4, Piece.BLACK);
        board.putAt(6, 5, Piece.WHITE);
        board.putAt(7, 5, Piece.BLACK);
        board.putAt(7, 3, Piece.WHITE);
        board.putAt(1, 6, Piece.BLACK);
        board.putAt(1, 4, Piece.WHITE);
        board.putAt(6, 3, Piece.BLACK);
        board.putAt(0, 7, Piece.WHITE);
        board.putAt(5, 6, Piece.BLACK);
        board.putAt(1, 7, Piece.WHITE);
        board.putAt(3, 6, Piece.BLACK);
        board.putAt(7, 2, Piece.WHITE);
        board.putAt(7, 4, Piece.BLACK);
        board.putAt(7, 6, Piece.WHITE);
        board.putAt(0, 6, Piece.BLACK);
        board.putAt(7, 1, Piece.WHITE);
        board.putAt(0, 1, Piece.BLACK);
        board.putAt(0, 0, Piece.WHITE);
        board.putAt(6, 6, Piece.BLACK);
        board.putAt(7, 7, Piece.WHITE);

        for ( String line : board.getStringForPrint() ) System.out.println(line);

        int[] next = ai.getNext(board, Piece.BLACK);
        System.out.println(next[0] + " - " + next[1]);

        board.putAt(next[0], next[1], Piece.BLACK);
        for ( String line : board.getStringForPrint() ) System.out.println(line);

    }
}
