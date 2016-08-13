/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi;

import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.reversi.game.GameSession;
import org.bitbucket.ucchy.reversi.game.SingleGameDifficulty;
import org.bitbucket.ucchy.reversi.game.VersusGameSession;
import org.bitbucket.ucchy.reversi.ranking.PlayerScoreData;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

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

        if ( args[0].equalsIgnoreCase("single") ) {
            return doSingle(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("versus") ) {
            return doVersus(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("accept") ) {
            return doAccept(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("deny") ) {
            return doDeny(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("cancel") ) {
            return doCancel(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("resign") ) {
            return doResign(sender, command, label, args);
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

    private boolean doSingle(CommandSender sender, Command command, String label, String[] args) {

        // パーミッションのチェック
        if ( !sender.hasPermission(PERMISSION + "single") ) {
            sendErrorMessage(sender, Messages.get("ErrorNotHavePermission"));
            return true;
        }

        // Playerでないならエラー
        if ( !(sender instanceof Player) ) {
            sendErrorMessage(sender, Messages.get("ErrorNotPlayer"));
            return true;
        }

        Player player = (Player)sender;
        SingleGameDifficulty difficulty = SingleGameDifficulty.NORMAL;

        // 難易度指定の取得
        if ( args.length >= 2 ) {
            if ( args[1].equalsIgnoreCase("easy") ) {
                difficulty = SingleGameDifficulty.EASY;
            } else if ( args[1].equalsIgnoreCase("hard") ) {
                difficulty = SingleGameDifficulty.HARD;
            }
        }

        // 既にセッションに居る場合はエラー
        if ( parent.getGameSessionManager().getSession(player) != null ) {
            sendErrorMessage(sender, Messages.get("ErrorYouAreInGame"));
            return true;
        }

        // 禁止ワールドに居る場合はエラー
        if ( parent.getReversiLabConfig().getProhibitWorlds().contains(player.getWorld().getName()) ) {
            sendErrorMessage(sender, Messages.get("ErrorProhibitWorlds"));
            return true;
        }

        // 掛け金、掛けアイテムが必要な場合は、ここで徴収する。
        // 無い場合はエラー
        ReversiLabConfig config = parent.getReversiLabConfig();
        if ( config.getBetRewardType() != BetRewardType.NONE ) {
            if ( config.getBetRewardType() == BetRewardType.ITEM ) {
                ItemStack item = config.getBetItem(difficulty);
                if ( item.getType() != Material.AIR ) {
                    if ( !hasItem(player, item) ) {
                        sendErrorMessage(sender, Messages.get("ErrorBetItemShortage",
                                new String[]{"%material", "%amount"},
                                new String[]{item.getType().toString(), item.getAmount() + ""}));
                        return true;
                    }
                    consumeItem(player, item);
                    sendInfoMessage(sender, Messages.get("InformationBetItemConsumed",
                                new String[]{"%material", "%amount"},
                                new String[]{item.getType().toString(), item.getAmount() + ""}));
                }
            } else {
                int amount = config.getBetEco(difficulty);
                if ( amount > 0 ) {
                    String format = parent.getVaultEco().format(amount);
                    if ( !parent.getVaultEco().has(player, amount) ) {
                        sendErrorMessage(sender, Messages.get("ErrorBetEcoShortage", "%eco", format));
                        return true;
                    }
                    parent.getVaultEco().withdrawPlayer(player, amount);
                    sendInfoMessage(sender, Messages.get("InformationBetEcoConsumed", "%eco", format));
                }
            }
        }

        // ゲームセッションを作成する
        parent.getGameSessionManager().createNewSingleGameSession(player, difficulty);

        return true;
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

        // 既にセッションに居る場合はエラー
        if ( parent.getGameSessionManager().getSession(player) != null ) {
            sendErrorMessage(sender, Messages.get("ErrorYouAreInGame"));
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

        // 掛け金、掛けアイテムが必要な場合は、持っているかどうかを確認する。持っていないならエラー
        ReversiLabConfig config = parent.getReversiLabConfig();
        if ( config.getBetRewardType() != BetRewardType.NONE ) {
            if ( config.getBetRewardType() == BetRewardType.ITEM ) {
                ItemStack item = config.getVersusBetItem();
                if ( item.getType() != Material.AIR ) {
                    if ( !hasItem(player, item) ) {
                        sendErrorMessage(sender, Messages.get("ErrorBetItemShortage",
                                new String[]{"%material", "%amount"},
                                new String[]{item.getType().toString(), item.getAmount() + ""}));
                        return true;
                    }
                }
            } else {
                int amount = config.getVersusBetEco();
                if ( amount > 0 ) {
                    String format = parent.getVaultEco().format(amount);
                    if ( !parent.getVaultEco().has(player, amount) ) {
                        sendErrorMessage(sender, Messages.get("ErrorBetEcoShortage", "%eco", format));
                        return true;
                    }
                }
            }
        }

        // ゲームセッションを作成する
        VersusGameSession session =
                parent.getGameSessionManager().createNewVersusGameSession(player, target);

        // 掛け金、掛けアイテムを徴収する
        if ( config.getBetRewardType() != BetRewardType.NONE ) {
            if ( config.getBetRewardType() == BetRewardType.ITEM ) {
                ItemStack item = config.getVersusBetItem();
                if ( item.getType() != Material.AIR ) {
                    consumeItem(player, item);
                    session.setOwnerBetItemTemp(item);
                    sendInfoMessage(sender, Messages.get("InformationBetItemConsumed",
                            new String[]{"%material", "%amount"},
                            new String[]{item.getType().toString(), item.getAmount() + ""}));
                }
            } else {
                int amount = config.getVersusBetEco();
                if ( amount > 0 ) {
                    String format = parent.getVaultEco().format(amount);
                    parent.getVaultEco().withdrawPlayer(player, amount);
                    session.setOwnerBetEcoTemp(amount);
                    sendInfoMessage(sender, Messages.get("InformationBetEcoConsumed", "%eco", format));
                }
            }
        }

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
        VersusGameSession session = parent.getGameSessionManager().getInvitedSession(player);
        if ( session == null ) {
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

        // 掛け金、掛けアイテムが必要な場合は、持っているかどうかを確認する。
        // 持っていないならエラー。持っているなら徴収。
        ReversiLabConfig config = parent.getReversiLabConfig();
        if ( config.getBetRewardType() != BetRewardType.NONE ) {
            if ( config.getBetRewardType() == BetRewardType.ITEM ) {
                ItemStack item = config.getVersusBetItem();
                if ( item.getType() != Material.AIR ) {
                    if ( !hasItem(player, item) ) {
                        sendErrorMessage(sender, Messages.get("ErrorBetItemShortage",
                                new String[]{"%material", "%amount"},
                                new String[]{item.getType().toString(), item.getAmount() + ""}));
                        return true;
                    }
                    consumeItem(player, item);
                    sendInfoMessage(sender, Messages.get("InformationBetItemConsumed",
                            new String[]{"%material", "%amount"},
                            new String[]{item.getType().toString(), item.getAmount() + ""}));
                }
            } else {
                int amount = config.getVersusBetEco();
                if ( amount > 0 ) {
                    String format = parent.getVaultEco().format(amount);
                    if ( !parent.getVaultEco().has(player, amount) ) {
                        sendErrorMessage(sender, Messages.get("ErrorBetEcoShortage", "%eco", format));
                        return true;
                    }
                    parent.getVaultEco().withdrawPlayer(player, amount);
                    sendInfoMessage(sender, Messages.get("InformationBetEcoConsumed", "%eco", format));
                }
            }
        }

        // 対戦を開始する。
        session.runPrepare();

        return true;
    }

    private boolean doDeny(CommandSender sender, Command command, String label, String[] args) {

        // パーミッションのチェック
        if ( !sender.hasPermission(PERMISSION + "deny") ) {
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
        VersusGameSession session = parent.getGameSessionManager().getInvitedSession(player);
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
        if ( !sender.hasPermission(PERMISSION + "cancel") ) {
            sendErrorMessage(sender, Messages.get("ErrorNotHavePermission"));
            return true;
        }

        // Playerでないならエラー
        if ( !(sender instanceof Player) ) {
            sendErrorMessage(sender, Messages.get("ErrorNotPlayer"));
            return true;
        }

        // セッションが無い場合はエラー
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

    private boolean doResign(CommandSender sender, Command command, String label, String[] args) {

        // パーミッションのチェック
        if ( !sender.hasPermission(PERMISSION + "resign") ) {
            sendErrorMessage(sender, Messages.get("ErrorNotHavePermission"));
            return true;
        }

        // Playerでないならエラー
        if ( !(sender instanceof Player) ) {
            sendErrorMessage(sender, Messages.get("ErrorNotPlayer"));
            return true;
        }

        // セッションが無い場合はエラー
        Player player = (Player)sender;
        GameSession session = parent.getGameSessionManager().getSession(player);
        if ( session == null ) {
            sendErrorMessage(sender, Messages.get("ErrorNotFoundSession"));
            return true;
        }

        // 投了不可ならエラー
        if ( !session.isOKtoResign(player) ) {
            sendErrorMessage(sender, Messages.get("ErrorCannotResignSession"));
            return true;
        }

        // 対戦を投了する。
        session.resign(player);

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
        GameSession mySession = parent.getGameSessionManager().getSession(player);
        if ( mySession != null && !mySession.isEnd() ) {
            // 既にセッションに参加している場合

            // セッションの観客ではなく、プレイヤーである場合
            if ( mySession.isPlayer(player.getName()) ) {
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
            GameSession targetSession = parent.getGameSessionManager().getSession(args[1]);
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

        // 引数のパース
        String type = "win";
        String typeDisplayString = Messages.get("RankingWin");
        String kind = "versus";
        String kindDisplayString = Messages.get("RankingVersus");

        if ( args.length >= 2 ) {
            if ( (args[1].equalsIgnoreCase("play") || args[1].equalsIgnoreCase("played")) ) {
                type = "played";
                typeDisplayString = Messages.get("RankingPlay");
            } else if ( args[1].equalsIgnoreCase("ratio") ) {
                type = "ratio";
                typeDisplayString = Messages.get("RankingRatio");
            } else if ( args[1].equalsIgnoreCase("lose") ) {
                type = "lose";
                typeDisplayString = Messages.get("RankingLose");
            }
        }

        if ( args.length >= 3 ) {
            if ( args[2].equalsIgnoreCase("easy") ) {
                kind = "easy";
                kindDisplayString = SingleGameDifficulty.EASY.toString();
            } else if ( args[2].equalsIgnoreCase("normal") ) {
                kind = "normal";
                kindDisplayString = SingleGameDifficulty.NORMAL.toString();
            } else if ( args[2].equalsIgnoreCase("hard") ) {
                kind = "hard";
                kindDisplayString = SingleGameDifficulty.HARD.toString();
            }
        }

        // ランキングデータの取得とソート
        ArrayList<PlayerScoreData> datas = PlayerScoreData.getAllData();
        PlayerScoreData.sortBy(datas, kind, type);

        // 表示
        sender.sendMessage(Messages.get("RankingTitle",
                new String[]{"%type", "%kind"},
                new String[]{typeDisplayString, kindDisplayString}));

        boolean isRankin = false;

        for ( int index=0; index<10; index++ ) {

            if ( index >= datas.size() ) break;

            PlayerScoreData data = datas.get(index);

            boolean isSelf = data.getName().equals(sender.getName());
            if ( isSelf ) isRankin = true;

            String rank = (isSelf ? ChatColor.RED.toString() : "") +
                    String.format("%1$2d", (index + 1));
            String name = String.format("%1$-12s", data.getName());
            String played = "" + data.get(kind).getPlayed();
            String win = "" + data.get(kind).getWin();
            String lose = "" + data.get(kind).getLose();
            String ratio = String.format("%1$.2f", data.get(kind).getRatio());

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
                    String played = "" + data.get(kind).getPlayed();
                    String win = "" + data.get(kind).getWin();
                    String lose = "" + data.get(kind).getLose();
                    String ratio = String.format("%1$.2f", data.get(kind).getRatio());

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
        ReversiLabConfig config = parent.getReversiLabConfig();
        config.reload();
        Messages.reload(config.getLang());

        if ( config.getBetRewardType() == BetRewardType.ECO
                && parent.getVaultEco() == null ) {
            config.setBetRewardType(BetRewardType.NONE);
        }

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

    /**
     * 指定したプレイヤーが指定したアイテムを十分な個数持っているかどうか確認する
     * @param player プレイヤー
     * @param item アイテム
     * @return 持っているかどうか
     */
    private boolean hasItem(Player player, ItemStack item) {
        //return player.getInventory().contains(item.getType(), item.getAmount());
        // ↑のコードは、アイテムのデータ値を検査しないのでNG

        int total = 0;
        for ( ItemStack i : player.getInventory().getContents() ) {
            if ( i != null && i.getType() == item.getType()
                    && i.getDurability() == item.getDurability() ) {
                total += i.getAmount();
                if ( total >= item.getAmount() ) return true;
            }
        }
        return false;
    }

    /**
     * 指定したプレイヤーから指定したアイテムを回収する
     * @param player プレイヤー
     * @param item アイテム
     * @return 回収に成功したかどうか
     */
    @SuppressWarnings("deprecation")
    private boolean consumeItem(Player player, ItemStack item) {
        Inventory inv = player.getInventory();
        int remain = item.getAmount();
        for ( int index=0; index<inv.getSize(); index++ ) {
            ItemStack i = inv.getItem(index);
            if ( i == null || i.getType() != item.getType()
                    || i.getDurability() != item.getDurability() ) {
                continue;
            }

            if ( i.getAmount() >= remain ) {
                if ( i.getAmount() == remain ) {
                    inv.clear(index);
                } else {
                    i.setAmount(i.getAmount() - remain);
                    inv.setItem(index, i);
                }
                remain = 0;
                break;
            } else {
                remain -= i.getAmount();
                inv.clear(index);
            }
        }
        player.updateInventory();
        return (remain <= 0);
    }
}
