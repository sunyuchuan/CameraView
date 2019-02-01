package com.example.ffcmd.ffcmd_demo.view.gpuimage.filter;

import com.example.ffcmd.ffcmd_demo.view.utils.XMFilterType;

/**
 * Created by sunyc on 18-9-29.
 */

public class GPUImageFilterFactory {

    public static GPUImageFilter CreateFilter(XMFilterType filterType) {
        GPUImageFilter filter = null;
        switch(filterType) {
            case FILTER_BEAUTY:
                filter = new GPUImageFilterGroup();
                ((GPUImageFilterGroup) filter).addFilter(new GPUImageBeautyFilter(5));
                ((GPUImageFilterGroup) filter).addFilter(new GPUImageBrightnessFilter(0.05f));
                break;
            default:
                filter = new GPUImageFilter();
                break;
        }
        return filter;
    }
}
