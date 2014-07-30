package com.chham.lirc_client.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;


public class MainListFragment extends ListFragment
{
	private Callbacks mCallbacks = sDummyCallbacks;
	private int mActivatedPosition = ListView.INVALID_POSITION;

	public interface Callbacks
	{
		abstract void onListItemSelected(int index);
		abstract void onShowListItemMenu(ContextMenu menu, View v, AdapterContextMenuInfo menuInfo);
	}

	private static Callbacks sDummyCallbacks = new Callbacks()
	{
		@Override
		public void onListItemSelected(int index) { }

		@Override
		public void onShowListItemMenu(ContextMenu menu, View v, AdapterContextMenuInfo menuInfo) { }
	};

	public MainListFragment() { }

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		
		// Register context menu
		registerForContextMenu(getListView());
	}

	@Override
	public void onAttach(Activity activity)
	{
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks))
		{
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach()
	{
		super.onDetach();

		mCallbacks = sDummyCallbacks;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id)
	{
		super.onListItemClick(listView, view, position, id);

		mActivatedPosition = position;
		mCallbacks.onListItemSelected(position);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
	{
		super.onCreateContextMenu(menu, v, menuInfo);
		
		mCallbacks.onShowListItemMenu(menu, v, (AdapterContextMenuInfo)menuInfo);
	}

	public void setActivateOnItemClick(boolean activateOnItemClick)
	{
		getListView().setChoiceMode(activateOnItemClick ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
	}

	public void setActivatedPosition(int position)
	{
		if (position == ListView.INVALID_POSITION)
			getListView().setItemChecked(mActivatedPosition, false);
		else
			getListView().setItemChecked(position, true);

		mActivatedPosition = position;
	}
}
