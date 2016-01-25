/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bitbucket.ucchy.reversi.game.GameSession;
import org.bitbucket.ucchy.reversi.game.GameSessionManager;
import org.bitbucket.ucchy.reversi.game.PlayerMoveChecker;
import org.bitbucket.ucchy.reversi.ranking.PlayerScoreData;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * リバーシ プラグイン
 * @author ucchy
 */
public class ReversiLab extends JavaPlugin {

    protected static final String WORLD_NAME = "ReversiLab";

    private World world;
    private ReversiLabConfig config;
    private GameSessionManager gameSessionManager;
    private ReversiLabCommand command;
    private PlayerMoveChecker checker;

    /**
     * プラグインが有効化された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        // サーバーのバージョンが v1.7.10 以前なら、プラグインを停止して動作しない。
        if ( !Utility.isCB180orLater() ) {
            getLogger().severe("This plugin cannot run at old version Bukkit server. Please use Bukkit 1.8 or later.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // ワールドのロード
        world = getServer().getWorld(WORLD_NAME);
        if ( world == null ) {
            world = createWorld();
        }

        // コンフィグのロード
        config = new ReversiLabConfig();

        // メッセージをロードする
        Messages.initialize(getFile(), getDataFolder(), config.getLang());
        Messages.reload(config.getLang());

        // マネージャの作成
        gameSessionManager = new GameSessionManager(this);

        // コマンドの準備
        command = new ReversiLabCommand(this);

        // リスナーの登録
        getServer().getPluginManager().registerEvents(new ReversiLabListener(this), this);

        // チェッカーの起動
        checker = new PlayerMoveChecker();
        checker.start(this);

        // ランキングデータのロード
        PlayerScoreData.initCache(new File(getDataFolder(), "ranking"));
    }

    /**
     * プラグインが無効化された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {

        // ゲーム中のセッションがある場合、全てキャンセルする
        ArrayList<GameSession> sessions = new ArrayList<GameSession>(gameSessionManager.getAllSessions());
        for ( GameSession session : sessions ) {
            if ( !session.isEnd() ) {
                session.runCancel();
            }
        }

        // チェッカーの動作を停止する
        if ( checker != null ) {
            checker.cancel();
        }
    }

    /**
     * プラグインのコマンドが実行された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return this.command.onCommand(sender, command, label, args);
    }

    /**
     * プラグインのコマンドでTABキー補完された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return this.command.onTabComplete(sender, command, alias, args);
    }

    /**
     * プラグイン用のワールドを生成する
     * @return
     */
    private World createWorld() {

        WorldCreator creator = new WorldCreator(WORLD_NAME);

        // Nullチャンクジェネレータを設定し、からっぽの世界が生成されるようにする
        creator.generator(new ChunkGenerator() {
            public byte[][] generateBlockSections(
                    World world, Random r,
                    int x, int z, ChunkGenerator.BiomeGrid biomes) {
                return new byte[256 / 16][];
            }
        });

        World world = getServer().createWorld(creator);

        // ずっと昼にする
        world.setTime(6000);
        world.setGameRuleValue("doDaylightCycle", "false");

        // MOBが沸かないようにする
        world.setGameRuleValue("doMobSpawning", "false");

        // 天候を晴れにする
        world.setStorm(false);
        world.setThundering(false);

        // ピースフルにする
        world.setDifficulty(Difficulty.PEACEFUL);

        return world;
    }

    /**
     * ReversiLabのコンフィグデータを取得する
     * @return
     */
    public ReversiLabConfig getReversiLabConfig() {
        return config;
    }

    /**
     * プラグイン用のワールドを取得する
     * @return プラグイン用のワールド
     */
    public World getWorld() {
        return world;
    }

    /**
     * ゲームセッションマネージャを取得する
     * @return ゲームセッションマネージャ
     */
    public GameSessionManager getGameSessionManager() {
        return gameSessionManager;
    }

    /**
     * このプラグインのJarファイルを返す
     * @return
     */
    protected File getJarFile() {
        return getFile();
    }

    /**
     * このプラグインのインスタンスを返す
     * @return プラグインのインスタンス
     */
    public static ReversiLab getInstance() {
        return (ReversiLab)Bukkit.getPluginManager().getPlugin("ReversiLab");
    }
}
