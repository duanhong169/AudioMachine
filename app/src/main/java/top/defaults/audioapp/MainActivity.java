package top.defaults.audioapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    @OnClick(R.id.activity1)
    void activity1() {
        startActivity(new Intent(this, PcmRecordActivity.class));
    }

    @OnClick(R.id.activity2)
    void activity2() {
        startActivity(new Intent(this, SpeechRecognitionActivity.class));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Timber.tag("LifeCycles");
        Timber.d("Activity Created");
    }
}
