package com.chham.lirc_client.content;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import com.chham.lirc.BaseCommand;
import com.chham.lirc.LIRC_Client;
import com.chham.lirc.Remote;
import com.chham.lirc_client.content.MacroManager.Macro;

import java.util.ArrayList;


public class ActivityManager
{
	public class Activity
	{
		public class Button
		{
			private int x;
			private int y;
			private String caption;
			private BaseCommand command;
			private String icon;
			
			public int getX()
			{
				return x;
			}
			
			public int getY()
			{
				return y;
			}
			
			public String getCaption()
			{
				return caption;
			}
			
			public BaseCommand getCommand()
			{
				return command;
			}
			
			public String getIcon()
			{
				return icon;
			}
			
			public void setX(int x)
			{
				this.x = x;
			}
			
			public void setY(int y)
			{
				this.y = y;
			}
			
			public void setCaption(String text)
			{
				this.caption = text;
			}
			
			public void setCommand(BaseCommand baseCommand)
			{
				this.command = baseCommand;
				
				if (baseCommand != null)
				{
					String caption = baseCommand.getName();
					if (caption.startsWith("KEY_"))
						caption = caption.substring(4);
					setCaption(caption);
				}
			}
			
			public void setIcon(String icon)
			{
				this.icon = icon;
			}
		}
		
		private long id;
		private String name;
		private int pages;
		private Remote device;
		
		private ArrayList<Button> buttons;
		private ArrayList<Remote.Command> init_actions;
		private ArrayList<Remote.Command> stop_actions;
		
		public Activity()
		{
			buttons = new ArrayList<Button>();
			init_actions = new ArrayList<Remote.Command>();
			stop_actions = new ArrayList<Remote.Command>();
			
			pages = 1;
		}
		
		public void setId(long id)
		{
			this.id = id;
		}
		
		public long getId()
		{
			if (device == null)
				return id;
			
			return device.getId();
		}
		
		public String getName()
		{
			if (name != null)
				return name;
			
			return device.getName();
		}
		
		public void setName(String name)
		{
			this.name = name;
		}
		
		public int getPages()
		{
			return pages;
		}
		
		public void setPages(int pages)
		{
			this.pages = pages;
		}
		
		public void setDevice(Remote device)
		{
			this.device = device;
		}
		
		public Remote getDevice()
		{
			return device;
		}
		
		public void save()
		{
			ContentValues values = new ContentValues();
			String table;

			SQLiteDatabase db = dbHelper.getWritableDatabase();
			db.beginTransaction();
			
			if (mode == Mode.MODE_ACTIVITY)
			{
				table = "Activity_Button";
				
				// save activity
				values.put("name", name);
				values.put("pages", pages);
				if (id == 0)
					id = db.insert("Activity", null, values);
				else
				{
					db.update("Activity", values, "id=?", new String[] { String.valueOf(id) });
					
					db.delete("Activity_Button", "activity=?", new String[] { String.valueOf(id) });
					db.delete("Activity_Command", "activity=?", new String[] { String.valueOf(id) });
				}
				
				// save init action
				int priority = 1;
				for(BaseCommand cmd : init_actions)
				{
					if (cmd != null)
					{
						values.clear();
						values.put("activity", id);
						values.put("command", cmd.getId());
						values.put("priority", priority);
						db.insert("Activity_Command", null, values);
						
						priority++;
					}
				}
				
				// save stop commands
				priority = -1;
				for(BaseCommand cmd : stop_actions)
				{
					if (cmd != null)
					{
						values.clear();
						values.put("activity", id);
						values.put("command", cmd.getId());
						values.put("priority", priority);
						db.insert("Activity_Command", null, values);
						
						priority--;
					}
				}
			}
			else
			{
				table = "Device_Button";
				
				db.delete("Device_Button", "device=?", new String[] { String.valueOf(device.getId()) });
				
				values.put("pages", pages);
				db.update("Remote", values, "id=?", new String[] { String.valueOf(device.getId()) });
			}
			
			// save buttons
			for(Button btn : buttons)
			{
				values.clear();
				if (mode == Mode.MODE_ACTIVITY)
					values.put("activity", id);
				else
					values.put("device", device.getId());
				
				values.put("command", btn.getCommand().getId());
				values.put("x", btn.getX());
				values.put("y", btn.getY());
				values.put("icon", btn.getIcon());
				values.put("caption", btn.getCaption());
				if (getMode() == Mode.MODE_ACTIVITY)
					values.put("is_macro", btn.getCommand() instanceof Macro);
				
				db.insert(table, null, values);
			}
			
			// finish
			db.setTransactionSuccessful();
			db.endTransaction();
			db.close();
		}
		
		public int getVisibleButtons(int num_cols, int num_rows, int page)
		{
			int button_count = 0, x, y;
			for(Button btn : buttons)
			{
				x = btn.getX();
				y = btn.getY();
				
				if (x >= page * num_cols && x < (page +1) * num_cols && y < num_rows)
					button_count++;
			}
			
			return button_count;
		}
		
		public Button addButton(int x, int y, BaseCommand baseCommand, String icon)
		{
			Button btn = new Button();
			btn.setX(x);
			btn.setY(y);
			btn.setCommand(baseCommand);
			btn.setIcon(icon);
			
			buttons.add(btn);
			return btn;
		}
		
		public void deleteButton(Button button)
		{
			buttons.remove(button);
		}

