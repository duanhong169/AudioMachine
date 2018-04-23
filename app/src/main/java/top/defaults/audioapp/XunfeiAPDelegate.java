package top.defaults.audioapp;

import android.util.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Callable;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import top.defaults.audio.AudioProcessorDelegate;
import top.defaults.audio.Error;
import top.defaults.audio.RawResult;

/**
 * 科大讯飞 REST API 开发指南：http://doc.xfyun.cn/rest_api/
 *
 * 注意，需要在讯飞后台设置IP白名单，否则请求会被拒绝：
 *
 * <pre>
 * {
 *   "code":"10105",
 *   "desc":"illegal access|illegal client_ip",
 *   "data":"",
 *   "sid":"xxxxxx"
 * }
 * </pre>
 */
public class XunfeiAPDelegate implements AudioProcessorDelegate {

    private static final String API_ID = "5ad7fd88";
    private static final String API_KEY = "704492ea3911b2824c15c877575162f0";

    private static final String X_PARAM = "{\"engine_type\": \"sms16k\", \"aue\": \"raw\"}";
    private static final MediaType MEDIA_TYPE = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");

    private ByteBuffer audio = ByteBuffer.allocate(2 * 1024 * 768); // max audio length 2M consider as BASE64
    private OkHttpClient client = new OkHttpClient();

    private boolean exhausted = false; // delegate will not accept more data if it's exhausted

    @Override
    public void initialize() {}

    @Override
    public void release() {}

    @Override
    public Callable<RawResult> compose(int index, byte[] buffer, int length, boolean end) {
        if (exhausted) {
            return () -> null;
        }
        if (audio.remaining() < length) end = true;
        if (end) {
            exhausted = true;

            int lastLen = Math.min(audio.remaining(), length);
            if (lastLen > 0) {
                audio.put(buffer, 0, lastLen);
            }

            return () -> {
                byte[] audioBytes = new byte[audio.position()];
                audio.rewind();
                audio.get(audioBytes);
                byte[] audioBase64 = Base64.encode(audioBytes, Base64.NO_WRAP);
                String audioURLEncoded = "";
                try {
                    audioURLEncoded = URLEncoder.encode(new String(audioBase64), "utf-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                String curTime = System.currentTimeMillis() / 1000 + "";
                String xParamBase64 = new String(Base64.encode(X_PARAM.getBytes(), Base64.NO_WRAP));
                RequestBody body = RequestBody.create(MEDIA_TYPE, "audio=" + audioURLEncoded);
                Request request = new Request.Builder()
                        .url("http://api.xfyun.cn/v1/service/v1/iat")
                        .header("X-Appid", API_ID)
                        .header("X-CurTime", curTime)
                        .header("X-Param", xParamBase64)
                        .header("X-CheckSum", md5(API_KEY + curTime + xParamBase64))
                        .post(body)
                        .build();
                Response response;
                try {
                    response = client.newCall(request).execute();
                } catch (IOException e) {
                    throw new Error(Error.ERROR_NETWORK);
                }

                String responseString = "No response";
                try {
                    ResponseBody responseBody = response.body();
                    if (responseBody != null) {
                        responseString = responseBody.string();
                    }
                } catch (IOException e) {
                    throw new Error(Error.ERROR_NETWORK);
                }
                return new RawResult(responseString, index, true);
            };
        } else {
            audio.put(buffer);
            return () -> new RawResult("Recording...", index, false);
        }
    }

    @Override
    public int packageSize() {
        return 3200; // 100ms in 16k sample rate
    }

    @Override
    public int threadCount() {
        return 1; // only support 1 thread
    }

    @Override
    public boolean exhausted() {
        return exhausted;
    }

    private String md5(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(s.getBytes("UTF-8"));
            byte[] bytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte aByte : bytes) {
                int value = (int) aByte & 0xff;
                if (value < 16) {
                    sb.append("0");
                }
                sb.append(Integer.toHexString(value));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            return "";
        }
    }
}
