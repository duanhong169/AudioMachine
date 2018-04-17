package top.defaults.audio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.IntDef;
import android.support.v4.content.ContextCompat;

import org.json.JSONObject;

import java.util.Map;

public class SpeechRecognizer {

    private static final int CALLBACK_ON_READY_FOR_SPEECH = 1;
    private static final int CALLBACK_ON_BEGINNING_OF_SPEECH = 2;
    private static final int CALLBACK_ON_RMS_CHANGED = 3;
    private static final int CALLBACK_ON_BUFFER_RECEIVED = 4;
    private static final int CALLBACK_ON_END_OF_SPEECH = 5;
    private static final int CALLBACK_ON_ERROR = 6;
    private static final int CALLBACK_ON_PARTIAL_RESULTS = 7;
    private static final int CALLBACK_ON_RESULTS = 8;
    private static final int CALLBACK_ON_EVENT = 9;
    private static final int CALLBACK_ON_FINISH = 10;

    private Context context;
    private AudioMachine machine;
    private AudioMachine.EventListener machineEventListener;
    private AudioInterceptor interceptor;
    private ResultParser<Results> parser = new ResultParser<Results>() {
        @Override
        public Results parse(RawResult rawResult) {
            Results results = new Results();
            results.type = rawResult.end ? Results.ResultsType.RESULTS_TYPE_FULL : Results.ResultsType.RESULTS_TYPE_PARTIAL;
            results.rawJSONString = rawResult.string;
            return results;
        }
    };
    private CallbackHandler callbackHandler;

    public SpeechRecognizer(Context context) {
        this.context = context;
        callbackHandler = new CallbackHandler(context);
        machineEventListener = new AudioMachine.EventListener() {
            @Override
            public void didStartWorking() {
                callbackHandler.onReadyForSpeech();
            }

            @Override
            public void willStopWorking() {
                callbackHandler.onEndOfSpeech();
            }

            @Override
            public void onPartialResult(RawResult result) {
                callbackHandler.onPartialResults(parser.parse(result));
            }

            @Override
            public void onResult(RawResult result) {
                callbackHandler.onResults(parser.parse(result));
            }

            @Override
            public void onError(Error error) {
                callbackHandler.onError(error);
            }

            @Override
            public void didStopWorking() {
                callbackHandler.onFinish();
            }
        };
        interceptor = new AudioInterceptor<Void>() {

            @Override
            public int interceptPoint() {
                return POINT_BEFORE_ENCODE;
            }

            @Override
            public void onAudio(byte[] buffer, boolean end) {
                Logger.d("new buffer received: " + (buffer == null ? 0 : buffer.length));
                callbackHandler.onBufferReceived(buffer);
            }

            @Override
            public void registerCallback(InterceptResultCallback<Void> listener) {

            }
        };
    }

    public void setRecognitionListener(RecognitionListener listener) {
        callbackHandler.setRecognitionListener(listener);
    }

