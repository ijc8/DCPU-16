package net.ian.dcpu;

public class MemoryCell extends Cell {
	private DCPU cpu;
	private char addr;
	
	public MemoryCell(DCPU cpu, char addr, char value) {
		super(value);
		this.addr = addr;
		this.cpu = cpu;
	}
	
	public void set(char value) {
		char old = this.value;
		super.set(value);
		if (value != old) {
			for (MemoryListener listener : cpu.listeners) {
				if (listener.inMemoryRange(addr))
					listener.onSet(addr, value);
			}
		}
	}
	
	public char get() {
		for (MemoryListener listener : cpu.listeners) {
			if (listener.inMemoryRange(addr))
				listener.onGet(addr, value);
		}
		return value;
	}
}
