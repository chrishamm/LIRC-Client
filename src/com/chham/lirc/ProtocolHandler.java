package com.chham.lirc;

import android.os.Handler;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

class ProtocolHandler extends Thread
{
	/* Packet intercommunication between thread and main thread */
	enum PacketDescription
	{
		PACKET_CONNECT_STATUS,
		PACKET_LIST,
		PACKET_QUERY,
		PACKET_QUERY_COMPLETE,
		PACKET_COMMAND_SENT_ONCE,
		PACKET_COMMAND_SEND_SIMULATE,
		PACKET_COMMAND_SENT_START,
		PACKET_COMMAND_SENT_STOP
	}

	class HandlerPacket
	{
		public PacketDescription description;
		public String remote;
		public Object data;
		public int delay;
		
		public HandlerPacket(PacketDescription description, String remote, Object data, int delay)
		{
			this.description = description;
			this.remote = remote;
			this.data = data;
			this.delay = delay;
		}
	}
	
	@SuppressWarnings("unchecked")
	public class HandlerCode implements Runnable
	{
		private final LinkedBlockingDeque<HandlerPacket> packet_queue;
		
		public HandlerCode(LinkedBlockingDeque<HandlerPacket> packet_queue)
		{
			this.packet_queue = packet_queue;
		}
		
		@Override
		public void run()
		{
			HandlerPacket data = packet_queue.poll();
			
			switch (data.description)
			{
				case PACKET_CONNECT_STATUS:
					boolean connected = (Boolean)data.data;
					setConnected(connected);
					
					if (connected)
						parent.getListener().onConnected();
					else
						parent.getListener().onDisconnected(data.remote);
					
					break;
					
				case PACKET_LIST:
					ArrayList<String> remotes = (ArrayList<String>)data.data;
					
					// merge remote objects
					boolean contains_remote;
					for(String name : remotes)
					{
						contains_remote = false;
						for(Remote ref_r : parent.remotes)
						{
							if (ref_r.getName().equals(name))
							{
								contains_remote = true;
								break;
							}
						}
						
						if (!contains_remote)
						{
							parent.getListener().onRemoteAdding(name);
							Remote remote = parent.addRemote(name);
							if (!getVerboseQuery())
							{
								remote.initialized();
								parent.getListener().onRemoteAdded(remote);
							}
						}
					}
					
					break;
					
				case PACKET_QUERY:
					// get target remote first
					Remote target = null;
					for(Remote r : parent.remotes)
					{
						if (r.getName().equals(data.remote))
						{
							target = r;
							break;
						}
					}
					
					boolean is_remote_new = false;
					if (target == null)
					{
						target = parent.addRemote(data.remote);
						parent.getListener().onRemoteAdding(data.remote);
						is_remote_new = true;
					}
					
					// merge commands
					ArrayList<Remote.Command> commands = target.getCommands();
					HashMap<String, String> new_cmds = (HashMap<String, String>)data.data;
					boolean command_exists;
					
					for(String new_cmd : new_cmds.keySet())
					{
						command_exists = false;
						for(Remote.Command cmd : commands)
						{
							if (cmd.getName().equals(new_cmd))
							{
								command_exists = true;
								if (cmd.getCode() == null)
									cmd.setCode(new_cmds.get(new_cmd));
								
								break;
							}
						}
		
						if (!command_exists)
							target.addCommand(new_cmd, new_cmds.get(new_cmd));
					}
					
					// find and remove deleted commands
					ArrayList<Remote.Command> orphaned_commands = new ArrayList<Remote.Command>();
					for(Remote.Command cmd : target.getCommands())
					{
						if (!new_cmds.containsKey(cmd.getName()))
							orphaned_commands.add(cmd);
					}
					
					for(Remote.Command cmd : orphaned_commands)
						target.deleteCommand(cmd);
					
					// sort commands by name
					Collections.sort(target.getCommands());
					
					// notify callbacks
					target.initialized();
					if (is_remote_new || getVerboseQuery())
						parent.getListener().onRemoteAdded(target);
					
					// check for callback completion
					int count = getRemotesLeft();
					if (count != -1)
					{
						count--;
						if (count == 0)
						{
							parent.getListener().onRemoteVerboseQueryComplete();
							setRemotesLeft(-1);
						}
						else
							setRemotesLeft(count);
					}
					
					break;
					
				case PACKET_QUERY_COMPLETE:
					parent.getListener().onRemoteQueryComplete();
					break;
					
				case PACKET_COMMAND_SENT_ONCE:
					parent.getListener().onSendCommandOnce((Remote.Command)data.data, data.delay);
					break;
					
				case PACKET_COMMAND_SEND_SIMULATE:
					parent.getListener().onSendCommandSimulate((Remote.Command)data.data, data.delay);
					break;
					
				case PACKET_COMMAND_SENT_START:
					parent.getListener().onSendCommandStart((Remote.Command)data.data, data.delay);
					break;
					
				case PACKET_COMMAND_SENT_STOP:
					parent.getListener().onSendCommandStop((Remote.Command)data.data, data.delay);
					break;
			}
		}
	}
	
