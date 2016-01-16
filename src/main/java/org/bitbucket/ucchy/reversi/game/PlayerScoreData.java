/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.reversi.game;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;

import org.bitbucket.ucchy.reversi.Utility;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 * ランキング用のプレイヤースコアデータ
 * @author ucchy
 */
public class PlayerScoreData {

    private static File saveFolder;

    private static HashMap<UUID, PlayerScoreData> cache;

    private File file;

    /** プレイヤー名 */
    private String name;

    /** プレイヤーID */
    private UUID id;

    // 各種統計情報
    private int gamePlayed;
    private int gameWin;
    private int gameLose;
    private int gameDraw;

    /**
     * コンストラクタ。
     */
    private PlayerScoreData() {
    }

    /**
     * コンストラクタ。
     * @param player プレイヤー
     */
    private PlayerScoreData(OfflinePlayer player) {
        this.name = player.getName();
        this.id = player.getUniqueId();
        this.gamePlayed = 0;
        this.gameWin = 0;
        this.gameLose = 0;
        this.gameDraw = 0;
    }

    /**
     * このオブジェクトを保存する
     */
    public void save() {

        if ( file == null ) {
            file = new File(saveFolder, id.toString() + ".yml");
        }

        // この時点で、nameを更新しておく
        Player player = Bukkit.getPlayer(id);
        if ( player != null ) name = player.getName();

        YamlConfiguration config = new YamlConfiguration();
        config.set("name", name);
        config.set("gamePlayed", gamePlayed);
        config.set("gameWin", gameWin);
        config.set("gameLose", gameLose);
        config.set("gameDraw", gameDraw);

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * ファイルからロードする
     * @param file ファイル
     * @return ロードされたスコアデータ
     */
    private static PlayerScoreData load(File file) {

        String idstr = file.getName().substring(0, file.getName().length() - 4);
        if ( !Utility.isUUID(idstr) ) return null;

        PlayerScoreData data = new PlayerScoreData();
        data.id = UUID.fromString(idstr);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        data.name = config.getString("name", "xxx");
        data.gamePlayed = config.getInt("gamePlayed", 0);
        data.gameWin = config.getInt("gameWin", 0);
        data.gameLose = config.getInt("gameLose", 0);
        data.gameDraw = config.getInt("gameDraw", 0);

        return data;
    }

    /**
     * 全データを再読み込みして、キャッシュを初期化する。
     */
    public static void initCache(File saveFolder) {

        PlayerScoreData.saveFolder = saveFolder;
        if ( !saveFolder.exists() ) saveFolder.mkdirs();

        cache = new HashMap<UUID, PlayerScoreData>();
        for ( PlayerScoreData data : getAllData() ) {
            cache.put(data.id, data);
        }
    }

    /**
     * プレイヤーに対応したユーザーデータを取得する
     * @param id プレイヤーID
     * @return PlayerScoreData
     */
    public static PlayerScoreData getData(UUID id) {

        if ( id == null ) {
            return null;
        }

        if ( cache.containsKey(id) ) {
            return cache.get(id);
        }

        String filename = id.toString() + ".yml";
        File file = new File(saveFolder, filename);
        if ( !file.exists() ) {
            OfflinePlayer player = Bukkit.getPlayer(id);
            if ( player == null ) player = Bukkit.getOfflinePlayer(id);
            if ( player == null ) return null;
            cache.put(id, new PlayerScoreData(player));
            return cache.get(id);
        }

        PlayerScoreData data = load(file);
        cache.put(id, data);
        return cache.get(id);
    }

    /**
     * プレイヤーに対応したユーザーデータを取得する
     * @param name プレイヤー名
     * @return PlayerScoreData
     */
    public static PlayerScoreData getData(String name) {

        if ( name == null ) return null;
        OfflinePlayer player = Utility.getOfflinePlayer(name);
        if ( player == null ) return null;
        return getData(player.getUniqueId());
    }

    /**
     * 全てのユーザーデータをまとめて返す。
     * @return 全てのユーザーデータ。
     */
    public static ArrayList<PlayerScoreData> getAllData() {

        if ( cache != null && cache.size() > 0 ) {
            return new ArrayList<PlayerScoreData>(cache.values());
        }

        String[] filelist = saveFolder.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });

        ArrayList<PlayerScoreData> results = new ArrayList<PlayerScoreData>();
        for ( String f : filelist ) {
            String id = f.substring(0, f.indexOf(".") );
            results.add(getData(UUID.fromString(id)));
        }

        return results;
    }

    public String getName() {
        return name;
    }

    public int getGamePlayed() {
        return gamePlayed;
    }

    public int getGameWin() {
        return gameWin;
    }

    public int getGameLose() {
        return gameLose;
    }

    public int getGameDraw() {
        return gameDraw;
    }

    public double getRatio() {
        return (double)gameWin / (double)(gameWin + gameLose);
    }

    public void increaseGamePlayed() {
        this.gamePlayed++;
    }

    public void increaseGameWin() {
        this.gameWin++;
    }

    public void increaseGameLose() {
        this.gameLose++;
    }

    public void increaseGameDraw() {
        this.gameDraw++;
    }

    /**
     * ArrayList&lt;PlayerScoreData&gt; 型の配列を、プレイ回数降順にソートする。
     * @param data ソート対象の配列
     */
    public static void sortByGamePlayed(ArrayList<PlayerScoreData> data) {

        Collections.sort(data, new Comparator<PlayerScoreData>() {
            public int compare(PlayerScoreData ent1, PlayerScoreData ent2) {
                return ent2.gamePlayed - ent1.gamePlayed;
            }
        });
    }

    /**
     * ArrayList&lt;PlayerScoreData&gt; 型の配列を、勝利回数降順にソートする。
     * @param data ソート対象の配列
     */
    public static void sortByGameWin(ArrayList<PlayerScoreData> data) {

        Collections.sort(data, new Comparator<PlayerScoreData>() {
            public int compare(PlayerScoreData ent1, PlayerScoreData ent2) {
                return ent2.gameWin - ent1.gameWin;
            }
        });
    }

    /**
     * ArrayList&lt;PlayerScoreData&gt; 型の配列を、敗北回数降順にソートする。
     * @param data ソート対象の配列
     */
    public static void sortByGameLose(ArrayList<PlayerScoreData> data) {

        Collections.sort(data, new Comparator<PlayerScoreData>() {
            public int compare(PlayerScoreData ent1, PlayerScoreData ent2) {
                return ent2.gameLose - ent1.gameLose;
            }
        });
    }

    /**
     * ArrayList&lt;PlayerScoreData&gt; 型の配列を、勝率の降順にソートする。
     * @param data ソート対象の配列
     */
    public static void sortByRatio(ArrayList<PlayerScoreData> data) {

        Collections.sort(data, new Comparator<PlayerScoreData>() {
            public int compare(PlayerScoreData ent1, PlayerScoreData ent2) {
                if ( ent2.getRatio() > ent1.getRatio() ) return 1;
                if ( ent2.getRatio() < ent1.getRatio() ) return -1;
                return 0;
            }
        });
    }
}
