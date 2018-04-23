package top.defaults.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static top.defaults.audio.Error.ERROR_DEFAULT_CODE;

class Utils {

    private static Pattern pattern = Pattern.compile("^#(\\d+), (.+)");

    static String exceptionMessage(int code, String message) {
        return String.format(Locale.getDefault(), "#%d, %s", code, message);
    }

    private static int codeFromThrowable(Throwable throwable, int fallback) {
        int errorCode = fallback;

        String message = throwable.getMessage();
        if (message != null) {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                errorCode = Integer.parseInt(matcher.group(1));
            }
        }

        return errorCode;
    }

    private static String messageFromThrowable(Throwable throwable) {
        String message = throwable.getMessage();

        if (message == null) {
            message = throwable.toString();
        } else {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                message = matcher.group(2);
            }
        }
        return message;
    }

    static Error errorFromThrowable(Throwable throwable) {
        if (throwable instanceof Error) {
            return (Error) throwable;
        }
        return errorFromThrowable(throwable, ERROR_DEFAULT_CODE);
    }

    private static Error errorFromThrowable(Throwable throwable, int fallback) {
        return new Error(codeFromThrowable(throwable, fallback), messageFromThrowable(throwable));
    }

    static boolean napInterrupted() {
        return !sleep(1);
    }

    private static boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    static boolean getBoolean(Map<String, Object> params, String key, boolean defaultValue) {
        boolean value = defaultValue;
        if (params == null) {
            return value;
        }

        Object valueObject = params.get(key);
        if (valueObject instanceof Boolean) {
            value = (Boolean) valueObject;
        }
        return value;
    }

    static int getInt(Map<String, Object> params, String key, int defaultValue) {
        int value = defaultValue;
        if (params == null) {
            return value;
        }

        Object valueObject = params.get(key);
        if (valueObject instanceof Integer) {
            value = (Integer) valueObject;
        }
        return value;
    }

    static String getString(Map<String, Object> params, String key, String defaultValue) {
        String value = defaultValue;
        if (params == null) {
            return value;
        }

        Object valueObject = params.get(key);
        if (valueObject instanceof String) {
            value = (String) valueObject;
            if (value.length() == 0) {
                value = defaultValue;
            }
        }
        return value;
    }

    static <T> T getObject(Class<T> cls, Map<String, Object> params, String key) {
        if (params == null) {
            return null;
        }

        Object valueObject = params.get(key);
        if (valueObject == null || !cls.isInstance(valueObject)) {
            return null;
        }

        return cls.cast(valueObject);
    }

    static float calculateRmsdb(byte[] buffer) {
        if (buffer == null || buffer.length == 0) return 0;
        short[] shortBuffer = new short[buffer.length / 2];
        ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer);

        double[] normalizedDoubleBuffer = new double[shortBuffer.length];
        for (int i = 0; i < shortBuffer.length; i++) {
            normalizedDoubleBuffer[i] = (double) shortBuffer[i] / Short.MAX_VALUE;
        }

        return (float) volumeRMS(normalizedDoubleBuffer);
    }

    private static double volumeRMS(double[] raw) {
        double sum = 0d;
        if (raw.length==0) {
            return sum;
        } else {
            for (double d : raw) {
                sum += d;
            }
        }

        double average = sum / raw.length;
        double sumMeanSquare = 0d;
        for (double d : raw) {
            sumMeanSquare += Math.pow(d - average, 2d);
        }
        double averageMeanSquare = sumMeanSquare / raw.length;
        return Math.sqrt(averageMeanSquare);
    }
}
