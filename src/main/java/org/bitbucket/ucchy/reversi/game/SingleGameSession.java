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
import org.bitbucket.ucchy.reversi.TitleDisplayComponent;
import org.bitbucket.ucchy.reversi.Utility;
import org.bitbucket.ucchy.reversi.ai.ReversiAI;
import org.bitbucket.ucchy.reversi.tellraw.MessageComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * CPU戦のゲームセッション
 * @author ucchy
 */
public class SingleGameSession {

    private static final boolean DEBUG = true;
    private static final int EFFECT_SPEED = 3;

    private int grid_x;
    private int grid_z;

    private ReversiLab parent;

    private GameSessionPhase phase;
    private GameSessionTurn turn;
    private GameBoard board;
    private GameField field;
    private GameSessionLogger logger;

    private SingleGameDifficulty difficulty;
    private ReversiAI ai;

    private String ownerName;
    private ArrayList<String> spectators;
    private boolean isOwnerBlack;

    private Location ownerReturnPoint;
    private HashMap<String, Location> spectatorReturnPoints;

    private TemporaryStorage tempStorage;

    /**
     * コンストラクタ
     * @param parent
     * @param ownerName
     * @param difficulty
     */
    protected SingleGameSession(ReversiLab parent, String ownerName, SingleGameDifficulty difficulty) {

        this.parent = parent;
        this.ownerName = ownerName;
        this.difficulty = difficulty;

        this.spectators = new ArrayList<String>();
        this.spectatorReturnPoints = new HashMap<String, Location>();
        this.logger = new GameSessionLogger(new File(ReversiLab.getInstance().getDataFolder(), "logs"));

        // そのままPREPAREフェーズを実行する
        runPrepare();
    }

    /**
     * ゲームのPREPAREフェーズを実行する
     */
    public void runPrepare() {

        this.phase = GameSessionPhase.PREPARE;

        // メッセージ表示
        sendInfoMessageAll(Messages.get("InformationPreparing"));

        // ゲームボードを生成
        board = new GameBoard();

        // グリッドをマネージャから取得する
        int[] grid = parent.getGameSessionManager().getOpenGrid();
        this.grid_x = grid[0];
        this.grid_z = grid[1];

        // グリッドにゲーム用フィールドを生成する
        Location origin = new Location(
                parent.getWorld(), grid_x * 640, 75, grid_z * 640);
        this.field = new GameField(origin);
        Location ownerStartLoc = origin.clone().add(4, 5, -1)
                .setDirection(new Vector(0, -5, 5).normalize());

        // オーナープレイヤーを取得する。ログイン状態でなければ、ゲームはキャンセルする。
        Player owner = Utility.getPlayerExact(ownerName);
        if ( owner == null || !owner.isOnline() ) {
            runCancel();
            return;
        }

        // 何かに乗っている、何かを乗せているなら強制パージする
        owner.leaveVehicle();
        owner.eject();

        // 元いた場所を記憶する
        ownerReturnPoint = owner.getLocation();

        // スタート地点に送る
        owner.teleport(ownerStartLoc, TeleportCause.PLUGIN);

        // 飛行状態に変更する、ゲームモードはSURVIVALにする
        owner.setGameMode(GameMode.SURVIVAL);
        owner.setAllowFlight(true);
        owner.setFlying(true);

        // プレイヤーの身ぐるみを剥がす
        tempStorage = new TemporaryStorage();
        tempStorage.sendToTemp(owner);

        // 先攻後攻を決める
        int value = (int)(Math.random() * 2);
        isOwnerBlack = ( value == 0 );

        // アイテムを持たせる
        if ( isOwnerBlack ) {
            ItemStack netherBrick = new ItemStack(Material.NETHER_BRICK, 64);
            owner.getInventory().addItem(netherBrick);
        } else {
            ItemStack quartzBlock = new ItemStack(Material.QUARTZ_BLOCK, 64);
            owner.getInventory().addItem(quartzBlock);
        }

        // AIを生成する
        // TODO

        // メッセージを流す
        sendInfoMessageAll(Messages.get("InformationStarting"));
        if ( parent.getReversiLabConfig().isBroadcastSessionStartEnd() ) {
            // TODO
            sendBroadcastInfoMessage(Messages.get("BroadcastSessionStart",
                    new String[]{"%owner", "%difficulty"}, new String[]{ownerName, difficulty.name()}));
        }

        // そのまま、IN_GAMEフェーズに進む
        runInGame();
    }

