package com.chham.lirc_client.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import com.chham.lirc.Remote;
import com.chham.lirc_client.R;

import java.util.ArrayList;


public class UniversalCommandPickerDialog implements OnItemClickListener, TabHost.TabContentFactory
{
	private AlertDialog dialog;
	private ArrayList<Remote> remotes;
	private Activity activity;
	
	private int target_id;
	private DeviceCommandPickerDialog.OnCommandSelectedListener listener;
	
	private final TabHost tab_host;
	
	public UniversalCommandPickerDialog(Activity activity, ArrayList<Remote> remotes, DeviceCommandPickerDialog.OnCommandSelectedListener listener)
	{
		// set properties
		this.activity = activity;
		this.remotes = remotes;
		this.listener = listener;
		
		// build new dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.dialog_cmd_title);
		
		LayoutInflater inflater = activity.getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_universal_command_picker, null);
		
		// fill tabs spinner
		tab_host = (TabHost)view.findViewById(R.id.tab_host);
		tab_host.setup();
		for(Remote remote : remotes)
		{
			TabHost.TabSpec tab_spec = tab_host.newTabSpec(remote.getName());
			tab_spec.setIndicator(remote.getName());
			tab_spec.setContent(this);
			tab_host.addTab(tab_spec);
		}
		
		// finish dialog setup
		builder.setView(view);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setCancelable(true);
		dialog = builder.create();
	}
	
	public void show(int target_id, int last_remote_index)
	{
		this.target_id = target_id;
		if (last_remote_index != -1 && last_remote_index < remotes.size())
			tab_host.setCurrentTab(last_remote_index);
		
		dialog.show();
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		if (listener != null)
		{
			ArrayList<Remote.Command> baseCommands = remotes.get(tab_host.getCurrentTab()).getCommands();
			listener.onCommandSelected(target_id, baseCommands.get(position), tab_host.getCurrentTab());
			dialog.dismiss();
		}
	}

	@Override
	public View createTabContent(String tag)
	{
		// get all commands
		ArrayList<Remote.Command> commands = null;
		for(Remote remote : remotes)
		{
			if (remote.getName().equals(tag))
			{
				commands = remote.getCommands();
				break;
			}
		}
		
		// create listview and assign commands to it
		ListView command_view = new ListView(activity);
		command_view.setOnItemClickListener(this);
		command_view.setAdapter(new ArrayAdapter<Remote.Command>(activity, android.R.layout.simple_list_item_1, commands));
		
		return command_view;
	}
}