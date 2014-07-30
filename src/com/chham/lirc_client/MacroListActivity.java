package com.chham.lirc_client;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import com.chham.lirc_client.components.BaseActivity;
import com.chham.lirc_client.components.EmbeddedFragment;
import com.chham.lirc_client.content.ActivityManager;
import com.chham.lirc_client.content.MacroManager;
import com.chham.lirc_client.content.MacroManager.Macro;
import com.chham.lirc_client.dialogs.TextPickerDialog;
import com.chham.lirc_client.dialogs.TextPickerDialog.OnTextChosenListener;
import com.chham.lirc_client.fragments.InfoTextFragment;
import com.chham.lirc_client.fragments.MacroContentFragment;
import com.chham.lirc_client.fragments.MainListFragment;

public class MacroListActivity extends BaseActivity implements MainListFragment.Callbacks, OnTextChosenListener
{
	private static final String KEY_SELECTED_MACRO = "selected_macro";
	private static final String KEY_LAST_TAG = "last_tag";
	
	private MacroListActivity activity;
	private boolean twoPane;
	private ArrayAdapter<MacroManager.Macro> listAdapter;
	
	private int selectedMacro;
	private String lastTag;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		activity = this;
		
		// set up view and restore values
		setContentView(R.layout.activity_main_list);
		if (savedInstanceState != null)
		{
			selectedMacro = savedInstanceState.getInt(KEY_SELECTED_MACRO);
			lastTag = savedInstanceState.getString(KEY_LAST_TAG);
		}
		
		// set up action bar
		ActionBar actionbar = getActionBar();
		actionbar.setDisplayHomeAsUpEnabled(true);
		actionbar.setTitle(R.string.manage_macros);
		
		// fill macro list
		MainListFragment listFragment = ((MainListFragment)getSupportFragmentManager().findFragmentById(R.id.navigation_list));
		listAdapter = new ArrayAdapter<MacroManager.Macro>(this,
				android.R.layout.simple_list_item_activated_1,
				android.R.id.text1, macroManager.getMacros());
		listFragment.setListAdapter(listAdapter);
		
		// apply logic for two-pane mode
		if (findViewById(R.id.fragment_container) != null)
		{
			listFragment.setActivateOnItemClick(true);
			twoPane = true;
			
			// set info text
			if (savedInstanceState == null)
			{
				int res_id = macroManager.getMacros().size() != 0 ? R.string.info_select_macro : R.string.info_add_macro;
				lastTag = String.format("INFO:%d", res_id);
				
				Bundle args = new Bundle();
				args.putInt(InfoTextFragment.KEY_TEXT, res_id);
				
				InfoTextFragment info_fragment = new InfoTextFragment();
				info_fragment.setArguments(args);
				getSupportFragmentManager()
					.beginTransaction()
					.add(R.id.fragment_container, info_fragment, lastTag)
					.commit();
			}
		}
		
		// create options menu
		invalidateOptionsMenu();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// add macro
		menu.add(R.string.menu_add_macro)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					TextPickerDialog dialog = new TextPickerDialog(activity, "", activity);
					dialog.promptMacroName(-1);
					
