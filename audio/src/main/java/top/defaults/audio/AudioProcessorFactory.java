package top.defaults.audio;

class AudioProcessorFactory {

    static AudioProcessor createMockProcessor() {
        MockProcessorDelegate delegate = new MockProcessorDelegate();
        return new AudioProcessorImpl(delegate);
    }

    static AudioProcessor createAudioProcessor(AudioProcessorDelegate delegate) {
        if (delegate == null) return createMockProcessor();
        return new AudioProcessorImpl(delegate);
    }
}
