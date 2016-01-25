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
public class GameBoard implements Cloneable {

    private Piece[][] board;

    /**
     * コンストラクタ
     */
    public GameBoard() {
        board = new Piece[8][8];
        for ( int y=0; y<8; y++ ) {
            for ( int x=0; x<8; x++ ) {
                board[y][x] = Piece.EMPTY;
            }
        }
        board[3][3] = Piece.BLACK;
        board[3][4] = Piece.WHITE;
        board[4][3] = Piece.WHITE;
        board[4][4] = Piece.BLACK;
    }

    /**
     * このボードのクローンを作成して返す
     * @see java.lang.Cloneable#clone()
     */
    public GameBoard clone() {
        GameBoard clone = new GameBoard();
        for ( int y=0; y<board.length; y++ ) {
            for ( int x=0; x<board[y].length; x++ ) {
                clone.board[y][x] = board[y][x];
            }
        }
        return clone;
    }

    /**
     * 指定された座標のピースを取得する
     * @param x
     * @param y
     * @return ピース
     */
    public Piece getPieceAt(int x, int y) {
        return board[y][x];
    }

    /**
     * 何も置かれていないマス目の個数を返す
     * @return 何も置かれていないマス目の個数
     */
    public int getEmptyCount() {
        return getCountOf(Piece.EMPTY);
    }

    /**
     * 黒が置かれているマス目の個数を返す
     * @return 黒が置かれているマス目の個数
     */
    public int getBlackCount() {
        return getCountOf(Piece.BLACK);
    }

    /**
     * 白が置かれているマス目の個数を返す
     * @return 白が置かれているマス目の個数
     */
    public int getWhiteCount() {
        return getCountOf(Piece.WHITE);
    }

    /**
     * 指定したマス目に石を置いた場合、裏返すことができる石の座標を調べて返す。
     * @param x マス目のx座標
     * @param y マス目のy座標
     * @param piece 置く石
     * @return 裏返される石の座標
     */
    public ArrayList<int[]> findPath(int x, int y, Piece piece) {
        ArrayList<int[]> results = new ArrayList<int[]>();
        if ( board[y][x] != Piece.EMPTY ) {
            return results;
        }
        results.addAll(findPath(x, y, piece, -1, -1));
        results.addAll(findPath(x, y, piece, -1, 0));
        results.addAll(findPath(x, y, piece, -1, 1));
        results.addAll(findPath(x, y, piece, 0, 1));
        results.addAll(findPath(x, y, piece, 1, 1));
        results.addAll(findPath(x, y, piece, 1, 0));
        results.addAll(findPath(x, y, piece, 1, -1));
        results.addAll(findPath(x, y, piece, 0, -1));
        return results;
    }

    /**
     * 指定された座標に石を置くことができるかどうかを調べて返す。
     * @param x マス目のx座標
     * @param y マス目のy座標
     * @param piece 置く石
     * @return 石を置くことができるかどうか
     */
    public boolean canPutAt(int x, int y, Piece piece) {
        if ( board[y][x] != Piece.EMPTY ) {
            return false;
        }
        return findPath(x, y, piece).size() > 0;
    }

    /**
     * 盤上のどこかに石を置くことができるかどうかを調べて返す。
     * @return 石を置くことができるかどうか
     */
    public boolean canPut(Piece piece) {
        for ( int y=0; y<8; y++ ) {
            for ( int x=0; x<8; x++ ) {
                if ( canPutAt(x, y, piece) ) {
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
        return canPut(Piece.BLACK) || canPut(Piece.WHITE);
    }

    /**
     * 指定された座標に石を置く。
     * @param x マス目のx座標
     * @param y マス目のy座標
     * @param piece 置く石
     * @return 裏返された石の座標
     */
    public ArrayList<int[]> putAt(int x, int y, Piece piece) {
        if ( !canPutAt(x, y, piece) ) {
            return new ArrayList<int[]>();
        }
        ArrayList<int[]> reverse = findPath(x, y, piece);
        board[y][x] = piece;
        for ( int[] coordinate : reverse ) {
            board[coordinate[1]][coordinate[0]] = piece;
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
        for ( Piece[] line : board ) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("|");
            for ( Piece cell : line ) {
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
     * @param piece 状態
     * @return 個数
     */
    public int getCountOf(Piece piece) {
        int count = 0;
        for ( Piece[] line : board ) {
            for ( Piece cell : line ) {
                if ( cell == piece ) {
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
     * @param piece 置く石
     * @param xDirection 調査する方向のx座標
     * @param yDirection 調査する方向のy座標
     * @return 裏返される石の座標
     */
    private ArrayList<int[]> findPath(int x, int y, Piece piece, int xDirection, int yDirection) {

        int xCursole = x + xDirection;
        int yCursole = y + yDirection;
        ArrayList<Piece> line = new ArrayList<Piece>();
        ArrayList<int[]> coordinates = new ArrayList<int[]>();

        while ( 0 <= xCursole && xCursole < 8 && 0 <= yCursole && yCursole < 8
                && board[yCursole][xCursole] != Piece.EMPTY ) {
            line.add(board[yCursole][xCursole]);
            coordinates.add(new int[]{xCursole, yCursole});
            xCursole += xDirection;
            yCursole += yDirection;
        }

        if ( coordinates.size() <= 1 ) {
            return new ArrayList<int[]>();
        }

        int index = 0;
        while ( index < line.size() && line.get(index).isReverseOf(piece) ) {
            index++;
        }
        if ( 0 < index && index < line.size() && line.get(index) == piece ) {
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
        System.out.println("黒を (3, 5) に置けるかどうか: " + board.canPutAt(3, 5, Piece.BLACK));
        System.out.println("黒を (3, 5) に置く。");
        board.putAt(3, 5, Piece.BLACK);
        board.debugPrint();
        System.out.println("白を (3, 5) に置けるかどうか: " + board.canPutAt(3, 5, Piece.WHITE));
        System.out.println("白を (5, 3) に置けるかどうか: " + board.canPutAt(5, 3, Piece.WHITE));
        System.out.println("白を (2, 3) に置けるかどうか: " + board.canPutAt(2, 3, Piece.WHITE));
        System.out.println("白を (2, 3) に置く。");
        board.putAt(2, 3, Piece.WHITE);
        board.debugPrint();
    }
}