    /**
     * ゲームのIN_GAMEフェーズを実行する
     */
    public void runInGame() {

        this.phase = GameSessionPhase.IN_GAME;

        // 先攻のターン
        runPreTurn(CellState.BLACK);
    }

    /**
     * 手番を開始する。
     * @param state 黒または白
     */
    private void runPreTurn(final CellState state) {

        this.turn = (state == CellState.BLACK) ? GameSessionTurn.BLACK_PRE : GameSessionTurn.WHITE_PRE;

        boolean isPlayerTurn =
                (state == CellState.BLACK && isOwnerBlack)  || (state == CellState.WHITE && !isOwnerBlack);

        Player owner = Utility.getPlayerExact(ownerName);

        // メッセージを表示
        if ( isPlayerTurn ) {
            TitleDisplayComponent.display(owner, Messages.get("InformationYourTurn"), 10, 50, 20);
        }

        // 石を置けることを確認する。置けないならパスを行う。
        if ( !board.canPut(state) ) {
            String name = isPlayerTurn ? ownerName : Messages.get("NameOfCPU");
            sendInfoMessageAll(Messages.get("InformationAutoPass", "%player", name));
            runPreTurn(state.getReverse());
            return;
        }

        this.turn = (state == CellState.BLACK) ? GameSessionTurn.BLACK : GameSessionTurn.WHITE;

        // CPUが石を置く
        if ( !isPlayerTurn ) {

            final long startTime = System.currentTimeMillis();

            // CPUが長考する可能性があるので、非同期処理スレッドで実行する
            new BukkitRunnable() {
                public void run() {

                    // 次に置く座標を取得
                    final int[] next = ai.getNext(board, state);

                    long cpuTimeMillis = System.currentTimeMillis() - startTime;

                    // 同期処理に戻す。CPUに1秒かかっていない場合は、演出のために1秒待たせる。
                    int ticks = 20 - (int)(cpuTimeMillis / 50);
                    if ( ticks <= 0 ) ticks = 1;

                    if ( DEBUG ) {
                        System.out.println("DEBUG : cputime = " + cpuTimeMillis + ", ticks = " + ticks);
                    }

                    new BukkitRunnable() {
                        public void run() {
                            tryPut(next[0], next[1], state);
                        }
                    }.runTaskLater(parent, ticks);
                }
            }.runTaskAsynchronously(parent);
        }
    }

    /**
     * 指定した座標に石を置いてみる。
     * @param location 座標
     * @param state 置く石の種類
     */
    public void tryPut(Location location, CellState state) {

        int xOffset = location.getBlockX() - field.getOrigin().getBlockX();
        int zOffset = location.getBlockZ() - field.getOrigin().getBlockZ();

        // 置く場所おかしい場合は、エラーメッセージを表示する。
        if ( location.getBlockY() != field.getOrigin().getBlockY() + 1
                || xOffset < 0 || 8 <= xOffset || zOffset < 0 || 8 <= zOffset ) {
            sendErrorMessage(ownerName, Messages.get("ErrorCannotPut"));
            return;
        }

        tryPut(xOffset, zOffset, state);
    }

    /**
     * 指定した座標に石を置いてみる。
     * @param x マス目のx座標
     * @param y マス目のy座標
     * @param state 置く石の種類
     */
    private void tryPut(int x, int y, final CellState state) {

        // 手番でなければ、エラーメッセージを表示する。
        if ( state == CellState.BLACK && turn != GameSessionTurn.BLACK ) {
            sendErrorMessage(ownerName, Messages.get("ErrorNotYourTurn"));
            return;
        }
        if ( state == CellState.WHITE && turn != GameSessionTurn.WHITE ) {
            sendErrorMessage(ownerName, Messages.get("ErrorNotYourTurn"));
            return;
        }

        // 置ける場所なのかどうかを確認する。置けないならエラーメッセージを表示する。
        if ( !board.canPutAt(x, y, state) ) {
            sendErrorMessage(ownerName, Messages.get("ErrorCannotPut"));
            return;
        }

        // 実際に置く。
        this.turn = (state == CellState.BLACK) ? GameSessionTurn.BLACK_POST : GameSessionTurn.WHITE_POST;
        final ArrayList<int[]> reverses = board.putAt(x, y, state);
        field.putStone(x, y, state);

        // 演出のために、1つ1つ遅延をかけてひっくり返す
        new BukkitRunnable() {
            int index = 0;
            public void run() {
                if ( index < reverses.size() ) {
                    int[] coodinate = reverses.get(index);
                    field.putStone(coodinate[0], coodinate[1], state);
                } else {
                    cancel();

                    // 勝負が決着したかどうかを確認する。
                    // 決着したならENDへ、していないなら相手の番へ。
                    if ( !board.canPutAll() ) {
                        runEnd();
                    } else {
                        runPreTurn(state.getReverse());
                    }
                }
                index++;
            }
        }.runTaskTimer(ReversiLab.getInstance(), EFFECT_SPEED, EFFECT_SPEED);
    }

