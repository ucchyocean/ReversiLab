/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * コンフィグクラス
 * @author ucchy
 */
public class ReversiLabConfig {

    /** メッセージの言語 */
    private String lang;

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
        sessionEndWaitSeconds = conf.getInt("sessionEndWaitSeconds", 15);
    }

    public String getLang() {
        return lang;
    }

    public int getSessionEndWaitSeconds() {
        return sessionEndWaitSeconds;
    }
}
