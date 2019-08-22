/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi.game;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.bitbucket.ucchy.reversi.Messages;
import org.bitbucket.ucchy.reversi.ReversiLab;
import org.bitbucket.ucchy.reversi.Utility;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.ucchyocean.messaging.tellraw.MessageComponent;

/**
 * ゲームセッションの抽象クラス
 * @author ucchy
 */
public abstract class GameSession {

    private int grid_x;
    private int grid_z;

    private GameSessionPhase phase;
    private GameBoard board;
    private GameField field;
    private GameSessionLogger logger;

    private ArrayList<String> spectators;
    private HashMap<String, Location> spectatorReturnPoints;
    private SidebarDisplay sidebar;

    /**
     * コンストラクタ
     */
    public GameSession() {

        this.spectators = new ArrayList<String>();
        this.spectatorReturnPoints = new HashMap<String, Location>();
        this.logger = new GameSessionLogger(new File(ReversiLab.getInstance().getDataFolder(), "logs"));

        // ゲームボードを生成
        board = new GameBoard();

        // グリッドをマネージャから取得する
        ReversiLab parent = ReversiLab.getInstance();
        int[] grid = parent.getGameSessionManager().getOpenGrid();
        this.grid_x = grid[0];
        this.grid_z = grid[1];

        // グリッドにゲーム用フィールドを生成する
        Location origin = new Location(
                parent.getWorld(), grid_x * 640, 75, grid_z * 640);
        this.field = new GameField(origin);

        // スコアボードを準備する
        sidebar = new SidebarDisplay();
    }

    /**
     * 観客としてゲーム参加する
     * @param player
     */
    public void joinSpectator(final Player player) {

        // IN_GAMEフェーズでない場合は、何もしない。
        if ( phase != GameSessionPhase.IN_GAME ) {
            return;
        }

        // 既に観客なら、何もしない。
        if ( spectators.contains(player.getName()) ) {
            return;
        }

        // 観客に追加
        spectators.add(player.getName());
        spectatorReturnPoints.put(player.getName(), player.getLocation());

        // ゲームフィールドへテレポート
        player.teleport(field.getCenterRespawnPoint(), TeleportCause.PLUGIN);

        // サイドバーを設定
        sidebar.setShowPlayer(player);

        // ゲームモードを変更
        player.setGameMode(GameMode.SPECTATOR);

        // MultiVerseが入っていると、ワールド間テレポートでSURVIVALに戻されてしまうので、
        // 2ticks後に再設定を試みる。
        new BukkitRunnable() {
            public void run() {
                if ( player.isOnline() && player.getGameMode() != GameMode.SPECTATOR ) {
                    player.setGameMode(GameMode.SPECTATOR);
                }
            }
        }.runTaskLater(ReversiLab.getInstance(), 2);
    }

    /**
     * ゲームの観客から退出する
     * @param player
     */
    public void leaveSpectator(final Player player) {

        // 既に観客でないなら、何もしない。
        if ( !spectators.contains(player.getName()) ) {
            return;
        }

        // 参加前に居た場所に戻す
        Location loc = spectatorReturnPoints.get(player.getName());
        player.teleport(loc, TeleportCause.PLUGIN);

        // 観客から削除する
        spectators.remove(player.getName());
        spectatorReturnPoints.remove(player.getName());

        // サイドバーを解除
        sidebar.setMainScoreboard(player);

        // ゲームモードを変更
        player.setGameMode(GameMode.SURVIVAL);
    }

    /**
     * 全ての観客を退出させる
     */
    protected void leaveAllSpectators() {

        for ( String name : spectators ) {

            final Player spectator = Utility.getPlayerExact(name);
            if ( spectator != null ) {

                // 参加前に居た場所に戻す
                Location loc = spectatorReturnPoints.get(name);
                spectator.teleport(loc, TeleportCause.PLUGIN);
                spectator.setFallDistance(0);

                // サイドバーを解除
                sidebar.setMainScoreboard(spectator);

                // ゲームモードを変更
                spectator.setGameMode(GameMode.SURVIVAL);
            }
        }
    }

    /**
     * 指定されたプレイヤーにサイドバーを表示する
     * @param player プレイヤー
     */
    public void setSidebar(Player player) {
        sidebar.setShowPlayer(player);
    }

