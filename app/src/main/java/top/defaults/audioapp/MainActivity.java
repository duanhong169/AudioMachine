package top.defaults.audioapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;
import top.defaults.audio.Error;
import top.defaults.audio.Keys;
import top.defaults.audio.SpeechRecognizer;

public class MainActivity extends AppCompatActivity {

    SpeechRecognizer speechRecognizer;
    RxPermissions rxPermissions;

    @OnClick(R.id.stop) void stop() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }
    @BindView(R.id.result) TextView result;

    @SuppressLint("CheckResult")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Timber.tag("LifeCycles");
        Timber.d("Activity Created");

        rxPermissions = new RxPermissions(this);
        RxView.clicks(findViewById(R.id.start))
                .compose(rxPermissions.ensure(Manifest.permission.RECORD_AUDIO))
                .subscribe(granted -> {
                    if (granted) {
                        speechRecognizer = new SpeechRecognizer(this);
                        speechRecognizer.setRecognitionListener(new SpeechRecognizer.RecognitionListener() {
                            @Override
                            public void onReadyForSpeech() {

                            }

                            @Override
                            public void onRmsChanged(float rmsdB) {

                            }

                            @Override
                            public void onBufferReceived(byte[] buffer) {

                            }

                            @Override
                            public void onEndOfSpeech() {

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
                            }

                            @Override
                            public void onFinish() {

                            }
                        });
                        Map<String, Object> params = new HashMap<>(10);
                        params.put(Keys.PACKAGE_SIZE, 4000);
                        params.put(Keys.THREAD_COUNT, 5);
                        speechRecognizer.startListening(params);
                    }
                });
    }
}
