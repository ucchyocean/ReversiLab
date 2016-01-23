package org.bitbucket.ucchy.reversi;

import org.bitbucket.ucchy.reversi.game.CellState;
import org.bitbucket.ucchy.reversi.game.VersusGameSession;
import org.bitbucket.ucchy.reversi.game.GameSessionManager;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

/**
 * ReversiLabのリスナークラス
 * @author ucchy
 */
public class ReversiLabListener implements Listener {

    private GameSessionManager manager;

    /**
     * コンストラクタ
     * @param parent
     */
    public ReversiLabListener(ReversiLab parent) {
        manager = parent.getGameSessionManager();
    }

    /**
     * ブロックが置かれたときに呼び出されるメソッド
     * @param event
     */
    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {

        // プレイヤーによるブロック設置でなければ、イベントを無視する。
        if ( event.getPlayer() == null ) return;

        Player player = event.getPlayer();

        // リバーシの対局参加者でなければ、イベントを無視する。
        VersusGameSession session = manager.getSession(player);
        if ( session == null ) return;

        // この時点で、イベントはキャンセルしておく。
        event.setCancelled(true);

        CellState state;
        if ( event.getBlock().getType() == Material.NETHER_BRICK ) {
            state = CellState.BLACK;
        } else if ( event.getBlock().getType() == Material.QUARTZ_BLOCK ) {
            state = CellState.WHITE;
        } else {
            return;
        }

        // 石を置いてみる
        session.tryPut(event.getBlock().getLocation(), state);
    }

    /**
     * ブロックを破壊したときに呼び出されるメソッド
     * @param event
     */
    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent event) {

        // プレイヤーによるブロック設置でなければ、イベントを無視する。
        if ( event.getPlayer() == null ) return;

        Player player = event.getPlayer();

        // リバーシの対局参加者でなければ、イベントを無視する。
        VersusGameSession session = manager.getSession(player);
        if ( session == null ) return;

        // イベントをキャンセルする。
        event.setCancelled(true);
    }

    /**
     * エンティティが何らかのダメージを受けたときに呼び出されるメソッド
     * @param event
     */
    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onEntityDamage(EntityDamageEvent event) {

        // プレイヤーへのダメージでなければ、イベントを無視する。
        if ( !(event.getEntity() instanceof Player) ) return;

        Player player = (Player)event.getEntity();

        // リバーシの対局参加者でなければ、イベントを無視する。
        VersusGameSession session = manager.getSession(player);
        if ( session == null ) return;

        // イベントをキャンセルして、すべてのダメージを無効化する。
        event.setCancelled(true);
    }

    /**
     * プレイヤーがアイテムを捨てたときに呼び出されるメソッド
     * @param event
     */
    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        // リバーシの対局参加者でなければ、イベントを無視する。
        VersusGameSession session = manager.getSession(event.getPlayer());
        if ( session == null ) return;

        // イベントをキャンセルして、すべてのアイテムドロップをキャンセルし、
        // アイテムを捨てることができないようにする。
        event.setCancelled(true);
    }

    /**
     * プレイヤーがサーバーに参加したときに呼び出されるメソッド
     * @param event
     */
    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerJoin(PlayerJoinEvent event) {

        VersusGameSession session = manager.getSession(event.getPlayer());
        if ( event.getPlayer().getWorld().getName().equals(ReversiLab.WORLD_NAME) ) {

            if ( session == null || session.isEnd() ) {
                // セッションが無いのに専用ワールドに来た場合は、
                // 強制リスポーンさせる。
                event.getPlayer().setGameMode(GameMode.SURVIVAL);
                event.getPlayer().setHealth(0);
            } else {
                // セッションがあってサーバーに再参加した場合は、
                // 飛行状態に再設定する。
                event.getPlayer().setAllowFlight(true);
                event.getPlayer().setFlying(true);
            }
        }
    }

    /**
     * プレイヤーがチャット発言をしたときに呼び出されるメソッド
     * @param event
     */
    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {

        // リバーシの対局参加者でなければ、イベントを無視する。
        VersusGameSession session = manager.getSession(event.getPlayer());
        if ( session == null ) return;

        // チャット発言をログに記録する
        String message = String.format(event.getFormat(), event.getPlayer().getDisplayName(), event.getMessage());
        session.log(message);
    }


    /**
     * 天候（降雨）が変化したときに呼び出されるメソッド
     * @param event
     */
    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onWeatherChange(WeatherChangeEvent event) {

        // プラグインのワールドで天候が変更した場合に阻止する。
        if ( event.getWorld().getName().equals(ReversiLab.WORLD_NAME) ) {
            event.setCancelled(true);
        }
    }

    /**
     * 天候（雷）が変化したときに呼び出されるメソッド
     * @param event
     */
    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onThunderChange(ThunderChangeEvent event) {

        // プラグインのワールドで天候が変更した場合に阻止する。
        if ( event.getWorld().getName().equals(ReversiLab.WORLD_NAME) ) {
            event.setCancelled(true);
        }
    }
}
