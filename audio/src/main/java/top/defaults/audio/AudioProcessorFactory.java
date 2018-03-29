package top.defaults.audio;

class AudioProcessorFactory {

    static AudioProcessor createRemoteProcessor() {
        return new AudioProcessorImpl(new RemoteProcessorDelegate());
    }
}
