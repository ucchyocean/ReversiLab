/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.reversi.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

/**
 * サイドバー表示クラス
 * @author ucchy
 */
public class SidebarDisplay {

    private static final String OBJECTIVE_NAME = "reversilab";

    private Scoreboard scoreboard;

    /**
     * コンストラクタ
     */
    public SidebarDisplay() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective sidebar = scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy");
        sidebar.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    /**
     * 指定されたプレイヤーを、このスコアボードの表示対象にする
     * @param player プレイヤー
     */
    public void setShowPlayer(Player player) {
        player.setScoreboard(scoreboard);
    }

    /**
     * 指定されたプレイヤーを、メインスコアボード表示に戻す
     * @param player プレイヤー
     */
    public void setMainScoreboard(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /**
     * サイドバーのタイトルを設定する。
     * @param title タイトル
     */
    public void setTitle(String title) {
        Objective obj = scoreboard.getObjective(OBJECTIVE_NAME);
        if ( obj != null ) {
            obj.setDisplayName(title);
        }
    }

    /**
     * スコア項目を設定する。項目名は16文字以下にすること。
     * @param name 項目名
     * @param point 項目のスコア
     */
    public void setScore(String name, int point) {
        Objective obj = scoreboard.getObjective(OBJECTIVE_NAME);
        Score score = obj.getScore(name);
        if ( point == 0 ) {
            score.setScore(1); // NOTE: set temporary.
        }
        score.setScore(point);
    }

    /**
     * 項目にスコアを加算する。マイナスを指定すれば減算も可能。
     * @param name 項目名
     * @param amount 加算する値
     */
    public void addScore(String name, int amount) {
        Objective obj = scoreboard.getObjective(OBJECTIVE_NAME);
        Score score = obj.getScore(name);
        int point = score.getScore();
        score.setScore(point + amount);
    }

    /**
     * スコアボードを削除する。
     */
    public void remove() {
        if ( scoreboard.getObjective(DisplaySlot.SIDEBAR) != null ) {
            scoreboard.getObjective(DisplaySlot.SIDEBAR).unregister();
        }
        scoreboard.clearSlot(DisplaySlot.SIDEBAR);
    }

    /**
     * スコア項目を削除する
     * @param name
     */
    public void removeScores(Scoreboard scoreboard, String name) {
        scoreboard.resetScores(name);
    }
}