    /**
     * このセッションが終了しているかどうかを返す
     * @return 終了しているかどうか
     */
    public boolean isEnd() {
        return (phase == GameSessionPhase.END ||
                phase == GameSessionPhase.CANCEL ||
                phase == GameSessionPhase.INVITATION_DENYED);
    }

    /**
     * 指定されたプレイヤー名は、観客かどうかを確認する。
     * @param playerName プレイヤー名
     * @return 観客かどうか
     */
    public boolean isSpectator(String playerName) {
        return spectators.contains(playerName);
    }

    /**
     * 観客名を返す。
     * @return 観客名
     */
    public ArrayList<String> getSpectators() {
        return spectators;
    }

    /**
     * グリッド座標のXを取得する。
     * @return
     */
    protected int getGrid_x() {
        return grid_x;
    }

    /**
     * グリッド座標のZを取得する。
     * @return
     */
    protected int getGrid_z() {
        return grid_z;
    }

    /**
     * 現在のゲームフェーズを取得する。
     * @return
     */
    public GameSessionPhase getPhase() {
        return phase;
    }

    /**
     * ゲームフェーズを設定する。
     * @param phase
     */
    protected void setPhase(GameSessionPhase phase) {
        this.phase = phase;
    }

    /**
     * ボードを取得する。
     * @return board
     */
    protected GameBoard getBoard() {
        return board;
    }

    /**
     * ゲームフィールドを取得する。
     * @return
     */
    protected GameField getField() {
        return field;
    }

    /**
     * ログにメッセージを1行追加する。主に、参加プレイヤーのチャットを記録することに使われる。
     * @param message メッセージ
     */
    public void log(String message) {
        logger.log(message);
    }

    /**
     * 全ての参加プレイヤーに情報メッセージを送る。
     * @param message メッセージ
     */
    protected void sendInfoMessageAll(String message) {
        if ( message == null || message.equals("") ) return;
        String prefix = Messages.get("PrefixInformation");
        for ( Player player : getRelatedPlayers() ) {
            player.sendMessage(prefix + message);
        }

        logger.log(message);
    }

    /**
     * 指定された名前のプレイヤーに情報メッセージを送る。
     * @param playerName プレイヤー名
     * @param message メッセージ
     */
    protected void sendInfoMessage(String playerName, String message) {
        if ( message == null || message.equals("") ) return;
        Player player = Utility.getPlayerExact(playerName);
        if ( player == null ) return;
        String prefix = Messages.get("PrefixInformation");
        player.sendMessage(prefix + message);
    }

    /**
     * 指定された名前のプレイヤーにエラーメッセージを送る。
     * @param playerName プレイヤー名
     * @param message メッセージ
     */
    protected void sendErrorMessage(String playerName, String message) {
        if ( message == null || message.equals("") ) return;
        Player player = Utility.getPlayerExact(playerName);
        if ( player == null ) return;
        String prefix = Messages.get("PrefixError");
        player.sendMessage(prefix + message);
    }

    /**
     * 指定された名前のプレイヤーにメッセージコンポーネントを送る。
     * @param playerName プレイヤー名
     * @param component メッセージコンポーネント
     */
    protected void sendMessageComponent(String playerName, MessageComponent component) {
        Player player = Utility.getPlayerExact(playerName);
        if ( player == null ) return;
        component.send(player);
    }

    /**
     * ブロードキャストで、サーバー全体に情報メッセージを送る。
     * @param message メッセージ
     */
    protected void sendBroadcastInfoMessage(String message) {
        if ( message == null || message.equals("") ) return;
        String prefix = Messages.get("PrefixInformation");
        Bukkit.broadcastMessage(prefix + message);
    }

    /**
     * サイドバーの残りマス数表示を設定する
     */
    protected void setSidebarLeast() {
        String msg = Messages.get("SidebarTitle", "%least", board.getEmptyCount());
        sidebar.setTitle(msg);
    }

    /**
     * サイドバーに黒のスコア項目を設定する
     * @param name 項目名
     */
    protected void setSidebarBlackScore(String name) {
        sidebar.setScore(ChatColor.BLACK + name, board.getBlackCount());
    }

    /**
     * サイドバーに白のスコア項目を設定する
     * @param name 項目名
     */
    protected void setSidebarWhiteScore(String name) {
        sidebar.setScore(ChatColor.WHITE + name, board.getWhiteCount());
    }

