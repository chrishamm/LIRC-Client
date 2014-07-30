package com.chham.lirc_client.components;

import android.app.ProgressDialog;
import android.content.*;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;
import com.chham.lirc.LIRC_Client;
import com.chham.lirc.Remote;
import com.chham.lirc_client.R;
import com.chham.lirc_client.content.ActivityManager;
import com.chham.lirc_client.content.DbHelper;
import com.chham.lirc_client.content.MacroManager;

public class BaseActivity extends FragmentActivity implements LIRC_Client.EventListener
{
	// Static multi-purpose values
	public static final String SHARED_PREFERENCES_NAME = "settings";
	
	public static final String PREFERENCE_FIRST_RUN = "firstrun";
	public static final String PREFERENCE_IP_ADDRESS = "ip_address";
	public static final String PREFERENCE_WIFI_DEVICE = "wifi_device";
	public static final String PREFERENCE_GRID_ROWS = "grid_rows";
	public static final String PREFERENCE_GRID_COLS = "grid_cols";
	public static final String PREFERENCE_AUTO_SWITCH = "auto_switch";
	public static final String PREFERENCE_SIMULATE_DELAY = "simulate_delay";
	public static final String PREFERENCE_ACTIVITY_DELAY = "activity_delay";
	public static final String PREFERENCE_MACRO_DELAY = "macro_delay";
	public static final String PREFERENCE_STARTED_ACTIVITY = "started_activity";
	public static final String PREFERENCE_PREVIOUS_ACTIVITY = "previous_activity";
	public static final String PREFERENCE_ROTATION_BEHAVIOR = "rotation_behavior";
	
