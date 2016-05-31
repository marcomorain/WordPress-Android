package org.wordpress.android.provider;

public class ProviderConstants {
    public static final String MIME_TYPE_IMAGE = "image/";
    public static final String MIME_TYPE_VIDEO = "video/";
    public static final String MIME_TYPE_AUDIO = "audio/";

    public static final String MIME_TYPE_IMAGES = MIME_TYPE_IMAGE + "*";
    public static final String MIME_TYPE_VIDEOS = MIME_TYPE_VIDEO + "*";
    public static final String MIME_TYPE_ANY_AUDIO = MIME_TYPE_AUDIO + "*";

    public static boolean isImageMimeType(String types) {
        return types != null && types.contains(MIME_TYPE_IMAGE);
    }

    public static boolean isVideoMimeType(String types) {
        return types != null && types.contains(MIME_TYPE_VIDEO);
    }

    public static boolean isAudioMimeType(String types) {
        return types != null && types.contains(MIME_TYPE_AUDIO);
    }
}