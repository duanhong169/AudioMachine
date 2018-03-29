package top.defaults.audio;

public interface AudioInterceptor {

    void beforeEncode(byte[] buffer);

    void afterEncode(byte[] buffer);
}
