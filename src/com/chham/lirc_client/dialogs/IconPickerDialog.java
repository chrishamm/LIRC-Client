package com.chham.lirc_client.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import com.chham.lirc_client.R;
import com.chham.lirc_client.components.SquareImageView;
import com.chham.lirc_client.content.ImageMapper;

import java.util.ArrayList;

public class IconPickerDialog implements OnItemClickListener
{
	public static interface OnIconSelectedListener
	{
		abstract void onIconSelected(int id, String icon_string, Drawable icon);
	}
	
	public class IconAdapter extends BaseAdapter
	{
		ArrayList<ImageMapper.IconEntry> icons;
		Context context;
		
		public IconAdapter(Context context)
		{
			icons = ImageMapper.getIcons(context);
			this.context = context;
		}
		
		@Override
		public int getCount()
		{
			return icons.size();
		}

		@Override
		public Object getItem(int position)
		{
			return icons.get(position).name;
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			SquareImageView view = new SquareImageView(context);
			view.setImageResource(icons.get(position).resource);
			return view;
		}
	}
	
	private AlertDialog dialog;
	private GridView grid;
	
	private OnIconSelectedListener listener;
	private int target_id;
	
	public IconPickerDialog(Activity activity, int columns, OnIconSelectedListener listener)
	{
		// set properties
		this.listener = listener;
		
		// build new dialog
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.dialog_icon_title);
		
		LayoutInflater inflater = activity.getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_icon_picker, null);
		grid = (GridView)view.findViewById(R.id.grid_icons);
		grid.setNumColumns(columns);
		grid.setAdapter(new IconAdapter(activity));
		grid.setOnItemClickListener(this);

		// finish dialog setup
		builder.setView(view);
		builder.setNegativeButton(android.R.string.cancel, null);
		builder.setCancelable(true);
		dialog = builder.create();
	}
	
	public void show(int target_id)
	{
		this.target_id = target_id;
		dialog.show();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id)
	{
		dialog.dismiss();
		
		if (listener != null)
			listener.onIconSelected(target_id, "#" + (String)grid.getItemAtPosition(position), ((SquareImageView)view).getDrawable());
	}
}
