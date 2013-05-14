package org.ncrmnt.metronet;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.MenuItem;

public class Prefs extends PreferenceActivity {

    /**
 * Checks that a preference is a valid numerical value
 */
OnPreferenceChangeListener numberCheckListener = new OnPreferenceChangeListener() {
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		// TODO Auto-generated method stub
		return false;
	}
};

private boolean numberCheck(Object newValue) {
    if( !newValue.toString().equals("")  &&  newValue.toString().matches("\\d*") ) {
        return true;
    }
    else {
        //Toast.makeText(ActivityUserPreferences.this, newValue+" "+getResources().getString(R.string.is_an_invalid_number), Toast.LENGTH_SHORT).show();
        return false;
    }
}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//get a handle on preferences that require validation
		//Preference npings = getPreferenceScreen().findPreference("npings");
		Preference pdelay = getPreferenceScreen().findPreference("pdelay");
		//Validate numbers only
		//npings.setOnPreferenceChangeListener(numberCheckListener);
		pdelay.setOnPreferenceChangeListener(numberCheckListener);
		addPreferencesFromResource(R.xml.prefs);
	}
}
