package com.chham.lirc;

import java.util.ArrayList;


public class LIRC_Client
{
	public interface EventListener
	{
		abstract boolean onConnecting(String host, int port);
		abstract void onConnected();
		abstract void onDisconnected(String errorMessage);
		
		abstract void onRemoteAdding(String name);
		abstract void onRemoteAdded(Remote device);
		abstract void onRemoteCaptionChanged(Remote device, String old_caption);
		abstract void onRemoteDeleted(Remote device);
		
		abstract void onRemoteQueryComplete();
		abstract void onRemoteVerboseQueryComplete();
		abstract void onRemoteCommandsSet(Remote device);
		
		abstract void onCommandAdded(Remote.Command command);
		abstract void onCommandDeleted(Remote.Command command);
		
		abstract void onSendCommandOnce(Remote.Command command, int delay_ms);
		abstract void onSendCommandSimulate(Remote.Command command, int delay_ms);
		abstract void onSendCommandStart(Remote.Command command, int delay_ms);
		abstract void onSendCommandStop(Remote.Command command, int delay_ms);
	}
	
	private class DummyListener implements EventListener
	{
		@Override
		public boolean onConnecting(String host, int port) { return true; }
		
		@Override
		public void onConnected() { }

		@Override
		public void onDisconnected(String errorMessage) { }

		@Override
		public void onRemoteAdding(String name) { }

		@Override
		public void onRemoteAdded(Remote device) { }
		
		@Override
		public void onRemoteCaptionChanged(Remote device, String old_caption) { }

		@Override
		public void onRemoteDeleted(Remote device) { }
		
		@Override
		public void onRemoteQueryComplete() { }
		
		@Override
		public void onRemoteVerboseQueryComplete() { }
		
		@Override
		public void onRemoteCommandsSet(Remote device) { }

		@Override
		public void onCommandAdded(Remote.Command command) { }

		@Override
		public void onCommandDeleted(Remote.Command command) { }

		@Override
		public void onSendCommandOnce(Remote.Command command, int delay_ms) { }
		
		@Override
		public void onSendCommandSimulate(Remote.Command command, int delay_ms) { }

		@Override
		public void onSendCommandStart(Remote.Command command, int delay_ms) { }

		@Override
		public void onSendCommandStop(Remote.Command command, int delay_ms) { }
	}
	
	protected ProtocolHandler handler;
	private EventListener listener;
	private final DummyListener dummy_listener = new DummyListener();
	
	private String host;
	private int port;
	private int simulateDelay = 200;
	
	final ArrayList<Remote>remotes;
	
	/* LIRC_Client */
	public LIRC_Client()
	{
		remotes = new ArrayList<Remote>();
	}
	
	// Socket internals
	public boolean connect(String target, int port)
	{
		close();
		
		this.host = target;
		this.port = port;
		
		if (listener.onConnecting(host, port))
		{
			handler = new ProtocolHandler(this, target, port, simulateDelay);
			return true;
		}
		
		return false;
	}
	
	public boolean reconnect()
	{
		if (host != null)
		{
			if (listener.onConnecting(host, port))
			{
				handler = new ProtocolHandler(this, host, port, simulateDelay);
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isOpen()
	{
		return (handler != null && handler.isAlive());
	}
	
	public boolean isConnected()
	{
		return (isOpen() && handler.getConnected());
	}
	
	public void close()
	{
		if (isOpen())
			handler.interrupt();
		handler = null;
	}
	
	public String getHost()
	{
		return host;
	}
	
	public int getPort()
	{
		return port;
	}
	
	// Listener interface
	public void setListener(EventListener listener)
	{
		this.listener = listener;
	}
	
	protected EventListener getListener()
	{
		if (listener != null)
			return listener;
		
		return dummy_listener;
	}

	// Basic object management
	public Remote addRemote(String name)
	{
		Remote new_remote = new Remote(name, this);
		remotes.add(new_remote);
		return new_remote;
	}
	
	public Remote getRemote(String name)
	{
		for(Remote r : remotes)
		{
			if (r.getName().equals(name))
				return r;
		}
		
		return null;
	}
	
	public Remote getRemote(int index)
	{
		return remotes.get(index);
	}
	
	public ArrayList<Remote> getRemotes()
	{
		return remotes;
	}
	
	public void clearRemotes()
	{
		for(Remote r : remotes)
		{
			getListener().onRemoteDeleted(r);
		}
		
		remotes.clear();
	}
	
	public boolean removeRemote(Remote remote)
	{
		getListener().onRemoteDeleted(remote);
		return remotes.remove(remote);
	}
	
	public Remote.Command getCommandById(long id)
	{
		for(Remote r: remotes)
		{
			ArrayList<Remote.Command> commands = r.getCommands();
			for(Remote.Command c : commands)
			{
				if (c.getId() == id)
					return c;
			}
		}
		
		return null;
	}
	
	// LIRC interface
	public boolean queryRemoteNames()
	{
		if (host == null)
			return false;
		if (!isOpen() && !reconnect())
			return false;
		
		handler.sendRawCommand("LIST");
		return true;
	}
	
	public boolean queryRemoteCommands(Remote remote)
	{
		if (host == null)
			return false;
		if (!isOpen() && !reconnect())
			return false;
		
		handler.sendRawCommand(String.format("LIST %s", remote.getName()));
		return true;
	}
	
	public boolean queryRemotesVerbose()
	{
		if (host == null)
			return false;
		if (!isOpen() && !reconnect())
			return false;
		
		handler.setVerboseQuery(true);
		handler.sendRawCommand("LIST");
		return true;
	}
	
	public void flushCommandQueue()
	{
		if (isOpen())
			handler.flushCommandQueue();
	}
	
	public void setSimulateDelay(int delay_ms)
	{
		simulateDelay = delay_ms;
		
		if (handler != null)
			handler.setSimulateDelay(delay_ms);
	}
	
	// Send commands
	protected boolean sendCommand(Remote.Command command)
	{
		if (host == null)
			return false;
		if (!isOpen() && !reconnect())
			return false;
		
		handler.sendCommand(command);
		return true;
	}
	
	protected boolean sendCommand(Remote.Command command, int delay_ms)
	{
		if (host == null)
			return false;
		if (!isOpen() && !reconnect())
			return false;
		
		handler.sendCommand(command, delay_ms);
		return true;
	}
	
	protected boolean sendStart(Remote.Command command)
	{
		if (host == null)
			return false;
		if (!isOpen() && !reconnect())
			return false;
		
		handler.sendStart(command);
		return true;
	}
	
	protected boolean sendStop(Remote.Command command)
	{
		if (host == null)
			return false;
		if (!isOpen() && !reconnect())
			return false;
		
		handler.sendStop(command);
		return true;
	}
}
