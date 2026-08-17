package com.app.features.media.enums;

public enum MediaVideoEncoder {
    AUTO("auto"),
    LIBX264("libx264"),
    H264_QSV("h264_qsv"),
    H264_VAAPI("h264_vaapi");

    private final String ffmpegName;

    MediaVideoEncoder(String ffmpegName) {
        this.ffmpegName = ffmpegName;
    }

    public String getFfmpegName() {
        return ffmpegName;
    }
}
