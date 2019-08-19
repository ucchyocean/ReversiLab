/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi.game;

import java.util.ArrayList;

import org.bitbucket.ucchy.reversi.BetRewardType;
import org.bitbucket.ucchy.reversi.Messages;
import org.bitbucket.ucchy.reversi.ReversiLab;
import org.bitbucket.ucchy.reversi.ReversiLabConfig;
import org.bitbucket.ucchy.reversi.Utility;
import org.bitbucket.ucchy.reversi.ai.ReversiAI;
import org.bitbucket.ucchy.reversi.ai.ReversiAIEasy;
import org.bitbucket.ucchy.reversi.ai.ReversiAIHard;
import org.bitbucket.ucchy.reversi.ai.ReversiAINormal;
import org.bitbucket.ucchy.reversi.ranking.PlayerScoreData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.github.ucchyocean.messaging.TitleDisplayComponent;

/**
 * CPU戦のゲームセッション
 * @author ucchy
 */
public class SingleGameSession extends GameSession {

    private static final boolean DEBUG = false;
    private static final int EFFECT_SPEED = 3;

    private ReversiLab parent;
    private GameSessionTurn turn;

    private SingleGameDifficulty difficulty;
    private ReversiAI ai;

    private String ownerName;
    private boolean isOwnerBlack;

    private Location ownerReturnPoint;

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

        // そのままPREPAREフェーズを実行する
        runPrepare();
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

        // プレイヤーの持ち物を預かる
        if ( parent.getReversiLabConfig().isEnableTemporaryInventory() ) {
            tempStorage = new TemporaryStorage();
            tempStorage.sendToTemp(owner);
        }

        // スタート地点に送る
        owner.teleport(getField().getPrimaryPlayerLocation(), TeleportCause.PLUGIN);

        // 飛行状態に変更する、ゲームモードはSURVIVALにする
        owner.setGameMode(GameMode.SURVIVAL);
        owner.setAllowFlight(true);
        owner.setFlying(true);

        // 先攻後攻を決める
        int value = (int)(Math.random() * 2);
        isOwnerBlack = ( value == 0 );

        // アイテムを持たせる
        setDiscItemInHand(owner, isOwnerBlack);

        // AIを生成する
        if ( difficulty == SingleGameDifficulty.EASY ) {
            ai = new ReversiAIEasy();
        } else if ( difficulty == SingleGameDifficulty.NORMAL ) {
            ai = new ReversiAINormal();
        } else {
            ai = new ReversiAIHard();
        }

        // サイドバーを設定する
        setSidebarLeast();
        if ( isOwnerBlack ) {
            setSidebarBlackScore(ownerName);
            setSidebarWhiteScore(Messages.get("NameOfCPU"));
        } else {
            setSidebarBlackScore(Messages.get("NameOfCPU"));
            setSidebarWhiteScore(ownerName);
        }
        setSidebarShowPlayer(owner);

