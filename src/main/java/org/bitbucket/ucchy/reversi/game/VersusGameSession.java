/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi.game;

import java.util.ArrayList;

import org.bitbucket.ucchy.reversi.Messages;
import org.bitbucket.ucchy.reversi.ReversiLab;
import org.bitbucket.ucchy.reversi.TitleDisplayComponent;
import org.bitbucket.ucchy.reversi.Utility;
import org.bitbucket.ucchy.reversi.ranking.PlayerScoreData;
import org.bitbucket.ucchy.reversi.tellraw.ClickEventType;
import org.bitbucket.ucchy.reversi.tellraw.MessageComponent;
import org.bitbucket.ucchy.reversi.tellraw.MessageParts;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * 対戦のゲームセッション
 * @author ucchy
 */
public class VersusGameSession extends GameSession {

    private static final int EFFECT_SPEED = 3;

    private ReversiLab parent;
    private GameSessionTurn turn;

    private String ownerName;
    private String opponentName;
    private String blackPlayerName;
    private String whitePlayerName;

    private Location ownerReturnPoint;
    private Location opponentReturnPoint;
    private TemporaryStorage tempStorage;

    /**
     * コンストラクタ
     * @param parent
     * @param ownerName
     * @param opponentName
     */
    protected VersusGameSession(ReversiLab parent, String ownerName, String opponentName) {

        this.parent = parent;
        this.ownerName = ownerName;
        this.opponentName = opponentName;

        // そのままINVITATIONフェーズを実行する
        runInvitation();
    }

    /**
     * ゲームのINVITATIONフェーズを実行する
     */
    public void runInvitation() {

        setPhase(GameSessionPhase.INVITATION);

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

        setPhase(GameSessionPhase.PREPARE);

        // メッセージ表示
        sendInfoMessageAll(Messages.get("InformationPreparing"));

        // フィールドを生成する
        getField().makeField();

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
        owner.teleport(getField().getPrimaryPlayerLocation(), TeleportCause.PLUGIN);
        opponent.teleport(getField().getSecondaryPlayerLocation(), TeleportCause.PLUGIN);

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

        setPhase(GameSessionPhase.IN_GAME);

        // 先攻のターン
        runPreTurn(Piece.BLACK);
    }

    /**
     * 手番を開始する。
     * @param piece 黒または白
     */
    private void runPreTurn(Piece piece) {

        this.turn = (piece == Piece.BLACK) ? GameSessionTurn.BLACK_PRE : GameSessionTurn.WHITE_PRE;

        String playerName = (piece == Piece.BLACK) ? blackPlayerName : whitePlayerName;
        String otherName = (piece == Piece.BLACK) ? whitePlayerName : blackPlayerName;
        Player player = Utility.getPlayerExact(playerName);
        Player other = Utility.getPlayerExact(otherName);

        // 石を置けることを確認する。置けないならパスを行う。
        if ( !getBoard().canPut(piece) ) {
            sendInfoMessageAll(Messages.get("InformationAutoPass", "%player", playerName));
            runPreTurn(piece.getReverse());
            return;
        }

        // メッセージを表示
        if ( player != null && player.isOnline() ) {
            TitleDisplayComponent.display(player, Messages.get("InformationYourTurn"), 10, 50, 20);
        }
        if ( other != null && other.isOnline() ) {
            TitleDisplayComponent.display(other, Messages.get("InformationOtherTurn"), 10, 50, 20);
        }

        this.turn = (piece == Piece.BLACK) ? GameSessionTurn.BLACK : GameSessionTurn.WHITE;
    }

    /**
     * 指定した座標に石を置いてみる。
     * @param location 座標
     * @param piece 置く石の種類
     */
    public void tryPut(Location location, Piece piece) {

        int xOffset = location.getBlockX() - getField().getOrigin().getBlockX();
        int zOffset = location.getBlockZ() - getField().getOrigin().getBlockZ();

        // 置く場所おかしい場合は、エラーメッセージを表示する。
        if ( location.getBlockY() != getField().getOrigin().getBlockY() + 1
                || xOffset < 0 || 8 <= xOffset || zOffset < 0 || 8 <= zOffset ) {
            String name = (piece == Piece.BLACK) ? blackPlayerName : whitePlayerName;
            sendErrorMessage(name, Messages.get("ErrorCannotPut"));
            return;
        }

        tryPut(xOffset, zOffset, piece);
    }