	private class SendPacket
	{
		public String rawCommand;
		public int delayMS;
		public Remote.Command command;
		
		public SendPacket(String raw_command, int delay_ms, Remote.Command command)
		{
			this.rawCommand = raw_command;
			this.delayMS = delay_ms;
			this.command = command;
		}
	}
	
	private class SimulateTimerTask extends TimerTask
	{
		@Override
		public void run()
		{
			if (getConnected())
			{
				// increment counter
				simulateCounter++;
				if (simulateCounter > 255)
					simulateCounter = 0;
				
				// send formatted command
				sendRawCommand(String.format("SIMULATE %s %02x %s %s", simulatingCommand.getCode(), simulateCounter, simulatingCommand.getName(), simulatingCommand.getRemote().getName()), 0, simulatingCommand);
			}
			else
			{
				simulateTimer.cancel();
				simulateTimer.purge();
			}
		}
	}
	
	/* Protocol reader */
	private BufferedReader input;
	private BufferedWriter output;
	
	private final Handler handler;
	private final LinkedBlockingDeque<HandlerPacket> handler_queue;
	private final HandlerCode handler_code;

	private final String host;
	private final int port;
	private final LIRC_Client parent;
	private final LinkedBlockingDeque<SendPacket> command_queue;
	
	private int simulateDelay;
	private Timer simulateTimer;
	private Remote.Command simulatingCommand;
	private short simulateCounter;
	
	private boolean verbose_query = false;
	private int remotes_left;
	
	private boolean isConnected;
	
	public boolean getConnected()
	{
		return isConnected;
	}
	
	private void setConnected(boolean isConnected)
	{
		this.isConnected = isConnected;
	}
	
	ProtocolHandler(LIRC_Client parent, String host, int port, int simulate_delay)
	{
	    this.parent = parent;
	    this.host = host;
	    this.port = port;
	    this.simulateDelay = simulate_delay;
	    
	    handler = new Handler();
	    handler_queue = new LinkedBlockingDeque<HandlerPacket>();
	    handler_code = new HandlerCode(handler_queue);
	    
	    command_queue = new LinkedBlockingDeque<SendPacket>();
	    
	    start();
	}
	
	@Override
	public void run()
	{
		HandlerPacket handler_packet;
		Socket sock;
		String error_message = null;

		try
		{
			sock = new Socket(host, port);
			input = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			output = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
		}
		catch (Exception e)
		{
			command_queue.clear();
			
			if (!isInterrupted())
			{
				handler_packet = new HandlerPacket(PacketDescription.PACKET_CONNECT_STATUS, e.getLocalizedMessage(), false, 0);
				handler_queue.add(handler_packet);
				handler.post(handler_code);
			}
			return;
		}
		
		// make sure our callback is called
		handler_packet = new HandlerPacket(PacketDescription.PACKET_CONNECT_STATUS, null, true, 0);
		handler_queue.add(handler_packet);
		handler.post(handler_code);

		SendPacket cmd_packet;
		String command_string, response;
		
		PacketDescription result;
		String result_remote;
		Object result_data;
		int result_delay;
		
		boolean is_query_complete = false;
		
		try
		{
			while (!isInterrupted())
			{
				// reset old result variables
				result = null;
				result_remote = "";
				result_data = null;
				result_delay = 0;

				// poll command to be sent
				cmd_packet = command_queue.take();
				command_string = cmd_packet.rawCommand;
				
				// flush any events from socket input
				while (input.ready())
					input.readLine();

				// then send command to server
				output.write(command_string + "\n");
				output.flush();
				
				// first check if command was successful
				response = input.readLine();
				if (response == null)
				{
					error_message = "Connection has been dropped";
					break;
				}
				else
				{
					if (response.equals("BEGIN"))
					{
						input.readLine();
						response = input.readLine();
					}
					
					if (response.equals("SUCCESS"))
					{	
						// LIST -> Get remote devices
						if (command_string.equals("LIST"))
						{
							if (input.readLine().equals("DATA"))
							{
								int count = Integer.parseInt(input.readLine());
								
								ArrayList<String> remotes = new ArrayList<String>(count);
								for(int i=0; i<count; i++)
								{
									String name = input.readLine();
									remotes.add(name);
									
									if (getVerboseQuery())
									{
										sendRawCommand(String.format("LIST %s", name));
									}
								}
								
								result = PacketDescription.PACKET_LIST;
								result_data = remotes;
								
								if (getVerboseQuery())
									setRemotesLeft(count);
								
								is_query_complete = true;
								setVerboseQuery(false);
							}
						}
						// LIST <REMOTE> -> Get remote commands
						else if (command_string.startsWith("LIST "))
						{
							// first of all, get the right Remote object
							String remote_name = command_string.substring(5).trim();
							
							// then parse incoming commands
							if (input.readLine().equals("DATA"))
							{
								int count = Integer.parseInt(input.readLine());
								
								HashMap<String, String> cmd_list = new HashMap<String, String>(count);
								for(int i=0; i<count; i++)
								{
									String line = input.readLine();
									String output[] = line.split(" ");
									if (output.length == 2)
										cmd_list.put(output[1], output[0]);	
								}
								
								result = PacketDescription.PACKET_QUERY;
								result_remote = remote_name;
								result_data = cmd_list;
							}
						}
						// SEND_ONCE
						else if (command_string.startsWith("SEND_ONCE "))
						{
							result = PacketDescription.PACKET_COMMAND_SENT_ONCE;
							result_remote = command_string;
							result_data = cmd_packet.command;
							result_delay = cmd_packet.delayMS;
						}
						// SIMULATE
						else if (command_string.startsWith("SIMULATE "))
						{
							result = PacketDescription.PACKET_COMMAND_SEND_SIMULATE;
							result_remote = command_string;
							result_data = cmd_packet.command;
							result_delay = cmd_packet.delayMS;
						}
						// SEND_START
						else if (command_string.startsWith("SEND_START "))
						{
							result = PacketDescription.PACKET_COMMAND_SENT_START;
							result_remote = command_string;
							result_data = cmd_packet.command;
							result_delay = cmd_packet.delayMS;
						}
						// SEND_STOP
						else if (command_string.startsWith("SEND_STOP "))
						{
							result = PacketDescription.PACKET_COMMAND_SENT_STOP;
							result_remote = command_string;
							result_data = cmd_packet.command;
							result_delay = cmd_packet.delayMS;
						}
						
						input.readLine(); // should be END
	
						if (result != null)
						{
							handler_packet = new HandlerPacket(result, result_remote, result_data, result_delay);
							handler_queue.add(handler_packet);
							handler.post(handler_code);
						}
						
						// sleep a few ms if required
						if (cmd_packet.delayMS != 0)
							Thread.sleep(cmd_packet.delayMS);
						
						if (is_query_complete)
						{
							handler_packet = new HandlerPacket(PacketDescription.PACKET_QUERY_COMPLETE, null, null, 0);
							handler_queue.add(handler_packet);
							handler.post(handler_code);
							is_query_complete = false;
						}
					}
					else
					{
						// flush incoming data
						while (input.ready())
							input.readLine();
					}
				}
			}
		} catch (IOException e) {
			error_message = e.getLocalizedMessage();
		} catch (InterruptedException e) {
			// unused
		}
		
		// thread is dead, connection no longer exists
		handler_packet = new HandlerPacket(PacketDescription.PACKET_CONNECT_STATUS, error_message, false, 0);
		handler_queue.add(handler_packet);
		handler.post(handler_code);
	}
	
