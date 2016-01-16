/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi.game;

import java.util.ArrayList;

/**
 * ボード
 * @author ucchy
 */
public class GameBoard {

    private CellState[][] board;

    /**
     * コンストラクタ
     */
    public GameBoard() {
        board = new CellState[8][8];
        for ( int y=0; y<8; y++ ) {
            for ( int x=0; x<8; x++ ) {
                board[y][x] = CellState.EMPTY;
            }
        }
        board[3][3] = CellState.BLACK;
        board[3][4] = CellState.WHITE;
        board[4][3] = CellState.WHITE;
        board[4][4] = CellState.BLACK;
    }

    /**
     * 何も置かれていないマス目の個数を返す
     * @return 何も置かれていないマス目の個数
     */
    public int getEmptyCount() {
        return getCountOf(CellState.EMPTY);
    }

    /**
     * 黒が置かれているマス目の個数を返す
     * @return 黒が置かれているマス目の個数
     */
    public int getBlackCount() {
        return getCountOf(CellState.BLACK);
    }

    /**
     * 白が置かれているマス目の個数を返す
     * @return 白が置かれているマス目の個数
     */
    public int getWhiteCount() {
        return getCountOf(CellState.WHITE);
    }

    /**
     * 指定したマス目に石を置いた場合、裏返すことができる石の座標を調べて返す。
     * @param x マス目のx座標
     * @param y マス目のy座標
     * @param state 置く石
     * @return 裏返される石の座標
     */
    public ArrayList<int[]> findPath(int x, int y, CellState state) {
        ArrayList<int[]> results = new ArrayList<int[]>();
        results.addAll(findPath(x, y, state, -1, -1));
        results.addAll(findPath(x, y, state, -1, 0));
        results.addAll(findPath(x, y, state, -1, 1));
        results.addAll(findPath(x, y, state, 0, 1));
        results.addAll(findPath(x, y, state, 1, 1));
        results.addAll(findPath(x, y, state, 1, 0));
        results.addAll(findPath(x, y, state, 1, -1));
        results.addAll(findPath(x, y, state, 0, -1));
        return results;
    }

    /**
     * 指定された座標に石を置くことができるかどうかを調べて返す。
     * @param x マス目のx座標
     * @param y マス目のy座標
     * @param state 置く石
     * @return 石を置くことができるかどうか
     */
    public boolean canPutAt(int x, int y, CellState state) {
        if ( board[y][x] != CellState.EMPTY ) {
            return false;
        }
        return findPath(x, y, state).size() > 0;
    }

    /**
     * 盤上のどこかに石を置くことができるかどうかを調べて返す。
     * @return 石を置くことができるかどうか
     */
    public boolean canPut(CellState state) {
        for ( int y=0; y<8; y++ ) {
            for ( int x=0; x<8; x++ ) {
                if ( canPutAt(x, y, state) ) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 黒と白、両者とも置けなくなったかどうか（決着がついたかどうか）を調べて返す。
     * @return 決着がついていない=true、決着がついた=false
     */
    public boolean canPutAll() {
        return canPut(CellState.BLACK) || canPut(CellState.WHITE);
    }

    /**
     * 指定された座標に石を置く。
     * @param x マス目のx座標
     * @param y マス目のy座標
     * @param state 置く石
     * @return 裏返された石の座標
     */
    public ArrayList<int[]> putAt(int x, int y, CellState state) {
        if ( !canPutAt(x, y, state) ) {
            return new ArrayList<int[]>();
        }
        ArrayList<int[]> reverse = findPath(x, y, state);
        board[y][x] = state;
        for ( int[] coordinate : reverse ) {
            board[coordinate[1]][coordinate[0]] = state;
        }
        return reverse;
    }

    /**
     * ログ記録やデバッグ出力のための、盤面状況の文字表現を取得する。
     * @return 盤面
     */
    public ArrayList<String> getStringForPrint() {

        ArrayList<String> field = new ArrayList<String>();
        field.add("+----------------+");
        for ( CellState[] line : board ) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("|");
            for ( CellState cell : line ) {
                buffer.append(cell.toDisplayString());
            }
            buffer.append("|");
            field.add(buffer.toString());
        }
        field.add("+----------------+");

        return field;
    }

    /**
     * 指定された状態のマス目の個数を数える
     * @param state 状態
     * @return 個数
     */
    private int getCountOf(CellState state) {
        int count = 0;
        for ( CellState[] line : board ) {
            for ( CellState cell : line ) {
                if ( cell == state ) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * 指定したマス目に石を置いた場合、指定された方向について、裏返すことができる石の座標を調べて返す。
     * @param x マス目のx座標
     * @param y マス目のy座標
     * @param state 置く石
     * @param xDirection 調査する方向のx座標
     * @param yDirection 調査する方向のy座標
     * @return 裏返される石の座標
     */
    private ArrayList<int[]> findPath(int x, int y, CellState state, int xDirection, int yDirection) {

        int xCursole = x + xDirection;
        int yCursole = y + yDirection;
        ArrayList<CellState> line = new ArrayList<CellState>();
        ArrayList<int[]> coordinates = new ArrayList<int[]>();

        while ( 0 <= xCursole && xCursole < 8 && 0 <= yCursole && yCursole < 8
                && board[yCursole][xCursole] != CellState.EMPTY ) {
            line.add(board[yCursole][xCursole]);
            coordinates.add(new int[]{xCursole, yCursole});
            xCursole += xDirection;
            yCursole += yDirection;
        }

        if ( coordinates.size() <= 1 ) {
            return new ArrayList<int[]>();
        }

        int index = 0;
        while ( index < line.size() && line.get(index).isReverseOf(state) ) {
            index++;
        }
        if ( 0 < index && index < line.size() && line.get(index) == state ) {
            ArrayList<int[]> results = new ArrayList<int[]>();
            for ( int i=0; i<index; i++ ) {
                results.add(coordinates.get(i));
            }
            return results;
        }

        return new ArrayList<int[]>();
    }

    /**
     * 盤面をデバッグ出力する。
     */
    private void debugPrint() {
        for ( String line : getStringForPrint() ) {
            System.out.println(line);
        }
    }

    // デバッグ用エントリ
    public static void main(String[] args) {

        GameBoard board = new GameBoard();
        board.debugPrint();
        System.out.println("黒を (3, 5) に置けるかどうか: " + board.canPutAt(3, 5, CellState.BLACK));
        System.out.println("黒を (3, 5) に置く。");
        board.putAt(3, 5, CellState.BLACK);
        board.debugPrint();
        System.out.println("白を (3, 5) に置けるかどうか: " + board.canPutAt(3, 5, CellState.WHITE));
        System.out.println("白を (5, 3) に置けるかどうか: " + board.canPutAt(5, 3, CellState.WHITE));
        System.out.println("白を (2, 3) に置けるかどうか: " + board.canPutAt(2, 3, CellState.WHITE));
        System.out.println("白を (2, 3) に置く。");
        board.putAt(2, 3, CellState.WHITE);
        board.debugPrint();
    }
}
