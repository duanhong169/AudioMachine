package top.defaults.audio;

import java.util.Locale;

public class Error extends java.lang.Error {
    public static final int ERROR_DEFAULT_CODE = -1;
    public static final int ERROR_AUDIO = 3;
    public static final int ERROR_CLIENT = 5;
    public static final int ERROR_AUDIO_MACHINE_BUSY = 8;

    private int code;

    public Error(int code) {
        super(messageFor(code));
        this.code = code;
    }

    Error(int code, String message) {
        super(message);
        this.code = code;
    }

    private static String messageFor(int code) {
        String message;
        switch (code) {
            case ERROR_AUDIO:
                message = "音频错误";
                break;
            default:
                message = "未定义错误";
                break;
        }
        return message;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "%s(%d)", getMessage(), code);
    }
}
