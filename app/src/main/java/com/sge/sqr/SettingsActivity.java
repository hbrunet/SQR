package com.sge.sqr;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by hbrunet on 22/4/2016.
 */
public class SettingsActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference_settings);
    }
}
