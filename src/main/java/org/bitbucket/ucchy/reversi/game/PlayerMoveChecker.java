/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi.game;

import org.bitbucket.ucchy.reversi.ReversiLab;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 1秒ごとに、リバーシ対戦中のプレイヤーが範囲外に出てしまっていないか確認するためのタスククラス
 * @author ucchy
 */
public class PlayerMoveChecker extends BukkitRunnable {

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        GameSessionManager manager = ReversiLab.getInstance().getGameSessionManager();
        for ( VersusGameSession session : manager.getAllSessions() ) {
            if ( session.getPhase() != GameSessionPhase.IN_GAME ) {
                continue;
            }

            GameField field = session.getField();
            for ( Player player : session.getRelatedPlayers() ) {
                if ( field.isOutOfSpectateField(player.getLocation()) ) {
                    player.teleport(field.getCenterRespawnPoint(), TeleportCause.PLUGIN);
                }
            }
        }
    }

    /**
     * タスクを開始する
     */
    public void start(Plugin plugin) {
        this.runTaskTimer(plugin, 20, 20);
    }
}
