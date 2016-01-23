/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi;

import java.io.File;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * コンフィグクラス
 * @author ucchy
 */
public class ReversiLabConfig {

    /** メッセージの言語 */
    private String lang;

    /** 対戦の開始と終了を、サーバー全体メッセージで通知するかどうか */
    private boolean broadcastSessionStartEnd;

    /** ゲームの開始を禁止するワールド */
    private List<String> prohibitWorlds;

    /** ゲーム終了してから、テレポートして元の場所に戻るまでの、待ち時間（秒） */
    private int sessionEndWaitSeconds;

    /**
     * コンストラクタ
     */
    public ReversiLabConfig() {
        reload();
    }

    /**
     * このコンフィグを再読み込みする
     */
    public void reload() {

        ReversiLab parent = ReversiLab.getInstance();

        if ( !parent.getDataFolder().exists() ) {
            parent.getDataFolder().mkdirs();
        }

        // コンフィグファイルが無いなら生成する
        File file = new File(parent.getDataFolder(), "config.yml");
        if ( !file.exists() ) {
            if ( Utility.getDefaultLocaleLanguage().equals("ja") ) {
                Utility.copyFileFromJar(
                        parent.getJarFile(), file, "config_ja.yml", false);
            } else {
                Utility.copyFileFromJar(
                        parent.getJarFile(), file, "config.yml", false);
            }
        }

        parent.reloadConfig();
        FileConfiguration conf = parent.getConfig();

        // 読み込み
        lang = conf.getString("lang", Utility.getDefaultLocaleLanguage());
        broadcastSessionStartEnd = conf.getBoolean("broadcastSessionStartEnd", true);
        prohibitWorlds = conf.getStringList("prohibitWorlds");
        sessionEndWaitSeconds = conf.getInt("sessionEndWaitSeconds", 15);
    }

    public String getLang() {
        return lang;
    }

    public boolean isBroadcastSessionStartEnd() {
        return broadcastSessionStartEnd;
    }

    public List<String> getProhibitWorlds() {
        return prohibitWorlds;
    }

    public int getSessionEndWaitSeconds() {
        return sessionEndWaitSeconds;
    }
}