    /**
     * サイドバーの表示対象プレイヤーを設定する
     * @param player プレイヤー
     */
    protected void setSidebarShowPlayer(Player player) {
        sidebar.setShowPlayer(player);
    }

    /**
     * サイドバーの表示対象プレイヤーから外す
     * @param player プレイヤー
     */
    protected void removeSidebar(Player player) {
        sidebar.setMainScoreboard(player);
    }

    /**
     * 指定されたプレイヤーの手に、石を持たせる。
     * @param player プレイヤー
     * @param isBlack 黒かどうか
     */
    protected static void setDiscItemInHand(Player player, boolean isBlack) {
        ItemStack disc;
        if ( isBlack ) {
            disc = new ItemStack(Material.NETHER_BRICKS);
            ItemMeta meta = disc.getItemMeta();
            meta.setDisplayName("Black Stone");
            disc.setItemMeta(meta);
        } else {
            disc = new ItemStack(Material.QUARTZ_BLOCK);
            ItemMeta meta = disc.getItemMeta();
            meta.setDisplayName("White Stone");
            disc.setItemMeta(meta);
        }
        ItemStack temp = Utility.getItemInHand(player);
        Utility.setItemInHand(player, disc);
        if ( temp != null && temp.getType() != Material.AIR ) {
            player.getInventory().addItem(temp);
        }
    }

    /**
     * インベントリにある石を消去する。
     * @param player プレイヤー
     */
    protected static void clearDiscItemInInventory(Player player) {
        ArrayList<Integer> indexiesToRemove = new ArrayList<Integer>();
        for ( int index=0; index<player.getInventory().getSize(); index++ ) {
            ItemStack item = player.getInventory().getItem(index);
            if ( item == null || item.getType() == Material.AIR ) continue;
            if ( item.getType() == Material.NETHER_BRICKS && item.hasItemMeta() ) {
                ItemMeta meta = item.getItemMeta();
                if ( meta.hasDisplayName() && meta.getDisplayName().equals("Black Stone") ) {
                    indexiesToRemove.add(index);
                }
            } else if ( item.getType() == Material.QUARTZ_BLOCK && item.hasItemMeta() ) {
                ItemMeta meta = item.getItemMeta();
                if ( meta.hasDisplayName() && meta.getDisplayName().equals("White Stone") ) {
                    indexiesToRemove.add(index);
                }
            }
        }
        for ( int index : indexiesToRemove ) {
            player.getInventory().setItem(index, new ItemStack(Material.AIR));
        }
    }

    /**
     * 指定されたプレイヤー名は、このセッションの関係者(プレイヤーまたは観客)かどうかを確認する。
     * @param playerName プレイヤー名
     * @return 関係者かどうか
     */
    public abstract boolean isRelatedPlayer(String playerName);

    /**
     * 指定されたプレイヤー名は、このセッションのプレイヤーかどうかを確認する。
     * @param playerName プレイヤー名
     * @return プレイヤーかどうか
     */
    public abstract boolean isPlayer(String playerName);

    /**
     * このセッションに関連する全てのプレイヤーを取得する。
     * @return 関連プレイヤー
     */
    public abstract ArrayList<Player> getRelatedPlayers();

    /**
     * ゲームのCANCELフェーズを実行する
     */
    public abstract void runCancel();

    /**
     * ゲームを投了する
     * @param player 投了するプレイヤー
     */
    public abstract void resign(Player player);

    /**
     * 指定したプレイヤーが、セッションをキャンセルすることができるかどうかを判定する
     * @param player プレイヤー
     * @return キャンセル可能かどうか
     */
    public abstract boolean isOKtoCancel(Player player);

    /**
     * 指定したプレイヤーが、セッションを投了することができるかどうかを判定する
     * @param player プレイヤー
     * @return 投了かのうかどうか
     */
    public abstract boolean isOKtoResign(Player player);

    /**
     * 指定した座標に石を置いてみる。
     * @param location 座標
     * @param piece 置く石の種類
     */
    public abstract void tryPut(Location location, final Piece piece);

    /**
     * インベントリを預かっていたものと入れ替える。
     * @param player プレイヤー
     */
    public abstract void switchInventory(Player player);
}
