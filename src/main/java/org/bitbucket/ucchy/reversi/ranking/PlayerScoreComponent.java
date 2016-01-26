/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi.ranking;

import org.bukkit.configuration.ConfigurationSection;

/**
 * スコアコンポーネント
 * @author ucchy
 */
public class PlayerScoreComponent {

    private int played;
    private int win;
    private int lose;
    private int draw;

    protected PlayerScoreComponent() {
        played = 0;
        win = 0;
        lose = 0;
        draw = 0;
    }

    public int getPlayed() {
        return played;
    }

    public int getWin() {
        return win;
    }

    public int getLose() {
        return lose;
    }

    public int getDraw() {
        return draw;
    }

    public double getRatio() {
        if ( win + lose == 0 ) return 0;
        return (double)win / (double)(win + lose);
    }

    public void incrementPlayed() {
        this.played++;
    }

    public void incrementWin() {
        this.win++;
    }

    public void incrementLose() {
        this.lose++;
    }

    public void incrementDraw() {
        this.draw++;
    }

    protected static PlayerScoreComponent loadFromSection(ConfigurationSection section) {
        PlayerScoreComponent component = new PlayerScoreComponent();
        if ( section == null ) return component;
        component.played = section.getInt("played", 0);
        component.win = section.getInt("win", 0);
        component.lose = section.getInt("lose", 0);
        component.draw = section.getInt("draw", 0);
        return component;
    }

    protected void saveToSection(ConfigurationSection section) {
        section.set("played", played);
        section.set("win", win);
        section.set("lose", lose);
        section.set("draw", draw);
    }
}