    /**
     * 指定した座標に石を置いてみる。
     * @param x マス目のx座標
     * @param y マス目のy座標
     * @param piece 置く石の種類
     */
    private void tryPut(int x, int y, final Piece piece) {

        // 手番でなければ、エラーメッセージを表示する。
        if ( piece == Piece.BLACK && turn != GameSessionTurn.BLACK ) {
            sendErrorMessage(blackPlayerName, Messages.get("ErrorNotYourTurn"));
            return;
        }
        if ( piece == Piece.WHITE && turn != GameSessionTurn.WHITE ) {
            sendErrorMessage(whitePlayerName, Messages.get("ErrorNotYourTurn"));
            return;
        }

        // 置ける場所なのかどうかを確認する。置けないならエラーメッセージを表示する。
        if ( !getBoard().canPutAt(x, y, piece) ) {
            String name = (piece == Piece.BLACK) ? blackPlayerName : whitePlayerName;
            sendErrorMessage(name, Messages.get("ErrorCannotPut"));
            return;
        }

        // 実際に置く。
        this.turn = (piece == Piece.BLACK) ? GameSessionTurn.BLACK_POST : GameSessionTurn.WHITE_POST;
        final ArrayList<int[]> reverses = getBoard().putAt(x, y, piece);
        getField().putStone(x, y, piece);

        log(String.format("(%d,%d) %s", x, y, piece));

        // 演出のために、1つ1つ遅延をかけてひっくり返す
        new BukkitRunnable() {
            int index = 0;
            public void run() {
                if ( index < reverses.size() ) {
                    int[] coodinate = reverses.get(index);
                    getField().putStone(coodinate[0], coodinate[1], piece);
                } else {
                    cancel();

                    // 勝負が決着したかどうかを確認する。
                    // 決着したならENDへ、していないなら相手の番へ。
                    if ( !getBoard().canPutAll() ) {
                        runEnd();
                    } else {
                        runPreTurn(piece.getReverse());
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
        int black = getBoard().getBlackCount();
        int white = getBoard().getWhiteCount();
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
        for ( String line : getBoard().getStringForPrint() ) {
            log(line);
        }

        // 引き分けでなければ、花火を発生させる。
        if ( winner != null ) {
            getField().spawnFireworks();
        }

        // ランキングデータに勝敗を加算する
        if ( winner != null ) {
            PlayerScoreData winnerScore = PlayerScoreData.getData(winner);
            winnerScore.getVersus().incrementPlayed();
            winnerScore.getVersus().incrementWin();
            winnerScore.save();
            PlayerScoreData looserScore = PlayerScoreData.getData(looser);
            looserScore.getVersus().incrementPlayed();
            looserScore.getVersus().incrementLose();
            looserScore.save();
        } else {
            PlayerScoreData blackScore = PlayerScoreData.getData(blackPlayerName);
            blackScore.getVersus().incrementPlayed();
            blackScore.getVersus().incrementDraw();
            blackScore.save();
            PlayerScoreData whiteScore = PlayerScoreData.getData(whitePlayerName);
            whiteScore.getVersus().incrementPlayed();
            whiteScore.getVersus().incrementDraw();
            whiteScore.save();
        }

        // 15秒後に帰還する
        new BukkitRunnable() {
            public void run() {
                setPhase(GameSessionPhase.END);
                runFinalize();
            }
        }.runTaskLater(ReversiLab.getInstance(), sessionEndWaitSeconds * 20);
    }

    /**
     * ゲームのCANCELフェーズを実行する
     */
    public void runCancel() {

        setPhase(GameSessionPhase.CANCEL);

        sendInfoMessageAll(Messages.get("InformationCancel"));

        runFinalize();
    }

    /**
     * ゲームのCANCELフェーズを実行する
     */
    public void runInvitationDenyed() {

        setPhase(GameSessionPhase.INVITATION_DENYED);

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
            owner.setFallDistance(0);
            owner.setNoDamageTicks(5 * 20);

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
            opponent.setFallDistance(0);
            opponent.setNoDamageTicks(5 * 20);

            // 持ち物を預かっているなら返す
            if ( tempStorage != null ) {
                tempStorage.restoreFromTemp(opponent);
            }
        }

        // 全ての観客を退出させる
        leaveAllSpectators();

        // クリーンアップ
        getField().cleanup(false);
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
        for ( String name : getSpectators() ) {
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
        if ( getPhase() == GameSessionPhase.INVITATION && player.getName().equals(ownerName) ) {
            return true;
        }

        // IN_GAME中は、相手がオフラインなら、キャンセルOK
        if ( getPhase() == GameSessionPhase.IN_GAME && player.getName().equals(ownerName) ) {
            Player opponent = getOpponentPlayer();
            if ( opponent == null || !opponent.isOnline() ) {
                return true;
            }
        } else if ( getPhase() == GameSessionPhase.IN_GAME && player.getName().equals(opponentName) ) {
            Player owner = getOwnerPlayer();
            if ( owner == null || !owner.isOnline() ) {
                return true;
            }
        }

        return false;
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

    /**
     * このセッションの文字列表現
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return String.format("%s{owner=%s,opponent=%s}",
                this.getClass().getSimpleName(), ownerName, opponentName);
    }

    /**
     * 指定されたプレイヤー名は、このセッションのプレイヤーかどうかを確認する。
     * @param playerName プレイヤー名
     * @return プレイヤーかどうか
     * @see org.bitbucket.ucchy.reversi.game.GameSession#isPlayer(java.lang.String)
     */
    @Override
    public boolean isPlayer(String playerName) {
        return ownerName.equals(playerName) || opponentName.equals(playerName);
    }
}
