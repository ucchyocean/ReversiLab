/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi;

/**
 * betやrewardの設定タイプ
 * @author ucchy
 */
public enum BetRewardType {

    /** 使用しない */
    NONE,

    /** アイテムを使用する */
    ITEM,

    /** 経済プラグインのお金を使用する */
    ECO;

    /**
     * 与えられた文字列から、該当するReversiLabConfigBetRewardTypeを返す。
     * @param str 文字列
     * @param def デフォルト値
     * @return ReversiLabConfigBetRewardType
     */
    public static BetRewardType fromString(
            String str, BetRewardType def) {

        if ( str == null ) return def;
        for ( BetRewardType type : values() ) {
            if ( type.name().equalsIgnoreCase(str) ) return type;
        }
        return def;
    }
}
