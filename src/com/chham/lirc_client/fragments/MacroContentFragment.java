package com.chham.lirc_client.fragments;

import android.os.Bundle;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.SimpleAdapter;
import android.widget.Toast;
import com.chham.lirc.Remote;
import com.chham.lirc.Remote.Command;
import com.chham.lirc_client.R;
import com.chham.lirc_client.components.BaseActivity;
import com.chham.lirc_client.components.EmbeddedFragment;
import com.chham.lirc_client.content.MacroManager.Macro;
import com.chham.lirc_client.dialogs.DeviceCommandPickerDialog.OnCommandSelectedListener;
import com.chham.lirc_client.dialogs.UniversalCommandPickerDialog;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MacroContentFragment extends EmbeddedFragment implements DragSortListView.DropListener, OnCommandSelectedListener
{
	public static final String KEY_MACRO = "macro";
	
	private static final String KEY_COMMAND = "command";
	private static final String KEY_DEVICE = "device";
	
	private MacroContentFragment fragment;
	private Macro targetMacro;
	private DragSortListView list;
	private SimpleAdapter adapter;
	private ArrayList<Map<String, String>> adapterValues;
	private int lastRemoteIndex = -1;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		fragment = this;
		
		// fetch arguments
		int macro_id = getArguments().getInt(KEY_MACRO);
		targetMacro = BaseActivity.getMacroManager().getMacros().get(macro_id);
		
		// prepare macro values for adapter
		adapterValues = new ArrayList<Map<String, String>>();
		for(Remote.Command cmd : targetMacro.getCommands())
		{
			Map<String, String> item = new HashMap<String, String>(2);
			item.put(KEY_COMMAND, cmd.getName());
			item.put(KEY_DEVICE, cmd.getRemote().getCaption());
			adapterValues.add(item);
		}
		
		// register options menu
		setHasOptionsMenu(true);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		
		// add command
		menu.add(R.string.add_command)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					UniversalCommandPickerDialog dialog = new UniversalCommandPickerDialog(getActivity(), BaseActivity.getLircClient().getRemotes(), fragment);
					dialog.show(0, lastRemoteIndex);
					
					return true;
				}
			})
			.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// create view
		View result = inflater.inflate(R.layout.fragment_macro_commands, container, false);
		
		// adjust list
		list = (DragSortListView)result.findViewById(R.id.list_macro_commands);
		list.setDropListener(this);
		list.setOnCreateContextMenuListener(this);
		
		// fill list
		adapter = new SimpleAdapter(getActivity(), adapterValues, R.layout.item_command, new String[] { KEY_COMMAND, KEY_DEVICE }, new int[] { R.id.item_command, R.id.item_device });
		list.setAdapter(adapter);
		registerForContextMenu(list);
		
		return result;
	}
	
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
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo)
	{
		// delete command
		menu.add(R.string.menu_delete_command)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
					targetMacro.getCommands().remove(info.position);
					adapterValues.remove(info.position);
					adapter.notifyDataSetChanged();
					
					return true;
				}
			});
	}
	
	private void onFragmentActivated()
	{
		// show info text
		if (targetMacro.getCommands().size() == 0 && !isFragmentHidden())
			Toast.makeText(getActivity(), getResources().getString(R.string.info_add_command), Toast.LENGTH_SHORT).show();
	}
	
	@Override
	public void drop(int from, int to)
	{
		// change model
		ArrayList<Remote.Command> commands = targetMacro.getCommands();
		Remote.Command temp = commands.remove(from);
		commands.add(to, temp);
		
		// change UI
		Map<String, String> temp_ui = adapterValues.remove(from);
		adapterValues.add(to, temp_ui);
		adapter.notifyDataSetChanged();
	}

	@Override
	public void onCommandSelected(int id, Command command, int remote_index)
	{
		// update model
		targetMacro.addCommand(command);
		
		// update UI
		Map<String, String> item = new HashMap<String, String>(2);
		item.put(KEY_COMMAND, command.getName());
		item.put(KEY_DEVICE, command.getRemote().getCaption());
		adapterValues.add(item);
		adapter.notifyDataSetChanged();
		
		// set last remote index
		lastRemoteIndex = remote_index;
	}
}