		public Button getButton(int index)
		{
			return buttons.get(index);
		}
		
		public ArrayList<Button> getButtons()
		{
			return buttons;
		}
		
		public void addAction(Remote.Command command, boolean is_init)
		{
			if (is_init)
				init_actions.add(command);
			else
				stop_actions.add(command);
		}
		
		public void clearActions()
		{
			init_actions.clear();
			stop_actions.clear();
		}
		
		public ArrayList<Remote.Command> getInitCommands()
		{
			return init_actions;
		}
		
		public void setInitCommands(ArrayList<Remote.Command> commands)
		{
			init_actions = commands;
		}
		
		public ArrayList<Remote.Command> getStopCommands()
		{
			return stop_actions;
		}
		
		public void setStopCommands(ArrayList<Remote.Command> commands)
		{
			stop_actions = commands;
		}

		@Override
		public String toString()
		{
			return getName();
		}
	}
	
	public enum Mode
	{
		MODE_ACTIVITY,
		MODE_DEVICE
	}

	private ArrayList<Activity> activities;
	private Mode mode;
	private DbHelper dbHelper;
	
	public ActivityManager(Mode mode, DbHelper dbHelper)
	{
		activities = new ArrayList<Activity>();
		
		this.mode = mode;
		this.dbHelper = dbHelper;
	}
	
	public Mode getMode()
	{
		return mode;
	}
	
	public ArrayList<Activity> getActivities()
	{
		return activities;
	}
	
	public Activity addActivity()
	{
		Activity activity = new Activity();
		activities.add(activity);
		return activity;
	}
	

	public void deleteActivity(int position)
	{
		long id = activities.get(position).getId();
		activities.remove(position);
		
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		if (mode == Mode.MODE_ACTIVITY)
		{
			db.delete("Activity", "id=?", new String[] { String.valueOf(id) });
			db.delete("Activity_Button", "activity=?", new String[] { String.valueOf(id) });
			db.delete("Activity_Command", "activity=?", new String[] { String.valueOf(id) });
		}
		else
			db.delete("Device_Button", "device=?", new String[] { String.valueOf(id) });
	}
	
	public Activity getActivity(int index)
	{
		if (index >= activities.size())
			return null;
		
		return activities.get(index);
	}

	public Activity getActivityById(long id)
	{
		for(Activity activity : activities)
		{
			if (activity.getId() == id)
				return activity;
		}
		
		return null;
	}

	public void validateDevices(LIRC_Client client)
	{
		ArrayList<Remote> devices = client.getRemotes();
		
		// check if all devices are present
		for(Remote device : devices)
		{
			boolean contains_activity = false;
			for(Activity activity : activities)
			{
				if (activity.getDevice() == device)
				{
					contains_activity = true;
					break;
				}
			}
			
			if (!contains_activity)
			{
				Activity activity = addActivity();
				activity.setDevice(device);
			}
		}
		
		// check if any activities need to be removed
		if (activities.size() != devices.size())
		{
			Activity activity;
			for(int i=activities.size() -1; i>=0; i++)
			{
				activity = activities.get(i);
				if (activity.getDevice() == null)
					activities.remove(i);
			}
		}
	}
	
	public void validateDeviceCommands(LIRC_Client client)
	{
		for(Activity activity : activities)
		{
			ArrayList<Activity.Button> buttons = activity.getButtons();
			for(int i=buttons.size() -1; i>=0; i--)
			{
				Activity.Button button = buttons.get(i);
				BaseCommand cmd = button.getCommand();
				
				// check if the previously existing command has been deleted
				if (cmd instanceof Remote.Command && (cmd == null || client.getCommandById(cmd.getId()) == null))
					activity.deleteButton(button);
			}
		}
	}
	
	public void validateMacros(MacroManager macro_manager)
	{
		for(Activity activity : activities)
		{
			ArrayList<Activity.Button> buttons = activity.getButtons();
			for(int i=buttons.size() -1; i>=0; i--)
			{
				Activity.Button button = buttons.get(i);
				BaseCommand cmd = button.getCommand();
				
				// check if the previously existing command has been deleted
				if (cmd instanceof Macro && (cmd == null || macro_manager.getMacroById(cmd.getId()) == null))
					activity.deleteButton(button);
			}
		}
	}
	
	public static ArrayList<Remote.Command> makeWorkerQueue(ArrayList<Remote.Command> off_queue, ArrayList<Remote.Command> on_queue)
	{
		ArrayList<Remote.Command> results = new ArrayList<Remote.Command>(off_queue);
		results.addAll(on_queue);
		
		boolean increase = true;
		Remote.Command item, item2;
		for(int i=0; i<results.size();)
		{
			item = results.get(i);
			for(int k=i+1; k<results.size(); k++)
			{
				if (item != null)
				{
					item2 = results.get(k);
					// remove duplicates
					// if KEY_POWER and KEY_POWER2 for the same device is in the queue, remove it as well
					if (item2 != null && item2.equals(item) || (item.getRemote().equals(item2.getRemote()) && item.getName().equalsIgnoreCase("KEY_POWER2") && item2.getName().equalsIgnoreCase("KEY_POWER")))
					{
						results.remove(k);
						results.remove(i);
						increase = false;
						break;
					}
				}
			}
			
			if (increase)
				i++;
			increase = true;
		}
		
		return results;
	}
}
