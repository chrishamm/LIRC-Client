package com.chham.lirc_client.content;

import android.content.Context;
import com.chham.lirc_client.R;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

public class ImageMapper
{
	public static int getImageByCommand(String name)
	{
		// basic control buttons
		if (name.equalsIgnoreCase("KEY_POWER"))
			return R.drawable.icon_basic_power;
		if (name.equalsIgnoreCase("KEY_POWER2"))
			return R.drawable.icon_basic_power_off;
		if (name.equalsIgnoreCase("KEY_EXIT"))
			return R.drawable.icon_basic_exit;
		if (name.equalsIgnoreCase("KEY_OK"))
			return R.drawable.icon_basic_ok;
		if (name.equalsIgnoreCase("KEY_INFO"))
			return R.drawable.icon_basic_info;
		if (name.equalsIgnoreCase("KEY_HELP"))
			return R.drawable.icon_help;
		if (name.equalsIgnoreCase("KEY_SETUP"))
			return R.drawable.icon_basic_setup;
		if (name.equalsIgnoreCase("KEY_HOME"))
			return R.drawable.icon_basic_home;
		if (name.equalsIgnoreCase("KEY_SEARCH"))
			return R.drawable.icon_basic_search;
		if (name.equalsIgnoreCase("KEY_FAVORITES") || name.equalsIgnoreCase("KEY_BOOKMARKS"))
			return R.drawable.icon_basic_favorites;
		
		// 0-9, *, #
		if (name.equalsIgnoreCase("KEY_0"))
			return R.drawable.icon_n0;
		if (name.equalsIgnoreCase("KEY_1"))
			return R.drawable.icon_n1;
		if (name.equalsIgnoreCase("KEY_2"))
			return R.drawable.icon_n2;
		if (name.equalsIgnoreCase("KEY_3"))
			return R.drawable.icon_n3;
		if (name.equalsIgnoreCase("KEY_4"))
			return R.drawable.icon_n4;
		if (name.equalsIgnoreCase("KEY_5"))
			return R.drawable.icon_n5;
		if (name.equalsIgnoreCase("KEY_6"))
			return R.drawable.icon_n6;
		if (name.equalsIgnoreCase("KEY_7"))
			return R.drawable.icon_n7;
		if (name.equalsIgnoreCase("KEY_8"))
			return R.drawable.icon_n8;
		if (name.equalsIgnoreCase("KEY_9"))
			return R.drawable.icon_n9;
		if (name.equalsIgnoreCase("KEY_KPASTERISK") || name.equalsIgnoreCase("KEY_NUMERIC_STAR"))
			return R.drawable.icon_nasterisk;
		if (name.equalsIgnoreCase("KEY_NUMERIC_POUND"))
			return R.drawable.icon_npound;
		
		// arrow keys
		if (name.equalsIgnoreCase("KEY_LEFT"))
			return R.drawable.icon_arrow_left;
		if (name.equalsIgnoreCase("KEY_RIGHT"))
			return R.drawable.icon_arrow_right;
		if (name.equalsIgnoreCase("KEY_UP"))
			return R.drawable.icon_arrow_up;
		if (name.equalsIgnoreCase("KEY_DOWN"))
			return R.drawable.icon_arrow_down;
		if (name.equalsIgnoreCase("KEY_PAGEUP"))
			return R.drawable.icon_arrow_pageup;
		if (name.equalsIgnoreCase("KEY_PAGEDOWN"))
			return R.drawable.icon_arrow_pagedown;
		
		// multimedia keys
		if (name.equalsIgnoreCase("KEY_PLAY") || name.equalsIgnoreCase("KEY_PLAYPAUSE"))
			return R.drawable.icon_multimedia_play;
		if (name.equalsIgnoreCase("KEY_PAUSE"))
			return R.drawable.icon_multimedia_pause;
		if (name.equalsIgnoreCase("KEY_STOP"))
			return R.drawable.icon_multimedia_stop;
		if (name.equalsIgnoreCase("KEY_RECORD"))
			return R.drawable.icon_multimedia_record;
		if (name.equalsIgnoreCase("KEY_BACK"))
			return R.drawable.icon_multimedia_back;
		if (name.equalsIgnoreCase("KEY_FORWARD"))
			return R.drawable.icon_multimedia_play;
		if (name.equalsIgnoreCase("KEY_REWIND"))
			return R.drawable.icon_multimedia_rewind;
		if (name.equalsIgnoreCase("KEY_FASTFORWARD"))
			return R.drawable.icon_multimedia_fastforward;
		if (name.equalsIgnoreCase("KEY_PREVIOUS") || name.equalsIgnoreCase("KEY_PREVIOUSSONG"))
			return R.drawable.icon_multimedia_skip_back;
		if (name.equalsIgnoreCase("KEY_NEXT") || name.equalsIgnoreCase("KEY_NEXTSONG"))
			return R.drawable.icon_multimedia_skip_forward;
		
		// volume keys
		if (name.equalsIgnoreCase("KEY_MUTE"))
			return R.drawable.icon_vol_mute;
		if (name.equalsIgnoreCase("KEY_VOLUMEUP"))
			return R.drawable.icon_vol_up;
		if (name.equalsIgnoreCase("KEY_VOLUMEDOWN"))
			return R.drawable.icon_vol_down;
		
		// color keys
		if (name.equalsIgnoreCase("KEY_BLUE"))
			return R.drawable.icon_color_blue;
		if (name.equalsIgnoreCase("KEY_GREEN"))
			return R.drawable.icon_color_green;
		if (name.equalsIgnoreCase("KEY_RED"))
			return R.drawable.icon_color_red;
		if (name.equalsIgnoreCase("KEY_YELLOW"))
			return R.drawable.icon_color_yellow;
		
		// input selection buttons
		if (name.equalsIgnoreCase("KEY_AUX"))
			return R.drawable.icon_input_aux;
		if (name.equalsIgnoreCase("KEY_CAMERA"))
			return R.drawable.icon_input_camera;
		if (name.equalsIgnoreCase("KEY_CD"))
			return R.drawable.icon_input_cd;
		if (name.equalsIgnoreCase("KEY_COMPUTER"))
			return R.drawable.icon_input_computer;
		if (name.equalsIgnoreCase("KEY_TV") || name.equalsIgnoreCase("KEY_TV2"))
			return R.drawable.icon_input_tv;
		
		// other
		if (name.equalsIgnoreCase("KEY_WLAN"))
			return R.drawable.icon_wlan;
		if (name.equalsIgnoreCase("KEY_WWW"))
			return R.drawable.icon_www;
		
		return -1;
	}
	
