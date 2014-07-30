package com.chham.lirc_client;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import android.widget.SpinnerAdapter;
import com.chham.lirc.Remote;
import com.chham.lirc_client.components.BaseActivity;
import com.chham.lirc_client.components.EmbeddedFragment;
import com.chham.lirc_client.content.ActivityManager;
import com.chham.lirc_client.content.ActivityManager.Mode;
import com.chham.lirc_client.dialogs.TextPickerDialog;
import com.chham.lirc_client.fragments.ControlFragment;
import com.chham.lirc_client.fragments.InfoTextFragment;
import com.chham.lirc_client.fragments.MainListFragment;

import java.util.ArrayList;

public class MainListActivity extends BaseActivity implements
		MainListFragment.Callbacks, OnNavigationListener, TextPickerDialog.OnTextChosenListener
{
	private static final String KEY_NAVIGATION_INDEX = "nav_index";
	private static final String KEY_LIST_INDEX_ACTIVITIES = "list_index_activities";
	private static final String KEY_LIST_INDEX_DEVICES = "list_index_devices";
	private static final String KEY_LAST_TAG = "last_tag";
	
	private static final int INDEX_NAVITEMS_ACTIVITIES = 0;
	private static final int INDEX_NAVITEMS_DEVICES = 1;
	
	private boolean mTwoPane;
	private int navigationSelection = -1;
	private int listSelectionActivities = -1;
	private int listSelectionDevices = -1;
	private boolean isStopping;

	private ArrayAdapter<ActivityManager.Activity> listAdapter;
	private String lastTag;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// Initialize view
		setContentView(R.layout.activity_main_list);
		
		// Initialize navigation menu
		ActionBar actionbar = getActionBar();
		SpinnerAdapter spinner_adapter = ArrayAdapter.createFromResource(
				getApplicationContext(), R.array.navigation_targets,
				android.R.layout.simple_spinner_dropdown_item);
		actionbar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionbar.setDisplayShowTitleEnabled(false);
		actionbar.setListNavigationCallbacks(spinner_adapter, this);
		
		// Check for two-pane mode
		if (findViewById(R.id.fragment_container) != null)
		{
			MainListFragment listFragment = ((MainListFragment)getSupportFragmentManager().findFragmentById(R.id.navigation_list));
			listFragment.setActivateOnItemClick(true);
			mTwoPane = true;
		}
		
		// Restore view settings
		if (savedInstanceState == null)
		{
			if (realActivities.getActivities().size() != 0)
				actionbar.setSelectedNavigationItem(INDEX_NAVITEMS_ACTIVITIES);
			else
				actionbar.setSelectedNavigationItem(INDEX_NAVITEMS_DEVICES);
		}
		else
		{
			listSelectionActivities = savedInstanceState.getInt(KEY_LIST_INDEX_ACTIVITIES);
			listSelectionDevices = savedInstanceState.getInt(KEY_LIST_INDEX_DEVICES);
			lastTag = savedInstanceState.getString(KEY_LAST_TAG);
			
			navigationSelection = savedInstanceState.getInt(KEY_NAVIGATION_INDEX);
			actionbar.setSelectedNavigationItem(navigationSelection);
			
			loadContent(Mode.MODE_ACTIVITY);
			loadContent(Mode.MODE_DEVICE);
		}
	}
	
	@Override
	protected void onStart()
	{
		isStopping = false;
		super.onStart();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		if (!mTwoPane)
		{
			String orientation_prop = sharedPreferences.getString(BaseActivity.PREFERENCE_ROTATION_BEHAVIOR, "AUTO");
			if (orientation_prop.equals("PORTRAIT"))
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			else if (orientation_prop.equals("LANDSCAPE"))
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			else
				setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}
	}

	@Override
	protected void onStop()
	{
		isStopping = true;
		super.onStop();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		outState.putInt(KEY_LIST_INDEX_ACTIVITIES, listSelectionActivities);
		outState.putInt(KEY_LIST_INDEX_DEVICES, listSelectionDevices);
		outState.putInt(KEY_NAVIGATION_INDEX, navigationSelection);
		outState.putString(KEY_LAST_TAG, lastTag);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
       final MainListActivity this_activity = this;

		switch (navigationSelection)
		{
			case INDEX_NAVITEMS_ACTIVITIES:
				// add activity
				menu.add(R.string.add_activity)
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item)
						{
							TextPickerDialog dialog = new TextPickerDialog(this_activity, "", this_activity);
							dialog.promptActivityName(-1);
							
							return true;
						}
					});
				
				// manage macros
				menu.add(R.string.manage_macros)
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item)
						{
							Intent macroIntent = new Intent(this_activity, MacroListActivity.class);
							startActivity(macroIntent);
							
							return true;
						}
					});
				
				break;
				
			case INDEX_NAVITEMS_DEVICES:
				// query devices from server
				menu.add(R.string.query_remotes)
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item)
						{
							lircClient.queryRemotesVerbose();
							return true;
						}
					});
				
				break;
		}
		
		// menu item for settings
		MenuItem element = menu.add(getResources().getString(R.string.settings));
		element.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item)
			{
				Intent intent = new Intent(this_activity, PreferenceActivity.class);
				startActivity(intent);
				
				return true;
			}
		});
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId)
	{
		if (getSupportFragmentManager().findFragmentById(R.id.navigation_list) != null && navigationSelection != itemPosition)
		{
			navigationSelection = itemPosition;
			
			loadContent(ActivityManager.Mode.MODE_ACTIVITY);
			loadContent(ActivityManager.Mode.MODE_DEVICE);
			
			if (mTwoPane)
			{
				switch (navigationSelection)
				{
					case INDEX_NAVITEMS_ACTIVITIES:
						if (listSelectionActivities != -1)
							loadActivity(listSelectionActivities);
						
						break;
						
					case INDEX_NAVITEMS_DEVICES:
						if (listSelectionDevices != -1)
							loadDevice(listSelectionDevices);
						
						break;
				}
			}
			
			invalidateOptionsMenu();
		}
		
		return true;
	}
	
	@Override
	public void onListItemSelected(int index)
	{
		switch (navigationSelection)
		{
			// Activity in Activities selected
			case INDEX_NAVITEMS_ACTIVITIES:
				if (mTwoPane && listSelectionActivities == index)
					return;
				
				loadActivity(index);
				listSelectionActivities = index;
				
				break;
			
			// Remote in Remotes selected
			case INDEX_NAVITEMS_DEVICES:
				if (mTwoPane && listSelectionDevices == index)
					return;
				
				loadDevice(index);
				listSelectionDevices = index;
				
				break;
		}
	}
	
	@SuppressWarnings("rawtypes")
	private void startFragment(Class fragment, String title, Bundle arguments, String tag)
	{
		if (mTwoPane)
		{
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
				try {
					content = (EmbeddedFragment)fragment.newInstance();
					content.show();
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				if (arguments != null)
					content.setArguments(arguments);
				
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
			Intent fragmentIntent = new Intent(this, ContentActivity.class);
			fragmentIntent.putExtra(ContentActivity.KEY_FRAGMENT_CLASS,
					fragment.getName());
			fragmentIntent.putExtra(ContentActivity.KEY_TITLE, title);
			if (arguments != null)
				fragmentIntent.putExtras(arguments);
			startActivity(fragmentIntent);
		}
	}
	
	public void loadContent(ActivityManager.Mode desiredMode)
	{
		MainListFragment listFragment = ((MainListFragment)getSupportFragmentManager().findFragmentById(R.id.navigation_list));
		if (listFragment == null || navigationSelection == -1 || isStopping)
			return;
		
		// load activity info
		ArrayList<ActivityManager.Activity> activities = null;
		int res_no_items = 0;
		int res_select_item = 0;
		switch (navigationSelection)
		{
			case INDEX_NAVITEMS_ACTIVITIES:
				if (!desiredMode.equals(ActivityManager.Mode.MODE_ACTIVITY))
					return;
				
				activities = realActivities.getActivities();
				
				res_no_items = R.string.info_add_activity;
				res_select_item = R.string.info_select_activity;
				
				break;
				
			case INDEX_NAVITEMS_DEVICES:
				if (!desiredMode.equals(ActivityManager.Mode.MODE_DEVICE))
					return;

				activities = deviceActivities.getActivities();
				
				res_no_items = R.string.info_add_devices;
				res_select_item = R.string.info_select_device;
				
				break;
		}
		
		// update list view
		listAdapter = new ArrayAdapter<ActivityManager.Activity>(this,
				android.R.layout.simple_list_item_activated_1,
				android.R.id.text1, activities);
		listFragment.setListAdapter(listAdapter);
		
		// set selection in two-pane mode
		if (mTwoPane && !isStopping)
		{
			boolean show_info_text = true;
			
			switch (navigationSelection)
			{
				case INDEX_NAVITEMS_ACTIVITIES:
					if (listSelectionActivities != -1)
					{
						listFragment.setActivatedPosition(listSelectionActivities);
						show_info_text = false;
					}
					
					break;
					
				case INDEX_NAVITEMS_DEVICES:
					if (listSelectionDevices != -1)
					{
						listFragment.setActivatedPosition(listSelectionDevices);
						show_info_text = false;
					}
					
					break;
			}
			
			// possibly show info text in two-pane mode
			if (show_info_text)
			{
				int res_id;
				if (activities.size() == 0)
					res_id = res_no_items;
				else
					res_id = res_select_item;
				
				Bundle args = new Bundle();
				args.putInt(InfoTextFragment.KEY_TEXT, res_id);
				startFragment(InfoTextFragment.class, null, args, String.format("INFO:%d", res_id));
			}
		}
	}
	
	protected void loadActivity(int index)
	{
		Bundle args = new Bundle();
		args.putInt(ControlFragment.KEY_INDEX, index);
		args.putInt(ControlFragment.KEY_MODE, ActivityManager.Mode.MODE_ACTIVITY.ordinal());
		args.putBoolean(ControlFragment.KEY_TWOPANE, mTwoPane);
		startFragment(ControlFragment.class, realActivities.getActivity(index).getName(), args, String.format("ACTIVITY:%d", index));
	}
	
	protected void loadDevice(int index)
	{
		Bundle args = new Bundle();
		args.putInt(ControlFragment.KEY_INDEX, index);
		args.putInt(ControlFragment.KEY_MODE, ActivityManager.Mode.MODE_DEVICE.ordinal());
		args.putBoolean(ControlFragment.KEY_TWOPANE, mTwoPane);
		startFragment(ControlFragment.class, lircClient.getRemote(index).getCaption(), args, String.format("DEVICE:%d", index));
	}
	
	@Override
	public void onTextChosen(int id, String text)
	{
		if (id == -1)
		{
			ActivityManager.Activity new_item = realActivities.addActivity();
			new_item.setName(text);
			new_item.save();
			
			// make sure list content is updated
			listAdapter.notifyDataSetChanged();
			
			// set info text if the added activity has been the first one
			if (mTwoPane && realActivities.getActivities().size() == 1)
			{
				InfoTextFragment info_fragment = (InfoTextFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_container);
				switch (navigationSelection)
				{
					case INDEX_NAVITEMS_ACTIVITIES:
						info_fragment.setText(R.string.info_select_activity);
						break;
						
					case INDEX_NAVITEMS_DEVICES:
						info_fragment.setText(R.string.info_select_device);
						break;
				}
			}
		}
		// rename existing activity
		else
		{
			// change activity name
			ActivityManager.Activity activity = realActivities.getActivity(id);
			activity.setName(text);
			activity.save();
			
			// update UI
			listAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onShowListItemMenu(ContextMenu menu, View v, AdapterContextMenuInfo menuInfo)
	{
        final MainListActivity this_activity = this;

		switch (navigationSelection)
		{
			// Activities
			case INDEX_NAVITEMS_ACTIVITIES:
				// rename
				menu.add(R.string.menu_activity_rename)
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						
						@Override
						public boolean onMenuItemClick(MenuItem item)
						{
							int position = ((AdapterContextMenuInfo)item.getMenuInfo()).position;
							String caption = realActivities.getActivity(position).getName();


							TextPickerDialog picker = new TextPickerDialog(this_activity, caption, this_activity);
							picker.promptActivityName(position);
							
							return true;
						}
					});
				
				// delete
				menu.add(R.string.menu_activity_delete)
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item)
						{
							int position = ((AdapterContextMenuInfo)item.getMenuInfo()).position;
							
							if (mTwoPane)
							{
								// deselect current item and show info text
								MainListFragment list_fragment = ((MainListFragment)getSupportFragmentManager().findFragmentById(R.id.navigation_list));
								if (listSelectionActivities >= position)
								{
									listSelectionActivities = -1;
									list_fragment.setActivatedPosition(ListView.INVALID_POSITION);
									
									int res_id;
									if (getActivities().getActivities().size() == 0)
										res_id = R.string.info_add_activity;
									else
										res_id = R.string.info_select_activity;
								
									Bundle args = new Bundle();
									args.putInt(InfoTextFragment.KEY_TEXT, res_id);
									startFragment(InfoTextFragment.class, null, args, String.format("INFO:%d", res_id));
								}
								
								// remove fragments with a higher index
								Fragment control_fragment;
								for(int i=position; i<getActivities().getActivities().size(); i++)
								{
									control_fragment = getSupportFragmentManager().findFragmentByTag(String.format("ACTIVITY:%d", i));
									
									if (control_fragment != null)
									{
										getSupportFragmentManager()
											.beginTransaction()
											.remove(control_fragment)
											.commit();
									}
								}
							}
							
							realActivities.deleteActivity(position);
							listAdapter.notifyDataSetChanged();
							
							return true;
						}
					});
				
				break;
			
			// Devices
			case INDEX_NAVITEMS_DEVICES:
				// delete
				menu.add(R.string.menu_device_delete)
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item)
						{
							int position = ((AdapterContextMenuInfo)item.getMenuInfo()).position;
							
							if (mTwoPane)
							{
								// deselect current item and show info text
								MainListFragment list_fragment = ((MainListFragment)getSupportFragmentManager().findFragmentById(R.id.navigation_list));
								if (listSelectionDevices >= position)
								{
									listSelectionDevices = -1;
									list_fragment.setActivatedPosition(ListView.INVALID_POSITION);
									
									int res_id;
									if (getDeviceActivities().getActivities().size() == 0)
										res_id = R.string.info_add_devices;
									else
										res_id = R.string.info_select_device;
								
									Bundle args = new Bundle();
									args.putInt(InfoTextFragment.KEY_TEXT, res_id);
									startFragment(InfoTextFragment.class, null, args, String.format("INFO:%d", res_id));
								}
								
								// remove fragments with a higher index
								Fragment control_fragment;
								for(int i=position; i<getDeviceActivities().getActivities().size(); i++)
								{
									control_fragment = getSupportFragmentManager().findFragmentByTag(String.format("DEVICE:%d", i));
									
									if (control_fragment != null)
									{
										getSupportFragmentManager()
											.beginTransaction()
											.remove(control_fragment)
											.commit();
									}
								}
							}
							
							Remote remote = lircClient.getRemote(position);
							lircClient.removeRemote(remote);
							deviceActivities.deleteActivity(position);
							listAdapter.notifyDataSetChanged();
							
							return true;
						}
					});
				
				// toggle simulate
				Remote remote = lircClient.getRemote(menuInfo.position);
				menu.add(R.string.menu_device_simulate)
					.setCheckable(true)
					.setChecked(remote.getSimulate())
					.setOnMenuItemClickListener(new OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item)
						{
							int position = ((AdapterContextMenuInfo)item.getMenuInfo()).position;
							Remote remote = lircClient.getRemote(position);
							remote.setSimulate(!item.isChecked());
							dbHelper.saveClient(lircClient);
							
							return true;
						}
					});
				
				break;
		}
	}
	
	// LIRC callbacks
	@Override
	public void onRemoteAdded(Remote device)
	{
		super.onRemoteAdded(device);
		
		loadContent(ActivityManager.Mode.MODE_DEVICE);
	}

	@Override
	public void onRemoteCaptionChanged(Remote device, String old_caption)
	{
		super.onRemoteCaptionChanged(device, old_caption);
		
		loadContent(ActivityManager.Mode.MODE_DEVICE);
	}

	@Override
	public void onRemoteDeleted(Remote device)
	{
		super.onRemoteDeleted(device);
		
		loadContent(ActivityManager.Mode.MODE_DEVICE);
	}

	@Override
	public void onRemoteQueryComplete()
	{
		super.onRemoteQueryComplete();
		
		loadContent(ActivityManager.Mode.MODE_ACTIVITY);
		loadContent(ActivityManager.Mode.MODE_DEVICE);
	}

	@Override
	public void onRemoteVerboseQueryComplete()
	{
		super.onRemoteVerboseQueryComplete();
		
		loadContent(ActivityManager.Mode.MODE_DEVICE);
	}
}
