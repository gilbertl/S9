/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gilbertl.s9;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class LatinKeyboardView extends KeyboardView {

    static final int KEYCODE_OPTIONS = -100;

    private GestureDetector mGestureDetector;
    
    public LatinKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mGestureDetector = new GestureDetector(new SimpleOnGestureListener() {
            @Override
            public boolean
            onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        		Log.i("LatinKeyboardView",
        				String.format("e1: %s\ne2: %s\nvelocityX: %f\nvelocityY: %f\n",
        						e1, e2, velocityX, velocityY));
        		return true;
        	}
        });
    }

    public LatinKeyboardView(Context context, AttributeSet attrs, int defStyle) {
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
    	if (mGestureDetector.onTouchEvent(me)) {
    		return true;
    	} else {
    		return super.onTouchEvent(me);
    	}
    }
}
