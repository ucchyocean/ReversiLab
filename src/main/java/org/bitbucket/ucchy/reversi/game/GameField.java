/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi.game;

import org.bitbucket.ucchy.reversi.ReversiLab;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * ゲームフィールド
 * @author ucchy
 */
public class GameField {

    private Location origin;
    private Location center;

    private Location primaryPlayerLocation;
    private Location secondaryPlayerLocation;

    private ArmorStand[][] stands;

    /**
     * コンストラクタ
     * @param origin 基点となる座標
     */
    protected GameField(Location origin) {
        this.origin = origin;
        this.center = origin.clone().add(4, 0, 4);
        //makeField();
    }

    /**
     * フィールドを生成する
     */
    protected void makeField() {

        // クリーンアップ
        cleanup(true);

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
        putStone(3, 3, Piece.BLACK);
        putStone(3, 4, Piece.WHITE);
        putStone(4, 3, Piece.WHITE);
        putStone(4, 4, Piece.BLACK);

        // プレイヤーの開始位置を設定
        this.primaryPlayerLocation = origin.clone().add(4, 5, -1)
                .setDirection(new Vector(0, -5, 5).normalize());
        this.secondaryPlayerLocation = origin.clone().add(4, 5, 8)
                .setDirection(new Vector(0, -5, -5).normalize());
    }

    /**
     * 指定した座標に石を置く。
     * @param x マス目のx座標
     * @param y マス目のy座標
     * @param piece 置く石
     */
    protected void putStone(int x, int y, Piece piece) {

        if ( stands[y][x] == null ) {

            Location loc = new Location(origin.getWorld(),
                    origin.getBlockX() + x + 0.5,
                    origin.getBlockY() + 0.3,
                    origin.getBlockZ() + y + 0.5);

            ArmorStand stand = (ArmorStand)origin.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
            //stand.setMarker(true);
            stand.setGravity(false);
            stand.setSmall(true);
            stand.setVisible(false);
            stands[y][x] = stand;
        }

        ArmorStand stand = stands[y][x];

        ItemStack item = new ItemStack(Material.STEP);
        if ( piece == Piece.BLACK ) {
            item.setDurability((short) 6);
        } else {
            item.setDurability((short) 7);
        }
        stand.setHelmet(item);

        // エフェクト発生
        Location effectLocation = stand.getLocation().add(0, 1.2, 0);
        int data = (piece == Piece.BLACK) ? 49 : 42;
        stand.getWorld().playEffect(effectLocation, Effect.STEP_SOUND, data);
    }

    /**
     * 領域をクリーンアップする
     * @param cleanupEntities エンティティをクリアするかどうか
     */
    protected void cleanup(boolean cleanupEntities) {

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

        // エンティティをクリア
        if ( cleanupEntities ) {
            for ( Entity entity : world.getEntities() ) {
                Location loc = entity.getLocation();
                if ( startx-32 <= loc.getBlockX() && loc.getBlockX() <= startx+32 &&
                        startz-32 <= loc.getBlockZ() && loc.getBlockZ() <= startz+32 ) {
                    entity.remove();
                }
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
     * 第一プレイヤーの開始地点を返す
     * @return primaryPlayerLocation
     */
    protected Location getPrimaryPlayerLocation() {
        return primaryPlayerLocation;
    }

    /**
     * 第二プレイヤーの開始地点を返す
     * @return secondaryPlayerLocation
     */
    protected Location getSecondaryPlayerLocation() {
        return secondaryPlayerLocation;
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

    /**
     * 演出用の花火を打ち上げる
     */
    protected void spawnFireworks() {

        int duration = 20;

        new BukkitRunnable() {
            private int num = 3;

            public void run() {

                Location loc = center.clone().add(Math.random() * 4 - 2, 1, Math.random() * 4 - 2);

                Firework firework = (Firework)center.getWorld().spawnEntity(loc, EntityType.FIREWORK);
                FireworkMeta meta = firework.getFireworkMeta();
                FireworkEffect effect = FireworkEffect.builder()
                        .flicker(true)
                        .withColor(getRandomColor())
                        .withFade(getRandomColor())
                        .with(Type.BALL_LARGE)
                        .trail(true)
                        .build();
                meta.addEffect(effect);
                meta.setPower(1);
                firework.setFireworkMeta(meta);

                num--;
                if ( num <= 0 ) cancel();
            }
        }.runTaskTimer(ReversiLab.getInstance(), duration, duration);
    }

    private static Color getRandomColor() {
        int value = (int)(Math.random() * 17);
        switch (value) {
        case 0: return Color.AQUA;
        case 1: return Color.BLACK;
        case 2: return Color.BLUE;
        case 3: return Color.FUCHSIA;
        case 4: return Color.GRAY;
        case 5: return Color.GREEN;
        case 6: return Color.LIME;
        case 7: return Color.MAROON;
        case 8: return Color.NAVY;
        case 9: return Color.OLIVE;
        case 10: return Color.WHITE;
        case 11: return Color.ORANGE;
        case 12: return Color.PURPLE;
        case 13: return Color.RED;
        case 14: return Color.SILVER;
        case 15: return Color.TEAL;
        default: return Color.YELLOW;
        }
    }
}
