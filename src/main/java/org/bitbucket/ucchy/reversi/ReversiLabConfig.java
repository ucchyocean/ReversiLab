/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi;

import java.io.File;
import java.util.List;

import org.bitbucket.ucchy.reversi.game.SingleGameDifficulty;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

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

    private BetRewardType betRewardType;

    private ItemStack versusBetItem;
    private ItemStack versusRewardItem;
    private ItemStack easyBetItem;
    private ItemStack easyRewardItem;
    private ItemStack normalBetItem;
    private ItemStack normalRewardItem;
    private ItemStack hardBetItem;
    private ItemStack hardRewardItem;

    private int versusBetEco;
    private int versusRewardEco;
    private int easyBetEco;
    private int easyRewardEco;
    private int normalBetEco;
    private int normalRewardEco;
    private int hardBetEco;
    private int hardRewardEco;

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

        betRewardType = BetRewardType.fromString(
                conf.getString("betRewardType"), BetRewardType.NONE);

        versusBetItem = getItemStack(conf.getString("versusBetItem"));
        versusRewardItem = getItemStack(conf.getString("versusRewardItem"));
        easyBetItem = getItemStack(conf.getString("easyBetItem"));
        easyRewardItem = getItemStack(conf.getString("easyRewardItem"));
        normalBetItem = getItemStack(conf.getString("normalBetItem"));
        normalRewardItem = getItemStack(conf.getString("normalRewardItem"));
        hardBetItem = getItemStack(conf.getString("hardBetItem"));
        hardRewardItem = getItemStack(conf.getString("hardRewardItem"));

        versusBetEco = conf.getInt("versusBetEco", 0);
        versusRewardEco = conf.getInt("versusRewardEco", 0);
        easyBetEco = conf.getInt("easyBetEco", 0);
        easyRewardEco = conf.getInt("easyRewardEco", 0);
        normalBetEco = conf.getInt("normalBetEco", 0);
        normalRewardEco = conf.getInt("normalRewardEco", 0);
        hardBetEco = conf.getInt("hardBetEco", 0);
        hardRewardEco = conf.getInt("hardRewardEco", 0);
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

    public BetRewardType getBetRewardType() {
        return betRewardType;
    }

    public ItemStack getVersusBetItem() {
        return versusBetItem;
    }

    public ItemStack getVersusRewardItem() {
        return versusRewardItem;
    }

    public ItemStack getEasyBetItem() {
        return easyBetItem;
    }

    public ItemStack getEasyRewardItem() {
        return easyRewardItem;
    }

    public ItemStack getNormalBetItem() {
        return normalBetItem;
    }

    public ItemStack getNormalRewardItem() {
        return normalRewardItem;
    }

    public ItemStack getHardBetItem() {
        return hardBetItem;
    }

    public ItemStack getHardRewardItem() {
        return hardRewardItem;
    }

    public int getVersusBetEco() {
        return versusBetEco;
    }

    public int getVersusRewardEco() {
        return versusRewardEco;
    }

    public int getEasyBetEco() {
        return easyBetEco;
    }

    public int getEasyRewardEco() {
        return easyRewardEco;
    }

    public int getNormalBetEco() {
        return normalBetEco;
    }

    public int getNormalRewardEco() {
        return normalRewardEco;
    }

    public int getHardBetEco() {
        return hardBetEco;
    }

    public int getHardRewardEco() {
        return hardRewardEco;
    }

    protected void setBetRewardType(BetRewardType betRewardType) {
        this.betRewardType = betRewardType;
    }

    public ItemStack getBetItem(SingleGameDifficulty difficulty) {
        switch ( difficulty ) {
        case EASY:
            return getEasyBetItem();
        case NORMAL:
            return getNormalBetItem();
        case HARD:
            return getHardBetItem();
        }
        return null;
    }

    public ItemStack getRewardItem(SingleGameDifficulty difficulty) {
        switch ( difficulty ) {
        case EASY:
            return getEasyRewardItem();
        case NORMAL:
            return getNormalRewardItem();
        case HARD:
            return getHardRewardItem();
        }
        return null;
    }

    public int getBetEco(SingleGameDifficulty difficulty) {
        switch ( difficulty ) {
        case EASY:
            return getEasyBetEco();
        case NORMAL:
            return getNormalBetEco();
        case HARD:
            return getHardBetEco();
        }
        return 0;
    }

    public int getRewardEco(SingleGameDifficulty difficulty) {
        switch ( difficulty ) {
        case EASY:
            return getEasyRewardEco();
        case NORMAL:
            return getNormalRewardEco();
        case HARD:
            return getHardRewardEco();
        }
        return 0;
    }

    private static ItemStack getItemStack(String str) {
        if ( str == null ) return null;
        String[] temp = str.split("-");
        Material material = Material.matchMaterial(temp[0]);
        if ( material == null ) return null;
        ItemStack item = new ItemStack(material);
        if ( temp.length <= 1 || !temp[1].matches("[0-9]+") ) return item;
        int amount = Integer.parseInt(temp[1]);
        item.setAmount(amount);
        return item;
    }
}