        // メッセージを流す
        sendInfoMessageAll(Messages.get("InformationStarting"));
        if ( parent.getReversiLabConfig().isBroadcastSessionStartEnd() ) {
            sendBroadcastInfoMessage(Messages.get("BroadcastSingleSessionStart",
                    new String[]{"%owner", "%cpu", "%difficulty"},
                    new String[]{ownerName, Messages.get("NameOfCPU"), difficulty.name()}));
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
    private void runPreTurn(final Piece piece) {

        this.turn = (piece == Piece.BLACK) ? GameSessionTurn.BLACK_PRE : GameSessionTurn.WHITE_PRE;

        boolean isPlayerTurn =
                (piece == Piece.BLACK && isOwnerBlack)  || (piece == Piece.WHITE && !isOwnerBlack);

        Player owner = Utility.getPlayerExact(ownerName);

        // 石を置けることを確認する。置けないならパスを行う。
        if ( !getBoard().canPut(piece) ) {
            String name = isPlayerTurn ? ownerName : Messages.get("NameOfCPU");
            sendInfoMessageAll(Messages.get("InformationAutoPass", "%player", name));
            runPreTurn(piece.getReverse());
            return;
        }

        // メッセージを表示
        if ( isPlayerTurn ) {
            TitleDisplayComponent.display(owner, Messages.get("InformationYourTurn"), 10, 50, 20);
        }

        this.turn = (piece == Piece.BLACK) ? GameSessionTurn.BLACK : GameSessionTurn.WHITE;

        // CPUが石を置く
        if ( !isPlayerTurn ) {

            final long startTime = System.currentTimeMillis();

            // CPUが長考したときに、メッセージを表示するためのタスク
            final BukkitRunnable msgTask = new BukkitRunnable() {
                public void run() {
                    Player player = getOwnerPlayer();
                    if ( player != null ) {
                        String cpu = Messages.get("NameOfCPU");
                        sendInfoMessageAll(Messages.get("InformationCPULongTime", "%cpu", cpu));
                    }
                }
            };
            msgTask.runTaskLater(parent, 40);

            // CPUが長考する可能性があるので、非同期処理スレッドで実行する
            new BukkitRunnable() {
                public void run() {

                    // 次に置く座標を取得
                    final int[] next = ai.getNext(getBoard(), piece);

                    msgTask.cancel();
                    long cpuTimeMillis = System.currentTimeMillis() - startTime;

                    // 同期処理に戻す。CPUに1秒かかっていない場合は、演出のために1秒待たせる。
                    int ticks = 20 - (int)(cpuTimeMillis / 50);
                    if ( ticks <= 0 ) ticks = 1;

                    if ( DEBUG ) {
                        System.out.println("DEBUG : cputime = " + cpuTimeMillis + ", ticks = " + ticks);
                    }

                    new BukkitRunnable() {
                        public void run() {
                            tryPut(next[0], next[1], piece);
                        }
                    }.runTaskLater(parent, ticks);
                }
            }.runTaskAsynchronously(parent);
        }
    }

    /**
     * 指定した座標に石を置いてみる。
     * @param location 座標
     * @param piece 置く石の種類
     */
    public void tryPut(Location location, final Piece piece) {

        int xOffset = location.getBlockX() - getField().getOrigin().getBlockX();
        int zOffset = location.getBlockZ() - getField().getOrigin().getBlockZ();

        // 置く場所おかしい場合は、エラーメッセージを表示する。
        if ( location.getBlockY() != getField().getOrigin().getBlockY() + 1
                || xOffset < 0 || 8 <= xOffset || zOffset < 0 || 8 <= zOffset ) {
            sendErrorMessage(ownerName, Messages.get("ErrorCannotPut"));
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
            sendErrorMessage(ownerName, Messages.get("ErrorNotYourTurn"));
            return;
        }
        if ( piece == Piece.WHITE && turn != GameSessionTurn.WHITE ) {
            sendErrorMessage(ownerName, Messages.get("ErrorNotYourTurn"));
            return;
        }

        // 置ける場所なのかどうかを確認する。置けないならエラーメッセージを表示する。
        if ( !getBoard().canPutAt(x, y, piece) ) {
            sendErrorMessage(ownerName, Messages.get("ErrorCannotPut"));
            return;
        }

        // 実際に置く。
        this.turn = (piece == Piece.BLACK) ? GameSessionTurn.BLACK_POST : GameSessionTurn.WHITE_POST;
        final ArrayList<int[]> reverses = getBoard().putAt(x, y, piece);
        getField().putStone(x, y, piece);

        // ログを記録
        log(String.format("(%d,%d) %s", x, y, piece));

        // サイドバーを設定する
        setSidebarLeast();
        if ( isOwnerBlack ) {
            setSidebarBlackScore(ownerName);
            setSidebarWhiteScore(Messages.get("NameOfCPU"));
        } else {
            setSidebarBlackScore(Messages.get("NameOfCPU"));
            setSidebarWhiteScore(ownerName);
        }

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
     * ゲームのENDフェーズを実行する
     */
    public void runEnd() {

        // この時点では、まだフェーズを変更しない。IN_GAMEのままにしておく。
        // this.phase = GameSessionPhase.END;

        // どちらが勝ちか確認する。
        int black = getBoard().getBlackCount();
        int white = getBoard().getWhiteCount();
        ReversiLabConfig config = parent.getReversiLabConfig();
        int sessionEndWaitSeconds = config.getSessionEndWaitSeconds();
        String winner = null;
        String cpuName = Messages.get("NameOfCPU");
        if ( black > white ) {
            winner = isOwnerBlack ? ownerName : cpuName;
        } else if ( black < white ) {
            winner = isOwnerBlack ? cpuName : ownerName;
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
                msg = Messages.get("BroadcastSingleSessionEnd",
                        new String[]{"%owner", "%cpu", "%difficulty", "%black", "%white", "%winner"},
                        new String[]{ownerName, cpuName, difficulty.toString(), "" + black, "" + white, winner});
            } else {
                msg = Messages.get("BroadcastSingleSessionEndDraw",
                        new String[]{"%owner", "%cpu", "%difficulty", "%black", "%white"},
                        new String[]{ownerName, cpuName, difficulty.toString(), "" + black, "" + white});
            }

            sendBroadcastInfoMessage(msg);
        }

        // 盤面をログに記録する
        for ( String line : getBoard().getStringForPrint() ) {
            log(line);
        }

        // プレイヤーが勝利したなら、花火を発生させる。
        if ( winner != null && winner.equals(ownerName) ) {
            getField().spawnFireworks();
        }

        // ランキングデータに勝敗を加算する
        if ( winner != null ) {
            if ( winner.equals(ownerName) ) {
                PlayerScoreData winnerScore = PlayerScoreData.getData(ownerName);
                winnerScore.get(difficulty).incrementPlayed();
                winnerScore.get(difficulty).incrementWin();
                winnerScore.save();
            } else {
                PlayerScoreData looserScore = PlayerScoreData.getData(ownerName);
                looserScore.get(difficulty).incrementPlayed();
                looserScore.get(difficulty).incrementLose();
                looserScore.save();
            }
        } else {
            PlayerScoreData blackScore = PlayerScoreData.getData(ownerName);
            blackScore.get(difficulty).incrementPlayed();
            blackScore.get(difficulty).incrementDraw();
            blackScore.save();
        }

        // プレイヤーが勝利したなら、必要に応じて報酬を与える
        if ( winner != null && winner.equals(ownerName)
                && config.getBetRewardType() != BetRewardType.NONE ) {

            if ( config.getBetRewardType() == BetRewardType.ITEM ) {
                ItemStack item = config.getRewardItem(difficulty);
                if ( item.getType() != Material.AIR ) {
                    if ( tempStorage != null ) {
                        tempStorage.addItem(ownerName, item);
                    } else {
                        Player owner = Utility.getPlayerExact(ownerName);
                        if ( owner != null ) {
                            owner.getInventory().addItem(item);
                        }
                    }
                    sendInfoMessage(ownerName, Messages.get("InformationRewardItemPaid",
                            new String[]{"%material", "%amount"},
                            new String[]{item.getType().toString(), item.getAmount() + ""}));
                }
            } else {
                int amount = config.getRewardEco(difficulty);
                if ( amount > 0 ) {
                    String format = parent.getVaultEco().format(amount);
                    parent.getVaultEco().depositPlayer(getOwnerPlayer(), amount);
                    sendInfoMessage(ownerName, Messages.get("InformationRewardEcoPaid", "%eco", format));
                }
            }
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
    @Override
    public void runCancel() {

        setPhase(GameSessionPhase.CANCEL);

        sendInfoMessageAll(Messages.get("InformationCancel"));

        runFinalize();
    }

    /**
     * ゲームを投了する
     * @param player 投了するプレイヤー
     * @see org.bitbucket.ucchy.reversi.game.GameSession#resign(org.bukkit.entity.Player)
     */
    @Override
    public void resign(Player player) {

        // メッセージを表示する
        int black = getBoard().getBlackCount();
        int white = getBoard().getWhiteCount();
        String cpuName = Messages.get("NameOfCPU");
        String msg = Messages.get("InformationEndResign",
                new String[]{"%black", "%white", "%winner", "%loser"},
                new String[]{"" + black, "" + white, cpuName, ownerName});

        sendInfoMessageAll(msg);

        if ( parent.getReversiLabConfig().isBroadcastSessionStartEnd() ) {
            msg = Messages.get("BroadcastSingleSessionEndResign",
                    new String[]{"%owner", "%cpu", "%difficulty", "%black", "%white", "%winner", "%loser"},
                    new String[]{ownerName, cpuName, difficulty.toString(), "" + black, "" + white, cpuName, ownerName});

            sendBroadcastInfoMessage(msg);
        }

        // 盤面をログに記録する
        for ( String line : getBoard().getStringForPrint() ) {
            log(line);
        }

        // ランキングデータに勝敗を加算する
        PlayerScoreData looserScore = PlayerScoreData.getData(ownerName);
        looserScore.get(difficulty).incrementPlayed();
        looserScore.get(difficulty).incrementLose();
        looserScore.save();

        // すぐに帰還する
        setPhase(GameSessionPhase.END);
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

            // 石を消去する
            clearDiscItemInInventory(owner);

            // 持ち物を預かっているなら返す
            if ( tempStorage != null ) {
                tempStorage.restoreFromTemp(owner);
            }

            // スコアボードを非表示にする
            removeSidebar(owner);
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
     * @see org.bitbucket.ucchy.reversi.game.GameSession#isOKtoCancel(org.bukkit.entity.Player)
     */
    @Override
    public boolean isOKtoCancel(Player player) {

        // 常にfalse
        return false;
    }

    /**
     * 指定したプレイヤーが、セッションを投了することができるかどうかを判定する
     * @param player プレイヤー
     * @return 投了かのうかどうか
     * @see org.bitbucket.ucchy.reversi.game.GameSession#isOKtoResign(org.bukkit.entity.Player)
     */
    @Override
    public boolean isOKtoResign(Player player) {

        // IN_GAMEならresignできる。
        return player.getName().equals(ownerName) && getPhase() == GameSessionPhase.IN_GAME;
    }

    /**
     * このセッションのオーナー名を取得する
     * @return オーナー名
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * このセッションの文字列表現
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return String.format("%s{owner=%s}",
                this.getClass().getSimpleName(), ownerName);
    }

    /**
     * 指定されたプレイヤー名は、このセッションのプレイヤーかどうかを確認する。
     * @param playerName プレイヤー名
     * @return プレイヤーかどうか
     * @see org.bitbucket.ucchy.reversi.game.GameSession#isPlayer(java.lang.String)
     */
    @Override
    public boolean isPlayer(String playerName) {
        return ownerName.equals(playerName);
    }

    /**
     * インベントリを預かっていたものと入れ替える。
     * @param player プレイヤー
     */
    @Override
    public void switchInventory(Player player) {
        if ( parent.getReversiLabConfig().isEnableTemporaryInventory()
                && tempStorage != null ) {
            tempStorage.switchWithTemp(player);
        }
    }
}
