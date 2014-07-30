package com.chham.lirc_client.components;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import com.chham.lirc_client.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LicensePreference extends DialogPreference
{
	public LicensePreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		
		// open license file
		InputStream is;
		try {
			is = context.getResources().getAssets().open("license_summary");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// read contents
		InputStreamReader isr = new InputStreamReader(is);
		BufferedReader bufferedReader = new BufferedReader(isr);
		StringBuilder sb = new StringBuilder();
		try {
			String line;
			while ((line = bufferedReader.readLine()) != null)
			{
				sb.append(line);
				sb.append('\n');
			}
			
			bufferedReader.close();
			isr.close();
			is.close();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// set up dialog
		setDialogTitle(R.string.settings_licenses_title);
		setDialogMessage(sb.toString());
	}
	
	public LicensePreference(Context context, AttributeSet attrs,
			int defStyle)
	{
		super(context, attrs, defStyle);
	}
}
