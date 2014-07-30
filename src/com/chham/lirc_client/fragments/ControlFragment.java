package com.chham.lirc_client.fragments;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.view.*;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;
import com.chham.lirc.BaseCommand;
import com.chham.lirc.Remote;
import com.chham.lirc_client.R;
import com.chham.lirc_client.components.BaseActivity;
import com.chham.lirc_client.components.EmbeddedFragment;
import com.chham.lirc_client.content.ActivityManager;
import com.chham.lirc_client.content.ActivityManager.Mode;
import com.chham.lirc_client.dialogs.CommandManagerDialog;
import com.chham.lirc_client.dialogs.SolveIssuesDialog;

import java.util.ArrayList;

public class ControlFragment extends EmbeddedFragment implements CommandManagerDialog.OnCommandManagerDialogClosedListener
{
	public static final String KEY_INDEX = "index";
	public static final String KEY_MODE = "mode";
	public static final String KEY_TWOPANE = "two_pane";
	
	private static final String KEY_EDIT_MODE = "edit_mode";
	
	private class PageAdapter extends FragmentStatePagerAdapter
	{
		private SparseArray<Fragment> fragments;
		
		public PageAdapter(FragmentManager fm)
		{
			super(fm);
			fragments = new SparseArray<Fragment>();
		}

		@Override
		public Fragment getItem(int position)
		{
			PageFragment fragment;
			Bundle args = new Bundle();
			args.putInt(PageFragment.KEY_INDEX, position);
			args.putInt(PageFragment.KEY_COLS, numCols);
			args.putInt(PageFragment.KEY_ROWS, numRows);
			args.putInt(PageFragment.KEY_ACTIVITY_INDEX, activityIndex);
			args.putInt(PageFragment.KEY_ACTIVITY_MODE, mode.ordinal());
			args.putBoolean(PageFragment.KEY_EDIT_MODE, isEditing);
			
			fragment = new PageFragment();
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount()
		{
			return activity.getPages();
		}
		
		@Override
	    public int getItemPosition(Object item) {
	        if (fragments.indexOfValue((Fragment)item) == -1)
	        	return POSITION_NONE;
	        
	        return super.getItemPosition(item);
	    }
		
		@Override
		public Object instantiateItem(ViewGroup container, int position)
		{
			Fragment fragment = (Fragment)super.instantiateItem(container, position);
			fragments.put(position, fragment);
			return fragment;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object)
		{
			fragments.delete(position);
			super.destroyItem(container, position, object);
		}
		
		public void addPage()
		{
			// add new page
			activity.setPages(activity.getPages() +1);
			activity.save();

			// recalculate data
			pageAdapter.notifyDataSetChanged();
		}

		public void deleteLastPage()
		{
			int index = activity.getPages() -1;
			
			// delete activity page (data model)
			activity.setPages(index);
			activity.save();
			
			// delete activity page (UI)
			if (index == pager.getCurrentItem())
				pager.setCurrentItem(index -1);
			fragments.delete(index);
			
			// recalculate data
			notifyDataSetChanged();
		}

		public void setEditMode(boolean is_editing)
		{
			PageFragment page;
			for(int i=0; i<fragments.size(); i++)
			{
				page = (PageFragment)fragments.get(fragments.keyAt(i));
				if (page != null)
					page.setEditMode(is_editing);
			}
		}
	}
	
	private ViewPager pager;
	private PageAdapter pageAdapter;
	
	private int numCols, numRows;
	private ActivityManager.Mode mode;
	private int activityIndex;
	
	private ActivityManager.Activity activity;
	
	private MenuItem onButton;
	private MenuItem offButton;
	private MenuItem helpButton;
	private MenuItem deleteLastPageItem;
	
	private boolean isEditing;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// load content
		Bundle args = getArguments();
		mode = ActivityManager.Mode.values()[args.getInt(KEY_MODE)];
		activityIndex = args.getInt(KEY_INDEX);
		
		// get activity
		switch (mode)
		{
			case MODE_ACTIVITY:
				activity = BaseActivity.getActivities().getActivity(activityIndex);
				break;
				
			case MODE_DEVICE:
				activity = BaseActivity.getDeviceActivities().getActivity(activityIndex);
				break;
		}
		if (activity == null)
			return;
		
		// options menu
		setHasOptionsMenu(true);
		
