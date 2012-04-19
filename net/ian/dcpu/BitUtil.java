package net.ian.dcpu;

import java.util.BitSet;

public class BitUtil {
	public static int toInt(BitSet b) {
		int value = 0;
		for (int i = 0; i < b.length(); i++)
			value += b.get(i) ? (1 << i) : 0;
		return value;
	}
	
	public static BitSet fromInt(int value) {
		BitSet b = new BitSet();
		for (int i = 0; value != 0; i++, value = value >> 1) {
			if (value % 2 != 0)
				b.set(i);
		}
		return b;
	}
	
	public static String toString(BitSet b, int size, int spacing) {
		String s = "";
		for (int i = 0; i < size; i++) {
			if ((i != 0) && (spacing != 0) && (i % spacing == 0))
				s += " ";
			s += b.get(i) ? "1" : "0";
		}
		return s;
	}
	
	public static String toString(BitSet b, int size) {
		return toString(b, size, 0);
	}
	
	public static String toString(BitSet b) {
		return toString(b, b.size());
	}
}
