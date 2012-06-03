package net.ian.dcpu;

public class Hardware {
	public final int id;
	public final int version;
	public final int manufacturer;
	
	public Hardware(int id, int version, int manufacturer) {
		this.id = id;
		this.version = version;
		this.manufacturer = manufacturer;
	}

	public void interrupt() {}
	
	public void tick() {}
}
