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

        // 現在おける場所で、PRIORITYが最大、かつ、裏返せる個数がなるべく少なくなる場所を探す。
        int[] coordinates = new int[2];
        int priority = Integer.MIN_VALUE;
        int value = Integer.MAX_VALUE;

        for ( int x=0; x<8; x++ ) {
            for ( int y=0; y<8; y++ ) {
                if ( !board.canPutAt(x, y, piece) ) continue;
                int p = PRIORITY[x][y] + (int)(Math.random() * 10) - 5;
                int v = board.findPath(x, y, piece).size() + (int)(Math.random() * 4) - 2;
                if ( v == 0 ) continue;
                if ( priority < p || (priority == p && value > v) ) {
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
