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
import org.bitbucket.ucchy.reversi.tellraw.ClickEventType;
import org.bitbucket.ucchy.reversi.tellraw.MessageComponent;
import org.bitbucket.ucchy.reversi.tellraw.MessageParts;
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
 * ゲームセッション
 * @author ucchy
 */
public class GameSession {

    private static final int EFFECT_SPEED = 3;

    private int grid_x;
    private int grid_z;

    private ReversiLab parent;

    private GameSessionPhase phase;
    private GameSessionTurn turn;
    private GameBoard board;
    private GameField field;
    private GameSessionLogger logger;

    private String ownerName;
    private String opponentName;
    private String blackPlayerName;
    private String whitePlayerName;
    private ArrayList<String> spectators;

    private Location ownerReturnPoint;
    private Location opponentReturnPoint;
    private HashMap<String, Location> spectatorReturnPoints;
    private TemporaryStorage tempStorage;

    /**
     * コンストラクタ
     * @param parent
     * @param ownerName
     * @param opponentName
     */
    protected GameSession(ReversiLab parent, String ownerName, String opponentName) {

        this.parent = parent;
        this.ownerName = ownerName;
        this.opponentName = opponentName;

        this.spectators = new ArrayList<String>();
        this.spectatorReturnPoints = new HashMap<String, Location>();
        this.logger = new GameSessionLogger(new File(ReversiLab.getInstance().getDataFolder(), "logs"));

        // そのままINVITATIONフェーズを実行する
        runInvitation();
    }

