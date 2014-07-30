package com.chham.lirc_client;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import com.chham.lirc_client.components.BaseActivity;
import com.chham.lirc_client.fragments.MacroContentFragment;

public class MacroContentActivity extends BaseActivity
{
	public static final String KEY_TITLE = "title";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// set up view
		setContentView(R.layout.activity_macro_content);
		
		// set up action bar
		ActionBar actionbar = getActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setTitle(getIntent().getStringExtra(KEY_TITLE));
		
		// copy arguments from intent
		Bundle args = new Bundle();
		args.putInt(MacroContentFragment.KEY_MACRO, getIntent().getIntExtra(MacroContentFragment.KEY_MACRO, -1));
		
		// launch fragment
		MacroContentFragment macro_fragment = new MacroContentFragment();
		macro_fragment.setArguments(args);
		
		getSupportFragmentManager()
			.beginTransaction()
			.replace(R.id.fragment_macro_container, macro_fragment)
			.commit();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case android.R.id.home:
				Intent intent = new Intent(this, MacroListActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
				startActivity(intent);
				
				return true;
		}
		
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause()
	{
		dbHelper.saveMacroManager(macroManager);
		super.onPause();
	}
}
