package com.chham.lirc_client.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.ArrayAdapter;
import com.chham.lirc_client.R;
import com.chham.lirc_client.content.MacroManager.Macro;

import java.util.List;

public class MacroPickerDialog implements OnClickListener
{
	public interface OnMacroSelectedListener
	{
		void onMacroSelected(int target_id, Macro macro);
	}
	
	private OnMacroSelectedListener listener;
	private List<Macro> macros;
	private AlertDialog dialog;
	private int targetId;
	
	public MacroPickerDialog(Context context, OnMacroSelectedListener listener, List<Macro> macros)
	{
		this.listener = listener;
		this.macros = macros;
		
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.dialog_macro_title);
		builder.setAdapter(new ArrayAdapter<Macro>(context, android.R.layout.simple_list_item_1, macros), this);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setCancelable(true);
		dialog = builder.create();
	}
	
	public void show(int target_id)
	{
		targetId = target_id;
		dialog.show();
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if (listener != null)
			listener.onMacroSelected(targetId, macros.get(which));
	}
}
