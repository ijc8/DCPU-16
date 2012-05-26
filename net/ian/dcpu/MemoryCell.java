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
		super.set(value);
		for (Hardware device : cpu.devices) {
			if (device.inMemoryRange(addr)) {
				device.onSet(addr, value);
			}
		}
	}
	
	public char get() {
		for (Hardware device : cpu.devices) {
			if (device.inMemoryRange(addr))
				device.onGet(addr, value);
		}
		return value;
	}
}
