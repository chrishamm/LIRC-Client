package com.chham.lirc;

import java.util.ArrayList;

public class Remote
{
	public class Command extends BaseCommand
	{
		private String code;
		private final Remote remote;
		
		protected Command(String name, String code, Remote remote)
		{
			super(name);
			
			this.code = code;
			this.remote = remote;
		}
		
		public String getCode()
		{
			return code;
		}
		
		public void setCode(String code)
		{
			this.code = code;
		}
		
		public Remote getRemote()
		{
			return remote;
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Command) || o == null)
				return false;
			Command o_ref = (Command)o;
			
			boolean result;
			result = (o_ref.getId() == getId());
			result &= (o_ref.getName().equals(getName()));
			result &= (o_ref.getRemote() == getRemote());
			
			return result;
		}
		
		public boolean send()
		{
			return remote.sendCommand(this);
		}
		
		public boolean send(int delay_ms)
		{
			return remote.sendCommand(this, delay_ms);
		}
		
		public boolean sendStart()
		{
			return remote.sendStart(this);
		}
		
		public boolean sendStop()
		{
			return remote.sendStop(this);
		}
	}
	
	private long id;
	private String caption;
	private String name;
	private ArrayList<Command> commands;
	private boolean simulate;
	
	private boolean is_initialized = false;
	private final LIRC_Client client;
	
	public Remote(String name, LIRC_Client client)
	{
		this.name = name;
		this.caption = name;
		this.client = client;
		
		commands = new ArrayList<Command>();
	}
	
	public long getId()
	{
		return id;
	}
	
	public void setId(long id)
	{
		this.id = id;
	}
	
	public String getCaption()
	{
		if (caption != null)
			return caption;
		
		return name;
	}
	
	public void setCaption(String caption)
	{
		String old_caption = this.caption;
		this.caption = caption;
		
		if (is_initialized)
			client.getListener().onRemoteCaptionChanged(this, old_caption);
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	public boolean getSimulate()
	{
		return simulate;
	}
	
	public void setSimulate(boolean simulate)
	{
		this.simulate = simulate;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof Remote) || o == null)
			return false;
		
		Remote ref = (Remote)o;
		return (getId() == ref.getId() && getName().equals(ref.getName()));
	}

	@Override
	public String toString()
	{
		return getCaption();
	}
	
	public ArrayList<Command> getCommands()
	{
		return commands;
	}
	
	public void setCommands(ArrayList<Command> commands)
	{
		this.commands = commands;
	}
	
	public Command addCommand(String name, String code)
	{
		Command cmd = new Command(name, code, this);
		commands.add(cmd);
		
		if (is_initialized)
			client.getListener().onCommandAdded(cmd);
		
		return cmd;
	}
	
	public void deleteCommand(Command command)
	{
		commands.remove(command);
		
		if (is_initialized)
			client.getListener().onCommandDeleted(command);
	}
	
	public Command getCommand(String name)
	{
		for (Command c : commands)
		{
			if (c.getName().equals(name))
				return c;
		}
		
		return null;
	}
	
	public boolean sendCommand(Command command)
	{
		return client.sendCommand(command);
	}
	
	public boolean sendCommand(Command command, int delay_ms)
	{
		return client.sendCommand(command, delay_ms);
	}
	
	public boolean sendStart(Command command)
	{
		return client.sendStart(command);
	}
	
	public boolean sendStop(Command command)
	{
		return client.sendStop(command);
	}
	
	// call this as soon as all commands have been added
	public void initialized()
	{
		is_initialized = true;
		client.getListener().onRemoteCommandsSet(this);
	}
}
