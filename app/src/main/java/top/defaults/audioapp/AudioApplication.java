package top.defaults.audioapp;

import android.app.Application;

import timber.log.Timber;

import static timber.log.Timber.DebugTree;

public class AudioApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.plant(new DebugTree());
    }
}
