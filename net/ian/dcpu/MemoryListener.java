package net.ian.dcpu;

public interface MemoryListener {
	public boolean inMemoryRange(char loc);
	
	public void onSet(char location, char value);
	public void onGet(char location, char value);
}
