package org.bitbucket.ucchy.reversi.ai;

import org.bitbucket.ucchy.reversi.game.CellState;
import org.bitbucket.ucchy.reversi.game.GameBoard;
import org.bitbucket.ucchy.reversi.game.SingleGameDifficulty;

/**
 * NormalのAI
 * @author ucchy
 */
public class ReversiAINormal implements ReversiAI {

    private static final int[][] PRIORITY = {
        {5, 0, 3, 4, 4, 3, 0, 5},
        {0, 0, 1, 1, 1, 1, 0, 0},
        {3, 1, 2, 2, 2, 2, 1, 3},
        {4, 1, 2, 2, 2, 2, 1, 4},
        {4, 1, 2, 2, 2, 2, 1, 4},
        {3, 1, 2, 2, 2, 2, 1, 3},
        {0, 0, 1, 1, 1, 1, 0, 0},
        {5, 0, 3, 4, 4, 3, 0, 5},
    };

    /**
     * @see org.bitbucket.ucchy.reversi.ai.ReversiAI#getDifficulty()
     */
    @Override
    public SingleGameDifficulty getDifficulty() {
        return SingleGameDifficulty.NORMAL;
    }

    /**
     * @see org.bitbucket.ucchy.reversi.ai.ReversiAI#getNext(org.bitbucket.ucchy.reversi.game.GameBoard, org.bitbucket.ucchy.reversi.game.CellState)
     */
    @Override
    public int[] getNext(GameBoard board, CellState state) {

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
}
