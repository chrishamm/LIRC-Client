package com.chham.lirc_client.fragments;

import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.Paint.Align;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import com.chham.lirc.BaseCommand;
import com.chham.lirc.Remote;
import com.chham.lirc_client.R;
import com.chham.lirc_client.components.BaseActivity;
import com.chham.lirc_client.components.SquareImageView;
import com.chham.lirc_client.content.ActivityManager;
import com.chham.lirc_client.content.ActivityManager.Activity.Button;
import com.chham.lirc_client.content.ActivityManager.Mode;
import com.chham.lirc_client.content.ImageMapper;
import com.chham.lirc_client.content.MacroManager.Macro;
import com.chham.lirc_client.dialogs.*;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;

public class PageFragment extends Fragment implements DeviceCommandPickerDialog.OnCommandSelectedListener,
		IconPickerDialog.OnIconSelectedListener, TextPickerDialog.OnTextChosenListener, MacroPickerDialog.OnMacroSelectedListener
{
	public static final String KEY_INDEX = "index";
	public static final String KEY_COLS = "cols";
	public static final String KEY_ROWS = "rows";
	public static final String KEY_ACTIVITY_INDEX = "activity_index";
	public static final String KEY_ACTIVITY_MODE = "mode";
	public static final String KEY_EDIT_MODE = "edit_mode";
	
	private class GridAdapter extends BaseAdapter
		implements OnItemClickListener, OnItemLongClickListener, OnTouchListener, OnDragListener
	{
		private final ArrayList<Button> content;
		private final ArrayList<SquareImageView> views;
		private int iconsToLoad;
		private BaseCommand sendingCommand;
		private Drawable dragDrawable;
		private boolean ignoreOnItemClick, ignoreOnItemLongClick;
		
		public GridAdapter()
		{
			// fill up grid content
			views = new ArrayList<SquareImageView>();
			content = new ArrayList<Button>(numCols * numRows);
			for(int i=0;i<numCols*numRows;i++)
			{
				content.add(null);
				views.add(null);
			}
			iconsToLoad = activity.getVisibleButtons(numCols, numRows, activityIndex);
			
			// insert buttons
			int btn_index, x, y;
			for(Button btn : activity.getButtons())
			{
				x = btn.getX();
				y = btn.getY();
				
				if (x >= activityIndex * numCols && x < (activityIndex +1) * numCols)
				{
					if (isRotated)
						btn_index = (x % numCols) * numRows + y;
					else
						btn_index = y * numCols + (x % numCols);
					
					content.set(btn_index, btn);
				}
			}
			
			// if rotated, make sure arrow buttons are swapped
			if (isRotated)
			{
				int left = -1, up = -1, down = -1, right = -1;
				Button btn;
				for(int i=0; i<content.size(); i++)
				{
					btn = content.get(i);
					
					if (btn != null && btn.getCommand() != null)
					{
						if (left == -1 && btn.getCommand().getName().equalsIgnoreCase("KEY_LEFT"))
							left = i;
						else if (up == -1 && btn.getCommand().getName().equalsIgnoreCase("KEY_UP"))
							up = i;
						else if (down == -1 && btn.getCommand().getName().equalsIgnoreCase("KEY_DOWN"))
							down = i;
						else if (right == -1 && btn.getCommand().getName().equalsIgnoreCase("KEY_RIGHT"))
							right = i;
					}
				}
				
				// left <-> up
				if (left != -1 && up != -1)
				{
					Button temp = content.get(left);
					content.set(left, content.get(up));
					content.set(up, temp);
				}
				
				// down <-> right
				if (down != -1 && right != -1)
				{
					Button temp = content.get(right);
					content.set(right, content.get(down));
					content.set(down, temp);
				}
			}
		}
		
		@Override
		public int getCount()
		{
			if (activity == null)
				return 0;
			
			return numRows * numCols;
		}
	
		@Override
		public Object getItem(int position)
		{
			if (activity == null)
				return null;
			
			return content.get(position);
		}
	
		@Override
		public long getItemId(int position)
		{
			return position;
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			SquareImageView image_view = views.get(position);
			if (image_view == null)
			{
				Button button = content.get(position);
				image_view = new SquareImageView(parent.getContext());
				image_view.setOnDragListener(this);
				views.set(position, image_view);
				
				if (button != null)
					loadButton(position);
			}
			
			return image_view;
		}
		
		public Button setCommand(int id, BaseCommand cmd)
		{
			Button button = content.get(id);
			if (button == null)
			{
				button = activity.addButton(getX(id), getY(id), cmd, null);
				content.set(id, button);
			}
			else
				button.setCommand(cmd);
			
			return button;
		}
	
		public void setIcon(int id, String icon_name, Drawable icon)
		{
			Button button = content.get(id);
			if (button != null)
			{
				button.setIcon(icon_name);
				activity.save();
				
				SquareImageView view = views.get(id);
				view.setImageDrawable(icon);
			}
		}
		
		public String getCaption(int id)
		{
			Button item = content.get(id);
			if (item == null)
				return null;
			
			return item.getCaption();
		}
		
		public void setCaption(int id, String caption)
		{
			Button button = content.get(id);
			if (button != null)
			{
				button.setCaption(caption);
				activity.save();
			}
		}
		
		public void deleteCommand(int id)
		{
			Button button = content.get(id);
			if (button != null)
			{
				activity.deleteButton(button);
				activity.save();
				
				content.set(id, null);
			}
			loadButton(id);
		}
		
		protected int getX(int index)
		{
			if (isRotated)
				return activityIndex * numCols + index / numRows;
			return activityIndex * numCols + index % numCols;
		}
		
		protected int getY(int index)
		{
			if (isRotated)
				return index % numRows;
			return index / numCols;
		}
	
		protected void loadButton(int index)
		{
			SquareImageView view = views.get(index);
			Button button = content.get(index);
			
			if (view != null && !isHidden())
			{
				if (button == null)
					view.setImageDrawable(null);
				else
				{
					// start worker task
					if (paintjob == null)
					{
						paintjob = new ImageLoaderJob();
						paintjob.execute();
					}
					
					// load button icon
					if (iconsToLoad != 0)
						iconsToLoad--;
					itemQueue.add(new QueueElement(button, view, iconsToLoad == 0));
				}
			}
		}
		
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id)
		{
			if (ignoreOnItemClick)
			{
				ignoreOnItemClick = false;
				return;
			}

            // seems like ACTION_UP is not always handled in the grid's onTouch listener...
            if (sendingCommand != null)
            {
                sendingCommand.sendStop();
                sendingCommand = null;
            }

            Button item = content.get(position);
			if (isEditing)
			{
				ignoreOnItemLongClick = true;
				registerForContextMenu(remoteGrid);
				remoteGrid.showContextMenuForChild(view);
			}
			else if (item != null)
			{
				BaseCommand cmd = item.getCommand();
				if (cmd == null)
					Toast.makeText(getActivity(), getResources().getString(R.string.error_no_cmd), Toast.LENGTH_SHORT).show();
				else if ((cmd instanceof Remote.Command) && ((Remote.Command)cmd).getRemote().getSimulate() && ((Remote.Command)cmd).getCode() == null)
					Toast.makeText(getActivity(), getResources().getString(R.string.error_no_code), Toast.LENGTH_LONG).show();
				else if (cmd instanceof Macro)
					cmd.send(BaseActivity.getSharedPreferences().getInt(BaseActivity.PREFERENCE_MACRO_DELAY, 750));
				else
					cmd.send();
			}
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id)
		{
			if (ignoreOnItemLongClick)
			{
				ignoreOnItemLongClick = false;
				return false;
			}
			
			Button item = content.get(position);
			if (item == null)
			{
				if (!isEditing)
				{
					Toast.makeText(getActivity(), getResources().getString(R.string.info_consider_editmode), Toast.LENGTH_LONG).show();
					return true;
				}
				
				ignoreOnItemClick = true;
				return false;
			}
			
			BaseCommand cmd = item.getCommand();
			if (cmd == null)
			{
				Toast.makeText(getActivity(), getResources().getString(R.string.error_no_cmd), Toast.LENGTH_SHORT).show();
				return true;
			}
			else if (isEditing)
			{
				final String name = cmd.getName();
				if (isRotated && (name.equals("KEY_LEFT") || name.equals("KEY_UP") || name.equals("KEY_RIGHT") || name.equals("KEY_DOWN")))
				{
					Toast.makeText(getActivity(), getResources().getString(R.string.info_cannot_rotate), Toast.LENGTH_LONG).show();
					return true;
				}
				
				ClipData.Item clip_item = new ClipData.Item(String.valueOf(position));
				ClipData clip_data = new ClipData(String.valueOf(position), new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN }, clip_item);
				View.DragShadowBuilder shadow_builder = new View.DragShadowBuilder(view);
				view.startDrag(clip_data, shadow_builder, view, 0);
				
				return true;
			}
			else
			{
				sendingCommand = cmd;
				cmd.sendStart();
			}
			
			ignoreOnItemClick = true;
			return false;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event)
		{
			if (event.getAction() == MotionEvent.ACTION_UP)
			{
				if (sendingCommand != null)
				{
					sendingCommand.sendStop();
					sendingCommand = null;
				}
			}
			
			return false;
		}

		@Override
		public boolean onDrag(View v, DragEvent event)
		{
			final int action = event.getAction();
			switch (action)
			{
				case DragEvent.ACTION_DRAG_STARTED:
					if (!event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))
						return false;
					
					if (dragDrawable == null)
					{
						SquareImageView view = (SquareImageView)event.getLocalState();
						dragDrawable = view.getDrawable();
						view.setImageDrawable(null);
					}
					
					return true;
					
				case DragEvent.ACTION_DRAG_ENDED:
					if (dragDrawable != null && !event.getResult())
					{
						SquareImageView view = (SquareImageView)event.getLocalState();
						view.setImageDrawable(dragDrawable);
					}
					dragDrawable = null;
					
					return true;
				
				case DragEvent.ACTION_DROP:
					final int from = Integer.parseInt(event.getClipDescription().getLabel().toString());
					final int to = views.indexOf(v);
					
					// make sure source and destination are valid
					if (from == -1 || to == -1 || from == to)
						return false;
					
					final Button target_btn = content.get(to);
					if (target_btn != null && target_btn.getCommand() == null)
					{
						final String name = target_btn.getCommand().getName();
						if (isRotated && (name.equals("KEY_LEFT") || name.equals("KEY_UP") || name.equals("KEY_RIGHT") || name.equals("KEY_DOWN")))
						{
							Toast.makeText(getActivity(), getResources().getString(R.string.info_cannot_rotate), Toast.LENGTH_LONG).show();
							return false;
						}
					}
					
					// modify data model
					final Button source_btn = content.get(from);
					source_btn.setX(getX(to));
					source_btn.setY(getY(to));
					if (target_btn != null)
						activity.deleteButton(target_btn);
					activity.save();
					
					// move button
					content.set(to, source_btn);
					content.set(from, null);
					
					// reload button contents
					loadButton(from);
					loadButton(to);
					
					return true;
			}
			
			return false;
		}
		
		public void verifyButtons()
		{
			Button button;
			boolean delete_button = false;
			BaseCommand command;
			
			for(int i=content.size() -1; i>=0; i--)
			{
				button = content.get(i);
				if (button != null)
				{
					command = button.getCommand();
					delete_button = (command == null);
					delete_button |= (command instanceof Remote.Command && BaseActivity.getLircClient().getCommandById(command.getId()) == null);
					delete_button |= (command instanceof Macro && BaseActivity.getMacroManager().getMacroById(command.getId()) == null);
					
					if (delete_button)
					{
						content.set(i, null);
						loadButton(i);
					}
				}
			}
		}
	}
	
	private class QueueElement
	{
		public final Button button;
		public final SquareImageView element;
		public final boolean isLast;
		
		public QueueElement(Button button, SquareImageView element, boolean isLast)
		{
			this.button = button;
			this.element = element;
			this.isLast = isLast;
		}
	}

	public class ImageLoaderProgressInfo
	{
		public final Drawable drawable;
		public final SquareImageView element;
		
		public ImageLoaderProgressInfo(Drawable drawable, SquareImageView element)
		{
			this.drawable = drawable;
			this.element = element;
		}
	}
	
	public class ImageLoaderJob extends AsyncTask<Void, ImageLoaderProgressInfo, Void>
	{
		@Override
		protected Void doInBackground(Void... params)
		{
			QueueElement element = null;
			Drawable picture;
			
			do {
				// get element from queue
				try {
					element = itemQueue.take();
				} catch (InterruptedException e) {
					return null;
				}
				
				// get icon from button
				Button button = element.button;
				String icon = button.getIcon();
				if (icon == null || icon.isEmpty())
				{
					// load icon as bitmap
					BitmapFactory.Options opts = new BitmapFactory.Options();
					opts.inMutable = true;
					Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.empty, opts);
					Canvas canvas = new Canvas(bmp);
					
					// set up paint and scale text size to fit 
					Paint paint = new Paint();
					paint.setARGB(255, 255, 255, 255);
					paint.setTextAlign(Align.CENTER);
					paint.setTypeface(Typeface.DEFAULT_BOLD);
					
					String caption = button.getCaption();
					Rect bounds = new Rect();
					int width = bmp.getWidth() - 2 * bmp.getWidth() / 5;
					int height = bmp.getHeight() - 2 * bmp.getHeight() / 5;
					int textHeight = height / 2;
					for(; textHeight>5; textHeight--)
					{
						paint.setTextSize(textHeight);
						paint.getTextBounds(caption, 0, caption.length(), bounds);
						if (bounds.right <= width && bounds.bottom <= height)
							break;
					}
					
					// paint text and publish progress
					canvas.drawText(caption, bmp.getWidth() / 2, bmp.getHeight() / 2 + textHeight / 4, paint);
					picture = new BitmapDrawable(getResources(), bmp);
				}
				else if (icon.startsWith("#"))
				{
					icon = icon.substring(1);
					try {
						int icon_id = getResources().getIdentifier(icon, "drawable", "com.chham.lirc_client");
						picture = getResources().getDrawable(icon_id);
					} catch (Exception e) {
						picture = getResources().getDrawable(R.drawable.empty);
					}
				}
				else
				{
					// Reserved for future use
					picture = getResources().getDrawable(R.drawable.empty);
				}
				
				publishProgress(new ImageLoaderProgressInfo(picture, element.element));
			} while (!isCancelled() && !element.isLast);
			
			return null;
		}
		
		@Override
		protected void onProgressUpdate(ImageLoaderProgressInfo... values)
		{
			ImageLoaderProgressInfo element = values[0];
			if (element != null)
				element.element.setImageDrawable(element.drawable);
		}
		
		@Override
		protected void onPostExecute(Void result)
		{
			progressBar.setVisibility(View.GONE);
			remoteGrid.setVisibility(View.VISIBLE);
			
			paintjob = null;
		}
	}
	
	private ImageLoaderJob paintjob;
	private LinkedBlockingDeque<QueueElement> itemQueue;

	private ActivityManager.Activity activity;
	private ActivityManager.Mode mode;
	private boolean isEditing;
	
	private int activityIndex;
	private int numCols, numRows;
	private boolean isRotated;
	
	private int lastRemoteIndex;

	private ProgressBar progressBar;
	private GridView remoteGrid;
	private GridAdapter contentAdapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		// get arguments
		Bundle args = getArguments();
		activityIndex = args.getInt(KEY_INDEX);
		numCols = args.getInt(KEY_COLS);
		numRows = args.getInt(KEY_ROWS);
		if (savedInstanceState == null)
			isEditing = args.getBoolean(KEY_EDIT_MODE);
		else
			isEditing = savedInstanceState.getBoolean(KEY_EDIT_MODE);
		
		// get right activity
		mode = ActivityManager.Mode.values()[args.getInt(KEY_ACTIVITY_MODE)];
		int activityIndex = args.getInt(KEY_ACTIVITY_INDEX);
		switch (mode)
		{
			case MODE_ACTIVITY:
				activity = BaseActivity.getActivities().getActivity(activityIndex);
				break;
				
			case MODE_DEVICE:
				activity = BaseActivity.getDeviceActivities().getActivity(activityIndex);
				break;
		}
		
		if (activity == null)
			return;
		
		// set grid size
		SharedPreferences prefs = BaseActivity.getSharedPreferences();
		numCols = prefs.getInt(BaseActivity.PREFERENCE_GRID_COLS, getResources().getInteger(R.integer.default_grid_cols));
		numRows = prefs.getInt(BaseActivity.PREFERENCE_GRID_ROWS, getResources().getInteger(R.integer.default_grid_rows));
		
		// check if device is rotated
		if (BaseActivity.getSharedPreferences().getString(BaseActivity.PREFERENCE_ROTATION_BEHAVIOR, "AUTO").equals("AUTO"))
		{
			int orientation = getActivity().getResources().getConfiguration().orientation;
			isRotated = (orientation != Configuration.ORIENTATION_UNDEFINED && orientation != BaseActivity.getStartOrientation());
		}
		else
			isRotated = false;
		
		// Create grid adapter
		itemQueue = new LinkedBlockingDeque<QueueElement>();
		contentAdapter = new GridAdapter();
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		// create view
		View content = inflater.inflate(R.layout.fragment_page, container, false);
		
		if (activity != null)
		{
			// get progress bar
			progressBar = (ProgressBar)content.findViewById(R.id.progress_content);
			
			// fill button grid and register events
			remoteGrid = (GridView)content.findViewById(R.id.remote_grid);
			remoteGrid.setNumColumns(isRotated ? numRows : numCols);
			remoteGrid.setAdapter(contentAdapter);
			remoteGrid.setOnItemClickListener(contentAdapter);
			remoteGrid.setOnItemLongClickListener(contentAdapter);
            remoteGrid.setOnTouchListener(contentAdapter);

			// make sure GridView is centered properly
			remoteGrid.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
				@Override
				public void onGlobalLayout()
				{
					View view = getView();
					if (view != null && !((InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE)).isAcceptingText())
					{
						int width = view.getWidth();
						int height = view.getHeight();
						
						if (width != 0 && height != 0)
						{
							// get padding
							int padding;
					        if (isRotated)
					            padding = width - numRows * (height / numCols);
					        else
					        	padding = width - numCols * (height / numRows);
					        
					        // apply it
					        if (padding > 0)
						        view.setPadding(padding / 2, 0, padding / 2, 0);
						}
					}
				}
			});
			
			// check if activity is empty or if it has been created earlier
			if (activity.getVisibleButtons(numCols, numRows, activityIndex) == 0)
			{
				progressBar.setVisibility(View.GONE);
				remoteGrid.setVisibility(View.VISIBLE);
			}
			else if (itemQueue.size() != 0 && !isHidden())
			{
				paintjob = new ImageLoaderJob();
				paintjob.execute();
			}
		}

		return content;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		// context menu only shown via app logic, unregister it here
		unregisterForContextMenu(remoteGrid);
		
		// get context menu infos
		int id = ((AdapterContextMenuInfo)menuInfo).position;
		Button button = (Button)contentAdapter.getItem(id);
		boolean has_button = button != null;
		boolean has_icon = false;
		if (has_button)
			has_icon = (button.getIcon() != null) && (!button.getIcon().isEmpty());
		
		// add menu
		menu.add(R.string.menu_set_command)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
					promptCommand(info.position);
					
					return true;
				}
			});
		
		if (mode == Mode.MODE_ACTIVITY)
		{
			menu.add(R.string.menu_set_macro)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item)
					{
						AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
						promptMacro(info.position);
						
						return true;
					}
				});
		}
		
		menu.add(R.string.menu_delete_button)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
					contentAdapter.deleteCommand(info.position);
					
					return true;
				}
			})
			.setEnabled(has_button);
		
		menu.add(R.string.menu_set_icon)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
					promptIcon(info.position);
					
					return true;
				}
			})
			.setEnabled(has_button);
		
		menu.add(R.string.menu_delete_icon)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
					contentAdapter.setIcon(info.position, null, getResources().getDrawable(R.drawable.empty));
					contentAdapter.loadButton(info.position);
					
					return true;
				}
			})
			.setEnabled(has_icon);
		
		menu.add(R.string.menu_set_title)
			.setOnMenuItemClickListener(new OnMenuItemClickListener() {
				@Override
				public boolean onMenuItemClick(MenuItem item)
				{
					AdapterContextMenuInfo info = (AdapterContextMenuInfo)item.getMenuInfo();
					promptCaption(info.position);
					
					return true;
				}
			})
			.setEnabled(has_button && !has_icon);
		
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		outState.putBoolean(KEY_EDIT_MODE, isEditing);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onPause()
	{
		if (paintjob != null)
		{
			paintjob.cancel(true);
			paintjob = null;
		}
		
		super.onStop();
	}
	
	@Override
	public void onStart()
	{
		super.onStart();
		
		contentAdapter.verifyButtons();
	}

	private void promptCommand(int id)
	{
		switch (mode)
		{
			case MODE_ACTIVITY:
				UniversalCommandPickerDialog advanced_picker = new UniversalCommandPickerDialog(getActivity(), BaseActivity.getLircClient().getRemotes(), this);
				advanced_picker.show(id, lastRemoteIndex);
				
				break;
				
			case MODE_DEVICE:
				DeviceCommandPickerDialog picker = new DeviceCommandPickerDialog(getActivity(), activity.getDevice().getCommands(), this);
				picker.show(id);
				
				break;
		}
	}
	
	private void promptMacro(int id)
	{
		ArrayList<Macro> macros = BaseActivity.getMacroManager().getMacros();
		if (macros.size() == 0)
			Toast.makeText(getActivity(), getResources().getString(R.string.info_no_macros), Toast.LENGTH_LONG).show();
		else
		{
			MacroPickerDialog picker = new MacroPickerDialog(getActivity(), this, macros);
			picker.show(id);
		}
	}
	
	private void promptIcon(int id)
	{
		IconPickerDialog picker = new IconPickerDialog(getActivity(), isRotated ? numRows : numCols, this);
		picker.show(id);
	}
	
	private void promptCaption(int id)
	{
		String caption = contentAdapter.getCaption(id);
		
		TextPickerDialog picker = new TextPickerDialog(getActivity(), caption, this);
		picker.promptButtonCaption(id);
	}
	
	// Callbacks for custom dialogs
	@Override
	public void onCommandSelected(int id, Remote.Command command, int activity_index)
	{
		Button button = contentAdapter.setCommand(id, command);
		
		// try to find a drawable first, if none is found it is automatically redrawn
		int drawable = ImageMapper.getImageByCommand(command.getName());
		if (drawable != -1)
			button.setIcon("#" + ImageMapper.getNameByDrawable(drawable));
		else
			button.setIcon(null);
		
		// refresh UI content
		contentAdapter.loadButton(id);
		
		// save changes
		activity.save();
		
		// save activity index (in case dialog is reopened)
		if (mode == Mode.MODE_ACTIVITY)
			lastRemoteIndex = activity_index;
	}

	@Override
	public void onIconSelected(int id, String icon_name, Drawable icon)
	{
		// set icon
		contentAdapter.setIcon(id, icon_name, icon);
		contentAdapter.loadButton(id);
		
		// save changes
		activity.save();
	}

	@Override
	public void onTextChosen(int id, String text)
	{
		// set caption
		contentAdapter.setCaption(id, text);
		contentAdapter.loadButton(id);
		
		// save changes
		activity.save();
	}
	
	@Override
	public void onMacroSelected(int target_id, Macro macro)
	{
		// create new button
		Button button = contentAdapter.setCommand(target_id, macro);
		button.setCaption(macro.getName());
		
		// load it
		contentAdapter.loadButton(target_id);
		
		// save changes
		activity.save();
	}
	
	// Public functions
	public void setEditMode(boolean is_editing)
	{
		isEditing = is_editing;
	}
}
