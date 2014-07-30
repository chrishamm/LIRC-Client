package com.chham.lirc_client;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;
import com.chham.lirc_client.components.BaseActivity;

public class ContentActivity extends BaseActivity
{
	public static final String KEY_FRAGMENT_CLASS = "fragment_class";
	public static final String KEY_TITLE = "title";
	
	@SuppressWarnings("rawtypes")
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_content);
		
		// Show the Up button in the action bar and set title
		ActionBar actionbar = getActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setTitle(getIntent().getStringExtra(KEY_TITLE));
	
		// Only create fragment if activity is actually created
		if (savedInstanceState == null)
		{
			// attempt to find fragment class
			Class fragment_class;
			try
			{
				fragment_class = Class.forName(getIntent().getStringExtra(KEY_FRAGMENT_CLASS));
			} catch (ClassNotFoundException e)
			{
				e.printStackTrace();
				return;
			}
			
			// create fragment instance
			Bundle fragment_args = getIntent().getExtras();
			Fragment content;
			try
			{
				content = (Fragment)fragment_class.newInstance();
			} catch (InstantiationException e) {
				e.printStackTrace();
				return;
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				return;
			}
			content.setArguments(fragment_args);
			
			// set arguments and launch fragment
			getSupportFragmentManager()
				.beginTransaction()
				.add(R.id.fragment_container, content)
				.commit();
		}
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
