package com.chham.lirc_client.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import com.chham.lirc.LIRC_Client;
import com.chham.lirc_client.R;
import com.chham.lirc_client.components.BaseActivity;
import com.chham.lirc_client.components.PreferenceListFragment;


public class SettingsFragment extends PreferenceListFragment implements
	SharedPreferences.OnSharedPreferenceChangeListener,
	PreferenceListFragment.OnPreferenceAttachedListener
{
	public SettingsFragment() { }

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(BaseActivity.SHARED_PREFERENCES_NAME);
		addPreferencesFromResource(R.layout.fragment_settings);
		preferenceManager.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onPreferenceAttached(PreferenceScreen root, int xmlId)
	{
		//if (root == null)
		//	return;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (key.equals(BaseActivity.PREFERENCE_IP_ADDRESS))
		{
			String string_uri = sharedPreferences.getString(BaseActivity.PREFERENCE_IP_ADDRESS, "").trim();
			if (!string_uri.isEmpty())
			{
				Uri uri = Uri.parse("lirc://" + string_uri);
				int port = uri.getPort();
				
				LIRC_Client client = BaseActivity.getLircClient();
				client.connect(uri.getHost(), port != -1 ? port : 8765);
			}
		}
		else if (key.equals(BaseActivity.PREFERENCE_ROTATION_BEHAVIOR))
		{
			Activity parent = getActivity();
			if (parent != null)
			{
				String orientation_prop = sharedPreferences.getString(BaseActivity.PREFERENCE_ROTATION_BEHAVIOR, "AUTO");
				if (orientation_prop.equals("PORTRAIT"))
					parent.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				else if (orientation_prop.equals("LANDSCAPE"))
					parent.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				else
					parent.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
			}
		}
		else if (key.equals(BaseActivity.PREFERENCE_SIMULATE_DELAY))
		{
			LIRC_Client client = BaseActivity.getLircClient();
			int delay_ms = sharedPreferences.getInt(BaseActivity.PREFERENCE_SIMULATE_DELAY, 200);
			client.setSimulateDelay(delay_ms);
		}
	}
}
