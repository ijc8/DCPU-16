package net.ian.dcpu;

public class Cell {
	public char value;
	
	public Cell() {}
	
	public Cell(char value) {
		this.value = value;
	}
	
	public Cell(int value) {
		this((char)value);
	}
	
	public String toString() {
		return Integer.toBinaryString(value);
	}
}
