package top.defaults.audio;

class AudioBufferFactory {

    static AudioBuffer createAudioBuffer(Class<? extends AudioBuffer> audioBufferClass) {
        try {
            return audioBufferClass.newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return new OkioBuffer();
    }
}
