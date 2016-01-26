/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.reversi.game;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.ChatColor;

/**
 * ゲームセッションロガー
 * @author ucchy
 */
public class GameSessionLogger {

    private File file;
    private SimpleDateFormat lformat;

    /**
     * コンストラクタ
     * @param folder ログ出力フォルダ
     */
    public GameSessionLogger(File folder) {

        if ( !folder.exists() ) {
            folder.mkdirs();
        }

        SimpleDateFormat fndformat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String fileName = fndformat.format(new Date()) + "-log.txt";
        file = new File(folder, fileName);

        lformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * ログを出力する
     * @param message ログ内容
     */
    public void log(final String message) {

        String msg = ChatColor.stripColor(message).replace("\n", " ");
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file, true), "UTF-8"));
            String str = lformat.format(new Date()) + ", " + msg;
            writer.write(str);
            writer.newLine();
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if ( writer != null ) {
                try {
                    writer.close();
                } catch (Exception e) {
                    // do nothing.
                }
            }
        }
    }

    // デバッグエントリ
    public static void main(String[] args) {

        GameSessionLogger logger = new GameSessionLogger(new File("."));
        logger.log("てすと");
        logger.log("テストですよ");
    }
}
