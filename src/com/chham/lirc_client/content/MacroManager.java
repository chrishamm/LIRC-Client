package com.chham.lirc_client.content;

import com.chham.lirc.BaseCommand;
import com.chham.lirc.LIRC_Client;
import com.chham.lirc.Remote;
import com.chham.lirc.Remote.Command;

import java.util.ArrayList;

public class MacroManager
{
	public class Macro extends BaseCommand
	{
		private ArrayList<Command> commands;
		
		protected Macro(String name)
		{
			super(name);
			commands = new ArrayList<Command>();
		}
		
		public void addCommand(Command command)
		{
			commands.add(command);
		}
		
		public ArrayList<Command> getCommands()
		{
			return commands;
		}
		
		public void deleteCommand(Command command)
		{
			commands.remove(command);
		}
		
		@Override
		public boolean send()
		{
			for(Command cmd : commands)
			{
				if (!cmd.send())
					return false;
			}
			
			return true;
		}
		
		@Override
		public boolean send(int delay_ms)
		{
			for(Command cmd : commands)
			{
				if (!cmd.send(delay_ms))
					return false;
			}
			
			return true;
		}
		
		@Override
		public boolean sendStart()
		{
			return false;
		}
		
		@Override
		public boolean sendStop()
		{
			return false;
		}
	}
	
	private ArrayList<Macro> macros;
	
	public MacroManager()
	{
		macros = new ArrayList<Macro>();
	}
	
	public Macro addMacro(String name)
	{
		Macro macro = new Macro(name);
		macros.add(macro);
		return macro;
	}
	
	public Macro getMacroById(long id)
	{
		for(Macro macro : macros)
		{
			if (macro.getId() == id)
				return macro;
		}
		
		return null;
	}
	
	public ArrayList<Macro> getMacros()
	{
		return macros;
	}
	
	public void deleteMacro(Macro macro)
	{
		macros.remove(macro);
	}
	
	public void validateMacros(LIRC_Client client)
	{
		ArrayList<Remote.Command> orphaned_commands = new ArrayList<Remote.Command>();
		for(Macro macro : macros)
		{
			// find orphaned commands
			for(Remote.Command cmd : macro.getCommands())
			{
				if (cmd == null || client.getCommandById(cmd.getId()) == null)
					orphaned_commands.add(cmd);
			}
			
			// remove them from macro list
			for(Remote.Command cmd : orphaned_commands)
				macro.deleteCommand(cmd);
		}
	}
}
