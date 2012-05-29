package net.ian.dcpu;

// Maybe the name should be "MemoryListener"?
public class Hardware {
	public boolean inMemoryRange(char loc) {
		return false;
	}
	
	public void onSet(char location, char value) {}
	public void onGet(char location, char value) {}
}
