package com.gilbertl.s9;

import android.view.MotionEvent;

public final class SwipeMotion {
	private MotionEvent mDownAction;
	private MotionEvent mLastMotion;
	private MotionEvent mUpMotion;
	
	public SwipeMotion(MotionEvent downMotion) {
		this.mDownAction = downMotion;
		this.mLastMotion = downMotion;
		this.mUpMotion = null;
	}
	
	public MotionEvent getLastMotion() {
		return mLastMotion;
	}
	public void setLastMotion(MotionEvent lastMoveMotion) {
		this.mLastMotion = lastMoveMotion;
	}
	public MotionEvent getUpMotion() {
		return mUpMotion;
	}
	public void setUpMotion(MotionEvent upMotion) {
		this.mUpMotion = upMotion;
	}
	public MotionEvent getDownMotion() {
		return mDownAction;
	}
}
