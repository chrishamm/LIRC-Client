package com.chham.lirc_client.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.ArrayAdapter;
import com.chham.lirc.Remote;
import com.chham.lirc_client.R;

import java.util.ArrayList;

public class DeviceCommandPickerDialog implements OnClickListener
{
	public static interface OnCommandSelectedListener
	{
		abstract void onCommandSelected(int id, Remote.Command command, int remote_index);
	}
	
	private AlertDialog dialog;
	private ArrayList<Remote.Command> commands;
	private int target_id;
	private OnCommandSelectedListener listener;
	
	public DeviceCommandPickerDialog(Context context, ArrayList<Remote.Command> commands, OnCommandSelectedListener listener)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.dialog_cmd_title);
		builder.setAdapter(new ArrayAdapter<Remote.Command>(context, android.R.layout.simple_list_item_1, commands), this);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setCancelable(true);
		dialog = builder.create();
		
		this.commands = commands;
		this.listener = listener;
	}
	
	public void show(int target_id)
	{
		this.target_id = target_id;
		dialog.show();
	}
	
	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if (listener != null)
			listener.onCommandSelected(target_id, commands.get(which), -1);
	}
}
