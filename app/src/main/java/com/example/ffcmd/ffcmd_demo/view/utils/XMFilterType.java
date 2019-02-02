package com.example.ffcmd.ffcmd_demo.view.utils;

/**
 * Created by why8222 on 2016/2/25.
 */
public enum XMFilterType {
    NONE(-1),
    FILTER_BEAUTY(0),
    FILTER_FACE_STICKER(1),
    FILTER_CAMERA_INPUT(2),
    FILTER_PIP(3);

    private final int value;

    XMFilterType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