    /**
     * ゲームのENDフェーズを実行する
     */
    public void runEnd() {

        // この時点では、まだフェーズを変更しない。IN_GAMEのままにしておく。
        // this.phase = GameSessionPhase.END;

        // どちらが勝ちか確認する。
        int black = board.getBlackCount();
        int white = board.getWhiteCount();
        int sessionEndWaitSeconds = parent.getReversiLabConfig().getSessionEndWaitSeconds();
        boolean isPlayerWin = false;
        boolean isDraw = true;
        if ( black > white ) {
            isPlayerWin = isOwnerBlack;
            isDraw = false;
        } else if ( black < white ) {
            isPlayerWin = !isOwnerBlack;
            isDraw = false;
        }

        // メッセージを表示する
        String msg;
        // TODO

        //sendInfoMessageAll(msg);
        //sendInfoMessageAll(Messages.get("InformationEndWait", "%seconds", sessionEndWaitSeconds));

        if ( parent.getReversiLabConfig().isBroadcastSessionStartEnd() ) {
            // TODO
            //sendBroadcastInfoMessage(msg);
        }

        // 盤面をログに記録する
        for ( String line : board.getStringForPrint() ) {
            logger.log(line);
        }

        // プレイヤーが勝利したなら、花火を発生させる。
        if ( isPlayerWin ) {
            field.spawnFireworks();
        }

        // ランキングデータに勝敗を加算する
        // TODO

        // 15秒後に帰還する
        new BukkitRunnable() {
            public void run() {
                phase = GameSessionPhase.END;
                runFinalize();
            }
        }.runTaskLater(ReversiLab.getInstance(), sessionEndWaitSeconds * 20);
    }

    /**
     * ゲームのCANCELフェーズを実行する
     */
    public void runCancel() {

        phase = GameSessionPhase.CANCEL;

        sendInfoMessageAll(Messages.get("InformationCancel"));

        runFinalize();
    }

    /**
     * ゲームのCANCELフェーズを実行する
     */
    public void runInvitationDenyed() {

        phase = GameSessionPhase.INVITATION_DENYED;

        sendInfoMessageAll(Messages.get("InformationInvitationDeny"));

        runFinalize();
    }

