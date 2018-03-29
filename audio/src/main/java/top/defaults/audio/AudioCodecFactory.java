package top.defaults.audio;

import java.util.Map;

class AudioCodecFactory {

    static AudioCodec createAudioCodec(Map<String, Object> params) {
        String codecName = Utils.getString(params, Keys.COMPRESS_TYPE, Constants.DEFAULT_CODEC);
        if (codecName.equals(Constants.RAW_CODEC)) {
            return new RawCodec();
        }
        return new RawCodec();
    }
}
