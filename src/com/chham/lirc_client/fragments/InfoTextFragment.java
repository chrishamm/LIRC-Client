package com.chham.lirc_client.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.chham.lirc_client.R;
import com.chham.lirc_client.components.EmbeddedFragment;

public class InfoTextFragment extends EmbeddedFragment
{
	private int text;
	public void setText(int text)
	{
		this.text = text;
		
		TextView view = (TextView)getView().findViewById(R.id.text_info);
		if (view != null)
			view.setText(text);
	}
	
	public final static String KEY_TEXT = "text";
	
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState == null)
			text = getArguments().getInt(KEY_TEXT);
		else
			text = savedInstanceState.getInt(KEY_TEXT);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_info_text, container, false);
		
		if (text != 0)
		{
			TextView textview = (TextView)view.findViewById(R.id.text_info);
			textview.setText(text);
		}
		
		return view;
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putInt(KEY_TEXT, text);
		super.onSaveInstanceState(outState);
	}
}
