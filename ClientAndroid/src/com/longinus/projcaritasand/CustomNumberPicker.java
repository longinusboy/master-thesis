package com.longinus.projcaritasand;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.NumberPicker;

public class CustomNumberPicker extends NumberPicker {

	public CustomNumberPicker(Context context) {
		super(context);
	}

	public CustomNumberPicker(Context context, AttributeSet attrs) {
		super(context, attrs);
		processAttributeSet(attrs);
	}

	public CustomNumberPicker(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		processAttributeSet(attrs);
	}

	private void processAttributeSet(AttributeSet attrs) {
        this.setMinValue(attrs.getAttributeIntValue(null, "min", 0));
        this.setMaxValue(attrs.getAttributeIntValue(null, "max", 0));
    }
	
}
