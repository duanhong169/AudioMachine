package top.defaults.audio;

import android.os.Bundle;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

class SaveRawAudioToFileInterceptor implements AudioInterceptor<Bundle> {

    private final ExecutorService executorService;
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "AudioProcessor #" + mCount.getAndIncrement());
        }
    };
    private final LinkedList<Future<Integer>> futures = new LinkedList<>();

    private String filePath;
    private FileWriter fileWriter;
    private InterceptResultCallback<Bundle> callback;

    SaveRawAudioToFileInterceptor(String filePath) {
        this.filePath = filePath;
        try {
            fileWriter = new FileWriter(filePath);
            fileWriter.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (fileWriter != null) {
            executorService = Executors.newSingleThreadExecutor(sThreadFactory);
        } else {
            executorService = null;
        }
    }

    @Override
    public int interceptPoint() {
        return POINT_BEFORE_ENCODE;
    }

    @Override
    public void onAudio(@NonNull final byte[] buffer, boolean end) {
        if (fileWriter == null) return;

        Future<Integer> future = executorService.submit(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                return fileWriter.appendData(buffer);
            }
        });
        futures.offer(future);
        if (end) {
            executorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    int fileLength = 0;

                    while (futures.size() > 0) {
                        int packageLength = futures.poll().get();
                        if (packageLength == -1) {
                            fileLength = -1;
                            break;
                        }
                        fileLength += packageLength;
                    }

                    Bundle bundle = new Bundle();
                    bundle.putInt(Keys.SAVE_RAW_AUDIO_LENGTH, fileLength);
                    bundle.putString(Keys.SAVE_RAW_AUDIO_PATH, filePath);

                    if (callback != null) {
                        callback.onInterceptResult(bundle);
                    }

                    return null;
                }
            });
        }
    }

    @Override
    public void registerCallback(InterceptResultCallback<Bundle> callback) {
        this.callback = callback;
    }

}
