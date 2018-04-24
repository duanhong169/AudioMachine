package top.defaults.audio;

import java.util.Map;

class AudioProcessorFactory {

    static AudioProcessor createMockProcessor() {
        MockProcessorDelegate delegate = new MockProcessorDelegate();
        return new AudioProcessorImpl(delegate);
    }

    static AudioProcessor createAudioProcessor(Map<String, Object> params) {
        AudioProcessorDelegate delegate = Utils.getObject(AudioProcessorDelegate.class, params, Keys.AUDIO_PROCESSOR_DELEGATE);
        if (delegate == null) return createMockProcessor();
        return new AudioProcessorImpl(delegate);
    }
}
