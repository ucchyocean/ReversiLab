/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.reversi.game;

import java.util.ArrayList;
import java.util.HashMap;

import org.bitbucket.ucchy.reversi.ReversiLab;
import org.bitbucket.ucchy.reversi.Utility;
import org.bukkit.entity.Player;

/**
 * ゲームセッションマネージャ
 * @author ucchy
 */
public class GameSessionManager {

    private ReversiLab parent;
    private HashMap<String, GameSession> sessions;

    /**
     * コンストラクタ
     * @param parent プラグインのインスタンス
     */
    public GameSessionManager(ReversiLab parent) {
        this.parent = parent;
        sessions = new HashMap<String, GameSession>();
    }

    /**
     * 指定したプレイヤーに関連するゲームセッションを取得する
     * @param player プレイヤー
     * @return ゲームセッション
     */
    public GameSession getSession(Player player) {
        for ( GameSession session : sessions.values() ) {
            if ( !session.isEnd() && session.isRelatedPlayer(player.getName()) ) {
                return session;
            }
        }
        return null;
    }

    /**
     * 指定したプレイヤーが招待されているゲームセッションを取得する
     * @param player プレイヤー
     * @return ゲームセッション
     */
    public VersusGameSession getInvitedSession(Player player) {
        for ( GameSession session : sessions.values() ) {
            if ( session.isEnd() ) continue;
            if ( session.getPhase() != GameSessionPhase.INVITATION ) continue;
            if ( !(session instanceof VersusGameSession) ) continue;
            VersusGameSession vsession = (VersusGameSession)session;
            if ( vsession.isOpponent(player.getName()) ) return vsession;
        }
        return null;
    }

    /**
     * 指定したプレイヤー名に関連するゲームセッションを取得する
     * @param playerName プレイヤー名
     * @return ゲームセッション
     */
    public GameSession getSession(String playerName) {
        Player player = Utility.getPlayerExact(playerName);
        if ( player == null ) return null;
        return getSession(player);
    }

    /**
     * 指定したプレイヤーのゲームセッションを削除する
     * @param player プレイヤー
     */
    public void removeSession(Player player) {
        GameSession session = getSession(player);
        if ( session != null ) {
            removeSession(session);
        }
    }

    /**
     * 指定されたゲームセッションを登録削除する
     * @param session ゲームセッション
     */
    public void removeSession(GameSession session) {
        sessions.remove(session.toString());
    }

    /**
     * 新しいゲームセッションを作成する
     * @param owner オーナー
     * @param difficulty 難易度
     * @return 作成されたゲームセッション
     */
    public SingleGameSession createNewSingleGameSession(Player owner, SingleGameDifficulty difficulty) {
        SingleGameSession session = new SingleGameSession(parent, owner.getName(), difficulty);
        sessions.put(session.toString(), session);
        return session;
    }

    /**
     * 新しいゲームセッションを作成する
     * @param owner オーナー
     * @param opponent 対戦者
     * @return 作成されたゲームセッション
     */
    public VersusGameSession createNewVersusGameSession(Player owner, Player opponent) {
        VersusGameSession session = new VersusGameSession(parent, owner.getName(), opponent.getName());
        sessions.put(session.toString(), session);
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
            for ( GameSession session : sessions.values() ) {
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
        // 同時に420人が遊んでいるなら、この値が返されるが、まずそんなことはない。
    }

    /**
     * 現在のセッションを全て返す
     * @return 全てのセッション
     */
    public ArrayList<GameSession> getAllSessions() {
        return new ArrayList<GameSession>(sessions.values());
    }
}