	public void setSimulateDelay(int delay_ms)
	{
		simulateDelay = delay_ms;
	}
	
	public void sendRawCommand(String raw_command)
	{
		sendRawCommand(raw_command, 0, null);
	}
	
	public void sendRawCommand(String raw_command, Remote.Command command)
	{
		sendRawCommand(raw_command, 0, command);
	}
	
	public void sendRawCommand(String raw_command, int delay_ms, Remote.Command command)
	{
		try {
			command_queue.put(new SendPacket(raw_command, delay_ms, command));
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	protected void sendCommand(Remote.Command command)
	{
		sendCommand(command, 0);
	}
	
	protected void sendCommand(Remote.Command command, int delay_ms)
	{
		if (command.getRemote().getSimulate())
			sendRawCommand(String.format("SIMULATE %s 00 %s %s", command.getCode(), command.getName(), command.getRemote().getName()), delay_ms, command);
		else
			sendRawCommand(String.format("SEND_ONCE %s %s", command.getRemote().getName(), command.getName()), delay_ms, command);
	}
	
	protected void sendStart(Remote.Command command)
	{
		if (command.getRemote().getSimulate())
		{
			// schedule next sends
			simulateCounter = 0;
			simulatingCommand = command;
			
		    simulateTimer = new Timer();
			simulateTimer.schedule(new SimulateTimerTask(), simulateDelay, simulateDelay);
			
			// send command once
			sendCommand(command);
		}
		else
			sendRawCommand(String.format("SEND_START %s %s", command.getRemote().getName(), command.getName()), command);
	}
	
	protected void sendStop(Remote.Command command)
	{
		if (command.getRemote().getSimulate())
		{
			simulateTimer.cancel();
			simulateTimer.purge();
		}
		else
			sendRawCommand(String.format("SEND_STOP %s %s", command.getRemote().getName(), command.getName()), command);
	}
	
	private synchronized boolean getVerboseQuery()
	{
		return verbose_query;
	}
	
	protected synchronized void setVerboseQuery(boolean is_verbose)
	{
		verbose_query = is_verbose;
	}
	
	private synchronized int getRemotesLeft()
	{
		return remotes_left;
	}
	
	protected synchronized void setRemotesLeft(int remotes_left)
	{
		this.remotes_left = remotes_left;
	}
	
	protected void flushCommandQueue()
	{
		command_queue.clear();
	}
}