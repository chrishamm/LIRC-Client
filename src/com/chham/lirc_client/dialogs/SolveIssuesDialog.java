package com.chham.lirc_client.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import com.chham.lirc.BaseCommand;
import com.chham.lirc.Remote;
import com.chham.lirc_client.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SolveIssuesDialog implements OnItemClickListener
{
	private final String KEY_COMMAND = "command";
	private final String KEY_DEVICE = "device";
	
	private final ArrayList<Remote.Command> commands;
	private final AlertDialog dialog;
	private final ListView list;
	
	public SolveIssuesDialog(Activity activity, ArrayList<Remote.Command> commands)
	{
		this.commands = commands;
		
		// prepare values for adapter
		ArrayList<Map<String, String>> adapterValues = new ArrayList<Map<String, String>>(commands.size());
		for(Remote.Command cmd : commands)
		{
			Map<String, String> item = new HashMap<String, String>(2);
			item.put(KEY_COMMAND, cmd.getName());
			item.put(KEY_DEVICE, cmd.getRemote().getCaption());
			adapterValues.add(item);
		}
		
		// inflate layout
		LayoutInflater inflater = activity.getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_solve_issues, null);
		
		// set up list view
		list = (ListView)view.findViewById(R.id.list_fix_issues);
		SimpleAdapter adapter = new SimpleAdapter(activity, adapterValues, android.R.layout.simple_list_item_2, new String[] { KEY_COMMAND, KEY_DEVICE }, new int[] { android.R.id.text1, android.R.id.text2 });
		list.setAdapter(adapter);
		list.setOnItemClickListener(this);
		
		// create dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.dialog_fix_issues_title);
		builder.setView(view);
		builder.setPositiveButton(android.R.string.ok, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.dismiss();
			}
		});
		builder.setCancelable(true);
		
		dialog = builder.create();
	}
	
	public void show()
	{
		dialog.show();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		BaseCommand cmd = commands.get(position);
		
		if (cmd != null)
			cmd.send();
	}
}
