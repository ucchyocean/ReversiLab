/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.reversi.ranking;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;

import org.bitbucket.ucchy.reversi.Utility;
import org.bitbucket.ucchy.reversi.game.SingleGameDifficulty;
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
    private PlayerScoreComponent versus;
    private PlayerScoreComponent easy;
    private PlayerScoreComponent normal;
    private PlayerScoreComponent hard;

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
        this.versus = new PlayerScoreComponent();
        this.easy = new PlayerScoreComponent();
        this.normal = new PlayerScoreComponent();
        this.hard = new PlayerScoreComponent();
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
        versus.saveToSection(config.createSection("versus"));
        easy.saveToSection(config.createSection("easy"));
        normal.saveToSection(config.createSection("normal"));
        hard.saveToSection(config.createSection("hard"));

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
        data.versus = PlayerScoreComponent.loadFromSection(config.getConfigurationSection("versus"));
        data.easy = PlayerScoreComponent.loadFromSection(config.getConfigurationSection("easy"));
        data.normal = PlayerScoreComponent.loadFromSection(config.getConfigurationSection("normal"));
        data.hard = PlayerScoreComponent.loadFromSection(config.getConfigurationSection("hard"));

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

    public PlayerScoreComponent getVersus() {
        return versus;
    }

    public PlayerScoreComponent getEasy() {
        return easy;
    }

    public PlayerScoreComponent getNormal() {
        return normal;
    }

    public PlayerScoreComponent getHard() {
        return hard;
    }

    public PlayerScoreComponent get(String name) {
        if ( name.equalsIgnoreCase("easy") ) {
            return easy;
        } else if ( name.equalsIgnoreCase("normal") ) {
            return normal;
        } else if ( name.equalsIgnoreCase("hard") ) {
            return hard;
        }
        return versus;
    }

    public PlayerScoreComponent get(SingleGameDifficulty difficulty) {
        switch ( difficulty ) {
        case EASY: return easy;
        case NORMAL: return normal;
        case HARD: return hard;
        }
        return null;
    }

    /**
     * ArrayList&lt;PlayerScoreData&gt; 型の配列を、プレイ回数降順にソートする。
     * @param data ソート対象の配列
     * @param name ソート対象のパラメータ(versus, easy, normal, hard)
     */
    public static void sortByGamePlayed(ArrayList<PlayerScoreData> data, final String name) {

        Collections.sort(data, new Comparator<PlayerScoreData>() {
            public int compare(PlayerScoreData ent1, PlayerScoreData ent2) {
                return ent2.get(name).getPlayed() - ent1.get(name).getPlayed();
            }
        });
    }

    /**
     * ArrayList&lt;PlayerScoreData&gt; 型の配列を、勝利回数降順にソートする。
     * @param data ソート対象の配列
     * @param name ソート対象のパラメータ(versus, easy, normal, hard)
     */
    public static void sortByGameWin(ArrayList<PlayerScoreData> data, final String name) {

        Collections.sort(data, new Comparator<PlayerScoreData>() {
            public int compare(PlayerScoreData ent1, PlayerScoreData ent2) {
                return ent2.get(name).getWin() - ent1.get(name).getWin();
            }
        });
    }

    /**
     * ArrayList&lt;PlayerScoreData&gt; 型の配列を、敗北回数降順にソートする。
     * @param data ソート対象の配列
     * @param name ソート対象のパラメータ(versus, easy, normal, hard)
     */
    public static void sortByGameLose(ArrayList<PlayerScoreData> data, final String name) {

        Collections.sort(data, new Comparator<PlayerScoreData>() {
            public int compare(PlayerScoreData ent1, PlayerScoreData ent2) {
                return ent2.get(name).getLose() - ent1.get(name).getLose();
            }
        });
    }

    /**
     * ArrayList&lt;PlayerScoreData&gt; 型の配列を、勝率の降順にソートする。
     * @param data ソート対象の配列
     * @param name ソート対象のパラメータ(versus, easy, normal, hard)
     */
    public static void sortByRatio(ArrayList<PlayerScoreData> data, final String name) {

        Collections.sort(data, new Comparator<PlayerScoreData>() {
            public int compare(PlayerScoreData ent1, PlayerScoreData ent2) {
                double ratio1 = ent1.get(name).getRatio();
                double ratio2 = ent2.get(name).getRatio();
                if ( ratio2 > ratio1 ) return 1;
                if ( ratio2 < ratio1 ) return -1;
                return 0;
            }
        });
    }

    public static void sortBy(ArrayList<PlayerScoreData> data, final String name, String type) {
        if ( type.equals("played") ) {
            sortByGamePlayed(data, name);
        } else if ( type.equals("lose") ) {
            sortByGameLose(data, name);
        } else if ( type.equals("ratio") ) {
            sortByRatio(data, name);
        } else {
            sortByGameWin(data, name);
        }
    }
}