	// WiFi detection including helper function
	public final class ConnectionStateReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (intent.getIntExtra(ConnectivityManager.EXTRA_NETWORK_TYPE, -1) == ConnectivityManager.TYPE_WIFI)
			{
				if (sharedPreferences.getBoolean(PREFERENCE_WIFI_DEVICE, true))
				{
					if (hasWiFiConnection())
					{
						if (!lircClient.isOpen() && doAutoConnect)
							lircClient.reconnect();
					}
					else
					{
						if (lircClient.isOpen())
							lircClient.close();
					}
				}
			}
		}
	}
	private ConnectionStateReceiver receiver;
	private boolean isReceiverRegistered;
	
	protected boolean hasWiFiConnection()
	{
		ConnectivityManager connManager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		
		return (netInfo == null ? false : (netInfo.isConnected()));
	}
	
	// Variables for activity life cycle
	private static boolean isFirstStart = true;
	private static boolean isStarting;
	private boolean isStopping;
	private static boolean doAutoConnect = true;
	
	// Static fields that are used across different activities
	protected static LIRC_Client lircClient;
	protected static DbHelper dbHelper;
	protected static SharedPreferences sharedPreferences;
	
	protected static ActivityManager realActivities;
	protected static ActivityManager deviceActivities;
	protected static MacroManager macroManager;
	
	protected static ActivityManager.Activity startingActivity;
	protected static ActivityManager.Activity startedActivity;
	protected static ActivityManager.Activity previousActivity;
	
	protected static int startOrientation;
	
	// Dialog variables
	public enum ActivityProgressMode
	{
		MODE_STARTING,
		MODE_SWITCHING,
		MODE_STOPPING,
		MODE_GONE
	}

	protected static ProgressDialog connectProgressDialog;

	protected static ActivityProgressMode activityProgressMode = ActivityProgressMode.MODE_GONE;
	protected static ProgressDialog activityProgressDialog;
	protected static int activityProgress;
	protected static int activityProgressCount;
	protected static int activityProgressToast;
	
	// Preferences
	private SharedPreferences initializePreferences()
	{
		SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
		if (prefs.getBoolean(PREFERENCE_FIRST_RUN, true))
		{
			prefs
				.edit()
				.putBoolean(PREFERENCE_FIRST_RUN, false)
				.putString(PREFERENCE_IP_ADDRESS, "")
				.putBoolean(PREFERENCE_WIFI_DEVICE, true)
				.putInt(PREFERENCE_GRID_ROWS, getResources().getInteger(R.integer.default_grid_rows))
				.putInt(PREFERENCE_GRID_COLS, getResources().getInteger(R.integer.default_grid_cols))
				.putBoolean(PREFERENCE_AUTO_SWITCH, true)
				.putInt(PREFERENCE_SIMULATE_DELAY, 200)
				.putInt(PREFERENCE_ACTIVITY_DELAY, 750)
				.putInt(PREFERENCE_MACRO_DELAY, 750)
				.putString(PREFERENCE_ROTATION_BEHAVIOR, "AUTO")
				.commit();
		}
		else
		{
			Editor settings_editor = prefs.edit();
			if (!prefs.contains(PREFERENCE_WIFI_DEVICE))
				settings_editor.putBoolean(PREFERENCE_WIFI_DEVICE, true);
			if (!prefs.contains(PREFERENCE_ROTATION_BEHAVIOR))
				settings_editor.putString(PREFERENCE_ROTATION_BEHAVIOR, "AUTO");
			if (!prefs.contains(PREFERENCE_SIMULATE_DELAY))
				settings_editor.putInt(PREFERENCE_SIMULATE_DELAY, 200);
			if (!prefs.contains(PREFERENCE_MACRO_DELAY))
				settings_editor.putInt(PREFERENCE_MACRO_DELAY, 750);
			settings_editor.commit();
		}
		
		return prefs;
	}
	
	// Activity events
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// initialize static fields if app has just started
		if (isFirstStart)
		{
			isFirstStart = false;
			startOrientation = getResources().getConfiguration().orientation;
			
			// create db helper and preferences instance
			dbHelper = new DbHelper(getApplicationContext());
			sharedPreferences = initializePreferences();
			
			// create and load LIRC client
			lircClient = new LIRC_Client();
			dbHelper.fillClient(lircClient);
			lircClient.setSimulateDelay(sharedPreferences.getInt(PREFERENCE_SIMULATE_DELAY, 200));
			
			// load macros
			macroManager = new MacroManager();
			dbHelper.fillMacroManager(macroManager, lircClient);
			
			// create and load activity managers
			realActivities = new ActivityManager(ActivityManager.Mode.MODE_ACTIVITY, dbHelper);
			dbHelper.fillActivityManager(realActivities, lircClient, macroManager);
			deviceActivities = new ActivityManager(ActivityManager.Mode.MODE_DEVICE, dbHelper);
			dbHelper.fillActivityManager(deviceActivities, lircClient, null);
			
			// load started and previous activity
			long started_activity = sharedPreferences.getLong(PREFERENCE_STARTED_ACTIVITY, -1);
			if (started_activity != -1)
				startedActivity = realActivities.getActivityById(started_activity);
			long previous_activity = sharedPreferences.getLong(PREFERENCE_PREVIOUS_ACTIVITY, -1);
			if (previous_activity != -1)
				previousActivity = realActivities.getActivityById(previous_activity);
		}
		
		// set screen orientation if necessary
		String orientation_prop = sharedPreferences.getString(PREFERENCE_ROTATION_BEHAVIOR, "AUTO");
		if (orientation_prop.equals("PORTRAIT"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		else if (orientation_prop.equals("LANDSCAPE"))
			setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		
		// create activity components
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onStart()
	{
		isStarting = true;
		super.onStart();
		
		// set client listener to current class
		lircClient.setListener(this);

		// connect to server if possible
		boolean is_wifi_device = sharedPreferences.getBoolean(PREFERENCE_WIFI_DEVICE, true);
		if (!is_wifi_device || hasWiFiConnection())
		{
			if (!lircClient.isOpen() && doAutoConnect)
			{
				if (lircClient.getHost() == null)
				{
					String string_uri = sharedPreferences.getString(PREFERENCE_IP_ADDRESS, "").trim();
					if (!string_uri.isEmpty())
					{
						Uri uri = Uri.parse("lirc://" + string_uri);
						int port = uri.getPort();
						
						lircClient.connect(uri.getHost(), port != -1 ? port : 8765);
					}
				}
				else
					lircClient.reconnect();
			}
		}
		
		// register broadcast receiver for wlan connectivity changes
		if (is_wifi_device)
		{
			if (receiver == null)
				receiver = new ConnectionStateReceiver();
			
			registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
			isReceiverRegistered = true;
		}
		
		// recreate activity start/stop dialog
		if (activityProgressDialog != null && lircClient.isConnected())
		{
			showActivityDialog(activityProgressMode, -1);
			activityProgressDialog.setProgress(activityProgress);
		}
	}
	
	

	@Override
	protected void onPause()
	{
		// lifecycle variables
		isStopping = true;
		isStarting = false;
		
		// unregister WLAN broadcast receiver
		if (receiver != null && isReceiverRegistered)
		{
			isReceiverRegistered = false;
			unregisterReceiver(receiver);
		}

		super.onPause();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		
		// hide connect dialog
		if (connectProgressDialog != null)
		{
			connectProgressDialog.dismiss();
            connectProgressDialog = null;
		}
		
		// hide activity progress dialog
		if (activityProgressDialog != null)
        {
			activityProgressDialog.dismiss();
            activityProgressDialog = null;
        }
		
		// disconnect client if app is exiting
		if (!isStarting && !isChangingConfigurations())
		{
			if (lircClient.isOpen())
				lircClient.close();
			
			doAutoConnect = true;
		}
		else
			isStarting = false;
		isStopping = false;
	}
	
	// LIRC client events
	
	@Override
	public boolean onConnecting(String host, int port)
	{
		// check if connection can be established
		boolean need_wifi_device = sharedPreferences.getBoolean(PREFERENCE_WIFI_DEVICE, true);
		boolean can_connect = (!need_wifi_device || hasWiFiConnection());
		if (can_connect)
		{
			// display connecting dialog if it is not present
			if (connectProgressDialog == null)
				connectProgressDialog = showConnectDialog();
		}
		else
		{
			// make sure activity progress dialog is not displayed
			startingActivity = null;
			activityProgressMode = ActivityProgressMode.MODE_GONE;
			
			// show wlan info text
			Toast.makeText(this, getResources().getString(R.string.info_wlan_required), Toast.LENGTH_LONG).show();
		}
		
		return can_connect;
	}

	@Override
	public void onConnected()
	{
		// hide connect progress dialog
		if (connectProgressDialog != null)
			connectProgressDialog.dismiss();
		connectProgressDialog = null;
		
		// if activity has been started, show activity progress dialog
		if (activityProgressMode != ActivityProgressMode.MODE_GONE)
			showActivityDialog(activityProgressMode, -1);
	}

	@Override
	public void onDisconnected(String errorMessage)
	{
		// hide connect progress dialog
		if (connectProgressDialog != null)
			connectProgressDialog.dismiss();
		connectProgressDialog = null;
		
		// hide activity progress dialog
		if (activityProgressDialog != null)
		{
			activityProgressDialog.dismiss();
			activityProgressDialog = null;
		}
		startingActivity = null;
		activityProgressMode = ActivityProgressMode.MODE_GONE;
		
		// show error toast
		if (!isStopping && errorMessage != null)
		{
			doAutoConnect = false;
			Toast.makeText(this, getResources().getString(R.string.connect_error), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onRemoteAdding(String name)
	{
		// unused
	}

	@Override
	public void onRemoteAdded(Remote device)
	{
		// unused
	}
	
	@Override
	public void onRemoteCaptionChanged(Remote device, String old_caption)
	{
		// unused
	}

	@Override
	public void onRemoteDeleted(Remote device)
	{
		// unused
	}

	@Override
	public void onRemoteQueryComplete()
	{
		deviceActivities.validateDevices(lircClient);
		
		for(ActivityManager.Activity activity : deviceActivities.getActivities())
			activity.save();
	}

	@Override
	public void onRemoteVerboseQueryComplete()
	{
		// save client - this call removes deleted commands as well
		dbHelper.saveClient(lircClient);

		// validate activities and delete bad buttons
		deviceActivities.validateDeviceCommands(lircClient);
		realActivities.validateDeviceCommands(lircClient);
		
		// save each activity
		for(ActivityManager.Activity activity : realActivities.getActivities())
			activity.save();
		for(ActivityManager.Activity activity : deviceActivities.getActivities())
			activity.save();
		
		// validate and save macros
		macroManager.validateMacros(lircClient);
		dbHelper.saveMacroManager(macroManager);
		
		// show toast
		Toast.makeText(this, getResources().getString(R.string.query_complete), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onRemoteCommandsSet(Remote device)
	{
		// unused
	}
	
	@Override
	public void onCommandAdded(Remote.Command command)
	{
		// unused
	}

	@Override
	public void onCommandDeleted(Remote.Command command)
	{
		// unused
	}

	@Override
	public void onSendCommandOnce(Remote.Command command, int delay_ms)
	{
		if (activityProgressDialog != null)
		{
			activityProgress++;
			
			if (activityProgress == activityProgressCount)
			{
				activityProgressDialog.dismiss();
				activityProgressDialog = null;
				
				activityProgressMode = ActivityProgressMode.MODE_GONE;
				setStartedActivity(startingActivity);
				
				Toast.makeText(this, getResources().getString(activityProgressToast), Toast.LENGTH_SHORT).show();
				
				invalidateOptionsMenu();
			}
			else
				activityProgressDialog.setProgress(activityProgress);
		}
	}
	
	@Override
	public void onSendCommandSimulate(Remote.Command command, int delay_ms)
	{
		// forward this event, so activity actions, ie. start/stop, work properly
		onSendCommandOnce(command, delay_ms);
	}

	@Override
	public void onSendCommandStart(Remote.Command command, int delay_ms)
	{
		// unused
	}

	@Override
	public void onSendCommandStop(Remote.Command command, int delay_ms)
	{
		// unused
	}

	// Dialogs
	private ProgressDialog showConnectDialog()
	{
		ProgressDialog dialog = new ProgressDialog(this);
		dialog.setTitle(R.string.dialog_connect_title);
		dialog.setMessage(getResources().getString(R.string.dialog_connect_message));
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setCancelable(true);
		dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel), new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				dialog.cancel();
			}
		});
		dialog.setOnCancelListener(new OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog)
			{
				lircClient.close();
				connectProgressDialog = null;
				
				doAutoConnect = false;
			}
		});
		dialog.show();
		
		return dialog;
	}
	
	public void showActivityDialog(ActivityProgressMode target_mode, int cmd_count)
	{
		// set up activity preferences
		if (cmd_count != -1)
		{
			activityProgress = 0;
			activityProgressCount = cmd_count;
		}
		
		// only display dialog if client is connected
		if (lircClient.isConnected())
		{
			int title, message, toast;
			
			// get message
			switch (target_mode)
			{
				case MODE_STARTING:
					title = R.string.dialog_activity_start_title;
					message = R.string.dialog_activity_start_msg;
					toast = R.string.info_activity_started;
					
					break;
					
				case MODE_SWITCHING:
					title = R.string.dialog_activity_switch_title;
					message = R.string.dialog_activity_switch_msg;
					toast = R.string.info_activity_switched;
					
					break;
					
				default:
					title = R.string.dialog_activity_stop_title;
					message = R.string.dialog_activity_stop_msg;
					toast = R.string.info_activity_stopped;
					
					break;
			}
			
			// prepare dialog
			ProgressDialog dialog = new ProgressDialog(this);
			dialog.setTitle(title);
			dialog.setMessage(getResources().getString(message));
			dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			dialog.setMax(activityProgressCount);
			dialog.setProgress(activityProgress);
			dialog.setCancelable(true);
			dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, getResources().getString(android.R.string.cancel), new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					dialog.cancel();
				}
			});
			dialog.setOnCancelListener(new OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog)
				{
					activityProgressMode = ActivityProgressMode.MODE_GONE;
					lircClient.flushCommandQueue();
				}
			});
			dialog.show();
		
			// finish setup
			activityProgressDialog = dialog;
			activityProgressMode = target_mode;
			activityProgressToast = toast;
		}
		// otherwise set start conditions
		else
		{
			activityProgressDialog = null;
			activityProgressMode = target_mode;
		}
	}
	
	// Encapsulated properties
	public static ActivityManager getActivities()
	{
		return realActivities;
	}
	
	public static ActivityManager getDeviceActivities()
	{
		return deviceActivities;
	}
	
	public static ActivityManager.Activity getPreviousActivity()
	{
		return previousActivity;
	}
	
	public static ActivityManager.Activity getStartedActivity()
	{
		return startedActivity;
	}
	
	public static void setStartedActivity(ActivityManager.Activity new_activity)
	{
		// replace startedActivity and previousActivity
		if (startedActivity != previousActivity)
			previousActivity = startedActivity;
		startedActivity = new_activity;
		
		// save current activities
		long started_activity = (startedActivity == null) ? -1 : startedActivity.getId();
		long previous_activity = (previousActivity == null) ? -1 : previousActivity.getId();
		sharedPreferences
			.edit()
			.putLong(PREFERENCE_STARTED_ACTIVITY, started_activity)
			.putLong(PREFERENCE_PREVIOUS_ACTIVITY, previous_activity)
			.commit();
		
	}
	
	public static ActivityManager.Activity getStartingActivity()
	{
		return startingActivity;
	}
	
	public static void setStartingActivity(ActivityManager.Activity new_activity)
	{
		startingActivity = new_activity;
	}
	
	public static LIRC_Client getLircClient()
	{
		return lircClient;
	}
	
	public static MacroManager getMacroManager()
	{
		return macroManager;
	}
	
	public static SharedPreferences getSharedPreferences()
	{
		return sharedPreferences;
	}
	
	public static int getStartOrientation()
	{
		return startOrientation;
	}
}
