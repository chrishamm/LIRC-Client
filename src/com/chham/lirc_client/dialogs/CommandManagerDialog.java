package com.chham.lirc_client.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.Button;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import com.chham.lirc.Remote;
import com.chham.lirc_client.R;
import com.chham.lirc_client.components.BaseActivity;
import com.chham.lirc_client.content.ActivityManager;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CommandManagerDialog implements DeviceCommandPickerDialog.OnCommandSelectedListener, 
	DragSortListView.DropListener, OnCreateContextMenuListener
{
	public static interface OnCommandManagerDialogClosedListener
	{
		abstract void onCommandManagerDialogClosed(ArrayList<Remote.Command> commands, boolean is_init_queue);
	}
	
	private final String KEY_COMMAND = "command";
	private final String KEY_DEVICE = "device";
	
	private AlertDialog dialog;
	private DragSortListView listView;
	private SimpleAdapter adapter;
	private List<Map<String, String>> adapterValues;
	private int last_device = -1;
	
	private final Activity activity;
	private final boolean init_commands;
	private final OnCommandManagerDialogClosedListener listener;
	private ArrayList<Remote.Command> commands;
	private TextView empty_text;
	
	
	public CommandManagerDialog(Activity activity, ActivityManager.Activity lirc_activity, boolean init_commands, OnCommandManagerDialogClosedListener listener)
	{
		// assign local variables
		this.activity = activity;
		this.init_commands = init_commands;
		this.listener = listener;
		
		// prepare dialog and get right variables
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		if (init_commands)
		{
			builder.setTitle(R.string.menu_init_commands);
			commands = new ArrayList<Remote.Command>(lirc_activity.getInitCommands());
		}
		else
		{
			builder.setTitle(R.string.menu_stop_commands);
			commands = new ArrayList<Remote.Command>(lirc_activity.getStopCommands());
		}
		
		// inflate layout
		LayoutInflater inflater = activity.getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_command_manager, null);
		
		// hide info text if items are 
		empty_text = (TextView)view.findViewById(R.id.text_commands_empty);
		if (commands.size() != 0)
			empty_text.setVisibility(View.GONE);
		
		// set listener
		Button add_button = (Button)view.findViewById(R.id.button_add_command);
		add_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v)
			{
				onAddButtonClick();
			}
		});
		
		// prepare values for adapter
		adapterValues = new ArrayList<Map<String, String>>();
		for(Remote.Command cmd : commands)
		{
			Map<String, String> item = new HashMap<String, String>(2);
			item.put(KEY_COMMAND, cmd.getName());
			item.put(KEY_DEVICE, cmd.getRemote().getCaption());
			adapterValues.add(item);
		}
		
		// set list contents
		listView = (DragSortListView)view.findViewById(R.id.list_commands);
		listView.setDropListener(this);
		listView.setOnCreateContextMenuListener(this);
		
		adapter = new SimpleAdapter(activity, adapterValues, R.layout.item_command, new String[] { KEY_COMMAND, KEY_DEVICE }, new int[] { R.id.item_command, R.id.item_device });
		listView.setAdapter(adapter);

		// set up dialog
		builder.setView(view);
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				onOkClicked();
			}
		});
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setCancelable(true);
		
		dialog = builder.create();
	}
	
	public void show()
	{		
		dialog.show();
	}
	
	// Ok button
	private void onOkClicked()
	{
		listener.onCommandManagerDialogClosed(commands, init_commands);
	}
	
	// Add command button
	private void onAddButtonClick()
	{
		ArrayList<Remote> remotes = BaseActivity.getLircClient().getRemotes();
		
		UniversalCommandPickerDialog picker_dialog = new UniversalCommandPickerDialog(activity, remotes, this);
		picker_dialog.show(-1, last_device);
	}
	
	@Override
	public void onCommandSelected(int id, Remote.Command command, int last_remote_index)
	{
		last_device = last_remote_index;
		
		// add command
		commands.add(command);
		
		// add command to view
		Map<String, String> itemMap = new HashMap<String, String>(2);
		itemMap.put(KEY_COMMAND, command.getName());
		itemMap.put(KEY_DEVICE, command.getRemote().getCaption());
		adapterValues.add(itemMap);
		adapter.notifyDataSetChanged();
		
		// hide info text
		if (commands.size() == 1)
			empty_text.setVisibility(View.GONE);
	}
	
	@Override
	public void drop(int from, int to)
	{
		// change model
		Remote.Command command = commands.remove(from);
		commands.add(to, command);
		
		// change UI
		Map<String, String> temp = adapterValues.remove(from);
		adapterValues.add(to, temp);
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		// delete command
		menu.add(R.string.menu_delete_button)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					int id = ((AdapterContextMenuInfo)item.getMenuInfo()).position;
					commands.remove(id);
					adapterValues.remove(id);
					adapter.notifyDataSetChanged();

					return true;
				}
			});
	}
}
