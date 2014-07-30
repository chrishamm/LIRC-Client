package com.chham.lirc_client.components;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public class EmbeddedFragment extends Fragment
{
	private final static String KEY_HIDDEN = "hidden";
	
	private boolean isFragmentHidden;
	public boolean isFragmentHidden()
	{
		return isFragmentHidden;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		if (savedInstanceState != null)
		{
			isFragmentHidden = savedInstanceState.getBoolean(KEY_HIDDEN);
			if (isFragmentHidden)
			{
				getFragmentManager()
					.beginTransaction()
					.hide(this)
					.commitAllowingStateLoss();
			}
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putBoolean(KEY_HIDDEN, isFragmentHidden);
		
		super.onSaveInstanceState(outState);
	}
	
	public void show()
	{
		isFragmentHidden = false;
	}
	
	public void hide()
	{
		isFragmentHidden = true;
	}
}
