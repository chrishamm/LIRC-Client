package com.chham.lirc_client.content;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.chham.lirc.BaseCommand;
import com.chham.lirc.LIRC_Client;
import com.chham.lirc.Remote;
import com.chham.lirc_client.content.ActivityManager.Activity;
import com.chham.lirc_client.content.MacroManager.Macro;

import java.util.ArrayList;


public class DbHelper extends SQLiteOpenHelper
{
	// CREATE
	private static final String SQL_CREATE_REMOTE = "CREATE TABLE Remote (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, pages INT DEFAULT 1, simulate NUMERIC);";
	private static final String SQL_CREATE_REMOTE_COMMAND = "CREATE TABLE Remote_Command (id INTEGER PRIMARY KEY AUTOINCREMENT, device INTEGER NOT NULL, name TEXT NOT NULL, code TEXT);";
	private static final String SQL_CREATE_ACTIVITY = "CREATE TABLE Activity (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE, pages INT DEFAULT 1);";
	private static final String SQL_CREATE_ACTIVITY_COMMAND = "CREATE TABLE Activity_Command (activity INTEGER NOT NULL, command INTEGER NOT NULL, priority INTEGER NOT NULL);";
	private static final String SQL_CREATE_DEVICE_BUTTON = "CREATE TABLE Device_Button (device INTEGER NOT NULL, command INTEGER NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, icon TEXT, caption TEXT);";
	private static final String SQL_CREATE_ACTIVITY_BUTTON = "CREATE TABLE Activity_Button (activity INTEGER NOT NULL, command INTEGER NOT NULL, x INTEGER NOT NULL, y INTEGER NOT NULL, icon TEXT, caption TEXT, is_macro NUMERIC);";
	private static final String SQL_CREATE_MACRO = "CREATE TABLE Macro (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL);";
	private static final String SQL_CREATE_MACRO_COMMANDS = "CREATE TABLE Macro_Command (macro INTEGER NOT NULL, command INTEGER NOT NULL, priority INTEGER);";
	
	// SELECT
	private static final String SQL_SELECT_REMOTE = "SELECT * FROM Remote;";
	private static final String SQL_SELECT_REMOTE_DATA = "SELECT pages FROM Remote WHERE id=?;";
	private static final String SQL_SELECT_COMMAND_REMOTE = "SELECT * FROM Remote_Command WHERE device=? ORDER BY name;";
	private static final String SQL_SELECT_ACTIVITY = "SELECT * FROM Activity;";
	private static final String SQL_SELECT_ACTIVITY_COMMAND_INIT = "SELECT * FROM Activity_Command WHERE (activity=?) AND (priority>0) ORDER BY priority;";
	private static final String SQL_SELECT_ACTIVITY_COMMAND_STOP = "SELECT * FROM Activity_Command WHERE (activity=?) AND (priority<0) ORDER BY priority DESC;";
	private static final String SQL_SELECT_ACTIVITY_BUTTON = "SELECT * FROM Activity_Button WHERE activity=?;";
	private static final String SQL_SELECT_DEVICE_BUTTON = "SELECT * FROM Device_Button WHERE device=?;";
	private static final String SQL_SELECT_MACRO = "SELECT * FROM Macro;";
	private static final String SQL_SELECT_MACRO_COMMAND = "SELECT * FROM Macro_Command WHERE macro=? ORDER BY priority;";
	
	// Update statements for db upgrades
	private static final String SQL_ALTER_REMOTE_PAGES = "ALTER TABLE Remote ADD COLUMN pages INT DEFAULT 1;";
	private static final String SQL_ALTER_ACTIVITY_PAGES = "ALTER TABLE Activity ADD COLUMN pages INT DEFAULT 1;";
	
	private static final String SQL_ALTER_REMOTE_COMMAND_CODE = "ALTER TABLE Remote_Command ADD COLUMN code TEXT DEFAULT NULL;";
	private static final String SQL_ALTER_REMOTE_SIMULATE = "ALTER TABLE Remote ADD COLUMN simulate NUMERIC DEFAULT 0;";
	private static final String SQL_ALTER_ACTIVITY_BUTTON_IS_MACRO = "ALTER TABLE Activity_Button ADD COLUMN is_macro NUMERIC DEFAULT 0;";
	
	// DB internals
	private static final String DB_FILENAME = "LIRC.db";
	private static int DB_VERSION = 4;

