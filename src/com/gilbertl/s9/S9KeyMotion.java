package com.gilbertl.s9;

import android.graphics.PointF;
import android.util.Log;

public abstract class S9KeyMotion {
	
	public static String TAG = "S9KeyMotion";
	
	public static int MIDDLE = 0;
	public static int UP = 1;
	public static int RIGHT = 2;
	public static int DOWN = 3;
	public static int LEFT = 4;
	
	public static int calculate(PointF start, PointF end, float radius) {		
		// if start was at (0,0), end would be...
		PointF relativeEnd = new PointF(end.x - start.x, end.y - start.y);
		
		if (relativeEnd.length() < radius) {
			return MIDDLE;
		}
		
		if (relativeEnd.y >= relativeEnd.x) {
			// either UP or LEFT
			if (relativeEnd.y >= -relativeEnd.x) {
				return DOWN;
			} else {
				return LEFT;
			}
		} else {
			if (-relativeEnd.y >= relativeEnd.x) {
				return UP;
			} else {
				return RIGHT;
			}
		}
	}
}
