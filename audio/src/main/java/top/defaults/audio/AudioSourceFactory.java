package top.defaults.audio;

import java.util.Map;

class AudioSourceFactory {

    static AudioSource createAudioSource(Map<String, Object> params) {
        AudioSource audioSource = Utils.getObject(AudioSource.class, params, Keys.AUDIO_SOURCE);
        if (audioSource != null) return audioSource;
        return MicAudioSource.getAudioSource(params);
    }
}
