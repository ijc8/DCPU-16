package net.ian.dcpu;

public interface Hardware {
	public void onSet(char location, char value);
	public void onGet(char location, char value);
}
