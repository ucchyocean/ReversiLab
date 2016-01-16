/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi.game;

/**
 * ゲームのターン
 * @author ucchy
 */
public enum GameSessionTurn {

    /** 黒の手番の準備中 */
    BLACK_PRE,

    /** 黒の手番 */
    BLACK,

    /** 黒の手番の後処理中 */
    BLACK_POST,

    /** 白の手番の準備中 */
    WHITE_PRE,

    /** 白の手番 */
    WHITE,

    /** 白の手番の後処理中 */
    WHITE_POST,
}
