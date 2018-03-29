package top.defaults.audio;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class AudioProcessorImpl implements AudioProcessor {

    private final ExecutorService executorService;
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);
        public Thread newThread(@NonNull Runnable r) {
            return new Thread(r, "AudioProcessor #" + mCount.getAndIncrement());
        }
    };

    private ComposeThread composeThread;
    private AudioProcessorDelegate delegate;
    private int packageSize;

    private AudioBuffer audioBuffer;
    private final ConcurrentLinkedQueue<RawResult> results = new ConcurrentLinkedQueue<>();
    private final LinkedList<Future<RawResult>> futures = new LinkedList<>();
    private Error error;
    private boolean isInitialized;
    private boolean isCanceled;
    private boolean isStopped;
    private boolean isProcessFinished;

    AudioProcessorImpl(AudioProcessorDelegate delegate) {
        executorService = Executors.newFixedThreadPool(1, sThreadFactory);
        this.delegate = delegate;
        packageSize = delegate.packageSize();
    }

    @Override
    public void initialize() {
        if (!isInitialized) {
            isInitialized = true;
        } else {
            return;
        }
        audioBuffer = AudioBufferFactory.createAudioBuffer(OkioBuffer.class);
        delegate.initialize();
        composeThread = new ComposeThread();
        composeThread.start();
        FetchResultThread fetchResultThread = new FetchResultThread();
        fetchResultThread.start();
    }

    @Override
    public void appendData(byte[] data, int offset, int length, boolean end) {
        if (data != null) {
            try {
                audioBuffer.write(data, offset, length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (end) audioBuffer.markAsFinished();
    }

    private class ComposeThread extends Thread {
        @Override
        public void run() {
            Logger.logD("-------- " + this.getClass() + " start running --------");

            int index = 0;
            int audioTotalLen = 0;

            while (!isCanceled) {
                byte[] dataPack = new byte[packageSize];
                int readLen;
                try {
                    readLen = audioBuffer.readFully(dataPack);
                } catch (IOException e) {
                    e.printStackTrace();
                    setError(Utils.errorFromThrowable(e));
                    break;
                }

                if (isCanceled || isProcessFinished) {
                    break;
                }

                audioTotalLen += readLen;
                boolean end = readLen < packageSize;
                if (end && audioTotalLen == 0) {
                    setError(new Error(Error.ERROR_CLIENT, "No data sent"));
                    break;
                }

                Future<RawResult> future = executorService.submit(delegate.compose(index, dataPack, readLen, end));
                ++index;
                futures.offer(future);

                if (end || isProcessFinished) {
                    break;
                }

                if (Utils.napInterrupted()) {
                    break;
                }
            }
            Logger.logD("-------- " + this.getClass() + " finished running --------");
        }
    }

    private class FetchResultThread extends Thread {
        @Override
        public void run() {
            Logger.logD("-------- " + this.getClass() + " start running --------");

            while (!isCanceled) {
                final Future<RawResult> future = futures.peek();
                if (future == null || !future.isDone()) {
                    if (Utils.napInterrupted()) {
                        break;
                    }
                    continue;
                }
                futures.remove(future);
                try {
                    final RawResult result = future.get();
                    results.offer(result);
                    if (result.end) {
                        isProcessFinished = true;
                        break;
                    }
                } catch (InterruptedException e) {
                    setError(Utils.errorFromThrowable(e));
                    break;
                } catch (ExecutionException e) {
                    setError(Utils.errorFromThrowable(e.getCause()));
                    break;
                }

                if (Utils.napInterrupted()) {
                    break;
                }
            }
            try {
                composeThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            executorService.shutdown();
            for (Future future: futures) {
                future.cancel(true);
            }
            while (!executorService.isTerminated()) {
                if (Utils.napInterrupted()) {
                    break;
                }
            }
            isStopped = true;
            Logger.logD("-------- " + this.getClass() + " finished running --------");
        }
    }

    private synchronized void setError(Error error) {
        if (this.error == null) {
            this.error = error;
            cancel();
        }
    }

    @Override
    public RawResult read() throws Error {
        if (results.size() > 0) {
            return results.poll();
        }
        if (error != null) {
            throw error;
        }
        return null;
    }

    @Override
    public void cancel() {
        isCanceled = true;
    }

    @Override
    public boolean isStopped() {
        return isStopped;
    }

    @Override
    public void release() {
        delegate.release();
    }

}
