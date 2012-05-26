package net.ian.dcpu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DCPU {
	public Cell[] register;
	public MemoryCell[] memory;
	public Cell SP, PC, O;
	public boolean running;
	public int instructionCount = 0;
	
	public static final boolean debug = false;
	
	public Map<Integer, String> labels;
	
	public enum Register { A, B, C, X, Y, Z, I, J };
	
	public List<Hardware> devices;
	
	public DCPU() {
		this(new char[0]);
	}

	public DCPU(char[] mem) {
		register = new Cell[11];
		for (int i = 0; i < 11; i++)
			register[i] = new Cell(0);
		memory = new MemoryCell[0x10000]; // 0x10000 words in size
		for (int i = 0; i < 0x10000; i++)
			memory[i] = new MemoryCell(this, (char)i, i < mem.length ? mem[i] : 0);

		SP = new Cell(0);
		PC = new Cell(0);
		O = new Cell(0);
		
		devices = new ArrayList<Hardware>();
	}
	
	public DCPU(List<Integer> mem) {
		this(integersToInts(mem));
	}
	
	public void setMemory(List<Integer> listMem) {
		char[] mem = integersToInts(listMem);
		for (int i = 0; i < mem.length; i++)
			memory[i].value = mem[i];
	}
	
	// This is here because Java wants constructor calls to be the first statement in another constructor (see above).
	private static char[] integersToInts(List<Integer> mem) {
		Integer[] integers = mem.toArray(new Integer[0]);
		char[] ints = new char[integers.length];
		for (int i = 0; i < ints.length; i++)
			ints[i] = (char)(int)integers[i];
		return ints;
	}
	
	private void debug(Object o) {
		if (debug)
			System.out.print(o);
	}
	
	private void debugln(Object o) {
		if (debug)
			System.out.println(o);
	}
	
	private void debugf(String s, Object... o) {
		if (debug)
			System.out.printf(s, o);
	}
	
	public void attachDevice(Hardware h) {
		devices.add(h);
	}
	public Cell getRegister(Register r) {
		return register[r.ordinal()];
	}
	
	public void setRegister(Register r, char value) {
		register[r.ordinal()].value = value;
	}
	
	private Cell handleArgument(int code) {
		debugf("0x%s: ", Integer.toHexString(code));
		if (code >= 0x0 && code <= 0x7) {
			debug(Register.values()[code]);
			return register[code];
		} else if (code >= 0x8 && code <= 0xf) {
			debugf("[%s]", Register.values()[code - 0x8]);
			return memory[register[code - 0x8].value];
		} else if (code >= 0x10 && code <= 0x17) {
			debugf("[next word + %s]", Register.values()[code - 0x10]);
			return memory[memory[PC.value++].value + register[code - 0x10].value];
		} else if (code == 0x18) {
			debug("POP");
			return memory[SP.value++];
		} else if (code == 0x19) {
			debug("PEEK");
			return memory[SP.value];
		} else if (code == 0x1a) {
			debug("PUSH");
			return memory[--SP.value];
		} else if (code == 0x1b) {
			debug("SP");
			return SP;
		} else if (code == 0x1c) {
			debug("PC");
			return PC;
		} else if (code == 0x1d) {
			debug("O");
			return O;
		} else if (code == 0x1e) {
			debug("[next word]");
			return memory[memory[PC.value++].value];
		} else if (code == 0x1f) {
			debug("next word (literal)");
			return new Cell(memory[PC.value++].value);
		}
		debug("literal: " + (code - 0x20));
		return new Cell(code - 0x20);
	}
	
	private void skipInstruction() {
		// This skips to the end of the next instruction - used in IF operations.
		int instruction = memory[PC.value++].value;
		int a, b = -1;
		if ((instruction & 0b11111) == 0) {
			// Non-basic opcode. aaaaaaooooo00000
			a = instruction >> 10 & 0b111111;
		} else {
			// Basic opcode. aaaaaabbbbbooooo
			a = instruction >> 10 & 0b111111;
			b = instruction >>  5 & 0b11111;
		}
		
		if ((a >= 0x10 && a <= 0x17) || a == 0x1e || a == 0x1f)
			PC.value++;
		if ((b >= 0x10 && b <= 0x17) || b == 0x1e || b == 0x1f)
			PC.value++;
	}
	
	private void processBasic(int opcode, Cell cellA, Cell cellB) {
		int a = cellA.value;
		int b = cellB.value;
		int o = 0;
		switch (opcode) {
		case 0x1: // SET a to b
			debugln("SET");
			a = b;
			break;
		case 0x2: // ADD b to a
			debugln("ADD");
			o = (a += b) > 0xffff ? 1 : 0;
			a &= 0xffff;
			break;
		case 0x3: // SUBTRACT b from a
			debugln("SUB");
			o = (a -= b) < 0 ? 0xffff : 0;
			a = a < 0 ? 0 : a;
			break;
		case 0x4: // MUL multiplies a by b
			debugln("MUL");
			o = (a *= b) >> 16 & 0xffff;
			a &= 0xffff;
			break;
		case 0x5: // DIV divides a by b
			debugln("DIV");
			if (b == 0) {
				a = 0;
				O.value = 0;
			} else {
				O.value = (char)(((a << 16) / b) & 0xffff);
				a /= b;
			}
			break;
		case 0x6: // MOD (sets a to a % b)
			debugln("MOD");
			a = (b == 0) ? 0 : a % b;
			break;
		case 0x7: // SHL shifts a left by b
			debugln("SHL");
			O.value = (char)(a << b >> 16 & 0xffff);
			a = a << b & 0xffff;
			break;
		case 0x8: // SHR shifts a right by b
			debugln("SHR");
			O.value = (char)(a << 16 >> b & 0xffff);
			a >>= b;
			break;
		case 0x9: // AND sets a to a & b
			debugln("AND");
			a &= b;
			break;
		case 0xa: // BOR sets a to a | b
			debugln("BOR");
			a |= b;
			break;
		case 0xb: // XOR sets a to a ^ b
			debugln("XOR");
			a ^= b;
			break;
		case 0xc: // IFE performs next instruction if a == b
			debugln("IFE");
			if (a != b)
				skipInstruction();
			break;
		case 0xd: // IFN performs next instruction if a != b
			debugln("IFN");
			if (a == b)
				skipInstruction();
			break;
		case 0xe: // IFG performs next instruction if a > b
			debugln("IFG");
			if (a <= b)
				skipInstruction();
			break;
		case 0xf: // IFB performs next instructions if (a & b) != 0
			debugln("IFB");
			if ((a & b) == 0)
				skipInstruction();
			break;
		default:
			debugln("INVALID BASIC OPERATION");
		}
		cellA.set(a);
		cellB.set(b);
		O.set(o);
	}

	private void processSpecial(int opcode, Cell a) {
		switch (opcode) {
		case 0x0: // EXIT custom code, makes the processor stop.
			debugln("EXIT");
			running = false;
			break;
		case 0x1: // JSR pushes the address of the next instruction to the stack, sets PC to a
			debugln("JSR");
			memory[--SP.value].value = PC.value;
			PC.value = a.value;
			break;
		default:
			debug("INVALID SPECIAL OPERATION: " + opcode);
		}
	}
	
	@SuppressWarnings("unused")
	public void cycle() {
		if (debug && labels != null && labels.containsKey(PC.value)) {
			System.out.println(labels.get(PC.value));
		}
		
		int instruction = memory[PC.value].value;
		int opcode;
		int rawA, rawB = -1;
		if ((instruction & 0b11111) == 0) {
			// Non-basic opcode. aaaaaaooooo00000
			instruction >>= 5;
			opcode = instruction & 0b11111;
			rawA = instruction >> 6 & 0b111111;
		} else {
			// Basic opcode. aaaaaabbbbbooooo
			opcode = instruction & 0b11111;
			rawA = instruction >> 10 & 0b111111;
			rawB = instruction >>  5 & 0b11111;
		}
		
		PC.value++;
		
		debug("A: ");
		Cell a = handleArgument(rawA), b = null;
		debugln(" = " + (int)a.value);
		if (rawB != -1) {
			debug("B: ");
			b = handleArgument(rawB);
			debugln(" = " + (int)b.value);
		}
		
		if (b != null)
			processBasic(opcode, a, b);
		else
			processSpecial(opcode, a);
		
		instructionCount++;
	}
}