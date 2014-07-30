package com.chham.lirc;

public abstract class BaseCommand implements Comparable<BaseCommand>
{
	private long id;
	private String name;
	
	protected BaseCommand(String name)
	{
		this.setName(name);
	}
	
	public long getId()
	{
		return id;
	}
	
	public void setId(long id)
	{
		this.id = id;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	@Override
	public int compareTo(BaseCommand another)
	{
		return name.compareTo(another.getName());
	}
	
	@Override
	public String toString()
	{
		return getName();
	}
	
	abstract public boolean send();
	
	abstract public boolean send(int delay_ms);
	
	abstract public boolean sendStart();
	
	abstract public boolean sendStop();
}