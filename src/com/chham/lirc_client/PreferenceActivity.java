package com.chham.lirc_client;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import com.chham.lirc_client.components.BaseActivity;
import com.chham.lirc_client.fragments.SettingsFragment;

public class PreferenceActivity extends BaseActivity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_preferences);
		
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// Set activity properties and display preferences fragment
		setTitle(R.string.settings);
		
		getSupportFragmentManager()
			.beginTransaction()
			.add(R.id.fragment_container_prefs, new SettingsFragment())
			.commit();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				Intent intent = new Intent(this, MainListActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(intent);
				
				return true;
		}
		
		return super.onOptionsItemSelected(item);
	}
}
