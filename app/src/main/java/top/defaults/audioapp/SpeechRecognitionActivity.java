package top.defaults.audioapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;
import top.defaults.audio.AudioProcessorDelegate;
import top.defaults.audio.Error;
import top.defaults.audio.Keys;
import top.defaults.audio.SpeechRecognizer;

public class SpeechRecognitionActivity extends AppCompatActivity {

    SpeechRecognizer speechRecognizer;
    RxPermissions rxPermissions;

    @BindView(R.id.result) TextView result;
    @BindView(R.id.delegateChooser) RadioGroup delegateChooser;

    @OnClick(R.id.stop) void stop() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }
    @OnClick(R.id.cancel) void cancel() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }
    }

    @SuppressLint("CheckResult")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        ButterKnife.bind(this);
        setTitle(R.string.speech_recognition);
        delegateChooser.setVisibility(View.VISIBLE);

        rxPermissions = new RxPermissions(this);
        RxView.clicks(findViewById(R.id.start))
                .compose(rxPermissions.ensure(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                .subscribe(granted -> {
                    if (granted) {
                        if (speechRecognizer != null) {
                            speechRecognizer.cancel();
                        }
                        speechRecognizer = new SpeechRecognizer(this);
                        speechRecognizer.setRecognitionListener(new SpeechRecognizer.RecognitionListener() {
                            @Override
                            public void onReadyForSpeech() {

                            }

                            @Override
                            public void onBeginningOfSpeech() {

                            }

                            @Override
                            public void onRmsChanged(float rmsdB) {
                                result.setTextSize(12 + 12 * rmsdB);
                            }

                            @Override
                            public void onBufferReceived(byte[] buffer) {

                            }

                            @Override
                            public void onEndOfSpeech() {
                                result.setText(R.string.pending_result);
                            }

                            @Override
                            public void onPartialResults(SpeechRecognizer.Results results) {
                                result.setText(results.rawJSONString);
                                Timber.d(results.rawJSONString);
                            }

                            @Override
                            public void onResults(SpeechRecognizer.Results results) {
                                result.setText(results.rawJSONString);
                            }

                            @Override
                            public void onError(Error error) {
                                Timber.e(error);
                                result.setText(String.format("Error: %s", error));
                                Toast.makeText(SpeechRecognitionActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onEvent(int eventType, Bundle params) {
                                if (eventType == Keys.EVENT_TYPE_RAW_AUDIO_SAVED) {
                                    Timber.d("File name: %s, length: %d",
                                            params.getString(Keys.SAVE_RAW_AUDIO_PATH),
                                            params.getInt(Keys.SAVE_RAW_AUDIO_LENGTH));
                                }
                            }

                            @Override
                            public void onFinish() {

                            }
                        });

                        Map<String, Object> params = new HashMap<>(10);
                        params.put(Keys.PACKAGE_SIZE, 4000);
                        params.put(Keys.THREAD_COUNT, 5);
                        params.put(Keys.SAVE_RAW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath() + "/0/dev/1.pcm");
                        params.put(Keys.AUDIO_PROCESSOR_DELEGATE, chooseDelegate());
                        speechRecognizer.startListening(params);
                    }
                });
    }

    @Override
    protected void onPause() {
        if (speechRecognizer != null) {
            speechRecognizer.cancel();
        }
        super.onPause();
    }

    private AudioProcessorDelegate chooseDelegate() {
        switch (delegateChooser.getCheckedRadioButtonId()) {
            case R.id.useBaidu:
                break;
            case R.id.useXunfei:
                return new XunfeiAPDelegate();
            default:
                break;
        }
        return new BaiduAPDelegate();
    }
}
