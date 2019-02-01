package com.example.ffcmd.ffcmd_demo.camera_preview;

import com.example.ffcmd.ffcmd_demo.R;
import com.example.ffcmd.ffcmd_demo.view.utils.XMFilterType;

public class FilterTypeHelper {

	public static int FilterType2Color(XMFilterType filterType){
		switch (filterType) {
			case NONE:
				return R.color.filter_color_grey_light;
			case FILTER_BEAUTY:
				return R.color.filter_color_pink;
			default:
				return R.color.filter_color_red;
		}
	}

	public static int FilterType2Name(XMFilterType filterType){
		switch (filterType) {
		case NONE:
			return R.string.filter_none;
		case FILTER_BEAUTY:
			return R.string.filter_beauty;
		default:
			return R.string.filter_none;
		}
	}
}
