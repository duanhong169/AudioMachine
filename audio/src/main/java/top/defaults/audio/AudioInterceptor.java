package top.defaults.audio;

import android.support.annotation.NonNull;

interface AudioInterceptor<T> {

    int POINT_BEFORE_ENCODE = 0;

    int POINT_AFTER_ENCODE = 1;

    int interceptPoint();

    void onAudio(@NonNull byte[] buffer, boolean end);

    void registerCallback(InterceptResultCallback<T> callback);

    interface InterceptResultCallback<T> {

        void onInterceptResult(T result);
    }

    abstract class RawAudioInterceptor<T> implements AudioInterceptor<T> {

        @Override
        public int interceptPoint() {
            return POINT_BEFORE_ENCODE;
        }

        @Override
        public void onAudio(@NonNull byte[] buffer, boolean end) {

        }

        @Override
        public void registerCallback(InterceptResultCallback<T> callback) {

        }
    }

    abstract class EncodedAudioInterceptor<T> implements AudioInterceptor<T> {

        @Override
        public int interceptPoint() {
            return POINT_AFTER_ENCODE;
        }

        @Override
        public void onAudio(@NonNull byte[] buffer, boolean end) {

        }

        @Override
        public void registerCallback(InterceptResultCallback<T> callback) {

        }
    }
}
