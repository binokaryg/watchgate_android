package com.binokary.watchgate;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.firebase.messaging.FirebaseMessaging;


public class SettingsActivity extends AppCompatActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener,
        PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    private final String TAG = Constants.MAIN_TAG + "Settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsActivity.SettingsFragment())
                    .commit();
        }

        // Listener to reset the title when going back
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                setTitle(R.string.title_activity_settings);
            }
        });

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
        // Instantiate the new Fragment
        final Bundle args = pref.getExtras();
        final Fragment fragment = getSupportFragmentManager().getFragmentFactory().instantiate(
                getClassLoader(),
                pref.getFragment());
        fragment.setArguments(args);
        fragment.setTargetFragment(caller, 0);

        // Replace the existing Fragment with the new one
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.settings, fragment)
                .addToBackStack(null) // <--- THIS FIXES THE BACK BUTTON
                .commit();

        // Update the Title to match the clicked item (e.g., "General Settings")
        setTitle(pref.getTitle());

        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        // 1. Try to pop the fragment back stack (go back one screen inside Settings)
        if (getSupportFragmentManager().popBackStackImmediate()) {
            return true;
        }

        // 2. If stack is empty, go to MainActivity
        return super.onSupportNavigateUp();
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("switch_preference_notification".equals(key)) {
            final String topic = sharedPreferences.getString("instance_name", "none");
            boolean subscribe = sharedPreferences.getBoolean("switch_preference_notification", false);

            if (subscribe) {
                FirebaseMessaging.getInstance().subscribeToTopic(topic)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Subscribed to topic " + topic);
                                Toast.makeText(getApplicationContext(), "Subscribed to " + topic, Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "Unsubscribed from topic " + topic);
                                Toast.makeText(getApplicationContext(), "Unsubscribed from " + topic, Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        }
    }

    // --- Fragments ---

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_headers, rootKey);
        }
    }

    public static class GeneralPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_general, rootKey);

            // Input Type logic...
            EditTextPreference instanceName = findPreference("instance_name");
            if (instanceName != null) {
                instanceName.setOnBindEditTextListener(editText -> {
                    editText.setInputType(InputType.TYPE_CLASS_TEXT);
                    editText.setMaxLines(1);
                });
            }

            EditTextPreference countryCode = findPreference("country_code");
            if (countryCode != null) {
                countryCode.setOnBindEditTextListener(editText -> {
                    editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    editText.setMaxLines(1);
                });
            }
        }
    }

    public static class IntervalPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_interval, rootKey);
        }
    }

    public static class VariablePreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_variables, rootKey);
        }
    }

    public static class QueryPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_network, rootKey);
        }
    }

    public static class NotificationsPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_notifications, rootKey);
        }
    }

    public static class SMSPreferenceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.pref_smspacks, rootKey);
        }
    }
}