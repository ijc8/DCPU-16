package net.ian.dcpu;

public class Cell {
	public int value;
	
	public Cell() {}
	
	public Cell(int value) {
		this.value = value;
	}
	
	public String toString() {
		return Integer.toBinaryString(value);
	}
}
