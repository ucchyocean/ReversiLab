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
 * リバーシのAI
 * @author ucchy
 */
public interface ReversiAI {

    /** このAIの難易度を返す */
    public SingleGameDifficulty getDifficulty();

    /** 次に置く場所を返す */
    public int[] getNext(GameBoard board, CellState state);
}