					return true;
				}
			})
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
		
		return true;
	}
	
	@Override
	protected void onStart()
	{
		if (!twoPane && macroManager.getMacros().size() == 0)
			Toast.makeText(this, getResources().getString(R.string.info_add_macro), Toast.LENGTH_LONG).show();
		
		super.onStart();
	}

	@Override
	protected void onPause()
	{
		if (twoPane)
			dbHelper.saveMacroManager(macroManager);

		super.onPause();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putInt(KEY_SELECTED_MACRO, selectedMacro);
		outState.putString(KEY_LAST_TAG, lastTag);
		super.onSaveInstanceState(outState);
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
	
	@Override
	public void onListItemSelected(int index)
	{
		selectedMacro = index;
		
		Bundle args = new Bundle();
		args.putInt(MacroContentFragment.KEY_MACRO, index);
		
		if (twoPane)
		{
			String tag = "MACRO:" + String.valueOf(index);
			EmbeddedFragment content = (EmbeddedFragment)getSupportFragmentManager().findFragmentByTag(tag);
			
			if (content != null && content.isAdded())
			{
				// change visibilies
				EmbeddedFragment last_fragment = (EmbeddedFragment)getSupportFragmentManager().findFragmentByTag(lastTag);
				last_fragment.hide();
				content.show();
				
				// reactivate saved fragment
				getSupportFragmentManager()
					.beginTransaction()
					.hide(last_fragment)
					.show(content)
					.commit();
				
				lastTag = tag;
			}
			else
			{
				// create new fragment
				content = new MacroContentFragment();
				content.show();
				content.setArguments(args);
				
				// hide last fragment
				FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
				if (lastTag != null)
				{
					EmbeddedFragment last_fragment = (EmbeddedFragment)getSupportFragmentManager().findFragmentByTag(lastTag);
					if (last_fragment != null)
					{
						last_fragment.hide();
						transaction.hide(last_fragment);
					}
				}
				
				// show new one
				transaction
					.add(R.id.fragment_container, content, tag)
					.commit();
				
				lastTag = tag;
			}
		}
		else
		{
			Macro macro = macroManager.getMacros().get(index);
			
			Intent intent = new Intent(this, MacroContentActivity.class);
			intent.putExtra(MacroContentActivity.KEY_TITLE, macro.getName());
			intent.putExtra(MacroContentFragment.KEY_MACRO, index);
			startActivity(intent);
		}
	}

	@Override
	public void onShowListItemMenu(ContextMenu menu, View v, AdapterContextMenuInfo menuInfo)
	{
		// rename macro
		menu.add(R.string.menu_rename_macro)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
					Macro macro = macroManager.getMacros().get(info.position);
					
					TextPickerDialog dialog = new TextPickerDialog(activity, macro.getName(), activity);
					dialog.promptMacroName(info.position);
					
					return true;
				}
			});
			
		// delete macro
		menu.add(R.string.menu_delete_macro)
		.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item)
			{
				AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
				int position = info.position;
				Macro macro = macroManager.getMacros().get(position);
				
				if (twoPane)
				{
					// deselect current item and show info text
					MainListFragment list_fragment = ((MainListFragment)getSupportFragmentManager().findFragmentById(R.id.navigation_list));
					if (selectedMacro >= position)
					{
						selectedMacro = -1;
						list_fragment.setActivatedPosition(ListView.INVALID_POSITION);
						
						int res_id = macroManager.getMacros().size() != 0 ? R.string.info_select_macro : R.string.info_add_macro;
						String tag = "INFO:" + String.valueOf(res_id);
						
						Bundle args = new Bundle();
						args.putInt(InfoTextFragment.KEY_TEXT, res_id);
						
						InfoTextFragment fragment = (InfoTextFragment)getSupportFragmentManager().findFragmentByTag(tag);
						if (fragment == null)
						{
							fragment = new InfoTextFragment();
							fragment.setArguments(args);
							
							getSupportFragmentManager()
								.beginTransaction()
								.add(R.id.fragment_container, fragment, tag)
								.commit();
						}
						else
						{
							Fragment last_fragment = getSupportFragmentManager().findFragmentByTag(lastTag);
							
							getSupportFragmentManager()
								.beginTransaction()
								.hide(last_fragment)
								.show(fragment)
								.commit();
						}
						
						lastTag = tag;
					}
					
					// remove fragments with a higher index
					Fragment control_fragment;
					for(int i=position; i<getActivities().getActivities().size(); i++)
					{
						control_fragment = getSupportFragmentManager().findFragmentByTag(String.format("MACRO:%d", i));
						
						if (control_fragment != null)
						{
							getSupportFragmentManager()
								.beginTransaction()
								.remove(control_fragment)
								.commit();
						}
					}
				}
				
				// delete macro and save changes
				macroManager.deleteMacro(macro);
				dbHelper.saveMacroManager(macroManager);
				
				// remove deleted macro from activities
				realActivities.validateMacros(macroManager);
				for(ActivityManager.Activity activity : realActivities.getActivities())
					activity.save();
				
				// update UI
				listAdapter.notifyDataSetChanged();
				
				return true;
			}
		});
	}

	@Override
	public void onTextChosen(int id, String text)
	{
		if (id == -1)
			macroManager.addMacro(text);
		else
		{
			Macro macro = macroManager.getMacros().get(id);
			macro.setName(text);
		}
		
		dbHelper.saveMacroManager(macroManager);
		listAdapter.notifyDataSetChanged();
	}
}
