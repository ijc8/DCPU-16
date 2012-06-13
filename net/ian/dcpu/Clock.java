package net.ian.dcpu;

public class Clock extends Hardware {
	public static final int ID = 0x12d0b402;
	public static final int VERSION = 1;
	public static final int MANUFACTURER = 0xB65100;
	
	private DCPU cpu;
	private double interval;
	private long lastTime, time;
	private char ticks;
	private char interruptMsg;
	
	public Clock(DCPU cpu) {
		super(ID, VERSION, MANUFACTURER);
		this.cpu = cpu;
		cpu.attachDevice(this);
	}
	
	public void tick() {
		if (interval == 0)
			return;
		if (lastTime == 0) {
			lastTime = System.currentTimeMillis();
			return;
		}
		
		time += System.currentTimeMillis() - lastTime;
		lastTime = System.currentTimeMillis();
		if (time > interval) {
			ticks += time / interval;
			time %= interval;
			if (interruptMsg != 0) {
				cpu.interrupt(interruptMsg);
			}
		}
	}
	
	public void interrupt() {
		char b = cpu.getRegister(Register.B).value;
		switch (cpu.getRegister(Register.A).value) {
		case 0: // If B is 0, disable clock. Otherwise tick 60 / B times per second.
			ticks = 0;
			if (b == 0) {
				interval = 0;
				lastTime = 0;
				return;
			}
			interval = (double)1 / (6 / b) * 100; // Same as 1 / (60 / b) * 1000
			lastTime = System.currentTimeMillis();
			break;
		case 1: // Set C to number of ticks since this interrupt was called with A = 0.
			cpu.getRegister(Register.C).set(ticks);
			break;
		case 2: // Set interrupt message to B (interrupts disabled if B = 0).
			interruptMsg = b;
			break;
		}
	}
}
