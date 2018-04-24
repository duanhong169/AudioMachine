package top.defaults.audioapp;

import android.text.TextUtils;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.Callable;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import top.defaults.audio.Error;
import top.defaults.audio.OneShotDelegate;
import top.defaults.audio.RawResult;

public class BaiduAPDelegate extends OneShotDelegate {

    private static final String CLIENT_ID = "gW0Fr0nCUw7A597cXxUUH7cs";
    private static final String CLIENT_SECRET = "4553d111b93c1e00ffe13dd3c772a0ac";
    private static final String CUID = "duanhong169";

    private static final MediaType MEDIA_TYPE = MediaType.parse("audio/pcm;rate=16000");
    private ByteBuffer audio = ByteBuffer.allocate(16000 * 2 * 60); // max audio length 60s
    private OkHttpClient client = new OkHttpClient();

    private boolean exhausted = false; // delegate will not accept more data if it's exhausted

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
                String token;
                String tokenRequestUrl = String.format(Locale.getDefault(),
                        "https://openapi.baidu.com/oauth/2.0/token?grant_type=client_credentials" +
                                "&client_id=%s&client_secret=%s", CLIENT_ID, CLIENT_SECRET);
                Request tokenRequest = new Request.Builder()
                        .url(tokenRequestUrl)
                        .build();

                Response tokenResponse = client.newCall(tokenRequest).execute();
                String tokenResponseString = "No response";
                try {
                    ResponseBody responseBody = tokenResponse.body();
                    if (responseBody != null) {
                        tokenResponseString = responseBody.string();
                    }
                } catch (IOException e) {
                    throw new Error(Error.ERROR_NETWORK);
                }
                JSONObject tokenJson = new JSONObject(tokenResponseString);
                token = tokenJson.optString("access_token");

                if (TextUtils.isEmpty(token)) {
                    throw new Error(Error.ERROR_NETWORK);
                }

                byte[] audioBytes = new byte[audio.position()];
                audio.rewind();
                audio.get(audioBytes);
                RequestBody body = RequestBody.create(MEDIA_TYPE, audioBytes);
                String url = String.format(Locale.getDefault(), "http://vop.baidu.com/server_api?dev_pid=1536&cuid=%s&token=%s", CUID, token);
                Request request = new Request.Builder()
                        .url(url)
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
    public boolean exhausted() {
        return exhausted;
    }
}