		// get grid size
		SharedPreferences prefs = BaseActivity.getSharedPreferences();
		numCols = prefs.getInt(BaseActivity.PREFERENCE_GRID_COLS, getResources().getInteger(R.integer.default_grid_cols));
		numRows = prefs.getInt(BaseActivity.PREFERENCE_GRID_ROWS, getResources().getInteger(R.integer.default_grid_rows));
		
		// restore state
		if (savedInstanceState != null)
			isEditing = savedInstanceState.getBoolean(KEY_EDIT_MODE);
		else
			isEditing = (activity.getButtons().size() == 0);
		getActivity().invalidateOptionsMenu();
		
		// create page adapter
		if (args.getBoolean(KEY_TWOPANE))
			pageAdapter = new PageAdapter(getChildFragmentManager());
		else
			pageAdapter = new PageAdapter(getFragmentManager());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		// inflate view
		View result = inflater.inflate(R.layout.fragment_control, container, false);
		
		if (activity != null)
		{
			// create pages
			pager = (ViewPager)result.findViewById(R.id.content_pager);
			pager.setAdapter(pageAdapter);
		}
		
		return result;
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		// Edit mode
		menu.add(R.string.menu_edit_mode)
			.setCheckable(true)
			.setChecked(isEditing)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					setEditMode(!isEditing);
					return true;
				}
			});
		
		if (mode == Mode.MODE_ACTIVITY)
		{
			// On button
			boolean on_enabled = (activity.getInitCommands().size() != 0) && (!BaseActivity.getSharedPreferences().getBoolean("auto_switch", true) || activity != BaseActivity.getStartedActivity());
			Drawable on_icon = getResources().getDrawable(R.drawable.ic_menu_on);
			if (!on_enabled)
				on_icon.mutate().setAlpha(70);
			onButton = menu.add(R.string.on)
				.setIcon(on_icon)
				.setEnabled(on_enabled)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item)
					{
						startActivity();
						return true;
					}
				});
			onButton.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);

			// Off button
			boolean off_enabled = (activity.getStopCommands().size() != 0) && (!BaseActivity.getSharedPreferences().getBoolean("auto_switch", true) || BaseActivity.getStartedActivity() == activity);
			Drawable off_icon = getResources().getDrawable(R.drawable.ic_menu_off);
			if (!off_enabled)
				off_icon.mutate().setAlpha(70);
			offButton = menu.add(R.string.off)
				.setIcon(off_icon)
				.setEnabled(off_enabled)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item)
					{
						stopActivity();
						return true;
					}
				});
			offButton.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			
			// Help button
			boolean help_enabled = (BaseActivity.getStartedActivity() == activity && activity.getInitCommands().size() != 0) || (BaseActivity.getPreviousActivity() == activity && activity.getStopCommands().size() != 0);
			Drawable help_icon = getResources().getDrawable(R.drawable.ic_menu_help);
			if (!help_enabled)
				help_icon.mutate().setAlpha(70);
			helpButton = menu.add(R.string.help)
				.setIcon(help_icon)
				.setEnabled(help_enabled)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item)
					{
						solveIssues();
						return true;
					}
				});
			helpButton.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
			
			// Set init commands
			menu.add(R.string.menu_init_commands)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item)
					{
						showCommandManagerDialog(true);
						return true;
					}
				});
			
			// Set stop commands
			menu.add(R.string.menu_stop_commands)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item)
					{
						showCommandManagerDialog(false);
						return true;
					}
				});
		}
		
		// Add page
		menu.add(R.string.menu_add_page)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					deleteLastPageItem.setEnabled(true);
					pageAdapter.addPage();
					
					Toast.makeText(getActivity(), getResources().getString(R.string.info_page_added), Toast.LENGTH_SHORT).show();
					return true;
				}
			});
		
		// Delete last page
		deleteLastPageItem = menu.add(R.string.menu_delete_last_page)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					if (activity.getPages() > 1)
					{
						deleteLastPageItem.setEnabled(activity.getPages() > 2);
						pageAdapter.deleteLastPage();
						
						Toast.makeText(getActivity(), getResources().getString(R.string.info_last_page_deleted), Toast.LENGTH_SHORT).show();
						return true;
					}
					
					return false;
				}
			})
			.setEnabled(activity.getPages() > 1);
		
		super.onCreateOptionsMenu(menu, inflater);
	}
	
	@Override
	public void onStart()
	{
		onFragmentActivated();
		
		super.onStart();
	}
	
	@Override
	public void onHiddenChanged(boolean hidden)
	{
		if (!hidden && !isFragmentHidden())
			onFragmentActivated();
		
		super.onHiddenChanged(hidden);
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putBoolean(KEY_EDIT_MODE, isEditing);
		super.onSaveInstanceState(outState);
	}
	
	private void onFragmentActivated()
	{
		// show info text
		if (activity.getButtons().size() == 0 && !isFragmentHidden())
			Toast.makeText(getActivity(), getResources().getString(R.string.info_use_tiles), Toast.LENGTH_SHORT).show();		
	}
	
	private void startActivity()
	{
		ArrayList<Remote.Command> worker_queue;
		BaseActivity base_activity = (BaseActivity)getActivity();
		
		// check whether auto-switching is enabled
		ActivityManager.Activity current_activity = BaseActivity.getStartedActivity();
		if (current_activity != null && BaseActivity.getSharedPreferences().getBoolean(BaseActivity.PREFERENCE_AUTO_SWITCH, true) && current_activity != activity)
		{
			worker_queue = ActivityManager.makeWorkerQueue(current_activity.getStopCommands(), activity.getInitCommands());
			base_activity.showActivityDialog(BaseActivity.ActivityProgressMode.MODE_SWITCHING, worker_queue.size());
		}
		else
		{
			worker_queue = activity.getInitCommands();
			base_activity.showActivityDialog(BaseActivity.ActivityProgressMode.MODE_STARTING, worker_queue.size());
		}
		
		// send all commands
		int delay_ms = BaseActivity.getSharedPreferences().getInt(BaseActivity.PREFERENCE_ACTIVITY_DELAY, 750);
		for(BaseCommand baseCommand : worker_queue)
		{
			if (baseCommand != null && !baseCommand.send(delay_ms))
				return;
		}
		
		// set starting activity
		BaseActivity.setStartingActivity(activity);
	}
	
	private void stopActivity()
	{
		// show progress dialog
		ArrayList<Remote.Command> worker_queue = activity.getStopCommands();
		BaseActivity base_activity = (BaseActivity)getActivity();
		base_activity.showActivityDialog(BaseActivity.ActivityProgressMode.MODE_STOPPING, worker_queue.size());
		
		// send all commands
		int delay_ms = BaseActivity.getSharedPreferences().getInt(BaseActivity.PREFERENCE_ACTIVITY_DELAY, 750);
		for(BaseCommand baseCommand : worker_queue)
		{
			if (!baseCommand.send(delay_ms))
				return;
		}
		
		// set starting activity
		BaseActivity.setStartingActivity(null);
	}
	
	private void solveIssues()
	{
		ActivityManager.Activity activity = BaseActivity.getStartedActivity();
		if (activity == null)
		{
			// activity has been stopped; get last stop commands
			activity = BaseActivity.getPreviousActivity();
			if (activity != null)
			{
				SolveIssuesDialog dialog = new SolveIssuesDialog(getActivity(), activity.getStopCommands());
				dialog.show();
			}
		}
		else
		{
			ArrayList<Remote.Command> command_list = new ArrayList<Remote.Command>(activity.getInitCommands());
			
			// merge stop commands from last activity if auto-switching is enabled
			if (BaseActivity.getSharedPreferences().getBoolean(BaseActivity.PREFERENCE_AUTO_SWITCH, true))
			{
				ActivityManager.Activity previous_activity = BaseActivity.getPreviousActivity();
				if (previous_activity != null && previous_activity != activity)
				{
					for(Remote.Command cmd : previous_activity.getStopCommands())
					{
						if (!command_list.contains(cmd))
							command_list.add(cmd);
					}
				}
			}
			
			// show fix it dialog
			SolveIssuesDialog dialog = new SolveIssuesDialog(getActivity(), command_list);
			dialog.show();
		}
	}
	
	private void showCommandManagerDialog(boolean for_init_queue)
	{
		CommandManagerDialog dialog = new CommandManagerDialog(getActivity(), activity, for_init_queue, this);
		dialog.show();
	}

	@Override
	public void onCommandManagerDialogClosed(ArrayList<Remote.Command> baseCommands, boolean is_init_queue)
	{
		// assign commands
		if (is_init_queue)
			activity.setInitCommands(baseCommands);
		else
			activity.setStopCommands(baseCommands);
		activity.save();
		
		// recreate options menu
		getActivity().invalidateOptionsMenu();
	}
	
	// Other
	private void setEditMode(boolean is_editing)
	{
		isEditing = is_editing;
		pageAdapter.setEditMode(is_editing);
		
		getActivity().invalidateOptionsMenu();
	}
}
