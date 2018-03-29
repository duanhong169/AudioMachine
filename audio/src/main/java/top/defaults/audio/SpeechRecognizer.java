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
        interceptor = new AudioInterceptor() {
            @Override
            public void beforeEncode(byte[] buffer) {
                Logger.logD("new buffer received: " + buffer.length);
                callbackHandler.onBufferReceived(buffer);
            }

            @Override
            public void afterEncode(byte[] buffer) {

            }
        };
    }

    /**
     * 设置语音识别监听器
     *
     * @param listener 监听器
     */
    public void setRecognitionListener(RecognitionListener listener) {
        callbackHandler.setRecognitionListener(listener);
    }

    /**
     * 开始监听语音输入
     *
     * @param params 识别参数
     */
    public void startListening(Map<String, Object> params) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            callbackHandler.onError(new Error(Error.ERROR_AUDIO, "No RECORD_AUDIO permission. "));
            return;
        }

        Logger.logD("startListening params: " + params);

        machine = new AudioMachine.Builder()
                .audioSource(MicAudioSource.getAudioSource(params))
                .audioCodec(AudioCodecFactory.createAudioCodec(params))
                .audioProcessor(AudioProcessorFactory.createRemoteProcessor(params))
                .addInterceptor(interceptor).build();

        Logger.logD(machine.selfIntroduction());

        machine.start(machineEventListener);
    }

    /**
     * 停止监听语音输入
     */
    public void stopListening() {
        machine.finishInput();
    }

    /**
     * 取消本次识别
     */
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
            // CALLBACK_ON_END_OF_SPEECH通知录音机释放
            if ((done && msg.what != CALLBACK_ON_END_OF_SPEECH  && msg.what != CALLBACK_ON_FINISH)
                    || recognitionListener == null) {
                return;
            }
            Logger.logD("handleMessage: " + msg.what);
            switch (msg.what) {
                case CALLBACK_ON_READY_FOR_SPEECH:
                    recognitionListener.onReadyForSpeech();
                    break;
                case CALLBACK_ON_BEGINNING_OF_SPEECH:
//                    recognitionListener.onBeginningOfSpeech();
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
//                    recognitionListener.onEvent(msg.arg1, (Bundle) msg.obj);
                    break;
                case CALLBACK_ON_FINISH:
                    recognitionListener.onFinish();
                    break;
                default:
//                    recognitionListener.onEvent(msg.arg1, (Bundle) msg.obj);
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
            /**
             * 中间结果
             */
            int RESULTS_TYPE_PARTIAL = 0;
            /**
             * 最终结果
             */
            int RESULTS_TYPE_FULL = 1;
        }

        /**
         * 结果类型
         */
        public @ResultsType int type;

        /**
         * 原始JSON数据
         */
        public String rawJSONString;

        /**
         * 解析后的JSON对象
         */
        public JSONObject parsedResults;
    }

    public interface RecognitionListener {

        /**
         * 录音设备就绪，可以提示用户开始说话
         */
        void onReadyForSpeech();

        /**
         * 检测到音量发生变化
         *
         * @param rmsdB 分贝值，范围[0.0, 1.0]
         */
        void onRmsChanged(float rmsdB);

        /**
         * 录音设备返回了新的语音数据
         *
         * @param buffer 语音数据
         */
        void onBufferReceived(byte[] buffer);

        /**
         * 录音已结束
         */
        void onEndOfSpeech();

        /**
         * 识别器返回中间结果
         *
         * @param results 中间结果
         */
        void onPartialResults(Results results);

        /**
         * 识别器返回最终结果
         *
         * @param results 最终结果
         */
        void onResults(Results results);

        /**
         * 识别过程出错
         *
         * @param error 错误对象
         */
        void onError(Error error);

        /**
         * 识别结束，一定会回调
         */
        void onFinish();
    }
}
