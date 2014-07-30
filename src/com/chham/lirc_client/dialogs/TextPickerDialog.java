package com.chham.lirc_client.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.EditText;
import com.chham.lirc_client.R;

public class TextPickerDialog implements OnClickListener
{
	public static interface OnTextChosenListener
	{
		abstract void onTextChosen(int id, String text);
	}
	
	private Context context;
	private AlertDialog dialog;
	private final EditText text_input;
	boolean allow_empty;
	
	private int target_id;
	private OnTextChosenListener listener;
	
	public TextPickerDialog(Context context, String current_text, OnTextChosenListener listener)
	{
		this.context = context;
		this.listener = listener;
		
		text_input = new EditText(context);
		text_input.setSingleLine();
		text_input.setText(current_text);
	}
	
	public void promptButtonCaption(int target_id)
	{
		this.target_id = target_id;
		allow_empty = true;
		
		dialog = new AlertDialog.Builder(context)
			.setTitle(R.string.dialog_icon_title)
			.setMessage(R.string.dialog_icon_message)
			.setView(text_input)
			.setPositiveButton(android.R.string.ok, this)
			.setNegativeButton(android.R.string.cancel, null)
			.setCancelable(true)
			.create();
		
		dialog.show();
	}

	public void promptActivityName(int target_id)
	{
		this.target_id = target_id;
		allow_empty = false;
		
		dialog = new AlertDialog.Builder(context)
			.setTitle(R.string.add_activity)
			.setMessage(target_id == -1 ? R.string.dialog_activity_add_message : R.string.dialog_activity_rename_message)
			.setView(text_input)
			.setPositiveButton(android.R.string.ok, this)
			.setNegativeButton(android.R.string.cancel, null)
			.create();
		
		dialog.show();
	}
	
	public void promptMacroName(int target_id)
	{
		this.target_id = target_id;
		allow_empty = false;
		
		dialog = new AlertDialog.Builder(context)
			.setTitle(R.string.menu_add_macro)
			.setMessage(target_id == -1 ? R.string.dialog_macro_add_message : R.string.dialog_macro_rename_message)
			.setView(text_input)
			.setPositiveButton(android.R.string.ok, this)
			.setNegativeButton(android.R.string.cancel, null)
			.create();
		
		
		dialog.show();
	}

	@Override
	public void onClick(DialogInterface dialog, int which)
	{
		if (listener != null)
		{
			String text = text_input.getText().toString();
			if (allow_empty | !text.trim().isEmpty())
				listener.onTextChosen(target_id, text);
		}
	}
}
