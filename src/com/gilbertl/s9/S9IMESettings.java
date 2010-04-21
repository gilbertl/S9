package com.gilbertl.s9;

//import android.backup.BackupManager;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.PreferenceActivity;

public class S9IMESettings extends PreferenceActivity {
    //implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String SWIPE_SENSITIVITY_KEY = "swipe_sensitivity";
    
    private EditTextPreference mSwipeSensitivity;

    
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.prefs);
        mSwipeSensitivity = (EditTextPreference)
        	findPreference(SWIPE_SENSITIVITY_KEY);
        //getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(
        //        this);
    }

    /*
    @Override
    protected void onResume() {
        super.onResume();
        int autoTextSize = AutoText.getSize(getListView());
        if (autoTextSize < 1) {
            ((PreferenceGroup) findPreference(PREDICTION_SETTINGS_KEY))
                .removePreference(mQuickFixes);
        } else {
            mShowSuggestions.setDependency(QUICK_FIXES_KEY);
        }
    }
    */

    /*
    @Override
    protected void onDestroy() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
                this);
        super.onDestroy();
    }
    */

    /*
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        (new BackupManager(this)).dataChanged();
    }
    */
}
