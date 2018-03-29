package top.defaults.audio;

import android.annotation.SuppressLint;
import android.util.Log;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 日志类
 *
 * @author fosafer
 * @since Nov 1, 2014
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@SuppressLint("LogNotTimber")
public class Logger {

    private static final String DEFAULT_LOG_TAG = "AWLogger";
    private static String logTag = DEFAULT_LOG_TAG;
    private static int logLevel = Log.VERBOSE;
    private static String logFilePath = null;
    private static int logFileSizeInMegabytes = 2;
    private static final String logBrotherSuffix = "_1";
    private static boolean isLoggingToBrother = false;
    
    /**
     * 设置日志级别
     * 
     * @param logLevel 日志级别
     */
    public static void setLogLevel(int logLevel) {
        Logger.logLevel = logLevel;
    }

    public static int getLogLevel() {
        return logLevel;
    }

    public static void setLogTag(String newLogTag) {
        logTag = newLogTag;
    }

    /**
     * 设置日志文件路径，为防止日志文件过大，内部采用两个文件来回写，另一个文件加上后缀"_1"
     * 
     * @param filePath 文件路径
     */
    public static void setLogFile(String filePath) {
        logFilePath = filePath;
    }
    
    /**
     * 设置日志文件最大尺寸，单位兆字节
     * 
     * @param sizeInMegabyte 日志文件大小
     */
    public static void setLogFileMaxSizeInMegabyte(int sizeInMegabyte) {
        logFileSizeInMegabytes = sizeInMegabyte;
    }
    
    public static void logV(String logMessage) {
        logV(logTag + "|" + getLineInfo(), logMessage);
    }

    public static void logD(String logMessage) {
        logD(logTag + "|" + getLineInfo(), logMessage);
    }

    public static void logI(String logMessage) {
        logI(logTag + "|" + getLineInfo(), logMessage);
    }

    public static void logW(String logMessage) {
        logW(logTag + "|" + getLineInfo(), logMessage);
    }

    public static void logE(String logMessage) {
        logE(logTag + "|" + getLineInfo(), logMessage);
    }

    public static void logV(String tag, String logMessage) {
        if (logLevel > Log.VERBOSE && !Log.isLoggable(logTag, Log.DEBUG))
            return;
        Log.v(tag, logMessage);
        writeLogFile(tag + "\t" + logMessage);
    }

    public static void logD(String tag, String logMessage) {
        if (logLevel > Log.DEBUG && !Log.isLoggable(logTag, Log.DEBUG))
            return;
        Log.d(tag, logMessage);
        writeLogFile(tag + "\t" + logMessage);
    }

    public static void logI(String tag, String logMessage) {
        if (logLevel > Log.INFO && !Log.isLoggable(logTag, Log.DEBUG))
            return;
        Log.i(tag, logMessage);
        writeLogFile(tag + "\t" + logMessage);
    }

    public static void logW(String tag, String logMessage) {
        if (logLevel > Log.WARN && !Log.isLoggable(logTag, Log.DEBUG))
            return;
        Log.w(tag, logMessage);
        writeLogFile(tag + "\t" + logMessage);
    }

    public static void logE(String tag, String logMessage) {
        if (logLevel > Log.ERROR && !Log.isLoggable(logTag, Log.DEBUG))
            return;
        Log.e(tag, logMessage);
        writeLogFile(tag + "\t" + logMessage);
    }

    private static void writeLogFile(String logMessage) {
        if (logFilePath != null) {
            String fileName = logFilePath + (isLoggingToBrother ? logBrotherSuffix : "");
            File currentLogFile = new File(fileName);
            if (currentLogFile.length() >= logFileSizeInMegabytes * 1024 * 1024) {
                isLoggingToBrother = !isLoggingToBrother;
                
                // delete file before write
                try {
                    PrintWriter writer = new PrintWriter(logFilePath + (isLoggingToBrother ? logBrotherSuffix : ""));
                    writer.print("");
                    writer.close();
                } catch (FileNotFoundException ignored) {
                }
            }
            try {
                BufferedWriter logOut = new BufferedWriter(new java.io.FileWriter(fileName, true));
                SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                String strDate = sdfDate.format(new Date());
                logOut.append(strDate).append("\t").append(logMessage).append("\n");
                logOut.close();
            } catch (IOException ignored) {
            }
        }
    }
    
    private static String getLineInfo() {
        StackTraceElement[] stackTraceElement = Thread.currentThread()
                .getStackTrace();

        String fileName = stackTraceElement[4].getFileName();
        int lineNumber = stackTraceElement[4].getLineNumber();
        return fileName + ":" + lineNumber;
    }
}
