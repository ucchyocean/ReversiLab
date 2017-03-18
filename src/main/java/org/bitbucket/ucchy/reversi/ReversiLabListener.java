package org.bitbucket.ucchy.reversi;

import org.bitbucket.ucchy.reversi.game.GameSession;
import org.bitbucket.ucchy.reversi.game.GameSessionManager;
import org.bitbucket.ucchy.reversi.game.GameSessionPhase;
import org.bitbucket.ucchy.reversi.game.Piece;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
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
        GameSession session = manager.getSession(player);
        if ( session == null ) return;

        // ゲームセッションがIN_GAMEでなければ、イベントを無視する。
        if ( session.getPhase() != GameSessionPhase.IN_GAME ) return;

        // この時点で、イベントはキャンセルしておく。
        event.setCancelled(true);

        Piece piece;
        if ( event.getBlock().getType() == Material.NETHER_BRICK ) {
            piece = Piece.BLACK;
        } else if ( event.getBlock().getType() == Material.QUARTZ_BLOCK ) {
            piece = Piece.WHITE;
        } else {
            return;
        }

        // 石を置いてみる
        session.tryPut(event.getBlock().getLocation(), piece);
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
        GameSession session = manager.getSession(player);
        if ( session == null ) return;

        // ゲームセッションがIN_GAMEでなければ、イベントを無視する。
        if ( session.getPhase() != GameSessionPhase.IN_GAME ) return;

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
        GameSession session = manager.getSession(player);
        if ( session == null ) return;

        // ゲームセッションがIN_GAMEでなければ、イベントを無視する。
        if ( session.getPhase() != GameSessionPhase.IN_GAME ) return;

        // それ以外の場合は、イベントをキャンセルして、すべてのダメージを無効化する。
        event.setCancelled(true);
    }

    /**
     * プレイヤーがアイテムを捨てたときに呼び出されるメソッド
     * @param event
     */
    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {

        // リバーシの対局参加者でなければ、イベントを無視する。
        GameSession session = manager.getSession(event.getPlayer());
        if ( session == null ) return;

        // ゲームセッションがIN_GAMEでなければ、イベントを無視する。
        if ( session.getPhase() != GameSessionPhase.IN_GAME ) return;

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

        Player player = event.getPlayer();
        GameSession session = manager.getSession(player);

        if ( player.getWorld().getName().equals(ReversiLab.WORLD_NAME) ) {

            if ( session == null || session.isEnd() ) {
                // セッションが無いのに専用ワールドに来た場合は、
                // リスポーン地点へ強制送還させる。
                player.setGameMode(GameMode.SURVIVAL);
                Location respawn = player.getBedSpawnLocation();
                if ( respawn == null ) {
                    respawn = Bukkit.getWorld("world").getSpawnLocation();
                }
                player.teleport(respawn, TeleportCause.PLUGIN);
                return;

            } else {
                // セッションがあってサーバーに再参加した場合は、
                // 飛行状態に再設定する。サイドバーを表示する。
                player.setAllowFlight(true);
                player.setFlying(true);
                session.setSidebar(player);

                // インベントリを再度預かる。
                if ( session.isPlayer(player.getName()) ) {
                    session.switchInventory(player);
                }

                return;
            }
        }
    }

    /**
     * プレイヤーがサーバーから退出したときに呼び出されるメソッド
     * @param event
     */
    @EventHandler(priority=EventPriority.NORMAL, ignoreCancelled=true)
    public void onPlayerQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();
        GameSession session = manager.getSession(player);
        if ( session == null || session.isEnd() ) return;

        if ( player.getWorld().getName().equals(ReversiLab.WORLD_NAME) ) {

            if ( session.isPlayer(player.getName()) ) {
                // 途中で切断してしまった場合は、一時的にインベントリの内容を返してあげる
                session.switchInventory(player);
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
        GameSession session = manager.getSession(event.getPlayer());
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