	public DbHelper(Context context)
	{
		super(context, DB_FILENAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL(SQL_CREATE_REMOTE);
		db.execSQL(SQL_CREATE_REMOTE_COMMAND);
		db.execSQL(SQL_CREATE_ACTIVITY);
		db.execSQL(SQL_CREATE_ACTIVITY_COMMAND);
		db.execSQL(SQL_CREATE_DEVICE_BUTTON);
		db.execSQL(SQL_CREATE_ACTIVITY_BUTTON);		
		db.execSQL(SQL_CREATE_MACRO);
		db.execSQL(SQL_CREATE_MACRO_COMMANDS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
	{
		if (oldVersion == 1)
		{
			db.execSQL(SQL_ALTER_REMOTE_PAGES);
			db.execSQL(SQL_ALTER_ACTIVITY_PAGES);
		}
		
		if (oldVersion <= 2)
		{
			db.execSQL(SQL_ALTER_REMOTE_COMMAND_CODE);
			db.execSQL(SQL_ALTER_REMOTE_SIMULATE);
		}
		
		if (oldVersion <= 3)
		{
			db.execSQL(SQL_CREATE_MACRO);
			db.execSQL(SQL_CREATE_MACRO_COMMANDS);
			db.execSQL(SQL_ALTER_ACTIVITY_BUTTON_IS_MACRO);
		}
	}
	
	// LIRC_Client adapter
	public void fillClient(LIRC_Client client)
	{
		SQLiteDatabase db = getReadableDatabase();
		
		// load remotes
		Cursor remotes = db.rawQuery(SQL_SELECT_REMOTE, null);
		if (remotes.moveToFirst())
		{
			int col_rid = remotes.getColumnIndexOrThrow("id");
			int col_rname = remotes.getColumnIndexOrThrow("name");
			int col_rsimulate = remotes.getColumnIndexOrThrow("simulate");
			
			do {
				Remote remote = client.addRemote(remotes.getString(col_rname));
				long id = remotes.getLong(col_rid);
				remote.setId(id);
				remote.setSimulate(remotes.getInt(col_rsimulate) != 0);
				
				// load commands
				Cursor commands = db.rawQuery(SQL_SELECT_COMMAND_REMOTE, new String[] {String.valueOf(id)});
				if (commands.moveToFirst())
				{
					int col_cid = commands.getColumnIndexOrThrow("id");
					int col_cname = commands.getColumnIndexOrThrow("name");
					int col_ccode = commands.getColumnIndexOrThrow("code");

					do {
						Remote.Command cmd = remote.addCommand(commands.getString(col_cname), commands.getString(col_ccode));
						cmd.setId(commands.getLong(col_cid));
					} while (commands.moveToNext());
				}
				commands.close();
				
				// remote is initialized
				remote.initialized();
			} while (remotes.moveToNext());
		}
		
		remotes.close();
		db.close();
	}
	
	public void saveClient(LIRC_Client client)
	{
		// General purpose for updates and deletion
		long remote_id, command_id;
		ArrayList<Long> remote_ids = new ArrayList<Long>();
		
		// Save remotes and commands
		ArrayList<Remote> remotes_client = client.getRemotes();
		ArrayList<Remote.Command> commands_client;
		
		// Load current tables for comparison
		SQLiteDatabase db = getWritableDatabase();
		Cursor remotes_db = db.rawQuery(SQL_SELECT_REMOTE, null);
		
		// Start db update transaction
		db.beginTransaction();

		for(Remote r : remotes_client)
		{
			ContentValues remote_args = new ContentValues();
			remote_args.put("name", r.getName());
			remote_args.put("simulate", r.getSimulate());
			
			remote_id = r.getId();
			if (remote_id == 0)
			{
				remote_id = db.insert("Remote", null, remote_args);
				r.setId(remote_id);
			}
			else
				db.update("Remote", remote_args, "id=?", new String[] { String.valueOf(r.getId()) });
			remote_ids.add(remote_id);
			
			commands_client = r.getCommands();
			for(Remote.Command c : commands_client)
			{
				ContentValues cmd_args = new ContentValues();
				cmd_args.put("device", remote_id);
				cmd_args.put("name", c.getName());
				cmd_args.put("code", c.getCode());
				
				command_id = c.getId();
				if (command_id == 0)
				{
					command_id = db.insert("Remote_Command", null, cmd_args);
					c.setId(command_id);
				}
				else
					db.update("Remote_Command", cmd_args, "id=?", new String[] { String.valueOf(command_id) });
			}
		}
		
		// Delete orphaned remotes and remote commands
		if (remotes_client.size() < remotes_db.getCount())
		{
			if (remotes_db.moveToFirst())
			{
				int col_id = remotes_db.getColumnIndexOrThrow("id");
				
				do {
					remote_id = remotes_db.getLong(col_id);
					
					if (!remote_ids.contains(remote_id))
					{
						db.delete("Remote", "id=?", new String[] { String.valueOf(remote_id) });
						db.delete("Remote_Command", "device=?", new String[] { String.valueOf(remote_id) });
					}
				} while (remotes_db.moveToNext());
			}
			
			remotes_db.close();
		}
		
		// Commit changes
		db.setTransactionSuccessful();
		db.endTransaction();
		db.close();
	}
	
	// Activitiy Manager
	public void fillActivityManager(ActivityManager manager, LIRC_Client client, MacroManager macro_manager)
	{
		SQLiteDatabase db = getReadableDatabase();
		
		switch (manager.getMode())
		{
			case MODE_ACTIVITY:
				// get activities
				Cursor activities = db.rawQuery(SQL_SELECT_ACTIVITY, null);
				if (activities.moveToFirst())
				{
					int col_id = activities.getColumnIndexOrThrow("id");
					int col_name = activities.getColumnIndexOrThrow("name");
					int col_pages = activities.getColumnIndexOrThrow("pages");

					do {
						long id = activities.getLong(col_id);
						
						Activity activity = manager.addActivity();
						activity.setId(id);
						activity.setName(activities.getString(col_name));
						activity.setPages(activities.getInt(col_pages));
						
						// get init commands
						Cursor commands = db.rawQuery(SQL_SELECT_ACTIVITY_COMMAND_INIT, new String[] { String.valueOf(id) });
						if (commands.moveToFirst())
						{
							int col_command = commands.getColumnIndexOrThrow("command");
							
							do {
								int cmd = commands.getInt(col_command);
								
								activity.addAction(client.getCommandById(cmd), true);
							} while (commands.moveToNext());
						}
						commands.close();
						
						// get stop commands
						commands = db.rawQuery(SQL_SELECT_ACTIVITY_COMMAND_STOP, new String[] { String.valueOf(id) });
						if (commands.moveToFirst())
						{
							int col_command = commands.getColumnIndexOrThrow("command");
							
							do {
								int cmd = commands.getInt(col_command);
								
								activity.addAction(client.getCommandById(cmd), false);
							} while (commands.moveToNext());
						}
						commands.close();
						
						// get buttons
						Cursor buttons = db.rawQuery(SQL_SELECT_ACTIVITY_BUTTON, new String[] { String.valueOf(id) });
						if (buttons.moveToFirst())
						{
							int col_btn_command = buttons.getColumnIndexOrThrow("command");
							int col_btn_x = buttons.getColumnIndexOrThrow("x");
							int col_btn_y = buttons.getColumnIndexOrThrow("y");
							int col_btn_icon = buttons.getColumnIndexOrThrow("icon");
							int col_btn_caption = buttons.getColumnIndexOrThrow("caption");
							int col_is_macro = buttons.getColumnIndexOrThrow("is_macro");
							
							do {
								int x = buttons.getInt(col_btn_x);
								int y = buttons.getInt(col_btn_y);
								String icon = buttons.getString(col_btn_icon);
								
								BaseCommand cmd;
								if (buttons.getInt(col_is_macro) != 0)
									cmd = macro_manager.getMacroById(buttons.getLong(col_btn_command));
								else
									cmd = client.getCommandById(buttons.getLong(col_btn_command));
								
								activity.addButton(x, y, cmd, icon).setCaption(buttons.getString(col_btn_caption));
							} while (buttons.moveToNext());
						}
						buttons.close();
						
					} while (activities.moveToNext());
				}
				activities.close();
								
				break;
				
			case MODE_DEVICE:
				// go through all devices
				ArrayList<Remote> devices = client.getRemotes();
				for(Remote r : devices)
				{
					// add activity for each remote
					Activity activity = manager.addActivity();
					activity.setDevice(r);
					
					// get number of pages and background
					Cursor remote_data = db.rawQuery(SQL_SELECT_REMOTE_DATA, new String[] { String.valueOf(r.getId()) });
					if (remote_data.moveToFirst())
					{
						int col_pages = remote_data.getColumnIndexOrThrow("pages");
						activity.setPages(remote_data.getInt(col_pages));
					}
					
					// read buttons
					Cursor buttons = db.rawQuery(SQL_SELECT_DEVICE_BUTTON, new String[] { String.valueOf(r.getId()) });
					if (buttons.moveToFirst())
					{
						int col_command = buttons.getColumnIndexOrThrow("command");
						int col_x = buttons.getColumnIndexOrThrow("x");
						int col_y = buttons.getColumnIndexOrThrow("y");
						int col_icon = buttons.getColumnIndexOrThrow("icon");
						int col_caption = buttons.getColumnIndexOrThrow("caption");
						
						do {
							int command_id = buttons.getInt(col_command);
							int x = buttons.getInt(col_x);
							int y = buttons.getInt(col_y);
							String icon = buttons.getString(col_icon);
							
							Remote.Command command = client.getCommandById(command_id);
							activity.addButton(x, y, command, icon).setCaption(buttons.getString(col_caption));
						} while (buttons.moveToNext());
					}
					buttons.close();
				}
				
				break;
		}
		
		db.close();
	}
	
	// Macros
	public void fillMacroManager(MacroManager manager, LIRC_Client client)
	{
		SQLiteDatabase db = getReadableDatabase();
		Cursor macros = db.rawQuery(SQL_SELECT_MACRO, null);
		if (macros.moveToFirst())
		{
			int col_id = macros.getColumnIndexOrThrow("id");
			int col_name = macros.getColumnIndexOrThrow("name");
			
			do {
				// get macro
				long id = macros.getLong(col_id);
				Macro macro = manager.addMacro(macros.getString(col_name));
				macro.setId(id);
				
				// get commands
				Cursor macro_commands = db.rawQuery(SQL_SELECT_MACRO_COMMAND, new String[] { String.valueOf(id) });
				if (macro_commands.moveToFirst())
				{
					int col_command = macro_commands.getColumnIndexOrThrow("command");
					do {
						long macro_id = macro_commands.getLong(col_command);
						macro.addCommand(client.getCommandById(macro_id));
					} while (macro_commands.moveToNext());
				}
				macro_commands.close();
			} while (macros.moveToNext());
			macros.close();
		}
		db.close();
	}
	
	public void saveMacroManager(MacroManager manager)
	{
		SQLiteDatabase db = getWritableDatabase();
		db.beginTransaction();
		
		ArrayList<Long> macro_ids = new ArrayList<Long>();
		for(Macro macro : manager.getMacros())
		{
			// save macro
			ContentValues values = new ContentValues();
			values.put("name", macro.getName());
			
			long id = macro.getId();
			if (id == 0)
			{
				id = db.insert("Macro", null, values);
				macro.setId(id);
			}
			else
				db.update("Macro", values, "id=?", new String[] { String.valueOf(id) });
			macro_ids.add(id);
			
			// save commands
			db.delete("Macro_Command", "macro=?", new String[] { String.valueOf(id) });
			int priority = 1;
			for(Remote.Command cmd : macro.getCommands())
			{
				values.clear();
				values.put("macro", id);
				values.put("command", cmd.getId());
				values.put("priority", priority);
				
				db.insert("Macro_Command", null, values);
				priority++;
			}
		}
		
		// remove orphaned entries
		Cursor macros_db = db.rawQuery(SQL_SELECT_MACRO, null);
		if (macro_ids.size() < macros_db.getCount())
		{
			if (macros_db.moveToFirst())
			{
				int col_id = macros_db.getColumnIndexOrThrow("id");
				long macro_id;
				
				do {
					macro_id = macros_db.getLong(col_id);
					
					if (!macro_ids.contains(macro_id))
					{
						db.delete("Macro", "id=?", new String[] { String.valueOf(macro_id) });
						db.delete("Macro_Command", "macro=?", new String[] { String.valueOf(macro_id) });						
					}
				} while (macros_db.moveToNext());
			}
		}
		macros_db.close();
		
		db.setTransactionSuccessful();
		db.endTransaction();
		db.close();
	}
}
