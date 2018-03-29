package top.defaults.audio;

import java.util.Map;

class AudioProcessorFactory {

    static AudioProcessor createRemoteProcessor(Map<String, Object> params) {
        RemoteProcessorDelegate delegate = new RemoteProcessorDelegate();

        int packageSize = Utils.getInt(params, Keys.PACKAGE_SIZE, Constants.DEFAULT_PROCESSOR_PACKAGE_SIZE);
        delegate.setPackageSize(packageSize);

        int threadCount = Utils.getInt(params, Keys.THREAD_COUNT, Constants.DEFAULT_PROCESSOR_THREAD_COUNT);
        delegate.setThreadCount(threadCount);

        return new AudioProcessorImpl(delegate);
    }
}
