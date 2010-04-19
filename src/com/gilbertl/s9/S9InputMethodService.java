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

import java.util.ArrayList;
import java.util.List;

import android.graphics.PointF;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

public class S9InputMethodService extends InputMethodService 
        implements KeyboardView.OnKeyboardActionListener, View.OnTouchListener {
	
	static final String TAG = "S9InputMethodService";
    static final boolean DEBUG = false;
    
    private KeyboardView mInputView;
    private CandidateView mCandidateView;
    private CompletionInfo[] mCompletions;
    private List<String> mSuggestions;
    private Suggest mSuggest;
    
    private StringBuilder mComposing = new StringBuilder();
    private WordComposer mWord = new WordComposer();
    private boolean mPredictionOn;
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private long mMetaState;

    private S9Keyboard mDefaultKeyboard;
    private S9Keyboard mShiftedKeyboard;
    
    private S9Keyboard mCurKeyboard;
    
    private String mWordSeparators;
    
    private S9KeyMotion[] mS9KeyMotions;
    
    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        super.onCreate();
        initSuggest();
        mWordSeparators = getResources().getString(R.string.word_separators);
    }
    
    private void initSuggest() {
    	// the reason the dict file is .png is we need to trick the android
    	// into not compressing it. This is because we need to pass file
    	// descriptor from Java to native, and this is the only way to do it
        mSuggest = new Suggest(this, R.raw.en_dict);
        /*
        mSuggest.setCorrectionMode(mCorrectionMode);
        mUserDictionary = new UserDictionary(this);
        mContactsDictionary = new ContactsDictionary(this);
        mAutoDictionary = new AutoDictionary(this);
        mSuggest.setUserDictionary(mUserDictionary);
        mSuggest.setContactsDictionary(mContactsDictionary);
        mSuggest.setAutoDictionary(mAutoDictionary);
        */
        mSuggest.setCorrectionMode(Suggest.CORRECTION_FULL);
    }
    
    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        if (mDefaultKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mDefaultKeyboard = new S9Keyboard(this, R.xml.s9);
        mShiftedKeyboard = new S9Keyboard(this, R.xml.s9_shifted);
        mShiftedKeyboard.setShifted(true);
    }
    
    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
    	final int maxMultiTouchEvents = 256;
    	mS9KeyMotions = new S9KeyMotion[maxMultiTouchEvents];
    	
        mInputView = (KeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        //mInputView.setPreviewEnabled(false);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setOnTouchListener(this);
        mInputView.setKeyboard(mDefaultKeyboard);
        return mInputView;
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override public View onCreateCandidatesView() {
        mCandidateView = new CandidateView(this);
        mCandidateView.setService(this);
        return mCandidateView;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
        mWord.reset();
        updateCandidates();
        
        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }
        
        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;
        
        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType&EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:      
            case EditorInfo.TYPE_CLASS_PHONE:      
            case EditorInfo.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mDefaultKeyboard;
                mPredictionOn = true;
                
                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType &  EditorInfo.TYPE_MASK_VARIATION;
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }
                
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS 
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_URI
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    //mPredictionOn = false;
                	// it's nice to be able to see what's being typed
                }
                
                if ((attribute.inputType&EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    //mPredictionOn = false;
                	// it's nice to see what's being typed
                    mCompletionOn = isFullscreenMode();
                }
                
                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;
                
            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mDefaultKeyboard;
                updateShiftKeyState(attribute);
        }
        
        // Update the label on the enter key, depending on what the application
        // says it will do.
        mDefaultKeyboard.setImeOptions(getResources(), attribute.imeOptions);
        mShiftedKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();
        
        // Clear current composing text and candidates.
        mComposing.setLength(0);
        mWord.reset();
        updateCandidates();
        
        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);
        
        mCurKeyboard = mDefaultKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }
    
    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        mInputView.setKeyboard(mCurKeyboard);
        mInputView.closing();
    }
    
    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }
            
            List<String> stringList = new ArrayList<String>();
            for (int i=0; i<(completions != null ? completions.length : 0); i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true);
        }
    }
    
    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;
                
            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;
                
            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;
                
            default:
            	break;
        }
        
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConn) {
    	commitText(inputConn, mComposing, mComposing.length());
    }
    
    /**
     * Commits a certain char sequence to editor.
     */
    private void commitText(
    		InputConnection inputConn, CharSequence text, int textLength) {
        if (mComposing.length() > 0) {
            inputConn.commitText(text, textLength);
            mComposing.setLength(0);
            mWord.reset();
            updateCandidates();
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null 
                && mInputView != null && mCurKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            useShiftedKeyboard(caps != 0);

        }
    }
    
    private void useShiftedKeyboard(boolean useShifted) {
    	if (useShifted) {
	    	mCurKeyboard = mShiftedKeyboard;
	    	mInputView.setKeyboard(mShiftedKeyboard);
    	} else {
    		mCurKeyboard = mDefaultKeyboard;
    		mInputView.setKeyboard(mDefaultKeyboard);
    	}
    }
    
    private void toggleKeyboard() {
    	useShiftedKeyboard(!isShifted());
    }
    
    private boolean isShifted() {
    	return mCurKeyboard == mShiftedKeyboard;
    }
    
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }
    
    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection()
                    .commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener

    public void onKey(int primaryCode, int[] keyCodes) {
    	handleKey(primaryCode);
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
        	commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private void updateCandidates() {
        if (!mCompletionOn) {
            if (mComposing.length() > 0) {
            	List<CharSequence> suggestions =
            		mSuggest.getSuggestions(mInputView, mWord, false);
            	Log.d(TAG, "got suggestions: " + suggestions);
            	mSuggestions = new ArrayList<String>(suggestions.size());
            	for (CharSequence cs : suggestions) {
            		mSuggestions.add(cs.toString());
            	}
            	
            	boolean typedWordValid = mSuggest.isValidWord(mComposing) ||
                 (mWord.isCapitalized() &&
                	mSuggest.isValidWord(mComposing.toString().toLowerCase()));
                
                setSuggestions(mSuggestions, true, typedWordValid);
            } else {
                setSuggestions(null, false, false);
            }
        }
    }
    
    public void setSuggestions(List<String> suggestions, boolean completions,
            boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        } else if (isExtractViewShown()) {
            setCandidatesViewShown(true);
        }
        
        if (mCandidateView != null) {
            mCandidateView.setSuggestions(
            		suggestions, completions, typedWordValid);
        }
    }
    
    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            mWord.deleteLast();
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates();
        } else if (length > 0) {
            commitText(getCurrentInputConnection(), "", 0);
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }      
        toggleKeyboard();
    }
    
    private void handleCharacter(int primaryCode) {
        if (mPredictionOn) {
            if (isShifted() && mComposing.length() == 0) {
            	mWord.setCapitalized(true);
                int [] adjCodes = {primaryCode, Character.toLowerCase(primaryCode)};
        		mWord.add(Character.toLowerCase(primaryCode), adjCodes);
            } else {
                int [] adjCodes = {primaryCode};
            	mWord.add(primaryCode, adjCodes);
            }
            mComposing.append((char) primaryCode);
            getCurrentInputConnection().setComposingText(mComposing, 1);
            updateCandidates();
        } else {
            getCurrentInputConnection().commitText(
                    String.valueOf((char) primaryCode), 1);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    private void handleClose() {
    	commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }
    
    private String getWordSeparators() {
        return mWordSeparators;
    }
    
    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char)code));
    }

    public void pickDefaultCandidate() {
        pickSuggestionManually(0);
    }
    
    public void pickSuggestionManually(int index) {
        if (mCompletionOn && mCompletions != null && index >= 0
                && index < mCompletions.length) {
            CompletionInfo ci = mCompletions[index];
            getCurrentInputConnection().commitCompletion(ci);
            if (mCandidateView != null) {
                mCandidateView.clear();
            }
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (mComposing.length() > 0) {
        	String s = mSuggestions.get(index);
        	if (mWord.isCapitalized()) {
        		s = s.substring(0, 1).toUpperCase() + s.substring(1);
        	}
        	commitText(getCurrentInputConnection(), s, s.length());
        }
    }
    
    public void swipeRight() {
    }
    
    public void swipeLeft() {
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }
    
    public void onPress(int primaryCode) {
    }
    
    public void onRelease(int primaryCode) {
    }

	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		int actionCode= action & MotionEvent.ACTION_MASK;
		int pointerId = -1;
		int pointerIdx = -1;
		
		switch (actionCode) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_UP:
				assert event.getPointerCount() == 1;
				pointerIdx = 0;
				pointerId = event.getPointerId(pointerIdx);
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_POINTER_UP:
				pointerId = action >> MotionEvent.ACTION_POINTER_ID_SHIFT;
				pointerIdx = event.findPointerIndex(pointerId);
				break;
			default:
				return false;
		}
		
		if (pointerId == -1 && pointerIdx == -1) {
			// not going to handle any non-down/up touches
			return false;
		}
		
		S9KeyMotion s9km;
		switch (actionCode) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				assert mS9KeyMotions[pointerId] == null;
				PointF downPoint =
					new PointF(event.getX(pointerIdx), event.getY(pointerIdx));
				Key keyPressed = findKey(downPoint);
		    	s9km = new S9KeyMotion(downPoint, keyPressed);
	    		mS9KeyMotions[pointerId] = s9km;
	    		if (keyPressed != null && keyPressed.repeatable) {
	    			// let KeyboardView handle repeatable keys
	    			return false;
	    		}
				return true;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				s9km = mS9KeyMotions[pointerId];
				assert s9km != null;
				mS9KeyMotions[pointerId] = null;
				if (s9km.getKey() != null) {
					if (s9km.getKey().repeatable) {
		    			// let KeyboardView handle repeatable keys
						return false;
					}
					PointF upPoint = new PointF(
							event.getX(pointerIdx), event.getY(pointerIdx));
					int motion = s9km.calcMotion(upPoint);
					int code = s9km.getKey().codes[motion];
		    		handleKey(code);
				}
	    		return true;
			default:
				break;
		}
		
		return false;
	}
	
	private Key findKey(PointF pt) {
		int [] nearbyKeyIndices =
			mCurKeyboard.getNearestKeys((int) pt.x, (int) pt.y);
		int l = nearbyKeyIndices.length;
		List<Key> keys = mCurKeyboard.getKeys();
		for (int i = 0; i < l; i++) {
			Key key = keys.get(nearbyKeyIndices[i]);
			if (key.isInside((int) pt.x, (int) pt.y)) {
				return key;
			}
		}		
		return null;
	}
	
	private void handleKey(int code) {
        if (isWordSeparator(code)) {
            // Handle separator
        	Log.d(TAG, "handling word seperator");
            if (mComposing.length() > 0) {
                commitTyped(getCurrentInputConnection());
            }
            sendKey(code);
            updateShiftKeyState(getCurrentInputEditorInfo());
        } else if (code == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (code == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (code == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
        } else if (code == S9KeyboardView.KEYCODE_OPTIONS) {
            // Show a menu or somethin'
        } else if (code == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {
        	assert false;
        } else if (code == S9Keyboard.KEYCODE_NULL) {
        	// do nothing
        } else {
            handleCharacter(code);
        }
	}

}
