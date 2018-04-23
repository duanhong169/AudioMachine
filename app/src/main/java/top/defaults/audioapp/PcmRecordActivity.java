package top.defaults.audioapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding2.view.RxView;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;
import top.defaults.audio.Error;
import top.defaults.audio.Keys;
import top.defaults.audio.PcmRecorder;

public class PcmRecordActivity extends AppCompatActivity {

    PcmRecorder recorder;
    RxPermissions rxPermissions;

    @BindView(R.id.result)
    TextView result;
    @OnClick(R.id.stop) void stop() {
        if (recorder != null) {
            recorder.stopRecording();
        }
    }
    @OnClick(R.id.cancel) void cancel() {
        if (recorder != null) {
            recorder.cancel();
        }
    }

    @SuppressLint("CheckResult")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio);
        ButterKnife.bind(this);

        rxPermissions = new RxPermissions(this);
        RxView.clicks(findViewById(R.id.start))
                .compose(rxPermissions.ensure(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                .subscribe(granted -> {
                    if (granted) {
                        if (recorder != null) {
                            recorder.cancel();
                        }
                        recorder = new PcmRecorder(this);
                        recorder.setRecorderListener(new PcmRecorder.RecorderListener() {
                            @Override
                            public void onReadyForRecording() {

                            }

                            @Override
                            public void onRmsChanged(float rmsdB) {

                            }

                            @Override
                            public void onBufferReceived(byte[] buffer) {
                                result.setText(String.format(Locale.getDefault(), "Buffer received: %d", buffer.length));
                            }

                            @Override
                            public void onEndOfRecording() {

                            }

                            @Override
                            public void onError(Error error) {
                                Timber.e(error);
                                result.setText(String.format("Error: %s", error));
                                Toast.makeText(PcmRecordActivity.this, error.toString(), Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFileSaved(Bundle params) {
                                result.setText(String.format(Locale.getDefault(), "File name: %s, length: %d",
                                        params.getString(Keys.SAVE_RAW_AUDIO_PATH),
                                        params.getInt(Keys.SAVE_RAW_AUDIO_LENGTH)));
                            }

                            @Override
                            public void onFinish() {

                            }
                        });

                        Map<String, Object> params = new HashMap<>(10);
                        params.put(Keys.SAMPLE_RATE, 8000);
                        params.put(Keys.SAVE_RAW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath() + "/0/dev/2.pcm");
                        recorder.startRecording(params);
                    }
                });
    }

    @Override
    protected void onPause() {
        if (recorder != null) {
            recorder.cancel();
        }
        super.onPause();
    }
}
