/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi.ai;

import org.bitbucket.ucchy.reversi.game.Piece;
import org.bitbucket.ucchy.reversi.game.GameBoard;
import org.bitbucket.ucchy.reversi.game.SingleGameDifficulty;

/**
 * EasyのAI
 * @author ucchy
 */
public class ReversiAIEasy implements ReversiAI {

    /**
     * @see org.bitbucket.ucchy.reversi.ai.ReversiAI#getDifficulty()
     */
    @Override
    public SingleGameDifficulty getDifficulty() {
        return SingleGameDifficulty.EASY;
    }

    /**
     * @see org.bitbucket.ucchy.reversi.ai.ReversiAI#getNext(org.bitbucket.ucchy.reversi.game.GameBoard, org.bitbucket.ucchy.reversi.game.Piece)
     */
    @Override
    public int[] getNext(GameBoard board, Piece piece) {

        // 現在おける場所で、一番たくさん裏返すことができる場所を探す。
        int[] coordinates = new int[2];
        int value = 0;

        for ( int x=0; x<8; x++ ) {
            for ( int y=0; y<8; y++ ) {
                int v = board.findPath(x, y, piece).size();
                if ( value < v ) {
                    value = v;
                    coordinates[0] = x;
                    coordinates[1] = y;
                }
            }
        }

        return coordinates;
    }
}