    /**
     * セッションの最終処理を行う
     */
    public void runFinalize() {

        // このゲームセッションを登録から削除する
        //parent.getGameSessionManager().removeSession(this);
        // TODO

        Player owner = getOwnerPlayer();
        if ( owner != null ) {

            // 元いた場所に戻す
            if ( ownerReturnPoint != null ) {
                owner.teleport(ownerReturnPoint, TeleportCause.PLUGIN);
            }

            // 飛行状態を解除する
            owner.setAllowFlight(false);
            owner.setFlying(false);

            // 持ち物を預かっているなら返す
            if ( tempStorage != null ) {
                tempStorage.restoreFromTemp(owner);
            }
        }

        for ( String name : spectators ) {

            Player spectator = Utility.getPlayerExact(name);
            if ( spectator != null ) {

                // 参加前に居た場所に戻す
                Location loc = spectatorReturnPoints.get(name);
                spectator.teleport(loc, TeleportCause.PLUGIN);

                // ゲームモードを変更
                spectator.setGameMode(GameMode.SURVIVAL);
            }
        }

        // 10秒後に、フィールドをクリーンアップする。だたし、プラグインがdisabledなら実施しない。
        if ( ReversiLab.getInstance().isEnabled() && field != null ) {
            new BukkitRunnable() {
                public void run() {
                    field.cleanup();
                }
            }.runTaskLater(ReversiLab.getInstance(), 10 * 20);
        }
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
     * 指定されたプレイヤー名は、セッションオーナーかどうかを確認する。
     * @param playerName プレイヤー名
     * @return セッションオーナーかどうか
     */
    public boolean isOwner(String playerName) {
        return ownerName.equals(playerName);
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
     * 指定されたプレイヤー名は、このセッションの関係者(対局者、または、観客)かどうかを確認する。
     * @param playerName プレイヤー名
     * @return 関係者かどうか
     */
    public boolean isRelatedPlayer(String playerName) {
        return isOwner(playerName) || isSpectator(playerName);
    }

    /**
     * セッションオーナーを取得する
     * @return セッションオーナー
     */
    public Player getOwnerPlayer() {
        return Utility.getPlayerExact(ownerName);
    }

    /**
     * 関連プレイヤーをすべて取得する
     * @return 全ての関連プレイヤー
     */
    public ArrayList<Player> getRelatedPlayers() {
        ArrayList<Player> players = new ArrayList<Player>();
        Player owner = getOwnerPlayer();
        if ( owner != null ) players.add(owner);
        for ( String name : spectators ) {
            Player spectator = Utility.getPlayerExact(name);
            if ( spectator != null ) players.add(spectator);
        }
        return players;
    }

    /**
     * 指定したプレイヤーが、このセッションを中断することができるかどうかを返す
     * @param player プレイヤー
     * @return 中断することができるかどうか
     */
    public boolean isOKtoCancel(Player player) {

        // IN_GAME中は、キャンセルOK
        return (phase == GameSessionPhase.IN_GAME);
    }

    /**
     * 観客としてゲーム参加する
     * @param player
     */
    public void joinSpectator(Player player) {

        if ( spectators.contains(player.getName()) ) {
            return;
        }

        // 観客に追加
        spectators.add(player.getName());
        spectatorReturnPoints.put(player.getName(), player.getLocation());

        // ゲームフィールドへテレポート
        player.teleport(field.getCenterRespawnPoint(), TeleportCause.PLUGIN);

        // ゲームモードを変更
        player.setGameMode(GameMode.SPECTATOR);
    }

    /**
     * ゲームの観客から退出する
     * @param player
     */
    public void leaveSpectator(Player player) {

        if ( !spectators.contains(player.getName()) ) {
            return;
        }

        // 参加前に居た場所に戻す
        Location loc = spectatorReturnPoints.get(player.getName());
        player.teleport(loc, TeleportCause.PLUGIN);

        // 観客から削除する
        spectators.remove(player.getName());
        spectatorReturnPoints.remove(player.getName());

        // ゲームモードを変更
        player.setGameMode(GameMode.SURVIVAL);
    }

    public int getGrid_x() {
        return grid_x;
    }

    public int getGrid_z() {
        return grid_z;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public GameSessionPhase getPhase() {
        return phase;
    }

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
    private void sendInfoMessageAll(String message) {
        if ( message == null || message.equals("") ) return;
        ArrayList<String> players = new ArrayList<String>();
        players.add(ownerName);
        players.addAll(spectators);
        String prefix = Messages.get("PrefixInformation");
        for ( String name : players ) {
            if ( name == null ) continue;
            Player player = Utility.getPlayerExact(name);
            if ( player == null ) continue;
            player.sendMessage(prefix + message);
        }

        logger.log(message);
    }

    /**
     * 指定された名前のプレイヤーに情報メッセージを送る。
     * @param playerName プレイヤー名
     * @param message メッセージ
     */
    private void sendInfoMessage(String playerName, String message) {
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
    private void sendErrorMessage(String playerName, String message) {
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
    private void sendMessageComponent(String playerName, MessageComponent component) {
        Player player = Utility.getPlayerExact(playerName);
        if ( player == null ) return;
        component.send(player);
    }

    /**
     * ブロードキャストで、サーバー全体に情報メッセージを送る。
     * @param message メッセージ
     */
    private void sendBroadcastInfoMessage(String message) {
        if ( message == null || message.equals("") ) return;
        String prefix = Messages.get("PrefixInformation");
        Bukkit.broadcastMessage(prefix + message);
    }

    /**
     * このセッションの文字列表現
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return String.format("%s{owner=%s}",
                this.getClass().getSimpleName(), ownerName);
    }
}