    public void startListening(Map<String, Object> params) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            callbackHandler.onError(new Error(Error.ERROR_AUDIO, "No RECORD_AUDIO permission. "));
            return;
        }

        Logger.d("startListening params: " + params);

        AudioMachine.Builder machineBuilder = new AudioMachine.Builder()
                .audioSource(MicAudioSource.getAudioSource(params))
                .audioCodec(AudioCodecFactory.createAudioCodec(params))
                .audioProcessor(AudioProcessorFactory.createRemoteProcessor(params))
                .addInterceptor(interceptor);

        String saveRawAudioPath = Utils.getString(params, Keys.SAVE_RAW_AUDIO_PATH, null);
        if (saveRawAudioPath != null) {
            permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                callbackHandler.onError(new Error(Error.ERROR_AUDIO, "No WRITE_EXTERNAL_STORAGE permission. "));
                return;
            }

            SaveRawAudioToFileInterceptor interceptor = new SaveRawAudioToFileInterceptor(saveRawAudioPath);
            interceptor.registerCallback(new AudioInterceptor.InterceptResultCallback<Bundle>() {
                @Override
                public void onInterceptResult(Bundle result) {
                    callbackHandler.onEvent(Keys.EVENT_TYPE_RAW_AUDIO_SAVED, result);
                }
            });
            machineBuilder.addInterceptor(interceptor);
        }

        machine = machineBuilder.build();
        Logger.d(machine.selfIntroduction());
        machine.start(machineEventListener);
    }

    public void stopListening() {
        machine.finishInput();
    }

    public void cancel() {
        machine.cancel();
    }

    private static class CallbackHandler extends Handler {

        private RecognitionListener recognitionListener;

        private boolean done = false;

        CallbackHandler(Context context) {
            super(context.getMainLooper());
        }

        void setRecognitionListener(RecognitionListener listener) {
            recognitionListener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            if (recognitionListener == null) {
                return;
            }
            // 过滤无意义的消息
            if (done && msg.what != CALLBACK_ON_END_OF_SPEECH  && msg.what != CALLBACK_ON_FINISH
                    && msg.what != CALLBACK_ON_EVENT) {
                return;
            }
            Logger.d("handleMessage: " + msg.what);
            switch (msg.what) {
                case CALLBACK_ON_READY_FOR_SPEECH:
                    recognitionListener.onReadyForSpeech();
                    break;
                case CALLBACK_ON_BEGINNING_OF_SPEECH:
                    recognitionListener.onBeginningOfSpeech();
                    break;
                case CALLBACK_ON_RMS_CHANGED:
                    recognitionListener.onRmsChanged((Float) msg.obj);
                    break;
                case CALLBACK_ON_BUFFER_RECEIVED:
                    recognitionListener.onBufferReceived((byte[]) msg.obj);
                    break;
                case CALLBACK_ON_END_OF_SPEECH:
                    recognitionListener.onEndOfSpeech();
                    break;
                case CALLBACK_ON_ERROR:
                    done = true;
                    recognitionListener.onError((Error) msg.obj);
                    break;
                case CALLBACK_ON_RESULTS:
                    done = true;
                    recognitionListener.onResults((Results) msg.obj);
                    break;
                case CALLBACK_ON_PARTIAL_RESULTS:
                    recognitionListener.onPartialResults((Results) msg.obj);
                    break;
                case CALLBACK_ON_EVENT:
                    recognitionListener.onEvent(msg.arg1, (Bundle) msg.obj);
                    break;
                case CALLBACK_ON_FINISH:
                    recognitionListener.onFinish();
                    break;
                default:
                    break;
            }
        }

        public void onReadyForSpeech() {
            Message.obtain(this, CALLBACK_ON_READY_FOR_SPEECH).sendToTarget();
        }

        public void onBeginningOfSpeech() {
            Message.obtain(this, CALLBACK_ON_BEGINNING_OF_SPEECH).sendToTarget();
        }

        public void onRmsChanged(final float rmsdB) {
            Message.obtain(this, CALLBACK_ON_RMS_CHANGED, rmsdB).sendToTarget();
        }

        public void onBufferReceived(final byte[] buffer) {
            Message.obtain(this, CALLBACK_ON_BUFFER_RECEIVED, buffer).sendToTarget();
        }

        public void onEndOfSpeech() {
            Message.obtain(this, CALLBACK_ON_END_OF_SPEECH).sendToTarget();
        }

        public void onError(final Error error) {
            Message.obtain(this, CALLBACK_ON_ERROR, error).sendToTarget();
        }

        public void onPartialResults(final Results results) {
            Message.obtain(this, CALLBACK_ON_PARTIAL_RESULTS, results).sendToTarget();
        }

        public void onResults(final Results results) {
            Message.obtain(this, CALLBACK_ON_RESULTS, results).sendToTarget();
        }

        public void onEvent(final int eventType, final Bundle params) {
            Message.obtain(this, CALLBACK_ON_EVENT, eventType, eventType, params)
                    .sendToTarget();
        }

        public void onFinish() {
            Message.obtain(this, CALLBACK_ON_FINISH).sendToTarget();
        }
    }

    public static class Results {

        @IntDef({ ResultsType.RESULTS_TYPE_PARTIAL, ResultsType.RESULTS_TYPE_FULL })
        public @interface ResultsType {
            int RESULTS_TYPE_PARTIAL = 0;
            int RESULTS_TYPE_FULL = 1;
        }

        public @ResultsType int type;

        public String rawJSONString;

        public JSONObject parsedResults;
    }

    public interface RecognitionListener {

        void onReadyForSpeech();

        void onBeginningOfSpeech();

        void onRmsChanged(float rmsdB);

        void onBufferReceived(byte[] buffer);

        void onEndOfSpeech();

        void onPartialResults(Results results);

        void onResults(Results results);

        void onError(Error error);

        void onEvent(int eventType, Bundle params);

        void onFinish();
    }
}
