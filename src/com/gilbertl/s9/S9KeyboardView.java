package com.gilbertl.s9;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class S9KeyboardView extends KeyboardView {

	static final String TAG = "S9KeyboardView";
    static final int KEYCODE_OPTIONS = -100;

    private View.OnTouchListener onTouchListener;

    public S9KeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

	@Override
    protected boolean onLongPress(Key key) {
        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        } else {
            return super.onLongPress(key);
        }
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent me) {
    	return getOnTouchListener().onTouch(this,me) || super.onTouchEvent(me);
    }
    
    public View.OnTouchListener getOnTouchListener() {
		return onTouchListener;
	}

	public void setOnTouchListener(View.OnTouchListener onTouchListener) {
		this.onTouchListener = onTouchListener;
	}
}
