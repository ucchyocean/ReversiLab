/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi;

import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.reversi.game.VersusGameSession;
import org.bitbucket.ucchy.reversi.game.GameSessionPhase;
import org.bitbucket.ucchy.reversi.game.PlayerScoreData;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * ReversiLabのコマンドクラス
 * @author ucchy
 */
public class ReversiLabCommand implements TabExecutor {

    private static final String PERMISSION = "reversilab.";
    private ReversiLab parent;

    /**
     * コンストラクタ
     * @param parent
     */
    public ReversiLabCommand(ReversiLab parent) {
        this.parent = parent;
    }

    /**
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if ( args.length == 0 ) {
            return false;
        }

        if ( args[0].equalsIgnoreCase("versus") ) {
            return doVersus(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("accept") ) {
            return doAccept(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("deny") ) {
            return doDeny(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("cancel") ) {
            return doCancel(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("spectator") ) {
            return doSpectator(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("rank") ) {
            return doRank(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("reload") ) {
            return doReload(sender, command, label, args);
        }

        return false;
    }

    /**
     * @see org.bukkit.command.TabCompleter#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        // 1番目の引数の補完
        if ( args.length == 1 ) {
            String pre = args[0].toLowerCase();
            ArrayList<String> candidates = new ArrayList<String>();
            for ( String com : new String[]{"versus", "accept", "deny", "cancel", "spectator", "rank", "reload"} ) {
                if ( com.startsWith(pre) ) {
                    candidates.add(com);
                }
            }
            return candidates;
        }

        return null;
    }

    private boolean doVersus(CommandSender sender, Command command, String label, String[] args) {

        // パーミッションのチェック
        if ( !sender.hasPermission(PERMISSION + "versus") ) {
            sendErrorMessage(sender, Messages.get("ErrorNotHavePermission"));
            return true;
        }

        // Playerでないならエラー
        if ( !(sender instanceof Player) ) {
            sendErrorMessage(sender, Messages.get("ErrorNotPlayer"));
            return true;
        }

        Player player = (Player)sender;

        // コマンド引数が1つしかないならエラー
        if ( args.length < 2 ) {
            sendErrorMessage(sender, Messages.get("ErrorNotSpecifiedVersusPlayer"));
            return true;
        }

        // 指定された対戦相手が見つからないならエラー
        Player target = Utility.getPlayerExact(args[1]);
        if ( target == null || !target.isOnline() ) {
            sendErrorMessage(sender, Messages.get("ErrorCannotFindPlayer"));
            return true;
        }

        // 自分自身を指定した場合はエラー
        if ( player.getName().equals(target.getName()) ) {
            sendErrorMessage(sender, Messages.get("ErrorCannotSpecifySelfVersus"));
            return true;
        }

        // 指定されたプレイヤーが既に対戦中の場合はエラー
        if ( parent.getGameSessionManager().getSession(target) != null ) {
            sendErrorMessage(sender, Messages.get("ErrorTargetIsInGame"));
            return true;
        }

        // 禁止ワールドに居る場合はエラー
        if ( parent.getReversiLabConfig().getProhibitWorlds().contains(player.getWorld().getName()) ) {
            sendErrorMessage(sender, Messages.get("ErrorProhibitWorlds"));
            return true;
        }

        // ゲームセッションを作成する
        parent.getGameSessionManager().createNewSession(player, target);

        return true;
    }

    private boolean doAccept(CommandSender sender, Command command, String label, String[] args) {

        // パーミッションのチェック
        if ( !sender.hasPermission(PERMISSION + "accept") ) {
            sendErrorMessage(sender, Messages.get("ErrorNotHavePermission"));
            return true;
        }

        // Playerでないならエラー
        if ( !(sender instanceof Player) ) {
            sendErrorMessage(sender, Messages.get("ErrorNotPlayer"));
            return true;
        }

        // 対戦を受けたセッションが無い場合はエラー
        Player player = (Player)sender;
        VersusGameSession session = parent.getGameSessionManager().getSession(player);
        if ( session == null || !session.isOpponent(player.getName()) ) {
            sendErrorMessage(sender, Messages.get("ErrorNotFoundVersusSession"));
            return true;
        }

        // フェーズがINVITATIONで無い場合はエラー
        if ( session.getPhase() != GameSessionPhase.INVITATION ) {
            sendErrorMessage(sender, Messages.get("ErrorNotFoundVersusSession"));
            return true;
        }

        // 禁止ワールドに居る場合はエラー
        if ( parent.getReversiLabConfig().getProhibitWorlds().contains(player.getWorld().getName()) ) {
            sendErrorMessage(sender, Messages.get("ErrorProhibitWorlds"));
            return true;
        }

        // オーナーがオフラインの場合は、対戦をキャンセルする。
        Player owner = session.getOwnerPlayer();
        if ( owner == null || !owner.isOnline() ) {
            session.runCancel();
            sendErrorMessage(sender, Messages.get("ErrorOwnerIsOffline", "%owner", session.getOwnerName()));
            return true;
        }

        // オーナーが禁止ワールドに移動した場合は、対戦をキャンセルする。
        if ( parent.getReversiLabConfig().getProhibitWorlds().contains(owner.getWorld().getName()) ) {
            session.runCancel();
            sendErrorMessage(sender, Messages.get("ErrorOwnerInProhibitWorlds", "%owner", session.getOwnerName()));
            return true;
        }

        // 対戦を開始する。
        session.runPrepare();

        return true;
    }

    private boolean doDeny(CommandSender sender, Command command, String label, String[] args) {

        // パーミッションのチェック
        if ( !sender.hasPermission(PERMISSION + "accept") ) {
            sendErrorMessage(sender, Messages.get("ErrorNotHavePermission"));
            return true;
        }

        // Playerでないならエラー
        if ( !(sender instanceof Player) ) {
            sendErrorMessage(sender, Messages.get("ErrorNotPlayer"));
            return true;
        }

        // 対戦を受けたセッションが無い場合はエラー
        Player player = (Player)sender;
        VersusGameSession session = parent.getGameSessionManager().getSession(player);
        if ( session == null || !session.isOpponent(player.getName()) ) {
            sendErrorMessage(sender, Messages.get("ErrorNotFoundVersusSession"));
            return true;
        }

        // フェーズがINVITATIONで無い場合はエラー
        if ( session.getPhase() != GameSessionPhase.INVITATION ) {
            sendErrorMessage(sender, Messages.get("ErrorNotFoundVersusSession"));
            return true;
        }

        // 対戦を拒否する。
        session.runInvitationDenyed();

        return true;
    }

    private boolean doCancel(CommandSender sender, Command command, String label, String[] args) {

        // パーミッションのチェック
        if ( !sender.hasPermission(PERMISSION + "accept") ) {
            sendErrorMessage(sender, Messages.get("ErrorNotHavePermission"));
            return true;
        }

        // Playerでないならエラー
        if ( !(sender instanceof Player) ) {
            sendErrorMessage(sender, Messages.get("ErrorNotPlayer"));
            return true;
        }

        // オーナーのセッションが無い場合はエラー
        Player player = (Player)sender;
        VersusGameSession session = parent.getGameSessionManager().getSession(player);
        if ( session == null ) {
            sendErrorMessage(sender, Messages.get("ErrorNotFoundSession"));
            return true;
        }

        // キャンセル不可ならエラー
        if ( !session.isOKtoCancel(player) ) {
            sendErrorMessage(sender, Messages.get("ErrorCannotCancelSession"));
            return true;
        }

        // 対戦をキャンセルする。
        session.runCancel();

        return true;
    }

    private boolean doSpectator(CommandSender sender, Command command, String label, String[] args) {

        // パーミッションのチェック
        if ( !sender.hasPermission(PERMISSION + "spectator") ) {
            sendErrorMessage(sender, Messages.get("ErrorNotHavePermission"));
            return true;
        }

        // Playerでないならエラー
        if ( !(sender instanceof Player) ) {
            sendErrorMessage(sender, Messages.get("ErrorNotPlayer"));
            return true;
        }

        Player player = (Player)sender;
        VersusGameSession mySession = parent.getGameSessionManager().getSession(player);
        if ( mySession != null && !mySession.isEnd() ) {
            // 既にセッションに参加している場合

            // セッションの観客ではなく、プレイヤーである場合
            if ( mySession.isOwner(player.getName()) || mySession.isOpponent(player.getName()) ) {
                sendErrorMessage(sender, Messages.get("ErrorJoinedSessionAlready"));
                return true;
            }

            // 観客から退出する
            mySession.leaveSpectator(player);
            sendInfoMessage(sender, Messages.get("InformationLeaveSpectator"));
            return true;

        } else {
            // 現在セッションに参加していない場合

            // 引数が指定されていないならエラー
            if ( args.length < 2 ) {
                sendErrorMessage(sender, Messages.get("ErrorSpectatorInvalidArgument"));
                return true;
            }

            // 指定されたプレイヤー名に関連するセッションが見つからないならエラー
            VersusGameSession targetSession = parent.getGameSessionManager().getSession(args[1]);
            if ( targetSession == null ) {
                sendErrorMessage(sender, Messages.get("ErrorSpectatorInvalidArgument"));
                return true;
            }

            // 観客として参加する
            targetSession.joinSpectator(player);
            sendInfoMessage(sender, Messages.get("InformationJoinSpectator"));
            return true;
        }
    }

    private boolean doRank(CommandSender sender, Command command, String label, String[] args) {

        // パーミッションのチェック
        if ( !sender.hasPermission(PERMISSION + "rank") ) {
            sendErrorMessage(sender, Messages.get("ErrorNotHavePermission"));
            return true;
        }

        // ランキングデータの取得とソート
        String title;
        ArrayList<PlayerScoreData> datas = PlayerScoreData.getAllData();
        if ( args.length >= 2 && args[1].equalsIgnoreCase("play") ) {
            PlayerScoreData.sortByGamePlayed(datas);
            title = Messages.get("RankingTitle", "%type", Messages.get("RankingPlay"));
        } else if ( args.length >= 2 && args[1].equalsIgnoreCase("played") ) {
            PlayerScoreData.sortByGamePlayed(datas);
            title = Messages.get("RankingTitle", "%type", Messages.get("RankingPlay"));
        } else if ( args.length >= 2 && args[1].equalsIgnoreCase("ratio") ) {
            PlayerScoreData.sortByRatio(datas);
            title = Messages.get("RankingTitle", "%type", Messages.get("RankingRatio"));
        } else if ( args.length >= 2 && args[1].equalsIgnoreCase("lose") ) {
            PlayerScoreData.sortByGameLose(datas);
            title = Messages.get("RankingTitle", "%type", Messages.get("RankingLose"));
        } else {
            PlayerScoreData.sortByGameWin(datas);
            title = Messages.get("RankingTitle", "%type", Messages.get("RankingWin"));
        }

        // 表示
        sender.sendMessage(title);

        boolean isRankin = false;

        for ( int index=0; index<10; index++ ) {

            if ( index >= datas.size() ) break;

            PlayerScoreData data = datas.get(index);

            boolean isSelf = data.getName().equals(sender.getName());
            if ( isSelf ) isRankin = true;

            String rank = (isSelf ? ChatColor.RED.toString() : "") +
                    String.format("%1$2d", (index + 1));
            String name = String.format("%1$-12s", data.getName());
            String played = "" + data.getGamePlayed();
            String win = "" + data.getGameWin();
            String lose = "" + data.getGameLose();
            String ratio = String.format("%1$.2f", data.getRatio());

            String line = Messages.get("RankingFormat",
                    new String[]{"%rank", "%name", "%played", "%win", "%lose", "%ratio"},
                    new String[]{rank, name, played, win, lose, ratio});
            sender.sendMessage(line);
        }

        // 10位以内に入っていないなら、自分のスコアを探して表示する
        if ( !isRankin ) {
            for ( int index=10; index<datas.size(); index++ ) {
                PlayerScoreData data = datas.get(index);
                if ( data.getName().equals(sender.getName()) ) {
                    String rank = ChatColor.RED + String.format("%1$2d", (index + 1));
                    String name = String.format("%1$-12s", data.getName());
                    String played = "" + data.getGamePlayed();
                    String win = "" + data.getGameWin();
                    String lose = "" + data.getGameLose();
                    String ratio = String.format("%1$.2f", data.getRatio());

                    String line = Messages.get("RankingFormat",
                            new String[]{"%rank", "%name", "%played", "%win", "%lose", "%ratio"},
                            new String[]{rank, name, played, win, lose, ratio});
                    sender.sendMessage(line);

                    break;
                }
            }
        }

        return true;
    }

    private boolean doReload(CommandSender sender, Command command, String label, String[] args) {

        // パーミッションのチェック
        if ( !sender.hasPermission(PERMISSION + "reload") ) {
            sendErrorMessage(sender, Messages.get("ErrorNotHavePermission"));
            return true;
        }

        // リロードする
        parent.getReversiLabConfig().reload();
        Messages.reload(parent.getReversiLabConfig().getLang());

        sendInfoMessage(sender, Messages.get("InformationReloaded"));
        return true;
    }

    /**
     * 指定されたCommandSenderに情報メッセージを送る。
     * @param sender メッセージの送り先
     * @param message メッセージ
     */
    private void sendInfoMessage(CommandSender sender, String message) {
        if ( message == null || message.equals("") ) return;
        String prefix = Messages.get("PrefixInformation");
        sender.sendMessage(prefix + message);
    }

    /**
     * 指定されたCommandSenderにエラーメッセージを送る。
     * @param sender メッセージの送り先
     * @param message メッセージ
     */
    private void sendErrorMessage(CommandSender sender, String message) {
        if ( message == null || message.equals("") ) return;
        String prefix = Messages.get("PrefixError");
        sender.sendMessage(prefix + message);
    }
}
