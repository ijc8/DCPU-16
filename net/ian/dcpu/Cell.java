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
	
	public void set(char value) {
		this.value = value;
	}
	
	public void set(int value) {
		set((char)value);
	}
	
	public char get() {
		return value;
	}
	
	public String toString() {
		return Integer.toBinaryString(value);
	}
}
