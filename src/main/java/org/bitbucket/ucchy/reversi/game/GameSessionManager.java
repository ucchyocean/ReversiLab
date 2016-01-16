/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.reversi.game;

import java.util.ArrayList;

import org.bitbucket.ucchy.reversi.ReversiLab;
import org.bukkit.entity.Player;

/**
 * ゲームセッションマネージャ
 * @author ucchy
 */
public class GameSessionManager {

    private ReversiLab parent;
    private ArrayList<GameSession> sessions;

    /**
     * コンストラクタ
     * @param parent プラグインのインスタンス
     */
    public GameSessionManager(ReversiLab parent) {
        this.parent = parent;
        sessions = new ArrayList<GameSession>();
    }

    /**
     * 指定したプレイヤーのゲームセッションを取得する
     * @param player プレイヤー
     * @return ゲームセッション
     */
    public GameSession getSession(Player player) {
        for ( GameSession session : sessions ) {
            if ( !session.isEnd() &&
                    ( session.isOwner(player.getName()) || session.isOpponent(player.getName()) ) ) {
                return session;
            }
        }
        return null;
    }

    /**
     * 指定したプレイヤーのゲームセッションを削除する
     * @param player プレイヤー
     */
    public void removeSession(Player player) {
        GameSession session = getSession(player);
        if ( session != null ) {
            sessions.remove(session);
        }
    }

    /**
     * 指定されたゲームセッションを登録削除する
     * @param session ゲームセッション
     */
    public void removeSession(GameSession session) {
        if ( sessions.contains(session) ) {
            sessions.remove(session);
        }
    }

    /**
     * 新しいゲームセッションを作成する
     * @param owner オーナー
     * @param opponent 対戦者
     * @return 作成されたゲームセッション
     */
    public GameSession createNewSession(Player owner, Player opponent) {
        GameSession session = new GameSession(parent, owner.getName(), opponent.getName());
        sessions.add(session);
        return session;
    }

    /**
     * 空き状態のグリッドを取得する
     * @return グリッド
     */
    public int[] getOpenGrid() {

        int size = 1;
        int phase = 1;
        int x = 1, z = 0;

        while ( size <= 20 ) {

            boolean isUsed = false;
            for ( GameSession session : sessions ) {
                if ( session.getGrid_x() == x &&
                        session.getGrid_z() == z &&
                        !session.isEnd() ) {
                    isUsed = true;
                }
            }
            if ( !isUsed ) {
                return new int[]{x, z};
            }

            switch ( phase ) {
            case 1:
                x--;
                z++;
                if ( z == size ) {
                    phase = 2;
                }
                break;
            case 2:
                x--;
                z--;
                if ( x == -size ) {
                    phase = 3;
                }
                break;
            case 3:
                x++;
                z--;
                if ( z == -size ) {
                    phase = 4;
                }
                break;
            case 4:
                x++;
                z++;
                if ( x == size ) {
                    phase = 1;
                    size++;
                    x++;
                }
                break;
            }
        }

        return new int[]{20, 0};
        // 同時に840人が遊んでいるなら、この値が返されるが、まずそんなことはない。
    }

    /**
     * 現在のセッションを全て返す
     * @return 全てのセッション
     */
    public ArrayList<GameSession> getAllSessions() {
        return sessions;
    }
}
