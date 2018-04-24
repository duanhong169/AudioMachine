package top.defaults.audio;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

import java.util.Map;

public class PcmRecorder {

    private static final int CALLBACK_ON_READY_FOR_RECORDING = 1;
    private static final int CALLBACK_ON_RMS_CHANGED = 3;
    private static final int CALLBACK_ON_BUFFER_RECEIVED = 4;
    private static final int CALLBACK_ON_END_OF_RECORDING = 5;
    private static final int CALLBACK_ON_ERROR = 6;
    private static final int CALLBACK_ON_FILE_SAVED = 9;
    private static final int CALLBACK_ON_FINISH = 10;

    private Context context;
    private AudioMachine machine;
    private AudioMachine.EventListener machineEventListener;
    private AudioInterceptor interceptor;
    private CallbackHandler callbackHandler;

    public PcmRecorder(Context context) {
        this.context = context;
        callbackHandler = new CallbackHandler(context);
        machineEventListener = new AudioMachine.EventListener() {
            @Override
            public void onRmsChanged(float rmsdB) {
                callbackHandler.onRmsChanged(rmsdB);
            }

            @Override
            public void didStartWorking() {
                callbackHandler.onReadyForRecording();
            }

            @Override
            public void willStopWorking() {
                callbackHandler.onEndOfRecording();
            }

            @Override
            public void onPartialResult(RawResult result) {}

            @Override
            public void onResult(RawResult result) {}

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
            public void onAudio(@NonNull byte[] buffer, boolean end) {
                Logger.d("new buffer received: " + buffer.length);
                if (buffer.length > 0) {
                    callbackHandler.onBufferReceived(buffer);
                }
            }

            @Override
            public void registerCallback(InterceptResultCallback<Void> listener) {

            }
        };
    }

    public void setRecorderListener(RecorderListener listener) {
        callbackHandler.setRecorderListener(listener);
    }

    public void startRecording(Map<String, Object> params) {
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            callbackHandler.onError(new Error(Error.ERROR_AUDIO, "No RECORD_AUDIO permission. "));
            return;
        }

        Logger.d("startRecording params: " + params);

        AudioMachine.Builder machineBuilder = new AudioMachine.Builder()
                .audioSource(AudioSourceFactory.createAudioSource(params))
                .audioCodec(new RawCodec())
                .audioProcessor(AudioProcessorFactory.createMockProcessor())
                .addInterceptor(interceptor);

        String saveRawAudioPath = Utils.getString(params, Keys.SAVE_RAW_AUDIO_PATH, null);
        if (saveRawAudioPath != null) {
            permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                callbackHandler.onError(new Error(Error.ERROR_AUDIO, "No WRITE_EXTERNAL_STORAGE permission. "));
                return;
            }

            SaveRawAudioToFileInterceptor interceptor = new SaveRawAudioToFileInterceptor(saveRawAudioPath);
            interceptor.registerCallback(result -> callbackHandler.onFileSaved(result));
            machineBuilder.addInterceptor(interceptor);
        }

        machine = machineBuilder.build();
        Logger.d(machine.selfIntroduction());
        machine.start(machineEventListener);
    }

    public void stopRecording() {
        machine.finishInput();
    }

    public void cancel() {
        machine.cancel();
    }

    private static class CallbackHandler extends Handler {

        private RecorderListener recorderListener;

        CallbackHandler(Context context) {
            super(context.getMainLooper());
        }

        void setRecorderListener(RecorderListener listener) {
            recorderListener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            if (recorderListener == null) {
                return;
            }

            Logger.d("handleMessage: " + msg.what);
            switch (msg.what) {
                case CALLBACK_ON_READY_FOR_RECORDING:
                    recorderListener.onReadyForRecording();
                    break;
                case CALLBACK_ON_RMS_CHANGED:
                    recorderListener.onRmsChanged((Float) msg.obj);
                    break;
                case CALLBACK_ON_BUFFER_RECEIVED:
                    recorderListener.onBufferReceived((byte[]) msg.obj);
                    break;
                case CALLBACK_ON_END_OF_RECORDING:
                    recorderListener.onEndOfRecording();
                    break;
                case CALLBACK_ON_ERROR:
                    recorderListener.onError((Error) msg.obj);
                    break;
                case CALLBACK_ON_FILE_SAVED:
                    recorderListener.onFileSaved((Bundle) msg.obj);
                    break;
                case CALLBACK_ON_FINISH:
                    recorderListener.onFinish();
                    break;
                default:
                    break;
            }
        }

        void onReadyForRecording() {
            Message.obtain(this, CALLBACK_ON_READY_FOR_RECORDING).sendToTarget();
        }

        void onRmsChanged(final float rmsdB) {
            Message.obtain(this, CALLBACK_ON_RMS_CHANGED, rmsdB).sendToTarget();
        }

        void onBufferReceived(final byte[] buffer) {
            Message.obtain(this, CALLBACK_ON_BUFFER_RECEIVED, buffer).sendToTarget();
        }

        void onEndOfRecording() {
            Message.obtain(this, CALLBACK_ON_END_OF_RECORDING).sendToTarget();
        }

        void onError(final Error error) {
            Message.obtain(this, CALLBACK_ON_ERROR, error).sendToTarget();
        }

        void onFileSaved(final Bundle params) {
            Message.obtain(this, CALLBACK_ON_FILE_SAVED, params).sendToTarget();
        }

        void onFinish() {
            Message.obtain(this, CALLBACK_ON_FINISH).sendToTarget();
        }
    }

    public interface RecorderListener extends AudioVolumeListener {

        void onReadyForRecording();

        void onBufferReceived(byte[] buffer);

        void onEndOfRecording();

        void onError(Error error);

        void onFileSaved(Bundle params);

        void onFinish();
    }
}
