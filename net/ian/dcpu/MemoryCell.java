package net.ian.dcpu;

import net.ian.dcpu.DCPU.Device;

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
		for (Device device : cpu.devices) {
			if (device.rangeContains(addr))
				device.hardware.onSet(addr, value);
		}
	}
	
	public char get() {
		for (Device device : cpu.devices) {
			if (device.rangeContains(addr))
				device.hardware.onGet(addr, value);
		}
		return value;
	}
}