	public static class IconEntry implements Comparable<IconEntry>
	{
		public String name;
		public int resource;
		
		@Override
		public int compareTo(IconEntry another)
		{
			return name.compareTo(another.name);
		}
	}
	
	public static ArrayList<IconEntry> getIcons(Context context)
	{
		ArrayList<IconEntry> results = new ArrayList<IconEntry>();
		
		final Field[] fields = R.drawable.class.getFields();
		String name;
		for(Field field : fields)
		{
			name = field.getName();
			if (name.startsWith("icon_"))
			{
				try
				{
					IconEntry entry = new IconEntry();
					entry.name = name;
					entry.resource = field.getInt(R.drawable.class);
					results.add(entry);
				} catch (IllegalArgumentException e) {
					// unhandled
				} catch (IllegalAccessException e) {
					// unhandled
				}
			}
		}
		Collections.sort(results);
		
		return results;
	}

	public static String getNameByDrawable(int drawable)
	{
		final Field[] fields = R.drawable.class.getFields();
		String name;
		for(Field field : fields)
		{
			name = field.getName();
			if (name.startsWith("icon_"))
			{
				try
				{
					if (field.getInt(R.drawable.class) == drawable)
						return name;
				} catch (IllegalArgumentException e) {
					// unhandled
				} catch (IllegalAccessException e) {
					// unhandled
				}
			}
		}
		
		return null;
	}
}
