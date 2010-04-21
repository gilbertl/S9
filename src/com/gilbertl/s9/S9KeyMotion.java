package com.gilbertl.s9;

import android.graphics.PointF;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;

public class S9KeyMotion {
	
	public static String TAG = "S9KeyMotion";
	
	public static final int MIDDLE = 0;
	public static final int UP = 1;
	public static final int RIGHT = 2;
	public static final int DOWN = 3;
	public static final int LEFT = 4;
		
	private PointF mDownPoint;
	private Keyboard.Key mKey;
	private float mRadius;
	
	public S9KeyMotion(
			PointF downPoint, Keyboard.Key key, float swipeSensitivity) {
		mDownPoint = downPoint;
		mKey = key;
		mRadius = 1 / swipeSensitivity;
	}

	public int calcMotion(PointF upPoint) {		
		// if start was at (0,0), end would be...
		PointF relUpPoint =
			new PointF(upPoint.x - mDownPoint.x, upPoint.y - mDownPoint.y);
		
		if (relUpPoint.length() < mRadius) {
			return MIDDLE;
		}
		
		final int LATERAL_BIAS = 2;
		// we could divide the regions evenly with the lines y = x and y = -x
		// however, since letters are much more likely then symbols, 
		// we divide our regions with y = LATERAL_BIAS * x instead
		
		if (relUpPoint.y >= relUpPoint.x * LATERAL_BIAS) {
			// either UP or LEFT
			if (relUpPoint.y >= -relUpPoint.x * LATERAL_BIAS) {
				return DOWN;
			} else {
				return LEFT;
			}
		} else {
			if (-relUpPoint.y >= relUpPoint.x * LATERAL_BIAS) {
				return UP;
			} else {
				return RIGHT;
			}
		}
	}
	
	public Key getKey() {
		return mKey;
	}

}
