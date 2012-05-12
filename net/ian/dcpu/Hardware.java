package net.ian.dcpu;

// Maybe the name should be "MemoryListener"?
public interface Hardware {
	public void onSet(char location, char value);
	public void onGet(char location, char value);
}
