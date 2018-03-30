package top.defaults.audio;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class AudioMachine {

    private AudioSource audioSource;
    private AudioCodec audioCodec;
    private AudioProcessor audioProcessor;
    private List<AudioInterceptor> audioInterceptors;
    private AudioCollectThread audioCollectThread;
    private EventListener eventListener;
    private boolean isCanceled;
    private boolean isInputFinished;

    AudioMachine(Builder builder) {
        this.audioSource = builder.audioSource;
        this.audioCodec = builder.audioCodec;
        this.audioProcessor = builder.audioProcessor;
        this.audioInterceptors = Collections.unmodifiableList(builder.audioInterceptors);
    }

    String selfIntroduction() {
        return "AudioMachine >>>>>>" +
                "\n>>>>>>>>>>>>>>>>>>>>>>>> AudioMachine -----------------------\n" +
                "AudioSource: " + audioSource.getClass().getSimpleName() +
                ", sampleRate: " + audioSource.getSampleRate() +
                "\n" +
                "AudioCodec: " + audioCodec.getClass().getSimpleName() +
                "\n" +
                "AudioProcessor: " +  audioProcessor.selfIntroduction() +
                "\n------------------------ AudioMachine <<<<<<<<<<<<<<<<<<<<<<<";
    }

    void start(EventListener eventListener) {
        Logger.logD("-------- start() --------");
        this.eventListener = eventListener;
        audioCollectThread = new AudioCollectThread();
        audioCollectThread.start();
        AudioProcessThread audioProcessThread = new AudioProcessThread();
        audioProcessThread.start();
    }

    private class AudioCollectThread extends Thread {
        @Override
        public void run() {
            Logger.logThreadStart();

            try {
                audioSource.open();
                audioProcessor.initialize();

                if (eventListener != null) {
                    eventListener.didStartWorking();
                }

                int frameSizeInByte = audioCodec.getFrameSize();
                int frameSizeInShort = frameSizeInByte / 2;
                int audioTotalLen = 0;
                byte[] buffer = new byte[frameSizeInByte];

                while (!isCanceled) {
                    if (isInputFinished) {
                        audioSource.close();
                    }

                    int readLen = audioSource.readFully(buffer);
                    if (readLen < frameSizeInByte) {
                        throw new EOFException();
                    }
                    audioTotalLen += frameSizeInByte;
                    Logger.logV("read len: " + frameSizeInByte);
                    for (AudioInterceptor interceptor: audioInterceptors) {
                        if (interceptor.interceptPoint() == AudioInterceptor.POINT_BEFORE_ENCODE) {
                            interceptor.onAudio(buffer, false);
                        }
                    }

                    if (audioCodec != null) {
                        byte[] encodedBuffer = new byte[frameSizeInByte];
                        short[] shortBuffer = new short[frameSizeInShort];
                        ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shortBuffer);
                        int encodedLen = audioCodec.encode(shortBuffer, frameSizeInShort, encodedBuffer);
                        Logger.logV("encoded len: " + encodedLen);

                        for (AudioInterceptor interceptor: audioInterceptors) {
                            if (interceptor.interceptPoint() == AudioInterceptor.POINT_AFTER_ENCODE) {
                                interceptor.onAudio(buffer, false);
                            }
                        }

                        audioProcessor.appendData(encodedBuffer, 0, encodedLen, false);
                    } else {
                        audioProcessor.appendData(buffer, 0, frameSizeInByte, false);
                    }
                }

                Logger.logD("recorded audio total length: " + audioTotalLen);
            } catch (EOFException e) {
                Logger.logD("discard last bytes (not enough for one frame to encode)");
            } catch (Exception e) {
                e.printStackTrace();
                onErrorDelegate(Utils.errorFromThrowable(e));
            } finally {
                for (AudioInterceptor interceptor: audioInterceptors) {
                    interceptor.onAudio(null, true);
                }
                audioProcessor.appendData(null, 0, 0, true);
                audioCodec.close();
                try {
                    audioSource.close();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    audioSource = null;
                }
                if (eventListener != null) {
                    eventListener.willStopWorking();
                }
            }

            Logger.logThreadFinish();
        }
    }

    private class AudioProcessThread extends Thread {
        @Override
        public void run() {
            Logger.logThreadStart();

            try {
                while (!isCanceled) {
                    RawResult rawResult = audioProcessor.read();

                    if (rawResult == null) {
                        if (Utils.napInterrupted()) {
                            break;
                        }
                        continue;
                    }

                    if (rawResult.end) {
                        if (eventListener != null) {
                            eventListener.onResult(rawResult);
                        }
                        break;
                    } else {
                        if (eventListener != null) {
                            eventListener.onPartialResult(rawResult);
                        }
                    }
                }
            } catch (Error error) {
                onErrorDelegate(error);
            }

            while (!audioProcessor.isStopped()) {
                if (Utils.napInterrupted()) {
                    break;
                }
            }
            try {
                audioCollectThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            audioProcessor.release();
            if (eventListener != null) {
                eventListener.didStopWorking();
            }

            Logger.logThreadFinish();
        }
    }

    void finishInput() {
        isInputFinished = true;
    }

    void cancel() {
        isCanceled = true;
        if (audioProcessor != null) {
            audioProcessor.cancel();
        }
        Logger.logD("-------- cancel() --------");
    }

    private void onErrorDelegate(Error error) {
        if (isCanceled) {
            return;
        }
        isCanceled = true;
        Logger.logD("-------- onErrorDelegate() --------");

        if (eventListener != null) {
            eventListener.onError(error);
        }
    }

    static final class Builder {

        AudioSource audioSource;
        AudioCodec audioCodec;
        AudioProcessor audioProcessor;

        List<AudioInterceptor> audioInterceptors = new ArrayList<>();

        Builder() {}

        public Builder audioSource(AudioSource audioSource) {
            this.audioSource = audioSource;
            return this;
        }

        public Builder audioCodec(AudioCodec audioCodec) {
            this.audioCodec = audioCodec;
            return this;
        }

        public Builder audioProcessor(AudioProcessor audioProcessor) {
            this.audioProcessor = audioProcessor;
            return this;
        }

        public Builder addInterceptor(AudioInterceptor interceptor) {
            audioInterceptors.add(interceptor);
            return this;
        }

        public AudioMachine build() throws IllegalStateException {
            if (audioSource == null || audioCodec == null || audioProcessor == null) {
                throw new IllegalStateException("audioSource, audioCodec and audioProcessor cannot be null.");
            }

            return new AudioMachine(this);
        }
    }

    interface EventListener {

        void didStartWorking();

        void willStopWorking();

        void onPartialResult(RawResult result);

        void onResult(RawResult result);

        void onError(Error error);

        void didStopWorking();
    }
}
