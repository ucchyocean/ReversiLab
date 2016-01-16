/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi;

import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.reversi.game.GameSession;
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
            for ( String com : new String[]{"versus", "accept", "deny", "cancel", "rank", "reload"} ) {
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
        GameSession session = parent.getGameSessionManager().getSession(player);
        if ( session == null || !session.isOpponent(player.getName()) ) {
            sendErrorMessage(sender, Messages.get("ErrorNotFoundVersusSession"));
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
        GameSession session = parent.getGameSessionManager().getSession(player);
        if ( session == null || !session.isOpponent(player.getName()) ) {
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
        GameSession session = parent.getGameSessionManager().getSession(player);
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

    private boolean doRank(CommandSender sender, Command command, String label, String[] args) {
        // TODO 未実装
        return true;
    }

    private boolean doReload(CommandSender sender, Command command, String label, String[] args) {

        // パーミッションのチェック
        if ( !sender.hasPermission(PERMISSION + "reload") ) {
            sendErrorMessage(sender, Messages.get("ErrorNotHavePermission"));
            return true;
        }

        // リロードする
        parent.reload();

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
