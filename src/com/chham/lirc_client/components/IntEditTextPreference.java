package com.chham.lirc_client.components;

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.InputType;
import android.util.AttributeSet;

public class IntEditTextPreference extends EditTextPreference
{
	
	public IntEditTextPreference(Context context)
	{
		super(context);
		getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
	}
	
	public IntEditTextPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
	}
	
	public IntEditTextPreference(Context context, AttributeSet attrs,
			int defStyle)
	{
		super(context, attrs, defStyle);
		getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
	}
	
	@Override
	protected String getPersistedString(String defaultReturnValue)
	{
		return String.valueOf(getPersistedInt(-1));
	}
	
	@Override
	protected boolean persistString(String value)
	{
		if (value.isEmpty())
		{
			setText(getPersistedString(null));
			return false;
		}
		
		return persistInt(Integer.valueOf(value));
	}
}