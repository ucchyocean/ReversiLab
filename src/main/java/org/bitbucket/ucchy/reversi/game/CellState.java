package org.bitbucket.ucchy.reversi.game;

/**
 * 板目の状況
 * @author ucchy
 */
public enum CellState {

    /** 何も置かれていない */
    EMPTY("  "),

    /** 黒石 */
    BLACK("●"),

    /** 白石 */
    WHITE("○");

    private String displayString;

    /**
     * コンストラクタ
     * @param displayString
     */
    CellState(String displayString) {
        this.displayString = displayString;
    }

    /**
     * 板の最終結果ログ用の文字列を返す
     * @return
     */
    public String toDisplayString() {
        return displayString;
    }

    /**
     * 指定された状態の裏返し状態かどうかを返す
     * @param state 状態
     * @return 裏返しかどうか
     */
    public boolean isReverseOf(CellState state) {
        return (this == BLACK && state == WHITE) || (this == WHITE && state == BLACK);
    }

    /**
     * 状態の裏返しを返す
     * @return 裏返し
     */
    public CellState getReverse() {
        if (this == BLACK) return WHITE;
        if (this == WHITE) return BLACK;
        return EMPTY;
    }
}
