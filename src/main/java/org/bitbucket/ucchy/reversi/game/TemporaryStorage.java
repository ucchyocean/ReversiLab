/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.reversi.game;

import java.util.HashMap;

import org.bitbucket.ucchy.reversi.Utility;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * テンポラリストレージ
 * @author ucchy
 */
public class TemporaryStorage {

    private HashMap<String, Inventory> invs;
    private HashMap<String, Inventory> armors;
    private HashMap<String, Integer> levels;
    private HashMap<String, Float> exps;

    /**
     * コンストラクタ
     */
    public TemporaryStorage() {

        invs = new HashMap<String, Inventory>();
        armors = new HashMap<String, Inventory>();
        levels = new HashMap<String, Integer>();
        exps = new HashMap<String, Float>();
    }

    /**
     * プレイヤーのインベントリと経験値を、テンポラリ保存領域に保存する
     * @param name プレイヤー名
     */
    public void sendToTemp(String name) {
        Player player = Utility.getPlayerExact(name);
        if ( player != null ) {
            sendToTemp(player);
        }
    }

    /**
     * プレイヤーのインベントリと経験値を、テンポラリ保存領域に保存する
     * @param player プレイヤー
     */
    public void sendToTemp(Player player) {

        // インベントリの保存
        Inventory tempInventory = Bukkit.createInventory(player, 6 * 9);
        for ( ItemStack item : player.getInventory().getContents() ) {
            if ( item != null ) {
                tempInventory.addItem(item);
            }
        }
        invs.put(player.getName(), tempInventory);

        // 防具の保存
        Inventory tempArmors = Bukkit.createInventory(player, 9);
        for ( int index=0; index<4; index++ ) {
            ItemStack armor = player.getInventory().getArmorContents()[index];
            if ( armor != null ) {
                tempArmors.setItem(index, armor);
            }
        }
        armors.put(player.getName(), tempArmors);

        // インベントリの消去とアップデート
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[]{
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
        });
        updateInventory(player);

        // 経験値の保存と消去
        levels.put(player.getName(), player.getLevel());
        exps.put(player.getName(), player.getExp());
        player.setLevel(0);
        player.setExp(0);
    }

    /**
     * テンポラリ領域に保存していたインベントリや経験値を復帰する
     * @param name プレイヤー名
     */
    public void restoreFromTemp(String name) {
        Player player = Utility.getPlayerExact(name);
        if ( player != null ) {
            restoreFromTemp(player);
        }
    }

    /**
     * テンポラリ領域に保存していたインベントリや経験値を復帰する
     * @param player プレイヤー
     */
    public void restoreFromTemp(Player player) {

        // データが無いなら何もしない
        if ( !invs.containsKey(player.getName()) ) {
            return;
        }

        // インベントリの消去
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[]{
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
                new ItemStack(Material.AIR),
        });

        // インベントリと防具の復帰、更新
        for ( ItemStack item : invs.get(player.getName()).getContents() ) {
            if ( item != null ) {
                player.getInventory().addItem(item);
            }
        }
        ItemStack[] armorCont = new ItemStack[4];
        for ( int index=0; index<4; index++ ) {
            ItemStack armor = armors.get(player.getName()).getItem(index);
            if ( armor != null ) {
                armorCont[index] = armor;
            } else {
                armorCont[index] = new ItemStack(Material.AIR);
            }
            player.getInventory().setArmorContents(armorCont);
        }
        updateInventory(player);

        // レベルと経験値の復帰
        player.setLevel(levels.get(player.getName()));
        player.setExp(exps.get(player.getName()));

        // テンポラリの消去
        invs.remove(player.getName());
        armors.remove(player.getName());
        levels.remove(player.getName());
        exps.remove(player.getName());
    }

    /**
     * 現在の状況とテンポラリの状況を入れ替える
     * @param player プレイヤー
     */
    public void switchWithTemp(Player player) {

        // データが無いなら何もしない
        if ( !invs.containsKey(player.getName()) ) {
            return;
        }

        // インベントリの保存
        Inventory tempInventory = Bukkit.createInventory(player, 6 * 9);
        for ( ItemStack item : player.getInventory().getContents() ) {
            if ( item != null ) {
                tempInventory.addItem(item);
            }
        }

        // 防具の保存
        Inventory tempArmors = Bukkit.createInventory(player, 9);
        for ( int index=0; index<4; index++ ) {
            ItemStack armor = player.getInventory().getArmorContents()[index];
            if ( armor != null ) {
                tempArmors.setItem(index, armor);
            }
        }

        // リストア
        restoreFromTemp(player);

        // 上書き保存
        invs.put(player.getName(), tempInventory);
        armors.put(player.getName(), tempArmors);
        levels.put(player.getName(), player.getLevel());
        exps.put(player.getName(), player.getExp());
        player.setLevel(0);
        player.setExp(0);
    }

    /**
     * 指定された名前のプレイヤーのtempストレージに、アイテムを追加する
     * @param name プレイヤー名
     * @param item アイテム
     */
    public void addItem(String name, ItemStack item) {

        // null なら何もしない
        if ( name == null || item == null ) {
            return;
        }

        // データが無いなら何もしない
        if ( !invs.containsKey(name) ) {
            return;
        }

        invs.get(name).addItem(item);
    }

    /**
     * 指定したプレイヤーのインベントリを預かっているかどうか
     * @param player プレイヤー
     * @return 預かっているかどうか
     */
    public boolean isInventoryExists(Player player) {
        return invs.containsKey(player.getName());
    }

    /**
     * インベントリのアップデートを行う
     * @param player 更新対象のプレイヤー
     */
    @SuppressWarnings("deprecation")
    private void updateInventory(Player player) {
        player.updateInventory();
    }
}
