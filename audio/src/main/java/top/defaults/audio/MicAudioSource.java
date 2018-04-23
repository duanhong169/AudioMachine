package top.defaults.audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import java.io.IOException;
import java.util.Map;

class MicAudioSource implements AudioSource, Runnable {
    private AudioRecord audioRecord;
    private final int sampleRate;

    private Thread recorderThread;
    private AudioBuffer audioBuffer;

    private boolean isStarted = false;
    private boolean isStopped = false;

    private MicAudioSource(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public static MicAudioSource getAudioSource(Map<String, Object> params) {
        int sampleRate = Utils.getInt(params, Keys.SAMPLE_RATE, 16000);
        if (sampleRate == 8000) {
            return new8kMicrophoneAudioSource();
        } else {
            return new16kMicrophoneAudioSource();
        }
    }

    private static MicAudioSource new16kMicrophoneAudioSource() {
        return new MicAudioSource(16000);
    }

    private static MicAudioSource new8kMicrophoneAudioSource() {
        return new MicAudioSource(8000);
    }

    @Override
    public int readFully(byte[] buffer) throws IOException {
        if (audioBuffer == null) {
            throw new IOException(Utils.exceptionMessage(Error.ERROR_AUDIO, "MicrophoneInputStream not opened"));
        }

        return audioBuffer.readFully(buffer);
    }

    @Override
    public int read(byte[] buffer) throws IOException {
        return read(buffer, 0, buffer.length);
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        if (audioBuffer == null) {
            throw new IOException(Utils.exceptionMessage(Error.ERROR_AUDIO, "MicrophoneInputStream not opened"));
        }
        if (byteOffset + byteCount > buffer.length) {
            throw new IndexOutOfBoundsException(Utils.exceptionMessage(Error.ERROR_AUDIO,
                    "buffer overflow"));
        }

        return audioBuffer.read(buffer, byteOffset, byteCount);
    }

    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    @Override
    public void open() throws IOException {
        if (isStarted) {
            throw new IOException(Utils.exceptionMessage(Error.ERROR_AUDIO, "AudioRecord is opened."));
        }
        if (isStopped) {
            throw new IOException(Utils.exceptionMessage(Error.ERROR_AUDIO, "AudioRecord is closed."));
        }
        int bufferSize =
                AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioRecord =
                new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, bufferSize);
        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IOException(Utils.exceptionMessage(Error.ERROR_AUDIO,
                    "AudioRecord initial failed, please check if other application is using mic."));
        }

        audioBuffer = AudioBufferFactory.createAudioBuffer(OkioBuffer.class);
        audioRecord.startRecording();
        if (audioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
            throw new IOException(Utils.exceptionMessage(Error.ERROR_AUDIO,
                    "AudioRecord startRecording failed, please check if other application is using mic."));
        }
        isStarted = true;
        recorderThread = new Thread(this, "recorder thread");
        recorderThread.start();
    }

    @Override
    public void run() {
        Logger.d("recorder loop start");
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        while (!isStopped) {
            byte[] buffer = new byte[640];
            int length = audioRecord.read(buffer, 0, buffer.length);
            try {
                audioBuffer.write(buffer, 0, length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        audioRecord.stop();
        audioRecord.release();
        audioRecord = null;
        Logger.d("recorder loop end");
    }

    @Override
    public void close() {
        isStopped = true;
        if (recorderThread != null) {
            try {
                recorderThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            recorderThread = null;
        }
        audioBuffer.markAsFinished();
    }
}