    /**
     * ゲームのINVITATIONフェーズを実行する
     */
    public void runInvitation() {

        this.phase = GameSessionPhase.INVITATION;

        // メッセージ表示
        sendInfoMessage(ownerName, Messages.get("InformationInvitationSent", "%opponent", opponentName));
        sendInfoMessage(opponentName, Messages.get("InformationInvitationGot", "%owner", ownerName));

        MessageComponent comp = new MessageComponent();
        MessageParts buttonAccept = new MessageParts(Messages.get("ButtonInvitationAccept"));
        buttonAccept.setClickEvent(ClickEventType.RUN_COMMAND, "/reversi accept");
        comp.addParts(buttonAccept);
        comp.addText("  ");
        MessageParts buttonDeny = new MessageParts(Messages.get("ButtonInvitationDeny"));
        buttonDeny.setClickEvent(ClickEventType.RUN_COMMAND, "/reversi deny");
        comp.addParts(buttonDeny);
        sendMessageComponent(opponentName, comp);
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
        Location opponentStartLoc = origin.clone().add(4, 5, 8)
                .setDirection(new Vector(0, -5, -5).normalize());

        // 両者のプレイヤーを取得する。ログイン状態でなければ、ゲームはキャンセルする。
        Player owner = Utility.getPlayerExact(ownerName);
        Player opponent = Utility.getPlayerExact(opponentName);
        if ( owner == null || opponent == null || !owner.isOnline() || !opponent.isOnline() ) {
            runCancel();
            return;
        }

        // 何かに乗っている、何かを乗せているなら強制パージする
        owner.leaveVehicle();
        owner.eject();
        opponent.leaveVehicle();
        opponent.eject();

        // 元いた場所を記憶する
        ownerReturnPoint = owner.getLocation();
        opponentReturnPoint = opponent.getLocation();

        // スタート地点に送る
        owner.teleport(ownerStartLoc, TeleportCause.PLUGIN);
        opponent.teleport(opponentStartLoc, TeleportCause.PLUGIN);

        // 飛行状態に変更する、ゲームモードはSURVIVALにする
        owner.setGameMode(GameMode.SURVIVAL);
        owner.setAllowFlight(true);
        owner.setFlying(true);
        opponent.setGameMode(GameMode.SURVIVAL);
        opponent.setAllowFlight(true);
        opponent.setFlying(true);

        // プレイヤーの身ぐるみを剥がす
        tempStorage = new TemporaryStorage();
        tempStorage.sendToTemp(owner);
        tempStorage.sendToTemp(opponent);

        // 先攻後攻を決める
        int value = (int)(Math.random() * 2);
        if ( value == 0 ) {
            blackPlayerName = ownerName;
            whitePlayerName = opponentName;
        } else {
            blackPlayerName = opponentName;
            whitePlayerName = ownerName;
        }

        // アイテムを持たせる
        Player blackPlayer = Utility.getPlayerExact(blackPlayerName);
        Player whitePlayer = Utility.getPlayerExact(whitePlayerName);

        ItemStack netherBrick = new ItemStack(Material.NETHER_BRICK, 64);
        blackPlayer.getInventory().addItem(netherBrick);

        ItemStack quartzBlock = new ItemStack(Material.QUARTZ_BLOCK, 64);
        whitePlayer.getInventory().addItem(quartzBlock);

        // メッセージを流す
        sendInfoMessageAll(Messages.get("InformationStarting"));
        if ( parent.getReversiLabConfig().isBroadcastSessionStartEnd() ) {
            sendBroadcastInfoMessage(Messages.get("BroadcastSessionStart",
                    new String[]{"%owner", "%opponent"}, new String[]{ownerName, opponentName}));
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
    private void runPreTurn(CellState state) {

        this.turn = GameSessionTurn.BLACK_PRE;

        String playerName = (state == CellState.BLACK) ? blackPlayerName : whitePlayerName;
        String otherName = (state == CellState.BLACK) ? whitePlayerName : blackPlayerName;
        Player player = Utility.getPlayerExact(playerName);
        Player other = Utility.getPlayerExact(otherName);

        // メッセージを表示
        if ( player != null && player.isOnline() ) {
            TitleDisplayComponent.display(player, Messages.get("InformationYourTurn"), 10, 50, 20);
        }
        if ( other != null && other.isOnline() ) {
            TitleDisplayComponent.display(other, Messages.get("InformationOtherTurn"), 10, 50, 20);
        }

        // 黒が石を置けることを確認する。置けないならパスを行う。
        if ( !board.canPut(state) ) {
            sendInfoMessageAll(Messages.get("InformationAutoPass", "%player", playerName));
            runPreTurn(state.getReverse());
            return;
        }

        this.turn = (state == CellState.BLACK) ? GameSessionTurn.BLACK : GameSessionTurn.WHITE;
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
            String name = (state == CellState.BLACK) ? blackPlayerName : whitePlayerName;
            sendErrorMessage(name, Messages.get("ErrorCannotPut"));
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
    public void tryPut(int x, int y, final CellState state) {

        // 手番でなければ、エラーメッセージを表示する。
        if ( state == CellState.BLACK && turn != GameSessionTurn.BLACK ) {
            sendErrorMessage(blackPlayerName, Messages.get("ErrorNotYourTurn"));
            return;
        }
        if ( state == CellState.WHITE && turn != GameSessionTurn.WHITE ) {
            sendErrorMessage(whitePlayerName, Messages.get("ErrorNotYourTurn"));
            return;
        }

        // 置ける場所なのかどうかを確認する。置けないならエラーメッセージを表示する。
        if ( !board.canPutAt(x, y, state) ) {
            String name = (state == CellState.BLACK) ? blackPlayerName : whitePlayerName;
            sendErrorMessage(name, Messages.get("ErrorCannotPut"));
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
     * ゲームのWINフェーズを実行する
     */
    public void runEnd() {

        // この時点では、まだフェーズを変更しない。IN_GAMEのままにしておく。
        // this.phase = GameSessionPhase.END;

        // どちらが勝ちか確認する。
        int black = board.getBlackCount();
        int white = board.getWhiteCount();
        int sessionEndWaitSeconds = parent.getReversiLabConfig().getSessionEndWaitSeconds();
        String winner = null;
        String looser = null;
        if ( black > white ) {
            winner = blackPlayerName;
            looser = whitePlayerName;
        } else if ( black < white ) {
            winner = whitePlayerName;
            looser = blackPlayerName;
        }

        // メッセージを表示する
        String msg;
        if ( winner != null ) {
            msg = Messages.get("InformationEnd",
                    new String[]{"%black", "%white", "%winner"},
                    new String[]{"" + black, "" + white, winner});
        } else {
            msg = Messages.get("InformationEndDraw",
                    new String[]{"%black", "%white"},
                    new String[]{"" + black, "" + white});
        }

        sendInfoMessageAll(msg);
        sendInfoMessageAll(Messages.get("InformationEndWait", "%seconds", sessionEndWaitSeconds));

        if ( parent.getReversiLabConfig().isBroadcastSessionStartEnd() ) {
            if ( winner != null ) {
                msg = Messages.get("BroadcastSessionEnd",
                        new String[]{"%owner", "%opponent", "%black", "%white", "%winner"},
                        new String[]{ownerName, opponentName, "" + black, "" + white, winner});
            } else {
                msg = Messages.get("BroadcastSessionEndDraw",
                        new String[]{"%owner", "%opponent", "%black", "%white"},
                        new String[]{ownerName, opponentName, "" + black, "" + white});
            }

            sendBroadcastInfoMessage(msg);
        }

        // 盤面をログに記録する
        for ( String line : board.getStringForPrint() ) {
            logger.log(line);
        }

        // 引き分けでなければ、花火を発生させる。
        if ( winner != null ) {
            field.spawnFireworks();
        }

        // ランキングデータに勝敗を加算する
        if ( winner != null ) {
            PlayerScoreData winnerScore = PlayerScoreData.getData(winner);
            winnerScore.increaseGamePlayed();
            winnerScore.increaseGameWin();
            winnerScore.save();
            PlayerScoreData looserScore = PlayerScoreData.getData(looser);
            looserScore.increaseGamePlayed();
            looserScore.increaseGameLose();
            looserScore.save();
        } else {
            PlayerScoreData blackScore = PlayerScoreData.getData(blackPlayerName);
            blackScore.increaseGamePlayed();
            blackScore.increaseGameDraw();
            blackScore.save();
            PlayerScoreData whiteScore = PlayerScoreData.getData(whitePlayerName);
            whiteScore.increaseGamePlayed();
            whiteScore.increaseGameDraw();
            whiteScore.save();
        }

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
        parent.getGameSessionManager().removeSession(this);

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

        Player opponent = getOpponentPlayer();
        if ( opponent != null ) {

            // 元いた場所に戻す
            if ( opponentReturnPoint != null ) {
                opponent.teleport(opponentReturnPoint, TeleportCause.PLUGIN);
            }

            // 飛行状態を解除する
            opponent.setAllowFlight(false);
            opponent.setFlying(false);

            // 持ち物を預かっているなら返す
            if ( tempStorage != null ) {
                tempStorage.restoreFromTemp(opponent);
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
     * 指定されたプレイヤー名は、対戦を受けた側かどうかを確認する。
     * @param playerName プレイヤー名
     * @return 対戦を受けた側かどうか
     */
    public boolean isOpponent(String playerName) {
        return opponentName.equals(playerName);
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
        return isOwner(playerName) || isOpponent(playerName) || isSpectator(playerName);
    }

    /**
     * セッションオーナーを取得する
     * @return セッションオーナー
     */
    public Player getOwnerPlayer() {
        return Utility.getPlayerExact(ownerName);
    }

    /**
     * 対局者を取得する
     * @return 対局者
     */
    public Player getOpponentPlayer() {
        return Utility.getPlayerExact(opponentName);
    }

    /**
     * 関連プレイヤーをすべて取得する
     * @return 全ての関連プレイヤー
     */
    public ArrayList<Player> getRelatedPlayers() {
        ArrayList<Player> players = new ArrayList<Player>();
        Player owner = getOwnerPlayer();
        if ( owner != null ) players.add(owner);
        Player opponent = getOpponentPlayer();
        if ( opponent != null ) players.add(opponent);
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

        // INVITATION中のオーナーは、キャンセルOK
        if ( phase == GameSessionPhase.INVITATION && player.getName().equals(ownerName) ) {
            return true;
        }

        // IN_GAME中は、相手がオフラインなら、キャンセルOK
        if ( phase == GameSessionPhase.IN_GAME && player.getName().equals(ownerName) ) {
            Player opponent = getOpponentPlayer();
            if ( opponent == null || !opponent.isOnline() ) {
                return true;
            }
        } else if ( phase == GameSessionPhase.IN_GAME && player.getName().equals(opponentName) ) {
            Player owner = getOwnerPlayer();
            if ( owner == null || !owner.isOnline() ) {
                return true;
            }
        }

        return false;
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

    public String getOpponentName() {
        return opponentName;
    }

    public String getBlackPlayerName() {
        return blackPlayerName;
    }

    public String getWhitePlayerName() {
        return whitePlayerName;
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
        players.add(opponentName);
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
        return String.format("%s{owner=%s,opponent=%s}",
                this.getClass().getSimpleName(), ownerName, opponentName);
    }
}
