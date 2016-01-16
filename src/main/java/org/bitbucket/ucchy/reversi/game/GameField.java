/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi.game;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

/**
 * ゲームフィールド
 * @author ucchy
 */
public class GameField {

    private Location origin;
    private Location center;
    private ArmorStand[][] stands;

    /**
     * コンストラクタ
     * @param origin 基点となる座標
     */
    protected GameField(Location origin) {
        this.origin = origin;
        this.center = origin.clone().add(4, 0, 4);
        makeField();
    }

    /**
     * フィールドを生成する
     */
    private void makeField() {

        // クリーンアップ
        cleanup();

        int startx = origin.getBlockX();
        int startz = origin.getBlockZ();
        World world = origin.getWorld();

        // 草ブロックを生成
        for ( int x = startx; x < startx + 8; x++ ) {
            for ( int z = startz; z < startz + 8; z++ ) {
                Material material = (x + z) % 2 == 0 ? Material.GRASS : Material.MYCEL;
                world.getBlockAt(x, origin.getBlockY(), z).setType(material);
            }
        }

        // アーマースタンドを生成、中央に石を置く
        stands = new ArmorStand[8][8];
        putStone(3, 3, CellState.BLACK);
        putStone(3, 4, CellState.WHITE);
        putStone(4, 3, CellState.WHITE);
        putStone(4, 4, CellState.BLACK);
    }

    /**
     * 指定した座標に石を置く。
     * @param x マス目のx座標
     * @param y マス目のy座標
     * @param state 置く石
     */
    protected void putStone(int x, int y, CellState state) {

        if ( stands[y][x] == null ) {

            Location loc = new Location(origin.getWorld(),
                    origin.getBlockX() + x + 0.5,
                    origin.getBlockY(),
                    origin.getBlockZ() + y + 0.5);

            ArmorStand stand = (ArmorStand)origin.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            stand.setMarker(true);
            stand.setGravity(false);
            stand.setSmall(true);
            stands[y][x] = stand;
        }

        ArmorStand stand = stands[y][x];

        ItemStack item;
        if ( state == CellState.BLACK ) {
            item = new ItemStack(Material.NETHER_BRICK);
        } else {
            item = new ItemStack(Material.QUARTZ_BLOCK);
        }
        stand.setHelmet(item);

        // エフェクト発生
        Location effectLocation = stand.getLocation().add(0, 1.5, 0);
        int data = (state == CellState.BLACK) ? 49 : 42;
        stand.getWorld().playEffect(effectLocation, Effect.STEP_SOUND, data);
    }

    /**
     * 領域をクリーンアップする
     */
    protected void cleanup() {

        int startx = origin.getBlockX();
        int startz = origin.getBlockZ();
        World world = origin.getWorld();

        // 領域を全クリア
        for ( int x=startx-32; x<startx+32; x++ ) {
            for ( int z=startz-32; z<startz+32; z++ ) {
                for ( int y=255; y>=0; y-- ) {
                    if ( world.getBlockAt(x, y, z).getType() != Material.AIR ) {
                        world.getBlockAt(x, y, z).setType(Material.AIR);
                    }
                }
            }
        }
        for ( Entity entity : world.getEntities() ) {
            Location loc = entity.getLocation();
            if ( startx-32 <= loc.getBlockX() && loc.getBlockX() <= startx+32 &&
                    startz-32 <= loc.getBlockZ() && loc.getBlockZ() <= startz+32 ) {
                entity.remove();
            }
        }
    }

    /**
     * このゲームフィールドの基点を返す
     * @return フィールドの基点
     */
    protected Location getOrigin() {
        return origin;
    }

    /**
     * このゲームフィールドの再開地点を返す
     * @return
     */
    protected Location getCenterRespawnPoint() {
        return center.clone().add(0, 2, 0);
    }

    /**
     * 指定された地点は、ゲームフィールドの移動可能範囲外かどうかを返す
     * @param location 地点
     * @return 範囲外かどうか
     */
    protected boolean isOutOfSpectateField(Location location) {

        if ( !center.getWorld().getName().equals(location.getWorld().getName()) ) {
            return true;
        }
        double xDistanceSquared = (center.getX() - location.getX()) * (center.getX() - location.getX());
        double zDistanceSquared = (center.getZ() - location.getZ()) * (center.getZ() - location.getZ());
        if ( xDistanceSquared > 32 * 32 || zDistanceSquared > 32 * 32 ) {
            return true;
        }
        double height = location.getY() - center.getY();
        if ( height < -2 || 32 < height ) {
            return true;
        }
        return false;
    }
}
